package cn.liboshuai.pulsix.engine.runtime;

import cn.liboshuai.pulsix.engine.model.SceneSnapshot;
import cn.liboshuai.pulsix.engine.model.SceneSnapshotEnvelope;
import cn.liboshuai.pulsix.engine.snapshot.SceneSnapshotEnvelopes;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class SceneRuntimeManager {

    private final RuntimeCompiler runtimeCompiler;

    private final Map<String, CompiledSceneRuntime> activeRuntimes = new ConcurrentHashMap<>();

    public SceneRuntimeManager(RuntimeCompiler runtimeCompiler) {
        this.runtimeCompiler = runtimeCompiler;
    }

    public void apply(SceneSnapshotEnvelope envelope) {
        if (envelope == null || envelope.getSnapshot() == null) {
            return;
        }
        activate(SceneSnapshotEnvelopes.fromEnvelope(envelope).getSnapshot());
    }

    public CompiledSceneRuntime compile(SceneSnapshot snapshot) {
        return runtimeCompiler.compile(snapshot);
    }

    public CompiledSceneRuntime activate(SceneSnapshot snapshot) {
        return activate(compile(snapshot));
    }

    public CompiledSceneRuntime activate(CompiledSceneRuntime runtime) {
        String sceneCode = runtime.sceneCode();
        activeRuntimes.put(sceneCode, runtime);
        return runtime;
    }

    public Optional<CompiledSceneRuntime> getActiveRuntime(String sceneCode) {
        return Optional.ofNullable(activeRuntimes.get(sceneCode));
    }

}
