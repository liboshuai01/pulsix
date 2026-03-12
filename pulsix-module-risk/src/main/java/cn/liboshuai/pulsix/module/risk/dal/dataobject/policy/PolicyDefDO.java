package cn.liboshuai.pulsix.module.risk.dal.dataobject.policy;

import cn.liboshuai.pulsix.framework.mybatis.core.dataobject.BaseDO;
import cn.liboshuai.pulsix.framework.tenant.core.aop.TenantIgnore;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@TableName("policy_def")
@TenantIgnore
@Data
@EqualsAndHashCode(callSuper = true)
public class PolicyDefDO extends BaseDO {

    private Long id;

    private String sceneCode;

    private String policyCode;

    private String policyName;

    private String decisionMode;

    private String defaultAction;

    private String scoreCalcMode;

    private Integer status;

    private Integer version;

    private String description;

}
