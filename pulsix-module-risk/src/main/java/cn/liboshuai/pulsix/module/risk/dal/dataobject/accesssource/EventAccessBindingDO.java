package cn.liboshuai.pulsix.module.risk.dal.dataobject.accesssource;

import cn.liboshuai.pulsix.framework.tenant.core.aop.TenantIgnore;
import cn.liboshuai.pulsix.module.risk.dal.dataobject.RiskBaseDO;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.util.Map;

/**
 * 事件接入绑定 DO
 */
@TableName(value = "event_access_binding", autoResultMap = true)
@KeySequence("event_access_binding_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@TenantIgnore
public class EventAccessBindingDO extends RiskBaseDO {

    @TableId
    private Long id;

    private String eventCode;

    private String sourceCode;

    @TableField(value = "raw_sample_json", typeHandler = JacksonTypeHandler.class)
    private Map<String, Object> rawSampleJson;

    @TableField(value = "sample_headers_json", typeHandler = JacksonTypeHandler.class)
    private Map<String, Object> sampleHeadersJson;

    private String description;

    @TableField(exist = false)
    private String sceneCode;

    @TableField(exist = false)
    private String eventName;

    @TableField(exist = false)
    private String sourceName;

    @TableField(exist = false)
    private String sourceType;

    @TableField(exist = false)
    private String topicName;

    @TableField(exist = false)
    private Integer rawFieldCount;

    @TableField(exist = false)
    private Integer mappingRuleCount;

}
