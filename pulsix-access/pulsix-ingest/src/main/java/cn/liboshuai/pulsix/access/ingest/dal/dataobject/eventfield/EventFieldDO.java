package cn.liboshuai.pulsix.access.ingest.dal.dataobject.eventfield;

import cn.liboshuai.pulsix.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Map;

@TableName(value = "event_field_def", autoResultMap = true)
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

    @TableField(typeHandler = JacksonTypeHandler.class)
    private Map<String, Object> validationRuleJson;

    private String description;

    private Integer sortNo;

    private Integer status;

}
