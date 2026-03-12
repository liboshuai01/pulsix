package cn.liboshuai.pulsix.engine.flink;

import cn.liboshuai.pulsix.engine.demo.DemoFixtures;
import cn.liboshuai.pulsix.engine.flink.runtime.SceneReleaseTimeline;
import cn.liboshuai.pulsix.engine.flink.typeinfo.EngineTypeInfos;
import cn.liboshuai.pulsix.engine.json.EngineJson;
import cn.liboshuai.pulsix.engine.model.EngineErrorRecord;
import cn.liboshuai.pulsix.engine.model.RiskEvent;
import cn.liboshuai.pulsix.engine.model.SceneSnapshotEnvelope;
import org.apache.flink.api.common.serialization.SerializerConfigImpl;
import org.apache.flink.api.common.state.MapStateDescriptor;
import org.apache.flink.api.common.typeinfo.Types;
import org.apache.flink.runtime.checkpoint.OperatorSubtaskState;
import org.apache.flink.streaming.api.operators.co.CoBroadcastWithKeyedOperator;
import org.apache.flink.streaming.runtime.streamrecord.StreamRecord;
import org.apache.flink.streaming.util.KeyedBroadcastOperatorTestHarness;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Queue;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StreamFeatureRoutingProcessFunctionTest {

    private static final MapStateDescriptor<String, SceneReleaseTimeline> SNAPSHOT_STATE_DESCRIPTOR = new MapStateDescriptor<>(
            "scene-snapshot-broadcast-state",
            Types.STRING,
            EngineTypeInfos.sceneReleaseTimeline()
    );

    @Test
    void shouldRestoreBufferedEventAndRouteItWhenSnapshotArrives() throws Exception {
        SceneSnapshotEnvelope envelope = copy(DemoFixtures.demoEnvelope(), SceneSnapshotEnvelope.class);
        RiskEvent event = copy(DemoFixtures.demoEvents().get(0), RiskEvent.class);
        OperatorSubtaskState snapshot;

        long readyAt = epochMillis(envelope.getEffectiveFrom()) + 1L;
        try (KeyedBroadcastOperatorTestHarness<String, RiskEvent, SceneSnapshotEnvelope, StreamFeatureRouteEvent> harness = newHarness()) {
            harness.setProcessingTime(readyAt);
            harness.processElement(event, epochMillis(event.getEventTime()));
            assertTrue(harness.extractOutputValues().isEmpty());
            snapshot = harness.snapshot(101L, readyAt + 10L);
        }

        try (KeyedBroadcastOperatorTestHarness<String, RiskEvent, SceneSnapshotEnvelope, StreamFeatureRouteEvent> harness = restoredHarness(snapshot)) {
            harness.setProcessingTime(readyAt + 100L);
            harness.processBroadcastElement(envelope, epochMillis(envelope.getPublishedAt()));
            harness.setProcessingTime(readyAt + PendingEventDefaults.DEFAULT_PENDING_RETRY_DELAY_MS + 200L);

            List<StreamFeatureRouteEvent> routedEvents = harness.extractOutputValues();
            assertEquals(2, routedEvents.size());
            assertEquals(List.of("user_trade_cnt_5m", "user_trade_amt_sum_30m"), routedEvents.get(0).getFeatureCodes());
            assertEquals(List.of("device_bind_user_cnt_1h"), routedEvents.get(1).getFeatureCodes());
            assertEquals(routedEvents.get(0).getEventJoinKey(), routedEvents.get(1).getEventJoinKey());
            assertEquals(2, routedEvents.get(0).getExpectedGroupCount());
            assertTrue(sideOutput(harness, EngineOutputTags.ENGINE_ERROR).isEmpty());
            assertTrue(sideOutput(harness, StreamFeatureRoutingProcessFunction.PREPARED_DECISION_BYPASS).isEmpty());
        }
    }

    private KeyedBroadcastOperatorTestHarness<String, RiskEvent, SceneSnapshotEnvelope, StreamFeatureRouteEvent> newHarness()
            throws Exception {
        KeyedBroadcastOperatorTestHarness<String, RiskEvent, SceneSnapshotEnvelope, StreamFeatureRouteEvent> harness =
                createHarness();
        harness.open();
        return harness;
    }

    private KeyedBroadcastOperatorTestHarness<String, RiskEvent, SceneSnapshotEnvelope, StreamFeatureRouteEvent> restoredHarness(
            OperatorSubtaskState snapshot) throws Exception {
        KeyedBroadcastOperatorTestHarness<String, RiskEvent, SceneSnapshotEnvelope, StreamFeatureRouteEvent> harness =
                createHarness();
        harness.initializeState(snapshot);
        harness.open();
        return harness;
    }

    private KeyedBroadcastOperatorTestHarness<String, RiskEvent, SceneSnapshotEnvelope, StreamFeatureRouteEvent> createHarness()
            throws Exception {
        KeyedBroadcastOperatorTestHarness<String, RiskEvent, SceneSnapshotEnvelope, StreamFeatureRouteEvent> harness =
                new KeyedBroadcastOperatorTestHarness<>(
                        new CoBroadcastWithKeyedOperator<>(
                                new StreamFeatureRoutingProcessFunction(SNAPSHOT_STATE_DESCRIPTOR),
                                List.of(SNAPSHOT_STATE_DESCRIPTOR)
                        ),
                        RiskEvent::processingRouteKey,
                        Types.STRING,
                        1,
                        1,
                        0
                );
        harness.setup(EngineTypeInfos.streamFeatureRouteEvent().createSerializer(new SerializerConfigImpl()));
        return harness;
    }

    private long epochMillis(java.time.Instant instant) {
        return instant == null ? 0L : instant.toEpochMilli();
    }

    private <T> T copy(Object value,
                       Class<T> type) {
        return EngineJson.read(EngineJson.write(value), type);
    }

    @SuppressWarnings("unchecked")
    private <T> Queue<?> sideOutput(
            KeyedBroadcastOperatorTestHarness<String, RiskEvent, SceneSnapshotEnvelope, StreamFeatureRouteEvent> harness,
            org.apache.flink.util.OutputTag<T> outputTag) {
        Queue<?> queue = harness.getSideOutput(outputTag);
        return queue == null ? new java.util.ArrayDeque<>() : queue;
    }

}
