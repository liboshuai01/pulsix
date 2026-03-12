package cn.liboshuai.pulsix.module.risk.enums.eventsample;

import cn.liboshuai.pulsix.framework.common.core.ArrayValuable;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

@Getter
@AllArgsConstructor
public enum RiskEventSampleTypeEnum implements ArrayValuable<String> {

    RAW("RAW", "原始报文"),
    STANDARD("STANDARD", "标准事件"),
    SIMULATION("SIMULATION", "仿真输入");

    public static final String[] ARRAYS = Arrays.stream(values())
            .map(RiskEventSampleTypeEnum::getType).toArray(String[]::new);

    private final String type;
    private final String description;

    @Override
    public String[] array() {
        return ARRAYS;
    }

}
