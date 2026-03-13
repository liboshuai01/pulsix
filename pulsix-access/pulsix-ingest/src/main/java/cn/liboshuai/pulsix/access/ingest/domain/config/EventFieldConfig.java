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
public class EventFieldConfig {

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

    @Builder.Default
    private Map<String, Object> validationRuleJson = new LinkedHashMap<>();

    private String description;

    private Integer sortNo;

    private Integer status;

}
