package cn.liboshuai.pulsix.engine.runtime;

import cn.liboshuai.pulsix.engine.model.SceneSnapshot;
import cn.liboshuai.pulsix.engine.model.SceneSnapshotEnvelope;

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
        activate(envelope.getSnapshot());
    }

    public CompiledSceneRuntime activate(SceneSnapshot snapshot) {
        CompiledSceneRuntime runtime = runtimeCompiler.compile(snapshot);
        activeRuntimes.put(snapshot.getSceneCode(), runtime);
        return runtime;
    }

    public Optional<CompiledSceneRuntime> getActiveRuntime(String sceneCode) {
        return Optional.ofNullable(activeRuntimes.get(sceneCode));
    }

}
