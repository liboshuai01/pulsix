package cn.liboshuai.pulsix.module.risk.dal.dataobject.eventmodel;

import cn.liboshuai.pulsix.framework.mybatis.core.dataobject.BaseDO;
import cn.liboshuai.pulsix.framework.tenant.core.aop.TenantIgnore;
import cn.liboshuai.pulsix.module.risk.enums.eventmodel.EventFieldTypeEnum;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

/**
 * 事件字段定义 DO
 */
@TableName(value = "event_field_def", autoResultMap = true)
@KeySequence("event_field_def_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@TenantIgnore
public class EventFieldDefDO extends BaseDO {

    /**
     * 主键
     */
    @TableId
    private Long id;
    /**
     * 事件编码
     */
    private String eventCode;
    /**
     * 字段名
     */
    private String fieldName;
    /**
     * 字段显示名
     */
    private String fieldLabel;
    /**
     * 字段类型
     *
     * 枚举 {@link EventFieldTypeEnum}
     */
    private String fieldType;
    /**
     * 是否必填
     */
    private Integer requiredFlag;
    /**
     * 默认值
     */
    private String defaultValue;
    /**
     * 样例值
     */
    private String sampleValue;
    /**
     * 描述
     */
    private String description;
    /**
     * 排序
     */
    private Integer sortNo;

}
