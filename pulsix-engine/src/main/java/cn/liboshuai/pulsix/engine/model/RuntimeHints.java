package cn.liboshuai.pulsix.engine.model;

import cn.liboshuai.pulsix.engine.flink.typeinfo.EngineTypeInfoFactories;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.flink.api.common.typeinfo.TypeInfo;

import java.io.Serializable;
import java.util.List;

@Data
@NoArgsConstructor
@TypeInfo(EngineTypeInfoFactories.RuntimeHintsTypeInfoFactory.class)
public class RuntimeHints implements Serializable {

    private List<String> requiredStreamFeatures;

    private List<String> requiredLookupFeatures;

    private List<String> requiredDerivedFeatures;

    private Integer maxRuleExecutionCount;

    private Boolean allowGroovy;

    private Boolean needFullDecisionLog;

}
