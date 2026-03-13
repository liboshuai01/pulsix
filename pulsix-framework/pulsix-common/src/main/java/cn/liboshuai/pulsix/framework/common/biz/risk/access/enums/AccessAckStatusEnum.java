package cn.liboshuai.pulsix.framework.common.biz.risk.access.enums;

import cn.liboshuai.pulsix.framework.common.core.ArrayValuable;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

@Getter
@AllArgsConstructor
public enum AccessAckStatusEnum implements ArrayValuable<String> {

    ACCEPTED("ACCEPTED", "已接收"),
    REJECTED("REJECTED", "已拒绝"),
    RETRY("RETRY", "建议重试");

    public static final String[] ARRAYS = Arrays.stream(values())
            .map(AccessAckStatusEnum::getStatus).toArray(String[]::new);

    private final String status;
    private final String description;

    @Override
    public String[] array() {
        return ARRAYS;
    }

}
