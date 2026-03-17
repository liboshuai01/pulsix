package cn.liboshuai.pulsix.module.risk.dal.dataobject.accessmapping;

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
 * 接入映射原始字段定义 DO
 */
@TableName(value = "event_access_raw_field_def", autoResultMap = true)
@KeySequence("event_access_raw_field_def_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@TenantIgnore
public class EventAccessRawFieldDefDO extends BaseDO {

    @TableId
    private Long id;

    private Long bindingId;

    private String fieldName;

    private String fieldLabel;

    private String fieldPath;

    /**
     * 枚举 {@link EventFieldTypeEnum}
     */
    private String fieldType;

    private Integer requiredFlag;

    private String sampleValue;

    private String description;

    private Integer sortNo;

}
