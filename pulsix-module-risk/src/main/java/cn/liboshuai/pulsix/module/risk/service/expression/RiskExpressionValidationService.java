package cn.liboshuai.pulsix.module.risk.service.expression;

import cn.hutool.core.util.StrUtil;
import cn.liboshuai.pulsix.engine.model.EngineType;
import cn.liboshuai.pulsix.engine.script.DefaultScriptCompiler;
import org.springframework.stereotype.Service;

@Service
public class RiskExpressionValidationService {

    private final DefaultScriptCompiler scriptCompiler = new DefaultScriptCompiler();

    public void validate(String engineType, String expression, Integer sandboxFlag) {
        if (StrUtil.isBlank(expression)) {
            throw new IllegalArgumentException("表达式不能为空");
        }
        EngineType resolvedEngineType = EngineType.valueOf(StrUtil.blankToDefault(engineType, EngineType.AVIATOR.name()).trim().toUpperCase());
        if (resolvedEngineType == EngineType.GROOVY && !Integer.valueOf(1).equals(sandboxFlag)) {
            throw new IllegalArgumentException("Groovy 表达式必须开启脚本沙箱");
        }
        scriptCompiler.compile(resolvedEngineType, expression.trim());
    }

}
