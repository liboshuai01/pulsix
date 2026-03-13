package cn.liboshuai.pulsix.access.ingest.domain.config;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.LinkedHashMap;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IngestMappingConfig {

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

    @Builder.Default
    private Map<String, Object> cleanRuleJson = new LinkedHashMap<>();

    private Integer sortNo;

    private Integer status;

}
