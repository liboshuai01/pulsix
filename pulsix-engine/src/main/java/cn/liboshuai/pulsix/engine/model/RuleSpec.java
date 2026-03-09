package cn.liboshuai.pulsix.engine.model;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

@Data
@NoArgsConstructor
public class RuleSpec implements Serializable {

    private String code;

    private String name;

    private EngineType engineType;

    private Integer priority;

    private String whenExpr;

    private List<String> dependsOn;

    private ActionType hitAction;

    private Integer riskScore;

    private String hitReasonTemplate;

    private Boolean enabled;

}
