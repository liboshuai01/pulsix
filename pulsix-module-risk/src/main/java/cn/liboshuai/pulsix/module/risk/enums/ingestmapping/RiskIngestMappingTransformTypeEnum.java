package cn.liboshuai.pulsix.module.risk.enums.ingestmapping;

import cn.liboshuai.pulsix.framework.common.core.ArrayValuable;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

@Getter
@AllArgsConstructor
public enum RiskIngestMappingTransformTypeEnum implements ArrayValuable<String> {

    DIRECT("DIRECT", "直接映射"),
    CONST("CONST", "常量赋值"),
    TIME_MILLIS_TO_DATETIME("TIME_MILLIS_TO_DATETIME", "毫秒时间戳转日期时间"),
    DIVIDE_100("DIVIDE_100", "数值除以 100"),
    ENUM_MAP("ENUM_MAP", "枚举映射");

    public static final String[] ARRAYS = Arrays.stream(values())
            .map(RiskIngestMappingTransformTypeEnum::getType).toArray(String[]::new);

    private final String type;
    private final String description;

    @Override
    public String[] array() {
        return ARRAYS;
    }

}
