package cn.liboshuai.pulsix.module.risk.enums.feature;

import cn.liboshuai.pulsix.framework.common.core.ArrayValuable;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

@Getter
@AllArgsConstructor
public enum RiskFeatureTypeEnum implements ArrayValuable<String> {

    STREAM("STREAM", "流式特征"),
    LOOKUP("LOOKUP", "查询特征"),
    DERIVED("DERIVED", "派生特征");

    public static final String[] ARRAYS = Arrays.stream(values())
            .map(RiskFeatureTypeEnum::getType).toArray(String[]::new);

    private final String type;
    private final String description;

    @Override
    public String[] array() {
        return ARRAYS;
    }

}
