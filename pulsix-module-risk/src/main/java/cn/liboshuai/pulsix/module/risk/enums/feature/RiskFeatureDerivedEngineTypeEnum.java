package cn.liboshuai.pulsix.module.risk.enums.feature;

import cn.liboshuai.pulsix.framework.common.core.ArrayValuable;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

@Getter
@AllArgsConstructor
public enum RiskFeatureDerivedEngineTypeEnum implements ArrayValuable<String> {

    AVIATOR("AVIATOR", "Aviator"),
    GROOVY("GROOVY", "Groovy");

    public static final String[] ARRAYS = Arrays.stream(values())
            .map(RiskFeatureDerivedEngineTypeEnum::getType).toArray(String[]::new);

    private final String type;
    private final String description;

    @Override
    public String[] array() {
        return ARRAYS;
    }

}
