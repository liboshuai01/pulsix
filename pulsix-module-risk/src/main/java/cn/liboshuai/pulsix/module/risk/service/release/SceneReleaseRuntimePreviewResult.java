package cn.liboshuai.pulsix.module.risk.service.release;

import cn.hutool.core.util.StrUtil;
import lombok.Data;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Data
public class SceneReleaseRuntimePreviewResult {

    private boolean valid;

    private String stage;

    private String errorCode;

    private String featureCode;

    private String ruleCode;

    private String engineType;

    private String message;

    private int streamFeatureCount;

    private int lookupFeatureCount;

    private int derivedFeatureCount;

    private int orderedRuleCount;

    private int routingGroupCount;

    private boolean mixedStreamFeatureRouting;

    private Map<String, Object> runtimeHints = new LinkedHashMap<>();

    private List<String> featureCodes = new ArrayList<>();

    private List<String> orderedDerivedFeatureCodes = new ArrayList<>();

    private List<String> orderedRuleCodes = new ArrayList<>();

    private List<Map<String, Object>> streamFeatureGroups = new ArrayList<>();

    public String summaryMessage() {
        return valid ? "运行时快照编译通过" : StrUtil.blankToDefault(message, "运行时快照编译失败");
    }

    public Map<String, Object> toValidationPreviewMap() {
        Map<String, Object> preview = new LinkedHashMap<>();
        preview.put("valid", valid);
        preview.put("message", summaryMessage());
        preview.put("stage", stage);
        preview.put("errorCode", errorCode);
        preview.put("featureCode", featureCode);
        preview.put("ruleCode", ruleCode);
        preview.put("engineType", engineType);
        preview.put("streamFeatureCount", streamFeatureCount);
        preview.put("lookupFeatureCount", lookupFeatureCount);
        preview.put("derivedFeatureCount", derivedFeatureCount);
        preview.put("orderedRuleCount", orderedRuleCount);
        preview.put("routingGroupCount", routingGroupCount);
        preview.put("mixedStreamFeatureRouting", mixedStreamFeatureRouting);
        preview.put("orderedDerivedFeatureCodes", orderedDerivedFeatureCodes);
        preview.put("orderedRuleCodes", orderedRuleCodes);
        return preview;
    }

    public Map<String, Object> toDependencyPreviewMap() {
        Map<String, Object> preview = new LinkedHashMap<>();
        preview.put("valid", valid);
        preview.put("message", summaryMessage());
        preview.put("runtimeHints", runtimeHints);
        preview.put("featureCodes", featureCodes);
        preview.put("orderedDerivedFeatureCodes", orderedDerivedFeatureCodes);
        preview.put("orderedRuleCodes", orderedRuleCodes);
        preview.put("streamFeatureGroups", streamFeatureGroups);
        preview.put("mixedStreamFeatureRouting", mixedStreamFeatureRouting);
        return preview;
    }

}
