package cn.liboshuai.pulsix.engine.flink;

import cn.liboshuai.pulsix.engine.core.DecisionExecutor;
import cn.liboshuai.pulsix.engine.feature.InMemoryLookupService;
import cn.liboshuai.pulsix.engine.feature.InMemoryStreamFeatureStateStore;
import cn.liboshuai.pulsix.engine.feature.LookupService;
import cn.liboshuai.pulsix.engine.feature.StreamFeatureStateStore;
import cn.liboshuai.pulsix.engine.model.DecisionLogRecord;
import cn.liboshuai.pulsix.engine.model.DecisionResult;
import cn.liboshuai.pulsix.engine.model.EngineErrorRecord;
import cn.liboshuai.pulsix.engine.model.RiskEvent;
import cn.liboshuai.pulsix.engine.model.SceneSnapshotEnvelope;
import cn.liboshuai.pulsix.engine.runtime.CompiledSceneRuntime;
import cn.liboshuai.pulsix.engine.runtime.RuntimeCompiler;
import cn.liboshuai.pulsix.engine.runtime.SceneRuntimeManager;
import cn.liboshuai.pulsix.engine.script.DefaultScriptCompiler;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.apache.flink.api.common.state.MapStateDescriptor;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.streaming.api.functions.co.KeyedBroadcastProcessFunction;
import org.apache.flink.util.Collector;

import java.io.Serializable;
import java.util.Optional;

public class DecisionBroadcastProcessFunction
        extends KeyedBroadcastProcessFunction<String, RiskEvent, String, DecisionResult> {

    private final MapStateDescriptor<String, String> snapshotStateDescriptor;

    private final LookupServiceFactory lookupServiceFactory;

    private transient LookupService lookupService;

    private transient SceneRuntimeManager runtimeManager;

    private transient StreamFeatureStateStore stateStore;

    private transient DecisionExecutor decisionExecutor;

    private transient ObjectMapper objectMapper;

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
        this.objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
    }

    @Override
    public void processBroadcastElement(String envelopeJson,
                                        Context context,
                                        Collector<DecisionResult> collector) throws Exception {
        SceneSnapshotEnvelope envelope = parseEnvelope(envelopeJson);
        context.getBroadcastState(snapshotStateDescriptor).put(envelope.getSceneCode(), envelopeJson);
        runtimeManager.apply(envelope);
    }

    @Override
    public void processElement(RiskEvent event,
                               ReadOnlyContext context,
                               Collector<DecisionResult> collector) throws Exception {
        CompiledSceneRuntime runtime = resolveRuntime(event, context).orElse(null);
        if (runtime == null) {
            context.output(EngineOutputTags.ENGINE_ERROR, EngineErrorRecord.of("runtime-missing", event, null,
                    new IllegalStateException("runtime not found")));
            return;
        }
        try {
            DecisionResult result = decisionExecutor.execute(runtime, event, stateStore, lookupService);
            collector.collect(result);
            context.output(EngineOutputTags.DECISION_LOG, DecisionLogRecord.from(result));
        } catch (Exception exception) {
            context.output(EngineOutputTags.ENGINE_ERROR,
                    EngineErrorRecord.of("decision-execute", event, runtime.version(), exception));
        }
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

    private SceneSnapshotEnvelope parseEnvelope(String envelopeJson) {
        try {
            return objectMapper.readValue(envelopeJson, SceneSnapshotEnvelope.class);
        } catch (Exception exception) {
            throw new IllegalStateException("parse scene snapshot envelope failed", exception);
        }
    }

    @FunctionalInterface
    public interface LookupServiceFactory extends Serializable {

        LookupService create();

    }

}
