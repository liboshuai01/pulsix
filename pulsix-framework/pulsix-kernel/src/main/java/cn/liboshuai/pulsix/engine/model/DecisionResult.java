package cn.liboshuai.pulsix.engine.model;

import cn.liboshuai.pulsix.engine.support.CollectionCopier;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
public class DecisionResult implements Serializable {

    private String eventId;

    private String traceId;

    private String sceneCode;

    private Integer version;

    private DecisionMode decisionMode;

    private ActionType finalAction;

    private Integer finalScore;

    private Long latencyMs;

    private List<RuleHit> ruleHits = new ArrayList<>();

    private Map<String, String> featureSnapshot = new LinkedHashMap<>();

    private List<String> traceLogs = new ArrayList<>();

    private String errorMessage;

    public void setFeatureSnapshot(Map<String, ?> featureSnapshot) {
        this.featureSnapshot = new LinkedHashMap<>();
        if (featureSnapshot == null) {
            return;
        }
        featureSnapshot.forEach((key, value) -> this.featureSnapshot.put(key, value == null ? null : String.valueOf(value)));
    }

    public void setRuleHits(List<RuleHit> ruleHits) {
        this.ruleHits = CollectionCopier.copyList(ruleHits);
    }

    public void setTraceLogs(List<String> traceLogs) {
        this.traceLogs = CollectionCopier.copyList(traceLogs);
    }

}
