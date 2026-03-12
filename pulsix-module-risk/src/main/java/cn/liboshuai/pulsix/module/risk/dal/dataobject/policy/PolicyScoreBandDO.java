package cn.liboshuai.pulsix.module.risk.dal.dataobject.policy;

import cn.liboshuai.pulsix.framework.mybatis.core.dataobject.BaseDO;
import cn.liboshuai.pulsix.framework.tenant.core.aop.TenantIgnore;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@TableName("policy_score_band")
@TenantIgnore
@Data
@EqualsAndHashCode(callSuper = true)
public class PolicyScoreBandDO extends BaseDO {

    private Long id;

    private String sceneCode;

    private String policyCode;

    private Integer bandNo;

    private Integer minScore;

    private Integer maxScore;

    private String hitAction;

    private String hitReasonTemplate;

    private Integer enabledFlag;

}
