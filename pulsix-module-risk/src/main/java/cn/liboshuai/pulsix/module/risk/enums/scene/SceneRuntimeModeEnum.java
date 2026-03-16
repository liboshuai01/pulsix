package cn.liboshuai.pulsix.module.risk.enums.scene;

import cn.liboshuai.pulsix.framework.common.core.ArrayValuable;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 场景运行模式枚举
 */
@Getter
@AllArgsConstructor
public enum SceneRuntimeModeEnum implements ArrayValuable<String> {

    ASYNC_DECISION("ASYNC_DECISION", "异步决策");

    public static final String[] ARRAYS = valuesAsArray();

    private final String mode;
    private final String name;

    @Override
    public String[] array() {
        return ARRAYS;
    }

    private static String[] valuesAsArray() {
        SceneRuntimeModeEnum[] values = values();
        String[] array = new String[values.length];
        for (int i = 0; i < values.length; i++) {
            array[i] = values[i].getMode();
        }
        return array;
    }

}
