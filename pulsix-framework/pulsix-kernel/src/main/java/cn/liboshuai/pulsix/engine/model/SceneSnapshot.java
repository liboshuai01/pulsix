package cn.liboshuai.pulsix.engine.model;

import cn.liboshuai.pulsix.engine.flink.typeinfo.EngineTypeInfoFactories;
import cn.liboshuai.pulsix.engine.support.CollectionCopier;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.flink.api.common.typeinfo.TypeInfo;

import java.io.Serializable;
import java.time.Instant;
import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@TypeInfo(EngineTypeInfoFactories.SceneSnapshotTypeInfoFactory.class)
public class SceneSnapshot implements Serializable {

    private String snapshotId;

    private String sceneCode;

    private String sceneName;

    private Integer version;

    private String status;

    private String checksum;

    private Instant publishedAt;

    private Instant effectiveFrom;

    private String runtimeMode;

    private SceneSpec scene;

    private EventSchemaSpec eventSchema;

    private Map<String, List<String>> variables;

    private List<StreamFeatureSpec> streamFeatures;

    private List<LookupFeatureSpec> lookupFeatures;

    private List<DerivedFeatureSpec> derivedFeatures;

    private List<RuleSpec> rules;

    private PolicySpec policy;

    private RuntimeHints runtimeHints;

    public void setVariables(Map<String, List<String>> variables) {
        this.variables = CollectionCopier.copyMapOfLists(variables);
    }

    public void setStreamFeatures(List<StreamFeatureSpec> streamFeatures) {
        this.streamFeatures = CollectionCopier.copyList(streamFeatures);
    }

    public void setLookupFeatures(List<LookupFeatureSpec> lookupFeatures) {
        this.lookupFeatures = CollectionCopier.copyList(lookupFeatures);
    }

    public void setDerivedFeatures(List<DerivedFeatureSpec> derivedFeatures) {
        this.derivedFeatures = CollectionCopier.copyList(derivedFeatures);
    }

    public void setRules(List<RuleSpec> rules) {
        this.rules = CollectionCopier.copyList(rules);
    }

}
