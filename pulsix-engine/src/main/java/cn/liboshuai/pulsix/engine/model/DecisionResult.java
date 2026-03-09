package cn.liboshuai.pulsix.engine.model;

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

    private Map<String, Object> featureSnapshot = new LinkedHashMap<>();

    private List<String> traceLogs = new ArrayList<>();

    private String errorMessage;

}
