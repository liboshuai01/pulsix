package cn.liboshuai.pulsix.module.risk.enums.list;

import cn.liboshuai.pulsix.framework.common.core.ArrayValuable;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

@Getter
@AllArgsConstructor
public enum RiskListSyncStatusEnum implements ArrayValuable<String> {

    PENDING("PENDING", "待同步"),
    SUCCESS("SUCCESS", "同步成功"),
    FAILED("FAILED", "同步失败");

    public static final String[] ARRAYS = Arrays.stream(values())
            .map(RiskListSyncStatusEnum::getType).toArray(String[]::new);

    private final String type;
    private final String description;

    @Override
    public String[] array() {
        return ARRAYS;
    }

}
