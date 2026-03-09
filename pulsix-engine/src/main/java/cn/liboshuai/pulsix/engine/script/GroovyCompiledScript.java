package cn.liboshuai.pulsix.engine.script;

import cn.liboshuai.pulsix.engine.context.EvalContext;
import groovy.lang.Binding;
import groovy.lang.GroovyClassLoader;
import groovy.lang.Script;

public class GroovyCompiledScript implements CompiledScript {

    private final String rawExpression;

    private final Class<? extends Script> scriptClass;

    @SuppressWarnings("unchecked")
    public GroovyCompiledScript(String rawExpression) {
        this.rawExpression = normalize(rawExpression);
        GroovyClassLoader classLoader = new GroovyClassLoader(Thread.currentThread().getContextClassLoader());
        this.scriptClass = (Class<? extends Script>) classLoader.parseClass(this.rawExpression);
    }

    @Override
    public Object execute(EvalContext context) {
        Binding binding = new Binding(context.getValues());
        Script script = newScript(binding);
        return script.run();
    }

    @Override
    public String rawExpression() {
        return rawExpression;
    }

    private Script newScript(Binding binding) {
        try {
            Script script = scriptClass.getDeclaredConstructor().newInstance();
            script.setBinding(binding);
            return script;
        } catch (Exception exception) {
            throw new IllegalStateException("create groovy script failed", exception);
        }
    }

    private static String normalize(String expression) {
        String trimmed = expression == null ? "" : expression.trim();
        if (trimmed.startsWith("return") || trimmed.contains(";")) {
            return trimmed;
        }
        return "return " + trimmed;
    }

}
