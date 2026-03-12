package cn.liboshuai.pulsix.module.risk.enums.eventschema;

import cn.liboshuai.pulsix.framework.common.core.ArrayValuable;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

@Getter
@AllArgsConstructor
public enum RiskEventSchemaTypeEnum implements ArrayValuable<String> {

    BUSINESS("BUSINESS", "业务事件"),
    CALLBACK("CALLBACK", "回调事件"),
    TEST("TEST", "测试事件");

    public static final String[] ARRAYS = Arrays.stream(values())
            .map(RiskEventSchemaTypeEnum::getType).toArray(String[]::new);

    private final String type;
    private final String description;

    @Override
    public String[] array() {
        return ARRAYS;
    }

}
