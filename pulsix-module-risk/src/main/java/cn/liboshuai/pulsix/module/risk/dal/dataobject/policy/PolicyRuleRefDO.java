package cn.liboshuai.pulsix.module.risk.dal.dataobject.policy;

import cn.liboshuai.pulsix.framework.mybatis.core.dataobject.BaseDO;
import cn.liboshuai.pulsix.framework.tenant.core.aop.TenantIgnore;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@TableName("policy_rule_ref")
@TenantIgnore
@Data
@EqualsAndHashCode(callSuper = true)
public class PolicyRuleRefDO extends BaseDO {

    private Long id;

    private String sceneCode;

    private String policyCode;

    private String ruleCode;

    private Integer orderNo;

    private Integer enabledFlag;

    private String branchExpr;

    private Integer scoreWeight;

    private Integer stopOnHit;

}
