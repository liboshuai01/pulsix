package cn.liboshuai.pulsix.module.risk.dal.dataobject.decisionlog;

import cn.liboshuai.pulsix.framework.mybatis.core.dataobject.BaseDO;
import cn.liboshuai.pulsix.framework.tenant.core.aop.TenantIgnore;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@TableName(value = "decision_log", autoResultMap = true)
@TenantIgnore
@Data
@EqualsAndHashCode(callSuper = true)
public class DecisionLogDO extends BaseDO {

    private Long id;

    private String traceId;

    private String eventId;

    private String sceneCode;

    private String sourceCode;

    private String eventCode;

    private String entityId;

    private String policyCode;

    private String finalAction;

    private Integer finalScore;

    private Integer versionNo;

    private Long latencyMs;

    private LocalDateTime eventTime;

    @TableField(typeHandler = JacksonTypeHandler.class)
    private Map<String, Object> inputJson;

    @TableField(typeHandler = JacksonTypeHandler.class)
    private Map<String, Object> featureSnapshotJson;

    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<Map<String, Object>> hitRulesJson;

    @TableField(typeHandler = JacksonTypeHandler.class)
    private Map<String, Object> decisionDetailJson;

}
