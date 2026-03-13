package cn.liboshuai.pulsix.framework.common.biz.risk.access.normalize;

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
public class StandardEventMappingDefinition {

    private String sourceFieldPath;

    private String targetFieldCode;

    private String transformType;

    private String transformExpr;

    private String defaultValue;

    @Builder.Default
    private Map<String, Object> cleanRuleJson = new LinkedHashMap<>();

}
