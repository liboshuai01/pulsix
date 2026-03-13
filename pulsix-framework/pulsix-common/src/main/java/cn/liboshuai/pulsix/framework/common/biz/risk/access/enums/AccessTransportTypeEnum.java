package cn.liboshuai.pulsix.framework.common.biz.risk.access.enums;

import cn.liboshuai.pulsix.framework.common.core.ArrayValuable;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

@Getter
@AllArgsConstructor
public enum AccessTransportTypeEnum implements ArrayValuable<String> {

    HTTP("HTTP", "HTTP"),
    BEACON("BEACON", "Beacon"),
    SDK("SDK", "SDK");

    public static final String[] ARRAYS = Arrays.stream(values())
            .map(AccessTransportTypeEnum::getType).toArray(String[]::new);

    private final String type;
    private final String description;

    @Override
    public String[] array() {
        return ARRAYS;
    }

}
