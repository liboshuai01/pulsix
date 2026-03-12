package cn.liboshuai.pulsix.engine.flink.runtime;

import cn.liboshuai.pulsix.engine.model.SceneSnapshot;
import cn.liboshuai.pulsix.engine.runtime.CompiledSceneRuntime;
import cn.liboshuai.pulsix.engine.runtime.RuntimeCompiler;

import java.util.Objects;
import java.util.Optional;

public class CompiledSceneRuntimeResolver {

    private final RuntimeCompiler runtimeCompiler;

    private final SceneRuntimeCache runtimeCache;

    public CompiledSceneRuntimeResolver(RuntimeCompiler runtimeCompiler) {
        this(runtimeCompiler, new SceneRuntimeCache());
    }

    CompiledSceneRuntimeResolver(RuntimeCompiler runtimeCompiler,
                                 int maxVersionsPerScene) {
        this(runtimeCompiler, new SceneRuntimeCache(maxVersionsPerScene));
    }

    CompiledSceneRuntimeResolver(RuntimeCompiler runtimeCompiler,
                                 SceneRuntimeCache runtimeCache) {
        this.runtimeCompiler = Objects.requireNonNull(runtimeCompiler, "runtimeCompiler");
        this.runtimeCache = Objects.requireNonNull(runtimeCache, "runtimeCache");
    }

    public CompiledSceneRuntime resolve(SceneSnapshot snapshot) {
        Objects.requireNonNull(snapshot, "snapshot");
        Optional<CompiledSceneRuntime> cachedRuntime = runtimeCache.get(snapshot.getSceneCode(), snapshot.getVersion());
        if (cachedRuntime.isPresent() && sameChecksum(cachedRuntime.get(), snapshot)) {
            return cachedRuntime.get();
        }
        CompiledSceneRuntime compiledRuntime = runtimeCompiler.compile(snapshot);
        runtimeCache.put(compiledRuntime);
        return compiledRuntime;
    }

    Optional<CompiledSceneRuntime> cachedRuntime(String sceneCode,
                                                 Integer version) {
        return runtimeCache.get(sceneCode, version);
    }

    private boolean sameChecksum(CompiledSceneRuntime runtime,
                                 SceneSnapshot snapshot) {
        return runtime != null
                && runtime.getSnapshot() != null
                && Objects.equals(runtime.getSnapshot().getChecksum(), snapshot.getChecksum());
    }

}
