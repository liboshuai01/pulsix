package cn.liboshuai.pulsix.module.risk.enums.accessmapping;

import cn.liboshuai.pulsix.framework.common.core.ArrayValuable;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 接入映射规则类型枚举
 */
@Getter
@AllArgsConstructor
public enum AccessMappingTypeEnum implements ArrayValuable<String> {

    SOURCE_FIELD("SOURCE_FIELD", "源字段"),
    CONSTANT("CONSTANT", "常量"),
    SCRIPT("SCRIPT", "脚本");

    public static final String[] ARRAYS = valuesAsArray();

    private final String type;
    private final String name;

    @Override
    public String[] array() {
        return ARRAYS;
    }

    private static String[] valuesAsArray() {
        AccessMappingTypeEnum[] values = values();
        String[] array = new String[values.length];
        for (int i = 0; i < values.length; i++) {
            array[i] = values[i].getType();
        }
        return array;
    }

}
