package cn.liboshuai.pulsix.engine.model;

import cn.liboshuai.pulsix.engine.support.CollectionCopier;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

@Data
@NoArgsConstructor
public class SceneSpec implements Serializable {

    private String defaultPolicyCode;

    private List<String> allowedEventTypes;

    private Integer decisionTimeoutMs;

    private String logLevel;

    public void setAllowedEventTypes(List<String> allowedEventTypes) {
        this.allowedEventTypes = CollectionCopier.copyList(allowedEventTypes);
    }

}
