package cn.liboshuai.pulsix.engine.model;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
public class PolicyRuleRefSpec implements Serializable {

    private String ruleCode;

    private Integer orderNo;

    private Boolean enabled;

    private Integer scoreWeight;

    private Boolean stopOnHit;

    private String branchExpr;

}
