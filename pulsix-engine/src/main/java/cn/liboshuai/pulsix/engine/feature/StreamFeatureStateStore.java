package cn.liboshuai.pulsix.engine.feature;

import java.time.Instant;
import java.util.Deque;

public interface StreamFeatureStateStore {

    WindowBuffer getWindow(String sceneCode, String featureCode, String entityKey);

    interface WindowBuffer {

        void cleanup(Instant now, long maxAgeMs);

        void add(Instant eventTime, Object value);

        Deque<Observation> observations();

    }

    record Observation(Instant eventTime, Object value) {
    }

}
