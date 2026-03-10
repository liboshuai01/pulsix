package cn.liboshuai.pulsix.engine.flink;

import cn.liboshuai.pulsix.engine.core.DecisionExecutor;
import cn.liboshuai.pulsix.engine.feature.InMemoryLookupService;
import cn.liboshuai.pulsix.engine.feature.InMemoryStreamFeatureStateStore;
import cn.liboshuai.pulsix.engine.feature.LookupService;
import cn.liboshuai.pulsix.engine.feature.StreamFeatureStateStore;
import cn.liboshuai.pulsix.engine.json.EngineJson;
import cn.liboshuai.pulsix.engine.model.DecisionLogRecord;
import cn.liboshuai.pulsix.engine.model.DecisionResult;
import cn.liboshuai.pulsix.engine.model.EngineErrorRecord;
import cn.liboshuai.pulsix.engine.model.RiskEvent;
import cn.liboshuai.pulsix.engine.model.SceneSnapshotEnvelope;
import cn.liboshuai.pulsix.engine.runtime.CompiledSceneRuntime;
import cn.liboshuai.pulsix.engine.runtime.RuntimeCompiler;
import cn.liboshuai.pulsix.engine.runtime.SceneRuntimeManager;
import cn.liboshuai.pulsix.engine.script.DefaultScriptCompiler;
import org.apache.flink.api.common.state.MapStateDescriptor;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.streaming.api.functions.co.KeyedBroadcastProcessFunction;
import org.apache.flink.util.Collector;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class DecisionBroadcastProcessFunction
        extends KeyedBroadcastProcessFunction<String, Tuple2<String, String>, String, DecisionResult> {

    private final MapStateDescriptor<String, String> snapshotStateDescriptor;

    private final LookupServiceFactory lookupServiceFactory;

    private transient LookupService lookupService;

    private transient SceneRuntimeManager runtimeManager;

    private transient StreamFeatureStateStore stateStore;

    private transient DecisionExecutor decisionExecutor;

    private transient Map<String, List<RiskEvent>> pendingEvents;

    public DecisionBroadcastProcessFunction(MapStateDescriptor<String, String> snapshotStateDescriptor) {
        this(snapshotStateDescriptor, InMemoryLookupService::demo);
    }

    public DecisionBroadcastProcessFunction(MapStateDescriptor<String, String> snapshotStateDescriptor,
                                            LookupServiceFactory lookupServiceFactory) {
        this.snapshotStateDescriptor = snapshotStateDescriptor;
        this.lookupServiceFactory = lookupServiceFactory;
    }

    @Override
    public void open(Configuration parameters) {
        this.runtimeManager = new SceneRuntimeManager(new RuntimeCompiler(new DefaultScriptCompiler()));
        this.stateStore = new InMemoryStreamFeatureStateStore();
        this.decisionExecutor = new DecisionExecutor();
        this.lookupService = lookupServiceFactory.create();
        this.pendingEvents = new LinkedHashMap<>();
    }

    @Override
    public void processBroadcastElement(String envelopeJson,
                                        Context context,
                                        Collector<DecisionResult> collector) throws Exception {
        SceneSnapshotEnvelope envelope = parseEnvelope(envelopeJson);
        context.getBroadcastState(snapshotStateDescriptor).put(envelope.getSceneCode(), envelopeJson);
        if (envelope.getSnapshot() == null) {
            return;
        }
        CompiledSceneRuntime runtime = runtimeManager.activate(envelope.getSnapshot());
        flushPendingEvents(envelope.getSceneCode(), runtime, collector, new OutputContext() {
            @Override
            public void emitDecisionLog(DecisionLogRecord record) {
                context.output(EngineOutputTags.DECISION_LOG, record);
            }

            @Override
            public void emitEngineError(EngineErrorRecord record) {
                context.output(EngineOutputTags.ENGINE_ERROR, record);
            }
        });
    }

    @Override
    public void processElement(Tuple2<String, String> eventRecord,
                               ReadOnlyContext context,
                               Collector<DecisionResult> collector) throws Exception {
        RiskEvent event = parseRiskEvent(eventRecord.f1);
        CompiledSceneRuntime runtime = resolveRuntime(event, context).orElse(null);
        if (runtime == null) {
            bufferPendingEvent(event);
            return;
        }
        emitDecision(event, runtime, collector, new OutputContext() {
            @Override
            public void emitDecisionLog(DecisionLogRecord record) {
                context.output(EngineOutputTags.DECISION_LOG, record);
            }

            @Override
            public void emitEngineError(EngineErrorRecord record) {
                context.output(EngineOutputTags.ENGINE_ERROR, record);
            }
        });
    }

    private Optional<CompiledSceneRuntime> resolveRuntime(RiskEvent event,
                                                          ReadOnlyContext context) throws Exception {
        Optional<CompiledSceneRuntime> runtime = runtimeManager.getActiveRuntime(event.getSceneCode());
        if (runtime.isPresent()) {
            return runtime;
        }
        String envelopeJson = context.getBroadcastState(snapshotStateDescriptor).get(event.getSceneCode());
        if (envelopeJson == null) {
            return Optional.empty();
        }
        SceneSnapshotEnvelope envelope = parseEnvelope(envelopeJson);
        if (envelope.getSnapshot() == null) {
            return Optional.empty();
        }
        return Optional.of(runtimeManager.activate(envelope.getSnapshot()));
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
            collector.collect(result);
            outputContext.emitDecisionLog(DecisionLogRecord.from(result));
        } catch (Exception exception) {
            outputContext.emitEngineError(EngineErrorRecord.of("decision-execute", event, runtime.version(), exception));
        }
    }

    private RiskEvent parseRiskEvent(String eventJson) {
        return EngineJson.read(eventJson, RiskEvent.class);
    }

    private SceneSnapshotEnvelope parseEnvelope(String envelopeJson) {
        return EngineJson.read(envelopeJson, SceneSnapshotEnvelope.class);
    }

    @FunctionalInterface
    public interface LookupServiceFactory extends Serializable {

        LookupService create();

    }

    private interface OutputContext {

        void emitDecisionLog(DecisionLogRecord record);

        void emitEngineError(EngineErrorRecord record);

    }

}
