package cn.liboshuai.pulsix.engine.runtime;

import cn.liboshuai.pulsix.engine.model.DerivedFeatureSpec;
import cn.liboshuai.pulsix.engine.model.EngineType;
import cn.liboshuai.pulsix.engine.model.PolicySpec;
import cn.liboshuai.pulsix.engine.model.RuleSpec;
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
        }

        snapshot.getLookupFeatures().stream().filter(Objects::nonNull).forEach(spec -> {
            CompiledSceneRuntime.CompiledLookupFeature compiledFeature = new CompiledSceneRuntime.CompiledLookupFeature();
            compiledFeature.setSpec(spec);
            compiledFeature.setKeyScript(compileAviator(spec.getKeyExpr()));
            runtime.getLookupFeatures().add(compiledFeature);
        });

        Map<String, DerivedFeatureSpec> derivedByCode = defaultList(snapshot.getDerivedFeatures()).stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(DerivedFeatureSpec::getCode, item -> item, (left, right) -> left, LinkedHashMap::new));
        for (DerivedFeatureSpec spec : orderDerived(defaultList(snapshot.getDerivedFeatures()))) {
            CompiledSceneRuntime.CompiledDerivedFeature compiledFeature = new CompiledSceneRuntime.CompiledDerivedFeature();
            compiledFeature.setSpec(spec);
            compiledFeature.setExpression(scriptCompiler.compile(defaultEngine(spec.getEngineType()), spec.getExpr()));
            runtime.getOrderedDerivedFeatures().add(compiledFeature);
        }

        Map<String, CompiledSceneRuntime.CompiledRule> ruleByCode = new LinkedHashMap<>();
        for (RuleSpec spec : defaultList(snapshot.getRules())) {
            if (spec == null) {
                continue;
            }
            CompiledSceneRuntime.CompiledRule compiledRule = new CompiledSceneRuntime.CompiledRule();
            compiledRule.setSpec(spec);
            compiledRule.setCondition(scriptCompiler.compile(defaultEngine(spec.getEngineType()), spec.getWhenExpr()));
            ruleByCode.put(spec.getCode(), compiledRule);
        }
        runtime.setOrderedRules(orderRules(ruleByCode.values(), snapshot.getPolicy()));
        validateDependencies(derivedByCode.keySet(), defaultList(snapshot.getRules()));
        return runtime;
    }

    private CompiledScript compileAviator(String expression) {
        return scriptCompiler.compile(EngineType.AVIATOR, expression);
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
