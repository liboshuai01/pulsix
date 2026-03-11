package cn.liboshuai.pulsix.engine.model;

import cn.liboshuai.pulsix.engine.support.CollectionCopier;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
public class DecisionLogRecord implements Serializable {

    private String eventId;

    private String traceId;

    private String sceneCode;

    private Integer version;

    private ActionType finalAction;

    private Integer finalScore;

    private Long latencyMs;

    private List<RuleHit> ruleHits;

    private Map<String, String> featureSnapshot;

    private List<String> traceLogs;

    public void setRuleHits(List<RuleHit> ruleHits) {
        this.ruleHits = CollectionCopier.copyList(ruleHits);
    }

    public void setFeatureSnapshot(Map<String, String> featureSnapshot) {
        this.featureSnapshot = CollectionCopier.copyMap(featureSnapshot);
    }

    public void setTraceLogs(List<String> traceLogs) {
        this.traceLogs = CollectionCopier.copyList(traceLogs);
    }

    public static DecisionLogRecord from(DecisionResult result) {
        return from(result, true);
    }

    public static DecisionLogRecord from(DecisionResult result, boolean needFullDecisionLog) {
        DecisionLogRecord record = new DecisionLogRecord();
        record.setEventId(result.getEventId());
        record.setTraceId(result.getTraceId());
        record.setSceneCode(result.getSceneCode());
        record.setVersion(result.getVersion());
        record.setFinalAction(result.getFinalAction());
        record.setFinalScore(result.getFinalScore());
        record.setLatencyMs(result.getLatencyMs());
        if (needFullDecisionLog) {
            record.setRuleHits(result.getRuleHits());
            record.setFeatureSnapshot(result.getFeatureSnapshot());
            record.setTraceLogs(result.getTraceLogs());
            return record;
        }
        List<RuleHit> matchedRuleHits = new ArrayList<>();
        if (result.getRuleHits() != null) {
            for (RuleHit ruleHit : result.getRuleHits()) {
                if (ruleHit != null && Boolean.TRUE.equals(ruleHit.getHit())) {
                    matchedRuleHits.add(ruleHit);
                }
            }
        }
        record.setRuleHits(matchedRuleHits);
        return record;
    }

}
