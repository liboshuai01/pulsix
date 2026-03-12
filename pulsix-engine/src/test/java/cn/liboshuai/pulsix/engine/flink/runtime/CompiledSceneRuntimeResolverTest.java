package cn.liboshuai.pulsix.engine.flink.runtime;

import cn.liboshuai.pulsix.engine.demo.DemoFixtures;
import cn.liboshuai.pulsix.engine.json.EngineJson;
import cn.liboshuai.pulsix.engine.model.SceneSnapshot;
import cn.liboshuai.pulsix.engine.runtime.CompiledSceneRuntime;
import cn.liboshuai.pulsix.engine.runtime.RuntimeCompiler;
import cn.liboshuai.pulsix.engine.script.DefaultScriptCompiler;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CompiledSceneRuntimeResolverTest {

    @Test
    void shouldReuseSameCompiledRuntimeWhenSnapshotChecksumMatches() {
        CompiledSceneRuntimeResolver resolver = new CompiledSceneRuntimeResolver(
                new RuntimeCompiler(new DefaultScriptCompiler()),
                3
        );
        SceneSnapshot snapshot = versionedSnapshot(12, "resolver-checksum-v12");

        CompiledSceneRuntime first = resolver.resolve(snapshot);
        CompiledSceneRuntime second = resolver.resolve(copy(snapshot));

        assertSame(first, second);
    }

    @Test
    void shouldTrimOldVersionsPerSceneWhenResolverCacheExceedsLimit() {
        CompiledSceneRuntimeResolver resolver = new CompiledSceneRuntimeResolver(
                new RuntimeCompiler(new DefaultScriptCompiler()),
                2
        );

        resolver.resolve(versionedSnapshot(12, "resolver-checksum-v12"));
        resolver.resolve(versionedSnapshot(13, "resolver-checksum-v13"));
        resolver.resolve(versionedSnapshot(14, "resolver-checksum-v14"));

        assertFalse(resolver.cachedRuntime("TRADE_RISK", 12).isPresent());
        assertTrue(resolver.cachedRuntime("TRADE_RISK", 13).isPresent());
        assertEquals(14, resolver.cachedRuntime("TRADE_RISK", 14).orElseThrow().version());
    }

    private SceneSnapshot versionedSnapshot(int version,
                                            String checksum) {
        SceneSnapshot snapshot = copy(DemoFixtures.demoSnapshot());
        snapshot.setVersion(version);
        snapshot.setChecksum(checksum);
        return snapshot;
    }

    private SceneSnapshot copy(SceneSnapshot snapshot) {
        return EngineJson.read(EngineJson.write(snapshot), SceneSnapshot.class);
    }

}
