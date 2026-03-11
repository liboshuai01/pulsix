package cn.liboshuai.pulsix.engine.flink.runtime;

import cn.liboshuai.pulsix.engine.runtime.CompiledSceneRuntime;

import java.util.Map;
import java.util.NavigableMap;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListMap;

public class SceneRuntimeCache {

    private static final int DEFAULT_MAX_VERSIONS_PER_SCENE = 8;

    private final int maxVersionsPerScene;

    private final Map<String, NavigableMap<Integer, CompiledSceneRuntime>> runtimeCache = new ConcurrentHashMap<>();

    public SceneRuntimeCache() {
        this(DEFAULT_MAX_VERSIONS_PER_SCENE);
    }

    public SceneRuntimeCache(int maxVersionsPerScene) {
        this.maxVersionsPerScene = Math.max(1, maxVersionsPerScene);
    }

    public void put(CompiledSceneRuntime runtime) {
        if (runtime == null || runtime.sceneCode() == null) {
            return;
        }
        int version = runtime.version() == null ? 0 : runtime.version();
        runtimeCache.computeIfAbsent(runtime.sceneCode(), ignored -> new ConcurrentSkipListMap<>())
                .put(version, runtime);
        trimSceneCache(runtime.sceneCode());
    }

    public Optional<CompiledSceneRuntime> get(String sceneCode, Integer version) {
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
        while (versions.size() > maxVersionsPerScene) {
            versions.pollFirstEntry();
        }
    }

}
