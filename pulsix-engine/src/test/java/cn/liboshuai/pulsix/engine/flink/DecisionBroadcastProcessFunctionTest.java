package cn.liboshuai.pulsix.engine.flink;

import cn.liboshuai.pulsix.engine.demo.DemoFixtures;
import cn.liboshuai.pulsix.engine.feature.StreamFeatureStateStore;
import cn.liboshuai.pulsix.engine.feature.LookupResult;
import cn.liboshuai.pulsix.engine.flink.runtime.SceneReleaseTimeline;
import cn.liboshuai.pulsix.engine.flink.typeinfo.EngineTypeInfos;
import cn.liboshuai.pulsix.engine.json.EngineJson;
import cn.liboshuai.pulsix.engine.model.ActionType;
import cn.liboshuai.pulsix.engine.model.DecisionLogRecord;
import cn.liboshuai.pulsix.engine.model.DecisionResult;
import cn.liboshuai.pulsix.engine.model.EngineErrorCodes;
import cn.liboshuai.pulsix.engine.model.EngineErrorRecord;
import cn.liboshuai.pulsix.engine.model.EngineErrorTypes;
import cn.liboshuai.pulsix.engine.model.PublishType;
import cn.liboshuai.pulsix.engine.model.RiskEvent;
import cn.liboshuai.pulsix.engine.model.RuleSpec;
import cn.liboshuai.pulsix.engine.model.SceneSnapshot;
import cn.liboshuai.pulsix.engine.model.SceneSnapshotEnvelope;
import cn.liboshuai.pulsix.engine.simulation.LocalReplayRunner;
import cn.liboshuai.pulsix.engine.simulation.LocalSimulationRunner;
import org.apache.flink.api.common.state.MapStateDescriptor;
import org.apache.flink.api.common.serialization.SerializerConfigImpl;
import org.apache.flink.api.common.typeinfo.Types;
import org.apache.flink.runtime.checkpoint.OperatorSubtaskState;
import org.apache.flink.streaming.api.operators.co.CoBroadcastWithKeyedOperator;
import org.apache.flink.streaming.runtime.streamrecord.StreamRecord;
import org.apache.flink.streaming.util.KeyedBroadcastOperatorTestHarness;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Queue;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DecisionBroadcastProcessFunctionTest {

    private static final MapStateDescriptor<String, SceneReleaseTimeline> SNAPSHOT_STATE_DESCRIPTOR = new MapStateDescriptor<>(
            "scene-snapshot-broadcast-state",
            Types.STRING,
            EngineTypeInfos.sceneReleaseTimeline()
    );

    @Test
    void shouldSnapshotBroadcastStateWithoutKryoFallback() throws Exception {
        try (KeyedBroadcastOperatorTestHarness<String, RiskEvent, SceneSnapshotEnvelope, DecisionResult> harness = newHarness()) {
            SceneSnapshotEnvelope envelope = baselineEnvelopeForBroadcastSwitch();

            harness.setProcessingTime(epochMillis(envelope.getEffectiveFrom()) + 1L);
            harness.processBroadcastElement(envelope, epochMillis(envelope.getPublishedAt()));
            harness.snapshot(1L, epochMillis(envelope.getPublishedAt()) + 1L);
        }
    }

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

            harness.setProcessingTime(epochMillis(version12.getEffectiveFrom()) + 1L);
            harness.processBroadcastElement(version12, epochMillis(version12.getPublishedAt()));
            harness.processElement(firstEvent, epochMillis(firstEvent.getEventTime()));
            harness.setProcessingTime(epochMillis(version13.getEffectiveFrom()) + 1L);
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

            harness.setProcessingTime(epochMillis(envelope.getEffectiveFrom()) + 1L);
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
    void shouldRestoreRuntimeAndKeyedStateAfterCheckpoint() throws Exception {
        SceneSnapshotEnvelope envelope = DemoFixtures.demoEnvelope();
        List<RiskEvent> events = DemoFixtures.demoEvents();
        OperatorSubtaskState snapshot;

        try (KeyedBroadcastOperatorTestHarness<String, RiskEvent, SceneSnapshotEnvelope, DecisionResult> harness = newHarness()) {
            harness.setProcessingTime(epochMillis(envelope.getEffectiveFrom()) + 1L);
            harness.processBroadcastElement(envelope, epochMillis(envelope.getPublishedAt()));
            for (int index = 0; index < events.size() - 1; index++) {
                RiskEvent event = copy(events.get(index), RiskEvent.class);
                harness.processElement(event, epochMillis(event.getEventTime()));
            }
            snapshot = harness.snapshot(100L, epochMillis(envelope.getPublishedAt()) + 1_000L);
        }

        try (KeyedBroadcastOperatorTestHarness<String, RiskEvent, SceneSnapshotEnvelope, DecisionResult> harness = restoredHarness(snapshot)) {
            harness.setProcessingTime(epochMillis(envelope.getEffectiveFrom()) + 2L);
            RiskEvent finalEvent = copy(events.get(events.size() - 1), RiskEvent.class);
            harness.processElement(finalEvent, epochMillis(finalEvent.getEventTime()));

            List<DecisionResult> results = harness.extractOutputValues();
            assertEquals(1, results.size(), "errors=" + sideOutput(harness, EngineOutputTags.ENGINE_ERROR));
            assertEquals(12, results.get(0).getVersion());
            assertEquals("TRADE_RISK_v12", results.get(0).getSnapshotId());
            assertEquals(ActionType.REJECT, results.get(0).getFinalAction());
            assertEquals(80, results.get(0).getFinalScore());
            assertEquals("3", results.get(0).getFeatureSnapshot().get("user_trade_cnt_5m"));
            assertEquals("4", results.get(0).getFeatureSnapshot().get("device_bind_user_cnt_1h"));
            assertTrue(sideOutput(harness, EngineOutputTags.ENGINE_ERROR).isEmpty());
        }
    }

    @Test
    void shouldRestoreCleanupTimerAfterCheckpoint() throws Exception {
        SceneSnapshotEnvelope envelope = timerOnlyEnvelope();
        RiskEvent firstEvent = timerEvent("E202603071011", "T202603071011", Instant.parse("2026-03-07T10:00:00Z"));
        RiskEvent secondEvent = timerEvent("E202603071012", "T202603071012", Instant.parse("2026-03-07T10:12:00Z"));
        OperatorSubtaskState snapshot;

        try (KeyedBroadcastOperatorTestHarness<String, RiskEvent, SceneSnapshotEnvelope, DecisionResult> harness = newHarness()) {
            harness.setProcessingTime(epochMillis(envelope.getEffectiveFrom()) + 1L);
            harness.processBroadcastElement(envelope, epochMillis(envelope.getPublishedAt()));
            harness.processElement(firstEvent, epochMillis(firstEvent.getEventTime()));
            assertEquals(1, harness.numEventTimeTimers());
            snapshot = harness.snapshot(200L, epochMillis(envelope.getPublishedAt()) + 1_000L);
        }

        try (KeyedBroadcastOperatorTestHarness<String, RiskEvent, SceneSnapshotEnvelope, DecisionResult> harness = restoredHarness(snapshot)) {
            harness.setProcessingTime(epochMillis(envelope.getEffectiveFrom()) + 2L);
            harness.processElement(secondEvent, epochMillis(secondEvent.getEventTime()));
            List<DecisionResult> results = harness.extractOutputValues();
            assertEquals(1, results.size());
            assertEquals("1", results.get(0).getFeatureSnapshot().get("user_trade_cnt_5m"));
            assertTrue(sideOutput(harness, EngineOutputTags.ENGINE_ERROR).isEmpty());
        }
    }

    @Test
    void shouldKeepDemoBaselineConsistentAcrossSimulationReplayAndFlink() throws Exception {
        LocalSimulationRunner simulationRunner = new LocalSimulationRunner();
        LocalSimulationRunner.SimulationReport simulationReport = simulationRunner.simulate(
                DemoFixtures.demoEnvelopeJson(),
                DemoFixtures.toJson(DemoFixtures.demoEvents()));

        LocalReplayRunner replayRunner = new LocalReplayRunner();
        LocalReplayRunner.ReplayReport replayReport = replayRunner.replay(
                DemoFixtures.demoEnvelopeJson(),
                DemoFixtures.toJson(DemoFixtures.demoEvents()));

        try (KeyedBroadcastOperatorTestHarness<String, RiskEvent, SceneSnapshotEnvelope, DecisionResult> harness = newHarness()) {
            SceneSnapshotEnvelope envelope = DemoFixtures.demoEnvelope();
            harness.setProcessingTime(epochMillis(envelope.getEffectiveFrom()) + 1L);
            harness.processBroadcastElement(envelope, epochMillis(envelope.getPublishedAt()));
            for (RiskEvent event : DemoFixtures.demoEvents()) {
                harness.processElement(copy(event, RiskEvent.class), epochMillis(event.getEventTime()));
            }

            List<DecisionResult> results = harness.extractOutputValues();
            DecisionResult flinkFinalResult = results.get(results.size() - 1);
            assertEquals(simulationReport.getFinalResult().getFinalAction(), replayReport.getFinalResult().getFinalAction());
            assertEquals(simulationReport.getFinalResult().getFinalAction(), flinkFinalResult.getFinalAction());
            assertEquals(simulationReport.getFinalResult().getFinalScore(), replayReport.getFinalResult().getFinalScore());
            assertEquals(simulationReport.getFinalResult().getFinalScore(), flinkFinalResult.getFinalScore());
            assertEquals(
                    simulationReport.getFinalResult().getHitRules().stream().map(LocalSimulationRunner.MatchedRule::getRuleCode).toList(),
                    replayReport.getFinalResult().getHitRules().stream().map(LocalSimulationRunner.MatchedRule::getRuleCode).toList()
            );
            assertEquals(
                    simulationReport.getFinalResult().getHitRules().stream().map(LocalSimulationRunner.MatchedRule::getRuleCode).toList(),
                    flinkFinalResult.getRuleHits().stream().filter(hit -> Boolean.TRUE.equals(hit.getHit())).map(cn.liboshuai.pulsix.engine.model.RuleHit::getRuleCode).toList()
            );
            assertTrue(sideOutput(harness, EngineOutputTags.ENGINE_ERROR).isEmpty());
        }
    }

    @Test
    void shouldClassifyStateErrorWhenStreamFeatureStateStoreFails() throws Exception {
        try (KeyedBroadcastOperatorTestHarness<String, RiskEvent, SceneSnapshotEnvelope, DecisionResult> harness = newHarness(
                null,
                runtimeContext -> new StreamFeatureStateStore() {
                    @Override
                    public Object evaluate(cn.liboshuai.pulsix.engine.runtime.CompiledSceneRuntime.CompiledStreamFeature feature,
                                           cn.liboshuai.pulsix.engine.context.EvalContext context) {
                        throw new IllegalStateException("numeric keyed state failed");
                    }
                })) {
            SceneSnapshotEnvelope envelope = timerOnlyEnvelope();
            RiskEvent event = timerEvent("E202603071021", "T202603071021", Instant.parse("2026-03-07T10:00:00Z"));

            harness.setProcessingTime(epochMillis(envelope.getEffectiveFrom()) + 1L);
            harness.processBroadcastElement(envelope, epochMillis(envelope.getPublishedAt()));
            harness.processElement(event, epochMillis(event.getEventTime()));

            assertTrue(harness.extractOutputValues().isEmpty());
            Queue<?> errors = sideOutput(harness, EngineOutputTags.ENGINE_ERROR);
            assertEquals(1, errors.size());
            @SuppressWarnings("unchecked")
            EngineErrorRecord record = ((StreamRecord<EngineErrorRecord>) errors.peek()).getValue();
            assertEquals("decision-stream-feature", record.getStage());
            assertEquals(EngineErrorTypes.STATE, record.getErrorType());
            assertEquals(EngineErrorCodes.STATE_ACCESS_FAILED, record.getErrorCode());
            assertEquals("user_trade_cnt_5m", record.getFeatureCode());
        }
    }

    @Test
    void shouldClassifyExecutionErrorWhenRuleEvaluationThrows() throws Exception {
        try (KeyedBroadcastOperatorTestHarness<String, RiskEvent, SceneSnapshotEnvelope, DecisionResult> harness = newHarness()) {
            SceneSnapshotEnvelope envelope = brokenRuleExecutionEnvelope();
            RiskEvent event = copy(DemoFixtures.blacklistedEvent(), RiskEvent.class);

            harness.setProcessingTime(epochMillis(envelope.getEffectiveFrom()) + 1L);
            harness.processBroadcastElement(envelope, epochMillis(envelope.getPublishedAt()));
            harness.processElement(event, epochMillis(event.getEventTime()));

            assertTrue(harness.extractOutputValues().isEmpty());
            Queue<?> errors = sideOutput(harness, EngineOutputTags.ENGINE_ERROR);
            assertEquals(1, errors.size());
            @SuppressWarnings("unchecked")
            EngineErrorRecord record = ((StreamRecord<EngineErrorRecord>) errors.peek()).getValue();
            assertEquals("decision-rule", record.getStage());
            assertEquals(EngineErrorTypes.EXECUTION, record.getErrorType());
            assertEquals(EngineErrorCodes.RULE_EXECUTION_FAILED, record.getErrorCode());
            assertEquals("R-BROKEN", record.getRuleCode());
            assertEquals("GROOVY", record.getEngineType());
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

            harness.setProcessingTime(epochMillis(baseline.getEffectiveFrom()) + 1L);
            harness.processBroadcastElement(baseline, epochMillis(baseline.getPublishedAt()));
            harness.processBroadcastElement(conflict, epochMillis(conflict.getPublishedAt()));
            harness.processElement(event, epochMillis(event.getEventTime()));

            List<DecisionResult> results = harness.extractOutputValues();
            assertEquals(1, results.size());
            assertEquals(12, results.get(0).getVersion());
            assertEquals(ActionType.REJECT, results.get(0).getFinalAction());
            Queue<?> errors = sideOutput(harness, EngineOutputTags.ENGINE_ERROR);
            assertEquals(1, errors.size());
            @SuppressWarnings("unchecked")
            EngineErrorRecord record = ((StreamRecord<EngineErrorRecord>) errors.peek()).getValue();
            assertEquals(EngineErrorTypes.SNAPSHOT, record.getErrorType());
            assertEquals(EngineErrorCodes.SNAPSHOT_VERSION_CONFLICT, record.getErrorCode());
            assertTrue(record.getErrorMessage().contains("same version but different checksum"));
        }
    }

    @Test
    void shouldNotPoisonRuntimeCacheWhenConflictingSnapshotIsRejected() throws Exception {
        try (KeyedBroadcastOperatorTestHarness<String, RiskEvent, SceneSnapshotEnvelope, DecisionResult> harness = newHarness()) {
            SceneSnapshotEnvelope baseline = baselineEnvelopeForBroadcastSwitch();
            SceneSnapshotEnvelope conflict = disableBlacklistRuleEnvelope();
            conflict.getSnapshot().setVersion(baseline.getVersion());
            conflict.getSnapshot().setChecksum("switch-checksum-conflict");
            conflict.getSnapshot().setPublishedAt(Instant.parse("2026-03-07T20:01:00Z"));
            conflict.getSnapshot().setEffectiveFrom(baseline.getEffectiveFrom());
            conflict = envelopeOf(conflict.getSnapshot());

            SceneSnapshotEnvelope version13 = disableBlacklistRuleEnvelope();
            version13.getSnapshot().setPublishedAt(Instant.parse("2026-03-07T20:05:00Z"));
            version13.getSnapshot().setEffectiveFrom(Instant.parse("2026-03-07T20:05:00Z"));
            version13 = envelopeOf(version13.getSnapshot());

            SceneSnapshotEnvelope rollback12 = rollbackEnvelope(baseline,
                    Instant.parse("2026-03-07T20:10:00Z"),
                    Instant.parse("2026-03-07T20:10:00Z"));

            RiskEvent firstEvent = copy(DemoFixtures.blacklistedEvent(), RiskEvent.class);
            RiskEvent secondEvent = copy(DemoFixtures.blacklistedEvent(), RiskEvent.class);
            RiskEvent thirdEvent = copy(DemoFixtures.blacklistedEvent(), RiskEvent.class);
            secondEvent.setEventId("E202603070155");
            secondEvent.setTraceId("T202603070155");
            secondEvent.setEventTime(Instant.parse("2026-03-07T20:05:30Z"));
            thirdEvent.setEventId("E202603070255");
            thirdEvent.setTraceId("T202603070255");
            thirdEvent.setEventTime(Instant.parse("2026-03-07T20:10:30Z"));

            harness.setProcessingTime(epochMillis(baseline.getEffectiveFrom()) + 1L);
            harness.processBroadcastElement(baseline, epochMillis(baseline.getPublishedAt()));
            harness.processBroadcastElement(conflict, epochMillis(conflict.getPublishedAt()));
            harness.processElement(firstEvent, epochMillis(firstEvent.getEventTime()));

            harness.setProcessingTime(epochMillis(version13.getEffectiveFrom()) + 1L);
            harness.processBroadcastElement(version13, epochMillis(version13.getPublishedAt()));
            harness.processElement(secondEvent, epochMillis(secondEvent.getEventTime()));

            harness.setProcessingTime(epochMillis(rollback12.getEffectiveFrom()) + 1L);
            harness.processBroadcastElement(rollback12, epochMillis(rollback12.getPublishedAt()));
            harness.processElement(thirdEvent, epochMillis(thirdEvent.getEventTime()));

            List<DecisionResult> results = harness.extractOutputValues();
            assertEquals(3, results.size(), "errors=" + sideOutput(harness, EngineOutputTags.ENGINE_ERROR));
            assertEquals(12, results.get(0).getVersion());
            assertEquals(ActionType.REJECT, results.get(0).getFinalAction());
            assertEquals(13, results.get(1).getVersion());
            assertEquals(ActionType.PASS, results.get(1).getFinalAction());
            assertEquals(12, results.get(2).getVersion());
            assertEquals(ActionType.REJECT, results.get(2).getFinalAction());
            assertEquals(1, sideOutput(harness, EngineOutputTags.ENGINE_ERROR).size());
            assertTrue(sideOutput(harness, EngineOutputTags.ENGINE_ERROR).toString().contains("same version but different checksum"));
        }
    }

    @Test
    void shouldEmitLookupErrorAndContinueWithFallbackValue() throws Exception {
        try (KeyedBroadcastOperatorTestHarness<String, RiskEvent, SceneSnapshotEnvelope, DecisionResult> harness = newHarness(
                () -> new cn.liboshuai.pulsix.engine.feature.LookupService() {
                    @Override
                    public LookupResult lookup(cn.liboshuai.pulsix.engine.runtime.CompiledSceneRuntime.CompiledLookupFeature feature,
                                               cn.liboshuai.pulsix.engine.context.EvalContext context) {
                        String lookupKey = feature.getKeyScript() == null ? null : String.valueOf(feature.getKeyScript().execute(context));
                        return LookupResult.fallback(Boolean.FALSE,
                                lookupKey,
                                LookupResult.ERROR_TIMEOUT,
                                "redis lookup timed out",
                                LookupResult.FALLBACK_DEFAULT_VALUE);
                    }
                })) {
            SceneSnapshotEnvelope envelope = baselineEnvelopeForBroadcastSwitch();
            RiskEvent event = copy(DemoFixtures.blacklistedEvent(), RiskEvent.class);

            harness.setProcessingTime(epochMillis(envelope.getEffectiveFrom()) + 1L);
            harness.processBroadcastElement(envelope, epochMillis(envelope.getPublishedAt()));
            harness.processElement(event, epochMillis(event.getEventTime()));

            List<DecisionResult> results = harness.extractOutputValues();
            assertEquals(1, results.size());
            assertEquals(ActionType.PASS, results.get(0).getFinalAction());

            Queue<?> errors = sideOutput(harness, EngineOutputTags.ENGINE_ERROR);
            assertEquals(1, errors.size());
            @SuppressWarnings("unchecked")
            EngineErrorRecord record = ((StreamRecord<EngineErrorRecord>) errors.peek()).getValue();
            assertEquals("decision-lookup", record.getStage());
            assertEquals(EngineErrorTypes.LOOKUP, record.getErrorType());
            assertEquals(LookupResult.ERROR_TIMEOUT, record.getErrorCode());
            assertEquals("device_in_blacklist", record.getFeatureCode());
            assertEquals(LookupResult.FALLBACK_DEFAULT_VALUE, record.getFallbackMode());
        }
    }

    @Test
    void shouldFallbackWhenLookupServiceThrowsUnexpectedException() throws Exception {
        try (KeyedBroadcastOperatorTestHarness<String, RiskEvent, SceneSnapshotEnvelope, DecisionResult> harness = newHarness(
                () -> new cn.liboshuai.pulsix.engine.feature.LookupService() {
                    @Override
                    public LookupResult lookup(cn.liboshuai.pulsix.engine.runtime.CompiledSceneRuntime.CompiledLookupFeature feature,
                                               cn.liboshuai.pulsix.engine.context.EvalContext context) {
                        throw new IllegalStateException("lookup exploded");
                    }
                })) {
            SceneSnapshotEnvelope envelope = baselineEnvelopeForBroadcastSwitch();
            RiskEvent event = copy(DemoFixtures.blacklistedEvent(), RiskEvent.class);

            harness.setProcessingTime(epochMillis(envelope.getEffectiveFrom()) + 1L);
            harness.processBroadcastElement(envelope, epochMillis(envelope.getPublishedAt()));
            harness.processElement(event, epochMillis(event.getEventTime()));

            List<DecisionResult> results = harness.extractOutputValues();
            assertEquals(1, results.size());
            assertEquals(ActionType.PASS, results.get(0).getFinalAction());

            Queue<?> errors = sideOutput(harness, EngineOutputTags.ENGINE_ERROR);
            assertEquals(1, errors.size());
            @SuppressWarnings("unchecked")
            EngineErrorRecord record = ((StreamRecord<EngineErrorRecord>) errors.peek()).getValue();
            assertEquals("decision-lookup", record.getStage());
            assertEquals(EngineErrorTypes.LOOKUP, record.getErrorType());
            assertEquals(LookupResult.ERROR_EXECUTION_FAILED, record.getErrorCode());
            assertEquals("device_in_blacklist", record.getFeatureCode());
            assertEquals(LookupResult.FALLBACK_DEFAULT_VALUE, record.getFallbackMode());
        }
    }

    @Test
    void shouldFlushBufferedEventsAfterSnapshotArrivesWithoutNeedingNextEvent() throws Exception {
        try (KeyedBroadcastOperatorTestHarness<String, RiskEvent, SceneSnapshotEnvelope, DecisionResult> harness = newHarness()) {
            SceneSnapshotEnvelope baseline = baselineEnvelopeForBroadcastSwitch();
            RiskEvent event = copy(DemoFixtures.blacklistedEvent(), RiskEvent.class);

            long runtimeReadyAt = epochMillis(baseline.getEffectiveFrom()) + 1L;
            harness.setProcessingTime(runtimeReadyAt);
            harness.processElement(event, epochMillis(event.getEventTime()));
            assertEquals(0, harness.extractOutputValues().size());

            harness.processBroadcastElement(baseline, epochMillis(baseline.getPublishedAt()));
            harness.setProcessingTime(runtimeReadyAt + 1_500L);

            List<DecisionResult> results = harness.extractOutputValues();
            assertEquals(1, results.size());
            assertEquals(12, results.get(0).getVersion());
            assertEquals(ActionType.REJECT, results.get(0).getFinalAction());
        }
    }

    @Test
    void shouldKeepFutureSnapshotPendingUntilEffectiveFrom() throws Exception {
        try (KeyedBroadcastOperatorTestHarness<String, RiskEvent, SceneSnapshotEnvelope, DecisionResult> harness = newHarness()) {
            SceneSnapshotEnvelope version12 = baselineEnvelopeForBroadcastSwitch();
            SceneSnapshotEnvelope version13 = disableBlacklistRuleEnvelope();
            version13.setEffectiveFrom(version13.getPublishedAt().plusSeconds(300));
            version13.getSnapshot().setEffectiveFrom(version13.getEffectiveFrom());
            RiskEvent beforeEffective = copy(DemoFixtures.blacklistedEvent(), RiskEvent.class);
            RiskEvent afterEffective = copy(DemoFixtures.blacklistedEvent(), RiskEvent.class);
            afterEffective.setEventId("E202603070299");
            afterEffective.setTraceId("T202603070299");
            afterEffective.setEventTime(beforeEffective.getEventTime().plusSeconds(600));

            harness.setProcessingTime(epochMillis(version12.getEffectiveFrom()) + 1L);
            harness.processBroadcastElement(version12, epochMillis(version12.getPublishedAt()));

            harness.setProcessingTime(epochMillis(version13.getPublishedAt()) + 1L);
            harness.processBroadcastElement(version13, epochMillis(version13.getPublishedAt()));
            harness.processElement(beforeEffective, epochMillis(beforeEffective.getEventTime()));

            harness.setProcessingTime(epochMillis(version13.getEffectiveFrom()) + 1L);
            harness.processElement(afterEffective, epochMillis(afterEffective.getEventTime()));

            List<DecisionResult> results = harness.extractOutputValues();
            assertEquals(2, results.size());
            assertEquals(12, results.get(0).getVersion());
            assertEquals(ActionType.REJECT, results.get(0).getFinalAction());
            assertEquals(13, results.get(1).getVersion());
            assertEquals(ActionType.PASS, results.get(1).getFinalAction());
        }
    }

    @Test
    void shouldActivateEarlierFutureVersionEvenIfHigherVersionArrivedFirst() throws Exception {
        try (KeyedBroadcastOperatorTestHarness<String, RiskEvent, SceneSnapshotEnvelope, DecisionResult> harness = newHarness()) {
            SceneSnapshotEnvelope version12 = baselineEnvelopeForBroadcastSwitch();
            SceneSnapshotEnvelope version14 = baselineEnvelopeForBroadcastSwitch();
            version14.getSnapshot().setVersion(14);
            version14.getSnapshot().setChecksum("switch-checksum-v14");
            version14.getSnapshot().setPublishedAt(Instant.parse("2026-03-07T20:06:00Z"));
            version14.getSnapshot().setEffectiveFrom(Instant.parse("2026-03-07T20:10:00Z"));
            version14 = envelopeOf(version14.getSnapshot());

            SceneSnapshotEnvelope version13 = disableBlacklistRuleEnvelope();
            version13.getSnapshot().setPublishedAt(Instant.parse("2026-03-07T20:07:00Z"));
            version13.getSnapshot().setEffectiveFrom(Instant.parse("2026-03-07T20:08:00Z"));
            version13 = envelopeOf(version13.getSnapshot());

            RiskEvent firstEvent = copy(DemoFixtures.blacklistedEvent(), RiskEvent.class);
            RiskEvent secondEvent = copy(DemoFixtures.blacklistedEvent(), RiskEvent.class);
            RiskEvent thirdEvent = copy(DemoFixtures.blacklistedEvent(), RiskEvent.class);
            secondEvent.setEventId("E202603070599");
            secondEvent.setTraceId("T202603070599");
            secondEvent.setEventTime(Instant.parse("2026-03-07T20:08:30Z"));
            thirdEvent.setEventId("E202603070699");
            thirdEvent.setTraceId("T202603070699");
            thirdEvent.setEventTime(Instant.parse("2026-03-07T20:10:30Z"));

            harness.setProcessingTime(epochMillis(version12.getEffectiveFrom()) + 1L);
            harness.processBroadcastElement(version12, epochMillis(version12.getPublishedAt()));
            harness.processElement(firstEvent, epochMillis(firstEvent.getEventTime()));

            harness.setProcessingTime(epochMillis(version14.getPublishedAt()) + 1L);
            harness.processBroadcastElement(version14, epochMillis(version14.getPublishedAt()));

            harness.setProcessingTime(epochMillis(version13.getPublishedAt()) + 1L);
            harness.processBroadcastElement(version13, epochMillis(version13.getPublishedAt()));
            harness.setProcessingTime(epochMillis(version13.getEffectiveFrom()) + 1L);
            harness.processElement(secondEvent, epochMillis(secondEvent.getEventTime()));

            harness.setProcessingTime(epochMillis(version14.getEffectiveFrom()) + 1L);
            harness.processElement(thirdEvent, epochMillis(thirdEvent.getEventTime()));

            List<DecisionResult> results = harness.extractOutputValues();
            assertEquals(3, results.size(), "errors=" + sideOutput(harness, EngineOutputTags.ENGINE_ERROR));
            assertEquals(12, results.get(0).getVersion());
            assertEquals(ActionType.REJECT, results.get(0).getFinalAction());
            assertEquals(13, results.get(1).getVersion());
            assertEquals(ActionType.PASS, results.get(1).getFinalAction());
            assertEquals(14, results.get(2).getVersion());
            assertEquals(ActionType.REJECT, results.get(2).getFinalAction());
        }
    }

    @Test
    void shouldRollbackToOlderVersionWhenPublishTypeIsRollback() throws Exception {
        try (KeyedBroadcastOperatorTestHarness<String, RiskEvent, SceneSnapshotEnvelope, DecisionResult> harness = newHarness()) {
            SceneSnapshotEnvelope version12 = baselineEnvelopeForBroadcastSwitch();
            SceneSnapshotEnvelope version13 = disableBlacklistRuleEnvelope();
            SceneSnapshotEnvelope rollback12 = rollbackEnvelope(version12,
                    Instant.parse("2026-03-07T20:10:00Z"),
                    Instant.parse("2026-03-07T20:10:00Z"));
            RiskEvent firstEvent = copy(DemoFixtures.blacklistedEvent(), RiskEvent.class);
            RiskEvent secondEvent = copy(DemoFixtures.blacklistedEvent(), RiskEvent.class);
            RiskEvent thirdEvent = copy(DemoFixtures.blacklistedEvent(), RiskEvent.class);
            secondEvent.setEventId("E202603070399");
            secondEvent.setTraceId("T202603070399");
            secondEvent.setEventTime(firstEvent.getEventTime().plusSeconds(30));
            thirdEvent.setEventId("E202603070499");
            thirdEvent.setTraceId("T202603070499");
            thirdEvent.setEventTime(firstEvent.getEventTime().plusSeconds(60));

            harness.setProcessingTime(epochMillis(version12.getEffectiveFrom()) + 1L);
            harness.processBroadcastElement(version12, epochMillis(version12.getPublishedAt()));
            harness.processElement(firstEvent, epochMillis(firstEvent.getEventTime()));

            harness.setProcessingTime(epochMillis(version13.getEffectiveFrom()) + 1L);
            harness.processBroadcastElement(version13, epochMillis(version13.getPublishedAt()));
            harness.processElement(secondEvent, epochMillis(secondEvent.getEventTime()));

            harness.setProcessingTime(epochMillis(rollback12.getEffectiveFrom()) + 1L);
            harness.processBroadcastElement(rollback12, epochMillis(rollback12.getPublishedAt()));
            harness.processElement(thirdEvent, epochMillis(thirdEvent.getEventTime()));

            List<DecisionResult> results = harness.extractOutputValues();
            assertEquals(3, results.size(), "errors=" + sideOutput(harness, EngineOutputTags.ENGINE_ERROR));
            assertEquals(12, results.get(0).getVersion());
            assertEquals(ActionType.REJECT, results.get(0).getFinalAction());
            assertEquals(13, results.get(1).getVersion());
            assertEquals(ActionType.PASS, results.get(1).getFinalAction());
            assertEquals(12, results.get(2).getVersion());
            assertEquals(ActionType.REJECT, results.get(2).getFinalAction());
            assertTrue(sideOutput(harness, EngineOutputTags.ENGINE_ERROR).isEmpty());
        }
    }

    @Test
    void shouldKeepCurrentRuntimeWhenNewSnapshotCompilationFails() throws Exception {
        try (KeyedBroadcastOperatorTestHarness<String, RiskEvent, SceneSnapshotEnvelope, DecisionResult> harness = newHarness()) {
            SceneSnapshotEnvelope version12 = baselineEnvelopeForBroadcastSwitch();
            SceneSnapshotEnvelope invalidVersion14 = groovyDisabledEnvelope();
            RiskEvent event = copy(DemoFixtures.blacklistedEvent(), RiskEvent.class);

            harness.setProcessingTime(epochMillis(version12.getEffectiveFrom()) + 1L);
            harness.processBroadcastElement(version12, epochMillis(version12.getPublishedAt()));

            harness.setProcessingTime(epochMillis(invalidVersion14.getEffectiveFrom()) + 1L);
            harness.processBroadcastElement(invalidVersion14, epochMillis(invalidVersion14.getPublishedAt()));
            harness.processElement(event, epochMillis(event.getEventTime()));

            List<DecisionResult> results = harness.extractOutputValues();
            assertEquals(1, results.size());
            assertEquals(12, results.get(0).getVersion());
            assertEquals(ActionType.REJECT, results.get(0).getFinalAction());
            Queue<?> errors = sideOutput(harness, EngineOutputTags.ENGINE_ERROR);
            assertEquals(1, errors.size());
            @SuppressWarnings("unchecked")
            EngineErrorRecord record = ((StreamRecord<EngineErrorRecord>) errors.peek()).getValue();
            assertEquals(EngineErrorTypes.SNAPSHOT, record.getErrorType());
            assertEquals(EngineErrorCodes.GROOVY_DISABLED, record.getErrorCode());
            assertEquals("R003", record.getRuleCode());
            assertEquals("GROOVY", record.getEngineType());
            assertTrue(record.getErrorMessage().contains("allowGroovy"));
        }
    }

    @Test
    void shouldTrimDecisionLogWhenFullDecisionLogDisabled() throws Exception {
        try (KeyedBroadcastOperatorTestHarness<String, RiskEvent, SceneSnapshotEnvelope, DecisionResult> harness = newHarness()) {
            SceneSnapshotEnvelope envelope = minimalDecisionLogEnvelope();
            RiskEvent event = copy(DemoFixtures.blacklistedEvent(), RiskEvent.class);

            harness.setProcessingTime(epochMillis(envelope.getEffectiveFrom()) + 1L);
            harness.processBroadcastElement(envelope, epochMillis(envelope.getPublishedAt()));
            harness.processElement(event, epochMillis(event.getEventTime()));

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

    private KeyedBroadcastOperatorTestHarness<String, RiskEvent, SceneSnapshotEnvelope, DecisionResult> newHarness() throws Exception {
        return newHarness(null);
    }

    private KeyedBroadcastOperatorTestHarness<String, RiskEvent, SceneSnapshotEnvelope, DecisionResult> newHarness(
            DecisionBroadcastProcessFunction.LookupServiceFactory lookupServiceFactory) throws Exception {
        return newHarness(lookupServiceFactory, null);
    }

    private KeyedBroadcastOperatorTestHarness<String, RiskEvent, SceneSnapshotEnvelope, DecisionResult> newHarness(
            DecisionBroadcastProcessFunction.LookupServiceFactory lookupServiceFactory,
            DecisionBroadcastProcessFunction.StreamFeatureStateStoreFactory stateStoreFactory) throws Exception {
        KeyedBroadcastOperatorTestHarness<String, RiskEvent, SceneSnapshotEnvelope, DecisionResult> harness =
                createHarness(lookupServiceFactory, stateStoreFactory);
        harness.open();
        return harness;
    }

    private KeyedBroadcastOperatorTestHarness<String, RiskEvent, SceneSnapshotEnvelope, DecisionResult> restoredHarness(
            OperatorSubtaskState snapshot) throws Exception {
        KeyedBroadcastOperatorTestHarness<String, RiskEvent, SceneSnapshotEnvelope, DecisionResult> harness =
                createHarness(null, null);
        harness.initializeState(snapshot);
        harness.open();
        return harness;
    }

    private KeyedBroadcastOperatorTestHarness<String, RiskEvent, SceneSnapshotEnvelope, DecisionResult> createHarness(
            DecisionBroadcastProcessFunction.LookupServiceFactory lookupServiceFactory,
            DecisionBroadcastProcessFunction.StreamFeatureStateStoreFactory stateStoreFactory) throws Exception {
        KeyedBroadcastOperatorTestHarness<String, RiskEvent, SceneSnapshotEnvelope, DecisionResult> harness =
                new KeyedBroadcastOperatorTestHarness<>(
                        new CoBroadcastWithKeyedOperator<>(
                                buildProcessFunction(lookupServiceFactory, stateStoreFactory),
                                List.of(SNAPSHOT_STATE_DESCRIPTOR)
                        ),
                        RiskEvent::routeKey,
                        Types.STRING,
                        1,
                        1,
                        0
                );
        harness.setup(EngineTypeInfos.decisionResult().createSerializer(new SerializerConfigImpl()));
        return harness;
    }

    private DecisionBroadcastProcessFunction buildProcessFunction(
            DecisionBroadcastProcessFunction.LookupServiceFactory lookupServiceFactory,
            DecisionBroadcastProcessFunction.StreamFeatureStateStoreFactory stateStoreFactory) {
        if (lookupServiceFactory == null && stateStoreFactory == null) {
            return new DecisionBroadcastProcessFunction(SNAPSHOT_STATE_DESCRIPTOR);
        }
        if (lookupServiceFactory == null) {
            return new DecisionBroadcastProcessFunction(SNAPSHOT_STATE_DESCRIPTOR,
                    cn.liboshuai.pulsix.engine.feature.InMemoryLookupService::demo,
                    stateStoreFactory);
        }
        if (stateStoreFactory == null) {
            return new DecisionBroadcastProcessFunction(SNAPSHOT_STATE_DESCRIPTOR, lookupServiceFactory);
        }
        return new DecisionBroadcastProcessFunction(SNAPSHOT_STATE_DESCRIPTOR, lookupServiceFactory, stateStoreFactory);
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

    private SceneSnapshotEnvelope rollbackEnvelope(SceneSnapshotEnvelope baseline,
                                                   Instant publishedAt,
                                                   Instant effectiveFrom) {
        SceneSnapshot snapshot = copy(baseline.getSnapshot(), SceneSnapshot.class);
        snapshot.setPublishedAt(publishedAt);
        snapshot.setEffectiveFrom(effectiveFrom);
        SceneSnapshotEnvelope envelope = envelopeOf(snapshot);
        envelope.setPublishType(PublishType.ROLLBACK);
        return envelope;
    }

    private SceneSnapshotEnvelope groovyDisabledEnvelope() {
        SceneSnapshot snapshot = DemoFixtures.demoSnapshot();
        snapshot.setVersion(14);
        snapshot.setChecksum("switch-checksum-v14-groovy-disabled");
        snapshot.setPublishedAt(Instant.parse("2026-03-07T20:08:00Z"));
        snapshot.setEffectiveFrom(snapshot.getPublishedAt());
        snapshot.getRuntimeHints().setAllowGroovy(false);
        return envelopeOf(snapshot);
    }

    private SceneSnapshotEnvelope minimalDecisionLogEnvelope() {
        SceneSnapshot snapshot = baselineEnvelopeForBroadcastSwitch().getSnapshot();
        snapshot.setVersion(16);
        snapshot.setChecksum("switch-checksum-v16-min-log");
        snapshot.setPublishedAt(Instant.parse("2026-03-07T20:12:00Z"));
        snapshot.setEffectiveFrom(snapshot.getPublishedAt());
        snapshot.getRuntimeHints().setNeedFullDecisionLog(false);
        return envelopeOf(snapshot);
    }

    private SceneSnapshotEnvelope brokenRuleExecutionEnvelope() {
        SceneSnapshot snapshot = DemoFixtures.demoSnapshot();
        snapshot.setSnapshotId("TRADE_RISK_BROKEN_RULE_v17");
        snapshot.setVersion(17);
        snapshot.setChecksum("switch-checksum-v17-broken-rule");
        snapshot.setPublishedAt(Instant.parse("2026-03-07T20:15:00Z"));
        snapshot.setEffectiveFrom(snapshot.getPublishedAt());
        snapshot.setStreamFeatures(List.of());
        snapshot.setLookupFeatures(List.of());
        snapshot.setDerivedFeatures(List.of());
        RuleSpec brokenRule = new RuleSpec();
        brokenRule.setCode("R-BROKEN");
        brokenRule.setName("broken rule");
        brokenRule.setEngineType(cn.liboshuai.pulsix.engine.model.EngineType.GROOVY);
        brokenRule.setPriority(100);
        brokenRule.setWhenExpr("return missing_context.toString() == 'x'");
        brokenRule.setHitAction(ActionType.REJECT);
        brokenRule.setRiskScore(99);
        brokenRule.setEnabled(true);
        snapshot.setRules(List.of(brokenRule));
        snapshot.getPolicy().setRuleOrder(List.of("R-BROKEN"));
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
