package cn.liboshuai.pulsix.engine.runtime;

import cn.liboshuai.pulsix.engine.model.DerivedFeatureSpec;
import cn.liboshuai.pulsix.engine.model.LookupFeatureSpec;
import cn.liboshuai.pulsix.engine.model.PolicySpec;
import cn.liboshuai.pulsix.engine.model.RuntimeHints;
import cn.liboshuai.pulsix.engine.model.RuleSpec;
import cn.liboshuai.pulsix.engine.model.SceneSnapshot;
import cn.liboshuai.pulsix.engine.model.StreamFeatureSpec;
import cn.liboshuai.pulsix.engine.script.CompiledScript;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@Data
@NoArgsConstructor
public class CompiledSceneRuntime implements Serializable {

    private SceneSnapshot snapshot;

    private List<CompiledStreamFeature> streamFeatures = new ArrayList<>();

    private List<CompiledLookupFeature> lookupFeatures = new ArrayList<>();

    private List<CompiledDerivedFeature> orderedDerivedFeatures = new ArrayList<>();

    private List<CompiledRule> orderedRules = new ArrayList<>();

    private PolicySpec policy;

    private StreamFeatureRoutingPlan streamFeatureRoutingPlan = new StreamFeatureRoutingPlan();

    public String sceneCode() {
        return snapshot.getSceneCode();
    }

    public Integer version() {
        return snapshot.getVersion();
    }

    public RuntimeHints runtimeHints() {
        return snapshot == null ? null : snapshot.getRuntimeHints();
    }

    public Integer maxRuleExecutionCount() {
        RuntimeHints hints = runtimeHints();
        return hints == null ? null : hints.getMaxRuleExecutionCount();
    }

    public boolean allowGroovy() {
        RuntimeHints hints = runtimeHints();
        return hints == null || !Boolean.FALSE.equals(hints.getAllowGroovy());
    }

    public boolean needFullDecisionLog() {
        RuntimeHints hints = runtimeHints();
        return hints == null || !Boolean.FALSE.equals(hints.getNeedFullDecisionLog());
    }

    public boolean hasMixedStreamFeatureRouting() {
        return streamFeatureRoutingPlan != null && streamFeatureRoutingPlan.hasMultipleGroups();
    }

    public List<String> featureCodes() {
        List<String> codes = new ArrayList<>();
        streamFeatures.forEach(feature -> codes.add(feature.getSpec().getCode()));
        lookupFeatures.forEach(feature -> codes.add(feature.getSpec().getCode()));
        orderedDerivedFeatures.forEach(feature -> codes.add(feature.getSpec().getCode()));
        return codes;
    }

    @Data
    @NoArgsConstructor
    public static class CompiledStreamFeature implements Serializable {

        private StreamFeatureSpec spec;

        private CompiledScript entityKeyScript;

        private CompiledScript valueScript;

        private CompiledScript filterScript;

        private long windowSizeMs;

        private long windowSlideMs;

        private long ttlMs;

        private long retentionMs;

    }

    @Data
    @NoArgsConstructor
    public static class CompiledLookupFeature implements Serializable {

        private LookupFeatureSpec spec;

        private CompiledScript keyScript;

    }

    @Data
    @NoArgsConstructor
    public static class CompiledDerivedFeature implements Serializable {

        private DerivedFeatureSpec spec;

        private CompiledScript expression;

    }

    @Data
    @NoArgsConstructor
    public static class CompiledRule implements Serializable {

        private RuleSpec spec;

        private CompiledScript condition;

    }

    @Data
    @NoArgsConstructor
    public static class StreamFeatureRoutingPlan implements Serializable {

        private String sceneCode;

        private List<StreamFeatureGroupPlan> groups = new ArrayList<>();

        public boolean isEmpty() {
            return groups == null || groups.isEmpty();
        }

        public boolean hasMultipleGroups() {
            return groups != null && groups.size() > 1;
        }

        public List<String> featureCodes() {
            List<String> codes = new ArrayList<>();
            if (groups == null) {
                return codes;
            }
            for (StreamFeatureGroupPlan group : groups) {
                if (group == null || group.getFeatureCodes() == null) {
                    continue;
                }
                codes.addAll(group.getFeatureCodes());
            }
            return codes;
        }
    }

    @Data
    @NoArgsConstructor
    public static class StreamFeatureGroupPlan implements Serializable {

        private String sceneCode;

        private String entityType;

        private String entityKeyExpr;

        private List<String> sourceEventTypes = new ArrayList<>();

        private List<String> featureCodes = new ArrayList<>();

        public String groupKey() {
            String normalizedSceneCode = sceneCode == null ? "scene:default" : sceneCode.trim();
            String normalizedEntityType = entityType == null ? "entity:unknown" : entityType.trim();
            String normalizedEntityKeyExpr = entityKeyExpr == null ? "expr:unknown" : entityKeyExpr.trim();
            String normalizedSourceEventTypes = sourceEventTypes == null || sourceEventTypes.isEmpty()
                    ? "event:*"
                    : String.join(",", new LinkedHashSet<>(sourceEventTypes));
            return normalizedSceneCode + '|' + normalizedEntityType + '|' + normalizedEntityKeyExpr + '|' + normalizedSourceEventTypes;
        }

        public boolean sameDimension(StreamFeatureGroupPlan other) {
            if (other == null) {
                return false;
            }
            return Objects.equals(entityType, other.entityType)
                    && Objects.equals(entityKeyExpr, other.entityKeyExpr)
                    && Objects.equals(normalizeSourceEventTypes(sourceEventTypes), normalizeSourceEventTypes(other.sourceEventTypes));
        }

        private Set<String> normalizeSourceEventTypes(List<String> rawSourceEventTypes) {
            LinkedHashSet<String> normalized = new LinkedHashSet<>();
            if (rawSourceEventTypes == null) {
                return normalized;
            }
            for (String sourceEventType : rawSourceEventTypes) {
                if (sourceEventType == null || sourceEventType.isBlank()) {
                    continue;
                }
                normalized.add(sourceEventType.trim());
            }
            return normalized;
        }
    }

}
