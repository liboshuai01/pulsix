package cn.liboshuai.pulsix.module.risk.enums.eventmodel;

import cn.liboshuai.pulsix.framework.common.core.ArrayValuable;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 事件字段类型枚举
 */
@Getter
@AllArgsConstructor
public enum EventFieldTypeEnum implements ArrayValuable<String> {

    STRING("STRING", "字符串"),
    INTEGER("INTEGER", "整数"),
    LONG("LONG", "长整数"),
    DECIMAL("DECIMAL", "小数"),
    BOOLEAN("BOOLEAN", "布尔"),
    DATETIME("DATETIME", "日期时间"),
    JSON("JSON", "JSON");

    public static final String[] ARRAYS = valuesAsArray();

    private final String type;
    private final String name;

    @Override
    public String[] array() {
        return ARRAYS;
    }

    private static String[] valuesAsArray() {
        EventFieldTypeEnum[] values = values();
        String[] array = new String[values.length];
        for (int i = 0; i < values.length; i++) {
            array[i] = values[i].getType();
        }
        return array;
    }

}
