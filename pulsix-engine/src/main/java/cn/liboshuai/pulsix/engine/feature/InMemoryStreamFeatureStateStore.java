package cn.liboshuai.pulsix.engine.feature;

import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryStreamFeatureStateStore implements StreamFeatureStateStore {

    private final Map<String, WindowBuffer> windows = new ConcurrentHashMap<>();

    @Override
    public WindowBuffer getWindow(String sceneCode, String featureCode, String entityKey) {
        return windows.computeIfAbsent(sceneCode + '|' + featureCode + '|' + entityKey, key -> new InMemoryWindowBuffer());
    }

    public static class InMemoryWindowBuffer implements WindowBuffer {

        private final Deque<Observation> observations = new ArrayDeque<>();

        @Override
        public void cleanup(Instant now, long maxAgeMs) {
            if (maxAgeMs <= 0) {
                return;
            }
            Instant boundary = now.minusMillis(maxAgeMs);
            while (!observations.isEmpty() && observations.peekFirst().eventTime().isBefore(boundary)) {
                observations.removeFirst();
            }
        }

        @Override
        public void add(Instant eventTime, Object value) {
            observations.addLast(new Observation(eventTime, value));
        }

        @Override
        public Deque<Observation> observations() {
            return observations;
        }

    }

}
