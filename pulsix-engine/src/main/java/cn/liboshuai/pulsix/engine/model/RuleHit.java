package cn.liboshuai.pulsix.engine.model;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.Map;

@Data
@NoArgsConstructor
public class RuleHit implements Serializable {

    private String ruleCode;

    private String ruleName;

    private Integer priority;

    private Boolean hit;

    private ActionType action;

    private Integer score;

    private String reason;

    private Map<String, Object> detail = new LinkedHashMap<>();

}
