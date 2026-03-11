package cn.liboshuai.pulsix.engine.flink;

import cn.liboshuai.pulsix.engine.core.DecisionExecutor;
import cn.liboshuai.pulsix.engine.feature.FlinkKeyedStateStreamFeatureStateStore;
import cn.liboshuai.pulsix.engine.feature.InMemoryLookupService;
import cn.liboshuai.pulsix.engine.feature.LookupService;
import cn.liboshuai.pulsix.engine.feature.StreamFeatureStateStore;
import cn.liboshuai.pulsix.engine.flink.runtime.SceneRuntimeCache;
import cn.liboshuai.pulsix.engine.flink.typeinfo.EngineTypeInfos;
import cn.liboshuai.pulsix.engine.model.DecisionLogRecord;
import cn.liboshuai.pulsix.engine.model.DecisionResult;
import cn.liboshuai.pulsix.engine.model.EngineErrorRecord;
import cn.liboshuai.pulsix.engine.model.PublishType;
import cn.liboshuai.pulsix.engine.model.RiskEvent;
import cn.liboshuai.pulsix.engine.model.SceneSnapshotEnvelope;
import cn.liboshuai.pulsix.engine.runtime.CompiledSceneRuntime;
import cn.liboshuai.pulsix.engine.runtime.RuntimeCompiler;
import cn.liboshuai.pulsix.engine.runtime.SceneRuntimeManager;
import cn.liboshuai.pulsix.engine.script.DefaultScriptCompiler;
import cn.liboshuai.pulsix.engine.snapshot.SceneSnapshotEnvelopes;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.api.common.state.BroadcastState;
import org.apache.flink.api.java.typeutils.ResultTypeQueryable;
import org.apache.flink.api.common.state.MapStateDescriptor;
import org.apache.flink.api.common.state.ReadOnlyBroadcastState;
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

    private static final String PENDING_STATE_SUFFIX = "#pending";

    private static final long PENDING_RETRY_DELAY_MS = 1_000L;

    private final MapStateDescriptor<String, SceneSnapshotEnvelope> snapshotStateDescriptor;

    private final LookupServiceFactory lookupServiceFactory;

    private transient LookupService lookupService;

    private transient SceneRuntimeManager runtimeManager;

    private transient SceneRuntimeCache runtimeCache;

    private transient StreamFeatureStateStore stateStore;

    private transient DecisionExecutor decisionExecutor;

    private transient Map<String, List<RiskEvent>> pendingEvents;

    private transient Map<String, Long> pendingRetryTimers;

    public DecisionBroadcastProcessFunction(MapStateDescriptor<String, SceneSnapshotEnvelope> snapshotStateDescriptor) {
        this(snapshotStateDescriptor, InMemoryLookupService::demo);
    }

    public DecisionBroadcastProcessFunction(MapStateDescriptor<String, SceneSnapshotEnvelope> snapshotStateDescriptor,
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
            BroadcastState<String, SceneSnapshotEnvelope> broadcastState = context.getBroadcastState(snapshotStateDescriptor);
            promotePendingIfEffective(normalizedEnvelope.getSceneCode(), broadcastState,
                    Instant.ofEpochMilli(context.currentProcessingTime()));
            SceneSnapshotEnvelope currentEnvelope = activeEnvelopeOf(broadcastState, normalizedEnvelope.getSceneCode());
            SceneSnapshotEnvelope pendingEnvelope = pendingEnvelopeOf(broadcastState, normalizedEnvelope.getSceneCode());
            if (shouldIgnoreEnvelope(currentEnvelope, pendingEnvelope, normalizedEnvelope, context)) {
                return;
            }
            CompiledSceneRuntime runtime = runtimeManager.compile(normalizedEnvelope.getSnapshot());
            runtimeCache.put(runtime);
            if (isEffectiveAt(normalizedEnvelope, Instant.ofEpochMilli(context.currentProcessingTime()))) {
                activateEnvelope(broadcastState, normalizedEnvelope, runtime);
            } else {
                broadcastState.put(pendingStateKey(normalizedEnvelope.getSceneCode()), normalizedEnvelope);
            }
        } catch (Exception exception) {
            context.output(EngineOutputTags.ENGINE_ERROR, snapshotError("snapshot-activate", envelope, exception));
        }
    }

    @Override
    public void processElement(RiskEvent event,
                               ReadOnlyContext context,
                               Collector<DecisionResult> collector) throws Exception {
        CompiledSceneRuntime runtime = resolveRuntime(event.getSceneCode(),
                Instant.ofEpochMilli(context.timerService().currentProcessingTime()),
                context.getBroadcastState(snapshotStateDescriptor)).orElse(null);
        if (runtime == null) {
            bufferPendingEvent(event);
            schedulePendingRetry(event.getSceneCode(), context.timerService());
            return;
        }
        pendingRetryTimers.remove(event.getSceneCode());
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
            flushPendingEvents(event.getSceneCode(), runtime, collector, outputContext);
            emitDecision(event, runtime, collector, outputContext);
        } finally {
            stateStore.clearExecutionContext();
        }
    }

    @Override
    public void onTimer(long timestamp,
                        OnTimerContext context,
                        Collector<DecisionResult> collector) throws Exception {
        String sceneCode = context.getCurrentKey();
        if (isPendingRetryTimer(sceneCode, timestamp, context)) {
            pendingRetryTimers.remove(sceneCode);
            CompiledSceneRuntime runtime = resolveRuntime(sceneCode,
                    Instant.ofEpochMilli(timestamp),
                    context.getBroadcastState(snapshotStateDescriptor)).orElse(null);
            if (runtime == null) {
                if (hasPendingEvents(sceneCode)) {
                    schedulePendingRetry(sceneCode, context.timerService());
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
                flushPendingEvents(sceneCode, runtime, collector, outputContext);
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
                                                          ReadOnlyBroadcastState<String, SceneSnapshotEnvelope> broadcastState) throws Exception {
        SceneSnapshotEnvelope envelope = effectiveEnvelopeOf(broadcastState, sceneCode, referenceTime);
        if (envelope == null || envelope.getSnapshot() == null) {
            return Optional.empty();
        }
        Optional<CompiledSceneRuntime> activeRuntime = runtimeManager.getActiveRuntime(sceneCode);
        if (activeRuntime.isPresent() && Objects.equals(activeRuntime.get().version(), envelope.getVersion())) {
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

    private void bufferPendingEvent(RiskEvent event) {
        pendingEvents.computeIfAbsent(event.getSceneCode(), ignored -> new ArrayList<>()).add(event);
    }

    private boolean hasPendingEvents(String sceneCode) {
        List<RiskEvent> events = pendingEvents.get(sceneCode);
        return events != null && !events.isEmpty();
    }

    private void schedulePendingRetry(String sceneCode, TimerService timerService) {
        if (sceneCode == null || timerService == null) {
            return;
        }
        long nextRetryAt = timerService.currentProcessingTime() + PENDING_RETRY_DELAY_MS;
        Long existingRetryAt = pendingRetryTimers.get(sceneCode);
        if (existingRetryAt != null && existingRetryAt >= nextRetryAt) {
            return;
        }
        pendingRetryTimers.put(sceneCode, nextRetryAt);
        timerService.registerProcessingTimeTimer(nextRetryAt);
    }

    private void flushPendingEvents(String sceneCode,
                                    CompiledSceneRuntime runtime,
                                    Collector<DecisionResult> collector,
                                    OutputContext outputContext) {
        List<RiskEvent> events = pendingEvents.remove(sceneCode);
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

    private boolean shouldIgnoreEnvelope(SceneSnapshotEnvelope currentEnvelope,
                                         SceneSnapshotEnvelope pendingEnvelope,
                                         SceneSnapshotEnvelope incomingEnvelope,
                                         Context context) {
        int incomingVersion = Optional.ofNullable(incomingEnvelope.getVersion()).orElse(0);
        SceneSnapshotEnvelope sameVersionEnvelope = sameVersionEnvelope(currentEnvelope, pendingEnvelope, incomingVersion);
        if (sameVersionEnvelope != null) {
            if (Objects.equals(sameVersionEnvelope.getChecksum(), incomingEnvelope.getChecksum())) {
                return true;
            }
            context.output(EngineOutputTags.ENGINE_ERROR,
                    snapshotError("snapshot-version-conflict", incomingEnvelope,
                            new IllegalStateException("same version but different checksum")));
            return true;
        }
        int latestVersion = Math.max(versionOf(currentEnvelope), versionOf(pendingEnvelope));
        if (incomingVersion < latestVersion && !isRollbackEnvelope(incomingEnvelope)) {
            return true;
        }
        return false;
    }

    private void activateEnvelope(BroadcastState<String, SceneSnapshotEnvelope> broadcastState,
                                  SceneSnapshotEnvelope envelope,
                                  CompiledSceneRuntime runtime) throws Exception {
        broadcastState.put(activeStateKey(envelope.getSceneCode()), envelope);
        SceneSnapshotEnvelope pendingEnvelope = pendingEnvelopeOf(broadcastState, envelope.getSceneCode());
        if (pendingEnvelope != null && versionOf(pendingEnvelope) <= versionOf(envelope)) {
            broadcastState.remove(pendingStateKey(envelope.getSceneCode()));
        }
        runtimeManager.activate(runtime);
    }

    private void promotePendingIfEffective(String sceneCode,
                                           BroadcastState<String, SceneSnapshotEnvelope> broadcastState,
                                           Instant referenceTime) throws Exception {
        SceneSnapshotEnvelope pendingEnvelope = pendingEnvelopeOf(broadcastState, sceneCode);
        if (pendingEnvelope == null || !isEffectiveAt(pendingEnvelope, referenceTime)) {
            return;
        }
        SceneSnapshotEnvelope activeEnvelope = activeEnvelopeOf(broadcastState, sceneCode);
        if (!shouldPreferPendingOverActive(activeEnvelope, pendingEnvelope)) {
            broadcastState.remove(pendingStateKey(sceneCode));
            return;
        }
        CompiledSceneRuntime runtime = runtimeCache.get(sceneCode, pendingEnvelope.getVersion())
                .orElseGet(() -> {
                    CompiledSceneRuntime compiledRuntime = runtimeManager.compile(pendingEnvelope.getSnapshot());
                    runtimeCache.put(compiledRuntime);
                    return compiledRuntime;
                });
        activateEnvelope(broadcastState, pendingEnvelope, runtime);
    }

    private SceneSnapshotEnvelope effectiveEnvelopeOf(ReadOnlyBroadcastState<String, SceneSnapshotEnvelope> broadcastState,
                                                      String sceneCode,
                                                      Instant referenceTime) throws Exception {
        SceneSnapshotEnvelope activeEnvelope = activeEnvelopeOf(broadcastState, sceneCode);
        SceneSnapshotEnvelope pendingEnvelope = pendingEnvelopeOf(broadcastState, sceneCode);
        if (pendingEnvelope != null
                && isEffectiveAt(pendingEnvelope, referenceTime)
                && shouldPreferPendingOverActive(activeEnvelope, pendingEnvelope)) {
            return pendingEnvelope;
        }
        return activeEnvelope;
    }

    private SceneSnapshotEnvelope activeEnvelopeOf(ReadOnlyBroadcastState<String, SceneSnapshotEnvelope> broadcastState,
                                                   String sceneCode) throws Exception {
        return broadcastState == null || sceneCode == null ? null : broadcastState.get(activeStateKey(sceneCode));
    }

    private SceneSnapshotEnvelope pendingEnvelopeOf(ReadOnlyBroadcastState<String, SceneSnapshotEnvelope> broadcastState,
                                                    String sceneCode) throws Exception {
        return broadcastState == null || sceneCode == null ? null : broadcastState.get(pendingStateKey(sceneCode));
    }

    private SceneSnapshotEnvelope activeEnvelopeOf(BroadcastState<String, SceneSnapshotEnvelope> broadcastState,
                                                   String sceneCode) throws Exception {
        return broadcastState == null || sceneCode == null ? null : broadcastState.get(activeStateKey(sceneCode));
    }

    private SceneSnapshotEnvelope pendingEnvelopeOf(BroadcastState<String, SceneSnapshotEnvelope> broadcastState,
                                                    String sceneCode) throws Exception {
        return broadcastState == null || sceneCode == null ? null : broadcastState.get(pendingStateKey(sceneCode));
    }

    private SceneSnapshotEnvelope sameVersionEnvelope(SceneSnapshotEnvelope currentEnvelope,
                                                      SceneSnapshotEnvelope pendingEnvelope,
                                                      int incomingVersion) {
        if (versionOf(currentEnvelope) == incomingVersion) {
            return currentEnvelope;
        }
        if (versionOf(pendingEnvelope) == incomingVersion) {
            return pendingEnvelope;
        }
        return null;
    }

    private int versionOf(SceneSnapshotEnvelope envelope) {
        return envelope == null || envelope.getVersion() == null ? 0 : envelope.getVersion();
    }

    private boolean shouldPreferPendingOverActive(SceneSnapshotEnvelope activeEnvelope,
                                                  SceneSnapshotEnvelope pendingEnvelope) {
        if (pendingEnvelope == null) {
            return false;
        }
        if (isRollbackEnvelope(pendingEnvelope)) {
            return true;
        }
        return versionOf(pendingEnvelope) > versionOf(activeEnvelope);
    }

    private boolean isRollbackEnvelope(SceneSnapshotEnvelope envelope) {
        return envelope != null && envelope.getPublishType() == PublishType.ROLLBACK;
    }

    private boolean isEffectiveAt(SceneSnapshotEnvelope envelope, Instant referenceTime) {
        if (envelope == null || envelope.getEffectiveFrom() == null) {
            return true;
        }
        Instant effectiveFrom = envelope.getEffectiveFrom();
        Instant effectiveReference = referenceTime == null ? Instant.now() : referenceTime;
        return !effectiveFrom.isAfter(effectiveReference);
    }

    private boolean isPendingRetryTimer(String sceneCode, long timestamp, OnTimerContext context) {
        if (sceneCode == null) {
            return false;
        }
        Long expectedRetryAt = pendingRetryTimers.get(sceneCode);
        if (expectedRetryAt == null || expectedRetryAt != timestamp) {
            return false;
        }
        return context.timeDomain() == TimeDomain.PROCESSING_TIME;
    }

    private String activeStateKey(String sceneCode) {
        return sceneCode;
    }

    private String pendingStateKey(String sceneCode) {
        return sceneCode + PENDING_STATE_SUFFIX;
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
