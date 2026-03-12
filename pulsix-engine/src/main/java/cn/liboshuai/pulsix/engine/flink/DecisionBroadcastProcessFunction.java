package cn.liboshuai.pulsix.engine.flink;

import cn.liboshuai.pulsix.engine.core.DecisionExecutor;
import cn.liboshuai.pulsix.engine.core.DecisionExecutionException;
import cn.liboshuai.pulsix.engine.feature.FlinkKeyedStateStreamFeatureStateStore;
import cn.liboshuai.pulsix.engine.feature.InMemoryLookupService;
import cn.liboshuai.pulsix.engine.feature.LookupService;
import cn.liboshuai.pulsix.engine.feature.StreamFeatureStateStore;
import cn.liboshuai.pulsix.engine.flink.runtime.SceneReleaseTimeline;
import cn.liboshuai.pulsix.engine.flink.runtime.SceneRuntimeCache;
import cn.liboshuai.pulsix.engine.flink.typeinfo.EngineTypeInfos;
import cn.liboshuai.pulsix.engine.model.DecisionLogRecord;
import cn.liboshuai.pulsix.engine.model.DecisionResult;
import cn.liboshuai.pulsix.engine.model.EngineErrorCodes;
import cn.liboshuai.pulsix.engine.model.EngineErrorRecord;
import cn.liboshuai.pulsix.engine.model.EngineErrorTypes;
import cn.liboshuai.pulsix.engine.model.RiskEvent;
import cn.liboshuai.pulsix.engine.model.SceneSnapshotEnvelope;
import cn.liboshuai.pulsix.engine.runtime.RuntimeCompileException;
import cn.liboshuai.pulsix.engine.runtime.CompiledSceneRuntime;
import cn.liboshuai.pulsix.engine.runtime.RuntimeCompiler;
import cn.liboshuai.pulsix.engine.runtime.SceneRuntimeManager;
import cn.liboshuai.pulsix.engine.script.DefaultScriptCompiler;
import cn.liboshuai.pulsix.engine.snapshot.SceneSnapshotEnvelopes;
import org.apache.flink.api.common.functions.RuntimeContext;
import org.apache.flink.api.common.state.ListState;
import org.apache.flink.api.common.state.ListStateDescriptor;
import org.apache.flink.api.common.state.MapStateDescriptor;
import org.apache.flink.api.common.state.ReadOnlyBroadcastState;
import org.apache.flink.api.common.state.ValueState;
import org.apache.flink.api.common.state.ValueStateDescriptor;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.api.common.typeinfo.Types;
import org.apache.flink.api.java.typeutils.ResultTypeQueryable;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.streaming.api.TimeDomain;
import org.apache.flink.streaming.api.TimerService;
import org.apache.flink.streaming.api.functions.co.KeyedBroadcastProcessFunction;
import org.apache.flink.util.Collector;

