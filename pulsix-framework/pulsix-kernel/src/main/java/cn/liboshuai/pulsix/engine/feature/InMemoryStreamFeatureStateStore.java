package cn.liboshuai.pulsix.engine.feature;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryStreamFeatureStateStore extends AbstractStreamFeatureStateStore {

    private final Map<String, NumericWindowState> numericStates = new ConcurrentHashMap<>();

    private final Map<String, LatestValueState> latestStates = new ConcurrentHashMap<>();

    private final Map<String, DistinctWindowState> distinctStates = new ConcurrentHashMap<>();

    @Override
    protected NumericWindowState getNumericState(String featureInstanceKey) {
        return numericStates.get(featureInstanceKey);
    }

    @Override
    protected void putNumericState(String featureInstanceKey, NumericWindowState state) {
        numericStates.put(featureInstanceKey, state);
    }

    @Override
    protected void removeNumericState(String featureInstanceKey) {
        numericStates.remove(featureInstanceKey);
    }

    @Override
    protected LatestValueState getLatestState(String featureInstanceKey) {
        return latestStates.get(featureInstanceKey);
    }

    @Override
    protected void putLatestState(String featureInstanceKey, LatestValueState state) {
        latestStates.put(featureInstanceKey, state);
    }

    @Override
    protected void removeLatestState(String featureInstanceKey) {
        latestStates.remove(featureInstanceKey);
    }

    @Override
    protected DistinctWindowState getDistinctState(String featureInstanceKey) {
        return distinctStates.get(featureInstanceKey);
    }

    @Override
    protected void putDistinctState(String featureInstanceKey, DistinctWindowState state) {
        distinctStates.put(featureInstanceKey, state);
    }

    @Override
    protected void removeDistinctState(String featureInstanceKey) {
        distinctStates.remove(featureInstanceKey);
    }

}
