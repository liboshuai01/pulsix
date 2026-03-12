package cn.liboshuai.pulsix.module.risk.enums.feature;

import cn.liboshuai.pulsix.framework.common.core.ArrayValuable;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

@Getter
@AllArgsConstructor
public enum RiskFeatureAggTypeEnum implements ArrayValuable<String> {

    COUNT("COUNT", "计数"),
    SUM("SUM", "求和"),
    MAX("MAX", "最大值"),
    LATEST("LATEST", "最新值"),
    DISTINCT_COUNT("DISTINCT_COUNT", "去重计数");

    public static final String[] ARRAYS = Arrays.stream(values())
            .map(RiskFeatureAggTypeEnum::getType).toArray(String[]::new);

    private final String type;
    private final String description;

    @Override
    public String[] array() {
        return ARRAYS;
    }

}
