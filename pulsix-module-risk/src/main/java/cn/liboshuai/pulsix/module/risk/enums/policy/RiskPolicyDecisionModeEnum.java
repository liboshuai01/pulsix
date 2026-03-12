package cn.liboshuai.pulsix.module.risk.enums.policy;

import cn.liboshuai.pulsix.framework.common.core.ArrayValuable;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

@Getter
@AllArgsConstructor
public enum RiskPolicyDecisionModeEnum implements ArrayValuable<String> {

    FIRST_HIT("FIRST_HIT", "命中首条即返回"),
    SCORE_CARD("SCORE_CARD", "命中规则累计分值后按分段映射动作");

    public static final String[] ARRAYS = Arrays.stream(values())
            .map(RiskPolicyDecisionModeEnum::getType).toArray(String[]::new);

    private final String type;
    private final String description;

    @Override
    public String[] array() {
        return ARRAYS;
    }

}
