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
import cn.liboshuai.pulsix.engine.model.SceneSnapshot;
import cn.liboshuai.pulsix.engine.model.SceneSnapshotEnvelope;
import cn.liboshuai.pulsix.engine.runtime.CompiledSceneRuntime;
import cn.liboshuai.pulsix.engine.runtime.RuntimeCompiler;
import cn.liboshuai.pulsix.engine.runtime.SceneRuntimeManager;
import cn.liboshuai.pulsix.engine.script.DefaultScriptCompiler;
import org.apache.flink.api.common.state.MapStateDescriptor;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.streaming.api.functions.co.KeyedBroadcastProcessFunction;
import org.apache.flink.util.Collector;

import java.io.Serializable;
import java.util.Optional;

public class DecisionBroadcastProcessFunction
        extends KeyedBroadcastProcessFunction<String, RiskEvent, SceneSnapshotEnvelope, DecisionResult> {

    private final MapStateDescriptor<String, SceneSnapshot> snapshotStateDescriptor;

    private final LookupServiceFactory lookupServiceFactory;

    private transient LookupService lookupService;

    private transient SceneRuntimeManager runtimeManager;

    private transient StreamFeatureStateStore stateStore;

    private transient DecisionExecutor decisionExecutor;

    public DecisionBroadcastProcessFunction(MapStateDescriptor<String, SceneSnapshot> snapshotStateDescriptor) {
        this(snapshotStateDescriptor, InMemoryLookupService::demo);
    }

    public DecisionBroadcastProcessFunction(MapStateDescriptor<String, SceneSnapshot> snapshotStateDescriptor,
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
    }

    @Override
    public void processBroadcastElement(SceneSnapshotEnvelope envelope,
                                        Context context,
                                        Collector<DecisionResult> collector) throws Exception {
        context.getBroadcastState(snapshotStateDescriptor).put(envelope.getSceneCode(), envelope.getSnapshot());
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
        SceneSnapshot snapshot = context.getBroadcastState(snapshotStateDescriptor).get(event.getSceneCode());
        if (snapshot == null) {
            return Optional.empty();
        }
        return Optional.of(runtimeManager.activate(snapshot));
    }

    @FunctionalInterface
    public interface LookupServiceFactory extends Serializable {

        LookupService create();

    }

}
