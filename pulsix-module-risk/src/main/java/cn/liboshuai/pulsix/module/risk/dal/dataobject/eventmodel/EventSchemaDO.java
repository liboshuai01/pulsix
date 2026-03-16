package cn.liboshuai.pulsix.module.risk.dal.dataobject.eventmodel;

import cn.liboshuai.pulsix.framework.common.enums.CommonStatusEnum;
import cn.liboshuai.pulsix.framework.mybatis.core.dataobject.BaseDO;
import cn.liboshuai.pulsix.framework.tenant.core.aop.TenantIgnore;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.util.Map;

/**
 * 事件模型定义 DO
 */
@TableName(value = "event_schema", autoResultMap = true)
@KeySequence("event_schema_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@TenantIgnore
public class EventSchemaDO extends BaseDO {

    /**
     * 主键
     */
    @TableId
    private Long id;
    /**
     * 场景编码
     */
    private String sceneCode;
    /**
     * 事件编码
     */
    private String eventCode;
    /**
     * 事件名称
     */
    private String eventName;
    /**
     * 事件类型
     */
    private String eventType;
    /**
     * 接入类型
     */
    private String sourceType;
    /**
     * 标准事件 Topic
     */
    private String topicName;
    /**
     * 样例事件
     */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private Map<String, Object> sampleEventJson;
    /**
     * 版本
     */
    private Integer version;
    /**
     * 状态
     *
     * 枚举 {@link CommonStatusEnum}
     */
    private Integer status;
    /**
     * 描述
     */
    private String description;

}
