package cn.liboshuai.pulsix.module.risk.enums.rule;

import cn.liboshuai.pulsix.framework.common.core.ArrayValuable;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

@Getter
@AllArgsConstructor
public enum RiskRuleEngineTypeEnum implements ArrayValuable<String> {

    DSL("DSL", "DSL"),
    AVIATOR("AVIATOR", "Aviator"),
    GROOVY("GROOVY", "Groovy");

    public static final String[] ARRAYS = Arrays.stream(values())
            .map(RiskRuleEngineTypeEnum::getType).toArray(String[]::new);

    private final String type;
    private final String description;

    @Override
    public String[] array() {
        return ARRAYS;
    }

}
