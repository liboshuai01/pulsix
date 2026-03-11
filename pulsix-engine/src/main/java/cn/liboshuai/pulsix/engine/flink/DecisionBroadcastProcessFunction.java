package cn.liboshuai.pulsix.engine.flink;

import cn.liboshuai.pulsix.engine.core.DecisionExecutor;
import cn.liboshuai.pulsix.engine.feature.FlinkKeyedStateStreamFeatureStateStore;
import cn.liboshuai.pulsix.engine.feature.InMemoryLookupService;
import cn.liboshuai.pulsix.engine.feature.LookupService;
import cn.liboshuai.pulsix.engine.feature.StreamFeatureStateStore;
import cn.liboshuai.pulsix.engine.flink.runtime.SceneReleaseTimeline;
import cn.liboshuai.pulsix.engine.flink.runtime.SceneRuntimeCache;
import cn.liboshuai.pulsix.engine.flink.typeinfo.EngineTypeInfos;
import cn.liboshuai.pulsix.engine.model.DecisionLogRecord;
import cn.liboshuai.pulsix.engine.model.DecisionResult;
import cn.liboshuai.pulsix.engine.model.EngineErrorRecord;
import cn.liboshuai.pulsix.engine.model.RiskEvent;
import cn.liboshuai.pulsix.engine.model.SceneSnapshotEnvelope;
import cn.liboshuai.pulsix.engine.runtime.CompiledSceneRuntime;
import cn.liboshuai.pulsix.engine.runtime.RuntimeCompiler;
import cn.liboshuai.pulsix.engine.runtime.SceneRuntimeManager;
import cn.liboshuai.pulsix.engine.script.DefaultScriptCompiler;
import cn.liboshuai.pulsix.engine.snapshot.SceneSnapshotEnvelopes;
import org.apache.flink.api.common.state.MapStateDescriptor;
import org.apache.flink.api.common.state.ReadOnlyBroadcastState;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.api.java.typeutils.ResultTypeQueryable;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.streaming.api.TimeDomain;
import org.apache.flink.streaming.api.TimerService;
import org.apache.flink.streaming.api.functions.co.KeyedBroadcastProcessFunction;
import org.apache.flink.util.Collector;

