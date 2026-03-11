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
@TypeInfo(EngineTypeInfoFactories.RuleSpecTypeInfoFactory.class)
public class RuleSpec implements Serializable {

    private String code;

    private String name;

    private EngineType engineType;

    private Integer priority;

    private String whenExpr;

    private List<String> dependsOn;

    private ActionType hitAction;

    private Integer riskScore;

    private String hitReasonTemplate;

    private Boolean enabled;

    public void setDependsOn(List<String> dependsOn) {
        this.dependsOn = CollectionCopier.copyList(dependsOn);
    }

}
