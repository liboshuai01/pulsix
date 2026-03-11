package cn.liboshuai.pulsix.engine.script;

import cn.liboshuai.pulsix.engine.model.EngineType;

public class DefaultScriptCompiler implements ScriptCompiler {

    @Override
    public CompiledScript compile(EngineType engineType, String expression) {
        if (engineType == null || engineType == EngineType.AVIATOR || engineType == EngineType.DSL) {
            return new AviatorCompiledScript(expression);
        }
        if (engineType == EngineType.GROOVY) {
            return new GroovyCompiledScript(expression);
        }
        throw new IllegalArgumentException("unsupported engine type: " + engineType);
    }

}
