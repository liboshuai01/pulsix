package cn.liboshuai.pulsix.engine.flink;

import cn.liboshuai.pulsix.engine.context.EvalContext;
import cn.liboshuai.pulsix.engine.flink.runtime.SceneReleaseTimeline;
import cn.liboshuai.pulsix.engine.flink.runtime.SceneRuntimeCache;
import cn.liboshuai.pulsix.engine.flink.typeinfo.EngineTypeInfos;
import cn.liboshuai.pulsix.engine.model.EngineErrorCodes;
import cn.liboshuai.pulsix.engine.model.EngineErrorRecord;
import cn.liboshuai.pulsix.engine.model.EngineErrorTypes;
import cn.liboshuai.pulsix.engine.model.RiskEvent;
import cn.liboshuai.pulsix.engine.model.SceneSnapshotEnvelope;
import cn.liboshuai.pulsix.engine.runtime.CompiledSceneRuntime;
import cn.liboshuai.pulsix.engine.runtime.RuntimeCompileException;
import cn.liboshuai.pulsix.engine.runtime.RuntimeCompiler;
import cn.liboshuai.pulsix.engine.runtime.SceneRuntimeManager;
import cn.liboshuai.pulsix.engine.script.DefaultScriptCompiler;
import cn.liboshuai.pulsix.engine.snapshot.SceneSnapshotEnvelopes;
import cn.liboshuai.pulsix.engine.support.ValueConverter;
import org.apache.flink.api.common.state.ListState;
import org.apache.flink.api.common.state.ListStateDescriptor;
import org.apache.flink.api.common.state.MapStateDescriptor;
import org.apache.flink.api.common.state.ReadOnlyBroadcastState;
import org.apache.flink.api.common.state.ValueState;
import org.apache.flink.api.common.state.ValueStateDescriptor;
import org.apache.flink.api.common.typeinfo.Types;
import org.apache.flink.streaming.api.TimeDomain;
import org.apache.flink.streaming.api.TimerService;
import org.apache.flink.streaming.api.functions.co.KeyedBroadcastProcessFunction;
import org.apache.flink.util.Collector;
import org.apache.flink.util.OutputTag;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public class StreamFeatureRoutingProcessFunction
        extends KeyedBroadcastProcessFunction<String, RiskEvent, SceneSnapshotEnvelope, StreamFeatureRouteEvent> {

    public static final OutputTag<PreparedDecisionInput> PREPARED_DECISION_BYPASS = new OutputTag<>(
            "prepared-decision-bypass",
            EngineTypeInfos.preparedDecisionInput()
    );

    private static final String PENDING_EVENTS_STATE_NAME = "stream-route-pending-events";

    private static final String PENDING_RETRY_AT_STATE_NAME = "stream-route-pending-retry-at";

    private static final String PENDING_EVENT_COUNT_STATE_NAME = "stream-route-pending-event-count";

    private static final String PENDING_OLDEST_BUFFERED_AT_STATE_NAME = "stream-route-pending-oldest-buffered-at";

    private final MapStateDescriptor<String, SceneReleaseTimeline> snapshotStateDescriptor;

    private transient SceneRuntimeManager runtimeManager;

    private transient SceneRuntimeCache runtimeCache;

    private transient ListState<RiskEvent> pendingEventsState;

    private transient ValueState<Long> pendingRetryAtState;

    private transient ValueState<Integer> pendingEventCountState;

    private transient ValueState<Long> pendingOldestBufferedAtState;

    private transient FlinkDecisionMetrics metrics;

    public StreamFeatureRoutingProcessFunction(MapStateDescriptor<String, SceneReleaseTimeline> snapshotStateDescriptor) {
        this.snapshotStateDescriptor = snapshotStateDescriptor;
    }

    @Override
    public void open(org.apache.flink.configuration.Configuration parameters) {
        this.runtimeManager = new SceneRuntimeManager(new RuntimeCompiler(new DefaultScriptCompiler()));
        this.runtimeCache = new SceneRuntimeCache();
        org.apache.flink.api.common.functions.RuntimeContext runtimeContext = getRuntimeContext();
        this.pendingEventsState = runtimeContext.getListState(new ListStateDescriptor<>(
                PENDING_EVENTS_STATE_NAME,
                EngineTypeInfos.riskEvent()
        ));
        this.pendingRetryAtState = runtimeContext.getState(new ValueStateDescriptor<>(
                PENDING_RETRY_AT_STATE_NAME,
                Types.LONG
        ));
        this.pendingEventCountState = runtimeContext.getState(new ValueStateDescriptor<>(
                PENDING_EVENT_COUNT_STATE_NAME,
                Types.INT
        ));
        this.pendingOldestBufferedAtState = runtimeContext.getState(new ValueStateDescriptor<>(
                PENDING_OLDEST_BUFFERED_AT_STATE_NAME,
                Types.LONG
        ));
        this.metrics = FlinkDecisionMetrics.create(runtimeContext.getMetricGroup());
    }

    @Override
    public void processBroadcastElement(SceneSnapshotEnvelope envelope,
                                        Context context,
                                        Collector<StreamFeatureRouteEvent> collector) throws Exception {
        try {
            SceneSnapshotEnvelope normalizedEnvelope = SceneSnapshotEnvelopes.fromEnvelope(envelope);
            org.apache.flink.api.common.state.BroadcastState<String, SceneReleaseTimeline> broadcastState =
                    context.getBroadcastState(snapshotStateDescriptor);
            SceneReleaseTimeline timeline = timelineOf(broadcastState, normalizedEnvelope.getSceneCode());
            if (timeline.hasVersionConflict(normalizedEnvelope)) {
                emitEngineError(context, snapshotError("snapshot-version-conflict",
                        EngineErrorCodes.SNAPSHOT_VERSION_CONFLICT,
                        normalizedEnvelope,
                        new IllegalStateException("same version but different checksum")));
                return;
            }
            if (!timeline.contains(normalizedEnvelope)) {
                CompiledSceneRuntime runtime = runtimeManager.compile(normalizedEnvelope.getSnapshot());
                runtimeCache.put(runtime);
                timeline.add(normalizedEnvelope);
                broadcastState.put(normalizedEnvelope.getSceneCode(), timeline);
                metrics.onSnapshotCompiled();
            }
            activateEffectiveRuntime(normalizedEnvelope.getSceneCode(), timeline,
                    Instant.ofEpochMilli(context.currentProcessingTime()));
        } catch (Exception exception) {
            emitEngineError(context, snapshotError("snapshot-route-activate",
                    EngineErrorCodes.SNAPSHOT_ACTIVATE_FAILED,
                    envelope,
                    exception));
        }
    }

    @Override
    public void processElement(RiskEvent event,
                               ReadOnlyContext context,
                               Collector<StreamFeatureRouteEvent> collector) throws Exception {
        metrics.onInputEvent();
        String sceneKey = event.getSceneCode();
        long processingTimeMs = context.timerService().currentProcessingTime();
        CompiledSceneRuntime runtime = resolveRuntime(event.getSceneCode(),
                Instant.ofEpochMilli(processingTimeMs),
                context.getBroadcastState(snapshotStateDescriptor)).orElse(null);
        OutputContext outputContext = new OutputContext() {
            @Override
            public void emitEngineError(EngineErrorRecord record) {
                context.output(EngineOutputTags.ENGINE_ERROR, record);
                metrics.onEngineError(record);
            }

            @Override
            public void emitBypassPreparedDecision(PreparedDecisionInput input) {
                context.output(PREPARED_DECISION_BYPASS, input);
                metrics.onPreparedBypass();
            }
        };
        if (runtime == null) {
            metrics.onNoSnapshot();
            dropExpiredPendingEventsIfNeeded(processingTimeMs, outputContext);
            boolean buffered = bufferPendingEvent(event, processingTimeMs, outputContext);
            if (buffered || hasPendingEvents()) {
                schedulePendingRetry(sceneKey, context.timerService());
            }
            return;
        }
        dropExpiredPendingEventsIfNeeded(processingTimeMs, outputContext);
        clearPendingRetryTimer();
        flushPendingEvents(runtime, collector, outputContext, processingTimeMs);
        routeEvent(event, runtime, collector, outputContext, processingTimeMs);
    }

    @Override
    public void onTimer(long timestamp,
                        OnTimerContext context,
                        Collector<StreamFeatureRouteEvent> collector) throws Exception {
        if (!isPendingRetryTimer(timestamp, context)) {
            return;
        }
        clearPendingRetryTimer();
        OutputContext outputContext = new OutputContext() {
            @Override
            public void emitEngineError(EngineErrorRecord record) {
                context.output(EngineOutputTags.ENGINE_ERROR, record);
                metrics.onEngineError(record);
            }

            @Override
            public void emitBypassPreparedDecision(PreparedDecisionInput input) {
                context.output(PREPARED_DECISION_BYPASS, input);
                metrics.onPreparedBypass();
            }
        };
        if (dropExpiredPendingEventsIfNeeded(timestamp, outputContext)) {
            return;
        }
        String sceneKey = context.getCurrentKey();
        CompiledSceneRuntime runtime = resolveRuntime(sceneKey,
                Instant.ofEpochMilli(timestamp),
                context.getBroadcastState(snapshotStateDescriptor)).orElse(null);
        if (runtime == null) {
            if (hasPendingEvents()) {
                schedulePendingRetry(sceneKey, context.timerService());
            }
            return;
        }
        flushPendingEvents(runtime, collector, outputContext, timestamp);
    }

    private void routeEvent(RiskEvent event,
                            CompiledSceneRuntime runtime,
                            Collector<StreamFeatureRouteEvent> collector,
                            OutputContext outputContext,
                            long preparedAtEpochMs) {
        if (runtime == null || event == null) {
            return;
        }
        List<CompiledSceneRuntime.CompiledStreamFeature> streamFeatures = runtime.getStreamFeatures();
        if (streamFeatures == null || streamFeatures.isEmpty()) {
            outputContext.emitBypassPreparedDecision(newPreparedDecisionInput(event, runtime.getSnapshot(), preparedAtEpochMs, Map.of()));
            return;
        }
        CompiledSceneRuntime.StreamFeatureRoutingPlan routingPlan = runtime.getStreamFeatureRoutingPlan();
        List<CompiledSceneRuntime.StreamFeatureGroupPlan> groups = routingPlan == null ? List.of() : routingPlan.getGroups();
        if (groups == null || groups.isEmpty()) {
            outputContext.emitEngineError(runtimeError("stream-feature-route",
                    EngineErrorTypes.SNAPSHOT,
                    EngineErrorCodes.SNAPSHOT_COMPILE_FAILED,
                    event,
                    runtime,
                    new IllegalStateException("stream feature routing groups must not be empty")));
            return;
        }
        Map<String, CompiledSceneRuntime.CompiledStreamFeature> featuresByCode = streamFeaturesByCode(runtime);
        int expectedGroupCount = groups.size();
        String eventJoinKey = eventJoinKey(event);
        int routedGroupCount = 0;
        for (CompiledSceneRuntime.StreamFeatureGroupPlan group : groups) {
            if (group == null || group.getFeatureCodes() == null || group.getFeatureCodes().isEmpty()) {
                continue;
            }
            String entityKey = resolveEntityKey(event, group, featuresByCode);
            StreamFeatureRouteEvent routedEvent = new StreamFeatureRouteEvent();
            routedEvent.setSceneCode(event.getSceneCode());
            routedEvent.setGroupKey(group.groupKey());
            routedEvent.setRouteExecutionKey(buildRouteExecutionKey(group.groupKey(), entityKey));
            routedEvent.setEventJoinKey(eventJoinKey);
            routedEvent.setExpectedGroupCount(expectedGroupCount);
            routedEvent.setPreparedAtEpochMs(preparedAtEpochMs);
            routedEvent.setEvent(event);
            routedEvent.setSnapshot(runtime.getSnapshot());
            routedEvent.setFeatureCodes(new ArrayList<>(group.getFeatureCodes()));
            collector.collect(routedEvent);
            routedGroupCount++;
        }
        if (routedGroupCount <= 0) {
            outputContext.emitEngineError(runtimeError("stream-feature-route",
                    EngineErrorTypes.SNAPSHOT,
                    EngineErrorCodes.SNAPSHOT_COMPILE_FAILED,
                    event,
                    runtime,
                    new IllegalStateException("stream feature routing groups must produce at least one routed event")));
            return;
        }
        metrics.onPreparedRoute(routedGroupCount);
    }

    private String resolveEntityKey(RiskEvent event,
                                    CompiledSceneRuntime.StreamFeatureGroupPlan group,
                                    Map<String, CompiledSceneRuntime.CompiledStreamFeature> featuresByCode) {
        if (event == null || group == null || group.getFeatureCodes() == null || group.getFeatureCodes().isEmpty()) {
            return "entity:blank";
        }
        CompiledSceneRuntime.CompiledStreamFeature feature = featuresByCode.get(group.getFeatureCodes().get(0));
        if (feature == null || feature.getEntityKeyScript() == null) {
            return "entity:blank";
        }
        EvalContext context = new EvalContext();
        context.setSceneCode(event.getSceneCode());
        context.setEvent(event);
        context.getValues().putAll(event.toFlatMap());
        try {
            String entityKey = ValueConverter.asString(feature.getEntityKeyScript().execute(context));
            if (entityKey == null || entityKey.isBlank()) {
                return "entity:blank";
            }
            return entityKey.trim();
        } catch (RuntimeException ignored) {
            return "entity:blank";
        }
    }

    private Map<String, CompiledSceneRuntime.CompiledStreamFeature> streamFeaturesByCode(CompiledSceneRuntime runtime) {
        Map<String, CompiledSceneRuntime.CompiledStreamFeature> featuresByCode = new LinkedHashMap<>();
        if (runtime == null || runtime.getStreamFeatures() == null) {
            return featuresByCode;
        }
        for (CompiledSceneRuntime.CompiledStreamFeature feature : runtime.getStreamFeatures()) {
            if (feature == null || feature.getSpec() == null || feature.getSpec().getCode() == null) {
                continue;
            }
            featuresByCode.put(feature.getSpec().getCode(), feature);
        }
        return featuresByCode;
    }

    private PreparedDecisionInput newPreparedDecisionInput(RiskEvent event,
                                                           cn.liboshuai.pulsix.engine.model.SceneSnapshot snapshot,
                                                           long preparedAtEpochMs,
                                                           Map<String, String> featureSnapshot) {
        PreparedDecisionInput input = new PreparedDecisionInput();
        input.setSceneCode(event == null ? null : event.getSceneCode());
        input.setEventJoinKey(eventJoinKey(event));
        input.setPreparedAtEpochMs(preparedAtEpochMs);
        input.setEvent(event);
        input.setSnapshot(snapshot);
        input.setFeatureSnapshot(featureSnapshot);
        return input;
    }

    private String buildRouteExecutionKey(String groupKey, String entityKey) {
        String safeGroupKey = groupKey == null || groupKey.isBlank() ? "group:default" : groupKey.trim();
        String safeEntityKey = entityKey == null || entityKey.isBlank() ? "entity:blank" : entityKey.trim();
        return safeGroupKey + '|' + safeEntityKey;
    }

    private String eventJoinKey(RiskEvent event) {
        if (event == null) {
            return "scene:default|event:null|trace:null|time:0";
        }
        String sceneCode = event.getSceneCode() == null || event.getSceneCode().isBlank() ? "scene:default" : event.getSceneCode().trim();
        String eventId = event.getEventId() == null || event.getEventId().isBlank() ? "event:null" : event.getEventId().trim();
        String traceId = event.getTraceId() == null || event.getTraceId().isBlank() ? "trace:null" : event.getTraceId().trim();
        long eventTime = event.getEventTime() == null ? 0L : event.getEventTime().toEpochMilli();
        return sceneCode + '|' + eventId + '|' + traceId + "|time:" + eventTime;
    }

    private Optional<CompiledSceneRuntime> resolveRuntime(String sceneCode,
                                                          Instant referenceTime,
                                                          ReadOnlyBroadcastState<String, SceneReleaseTimeline> broadcastState) throws Exception {
        if (sceneCode == null || broadcastState == null) {
            return Optional.empty();
        }
        SceneReleaseTimeline timeline = broadcastState.get(sceneCode);
        if (timeline == null) {
            return Optional.empty();
        }
        SceneSnapshotEnvelope envelope = timeline.effectiveAt(referenceTime).orElse(null);
        if (envelope == null || envelope.getSnapshot() == null) {
            return Optional.empty();
        }
        Optional<CompiledSceneRuntime> activeRuntime = runtimeManager.getActiveRuntime(sceneCode);
        if (activeRuntime.isPresent()
                && Objects.equals(activeRuntime.get().version(), envelope.getVersion())
                && Objects.equals(activeRuntime.get().getSnapshot().getChecksum(), envelope.getChecksum())) {
            return activeRuntime;
        }
        Optional<CompiledSceneRuntime> cachedRuntime = runtimeCache.get(sceneCode, envelope.getVersion());
        if (cachedRuntime.isPresent()) {
            runtimeManager.activate(cachedRuntime.get());
            return cachedRuntime;
        }
        CompiledSceneRuntime compiledRuntime = runtimeManager.compile(envelope.getSnapshot());
        runtimeCache.put(compiledRuntime);
        runtimeManager.activate(compiledRuntime);
        return Optional.of(compiledRuntime);
    }

    private void activateEffectiveRuntime(String sceneCode,
                                          SceneReleaseTimeline timeline,
                                          Instant referenceTime) {
        if (sceneCode == null || timeline == null) {
            return;
        }
        SceneSnapshotEnvelope envelope = timeline.effectiveAt(referenceTime).orElse(null);
        if (envelope == null || envelope.getSnapshot() == null) {
            return;
        }
        Optional<CompiledSceneRuntime> activeRuntime = runtimeManager.getActiveRuntime(sceneCode);
        if (activeRuntime.isPresent()
                && Objects.equals(activeRuntime.get().version(), envelope.getVersion())
                && Objects.equals(activeRuntime.get().getSnapshot().getChecksum(), envelope.getChecksum())) {
            return;
        }
        runtimeCache.get(sceneCode, envelope.getVersion())
                .ifPresentOrElse(runtimeManager::activate, () -> runtimeManager.activate(runtimeManager.compile(envelope.getSnapshot())));
    }

    private SceneReleaseTimeline timelineOf(org.apache.flink.api.common.state.BroadcastState<String, SceneReleaseTimeline> broadcastState,
                                            String sceneCode) throws Exception {
        SceneReleaseTimeline timeline = broadcastState == null || sceneCode == null ? null : broadcastState.get(sceneCode);
        if (timeline != null) {
            return timeline;
        }
        SceneReleaseTimeline createdTimeline = new SceneReleaseTimeline();
        createdTimeline.setSceneCode(sceneCode);
        return createdTimeline;
    }

    private void flushPendingEvents(CompiledSceneRuntime runtime,
                                    Collector<StreamFeatureRouteEvent> collector,
                                    OutputContext outputContext,
                                    long processingTimeMs) throws Exception {
        List<RiskEvent> events = pendingEventsSnapshot();
        clearPendingEventState();
        metrics.onPendingFlushed(events.size());
        metrics.onPendingOldestAgeObserved(0L);
        if (events.isEmpty()) {
            return;
        }
        for (RiskEvent event : events) {
            routeEvent(event, runtime, collector, outputContext, processingTimeMs);
        }
    }

    private boolean bufferPendingEvent(RiskEvent event,
                                       long processingTimeMs,
                                       OutputContext outputContext) throws Exception {
        int currentPendingCount = pendingEventCount();
        if (currentPendingCount >= PendingEventDefaults.DEFAULT_MAX_PENDING_EVENTS_PER_KEY) {
            metrics.onPendingDropped(1);
            metrics.onPendingOldestAgeObserved(pendingOldestAgeMs(processingTimeMs));
            outputContext.emitEngineError(pendingError("stream-feature-route-pending-overflow",
                    EngineErrorCodes.PENDING_BUFFER_OVERFLOW,
                    event,
                    new IllegalStateException("pending buffer overflow: sceneCode=" + event.getSceneCode()
                            + ", limit=" + PendingEventDefaults.DEFAULT_MAX_PENDING_EVENTS_PER_KEY
                            + ", currentCount=" + currentPendingCount)));
            return false;
        }
        pendingEventsState.add(event);
        pendingEventCountState.update(currentPendingCount + 1);
        if (pendingOldestBufferedAtState.value() == null) {
            pendingOldestBufferedAtState.update(processingTimeMs);
        }
        metrics.onPendingBuffered();
        metrics.onPendingOldestAgeObserved(pendingOldestAgeMs(processingTimeMs));
        return true;
    }

    private boolean hasPendingEvents() throws Exception {
        return pendingEventCount() > 0;
    }

    private void schedulePendingRetry(String sceneKey, TimerService timerService) throws Exception {
        if (sceneKey == null || timerService == null) {
            return;
        }
        if (pendingRetryAtState.value() != null) {
            return;
        }
        long nextRetryAt = timerService.currentProcessingTime() + PendingEventDefaults.DEFAULT_PENDING_RETRY_DELAY_MS;
        pendingRetryAtState.update(nextRetryAt);
        timerService.registerProcessingTimeTimer(nextRetryAt);
    }

    private void clearPendingRetryTimer() throws Exception {
        pendingRetryAtState.clear();
    }

    private List<RiskEvent> pendingEventsSnapshot() throws Exception {
        List<RiskEvent> events = new ArrayList<>();
        Iterable<RiskEvent> bufferedEvents = pendingEventsState.get();
        if (bufferedEvents == null) {
            return events;
        }
        for (RiskEvent event : bufferedEvents) {
            if (event != null) {
                events.add(event);
            }
        }
        return events;
    }

    private void clearPendingEventState() throws Exception {
        pendingEventsState.clear();
        pendingEventCountState.clear();
        pendingOldestBufferedAtState.clear();
    }

    private int pendingEventCount() throws Exception {
        Integer count = pendingEventCountState.value();
        if (count != null) {
            return count;
        }
        int derivedCount = pendingEventsSnapshot().size();
        if (derivedCount > 0) {
            pendingEventCountState.update(derivedCount);
        }
        return derivedCount;
    }

    private RiskEvent firstPendingEvent() throws Exception {
        Iterable<RiskEvent> bufferedEvents = pendingEventsState.get();
        if (bufferedEvents == null) {
            return null;
        }
        for (RiskEvent event : bufferedEvents) {
            if (event != null) {
                return event;
            }
        }
        return null;
    }

    private boolean dropExpiredPendingEventsIfNeeded(long processingTimeMs,
                                                     OutputContext outputContext) throws Exception {
        if (!hasPendingEvents()) {
            metrics.onPendingOldestAgeObserved(0L);
            return false;
        }
        long oldestAgeMs = pendingOldestAgeMs(processingTimeMs);
        if (oldestAgeMs <= PendingEventDefaults.DEFAULT_MAX_PENDING_EVENT_AGE_MS) {
            metrics.onPendingOldestAgeObserved(oldestAgeMs);
            return false;
        }
        int droppedCount = pendingEventCount();
        RiskEvent representativeEvent = firstPendingEvent();
        clearPendingEventState();
        clearPendingRetryTimer();
        metrics.onPendingDropped(droppedCount);
        metrics.onPendingOldestAgeObserved(0L);
        outputContext.emitEngineError(pendingError("stream-feature-route-pending-timeout",
                EngineErrorCodes.PENDING_BUFFER_EXPIRED,
                representativeEvent,
                new IllegalStateException("pending buffer expired: sceneCode="
                        + (representativeEvent == null ? null : representativeEvent.getSceneCode())
                        + ", oldestAgeMs=" + oldestAgeMs
                        + ", limitMs=" + PendingEventDefaults.DEFAULT_MAX_PENDING_EVENT_AGE_MS
                        + ", droppedCount=" + droppedCount)));
        return true;
    }

    private long pendingOldestAgeMs(long processingTimeMs) throws Exception {
        Long oldestBufferedAt = pendingOldestBufferedAtState.value();
        if (oldestBufferedAt == null || pendingEventCount() == 0) {
            return 0L;
        }
        return Math.max(processingTimeMs - oldestBufferedAt, 0L);
    }

    private boolean isPendingRetryTimer(long timestamp, OnTimerContext context) throws Exception {
        Long expectedRetryAt = pendingRetryAtState.value();
        if (expectedRetryAt == null || expectedRetryAt != timestamp) {
            return false;
        }
        return context.timeDomain() == TimeDomain.PROCESSING_TIME;
    }

    private EngineErrorRecord pendingError(String stage,
                                           String errorCode,
                                           RiskEvent event,
                                           Throwable throwable) {
        return EngineErrorRecord.of(stage,
                EngineErrorTypes.STATE,
                errorCode,
                event,
                null,
                throwable);
    }

    private EngineErrorRecord snapshotError(String stage,
                                            String defaultErrorCode,
                                            SceneSnapshotEnvelope envelope,
                                            Throwable throwable) {
        String resolvedErrorCode = throwable instanceof RuntimeCompileException compileException
                ? compileException.getErrorCode()
                : defaultErrorCode;
        EngineErrorRecord record = EngineErrorRecord.of(stage,
                EngineErrorTypes.SNAPSHOT,
                resolvedErrorCode,
                null,
                envelope != null ? envelope.getVersion() : null,
                throwable);
        record.setSceneCode(envelope != null ? envelope.getSceneCode() : null);
        if (envelope != null && envelope.getSnapshot() != null) {
            record.setSnapshotId(envelope.getSnapshot().getSnapshotId());
            record.setSnapshotChecksum(envelope.getSnapshot().getChecksum());
        } else if (envelope != null) {
            record.setSnapshotChecksum(envelope.getChecksum());
        }
        if (throwable instanceof RuntimeCompileException compileException) {
            record.setFeatureCode(compileException.getFeatureCode());
            record.setRuleCode(compileException.getRuleCode());
            record.setEngineType(compileException.getEngineType() == null ? null : compileException.getEngineType().name());
        }
        return record;
    }

    private EngineErrorRecord runtimeError(String stage,
                                           String errorType,
                                           String errorCode,
                                           RiskEvent event,
                                           CompiledSceneRuntime runtime,
                                           Throwable throwable) {
        EngineErrorRecord record = EngineErrorRecord.of(stage,
                errorType,
                errorCode,
                event,
                runtime == null ? null : runtime.version(),
                throwable);
        if (runtime != null && runtime.getSnapshot() != null) {
            record.setSnapshotId(runtime.getSnapshot().getSnapshotId());
            record.setSnapshotChecksum(runtime.getSnapshot().getChecksum());
        }
        return record;
    }

    private void emitEngineError(Context context, EngineErrorRecord record) {
        context.output(EngineOutputTags.ENGINE_ERROR, record);
        metrics.onEngineError(record);
    }

    private interface OutputContext {

        void emitEngineError(EngineErrorRecord record);

        void emitBypassPreparedDecision(PreparedDecisionInput input);

    }

}
