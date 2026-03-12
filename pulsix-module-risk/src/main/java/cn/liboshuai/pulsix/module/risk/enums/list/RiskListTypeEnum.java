package cn.liboshuai.pulsix.module.risk.enums.list;

import cn.liboshuai.pulsix.framework.common.core.ArrayValuable;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

@Getter
@AllArgsConstructor
public enum RiskListTypeEnum implements ArrayValuable<String> {

    BLACK("BLACK", "黑名单"),
    WHITE("WHITE", "白名单"),
    WATCH("WATCH", "观察名单");

    public static final String[] ARRAYS = Arrays.stream(values())
            .map(RiskListTypeEnum::getType).toArray(String[]::new);

    private final String type;
    private final String description;

    @Override
    public String[] array() {
        return ARRAYS;
    }

}
