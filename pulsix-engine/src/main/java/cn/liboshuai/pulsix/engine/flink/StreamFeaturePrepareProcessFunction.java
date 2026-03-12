package cn.liboshuai.pulsix.engine.flink;

import cn.liboshuai.pulsix.engine.core.DecisionExecutionException;
import cn.liboshuai.pulsix.engine.core.DecisionExecutor;
import cn.liboshuai.pulsix.engine.feature.FlinkKeyedStateStreamFeatureStateStore;
import cn.liboshuai.pulsix.engine.feature.StreamFeatureStateStore;
import cn.liboshuai.pulsix.engine.model.EngineErrorCodes;
import cn.liboshuai.pulsix.engine.model.EngineErrorRecord;
import cn.liboshuai.pulsix.engine.model.EngineErrorTypes;
import cn.liboshuai.pulsix.engine.model.SceneSnapshot;
import cn.liboshuai.pulsix.engine.runtime.CompiledSceneRuntime;
import cn.liboshuai.pulsix.engine.runtime.RuntimeCompileException;
import cn.liboshuai.pulsix.engine.runtime.RuntimeCompiler;
import cn.liboshuai.pulsix.engine.script.DefaultScriptCompiler;
import org.apache.flink.api.common.functions.RuntimeContext;
import org.apache.flink.streaming.api.TimeDomain;
import org.apache.flink.streaming.api.TimerService;
import org.apache.flink.streaming.api.functions.KeyedProcessFunction;
import org.apache.flink.util.Collector;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class StreamFeaturePrepareProcessFunction
        extends KeyedProcessFunction<String, StreamFeatureRouteEvent, PreparedStreamFeatureChunk> {

    private transient RuntimeCompiler runtimeCompiler;

    private transient Map<String, CompiledSceneRuntime> runtimeCache;

    private transient StreamFeatureStateStore stateStore;

    private transient DecisionExecutor decisionExecutor;

    private transient FlinkDecisionMetrics metrics;

    @Override
    public void open(org.apache.flink.configuration.Configuration parameters) {
        RuntimeContext runtimeContext = getRuntimeContext();
        this.runtimeCompiler = new RuntimeCompiler(new DefaultScriptCompiler());
        this.runtimeCache = new ConcurrentHashMap<>();
        this.stateStore = new FlinkKeyedStateStreamFeatureStateStore(runtimeContext);
        this.decisionExecutor = new DecisionExecutor();
        this.metrics = FlinkDecisionMetrics.create(runtimeContext.getMetricGroup());
    }

    @Override
    public void processElement(StreamFeatureRouteEvent value,
                               Context context,
                               Collector<PreparedStreamFeatureChunk> collector) {
        if (value == null || value.getEvent() == null || value.getSnapshot() == null) {
            return;
        }
        CompiledSceneRuntime runtime = null;
        stateStore.bindExecutionContext(new FlinkExecutionContext(context.timerService(), context.timerService().currentWatermark()));
        try {
            runtime = resolveRuntime(value.getSnapshot());
            Map<String, String> featureSnapshot = decisionExecutor.prepareStreamFeatureSnapshot(runtime,
                    value.getEvent(),
                    stateStore,
                    value.getFeatureCodes());
            PreparedStreamFeatureChunk chunk = new PreparedStreamFeatureChunk();
            chunk.setSceneCode(value.getSceneCode());
            chunk.setEventJoinKey(value.getEventJoinKey());
            chunk.setExpectedGroupCount(value.getExpectedGroupCount());
            chunk.setPreparedAtEpochMs(value.getPreparedAtEpochMs());
            chunk.setEvent(value.getEvent());
            chunk.setSnapshot(value.getSnapshot());
            chunk.setFeatureSnapshot(featureSnapshot);
            collector.collect(chunk);
            metrics.onPreparedChunk();
        } catch (Exception exception) {
            emitEngineError(context, errorRecord(value, runtime, exception));
        } finally {
            stateStore.clearExecutionContext();
        }
    }

    @Override
    public void onTimer(long timestamp,
                        OnTimerContext context,
                        Collector<PreparedStreamFeatureChunk> collector) {
        if (context.timeDomain() != TimeDomain.EVENT_TIME) {
            return;
        }
        try {
            stateStore.onTimer(timestamp);
        } catch (Exception exception) {
            emitEngineError(context, EngineErrorRecord.of("stream-feature-prepare-timer",
                    EngineErrorTypes.STATE,
                    EngineErrorCodes.STATE_TIMER_CLEANUP_FAILED,
                    null,
                    null,
                    exception));
        }
    }

    private CompiledSceneRuntime resolveRuntime(SceneSnapshot snapshot) {
        String runtimeKey = runtimeKey(snapshot);
        CompiledSceneRuntime cachedRuntime = runtimeCache.get(runtimeKey);
        if (cachedRuntime != null
                && Objects.equals(cachedRuntime.getSnapshot().getChecksum(), snapshot.getChecksum())) {
            return cachedRuntime;
        }
        CompiledSceneRuntime compiledRuntime = runtimeCompiler.compile(snapshot);
        runtimeCache.put(runtimeKey, compiledRuntime);
        return compiledRuntime;
    }

    private String runtimeKey(SceneSnapshot snapshot) {
        if (snapshot == null) {
            return "scene:default|version:0|checksum:null";
        }
        return (snapshot.getSceneCode() == null ? "scene:default" : snapshot.getSceneCode())
                + "|version:" + (snapshot.getVersion() == null ? 0 : snapshot.getVersion())
                + "|checksum:" + Optional.ofNullable(snapshot.getChecksum()).orElse("null");
    }

    private EngineErrorRecord errorRecord(StreamFeatureRouteEvent routedEvent,
                                          CompiledSceneRuntime runtime,
                                          Throwable throwable) {
        if (throwable instanceof RuntimeCompileException compileException) {
            EngineErrorRecord record = EngineErrorRecord.of("stream-feature-prepare-compile",
                    EngineErrorTypes.SNAPSHOT,
                    compileException.getErrorCode(),
                    routedEvent == null ? null : routedEvent.getEvent(),
                    routedEvent != null && routedEvent.getSnapshot() != null ? routedEvent.getSnapshot().getVersion() : null,
                    throwable);
            if (routedEvent != null && routedEvent.getSnapshot() != null) {
                record.setSceneCode(routedEvent.getSnapshot().getSceneCode());
                record.setSnapshotId(routedEvent.getSnapshot().getSnapshotId());
                record.setSnapshotChecksum(routedEvent.getSnapshot().getChecksum());
            }
            record.setFeatureCode(compileException.getFeatureCode());
            record.setRuleCode(compileException.getRuleCode());
            record.setEngineType(compileException.getEngineType() == null ? null : compileException.getEngineType().name());
            return record;
        }
        if (throwable instanceof DecisionExecutionException executionException) {
            EngineErrorRecord record = EngineErrorRecord.of("stream-feature-prepare",
                    executionException.getErrorType(),
                    executionException.getErrorCode(),
                    routedEvent == null ? null : routedEvent.getEvent(),
                    runtime == null ? null : runtime.version(),
                    throwable);
            populateRuntimeDetails(record, runtime);
            record.setFeatureCode(executionException.getFeatureCode());
            record.setRuleCode(executionException.getRuleCode());
            record.setEngineType(executionException.getEngineType() == null ? null : executionException.getEngineType().name());
            return record;
        }
        EngineErrorRecord record = EngineErrorRecord.of("stream-feature-prepare",
                EngineErrorTypes.STATE,
                EngineErrorCodes.STATE_ACCESS_FAILED,
                routedEvent == null ? null : routedEvent.getEvent(),
                runtime == null ? null : runtime.version(),
                throwable);
        populateRuntimeDetails(record, runtime);
        return record;
    }

    private void populateRuntimeDetails(EngineErrorRecord record, CompiledSceneRuntime runtime) {
        if (record == null || runtime == null || runtime.getSnapshot() == null) {
            return;
        }
        record.setSceneCode(runtime.sceneCode());
        record.setSnapshotId(runtime.getSnapshot().getSnapshotId());
        record.setSnapshotChecksum(runtime.getSnapshot().getChecksum());
    }

    private void emitEngineError(Context context, EngineErrorRecord record) {
        context.output(EngineOutputTags.ENGINE_ERROR, record);
        metrics.onEngineError(record);
    }

    private void emitEngineError(OnTimerContext context, EngineErrorRecord record) {
        context.output(EngineOutputTags.ENGINE_ERROR, record);
        metrics.onEngineError(record);
    }

    private record FlinkExecutionContext(TimerService timerService,
                                         long currentWatermark) implements StreamFeatureStateStore.StreamFeatureExecutionContext {

        @Override
        public void registerEventTimeTimer(long timestamp) {
            timerService.registerEventTimeTimer(timestamp);
        }

    }

}
