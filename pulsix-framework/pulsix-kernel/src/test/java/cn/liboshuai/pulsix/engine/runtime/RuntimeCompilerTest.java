package cn.liboshuai.pulsix.engine.runtime;

import cn.liboshuai.pulsix.engine.demo.DemoFixtures;
import cn.liboshuai.pulsix.engine.model.EngineErrorCodes;
import cn.liboshuai.pulsix.engine.model.RuleSpec;
import cn.liboshuai.pulsix.engine.model.SceneSnapshot;
import cn.liboshuai.pulsix.engine.script.DefaultScriptCompiler;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RuntimeCompilerTest {

    @Test
    void shouldRejectGroovyScriptWhenRuntimeHintsDisallowGroovy() {
        SceneSnapshot snapshot = DemoFixtures.demoSnapshot();
        snapshot.getRuntimeHints().setAllowGroovy(false);

        RuntimeCompileException exception = assertThrows(RuntimeCompileException.class,
                () -> new RuntimeCompiler(new DefaultScriptCompiler()).compile(snapshot));

        assertEquals(EngineErrorCodes.GROOVY_DISABLED, exception.getErrorCode());
        assertEquals("R003", exception.getRuleCode());
        assertTrue(exception.getMessage().contains("allowGroovy"));
    }

    @Test
    void shouldRejectDangerousGroovyRuleInSandboxAtCompileTime() {
        SceneSnapshot snapshot = DemoFixtures.demoSnapshot();
        RuleSpec rule = snapshot.getRules().stream()
                .filter(item -> "R003".equals(item.getCode()))
                .findFirst()
                .orElseThrow();
        rule.setWhenExpr("return new File('/tmp/pulsix-danger').text");

        RuntimeCompileException exception = assertThrows(RuntimeCompileException.class,
                () -> new RuntimeCompiler(new DefaultScriptCompiler()).compile(snapshot));

        assertEquals(EngineErrorCodes.GROOVY_SANDBOX_REJECTED, exception.getErrorCode());
        assertEquals("R003", exception.getRuleCode());
        assertTrue(exception.getMessage().contains("sandbox"));
    }

}
