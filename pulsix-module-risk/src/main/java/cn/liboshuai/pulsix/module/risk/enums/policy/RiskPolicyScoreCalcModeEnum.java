package cn.liboshuai.pulsix.module.risk.enums.policy;

import cn.liboshuai.pulsix.framework.common.core.ArrayValuable;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

@Getter
@AllArgsConstructor
public enum RiskPolicyScoreCalcModeEnum implements ArrayValuable<String> {

    NONE("NONE", "不累计分值");

    public static final String[] ARRAYS = Arrays.stream(values())
            .map(RiskPolicyScoreCalcModeEnum::getType).toArray(String[]::new);

    private final String type;
    private final String description;

    @Override
    public String[] array() {
        return ARRAYS;
    }

}
