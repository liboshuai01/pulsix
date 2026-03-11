package cn.liboshuai.pulsix.engine.script;

import cn.liboshuai.pulsix.engine.model.EngineType;

public interface ScriptCompiler {

    CompiledScript compile(EngineType engineType, String expression);

}
