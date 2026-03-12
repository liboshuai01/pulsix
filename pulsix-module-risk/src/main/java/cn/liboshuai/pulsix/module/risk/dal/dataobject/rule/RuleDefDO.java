package cn.liboshuai.pulsix.module.risk.dal.dataobject.rule;

import cn.liboshuai.pulsix.framework.mybatis.core.dataobject.BaseDO;
import cn.liboshuai.pulsix.framework.tenant.core.aop.TenantIgnore;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@TableName("rule_def")
@TenantIgnore
@Data
@EqualsAndHashCode(callSuper = true)
public class RuleDefDO extends BaseDO {

    private Long id;

    private String sceneCode;

    private String ruleCode;

    private String ruleName;

    private String ruleType;

    private String engineType;

    private String exprContent;

    private Integer priority;

    private String hitAction;

    private Integer riskScore;

    private String hitReasonTemplate;

    private Integer status;

    private Integer version;

    private String description;

}
