package cn.liboshuai.pulsix.module.risk.enums.scene;

import cn.liboshuai.pulsix.framework.common.core.ArrayValuable;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

@Getter
@AllArgsConstructor
public enum RiskSceneTypeEnum implements ArrayValuable<String> {

    GENERAL("GENERAL", "通用风控"),
    ACCOUNT_SECURITY("ACCOUNT_SECURITY", "账号安全"),
    TRADE_SECURITY("TRADE_SECURITY", "交易风控");

    public static final String[] ARRAYS = Arrays.stream(values())
            .map(RiskSceneTypeEnum::getType).toArray(String[]::new);

    private final String type;
    private final String description;

    @Override
    public String[] array() {
        return ARRAYS;
    }

}

