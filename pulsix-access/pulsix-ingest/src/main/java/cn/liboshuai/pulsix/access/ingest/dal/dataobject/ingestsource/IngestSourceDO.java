package cn.liboshuai.pulsix.access.ingest.dal.dataobject.ingestsource;

import cn.liboshuai.pulsix.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;
import java.util.Map;

@TableName(value = "ingest_source", autoResultMap = true)
@Data
@EqualsAndHashCode(callSuper = true)
public class IngestSourceDO extends BaseDO {

    private Long id;

    private String sourceCode;

    private String sourceName;

    private String sourceType;

    private String authType;

    @TableField(typeHandler = JacksonTypeHandler.class)
    private Map<String, Object> authConfigJson;

    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<String> sceneScopeJson;

    private String standardTopicName;

    private String errorTopicName;

    private Integer rateLimitQps;

    private Integer status;

    private String description;

}
