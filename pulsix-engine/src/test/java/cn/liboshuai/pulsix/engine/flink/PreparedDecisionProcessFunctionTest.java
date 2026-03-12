package cn.liboshuai.pulsix.engine.flink;

import cn.liboshuai.pulsix.engine.demo.DemoFixtures;
import cn.liboshuai.pulsix.engine.feature.InMemoryLookupService;
import cn.liboshuai.pulsix.engine.json.EngineJson;
import cn.liboshuai.pulsix.engine.model.ActionType;
import cn.liboshuai.pulsix.engine.model.DecisionLogRecord;
import cn.liboshuai.pulsix.engine.model.DecisionResult;
import cn.liboshuai.pulsix.engine.model.RiskEvent;
import cn.liboshuai.pulsix.engine.model.RuleSpec;
import cn.liboshuai.pulsix.engine.model.SceneSnapshot;
import org.apache.flink.api.common.serialization.SerializerConfigImpl;
import org.apache.flink.streaming.api.operators.ProcessOperator;
import org.apache.flink.streaming.runtime.streamrecord.StreamRecord;
import org.apache.flink.streaming.util.OneInputStreamOperatorTestHarness;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Queue;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PreparedDecisionProcessFunctionTest {

    @Test
    void shouldEmitFullDecisionLogWhenEnabled() throws Exception {
        try (OneInputStreamOperatorTestHarness<PreparedDecisionInput, DecisionResult> harness = newHarness()) {
            RiskEvent event = copy(DemoFixtures.blacklistedEvent(), RiskEvent.class);
            PreparedDecisionInput input = preparedInput(minimalDecisionLogSnapshot(true), event);

            harness.processElement(input, epochMillis(event.getEventTime()));

            List<DecisionResult> results = harness.extractOutputValues();
            assertEquals(1, results.size());
            assertEquals(ActionType.REJECT, results.get(0).getFinalAction());

            Queue<?> decisionLogs = sideOutput(harness, EngineOutputTags.DECISION_LOG);
            assertEquals(1, decisionLogs.size());
            @SuppressWarnings("unchecked")
            DecisionLogRecord record = ((StreamRecord<DecisionLogRecord>) decisionLogs.peek()).getValue();
            assertEquals(ActionType.REJECT, record.getFinalAction());
            assertEquals(1, record.getRuleHits().size());
            assertEquals("R001", record.getRuleHits().get(0).getRuleCode());
            assertEquals("true", record.getFeatureSnapshot().get("device_in_blacklist"));
            assertTrue(record.getTraceLogs().contains("lookup:device_in_blacklist=true"));
        }
    }

    @Test
    void shouldTrimDecisionLogWhenFullDecisionLogDisabled() throws Exception {
        try (OneInputStreamOperatorTestHarness<PreparedDecisionInput, DecisionResult> harness = newHarness()) {
            RiskEvent event = copy(DemoFixtures.blacklistedEvent(), RiskEvent.class);
            PreparedDecisionInput input = preparedInput(minimalDecisionLogSnapshot(false), event);

            harness.processElement(input, epochMillis(event.getEventTime()));

            List<DecisionResult> results = harness.extractOutputValues();
            assertEquals(1, results.size());
            assertEquals(ActionType.REJECT, results.get(0).getFinalAction());

            Queue<?> decisionLogs = sideOutput(harness, EngineOutputTags.DECISION_LOG);
            assertEquals(1, decisionLogs.size());
            @SuppressWarnings("unchecked")
            DecisionLogRecord record = ((StreamRecord<DecisionLogRecord>) decisionLogs.peek()).getValue();
            assertEquals(ActionType.REJECT, record.getFinalAction());
            assertEquals(1, record.getRuleHits().size());
            assertEquals("R001", record.getRuleHits().get(0).getRuleCode());
            assertNull(record.getFeatureSnapshot());
            assertNull(record.getTraceLogs());
        }
    }

    private OneInputStreamOperatorTestHarness<PreparedDecisionInput, DecisionResult> newHarness()
            throws Exception {
        OneInputStreamOperatorTestHarness<PreparedDecisionInput, DecisionResult> harness =
                new OneInputStreamOperatorTestHarness<>(
                        new ProcessOperator<>(new PreparedDecisionProcessFunction(InMemoryLookupService::demo))
                );
        harness.setup(cn.liboshuai.pulsix.engine.flink.typeinfo.EngineTypeInfos.decisionResult()
                .createSerializer(new SerializerConfigImpl()));
        harness.open();
        return harness;
    }

    private PreparedDecisionInput preparedInput(SceneSnapshot snapshot,
                                                RiskEvent event) {
        PreparedDecisionInput input = new PreparedDecisionInput();
        input.setSceneCode(event.getSceneCode());
        input.setEventJoinKey(event.getSceneCode() + '|' + event.getEventId());
        input.setPreparedAtEpochMs(epochMillis(event.getEventTime()));
        input.setEvent(event);
        input.setSnapshot(snapshot);
        input.setFeatureSnapshot(Map.of());
        return input;
    }

    private SceneSnapshot minimalDecisionLogSnapshot(boolean needFullDecisionLog) {
        SceneSnapshot snapshot = copy(DemoFixtures.demoSnapshot(), SceneSnapshot.class);
        snapshot.setVersion(16);
        snapshot.setChecksum(needFullDecisionLog ? "prepared-log-full-v16" : "prepared-log-trim-v16");
        snapshot.setPublishedAt(Instant.parse("2026-03-07T20:12:00Z"));
        snapshot.setEffectiveFrom(snapshot.getPublishedAt());
        snapshot.setStreamFeatures(List.of());
        snapshot.setDerivedFeatures(List.of());
        snapshot.setLookupFeatures(List.of(copy(snapshot.getLookupFeatures().get(0), cn.liboshuai.pulsix.engine.model.LookupFeatureSpec.class)));
        snapshot.setRules(List.of(copy(snapshot.getRules().get(0), RuleSpec.class)));
        snapshot.getPolicy().setRuleOrder(List.of("R001"));
        snapshot.getRuntimeHints().setNeedFullDecisionLog(needFullDecisionLog);
        return snapshot;
    }

    private long epochMillis(Instant instant) {
        return instant == null ? 0L : instant.toEpochMilli();
    }

    private <T> T copy(Object value,
                       Class<T> type) {
        return EngineJson.read(EngineJson.write(value), type);
    }

    @SuppressWarnings("unchecked")
    private <T> Queue<?> sideOutput(OneInputStreamOperatorTestHarness<PreparedDecisionInput, DecisionResult> harness,
                                    org.apache.flink.util.OutputTag<T> outputTag) {
        Queue<?> queue = harness.getSideOutput(outputTag);
        return queue == null ? new java.util.ArrayDeque<>() : queue;
    }

}
