package cn.liboshuai.pulsix.module.risk.dal.dataobject.eventsample;

import cn.liboshuai.pulsix.framework.mybatis.core.dataobject.BaseDO;
import cn.liboshuai.pulsix.framework.tenant.core.aop.TenantIgnore;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Map;

@TableName(value = "event_sample", autoResultMap = true)
@TenantIgnore
@Data
@EqualsAndHashCode(callSuper = true)
public class EventSampleDO extends BaseDO {

    private Long id;

    private String sceneCode;

    private String eventCode;

    private String sampleCode;

    private String sampleName;

    private String sampleType;

    private String sourceCode;

    @TableField(typeHandler = JacksonTypeHandler.class)
    private Map<String, Object> sampleJson;

    private String description;

    private Integer sortNo;

    private Integer status;

}
