package cn.liboshuai.pulsix.module.risk.enums.feature;

import cn.liboshuai.pulsix.framework.common.core.ArrayValuable;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

@Getter
@AllArgsConstructor
public enum RiskFeatureValueTypeEnum implements ArrayValuable<String> {

    INT("INT", "整数"),
    LONG("LONG", "长整型"),
    DECIMAL("DECIMAL", "小数"),
    BOOLEAN("BOOLEAN", "布尔"),
    STRING("STRING", "字符串");

    public static final String[] ARRAYS = Arrays.stream(values())
            .map(RiskFeatureValueTypeEnum::getType).toArray(String[]::new);

    private final String type;
    private final String description;

    @Override
    public String[] array() {
        return ARRAYS;
    }

}
