package cn.liboshuai.pulsix.engine.flink;

import cn.liboshuai.pulsix.engine.model.RiskEvent;
import cn.liboshuai.pulsix.engine.model.SceneSnapshot;

import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.Map;

public class PreparedStreamFeatureChunk implements Serializable {

    private String sceneCode;

    private String eventJoinKey;

    private Integer expectedGroupCount;

    private Long preparedAtEpochMs;

    private RiskEvent event;

    private SceneSnapshot snapshot;

    private Map<String, String> featureSnapshot = new LinkedHashMap<>();

    public String getSceneCode() {
        return sceneCode;
    }

    public void setSceneCode(String sceneCode) {
        this.sceneCode = sceneCode;
    }

    public String getEventJoinKey() {
        return eventJoinKey;
    }

    public void setEventJoinKey(String eventJoinKey) {
        this.eventJoinKey = eventJoinKey;
    }

    public Integer getExpectedGroupCount() {
        return expectedGroupCount;
    }

    public void setExpectedGroupCount(Integer expectedGroupCount) {
        this.expectedGroupCount = expectedGroupCount;
    }

    public Long getPreparedAtEpochMs() {
        return preparedAtEpochMs;
    }

    public void setPreparedAtEpochMs(Long preparedAtEpochMs) {
        this.preparedAtEpochMs = preparedAtEpochMs;
    }

    public RiskEvent getEvent() {
        return event;
    }

    public void setEvent(RiskEvent event) {
        this.event = event;
    }

    public SceneSnapshot getSnapshot() {
        return snapshot;
    }

    public void setSnapshot(SceneSnapshot snapshot) {
        this.snapshot = snapshot;
    }

    public Map<String, String> getFeatureSnapshot() {
        return featureSnapshot;
    }

    public void setFeatureSnapshot(Map<String, String> featureSnapshot) {
        this.featureSnapshot = featureSnapshot == null ? new LinkedHashMap<>() : new LinkedHashMap<>(featureSnapshot);
    }

}
