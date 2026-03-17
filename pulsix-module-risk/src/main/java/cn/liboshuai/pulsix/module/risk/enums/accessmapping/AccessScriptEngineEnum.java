package cn.liboshuai.pulsix.module.risk.enums.accessmapping;

import cn.liboshuai.pulsix.framework.common.core.ArrayValuable;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 接入映射脚本引擎枚举
 */
@Getter
@AllArgsConstructor
public enum AccessScriptEngineEnum implements ArrayValuable<String> {

    AVIATOR("AVIATOR", "Aviator"),
    GROOVY("GROOVY", "Groovy");

    public static final String[] ARRAYS = valuesAsArray();

    private final String type;
    private final String name;

    @Override
    public String[] array() {
        return ARRAYS;
    }

    private static String[] valuesAsArray() {
        AccessScriptEngineEnum[] values = values();
        String[] array = new String[values.length];
        for (int i = 0; i < values.length; i++) {
            array[i] = values[i].getType();
        }
        return array;
    }

}
