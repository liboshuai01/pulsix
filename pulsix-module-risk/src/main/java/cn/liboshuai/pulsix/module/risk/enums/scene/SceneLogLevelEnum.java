package cn.liboshuai.pulsix.module.risk.enums.scene;

import cn.liboshuai.pulsix.framework.common.core.ArrayValuable;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 场景日志级别枚举
 */
@Getter
@AllArgsConstructor
public enum SceneLogLevelEnum implements ArrayValuable<String> {

    FULL("FULL", "完整日志");

    public static final String[] ARRAYS = valuesAsArray();

    private final String level;
    private final String name;

    @Override
    public String[] array() {
        return ARRAYS;
    }

    private static String[] valuesAsArray() {
        SceneLogLevelEnum[] values = values();
        String[] array = new String[values.length];
        for (int i = 0; i < values.length; i++) {
            array[i] = values[i].getLevel();
        }
        return array;
    }

}
