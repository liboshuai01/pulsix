package cn.liboshuai.pulsix.module.risk.service.release;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.liboshuai.pulsix.engine.model.EngineErrorCodes;
import cn.liboshuai.pulsix.engine.model.RuntimeHints;
import cn.liboshuai.pulsix.engine.model.SceneSnapshot;
import cn.liboshuai.pulsix.engine.model.StreamFeatureSpec;
import cn.liboshuai.pulsix.engine.runtime.CompiledSceneRuntime;
import cn.liboshuai.pulsix.engine.runtime.RuntimeCompileException;
import cn.liboshuai.pulsix.engine.runtime.RuntimeCompiler;
import cn.liboshuai.pulsix.engine.script.DefaultScriptCompiler;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class SceneReleaseRuntimePreviewService {

    private final RuntimeCompiler runtimeCompiler;

    public SceneReleaseRuntimePreviewService() {
        this(new RuntimeCompiler(new DefaultScriptCompiler()));
    }

    SceneReleaseRuntimePreviewService(RuntimeCompiler runtimeCompiler) {
        this.runtimeCompiler = runtimeCompiler;
    }

    public SceneReleaseRuntimePreviewResult preview(SceneSnapshot snapshot) {
        SceneReleaseRuntimePreviewResult result = initialize(snapshot);
        try {
            CompiledSceneRuntime runtime = runtimeCompiler.compile(snapshot);
            populateSuccess(result, runtime);
            return result;
        } catch (RuntimeException exception) {
            populateFailure(result, exception);
            return result;
        }
    }

    private SceneReleaseRuntimePreviewResult initialize(SceneSnapshot snapshot) {
        SceneReleaseRuntimePreviewResult result = new SceneReleaseRuntimePreviewResult();
        result.setValid(false);
        result.setStage("snapshot-compile");
        result.setErrorCode(null);
        result.setMessage(null);
        result.setStreamFeatureCount(CollUtil.size(snapshot == null ? null : snapshot.getStreamFeatures()));
        result.setLookupFeatureCount(CollUtil.size(snapshot == null ? null : snapshot.getLookupFeatures()));
        result.setDerivedFeatureCount(CollUtil.size(snapshot == null ? null : snapshot.getDerivedFeatures()));
        result.setOrderedRuleCount(CollUtil.size(snapshot == null ? null : snapshot.getRules()));
        result.setFeatureCodes(resolveFeatureCodes(snapshot));
        result.setRuntimeHints(resolveRuntimeHints(snapshot));
        return result;
    }

    private void populateSuccess(SceneReleaseRuntimePreviewResult result, CompiledSceneRuntime runtime) {
        result.setValid(true);
        result.setErrorCode(null);
        result.setFeatureCode(null);
        result.setRuleCode(null);
        result.setEngineType(null);
        result.setMessage("运行时快照编译通过");
        result.setFeatureCodes(new ArrayList<>(runtime.featureCodes()));
        result.setOrderedDerivedFeatureCodes(runtime.getOrderedDerivedFeatures().stream()
                .map(CompiledSceneRuntime.CompiledDerivedFeature::getSpec)
                .filter(Objects::nonNull)
                .map(spec -> spec.getCode())
                .filter(Objects::nonNull)
                .toList());
        result.setOrderedRuleCodes(runtime.getOrderedRules().stream()
                .map(CompiledSceneRuntime.CompiledRule::getSpec)
                .filter(Objects::nonNull)
                .map(spec -> spec.getCode())
                .filter(Objects::nonNull)
                .toList());
        result.setOrderedRuleCount(result.getOrderedRuleCodes().size());
        result.setStreamFeatureGroups(runtime.getStreamFeatureRoutingPlan() == null
                ? List.of()
                : runtime.getStreamFeatureRoutingPlan().getGroups().stream()
                .filter(Objects::nonNull)
                .map(group -> {
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("groupKey", group.groupKey());
                    item.put("entityType", group.getEntityType());
                    item.put("entityKeyExpr", group.getEntityKeyExpr());
                    item.put("sourceEventTypes", group.getSourceEventTypes());
                    item.put("featureCodes", group.getFeatureCodes());
                    return item;
                }).toList());
        result.setRoutingGroupCount(result.getStreamFeatureGroups().size());
        result.setMixedStreamFeatureRouting(runtime.hasMixedStreamFeatureRouting());
    }

    private void populateFailure(SceneReleaseRuntimePreviewResult result, RuntimeException exception) {
        result.setValid(false);
        result.setMessage(rootMessage(exception));
        result.setRoutingGroupCount(0);
        result.setMixedStreamFeatureRouting(false);
        result.setOrderedDerivedFeatureCodes(List.of());
        result.setOrderedRuleCodes(List.of());
        result.setStreamFeatureGroups(List.of());
        if (exception instanceof RuntimeCompileException compileException) {
            result.setStage(StrUtil.blankToDefault(compileException.getStage(), "snapshot-compile"));
            result.setErrorCode(StrUtil.blankToDefault(compileException.getErrorCode(), EngineErrorCodes.SNAPSHOT_COMPILE_FAILED));
            result.setFeatureCode(compileException.getFeatureCode());
            result.setRuleCode(compileException.getRuleCode());
            result.setEngineType(compileException.getEngineType() == null ? null : compileException.getEngineType().name());
            return;
        }
        result.setErrorCode(EngineErrorCodes.SNAPSHOT_COMPILE_FAILED);
        result.setFeatureCode(null);
        result.setRuleCode(null);
        result.setEngineType(null);
    }

    private List<String> resolveFeatureCodes(SceneSnapshot snapshot) {
        List<String> featureCodes = new ArrayList<>();
        if (snapshot == null) {
            return featureCodes;
        }
        defaultStreamFeatures(snapshot).stream().map(StreamFeatureSpec::getCode).filter(Objects::nonNull).forEach(featureCodes::add);
        defaultLookupFeatures(snapshot).stream().map(item -> item.getCode()).filter(Objects::nonNull).forEach(featureCodes::add);
        defaultDerivedFeatures(snapshot).stream().map(item -> item.getCode()).filter(Objects::nonNull).forEach(featureCodes::add);
        return featureCodes;
    }

    private Map<String, Object> resolveRuntimeHints(SceneSnapshot snapshot) {
        RuntimeHints hints = snapshot == null ? null : snapshot.getRuntimeHints();
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("requiredStreamFeatures", hints == null ? List.of() : defaultList(hints.getRequiredStreamFeatures()));
        result.put("requiredLookupFeatures", hints == null ? List.of() : defaultList(hints.getRequiredLookupFeatures()));
        result.put("requiredDerivedFeatures", hints == null ? List.of() : defaultList(hints.getRequiredDerivedFeatures()));
        result.put("maxRuleExecutionCount", hints == null ? null : hints.getMaxRuleExecutionCount());
        result.put("allowGroovy", hints == null ? null : hints.getAllowGroovy());
        result.put("needFullDecisionLog", hints == null ? null : hints.getNeedFullDecisionLog());
        return result;
    }

    private List<StreamFeatureSpec> defaultStreamFeatures(SceneSnapshot snapshot) {
        return snapshot == null || snapshot.getStreamFeatures() == null ? List.of() : snapshot.getStreamFeatures();
    }

    private List<cn.liboshuai.pulsix.engine.model.LookupFeatureSpec> defaultLookupFeatures(SceneSnapshot snapshot) {
        return snapshot == null || snapshot.getLookupFeatures() == null ? List.of() : snapshot.getLookupFeatures();
    }

    private List<cn.liboshuai.pulsix.engine.model.DerivedFeatureSpec> defaultDerivedFeatures(SceneSnapshot snapshot) {
        return snapshot == null || snapshot.getDerivedFeatures() == null ? List.of() : snapshot.getDerivedFeatures();
    }

    private <T> List<T> defaultList(List<T> list) {
        return list == null ? List.of() : new ArrayList<>(list);
    }

    private String rootMessage(Throwable throwable) {
        if (throwable != null && StrUtil.isNotBlank(throwable.getMessage())) {
            return throwable.getMessage();
        }
        Throwable current = throwable;
        String message = null;
        while (current != null) {
            if (StrUtil.isNotBlank(current.getMessage())) {
                message = current.getMessage();
            }
            current = current.getCause();
        }
        return StrUtil.blankToDefault(message, "运行时快照编译失败");
    }

}