import java.io.Serializable;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public class DecisionBroadcastProcessFunction
        extends KeyedBroadcastProcessFunction<String, RiskEvent, SceneSnapshotEnvelope, DecisionResult>
        implements ResultTypeQueryable<DecisionResult> {

    private static final long PENDING_RETRY_DELAY_MS = 1_000L;

    private final MapStateDescriptor<String, SceneReleaseTimeline> snapshotStateDescriptor;

    private final LookupServiceFactory lookupServiceFactory;

    private transient LookupService lookupService;

    private transient SceneRuntimeManager runtimeManager;

    private transient SceneRuntimeCache runtimeCache;

    private transient StreamFeatureStateStore stateStore;

    private transient DecisionExecutor decisionExecutor;

    private transient Map<String, List<RiskEvent>> pendingEvents;

    private transient Map<String, Long> pendingRetryTimers;

    public DecisionBroadcastProcessFunction(MapStateDescriptor<String, SceneReleaseTimeline> snapshotStateDescriptor) {
        this(snapshotStateDescriptor, InMemoryLookupService::demo);
    }

    public DecisionBroadcastProcessFunction(MapStateDescriptor<String, SceneReleaseTimeline> snapshotStateDescriptor,
                                            LookupServiceFactory lookupServiceFactory) {
        this.snapshotStateDescriptor = snapshotStateDescriptor;
        this.lookupServiceFactory = lookupServiceFactory;
    }

    @Override
    public void open(Configuration parameters) {
        this.runtimeManager = new SceneRuntimeManager(new RuntimeCompiler(new DefaultScriptCompiler()));
        this.runtimeCache = new SceneRuntimeCache();
        this.stateStore = new FlinkKeyedStateStreamFeatureStateStore(getRuntimeContext());
        this.decisionExecutor = new DecisionExecutor();
        this.lookupService = lookupServiceFactory.create();
        this.pendingEvents = new LinkedHashMap<>();
        this.pendingRetryTimers = new LinkedHashMap<>();
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
                context.output(EngineOutputTags.ENGINE_ERROR,
                        snapshotError("snapshot-version-conflict", normalizedEnvelope,
                                new IllegalStateException("same version but different checksum")));
                return;
            }
            CompiledSceneRuntime runtime = runtimeManager.compile(normalizedEnvelope.getSnapshot());
            runtimeCache.put(runtime);
            if (!timeline.contains(normalizedEnvelope)) {
                timeline.add(normalizedEnvelope);
                broadcastState.put(normalizedEnvelope.getSceneCode(), timeline);
            }
            activateEffectiveRuntime(normalizedEnvelope.getSceneCode(), timeline,
                    Instant.ofEpochMilli(context.currentProcessingTime()));
        } catch (Exception exception) {
            context.output(EngineOutputTags.ENGINE_ERROR, snapshotError("snapshot-activate", envelope, exception));
        }
    }

    @Override
    public void processElement(RiskEvent event,
                               ReadOnlyContext context,
                               Collector<DecisionResult> collector) throws Exception {
        String routeKey = event.routeKey();
        CompiledSceneRuntime runtime = resolveRuntime(event.getSceneCode(),
                Instant.ofEpochMilli(context.timerService().currentProcessingTime()),
                context.getBroadcastState(snapshotStateDescriptor)).orElse(null);
        if (runtime == null) {
            bufferPendingEvent(routeKey, event);
            schedulePendingRetry(routeKey, context.timerService());
            return;
        }
        pendingRetryTimers.remove(routeKey);
        OutputContext outputContext = new OutputContext() {
            @Override
            public void emitDecisionLog(DecisionLogRecord record) {
                context.output(EngineOutputTags.DECISION_LOG, record);
            }

            @Override
            public void emitEngineError(EngineErrorRecord record) {
                context.output(EngineOutputTags.ENGINE_ERROR, record);
            }
        };
        stateStore.bindExecutionContext(new FlinkExecutionContext(context.timerService(), context.currentWatermark()));
        try {
            flushPendingEvents(routeKey, runtime, collector, outputContext);
            emitDecision(event, runtime, collector, outputContext);
        } finally {
            stateStore.clearExecutionContext();
        }
    }

    @Override
    public void onTimer(long timestamp,
                        OnTimerContext context,
                        Collector<DecisionResult> collector) throws Exception {
        String routeKey = context.getCurrentKey();
        if (isPendingRetryTimer(routeKey, timestamp, context)) {
            pendingRetryTimers.remove(routeKey);
            String sceneCode = pendingSceneCode(routeKey);
            CompiledSceneRuntime runtime = resolveRuntime(sceneCode,
                    Instant.ofEpochMilli(timestamp),
                    context.getBroadcastState(snapshotStateDescriptor)).orElse(null);
            if (runtime == null) {
                if (hasPendingEvents(routeKey)) {
                    schedulePendingRetry(routeKey, context.timerService());
                }
                return;
            }
            OutputContext outputContext = new OutputContext() {
                @Override
                public void emitDecisionLog(DecisionLogRecord record) {
                    context.output(EngineOutputTags.DECISION_LOG, record);
                }

                @Override
                public void emitEngineError(EngineErrorRecord record) {
                    context.output(EngineOutputTags.ENGINE_ERROR, record);
                }
            };
            stateStore.bindExecutionContext(new FlinkExecutionContext(context.timerService(), context.currentWatermark()));
            try {
                flushPendingEvents(routeKey, runtime, collector, outputContext);
            } finally {
                stateStore.clearExecutionContext();
            }
            return;
        }
        try {
            stateStore.onTimer(timestamp);
        } catch (Exception exception) {
            context.output(EngineOutputTags.ENGINE_ERROR, snapshotError("stream-feature-timer", null, exception));
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

    private void bufferPendingEvent(String routeKey, RiskEvent event) {
        pendingEvents.computeIfAbsent(routeKey, ignored -> new ArrayList<>()).add(event);
    }

    private boolean hasPendingEvents(String routeKey) {
        List<RiskEvent> events = pendingEvents.get(routeKey);
        return events != null && !events.isEmpty();
    }

    private String pendingSceneCode(String routeKey) {
        List<RiskEvent> events = pendingEvents.get(routeKey);
        if (events == null || events.isEmpty()) {
            return null;
        }
        return events.get(0).getSceneCode();
    }

    private void schedulePendingRetry(String routeKey, TimerService timerService) {
        if (routeKey == null || timerService == null) {
            return;
        }
        long nextRetryAt = timerService.currentProcessingTime() + PENDING_RETRY_DELAY_MS;
        Long existingRetryAt = pendingRetryTimers.get(routeKey);
        if (existingRetryAt != null && existingRetryAt >= nextRetryAt) {
            return;
        }
        pendingRetryTimers.put(routeKey, nextRetryAt);
        timerService.registerProcessingTimeTimer(nextRetryAt);
    }

    private void flushPendingEvents(String routeKey,
                                    CompiledSceneRuntime runtime,
                                    Collector<DecisionResult> collector,
                                    OutputContext outputContext) {
        List<RiskEvent> events = pendingEvents.remove(routeKey);
        if (events == null || events.isEmpty()) {
            return;
        }
        for (RiskEvent event : events) {
            emitDecision(event, runtime, collector, outputContext);
        }
    }

    private void emitDecision(RiskEvent event,
                              CompiledSceneRuntime runtime,
                              Collector<DecisionResult> collector,
                              OutputContext outputContext) {
        try {
            DecisionResult result = decisionExecutor.execute(runtime,
                    event,
                    stateStore,
                    lookupService,
                    outputContext::emitEngineError);
            try {
                collector.collect(result);
            } catch (Exception exception) {
                outputContext.emitEngineError(EngineErrorRecord.of("decision-collect", event, runtime.version(), exception));
                return;
            }
            try {
                outputContext.emitDecisionLog(DecisionLogRecord.from(result, runtime.needFullDecisionLog()));
            } catch (Exception exception) {
                outputContext.emitEngineError(EngineErrorRecord.of("decision-log", event, runtime.version(), exception));
            }
        } catch (Exception exception) {
            outputContext.emitEngineError(EngineErrorRecord.of("decision-execute", event, runtime.version(), exception));
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

    private boolean isPendingRetryTimer(String routeKey, long timestamp, OnTimerContext context) {
        if (routeKey == null) {
            return false;
        }
        Long expectedRetryAt = pendingRetryTimers.get(routeKey);
        if (expectedRetryAt == null || expectedRetryAt != timestamp) {
            return false;
        }
        return context.timeDomain() == TimeDomain.PROCESSING_TIME;
    }

    private EngineErrorRecord snapshotError(String stage,
                                            SceneSnapshotEnvelope envelope,
                                            Throwable throwable) {
        EngineErrorRecord record = new EngineErrorRecord();
        record.setStage(stage);
        record.setSceneCode(envelope != null ? envelope.getSceneCode() : null);
        record.setVersion(envelope != null ? envelope.getVersion() : null);
        record.setErrorMessage(throwable != null ? throwable.getMessage() : null);
        record.setOccurredAt(Instant.now());
        return record;
    }

    @FunctionalInterface
    public interface LookupServiceFactory extends Serializable {

        LookupService create();

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
