package cn.liboshuai.pulsix.engine.runtime;

import cn.liboshuai.pulsix.engine.model.SceneSnapshot;
import cn.liboshuai.pulsix.engine.model.SceneSnapshotEnvelope;

import java.util.Map;
import java.util.NavigableMap;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListMap;

public class SceneRuntimeManager {

    private final RuntimeCompiler runtimeCompiler;

    private final Map<String, CompiledSceneRuntime> activeRuntimes = new ConcurrentHashMap<>();

    private final Map<String, NavigableMap<Integer, CompiledSceneRuntime>> runtimeCache = new ConcurrentHashMap<>();

    public SceneRuntimeManager(RuntimeCompiler runtimeCompiler) {
        this.runtimeCompiler = runtimeCompiler;
    }

    public void apply(SceneSnapshotEnvelope envelope) {
        if (envelope == null || envelope.getSnapshot() == null) {
            return;
        }
        activate(envelope.getSnapshot());
    }

    public CompiledSceneRuntime compile(SceneSnapshot snapshot) {
        return runtimeCompiler.compile(snapshot);
    }

    public CompiledSceneRuntime activate(SceneSnapshot snapshot) {
        return activate(compile(snapshot));
    }

    public CompiledSceneRuntime activate(CompiledSceneRuntime runtime) {
        String sceneCode = runtime.sceneCode();
        int version = runtime.version() == null ? 0 : runtime.version();
        activeRuntimes.put(sceneCode, runtime);
        runtimeCache.computeIfAbsent(sceneCode, ignored -> new ConcurrentSkipListMap<>())
                .put(version, runtime);
        trimSceneCache(sceneCode);
        return runtime;
    }

    public Optional<CompiledSceneRuntime> getActiveRuntime(String sceneCode) {
        return Optional.ofNullable(activeRuntimes.get(sceneCode));
    }

    public Optional<CompiledSceneRuntime> getRuntime(String sceneCode, Integer version) {
        if (sceneCode == null || version == null) {
            return Optional.empty();
        }
        NavigableMap<Integer, CompiledSceneRuntime> versions = runtimeCache.get(sceneCode);
        if (versions == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(versions.get(version));
    }

    private void trimSceneCache(String sceneCode) {
        NavigableMap<Integer, CompiledSceneRuntime> versions = runtimeCache.get(sceneCode);
        if (versions == null) {
            return;
        }
        while (versions.size() > 2) {
            versions.pollFirstEntry();
        }
    }

}
