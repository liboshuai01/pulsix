package cn.liboshuai.pulsix.module.risk.enums.scene;

import cn.liboshuai.pulsix.framework.common.core.ArrayValuable;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

@Getter
@AllArgsConstructor
public enum RiskSceneAccessModeEnum implements ArrayValuable<String> {

    HTTP("HTTP", "HTTP 接入"),
    BEACON("BEACON", "Beacon 接入"),
    SDK("SDK", "SDK 接入"),
    MIXED("MIXED", "混合接入");

    public static final String[] ARRAYS = Arrays.stream(values())
            .map(RiskSceneAccessModeEnum::getMode).toArray(String[]::new);

    private final String mode;
    private final String description;

    @Override
    public String[] array() {
        return ARRAYS;
    }

}

