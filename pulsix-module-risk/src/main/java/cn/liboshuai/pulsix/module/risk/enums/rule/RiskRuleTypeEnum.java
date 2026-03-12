package cn.liboshuai.pulsix.module.risk.enums.rule;

import cn.liboshuai.pulsix.framework.common.core.ArrayValuable;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

@Getter
@AllArgsConstructor
public enum RiskRuleTypeEnum implements ArrayValuable<String> {

    NORMAL("NORMAL", "普通规则"),
    TAG_ONLY("TAG_ONLY", "仅打标签规则"),
    MANUAL_REVIEW_HINT("MANUAL_REVIEW_HINT", "人工复核提示规则");

    public static final String[] ARRAYS = Arrays.stream(values())
            .map(RiskRuleTypeEnum::getType).toArray(String[]::new);

    private final String type;
    private final String description;

    @Override
    public String[] array() {
        return ARRAYS;
    }

}
