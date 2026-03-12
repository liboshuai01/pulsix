package cn.liboshuai.pulsix.engine.model;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
public class ScoreContribution implements Serializable {

    private String ruleCode;

    private String ruleName;

    private ActionType action;

    private Integer rawScore;

    private Integer scoreWeight;

    private Integer weightedScore;

    private Boolean stopOnHit;

    private String reason;

}
