package cn.liboshuai.pulsix.engine.context;

import cn.liboshuai.pulsix.engine.model.RiskEvent;
import cn.liboshuai.pulsix.engine.model.RuleHit;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
public class EvalContext implements Serializable {

    private String sceneCode;

    private Integer version;

    private RiskEvent event;

    private Map<String, Object> values = new LinkedHashMap<>();

    private List<RuleHit> ruleHits = new ArrayList<>();

    private List<String> traceLogs = new ArrayList<>();

    public void put(String key, Object value) {
        values.put(key, value);
    }

    public Object get(String key) {
        return values.get(key);
    }

    public void trace(String message) {
        traceLogs.add(message);
    }

}
