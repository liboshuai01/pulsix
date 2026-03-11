package cn.liboshuai.pulsix.engine.runtime;

import cn.liboshuai.pulsix.engine.demo.DemoFixtures;
import cn.liboshuai.pulsix.engine.model.SceneSnapshot;
import cn.liboshuai.pulsix.engine.script.DefaultScriptCompiler;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RuntimeCompilerTest {

    @Test
    void shouldRejectGroovyScriptWhenRuntimeHintsDisallowGroovy() {
        SceneSnapshot snapshot = DemoFixtures.demoSnapshot();
        snapshot.getRuntimeHints().setAllowGroovy(false);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> new RuntimeCompiler(new DefaultScriptCompiler()).compile(snapshot));

        assertEquals("groovy script is disabled by runtimeHints.allowGroovy", exception.getMessage());
    }

}
