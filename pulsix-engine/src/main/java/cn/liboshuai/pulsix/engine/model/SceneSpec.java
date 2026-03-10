package cn.liboshuai.pulsix.engine.model;

import cn.liboshuai.pulsix.engine.flink.typeinfo.EngineTypeInfoFactories;
import cn.liboshuai.pulsix.engine.support.CollectionCopier;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.flink.api.common.typeinfo.TypeInfo;

import java.io.Serializable;
import java.util.List;

@Data
@NoArgsConstructor
@TypeInfo(EngineTypeInfoFactories.SceneSpecTypeInfoFactory.class)
public class SceneSpec implements Serializable {

    private String defaultPolicyCode;

    private List<String> allowedEventTypes;

    private Integer decisionTimeoutMs;

    private String logLevel;

    public void setAllowedEventTypes(List<String> allowedEventTypes) {
        this.allowedEventTypes = CollectionCopier.copyList(allowedEventTypes);
    }

}
