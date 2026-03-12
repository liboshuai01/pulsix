package cn.liboshuai.pulsix.module.risk.enums.eventfield;

import cn.liboshuai.pulsix.framework.common.core.ArrayValuable;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

@Getter
@AllArgsConstructor
public enum RiskEventFieldTypeEnum implements ArrayValuable<String> {

    STRING("STRING", "字符串"),
    LONG("LONG", "长整型"),
    DECIMAL("DECIMAL", "数值"),
    BOOLEAN("BOOLEAN", "布尔值"),
    DATETIME("DATETIME", "日期时间"),
    JSON("JSON", "JSON 对象");

    public static final String[] ARRAYS = Arrays.stream(values())
            .map(RiskEventFieldTypeEnum::getType).toArray(String[]::new);

    private final String type;
    private final String description;

    @Override
    public String[] array() {
        return ARRAYS;
    }

}
