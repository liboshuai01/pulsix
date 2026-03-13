package cn.liboshuai.pulsix.module.risk.dal.dataobject.ingesterror;

import cn.liboshuai.pulsix.framework.mybatis.core.dataobject.BaseDO;
import cn.liboshuai.pulsix.framework.tenant.core.aop.TenantIgnore;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;
import java.util.Map;

@TableName(value = "ingest_error_log", autoResultMap = true)
@TenantIgnore
@Data
@EqualsAndHashCode(callSuper = true)
public class IngestErrorLogDO extends BaseDO {

    private Long id;

    private String traceId;

    private String sourceCode;

    private String sceneCode;

    private String eventCode;

    private String rawEventId;

    private String ingestStage;

    private String errorCode;

    private String errorMessage;

    @TableField(typeHandler = JacksonTypeHandler.class)
    private Map<String, Object> rawPayloadJson;

    @TableField(typeHandler = JacksonTypeHandler.class)
    private Map<String, Object> standardPayloadJson;

    private String errorTopicName;

    private String reprocessStatus;

    private LocalDateTime occurTime;

    private Integer status;

}
