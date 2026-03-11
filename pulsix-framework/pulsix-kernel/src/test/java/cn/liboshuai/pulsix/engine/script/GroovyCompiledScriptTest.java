package cn.liboshuai.pulsix.engine.script;

import cn.liboshuai.pulsix.engine.context.EvalContext;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GroovyCompiledScriptTest {

    @Test
    void shouldExecuteAllowedGroovyExpressionInSandbox() {
        EvalContext context = new EvalContext();
        context.put("device_bind_user_cnt_1h", 4);
        context.put("user_risk_level", "H");

        Object result = new GroovyCompiledScript("return device_bind_user_cnt_1h >= 4 && ['M','H'].contains(user_risk_level)")
                .execute(context);

        assertEquals(Boolean.TRUE, result);
    }

    @Test
    void shouldRejectConstructorCallInSandbox() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> new GroovyCompiledScript("return new File('/tmp/pulsix-danger')"));

        assertTrue(exception.getMessage().contains("sandbox"));
    }

    @Test
    void shouldRejectClassLoaderAccessInSandbox() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> new GroovyCompiledScript("return this.class.classLoader"));

        assertTrue(exception.getMessage().contains("sandbox"));
    }

}
