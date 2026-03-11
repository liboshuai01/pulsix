package cn.liboshuai.pulsix.engine.flink.runtime;

import cn.liboshuai.pulsix.engine.demo.DemoFixtures;
import cn.liboshuai.pulsix.engine.model.SceneSnapshot;
import cn.liboshuai.pulsix.engine.runtime.RuntimeCompiler;
import cn.liboshuai.pulsix.engine.script.DefaultScriptCompiler;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SceneRuntimeCacheTest {

    @Test
    void shouldKeepHistoricalVersionsByDefault() {
        RuntimeCompiler compiler = new RuntimeCompiler(new DefaultScriptCompiler());
        SceneRuntimeCache cache = new SceneRuntimeCache();

        cache.put(compiler.compile(versionedSnapshot(12, "checksum-v12")));
        cache.put(compiler.compile(versionedSnapshot(13, "checksum-v13")));
        cache.put(compiler.compile(versionedSnapshot(14, "checksum-v14")));

        assertTrue(cache.get("TRADE_RISK", 12).isPresent());
        assertTrue(cache.get("TRADE_RISK", 13).isPresent());
        assertEquals(14, cache.get("TRADE_RISK", 14).orElseThrow().version());
    }

    @Test
    void shouldTrimOldestVersionsWhenCustomLimitExceeded() {
        RuntimeCompiler compiler = new RuntimeCompiler(new DefaultScriptCompiler());
        SceneRuntimeCache cache = new SceneRuntimeCache(3);

        cache.put(compiler.compile(versionedSnapshot(12, "checksum-v12")));
        cache.put(compiler.compile(versionedSnapshot(13, "checksum-v13")));
        cache.put(compiler.compile(versionedSnapshot(14, "checksum-v14")));
        cache.put(compiler.compile(versionedSnapshot(15, "checksum-v15")));

        assertFalse(cache.get("TRADE_RISK", 12).isPresent());
        assertTrue(cache.get("TRADE_RISK", 13).isPresent());
        assertTrue(cache.get("TRADE_RISK", 14).isPresent());
        assertEquals(15, cache.get("TRADE_RISK", 15).orElseThrow().version());
    }

    private SceneSnapshot versionedSnapshot(int version, String checksum) {
        SceneSnapshot snapshot = DemoFixtures.demoSnapshot();
        snapshot.setVersion(version);
        snapshot.setChecksum(checksum);
        return snapshot;
    }

}
