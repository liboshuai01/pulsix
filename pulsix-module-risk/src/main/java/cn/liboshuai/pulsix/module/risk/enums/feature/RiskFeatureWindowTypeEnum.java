package cn.liboshuai.pulsix.module.risk.enums.feature;

import cn.liboshuai.pulsix.framework.common.core.ArrayValuable;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

@Getter
@AllArgsConstructor
public enum RiskFeatureWindowTypeEnum implements ArrayValuable<String> {

    TUMBLING("TUMBLING", "滚动窗口"),
    SLIDING("SLIDING", "滑动窗口"),
    NONE("NONE", "无窗口 / 最新值");

    public static final String[] ARRAYS = Arrays.stream(values())
            .map(RiskFeatureWindowTypeEnum::getType).toArray(String[]::new);

    private final String type;
    private final String description;

    @Override
    public String[] array() {
        return ARRAYS;
    }

}
