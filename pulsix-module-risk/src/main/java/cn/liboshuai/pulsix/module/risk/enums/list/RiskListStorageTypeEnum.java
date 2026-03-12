package cn.liboshuai.pulsix.module.risk.enums.list;

import cn.liboshuai.pulsix.framework.common.core.ArrayValuable;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

@Getter
@AllArgsConstructor
public enum RiskListStorageTypeEnum implements ArrayValuable<String> {

    REDIS_SET("REDIS_SET", "Redis 前缀 Key"),
    REDIS_HASH("REDIS_HASH", "Redis Hash");

    public static final String[] ARRAYS = Arrays.stream(values())
            .map(RiskListStorageTypeEnum::getType).toArray(String[]::new);

    private final String type;
    private final String description;

    @Override
    public String[] array() {
        return ARRAYS;
    }

}
