package cn.liboshuai.pulsix.engine.model;

import cn.liboshuai.pulsix.engine.support.CollectionCopier;
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

    public void setRuleOrder(List<String> ruleOrder) {
        this.ruleOrder = CollectionCopier.copyList(ruleOrder);
    }

    public void setScoreBands(List<ScoreBandSpec> scoreBands) {
        this.scoreBands = CollectionCopier.copyList(scoreBands);
    }

}
