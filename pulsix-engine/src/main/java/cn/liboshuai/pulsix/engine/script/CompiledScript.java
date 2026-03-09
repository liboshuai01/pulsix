package cn.liboshuai.pulsix.engine.script;

import cn.liboshuai.pulsix.engine.context.EvalContext;

public interface CompiledScript {

    Object execute(EvalContext context);

    String rawExpression();

}
