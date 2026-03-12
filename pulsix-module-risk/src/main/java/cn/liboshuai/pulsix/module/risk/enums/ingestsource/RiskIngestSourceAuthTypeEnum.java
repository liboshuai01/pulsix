package cn.liboshuai.pulsix.module.risk.enums.ingestsource;

import cn.liboshuai.pulsix.framework.common.core.ArrayValuable;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

@Getter
@AllArgsConstructor
public enum RiskIngestSourceAuthTypeEnum implements ArrayValuable<String> {

    NONE("NONE", "无需鉴权"),
    TOKEN("TOKEN", "Token"),
    HMAC("HMAC", "HMAC"),
    AKSK("AKSK", "AK/SK"),
    JWT("JWT", "JWT");

    public static final String[] ARRAYS = Arrays.stream(values())
            .map(RiskIngestSourceAuthTypeEnum::getType).toArray(String[]::new);

    private final String type;
    private final String description;

    @Override
    public String[] array() {
        return ARRAYS;
    }

}
