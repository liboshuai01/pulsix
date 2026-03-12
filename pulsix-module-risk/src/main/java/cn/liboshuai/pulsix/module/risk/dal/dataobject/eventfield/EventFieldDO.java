package cn.liboshuai.pulsix.module.risk.dal.dataobject.eventfield;

import cn.liboshuai.pulsix.framework.mybatis.core.dataobject.BaseDO;
import cn.liboshuai.pulsix.framework.tenant.core.aop.TenantIgnore;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@TableName("event_field_def")
@TenantIgnore
@Data
@EqualsAndHashCode(callSuper = true)
public class EventFieldDO extends BaseDO {

    private Long id;

    private String sceneCode;

    private String eventCode;

    private String fieldCode;

    private String fieldName;

    private String fieldType;

    private String fieldPath;

    private Integer standardFieldFlag;

    private Integer requiredFlag;

    private Integer nullableFlag;

    private String defaultValue;

    private String sampleValue;

    private String description;

    private Integer sortNo;

    private Integer status;

}
