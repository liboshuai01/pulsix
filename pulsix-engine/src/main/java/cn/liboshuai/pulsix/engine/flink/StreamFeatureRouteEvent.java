package cn.liboshuai.pulsix.engine.flink;

import cn.liboshuai.pulsix.engine.model.RiskEvent;
import cn.liboshuai.pulsix.engine.model.SceneSnapshot;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class StreamFeatureRouteEvent implements Serializable {

    private String sceneCode;

    private String groupKey;

    private String routeExecutionKey;

    private String eventJoinKey;

    private Integer expectedGroupCount;

    private Long preparedAtEpochMs;

    private RiskEvent event;

    private SceneSnapshot snapshot;

    private List<String> featureCodes = new ArrayList<>();

    public String getSceneCode() {
        return sceneCode;
    }

    public void setSceneCode(String sceneCode) {
        this.sceneCode = sceneCode;
    }

    public String getGroupKey() {
        return groupKey;
    }

    public void setGroupKey(String groupKey) {
        this.groupKey = groupKey;
    }

    public String getRouteExecutionKey() {
        return routeExecutionKey;
    }

    public void setRouteExecutionKey(String routeExecutionKey) {
        this.routeExecutionKey = routeExecutionKey;
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

    public List<String> getFeatureCodes() {
        return featureCodes;
    }

    public void setFeatureCodes(List<String> featureCodes) {
        this.featureCodes = featureCodes == null ? new ArrayList<>() : new ArrayList<>(featureCodes);
    }

}
