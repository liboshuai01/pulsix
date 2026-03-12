package cn.liboshuai.pulsix.module.risk.enums.list;

import cn.liboshuai.pulsix.framework.common.core.ArrayValuable;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

@Getter
@AllArgsConstructor
public enum RiskListMatchTypeEnum implements ArrayValuable<String> {

    USER("USER", "用户"),
    DEVICE("DEVICE", "设备"),
    IP("IP", "IP"),
    MOBILE("MOBILE", "手机号"),
    CARD("CARD", "卡号");

    public static final String[] ARRAYS = Arrays.stream(values())
            .map(RiskListMatchTypeEnum::getType).toArray(String[]::new);

    private final String type;
    private final String description;

    @Override
    public String[] array() {
        return ARRAYS;
    }

}
