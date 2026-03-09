package cn.liboshuai.pulsix.engine.model;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

@Data
@NoArgsConstructor
public class PolicySpec implements Serializable {

    private String policyCode;

    private String policyName;

    private DecisionMode decisionMode;

    private ActionType defaultAction;

    private List<String> ruleOrder;

    private List<ScoreBandSpec> scoreBands;

}
