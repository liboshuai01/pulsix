package cn.liboshuai.pulsix.access.ingest.dal.dataobject.ingestmapping;

import cn.liboshuai.pulsix.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Map;

@TableName(value = "ingest_mapping_def", autoResultMap = true)
@Data
@EqualsAndHashCode(callSuper = true)
public class IngestMappingDO extends BaseDO {

    private Long id;

    private String sourceCode;

    private String sceneCode;

    private String eventCode;

    private String sourceFieldPath;

    private String targetFieldCode;

    private String targetFieldName;

    private String transformType;

    private String transformExpr;

    private String defaultValue;

    private Integer requiredFlag;

    @TableField(typeHandler = JacksonTypeHandler.class)
    private Map<String, Object> cleanRuleJson;

    private Integer sortNo;

    private Integer status;

}
