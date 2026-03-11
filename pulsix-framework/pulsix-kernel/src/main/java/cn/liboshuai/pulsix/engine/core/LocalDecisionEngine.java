package cn.liboshuai.pulsix.engine.core;

import cn.liboshuai.pulsix.engine.feature.LookupService;
import cn.liboshuai.pulsix.engine.feature.StreamFeatureStateStore;
import cn.liboshuai.pulsix.engine.model.DecisionResult;
import cn.liboshuai.pulsix.engine.model.RiskEvent;
import cn.liboshuai.pulsix.engine.model.SceneSnapshot;
import cn.liboshuai.pulsix.engine.model.SceneSnapshotEnvelope;
import cn.liboshuai.pulsix.engine.runtime.CompiledSceneRuntime;
import cn.liboshuai.pulsix.engine.runtime.SceneRuntimeManager;

public class LocalDecisionEngine {

    private final SceneRuntimeManager runtimeManager;

    private final StreamFeatureStateStore stateStore;

    private final LookupService lookupService;

    private final DecisionExecutor decisionExecutor;

    public LocalDecisionEngine(SceneRuntimeManager runtimeManager,
                               StreamFeatureStateStore stateStore,
                               LookupService lookupService,
                               DecisionExecutor decisionExecutor) {
        this.runtimeManager = runtimeManager;
        this.stateStore = stateStore;
        this.lookupService = lookupService;
        this.decisionExecutor = decisionExecutor;
    }

    public void publish(SceneSnapshotEnvelope envelope) {
        runtimeManager.apply(envelope);
    }

    public CompiledSceneRuntime publish(SceneSnapshot snapshot) {
        return runtimeManager.activate(snapshot);
    }

    public DecisionResult evaluate(RiskEvent event) {
        CompiledSceneRuntime runtime = runtimeManager.getActiveRuntime(event.getSceneCode())
                .orElseThrow(() -> new IllegalStateException("runtime not found: " + event.getSceneCode()));
        return decisionExecutor.execute(runtime, event, stateStore, lookupService);
    }

}
