package cn.liboshuai.pulsix.engine.runtime;

import cn.liboshuai.pulsix.engine.model.DerivedFeatureSpec;
import cn.liboshuai.pulsix.engine.model.LookupFeatureSpec;
import cn.liboshuai.pulsix.engine.model.PolicySpec;
import cn.liboshuai.pulsix.engine.model.RuleSpec;
import cn.liboshuai.pulsix.engine.model.SceneSnapshot;
import cn.liboshuai.pulsix.engine.model.StreamFeatureSpec;
import cn.liboshuai.pulsix.engine.script.CompiledScript;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
public class CompiledSceneRuntime implements Serializable {

    private SceneSnapshot snapshot;

    private List<CompiledStreamFeature> streamFeatures = new ArrayList<>();

    private List<CompiledLookupFeature> lookupFeatures = new ArrayList<>();

    private List<CompiledDerivedFeature> orderedDerivedFeatures = new ArrayList<>();

    private List<CompiledRule> orderedRules = new ArrayList<>();

    private PolicySpec policy;

    public String sceneCode() {
        return snapshot.getSceneCode();
    }

    public Integer version() {
        return snapshot.getVersion();
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

}
