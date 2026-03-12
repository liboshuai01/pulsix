package cn.liboshuai.pulsix.engine.flink;

import cn.liboshuai.pulsix.engine.flink.typeinfo.EngineTypeInfos;
import cn.liboshuai.pulsix.engine.model.EngineErrorCodes;
import cn.liboshuai.pulsix.engine.model.EngineErrorRecord;
import cn.liboshuai.pulsix.engine.model.EngineErrorTypes;
import cn.liboshuai.pulsix.engine.model.RiskEvent;
import cn.liboshuai.pulsix.engine.model.SceneSnapshot;
import org.apache.flink.api.common.state.MapState;
import org.apache.flink.api.common.state.MapStateDescriptor;
import org.apache.flink.api.common.state.ValueState;
import org.apache.flink.api.common.state.ValueStateDescriptor;
import org.apache.flink.api.common.typeinfo.Types;
import org.apache.flink.streaming.api.TimeDomain;
import org.apache.flink.streaming.api.TimerService;
import org.apache.flink.streaming.api.functions.KeyedProcessFunction;
import org.apache.flink.util.Collector;

import java.util.LinkedHashMap;
import java.util.Map;

public class PreparedDecisionAggregateProcessFunction
        extends KeyedProcessFunction<String, PreparedStreamFeatureChunk, PreparedDecisionInput> {

    static final long DEFAULT_AGGREGATION_TIMEOUT_MS = 30_000L;

    private final long aggregationTimeoutMs;

    private transient MapState<String, String> featureSnapshotState;

    private transient ValueState<Integer> expectedGroupCountState;

    private transient ValueState<Integer> receivedGroupCountState;

    private transient ValueState<Long> preparedAtEpochMsState;

    private transient ValueState<RiskEvent> eventState;

    private transient ValueState<SceneSnapshot> snapshotState;

    private transient ValueState<Long> aggregateTimeoutAtState;

    private transient FlinkDecisionMetrics metrics;

    public PreparedDecisionAggregateProcessFunction() {
        this(DEFAULT_AGGREGATION_TIMEOUT_MS);
    }

    public PreparedDecisionAggregateProcessFunction(long aggregationTimeoutMs) {
        this.aggregationTimeoutMs = Math.max(aggregationTimeoutMs, 1L);
    }

    @Override
    public void open(org.apache.flink.configuration.Configuration parameters) {
        org.apache.flink.api.common.functions.RuntimeContext runtimeContext = getRuntimeContext();
        this.featureSnapshotState = runtimeContext.getMapState(new MapStateDescriptor<>(
                "prepared-decision-feature-snapshot",
                Types.STRING,
                Types.STRING
        ));
        this.expectedGroupCountState = runtimeContext.getState(new ValueStateDescriptor<>(
                "prepared-decision-expected-group-count",
                Types.INT
        ));
        this.receivedGroupCountState = runtimeContext.getState(new ValueStateDescriptor<>(
                "prepared-decision-received-group-count",
                Types.INT
        ));
        this.preparedAtEpochMsState = runtimeContext.getState(new ValueStateDescriptor<>(
                "prepared-decision-prepared-at-epoch-ms",
                Types.LONG
        ));
        this.eventState = runtimeContext.getState(new ValueStateDescriptor<>(
                "prepared-decision-event",
                EngineTypeInfos.riskEvent()
        ));
        this.snapshotState = runtimeContext.getState(new ValueStateDescriptor<>(
                "prepared-decision-snapshot",
                EngineTypeInfos.sceneSnapshot()
        ));
        this.aggregateTimeoutAtState = runtimeContext.getState(new ValueStateDescriptor<>(
                "prepared-decision-aggregate-timeout-at",
                Types.LONG
        ));
        this.metrics = FlinkDecisionMetrics.create(runtimeContext.getMetricGroup());
    }

    @Override
    public void processElement(PreparedStreamFeatureChunk value,
                               Context context,
                               Collector<PreparedDecisionInput> collector) throws Exception {
        if (value == null) {
            return;
        }
        int previousRemainingGroupCount = remainingGroupCount();
        Integer expectedGroupCount = expectedGroupCountState.value();
        if (expectedGroupCount == null) {
            expectedGroupCount = normalizeExpectedGroupCount(value.getExpectedGroupCount());
            expectedGroupCountState.update(expectedGroupCount);
        }
        if (eventState.value() == null) {
            eventState.update(value.getEvent());
        }
        if (snapshotState.value() == null) {
            snapshotState.update(value.getSnapshot());
        }
        Long currentPreparedAtEpochMs = preparedAtEpochMsState.value();
        if (currentPreparedAtEpochMs == null
                || value.getPreparedAtEpochMs() != null && value.getPreparedAtEpochMs() < currentPreparedAtEpochMs) {
            preparedAtEpochMsState.update(value.getPreparedAtEpochMs());
        }
        if (aggregateTimeoutAtState.value() == null) {
            long timeoutAt = context.timerService().currentProcessingTime() + aggregationTimeoutMs;
            aggregateTimeoutAtState.update(timeoutAt);
            context.timerService().registerProcessingTimeTimer(timeoutAt);
        }
        if (value.getFeatureSnapshot() != null) {
            for (Map.Entry<String, String> entry : value.getFeatureSnapshot().entrySet()) {
                featureSnapshotState.put(entry.getKey(), entry.getValue());
            }
        }
        int receivedGroupCount = receivedGroupCountState.value() == null ? 0 : receivedGroupCountState.value();
        receivedGroupCount++;
        receivedGroupCountState.update(receivedGroupCount);

        int remainingGroupCount = remainingGroupCount(expectedGroupCount, receivedGroupCount);
        metrics.onPreparedAggregatePendingGroupsDelta(remainingGroupCount - previousRemainingGroupCount);
        if (receivedGroupCount < expectedGroupCount) {
            return;
        }

        PreparedDecisionInput input = new PreparedDecisionInput();
        input.setSceneCode(value.getSceneCode());
        input.setEventJoinKey(value.getEventJoinKey());
        input.setPreparedAtEpochMs(preparedAtEpochMsState.value());
        input.setEvent(eventState.value());
        input.setSnapshot(snapshotState.value());
        Map<String, String> featureSnapshot = new LinkedHashMap<>();
        for (Map.Entry<String, String> entry : featureSnapshotState.entries()) {
            featureSnapshot.put(entry.getKey(), entry.getValue());
        }
        input.setFeatureSnapshot(featureSnapshot);
        collector.collect(input);
        metrics.onPreparedAggregateCompleted();
        clearState(context.timerService());
    }

    @Override
    public void onTimer(long timestamp,
                        OnTimerContext context,
                        Collector<PreparedDecisionInput> collector) throws Exception {
        if (context.timeDomain() != TimeDomain.PROCESSING_TIME) {
            return;
        }
        Long timeoutAt = aggregateTimeoutAtState.value();
        if (timeoutAt == null || timeoutAt != timestamp) {
            return;
        }
        int remainingGroupCount = remainingGroupCount();
        if (remainingGroupCount <= 0) {
            clearState(context.timerService());
            return;
        }
        EngineErrorRecord record = aggregateTimeoutError(context.getCurrentKey(), remainingGroupCount);
        context.output(EngineOutputTags.ENGINE_ERROR, record);
        metrics.onEngineError(record);
        metrics.onPreparedAggregateTimeout();
        metrics.onPreparedAggregatePendingGroupsDelta(-remainingGroupCount);
        clearState(context.timerService());
    }

    private EngineErrorRecord aggregateTimeoutError(String eventJoinKey,
                                                    int remainingGroupCount) throws Exception {
        RiskEvent event = eventState.value();
        SceneSnapshot snapshot = snapshotState.value();
        Integer expectedGroupCount = expectedGroupCountState.value();
        Integer receivedGroupCount = receivedGroupCountState.value();
        EngineErrorRecord record = EngineErrorRecord.of(
                "prepared-decision-aggregate-timeout",
                EngineErrorTypes.STATE,
                EngineErrorCodes.PREPARED_DECISION_AGGREGATE_TIMEOUT,
                event,
                snapshot == null ? null : snapshot.getVersion(),
                new IllegalStateException("prepared decision aggregate timeout: eventJoinKey=" + eventJoinKey
                        + ", expectedGroupCount=" + expectedGroupCount
                        + ", receivedGroupCount=" + receivedGroupCount
                        + ", remainingGroupCount=" + remainingGroupCount
                        + ", timeoutMs=" + aggregationTimeoutMs)
        );
        if (snapshot != null) {
            record.setSceneCode(snapshot.getSceneCode());
            record.setSnapshotId(snapshot.getSnapshotId());
            record.setSnapshotChecksum(snapshot.getChecksum());
        }
        return record;
    }

    private int remainingGroupCount() throws Exception {
        Integer expectedGroupCount = expectedGroupCountState.value();
        if (expectedGroupCount == null) {
            return 0;
        }
        return remainingGroupCount(expectedGroupCount, receivedGroupCountState.value());
    }

    private int remainingGroupCount(Integer expectedGroupCount,
                                    Integer receivedGroupCount) {
        int normalizedExpectedGroupCount = normalizeExpectedGroupCount(expectedGroupCount);
        int normalizedReceivedGroupCount = receivedGroupCount == null ? 0 : Math.max(receivedGroupCount, 0);
        return Math.max(normalizedExpectedGroupCount - normalizedReceivedGroupCount, 0);
    }

    private int normalizeExpectedGroupCount(Integer expectedGroupCount) {
        return expectedGroupCount == null || expectedGroupCount <= 0 ? 1 : expectedGroupCount;
    }

    private void clearState(TimerService timerService) throws Exception {
        Long timeoutAt = aggregateTimeoutAtState.value();
        if (timerService != null && timeoutAt != null) {
            timerService.deleteProcessingTimeTimer(timeoutAt);
        }
        featureSnapshotState.clear();
        expectedGroupCountState.clear();
        receivedGroupCountState.clear();
        preparedAtEpochMsState.clear();
        eventState.clear();
        snapshotState.clear();
        aggregateTimeoutAtState.clear();
    }

}
