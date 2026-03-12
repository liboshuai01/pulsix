package cn.liboshuai.pulsix.module.risk.enums.rule;

import cn.liboshuai.pulsix.framework.common.core.ArrayValuable;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

@Getter
@AllArgsConstructor
public enum RiskRuleHitActionEnum implements ArrayValuable<String> {

    PASS("PASS", "放行"),
    REVIEW("REVIEW", "人工复核"),
    REJECT("REJECT", "拒绝"),
    LIMIT("LIMIT", "限流/限制"),
    TAG_ONLY("TAG_ONLY", "仅打标签");

    public static final String[] ARRAYS = Arrays.stream(values())
            .map(RiskRuleHitActionEnum::getType).toArray(String[]::new);

    private final String type;
    private final String description;

    @Override
    public String[] array() {
        return ARRAYS;
    }

}
