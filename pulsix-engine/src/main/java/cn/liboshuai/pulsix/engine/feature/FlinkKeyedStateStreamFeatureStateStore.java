package cn.liboshuai.pulsix.engine.feature;

import org.apache.flink.api.common.functions.RuntimeContext;
import org.apache.flink.api.common.state.MapState;
import org.apache.flink.api.common.state.MapStateDescriptor;
import org.apache.flink.api.common.state.StateTtlConfig;
import org.apache.flink.api.common.time.Time;
import org.apache.flink.api.common.typeinfo.TypeInformation;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class FlinkKeyedStateStreamFeatureStateStore extends AbstractStreamFeatureStateStore {

    private final MapState<String, NumericWindowState> numericStates;

    private final MapState<String, LatestValueState> latestStates;

    private final MapState<String, DistinctWindowState> distinctStates;

    public FlinkKeyedStateStreamFeatureStateStore(RuntimeContext runtimeContext) {
        StateTtlConfig ttlConfig = StateTtlConfig.newBuilder(Time.hours(12))
                .setUpdateType(StateTtlConfig.UpdateType.OnCreateAndWrite)
                .setStateVisibility(StateTtlConfig.StateVisibility.NeverReturnExpired)
                .build();

        MapStateDescriptor<String, NumericWindowState> numericDescriptor = new MapStateDescriptor<>(
                "engine-stream-feature-numeric-state",
                TypeInformation.of(String.class),
                TypeInformation.of(NumericWindowState.class)
        );
        numericDescriptor.enableTimeToLive(ttlConfig);

        MapStateDescriptor<String, LatestValueState> latestDescriptor = new MapStateDescriptor<>(
                "engine-stream-feature-latest-state",
                TypeInformation.of(String.class),
                TypeInformation.of(LatestValueState.class)
        );
        latestDescriptor.enableTimeToLive(ttlConfig);

        MapStateDescriptor<String, DistinctWindowState> distinctDescriptor = new MapStateDescriptor<>(
                "engine-stream-feature-distinct-state",
                TypeInformation.of(String.class),
                TypeInformation.of(DistinctWindowState.class)
        );
        distinctDescriptor.enableTimeToLive(ttlConfig);

        this.numericStates = runtimeContext.getMapState(numericDescriptor);
        this.latestStates = runtimeContext.getMapState(latestDescriptor);
        this.distinctStates = runtimeContext.getMapState(distinctDescriptor);
    }

    @Override
    protected NumericWindowState getNumericState(String featureInstanceKey) {
        try {
            return numericStates.get(featureInstanceKey);
        } catch (Exception exception) {
            throw new IllegalStateException("read numeric keyed state failed", exception);
        }
    }

    @Override
    protected void putNumericState(String featureInstanceKey, NumericWindowState state) {
        try {
            numericStates.put(featureInstanceKey, state);
        } catch (Exception exception) {
            throw new IllegalStateException("write numeric keyed state failed", exception);
        }
    }

    @Override
    protected void removeNumericState(String featureInstanceKey) {
        try {
            numericStates.remove(featureInstanceKey);
        } catch (Exception exception) {
            throw new IllegalStateException("remove numeric keyed state failed", exception);
        }
    }

    @Override
    protected LatestValueState getLatestState(String featureInstanceKey) {
        try {
            return latestStates.get(featureInstanceKey);
        } catch (Exception exception) {
            throw new IllegalStateException("read latest keyed state failed", exception);
        }
    }

    @Override
    protected void putLatestState(String featureInstanceKey, LatestValueState state) {
        try {
            latestStates.put(featureInstanceKey, state);
        } catch (Exception exception) {
            throw new IllegalStateException("write latest keyed state failed", exception);
        }
    }

    @Override
    protected void removeLatestState(String featureInstanceKey) {
        try {
            latestStates.remove(featureInstanceKey);
        } catch (Exception exception) {
            throw new IllegalStateException("remove latest keyed state failed", exception);
        }
    }

    @Override
    protected DistinctWindowState getDistinctState(String featureInstanceKey) {
        try {
            return distinctStates.get(featureInstanceKey);
        } catch (Exception exception) {
            throw new IllegalStateException("read distinct keyed state failed", exception);
        }
    }

    @Override
    protected void putDistinctState(String featureInstanceKey, DistinctWindowState state) {
        try {
            distinctStates.put(featureInstanceKey, state);
        } catch (Exception exception) {
            throw new IllegalStateException("write distinct keyed state failed", exception);
        }
    }

    @Override
    protected void removeDistinctState(String featureInstanceKey) {
        try {
            distinctStates.remove(featureInstanceKey);
        } catch (Exception exception) {
            throw new IllegalStateException("remove distinct keyed state failed", exception);
        }
    }

    @Override
    public void onTimer(long timestamp) {
        cleanupNumericStates(timestamp);
        cleanupLatestStates(timestamp);
        cleanupDistinctStates(timestamp);
    }

    private void cleanupNumericStates(long timestamp) {
        try {
            List<String> keysToRemove = new ArrayList<>();
            for (Map.Entry<String, NumericWindowState> entry : numericStates.entries()) {
                NumericWindowState state = entry.getValue();
                if (state == null) {
                    keysToRemove.add(entry.getKey());
                    continue;
                }
                cleanupExpiredBuckets(state.getBuckets(), timestamp - state.getRetentionMs());
                if (state.getBuckets().isEmpty()) {
                    keysToRemove.add(entry.getKey());
                } else {
                    numericStates.put(entry.getKey(), state);
                }
            }
            for (String key : keysToRemove) {
                numericStates.remove(key);
            }
        } catch (Exception exception) {
            throw new IllegalStateException("cleanup numeric keyed state failed", exception);
        }
    }

    private void cleanupLatestStates(long timestamp) {
        try {
            List<String> keysToRemove = new ArrayList<>();
            for (Map.Entry<String, LatestValueState> entry : latestStates.entries()) {
                LatestValueState state = entry.getValue();
                if (state == null || state.getLatestEventTimeMs() == null) {
                    keysToRemove.add(entry.getKey());
                    continue;
                }
                if (state.getLatestEventTimeMs() < timestamp - state.getRetentionMs()) {
                    keysToRemove.add(entry.getKey());
                }
            }
            for (String key : keysToRemove) {
                latestStates.remove(key);
            }
        } catch (Exception exception) {
            throw new IllegalStateException("cleanup latest keyed state failed", exception);
        }
    }

    private void cleanupDistinctStates(long timestamp) {
        try {
            List<String> keysToRemove = new ArrayList<>();
            for (Map.Entry<String, DistinctWindowState> entry : distinctStates.entries()) {
                DistinctWindowState state = entry.getValue();
                if (state == null) {
                    keysToRemove.add(entry.getKey());
                    continue;
                }
                cleanupExpiredMembers(state.getMemberLastSeenMs(), timestamp - state.getRetentionMs());
                if (state.getMemberLastSeenMs().isEmpty()) {
                    keysToRemove.add(entry.getKey());
                } else {
                    distinctStates.put(entry.getKey(), state);
                }
            }
            for (String key : keysToRemove) {
                distinctStates.remove(key);
            }
        } catch (Exception exception) {
            throw new IllegalStateException("cleanup distinct keyed state failed", exception);
        }
    }

}
