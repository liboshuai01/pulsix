package cn.liboshuai.pulsix.engine.flink;

import cn.liboshuai.pulsix.engine.demo.DemoFixtures;
import cn.liboshuai.pulsix.engine.flink.typeinfo.EngineTypeInfos;
import cn.liboshuai.pulsix.engine.json.EngineJson;
import cn.liboshuai.pulsix.engine.model.ActionType;
import cn.liboshuai.pulsix.engine.model.DecisionResult;
import cn.liboshuai.pulsix.engine.model.RiskEvent;
import cn.liboshuai.pulsix.engine.model.RuleSpec;
import cn.liboshuai.pulsix.engine.model.SceneSnapshot;
import cn.liboshuai.pulsix.engine.model.SceneSnapshotEnvelope;
import org.apache.flink.api.common.state.MapStateDescriptor;
import org.apache.flink.api.common.serialization.SerializerConfigImpl;
import org.apache.flink.api.common.typeinfo.Types;
import org.apache.flink.streaming.api.operators.co.CoBroadcastWithKeyedOperator;
import org.apache.flink.streaming.util.KeyedBroadcastOperatorTestHarness;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Queue;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DecisionBroadcastProcessFunctionTest {

    private static final MapStateDescriptor<String, SceneSnapshotEnvelope> SNAPSHOT_STATE_DESCRIPTOR = new MapStateDescriptor<>(
            "scene-snapshot-broadcast-state",
            Types.STRING,
            EngineTypeInfos.sceneSnapshotEnvelope()
    );

    @Test
    void shouldSwitchToNewSnapshotAfterBroadcastUpdate() throws Exception {
        try (KeyedBroadcastOperatorTestHarness<String, RiskEvent, SceneSnapshotEnvelope, DecisionResult> harness = newHarness()) {
            SceneSnapshotEnvelope version12 = baselineEnvelopeForBroadcastSwitch();
            SceneSnapshotEnvelope version13 = disableBlacklistRuleEnvelope();
            RiskEvent firstEvent = copy(DemoFixtures.blacklistedEvent(), RiskEvent.class);
            RiskEvent secondEvent = copy(DemoFixtures.blacklistedEvent(), RiskEvent.class);
            secondEvent.setEventId("E202603070199");
            secondEvent.setTraceId("T202603070199");
            secondEvent.setEventTime(firstEvent.getEventTime().plusSeconds(30));

            harness.processBroadcastElement(version12, epochMillis(version12.getPublishedAt()));
            harness.processElement(firstEvent, epochMillis(firstEvent.getEventTime()));
            harness.processBroadcastElement(version13, epochMillis(version13.getPublishedAt()));
            harness.processElement(secondEvent, epochMillis(secondEvent.getEventTime()));

            List<DecisionResult> results = harness.extractOutputValues();
            assertEquals(2, results.size(), "errors=" + sideOutput(harness, EngineOutputTags.ENGINE_ERROR));
            assertEquals(12, results.get(0).getVersion());
            assertEquals(ActionType.REJECT, results.get(0).getFinalAction());
            assertEquals(13, results.get(1).getVersion());
            assertEquals(ActionType.PASS, results.get(1).getFinalAction());
            assertTrue(sideOutput(harness, EngineOutputTags.ENGINE_ERROR).isEmpty());
        }
    }

    @Test
    void shouldRegisterAndTriggerEventTimeCleanupTimer() throws Exception {
        try (KeyedBroadcastOperatorTestHarness<String, RiskEvent, SceneSnapshotEnvelope, DecisionResult> harness = newHarness()) {
            SceneSnapshotEnvelope envelope = timerOnlyEnvelope();
            RiskEvent firstEvent = timerEvent("E202603071001", "T202603071001", Instant.parse("2026-03-07T10:00:00Z"));
            RiskEvent secondEvent = timerEvent("E202603071002", "T202603071002", Instant.parse("2026-03-07T10:12:00Z"));

            harness.processBroadcastElement(envelope, epochMillis(envelope.getPublishedAt()));
            harness.processElement(firstEvent, epochMillis(firstEvent.getEventTime()));
            assertEquals(1, harness.numEventTimeTimers());

            long beforeCleanup = Instant.parse("2026-03-07T10:10:59Z").toEpochMilli();
            harness.processWatermark(beforeCleanup);
            harness.processBroadcastWatermark(beforeCleanup);
            assertEquals(1, harness.numEventTimeTimers());

            long cleanupWatermark = Instant.parse("2026-03-07T10:11:00.001Z").toEpochMilli();
            harness.processWatermark(cleanupWatermark);
            harness.processBroadcastWatermark(cleanupWatermark);
            assertEquals(0, harness.numEventTimeTimers());

            harness.processElement(secondEvent, epochMillis(secondEvent.getEventTime()));
            List<DecisionResult> results = harness.extractOutputValues();
            assertEquals(2, results.size());
            assertEquals("1", results.get(1).getFeatureSnapshot().get("user_trade_cnt_5m"));
            assertTrue(sideOutput(harness, EngineOutputTags.ENGINE_ERROR).isEmpty());
        }
    }

    @Test
    void shouldOutputConflictErrorWhenSameVersionHasDifferentChecksum() throws Exception {
        try (KeyedBroadcastOperatorTestHarness<String, RiskEvent, SceneSnapshotEnvelope, DecisionResult> harness = newHarness()) {
            SceneSnapshotEnvelope baseline = baselineEnvelopeForBroadcastSwitch();
            SceneSnapshotEnvelope conflict = baselineEnvelopeForBroadcastSwitch();
            conflict.setChecksum("switch-checksum-conflict");
            conflict.getSnapshot().setChecksum("switch-checksum-conflict");
            conflict.setPublishedAt(Instant.parse("2026-03-07T20:01:00Z"));
            conflict.getSnapshot().setPublishedAt(conflict.getPublishedAt());
            RiskEvent event = copy(DemoFixtures.blacklistedEvent(), RiskEvent.class);

            harness.processBroadcastElement(baseline, epochMillis(baseline.getPublishedAt()));
            harness.processBroadcastElement(conflict, epochMillis(conflict.getPublishedAt()));
            harness.processElement(event, epochMillis(event.getEventTime()));

            List<DecisionResult> results = harness.extractOutputValues();
            assertEquals(1, results.size());
            assertEquals(12, results.get(0).getVersion());
            assertEquals(ActionType.REJECT, results.get(0).getFinalAction());
            assertEquals(1, sideOutput(harness, EngineOutputTags.ENGINE_ERROR).size());
            assertTrue(sideOutput(harness, EngineOutputTags.ENGINE_ERROR).toString().contains("same version but different checksum"));
        }
    }

    private KeyedBroadcastOperatorTestHarness<String, RiskEvent, SceneSnapshotEnvelope, DecisionResult> newHarness() throws Exception {
        KeyedBroadcastOperatorTestHarness<String, RiskEvent, SceneSnapshotEnvelope, DecisionResult> harness =
                new KeyedBroadcastOperatorTestHarness<>(
                        new CoBroadcastWithKeyedOperator<>(
                                new DecisionBroadcastProcessFunction(SNAPSHOT_STATE_DESCRIPTOR),
                                List.of(SNAPSHOT_STATE_DESCRIPTOR)
                        ),
                        RiskEvent::routeKey,
                        Types.STRING,
                        1,
                        1,
                        0
                );
        harness.setup(EngineTypeInfos.decisionResult().createSerializer(new SerializerConfigImpl()));
        harness.open();
        return harness;
    }

    private SceneSnapshotEnvelope baselineEnvelopeForBroadcastSwitch() {
        SceneSnapshot snapshot = DemoFixtures.demoSnapshot();
        snapshot.setVersion(12);
        snapshot.setChecksum("switch-checksum-v12");
        snapshot.setPublishedAt(Instant.parse("2026-03-07T20:00:00Z"));
        snapshot.setEffectiveFrom(snapshot.getPublishedAt());
        snapshot.setStreamFeatures(List.of());
        snapshot.setDerivedFeatures(List.of());
        snapshot.setLookupFeatures(List.of(copy(snapshot.getLookupFeatures().get(0), cn.liboshuai.pulsix.engine.model.LookupFeatureSpec.class)));
        snapshot.setRules(List.of(copy(snapshot.getRules().get(0), RuleSpec.class)));
        snapshot.getPolicy().setRuleOrder(List.of("R001"));
        return envelopeOf(snapshot);
    }

    private SceneSnapshotEnvelope disableBlacklistRuleEnvelope() {
        SceneSnapshot snapshot = DemoFixtures.demoSnapshot();
        snapshot.setVersion(13);
        snapshot.setChecksum("switch-checksum-v13");
        snapshot.setPublishedAt(Instant.parse("2026-03-07T20:05:00Z"));
        snapshot.setEffectiveFrom(snapshot.getPublishedAt());
        snapshot.setStreamFeatures(List.of());
        snapshot.setDerivedFeatures(List.of());
        snapshot.setLookupFeatures(List.of(copy(snapshot.getLookupFeatures().get(0), cn.liboshuai.pulsix.engine.model.LookupFeatureSpec.class)));
        RuleSpec rule = copy(snapshot.getRules().get(0), RuleSpec.class);
        rule.setEnabled(false);
        snapshot.setRules(List.of(rule));
        snapshot.getPolicy().setRuleOrder(List.of("R001"));
        return envelopeOf(snapshot);
    }

    private SceneSnapshotEnvelope timerOnlyEnvelope() {
        SceneSnapshot snapshot = DemoFixtures.demoSnapshot();
        snapshot.setSnapshotId("TRADE_RISK_TIMER_v1");
        snapshot.setSceneCode("TRADE_RISK_TIMER");
        snapshot.setSceneName("交易风控-定时器测试");
        snapshot.setVersion(1);
        snapshot.setChecksum("timer-checksum-v1");
        snapshot.setPublishedAt(Instant.parse("2026-03-07T09:55:00Z"));
        snapshot.setEffectiveFrom(snapshot.getPublishedAt());
        snapshot.setStreamFeatures(List.of(copy(snapshot.getStreamFeatures().get(0), cn.liboshuai.pulsix.engine.model.StreamFeatureSpec.class)));
        snapshot.setLookupFeatures(List.of());
        snapshot.setDerivedFeatures(List.of());
        snapshot.setRules(List.of());
        return envelopeOf(snapshot);
    }

    private RiskEvent timerEvent(String eventId, String traceId, Instant eventTime) {
        RiskEvent event = copy(DemoFixtures.demoEvents().get(0), RiskEvent.class);
        event.setSceneCode("TRADE_RISK_TIMER");
        event.setEventId(eventId);
        event.setTraceId(traceId);
        event.setEventTime(eventTime);
        event.setUserId("U_TIMER_1");
        event.setDeviceId("D_TIMER_1");
        return event;
    }

    private SceneSnapshotEnvelope envelopeOf(SceneSnapshot snapshot) {
        SceneSnapshotEnvelope envelope = new SceneSnapshotEnvelope();
        envelope.setSceneCode(snapshot.getSceneCode());
        envelope.setVersion(snapshot.getVersion());
        envelope.setChecksum(snapshot.getChecksum());
        envelope.setPublishedAt(snapshot.getPublishedAt());
        envelope.setEffectiveFrom(snapshot.getEffectiveFrom());
        envelope.setSnapshot(snapshot);
        return envelope;
    }

    private long epochMillis(Instant instant) {
        return instant == null ? 0L : instant.toEpochMilli();
    }

    private <T> T copy(Object value, Class<T> type) {
        return EngineJson.read(EngineJson.write(value), type);
    }

    @SuppressWarnings("unchecked")
    private <T> Queue<?> sideOutput(KeyedBroadcastOperatorTestHarness<String, RiskEvent, SceneSnapshotEnvelope, DecisionResult> harness,
                                    org.apache.flink.util.OutputTag<T> outputTag) {
        Queue<?> queue = harness.getSideOutput(outputTag);
        return queue == null ? new java.util.ArrayDeque<>() : queue;
    }

}
