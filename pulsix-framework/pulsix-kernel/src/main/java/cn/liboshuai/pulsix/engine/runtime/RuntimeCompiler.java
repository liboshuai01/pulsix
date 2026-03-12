package cn.liboshuai.pulsix.engine.runtime;

import cn.liboshuai.pulsix.engine.model.DerivedFeatureSpec;
import cn.liboshuai.pulsix.engine.model.EngineErrorCodes;
import cn.liboshuai.pulsix.engine.model.EngineType;
import cn.liboshuai.pulsix.engine.model.LookupFeatureSpec;
import cn.liboshuai.pulsix.engine.model.PolicyRuleRefSpec;
import cn.liboshuai.pulsix.engine.model.PolicySpec;
import cn.liboshuai.pulsix.engine.model.RuleSpec;
import cn.liboshuai.pulsix.engine.model.RuntimeHints;
import cn.liboshuai.pulsix.engine.model.SceneSnapshot;
import cn.liboshuai.pulsix.engine.model.StreamFeatureSpec;
import cn.liboshuai.pulsix.engine.model.WindowType;
import cn.liboshuai.pulsix.engine.script.CompiledScript;
import cn.liboshuai.pulsix.engine.script.ScriptCompiler;
import cn.liboshuai.pulsix.engine.support.DurationParser;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public class RuntimeCompiler {

    private final ScriptCompiler scriptCompiler;

    public RuntimeCompiler(ScriptCompiler scriptCompiler) {
        this.scriptCompiler = scriptCompiler;
    }

    public CompiledSceneRuntime compile(SceneSnapshot snapshot) {
        CompiledSceneRuntime runtime = new CompiledSceneRuntime();
        runtime.setSnapshot(snapshot);
        runtime.setPolicy(snapshot.getPolicy());

        List<StreamFeatureSpec> streamFeatures = defaultList(snapshot.getStreamFeatures());
        for (StreamFeatureSpec spec : streamFeatures) {
            try {
                CompiledSceneRuntime.CompiledStreamFeature compiledFeature = new CompiledSceneRuntime.CompiledStreamFeature();
                compiledFeature.setSpec(spec);
                compiledFeature.setEntityKeyScript(compileAviator(spec.getEntityKeyExpr()));
                compiledFeature.setValueScript(compileAviator(defaultIfBlank(spec.getValueExpr(), "1")));
                compiledFeature.setFilterScript(compileAviator(defaultIfBlank(spec.getFilterExpr(), "true")));
                compiledFeature.setWindowSizeMs(resolveWindowSizeMs(spec));
                compiledFeature.setWindowSlideMs(resolveWindowSlideMs(spec));
                compiledFeature.setTtlMs(DurationParser.parse(spec.getTtl()).toMillis());
                compiledFeature.setRetentionMs(resolveRetentionMs(compiledFeature));
                runtime.getStreamFeatures().add(compiledFeature);
            } catch (RuntimeException exception) {
                throw RuntimeCompileException.streamFeature(spec == null ? null : spec.getCode(),
                        EngineType.AVIATOR,
                        resolveCompileErrorCode(exception),
                        exception);
            }
        }

        for (LookupFeatureSpec spec : defaultList(snapshot.getLookupFeatures())) {
            if (spec == null) {
                continue;
            }
            try {
                CompiledSceneRuntime.CompiledLookupFeature compiledFeature = new CompiledSceneRuntime.CompiledLookupFeature();
                compiledFeature.setSpec(spec);
                compiledFeature.setKeyScript(compileAviator(spec.getKeyExpr()));
                runtime.getLookupFeatures().add(compiledFeature);
            } catch (RuntimeException exception) {
                throw RuntimeCompileException.lookupFeature(spec.getCode(),
                        EngineType.AVIATOR,
                        resolveCompileErrorCode(exception),
                        exception);
            }
        }

        Map<String, DerivedFeatureSpec> derivedByCode = defaultList(snapshot.getDerivedFeatures()).stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(DerivedFeatureSpec::getCode, item -> item, (left, right) -> left, LinkedHashMap::new));
        for (DerivedFeatureSpec spec : orderDerived(defaultList(snapshot.getDerivedFeatures()))) {
            try {
                CompiledSceneRuntime.CompiledDerivedFeature compiledFeature = new CompiledSceneRuntime.CompiledDerivedFeature();
                compiledFeature.setSpec(spec);
                compiledFeature.setExpression(compileScript(snapshot, spec.getEngineType(), spec.getExpr()));
                runtime.getOrderedDerivedFeatures().add(compiledFeature);
            } catch (RuntimeException exception) {
                throw RuntimeCompileException.derivedFeature(spec == null ? null : spec.getCode(),
                        defaultEngine(spec == null ? null : spec.getEngineType()),
                        resolveCompileErrorCode(exception),
                        exception);
            }
        }

        Map<String, CompiledSceneRuntime.CompiledRule> ruleByCode = new LinkedHashMap<>();
        for (RuleSpec spec : defaultList(snapshot.getRules())) {
            if (spec == null) {
                continue;
            }
            try {
                CompiledSceneRuntime.CompiledRule compiledRule = new CompiledSceneRuntime.CompiledRule();
                compiledRule.setSpec(spec);
                compiledRule.setCondition(compileScript(snapshot, spec.getEngineType(), spec.getWhenExpr()));
                ruleByCode.put(spec.getCode(), compiledRule);
            } catch (RuntimeException exception) {
                throw RuntimeCompileException.rule(spec.getCode(),
                        defaultEngine(spec.getEngineType()),
                        resolveCompileErrorCode(exception),
                        exception);
            }
        }
        runtime.setOrderedRules(orderRules(ruleByCode.values(), snapshot.getPolicy()));
        runtime.setStreamFeatureRoutingPlan(buildStreamFeatureRoutingPlan(snapshot, runtime.getStreamFeatures()));
        validateDependencies(derivedByCode.keySet(), defaultList(snapshot.getRules()));
        return runtime;
    }

    private CompiledScript compileAviator(String expression) {
        return scriptCompiler.compile(EngineType.AVIATOR, expression);
    }

    private CompiledScript compileScript(SceneSnapshot snapshot,
                                         EngineType engineType,
                                         String expression) {
        EngineType resolvedEngineType = defaultEngine(engineType);
        if (resolvedEngineType == EngineType.GROOVY && !allowGroovy(snapshot)) {
            throw new IllegalArgumentException("groovy script is disabled by runtimeHints.allowGroovy");
        }
        return scriptCompiler.compile(resolvedEngineType, expression);
    }

    private boolean allowGroovy(SceneSnapshot snapshot) {
        RuntimeHints hints = snapshot == null ? null : snapshot.getRuntimeHints();
        return hints == null || !Boolean.FALSE.equals(hints.getAllowGroovy());
    }

    private String resolveCompileErrorCode(Throwable exception) {
        String message = exception == null || exception.getMessage() == null ? "" : exception.getMessage();
        if (message.contains("runtimeHints.allowGroovy")) {
            return EngineErrorCodes.GROOVY_DISABLED;
        }
        if (message.contains("sandbox")) {
            return EngineErrorCodes.GROOVY_SANDBOX_REJECTED;
        }
        return EngineErrorCodes.SNAPSHOT_COMPILE_FAILED;
    }

    private EngineType defaultEngine(EngineType engineType) {
        return engineType == null ? EngineType.AVIATOR : engineType;
    }

    private String defaultIfBlank(String text, String defaultValue) {
        return text == null || text.isBlank() ? defaultValue : text;
    }

    private long resolveWindowSizeMs(StreamFeatureSpec spec) {
        if (spec == null) {
            return 0L;
        }
        if (spec.getWindowType() == null || spec.getWindowType() == WindowType.NONE) {
            return DurationParser.parse(spec.getTtl()).toMillis();
        }
        return DurationParser.parse(spec.getWindowSize()).toMillis();
    }

    private long resolveWindowSlideMs(StreamFeatureSpec spec) {
        if (spec == null) {
            return 0L;
        }
        long parsedSlide = DurationParser.parse(spec.getWindowSlide()).toMillis();
        if (parsedSlide > 0L) {
            return parsedSlide;
        }
        long windowSizeMs = resolveWindowSizeMs(spec);
        return windowSizeMs > 0L ? windowSizeMs : 1L;
    }

    private long resolveRetentionMs(CompiledSceneRuntime.CompiledStreamFeature feature) {
        return Math.max(feature.getWindowSizeMs(), feature.getTtlMs());
    }

    private List<DerivedFeatureSpec> orderDerived(List<DerivedFeatureSpec> specs) {
        Map<String, DerivedFeatureSpec> byCode = specs.stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(DerivedFeatureSpec::getCode, item -> item, (left, right) -> left, LinkedHashMap::new));
        Map<String, Set<String>> graph = new LinkedHashMap<>();
        Map<String, Integer> indegree = new HashMap<>();
        byCode.keySet().forEach(code -> {
            graph.put(code, new LinkedHashSet<>());
            indegree.put(code, 0);
        });
        for (DerivedFeatureSpec spec : byCode.values()) {
            for (String dependency : defaultList(spec.getDependsOn())) {
                if (!byCode.containsKey(dependency)) {
                    continue;
                }
                graph.get(dependency).add(spec.getCode());
                indegree.compute(spec.getCode(), (key, value) -> value == null ? 1 : value + 1);
            }
        }
        Deque<String> queue = new ArrayDeque<>();
        indegree.forEach((code, value) -> {
            if (value == 0) {
                queue.add(code);
            }
        });
        List<DerivedFeatureSpec> ordered = new ArrayList<>();
        while (!queue.isEmpty()) {
            String code = queue.removeFirst();
            ordered.add(byCode.get(code));
            for (String next : graph.get(code)) {
                int nextInDegree = indegree.compute(next, (key, value) -> value == null ? 0 : value - 1);
                if (nextInDegree == 0) {
                    queue.add(next);
                }
            }
        }
        if (ordered.size() != byCode.size()) {
            throw new IllegalArgumentException("derived feature dependency cycle detected");
        }
        return ordered;
    }

    private List<CompiledSceneRuntime.CompiledRule> orderRules(Collection<CompiledSceneRuntime.CompiledRule> compiledRules,
                                                               PolicySpec policy) {
        Map<String, CompiledSceneRuntime.CompiledRule> byCode = compiledRules.stream()
                .filter(item -> item.getSpec() != null)
                .collect(Collectors.toMap(item -> item.getSpec().getCode(), item -> item, (left, right) -> left, LinkedHashMap::new));
        if (policy != null && policy.getRuleRefs() != null && !policy.getRuleRefs().isEmpty()) {
            List<CompiledSceneRuntime.CompiledRule> ordered = new ArrayList<>();
            for (PolicyRuleRefSpec ruleRef : policy.getRuleRefs().stream()
                    .filter(Objects::nonNull)
                    .filter(item -> !Boolean.FALSE.equals(item.getEnabled()))
                    .sorted(Comparator.comparing(PolicyRuleRefSpec::getOrderNo, Comparator.nullsLast(Comparator.naturalOrder())))
                    .toList()) {
                CompiledSceneRuntime.CompiledRule compiledRule = byCode.get(ruleRef.getRuleCode());
                if (compiledRule == null) {
                    throw new IllegalArgumentException("policy rule ref references missing rule: " + ruleRef.getRuleCode());
                }
                ordered.add(compiledRule);
            }
            return ordered;
        }
        List<CompiledSceneRuntime.CompiledRule> ordered = new ArrayList<>();
        Set<String> added = new HashSet<>();
        if (policy != null && policy.getRuleOrder() != null) {
            for (String code : policy.getRuleOrder()) {
                CompiledSceneRuntime.CompiledRule compiledRule = byCode.get(code);
                if (compiledRule != null) {
                    ordered.add(compiledRule);
                    added.add(code);
                }
            }
        }
        compiledRules.stream()
                .filter(item -> item.getSpec() != null)
                .filter(item -> !added.contains(item.getSpec().getCode()))
                .sorted(Comparator.comparing((CompiledSceneRuntime.CompiledRule item) -> item.getSpec().getPriority(),
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .forEach(ordered::add);
        return ordered;
    }

    private CompiledSceneRuntime.StreamFeatureRoutingPlan buildStreamFeatureRoutingPlan(
            SceneSnapshot snapshot,
            List<CompiledSceneRuntime.CompiledStreamFeature> compiledStreamFeatures) {
        CompiledSceneRuntime.StreamFeatureRoutingPlan routingPlan = new CompiledSceneRuntime.StreamFeatureRoutingPlan();
        routingPlan.setSceneCode(snapshot == null ? null : snapshot.getSceneCode());
        if (compiledStreamFeatures == null || compiledStreamFeatures.isEmpty()) {
            return routingPlan;
        }

        Map<String, CompiledSceneRuntime.StreamFeatureGroupPlan> groupsByKey = new LinkedHashMap<>();
        for (CompiledSceneRuntime.CompiledStreamFeature compiledStreamFeature : compiledStreamFeatures) {
            StreamFeatureSpec spec = compiledStreamFeature == null ? null : compiledStreamFeature.getSpec();
            if (spec == null) {
                continue;
            }
            String featureCode = spec.getCode();
            String entityKeyExpr = normalizeRequiredText(spec.getEntityKeyExpr(), "entityKeyExpr", featureCode);
            String entityType = normalizeOptionalText(spec.getEntityType(), "ENTITY");
            List<String> sourceEventTypes = normalizeSourceEventTypes(spec.getSourceEventTypes());
            String groupKey = buildRoutingGroupKey(snapshot == null ? null : snapshot.getSceneCode(),
                    entityType,
                    entityKeyExpr,
                    sourceEventTypes);
            CompiledSceneRuntime.StreamFeatureGroupPlan groupPlan = groupsByKey.computeIfAbsent(groupKey, ignored -> {
                CompiledSceneRuntime.StreamFeatureGroupPlan created = new CompiledSceneRuntime.StreamFeatureGroupPlan();
                created.setSceneCode(snapshot == null ? null : snapshot.getSceneCode());
                created.setEntityType(entityType);
                created.setEntityKeyExpr(entityKeyExpr);
                created.setSourceEventTypes(new ArrayList<>(sourceEventTypes));
                return created;
            });
            groupPlan.getFeatureCodes().add(featureCode);
        }

        routingPlan.setGroups(new ArrayList<>(groupsByKey.values()));
        validateRoutingPlanCoverage(compiledStreamFeatures, routingPlan);
        return routingPlan;
    }

    private void validateRoutingPlanCoverage(List<CompiledSceneRuntime.CompiledStreamFeature> compiledStreamFeatures,
                                             CompiledSceneRuntime.StreamFeatureRoutingPlan routingPlan) {
        List<String> compiledFeatureCodes = compiledStreamFeatures.stream()
                .map(CompiledSceneRuntime.CompiledStreamFeature::getSpec)
                .filter(Objects::nonNull)
                .map(StreamFeatureSpec::getCode)
                .filter(Objects::nonNull)
                .toList();
        List<String> plannedFeatureCodes = routingPlan == null ? List.of() : routingPlan.featureCodes();
        if (!compiledFeatureCodes.equals(plannedFeatureCodes)) {
            throw new IllegalArgumentException("stream feature routing plan coverage mismatch");
        }
    }

    private String buildRoutingGroupKey(String sceneCode,
                                        String entityType,
                                        String entityKeyExpr,
                                        List<String> sourceEventTypes) {
        String normalizedSceneCode = normalizeOptionalText(sceneCode, "scene:default");
        String normalizedEntityType = normalizeOptionalText(entityType, "ENTITY");
        String normalizedEntityKeyExpr = normalizeRequiredText(entityKeyExpr, "entityKeyExpr", null);
        String normalizedSourceEventTypes = sourceEventTypes == null || sourceEventTypes.isEmpty()
                ? "event:*"
                : String.join(",", sourceEventTypes);
        return normalizedSceneCode + '|' + normalizedEntityType + '|' + normalizedEntityKeyExpr + '|' + normalizedSourceEventTypes;
    }

    private List<String> normalizeSourceEventTypes(List<String> sourceEventTypes) {
        if (sourceEventTypes == null || sourceEventTypes.isEmpty()) {
            return List.of();
        }
        LinkedHashSet<String> normalized = new LinkedHashSet<>();
        for (String sourceEventType : sourceEventTypes) {
            if (sourceEventType == null || sourceEventType.isBlank()) {
                continue;
            }
            normalized.add(sourceEventType.trim());
        }
        if (normalized.isEmpty()) {
            return List.of();
        }
        List<String> ordered = new ArrayList<>(normalized);
        Collections.sort(ordered);
        return ordered;
    }

    private String normalizeRequiredText(String value, String fieldName, String featureCode) {
        if (value == null || value.isBlank()) {
            String prefix = featureCode == null || featureCode.isBlank()
                    ? "stream feature " + fieldName + " must not be blank"
                    : "stream feature " + fieldName + " must not be blank: " + featureCode;
            throw new IllegalArgumentException(prefix);
        }
        return value.trim();
    }

    private String normalizeOptionalText(String value, String defaultValue) {
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        return value.trim();
    }

    private void validateDependencies(Set<String> derivedCodes, List<RuleSpec> rules) {
        for (RuleSpec rule : rules) {
            if (rule == null || rule.getDependsOn() == null) {
                continue;
            }
            for (String dependency : rule.getDependsOn()) {
                if (dependency != null && dependency.isBlank()) {
                    throw new IllegalArgumentException("rule dependency must not be blank");
                }
            }
        }
        if (derivedCodes.contains(null)) {
            throw new IllegalArgumentException("derived feature code must not be null");
        }
    }

    private <T> List<T> defaultList(List<T> items) {
        return items == null ? List.of() : items;
    }

}
