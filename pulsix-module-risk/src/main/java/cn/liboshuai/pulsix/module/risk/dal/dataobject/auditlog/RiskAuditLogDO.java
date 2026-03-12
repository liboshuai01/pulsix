package cn.liboshuai.pulsix.module.risk.dal.dataobject.auditlog;

import cn.liboshuai.pulsix.framework.mybatis.core.dataobject.BaseDO;
import cn.liboshuai.pulsix.framework.tenant.core.aop.TenantIgnore;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;
import java.util.Map;

@TableName(value = "risk_audit_log", autoResultMap = true)
@TenantIgnore
@Data
@EqualsAndHashCode(callSuper = true)
public class RiskAuditLogDO extends BaseDO {

    private Long id;

    private String traceId;

    private String sceneCode;

    private Long operatorId;

    private String operatorName;

    private String bizType;

    private String bizCode;

    private String actionType;

    @TableField(typeHandler = JacksonTypeHandler.class)
    private Map<String, Object> beforeJson;

    @TableField(typeHandler = JacksonTypeHandler.class)
    private Map<String, Object> afterJson;

    private String remark;

    private LocalDateTime operateTime;

}
