package cn.liboshuai.pulsix.engine.runtime;

import cn.liboshuai.pulsix.engine.demo.DemoFixtures;
import cn.liboshuai.pulsix.engine.model.SceneSnapshot;
import cn.liboshuai.pulsix.engine.script.DefaultScriptCompiler;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SceneRuntimeManagerTest {

    @Test
    void shouldSwitchActiveRuntimeByVersion() {
        SceneRuntimeManager runtimeManager = new SceneRuntimeManager(new RuntimeCompiler(new DefaultScriptCompiler()));
        SceneSnapshot version12 = DemoFixtures.demoSnapshot();
        SceneSnapshot version13 = DemoFixtures.demoSnapshot();
        version13.setVersion(13);
        version13.setChecksum("8d2041a7cf8f47b4b6b0f91d2ab8d9d1");

        CompiledSceneRuntime runtime12 = runtimeManager.activate(version12);
        CompiledSceneRuntime runtime13 = runtimeManager.activate(version13);

        assertEquals(12, runtime12.version());
        assertEquals(13, runtime13.version());
        assertEquals(13, runtimeManager.getActiveRuntime(version13.getSceneCode()).orElseThrow().version());
        assertTrue(runtimeManager.getActiveRuntime(version12.getSceneCode()).isPresent());
    }

}
