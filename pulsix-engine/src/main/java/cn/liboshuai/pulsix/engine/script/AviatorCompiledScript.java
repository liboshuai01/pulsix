package cn.liboshuai.pulsix.engine.script;

import cn.liboshuai.pulsix.engine.context.EvalContext;
import com.googlecode.aviator.AviatorEvaluator;
import com.googlecode.aviator.Expression;

public class AviatorCompiledScript implements CompiledScript {

    private final String rawExpression;

    private final Expression expression;

    public AviatorCompiledScript(String rawExpression) {
        this.rawExpression = rawExpression;
        this.expression = AviatorEvaluator.compile(rawExpression, true);
    }

    @Override
    public Object execute(EvalContext context) {
        return expression.execute(context.getValues());
    }

    @Override
    public String rawExpression() {
        return rawExpression;
    }

}
