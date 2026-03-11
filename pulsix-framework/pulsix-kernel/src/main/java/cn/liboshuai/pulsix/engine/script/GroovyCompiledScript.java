package cn.liboshuai.pulsix.engine.script;

import cn.liboshuai.pulsix.engine.context.EvalContext;
import groovy.lang.Binding;
import groovy.lang.GroovyClassLoader;
import groovy.lang.Script;
import org.codehaus.groovy.ast.expr.ClassExpression;
import org.codehaus.groovy.ast.expr.ClosureExpression;
import org.codehaus.groovy.ast.expr.ConstructorCallExpression;
import org.codehaus.groovy.ast.expr.Expression;
import org.codehaus.groovy.ast.expr.MethodCallExpression;
import org.codehaus.groovy.ast.expr.MethodPointerExpression;
import org.codehaus.groovy.ast.expr.PropertyExpression;
import org.codehaus.groovy.ast.expr.VariableExpression;
import org.codehaus.groovy.control.CompilationFailedException;
import org.codehaus.groovy.control.CompilerConfiguration;
import org.codehaus.groovy.control.customizers.SecureASTCustomizer;

import java.io.IOException;
import java.util.List;
import java.util.Set;

public class GroovyCompiledScript implements CompiledScript {

    private static final Set<String> FORBIDDEN_PROPERTIES = Set.of(
            "class",
            "classLoader",
            "metaClass",
            "binding",
            "thisObject",
            "owner",
            "delegate"
    );

    private static final Set<String> FORBIDDEN_METHODS = Set.of(
            "getClass",
            "getMetaClass",
            "setMetaClass",
            "invokeMethod",
            "evaluate",
            "run",
            "parseClass",
            "forName",
            "newInstance",
            "exit",
            "exec",
            "wait",
            "notify",
            "notifyAll",
            "sleep"
    );

    private static final Set<String> FORBIDDEN_VARIABLES = Set.of(
            "this",
            "super"
    );

    private final String rawExpression;

    private final Class<? extends Script> scriptClass;

    @SuppressWarnings("unchecked")
    public GroovyCompiledScript(String rawExpression) {
        this.rawExpression = normalize(rawExpression);
        CompilerConfiguration configuration = new CompilerConfiguration();
        configuration.addCompilationCustomizers(groovySandbox());
        GroovyClassLoader classLoader = new GroovyClassLoader(Thread.currentThread().getContextClassLoader(), configuration);
        try {
            this.scriptClass = (Class<? extends Script>) classLoader.parseClass(this.rawExpression);
        } catch (CompilationFailedException exception) {
            throw new IllegalArgumentException("groovy script rejected by sandbox: " + rootMessage(exception), exception);
        } finally {
            closeQuietly(classLoader);
        }
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

    private static SecureASTCustomizer groovySandbox() {
        SecureASTCustomizer customizer = new SecureASTCustomizer();
        customizer.setClosuresAllowed(false);
        customizer.setMethodDefinitionAllowed(false);
        customizer.setPackageAllowed(false);
        customizer.setImportsWhitelist(List.of());
        customizer.setStarImportsWhitelist(List.of());
        customizer.setStaticImportsWhitelist(List.of());
        customizer.setStaticStarImportsWhitelist(List.of());
        customizer.addExpressionCheckers(GroovyCompiledScript::isSafeExpression);
        return customizer;
    }

    private static boolean isSafeExpression(Expression expression) {
        if (expression == null) {
            return true;
        }
        if (expression instanceof ConstructorCallExpression
                || expression instanceof ClassExpression
                || expression instanceof ClosureExpression
                || expression instanceof MethodPointerExpression) {
            return false;
        }
        if (expression instanceof VariableExpression variableExpression) {
            return !FORBIDDEN_VARIABLES.contains(variableExpression.getName());
        }
        if (expression instanceof PropertyExpression propertyExpression) {
            String property = propertyExpression.getPropertyAsString();
            return property == null || !FORBIDDEN_PROPERTIES.contains(property);
        }
        if (expression instanceof MethodCallExpression methodCallExpression) {
            String methodName = methodCallExpression.getMethodAsString();
            return methodName == null || !FORBIDDEN_METHODS.contains(methodName);
        }
        return true;
    }

    private static void closeQuietly(GroovyClassLoader classLoader) {
        if (classLoader == null) {
            return;
        }
        try {
            classLoader.close();
        } catch (IOException ignored) {
        }
    }

    private static String rootMessage(Throwable throwable) {
        Throwable current = throwable;
        String message = null;
        while (current != null) {
            if (current.getMessage() != null && !current.getMessage().isBlank()) {
                message = current.getMessage();
            }
            current = current.getCause();
        }
        return message == null ? "compile failed" : message;
    }

}
