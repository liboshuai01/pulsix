package cn.liboshuai.pulsix.module.risk.dal.dataobject.eventschema;

import cn.liboshuai.pulsix.framework.mybatis.core.dataobject.BaseDO;
import cn.liboshuai.pulsix.framework.tenant.core.aop.TenantIgnore;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@TableName("event_schema")
@TenantIgnore
@Data
@EqualsAndHashCode(callSuper = true)
public class EventSchemaDO extends BaseDO {

    private Long id;

    private String sceneCode;

    private String eventCode;

    private String eventName;

    private String eventType;

    private String sourceType;

    private String rawTopicName;

    private String standardTopicName;

    private Integer version;

    private Integer status;

    private String description;

}
