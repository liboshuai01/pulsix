package cn.liboshuai.pulsix.engine.flink;

import cn.liboshuai.pulsix.engine.core.DecisionExecutor;
import cn.liboshuai.pulsix.engine.feature.FlinkKeyedStateStreamFeatureStateStore;
import cn.liboshuai.pulsix.engine.feature.InMemoryLookupService;
import cn.liboshuai.pulsix.engine.feature.LookupService;
import cn.liboshuai.pulsix.engine.feature.StreamFeatureStateStore;
import cn.liboshuai.pulsix.engine.flink.typeinfo.EngineTypeInfos;
import cn.liboshuai.pulsix.engine.model.DecisionLogRecord;
import cn.liboshuai.pulsix.engine.model.DecisionResult;
import cn.liboshuai.pulsix.engine.model.EngineErrorRecord;
import cn.liboshuai.pulsix.engine.model.RiskEvent;
import cn.liboshuai.pulsix.engine.model.SceneSnapshotEnvelope;
import cn.liboshuai.pulsix.engine.runtime.CompiledSceneRuntime;
import cn.liboshuai.pulsix.engine.runtime.RuntimeCompiler;
import cn.liboshuai.pulsix.engine.runtime.SceneRuntimeCache;
import cn.liboshuai.pulsix.engine.runtime.SceneRuntimeManager;
import cn.liboshuai.pulsix.engine.script.DefaultScriptCompiler;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.api.java.typeutils.ResultTypeQueryable;
import org.apache.flink.api.common.state.MapStateDescriptor;
import org.apache.flink.configuration.Configuration;
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

    private final MapStateDescriptor<String, SceneSnapshotEnvelope> snapshotStateDescriptor;

    private final LookupServiceFactory lookupServiceFactory;

    private transient LookupService lookupService;

    private transient SceneRuntimeManager runtimeManager;

    private transient SceneRuntimeCache runtimeCache;

    private transient StreamFeatureStateStore stateStore;

    private transient DecisionExecutor decisionExecutor;

    private transient Map<String, List<RiskEvent>> pendingEvents;

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
    }

    @Override
    public void processBroadcastElement(SceneSnapshotEnvelope envelope,
                                        Context context,
                                        Collector<DecisionResult> collector) throws Exception {
        try {
            validateEnvelope(envelope);
            SceneSnapshotEnvelope currentEnvelope = context.getBroadcastState(snapshotStateDescriptor).get(envelope.getSceneCode());
            if (shouldIgnoreEnvelope(currentEnvelope, envelope, context)) {
                return;
            }
            CompiledSceneRuntime runtime = runtimeManager.compile(envelope.getSnapshot());
            context.getBroadcastState(snapshotStateDescriptor).put(envelope.getSceneCode(), envelope);
            runtimeCache.put(runtime);
            runtimeManager.activate(runtime);
        } catch (Exception exception) {
            context.output(EngineOutputTags.ENGINE_ERROR, snapshotError("snapshot-activate", envelope, exception));
        }
    }

    @Override
    public void processElement(RiskEvent event,
                               ReadOnlyContext context,
                               Collector<DecisionResult> collector) throws Exception {
        CompiledSceneRuntime runtime = resolveRuntime(event, context).orElse(null);
        if (runtime == null) {
            bufferPendingEvent(event);
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
        try {
            stateStore.onTimer(timestamp);
        } catch (Exception exception) {
            context.output(EngineOutputTags.ENGINE_ERROR, snapshotError("stream-feature-timer", null, exception));
        }
    }

    private Optional<CompiledSceneRuntime> resolveRuntime(RiskEvent event,
                                                          ReadOnlyContext context) throws Exception {
        SceneSnapshotEnvelope envelope = context.getBroadcastState(snapshotStateDescriptor).get(event.getSceneCode());
        if (envelope == null || envelope.getSnapshot() == null) {
            return Optional.empty();
        }
        Optional<CompiledSceneRuntime> activeRuntime = runtimeManager.getActiveRuntime(event.getSceneCode());
        if (activeRuntime.isPresent() && Objects.equals(activeRuntime.get().version(), envelope.getVersion())) {
            return activeRuntime;
        }
        Optional<CompiledSceneRuntime> cachedRuntime = runtimeCache.get(event.getSceneCode(), envelope.getVersion());
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
            DecisionResult result = decisionExecutor.execute(runtime, event, stateStore, lookupService);
            try {
                collector.collect(result);
            } catch (Exception exception) {
                outputContext.emitEngineError(EngineErrorRecord.of("decision-collect", event, runtime.version(), exception));
                return;
            }
            try {
                outputContext.emitDecisionLog(DecisionLogRecord.from(result));
            } catch (Exception exception) {
                outputContext.emitEngineError(EngineErrorRecord.of("decision-log", event, runtime.version(), exception));
            }
        } catch (Exception exception) {
            outputContext.emitEngineError(EngineErrorRecord.of("decision-execute", event, runtime.version(), exception));
        }
    }

    private void validateEnvelope(SceneSnapshotEnvelope envelope) {
        if (envelope == null) {
            throw new IllegalArgumentException("snapshot envelope must not be null");
        }
        if (envelope.getSceneCode() == null || envelope.getSceneCode().isBlank()) {
            throw new IllegalArgumentException("snapshot sceneCode must not be blank");
        }
        if (envelope.getVersion() == null || envelope.getVersion() <= 0) {
            throw new IllegalArgumentException("snapshot version must be positive");
        }
        if (envelope.getChecksum() == null || envelope.getChecksum().isBlank()) {
            throw new IllegalArgumentException("snapshot checksum must not be blank");
        }
        if (envelope.getSnapshot() == null) {
            throw new IllegalArgumentException("snapshot payload must not be null");
        }
    }

    private boolean shouldIgnoreEnvelope(SceneSnapshotEnvelope currentEnvelope,
                                         SceneSnapshotEnvelope incomingEnvelope,
                                         Context context) {
        if (currentEnvelope == null) {
            return false;
        }
        int currentVersion = Optional.ofNullable(currentEnvelope.getVersion()).orElse(0);
        int incomingVersion = Optional.ofNullable(incomingEnvelope.getVersion()).orElse(0);
        if (incomingVersion < currentVersion) {
            return true;
        }
        if (incomingVersion == currentVersion) {
            if (currentEnvelope.getChecksum() != null && currentEnvelope.getChecksum().equals(incomingEnvelope.getChecksum())) {
                return true;
            }
            context.output(EngineOutputTags.ENGINE_ERROR,
                    snapshotError("snapshot-version-conflict", incomingEnvelope,
                            new IllegalStateException("same version but different checksum")));
            return true;
        }
        return false;
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
