package cn.liboshuai.pulsix.engine.model;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
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

    public static DecisionLogRecord from(DecisionResult result) {
        DecisionLogRecord record = new DecisionLogRecord();
        record.setEventId(result.getEventId());
        record.setTraceId(result.getTraceId());
        record.setSceneCode(result.getSceneCode());
        record.setVersion(result.getVersion());
        record.setFinalAction(result.getFinalAction());
        record.setFinalScore(result.getFinalScore());
        record.setLatencyMs(result.getLatencyMs());
        record.setRuleHits(result.getRuleHits());
        record.setFeatureSnapshot(result.getFeatureSnapshot());
        record.setTraceLogs(result.getTraceLogs());
        return record;
    }

}
