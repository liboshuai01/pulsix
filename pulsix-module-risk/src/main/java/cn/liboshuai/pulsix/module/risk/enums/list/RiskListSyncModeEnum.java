package cn.liboshuai.pulsix.module.risk.enums.list;

import cn.liboshuai.pulsix.framework.common.core.ArrayValuable;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

@Getter
@AllArgsConstructor
public enum RiskListSyncModeEnum implements ArrayValuable<String> {

    FULL("FULL", "全量同步"),
    INCREMENTAL("INCREMENTAL", "增量同步");

    public static final String[] ARRAYS = Arrays.stream(values())
            .map(RiskListSyncModeEnum::getType).toArray(String[]::new);

    private final String type;
    private final String description;

    @Override
    public String[] array() {
        return ARRAYS;
    }

}
