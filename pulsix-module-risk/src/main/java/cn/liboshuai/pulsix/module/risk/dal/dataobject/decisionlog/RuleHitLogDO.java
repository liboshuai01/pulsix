package cn.liboshuai.pulsix.module.risk.dal.dataobject.decisionlog;

import cn.liboshuai.pulsix.framework.mybatis.core.dataobject.BaseDO;
import cn.liboshuai.pulsix.framework.tenant.core.aop.TenantIgnore;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Map;

@TableName(value = "rule_hit_log", autoResultMap = true)
@TenantIgnore
@Data
@EqualsAndHashCode(callSuper = true)
public class RuleHitLogDO extends BaseDO {

    private Long id;

    private Long decisionId;

    private String ruleCode;

    private String ruleName;

    private Integer ruleOrderNo;

    private Integer hitFlag;

    private String hitReason;

    private Integer score;

    @TableField(typeHandler = JacksonTypeHandler.class)
    private Map<String, Object> hitValueJson;

}
