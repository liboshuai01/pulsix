package cn.liboshuai.pulsix.engine.model;

import cn.liboshuai.pulsix.engine.flink.typeinfo.EngineTypeInfoFactories;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.flink.api.common.typeinfo.TypeInfo;

import java.io.Serializable;
import java.util.List;

@Data
@NoArgsConstructor
@TypeInfo(EngineTypeInfoFactories.PolicySpecTypeInfoFactory.class)
public class PolicySpec implements Serializable {

    private String policyCode;

    private String policyName;

    private DecisionMode decisionMode;

    private ActionType defaultAction;

    private List<String> ruleOrder;

    private List<ScoreBandSpec> scoreBands;

}
