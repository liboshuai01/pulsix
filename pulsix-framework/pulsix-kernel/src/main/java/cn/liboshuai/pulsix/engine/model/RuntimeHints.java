package cn.liboshuai.pulsix.engine.model;

import cn.liboshuai.pulsix.engine.support.CollectionCopier;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

@Data
@NoArgsConstructor
public class RuntimeHints implements Serializable {

    private List<String> requiredStreamFeatures;

    private List<String> requiredLookupFeatures;

    private List<String> requiredDerivedFeatures;

    private Integer maxRuleExecutionCount;

    private Boolean allowGroovy;

    private Boolean needFullDecisionLog;

    public void setRequiredStreamFeatures(List<String> requiredStreamFeatures) {
        this.requiredStreamFeatures = CollectionCopier.copyList(requiredStreamFeatures);
    }

    public void setRequiredLookupFeatures(List<String> requiredLookupFeatures) {
        this.requiredLookupFeatures = CollectionCopier.copyList(requiredLookupFeatures);
    }

    public void setRequiredDerivedFeatures(List<String> requiredDerivedFeatures) {
        this.requiredDerivedFeatures = CollectionCopier.copyList(requiredDerivedFeatures);
    }

}