import java.io.Serializable;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Deprecated(forRemoval = true)
class DecisionBroadcastProcessFunction
        extends KeyedBroadcastProcessFunction<String, RiskEvent, SceneSnapshotEnvelope, DecisionResult>
        implements ResultTypeQueryable<DecisionResult> {

    static final long DEFAULT_PENDING_RETRY_DELAY_MS = PendingEventDefaults.DEFAULT_PENDING_RETRY_DELAY_MS;

    static final int DEFAULT_MAX_PENDING_EVENTS_PER_KEY = PendingEventDefaults.DEFAULT_MAX_PENDING_EVENTS_PER_KEY;

    static final long DEFAULT_MAX_PENDING_EVENT_AGE_MS = PendingEventDefaults.DEFAULT_MAX_PENDING_EVENT_AGE_MS;

    private static final String PENDING_EVENTS_STATE_NAME = "pending-events";

    private static final String PENDING_RETRY_AT_STATE_NAME = "pending-retry-at";

    private static final String PENDING_EVENT_COUNT_STATE_NAME = "pending-event-count";

    private static final String PENDING_OLDEST_BUFFERED_AT_STATE_NAME = "pending-oldest-buffered-at";

    private final MapStateDescriptor<String, SceneReleaseTimeline> snapshotStateDescriptor;

    private final EngineLookupServiceFactory lookupServiceFactory;

    private final EngineStreamFeatureStateStoreFactory stateStoreFactory;

    private final long pendingRetryDelayMs;

    private final int maxPendingEventsPerKey;

    private final long maxPendingEventAgeMs;

    private transient LookupService lookupService;

    private transient SceneRuntimeManager runtimeManager;

    private transient SceneRuntimeCache runtimeCache;

    private transient StreamFeatureStateStore stateStore;

    private transient DecisionExecutor decisionExecutor;

    private transient ListState<RiskEvent> pendingEventsState;

    private transient ValueState<Long> pendingRetryAtState;

    private transient ValueState<Integer> pendingEventCountState;

    private transient ValueState<Long> pendingOldestBufferedAtState;

    private transient FlinkDecisionMetrics metrics;

    public DecisionBroadcastProcessFunction(MapStateDescriptor<String, SceneReleaseTimeline> snapshotStateDescriptor) {
        this(snapshotStateDescriptor, InMemoryLookupService::demo);
    }

    public DecisionBroadcastProcessFunction(MapStateDescriptor<String, SceneReleaseTimeline> snapshotStateDescriptor,
                                            EngineLookupServiceFactory lookupServiceFactory) {
        this(snapshotStateDescriptor, lookupServiceFactory, FlinkKeyedStateStreamFeatureStateStore::new);
    }

    public DecisionBroadcastProcessFunction(MapStateDescriptor<String, SceneReleaseTimeline> snapshotStateDescriptor,
                                            EngineLookupServiceFactory lookupServiceFactory,
                                            EngineStreamFeatureStateStoreFactory stateStoreFactory) {
        this(snapshotStateDescriptor,
                lookupServiceFactory,
                stateStoreFactory,
                DEFAULT_PENDING_RETRY_DELAY_MS,
                DEFAULT_MAX_PENDING_EVENTS_PER_KEY,
                DEFAULT_MAX_PENDING_EVENT_AGE_MS);
    }

    DecisionBroadcastProcessFunction(MapStateDescriptor<String, SceneReleaseTimeline> snapshotStateDescriptor,
                                     EngineLookupServiceFactory lookupServiceFactory,
                                     EngineStreamFeatureStateStoreFactory stateStoreFactory,
                                     long pendingRetryDelayMs,
                                     int maxPendingEventsPerKey,
                                     long maxPendingEventAgeMs) {
        this.snapshotStateDescriptor = snapshotStateDescriptor;
        this.lookupServiceFactory = lookupServiceFactory;
        this.stateStoreFactory = stateStoreFactory;
        this.pendingRetryDelayMs = pendingRetryDelayMs > 0L ? pendingRetryDelayMs : DEFAULT_PENDING_RETRY_DELAY_MS;
        this.maxPendingEventsPerKey = maxPendingEventsPerKey > 0 ? maxPendingEventsPerKey : DEFAULT_MAX_PENDING_EVENTS_PER_KEY;
        this.maxPendingEventAgeMs = maxPendingEventAgeMs > 0L ? maxPendingEventAgeMs : DEFAULT_MAX_PENDING_EVENT_AGE_MS;
    }

    @Override
    public void open(Configuration parameters) {
        RuntimeContext runtimeContext = getRuntimeContext();
        this.runtimeManager = new SceneRuntimeManager(new RuntimeCompiler(new DefaultScriptCompiler()));
        this.runtimeCache = new SceneRuntimeCache();
        this.stateStore = stateStoreFactory.create(runtimeContext);
        this.decisionExecutor = new DecisionExecutor();
        this.lookupService = lookupServiceFactory.create();
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
                                        Collector<DecisionResult> collector) throws Exception {
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
            emitEngineError(context, snapshotError("snapshot-activate",
                    EngineErrorCodes.SNAPSHOT_ACTIVATE_FAILED,
                    envelope,
                    exception));
        }
    }

    @Override
    public void processElement(RiskEvent event,
                               ReadOnlyContext context,
                               Collector<DecisionResult> collector) throws Exception {
        metrics.onInputEvent();
        String sceneKey = event.getSceneCode();
        long processingTimeMs = context.timerService().currentProcessingTime();
        CompiledSceneRuntime runtime = resolveRuntime(event.getSceneCode(),
                Instant.ofEpochMilli(processingTimeMs),
                context.getBroadcastState(snapshotStateDescriptor)).orElse(null);
        if (runtime == null) {
            metrics.onNoSnapshot();
            dropExpiredPendingEventsIfNeeded(processingTimeMs, context);
            boolean buffered = bufferPendingEvent(event, processingTimeMs, context);
            if (buffered || hasPendingEvents()) {
                schedulePendingRetry(sceneKey, context.timerService());
            }
            return;
        }
        dropExpiredPendingEventsIfNeeded(processingTimeMs, context);
        clearPendingRetryTimer();
        OutputContext outputContext = new OutputContext() {
            @Override
            public void emitDecisionLog(DecisionLogRecord record) {
                context.output(EngineOutputTags.DECISION_LOG, record);
                metrics.onDecisionLogEmitted();
            }

            @Override
            public void emitEngineError(EngineErrorRecord record) {
                context.output(EngineOutputTags.ENGINE_ERROR, record);
                metrics.onEngineError(record);
            }
        };
        stateStore.bindExecutionContext(new FlinkExecutionContext(context.timerService(), context.currentWatermark()));
        try {
            flushPendingEvents(sceneKey, runtime, collector, outputContext);
            emitDecision(event, runtime, collector, outputContext);
        } finally {
            stateStore.clearExecutionContext();
        }
    }

    @Override
    public void onTimer(long timestamp,
                        OnTimerContext context,
                        Collector<DecisionResult> collector) throws Exception {
        String sceneKey = context.getCurrentKey();
        if (isPendingRetryTimer(timestamp, context)) {
            clearPendingRetryTimer();
            if (dropExpiredPendingEventsIfNeeded(timestamp, context)) {
                return;
            }
            CompiledSceneRuntime runtime = resolveRuntime(sceneKey,
                    Instant.ofEpochMilli(timestamp),
                    context.getBroadcastState(snapshotStateDescriptor)).orElse(null);
            if (runtime == null) {
                if (hasPendingEvents()) {
                    schedulePendingRetry(sceneKey, context.timerService());
                }
                return;
            }
            OutputContext outputContext = new OutputContext() {
                @Override
                public void emitDecisionLog(DecisionLogRecord record) {
                    context.output(EngineOutputTags.DECISION_LOG, record);
                    metrics.onDecisionLogEmitted();
                }

                @Override
                public void emitEngineError(EngineErrorRecord record) {
                    context.output(EngineOutputTags.ENGINE_ERROR, record);
                    metrics.onEngineError(record);
                }
            };
            stateStore.bindExecutionContext(new FlinkExecutionContext(context.timerService(), context.currentWatermark()));
            try {
                flushPendingEvents(sceneKey, runtime, collector, outputContext);
            } finally {
                stateStore.clearExecutionContext();
            }
            return;
        }
        if (context.timeDomain() != TimeDomain.EVENT_TIME) {
            return;
        }
        try {
            stateStore.onTimer(timestamp);
        } catch (Exception exception) {
            emitEngineError(context, EngineErrorRecord.of("stream-feature-timer",
                    EngineErrorTypes.STATE,
                    EngineErrorCodes.STATE_TIMER_CLEANUP_FAILED,
                    null,
                    null,
                    exception));
        }
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

    @Override
    public TypeInformation<DecisionResult> getProducedType() {
        return EngineTypeInfos.decisionResult();
    }

    private boolean bufferPendingEvent(RiskEvent event,
                                      long processingTimeMs,
                                      ReadOnlyContext context) throws Exception {
        int currentPendingCount = pendingEventCount();
        if (currentPendingCount >= maxPendingEventsPerKey) {
            metrics.onPendingDropped(1);
            metrics.onPendingOldestAgeObserved(pendingOldestAgeMs(processingTimeMs));
            emitEngineError(context, pendingError("pending-buffer-overflow",
                    EngineErrorCodes.PENDING_BUFFER_OVERFLOW,
                    event,
                    new IllegalStateException("pending buffer overflow: sceneCode=" + event.getSceneCode()
                            + ", limit=" + maxPendingEventsPerKey
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
        long nextRetryAt = timerService.currentProcessingTime() + pendingRetryDelayMs;
        pendingRetryAtState.update(nextRetryAt);
        timerService.registerProcessingTimeTimer(nextRetryAt);
    }

    private void clearPendingRetryTimer() throws Exception {
        pendingRetryAtState.clear();
    }

    private void flushPendingEvents(String sceneKey,
                                    CompiledSceneRuntime runtime,
                                    Collector<DecisionResult> collector,
                                    OutputContext outputContext) throws Exception {
        List<RiskEvent> events = pendingEventsSnapshot();
        clearPendingEventState();
        metrics.onPendingOldestAgeObserved(0L);
        if (events.isEmpty()) {
            return;
        }
        metrics.onPendingFlushed(events.size());
        for (RiskEvent event : events) {
            emitDecision(event, runtime, collector, outputContext);
        }
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

    private boolean dropExpiredPendingEventsIfNeeded(long processingTimeMs, ReadOnlyContext context) throws Exception {
        if (!hasPendingEvents()) {
            metrics.onPendingOldestAgeObserved(0L);
            return false;
        }
        long oldestAgeMs = pendingOldestAgeMs(processingTimeMs);
        metrics.onPendingOldestAgeObserved(oldestAgeMs);
        if (oldestAgeMs <= maxPendingEventAgeMs) {
            return false;
        }
        int droppedCount = pendingEventCount();
        RiskEvent representativeEvent = firstPendingEvent();
        clearPendingEventState();
        clearPendingRetryTimer();
        metrics.onPendingDropped(droppedCount);
        metrics.onPendingOldestAgeObserved(0L);
        emitEngineError(context, pendingError("pending-buffer-timeout",
                EngineErrorCodes.PENDING_BUFFER_EXPIRED,
                representativeEvent,
                new IllegalStateException("pending buffer expired: sceneCode="
                        + (representativeEvent == null ? null : representativeEvent.getSceneCode())
                        + ", oldestAgeMs=" + oldestAgeMs
                        + ", limitMs=" + maxPendingEventAgeMs
                        + ", droppedCount=" + droppedCount)));
        return true;
    }

    private boolean dropExpiredPendingEventsIfNeeded(long processingTimeMs, OnTimerContext context) throws Exception {
        if (!hasPendingEvents()) {
            metrics.onPendingOldestAgeObserved(0L);
            return false;
        }
        long oldestAgeMs = pendingOldestAgeMs(processingTimeMs);
        metrics.onPendingOldestAgeObserved(oldestAgeMs);
        if (oldestAgeMs <= maxPendingEventAgeMs) {
            return false;
        }
        int droppedCount = pendingEventCount();
        RiskEvent representativeEvent = firstPendingEvent();
        clearPendingEventState();
        clearPendingRetryTimer();
        metrics.onPendingDropped(droppedCount);
        metrics.onPendingOldestAgeObserved(0L);
        emitEngineError(context, pendingError("pending-buffer-timeout",
                EngineErrorCodes.PENDING_BUFFER_EXPIRED,
                representativeEvent,
                new IllegalStateException("pending buffer expired: sceneCode="
                        + (representativeEvent == null ? null : representativeEvent.getSceneCode())
                        + ", oldestAgeMs=" + oldestAgeMs
                        + ", limitMs=" + maxPendingEventAgeMs
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

    private void emitDecision(RiskEvent event,
                              CompiledSceneRuntime runtime,
                              Collector<DecisionResult> collector,
                              OutputContext outputContext) {
        try {
            DecisionExecutor.PreparedDecisionContext preparedContext = decisionExecutor.prepare(runtime,
                    event,
                    stateStore);
            DecisionResult result = decisionExecutor.executePrepared(runtime,
                    preparedContext,
                    lookupService,
                    outputContext::emitEngineError);
            try {
                collector.collect(result);
                metrics.onDecisionResult(result);
            } catch (Exception exception) {
                outputContext.emitEngineError(outputError("decision-collect",
                        EngineErrorCodes.DECISION_RESULT_EMIT_FAILED,
                        event,
                        runtime,
                        exception));
                return;
            }
            try {
                outputContext.emitDecisionLog(DecisionLogRecord.from(result, runtime.needFullDecisionLog()));
            } catch (Exception exception) {
                outputContext.emitEngineError(outputError("decision-log",
                        EngineErrorCodes.DECISION_LOG_EMIT_FAILED,
                        event,
                        runtime,
                        exception));
            }
        } catch (Exception exception) {
            outputContext.emitEngineError(decisionError(event, runtime, exception));
        }
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

    private EngineErrorRecord decisionError(RiskEvent event,
                                            CompiledSceneRuntime runtime,
                                            Throwable throwable) {
        if (throwable instanceof DecisionExecutionException executionException) {
            EngineErrorRecord record = EngineErrorRecord.of(executionException.getStage(),
                    executionException.getErrorType(),
                    executionException.getErrorCode(),
                    event,
                    runtime == null ? null : runtime.version(),
                    throwable);
            populateRuntimeDetails(record, runtime);
            record.setFeatureCode(executionException.getFeatureCode());
            record.setRuleCode(executionException.getRuleCode());
            record.setEngineType(executionException.getEngineType() == null ? null : executionException.getEngineType().name());
            return record;
        }
        EngineErrorRecord record = EngineErrorRecord.of("decision-execute",
                EngineErrorTypes.EXECUTION,
                EngineErrorCodes.DECISION_EXECUTION_FAILED,
                event,
                runtime == null ? null : runtime.version(),
                throwable);
        populateRuntimeDetails(record, runtime);
        return record;
    }

    private EngineErrorRecord outputError(String stage,
                                          String errorCode,
                                          RiskEvent event,
                                          CompiledSceneRuntime runtime,
                                          Throwable throwable) {
        EngineErrorRecord record = EngineErrorRecord.of(stage,
                EngineErrorTypes.OUTPUT,
                errorCode,
                event,
                runtime == null ? null : runtime.version(),
                throwable);
        populateRuntimeDetails(record, runtime);
        return record;
    }

    private void populateRuntimeDetails(EngineErrorRecord record, CompiledSceneRuntime runtime) {
        if (record == null || runtime == null || runtime.getSnapshot() == null) {
            return;
        }
        record.setSnapshotId(runtime.getSnapshot().getSnapshotId());
        record.setSnapshotChecksum(runtime.getSnapshot().getChecksum());
    }

    private void emitEngineError(Context context, EngineErrorRecord record) {
        context.output(EngineOutputTags.ENGINE_ERROR, record);
        metrics.onEngineError(record);
    }

    private void emitEngineError(ReadOnlyContext context, EngineErrorRecord record) {
        context.output(EngineOutputTags.ENGINE_ERROR, record);
        metrics.onEngineError(record);
    }

    private void emitEngineError(OnTimerContext context, EngineErrorRecord record) {
        context.output(EngineOutputTags.ENGINE_ERROR, record);
        metrics.onEngineError(record);
    }

    @FunctionalInterface
    @Deprecated(forRemoval = true)
    public interface LookupServiceFactory extends EngineLookupServiceFactory {

        LookupService create();

    }

    @FunctionalInterface
    @Deprecated(forRemoval = true)
    public interface StreamFeatureStateStoreFactory extends EngineStreamFeatureStateStoreFactory {

        StreamFeatureStateStore create(RuntimeContext runtimeContext);

    }

    private interface OutputContext {

        void emitDecisionLog(DecisionLogRecord record);

        void emitEngineError(EngineErrorRecord record);

    }

    private record FlinkExecutionContext(TimerService timerService,
                                         long currentWatermark) implements StreamFeatureStateStore.StreamFeatureExecutionContext {

        @Override
        public void registerEventTimeTimer(long timestamp) {
            timerService.registerEventTimeTimer(timestamp);
        }

    }

}
