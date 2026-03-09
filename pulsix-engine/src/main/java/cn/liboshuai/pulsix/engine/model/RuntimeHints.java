package cn.liboshuai.pulsix.engine.model;

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

}
