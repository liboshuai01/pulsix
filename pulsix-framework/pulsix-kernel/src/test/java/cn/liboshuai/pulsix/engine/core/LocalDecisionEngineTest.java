package cn.liboshuai.pulsix.engine.core;

import cn.liboshuai.pulsix.engine.demo.DemoFixtures;
import cn.liboshuai.pulsix.engine.feature.InMemoryLookupService;
import cn.liboshuai.pulsix.engine.feature.InMemoryStreamFeatureStateStore;
import cn.liboshuai.pulsix.engine.json.EngineJson;
import cn.liboshuai.pulsix.engine.model.ActionType;
import cn.liboshuai.pulsix.engine.model.DecisionResult;
import cn.liboshuai.pulsix.engine.model.RiskEvent;
import cn.liboshuai.pulsix.engine.model.SceneSnapshot;
import cn.liboshuai.pulsix.engine.runtime.RuntimeCompiler;
import cn.liboshuai.pulsix.engine.runtime.SceneRuntimeManager;
import cn.liboshuai.pulsix.engine.script.DefaultScriptCompiler;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LocalDecisionEngineTest {

    @Test
    void shouldRejectByFirstHitPolicyAfterFeaturesPrepared() {
        LocalDecisionEngine engine = newEngine();
        engine.publish(DemoFixtures.demoSnapshot());

        List<RiskEvent> events = DemoFixtures.demoEvents();
        DecisionResult result = null;
        for (RiskEvent event : events) {
            result = engine.evaluate(event);
        }

        assertEquals(ActionType.REJECT, result.getFinalAction());
        assertEquals(80, result.getFinalScore());
        assertEquals("3", result.getFeatureSnapshot().get("user_trade_cnt_5m"));
        assertEquals("7180", result.getFeatureSnapshot().get("user_trade_amt_sum_30m"));
        assertEquals("4", result.getFeatureSnapshot().get("device_bind_user_cnt_1h"));
        assertTrue(result.getRuleHits().stream().anyMatch(hit -> "R002".equals(hit.getRuleCode()) && Boolean.TRUE.equals(hit.getHit())));
        assertTrue(result.getRuleHits().stream().anyMatch(hit -> "R003".equals(hit.getRuleCode()) && Boolean.TRUE.equals(hit.getHit())));
    }

    @Test
    void shouldRejectImmediatelyWhenDeviceInBlacklist() {
        LocalDecisionEngine engine = newEngine();
        engine.publish(DemoFixtures.demoSnapshot());

        DecisionResult result = engine.evaluate(DemoFixtures.blacklistedEvent());

        assertEquals(ActionType.REJECT, result.getFinalAction());
        assertEquals(100, result.getFinalScore());
        assertEquals("true", result.getFeatureSnapshot().get("device_in_blacklist"));
    }

    @Test
    void shouldStopEvaluatingRulesWhenMaxRuleExecutionCountReached() {
        LocalDecisionEngine engine = newEngine();
        SceneSnapshot snapshot = DemoFixtures.demoSnapshot();
        snapshot.getRuntimeHints().setMaxRuleExecutionCount(1);
        engine.publish(snapshot);

        DecisionResult result = null;
        for (RiskEvent event : DemoFixtures.demoEvents()) {
            result = engine.evaluate(event);
        }

        assertEquals(ActionType.PASS, result.getFinalAction());
        assertEquals(1, result.getRuleHits().size());
        assertEquals("R001", result.getRuleHits().get(0).getRuleCode());
        assertEquals(Boolean.FALSE, result.getRuleHits().get(0).getHit());
    }

    @Test
    void shouldIsolateStreamFeatureStateAcrossScenes() {
        LocalDecisionEngine engine = newEngine();
        engine.publish(sceneSnapshot("TRADE_RISK_A", "TRADE_RISK_A_v12", 12, "scene-a-checksum-v12"));
        engine.publish(sceneSnapshot("TRADE_RISK_B", "TRADE_RISK_B_v12", 12, "scene-b-checksum-v12"));

        DecisionResult sceneAResult = engine.evaluate(tradeEvent(
                "TRADE_RISK_A",
                "E202603071101",
                "T202603071101",
                Instant.parse("2026-03-07T10:00:00Z"),
                "U1001",
                "D9101",
                120));
        DecisionResult sceneBResult = engine.evaluate(tradeEvent(
                "TRADE_RISK_B",
                "E202603071102",
                "T202603071102",
                Instant.parse("2026-03-07T10:01:00Z"),
                "U1001",
                "D9102",
                120));

        assertEquals(ActionType.PASS, sceneAResult.getFinalAction());
        assertEquals(ActionType.PASS, sceneBResult.getFinalAction());
        assertEquals("1", sceneAResult.getFeatureSnapshot().get("user_trade_cnt_5m"));
        assertEquals("1", sceneBResult.getFeatureSnapshot().get("user_trade_cnt_5m"));
    }

    @Test
    void shouldApplyScoreCardPolicyWithWeightedContributionAndStopOnHit() {
        LocalDecisionEngine engine = newEngine();
        engine.publish(DemoFixtures.scoreCardSnapshot());

        DecisionResult result = engine.evaluate(copy(DemoFixtures.scoreCardHitEvent(), RiskEvent.class));

        assertEquals(ActionType.REJECT, result.getFinalAction());
        assertEquals(100, result.getFinalScore());
        assertEquals(100, result.getTotalScore());
        assertEquals("评分卡总分=100, 命中规则数=2, 落入REJECT段", result.getReason());
        assertEquals("BAND_REJECT", result.getMatchedScoreBand().getCode());
        assertEquals(List.of("SR001", "SR002"), result.getRuleHits().stream()
                .filter(hit -> Boolean.TRUE.equals(hit.getHit()))
                .map(hit -> hit.getRuleCode())
                .toList());
        assertEquals(2, result.getScoreContributions().size());
        assertEquals(80, result.getScoreContributions().get(0).getWeightedScore());
        assertEquals(20, result.getScoreContributions().get(1).getWeightedScore());
    }

    @Test
    void shouldUseDefaultActionWhenScoreCardBandMissing() {
        LocalDecisionEngine engine = newEngine();
        SceneSnapshot snapshot = copy(DemoFixtures.scoreCardSnapshot(), SceneSnapshot.class);
        snapshot.getPolicy().setScoreBands(List.of());
        engine.publish(snapshot);

        DecisionResult result = engine.evaluate(DemoFixtures.scoreCardPassEvent());

        assertEquals(ActionType.PASS, result.getFinalAction());
        assertEquals(0, result.getFinalScore());
        assertEquals(0, result.getTotalScore());
        assertTrue(result.getScoreContributions().isEmpty());
        assertEquals(null, result.getMatchedScoreBand());
        assertEquals(null, result.getReason());
    }

    @Test
    void shouldRejectEventTypeOutsideSceneAllowedEventTypes() {
        LocalDecisionEngine engine = newEngine();
        engine.publish(DemoFixtures.demoSnapshot());

        RiskEvent event = DemoFixtures.blacklistedEvent();
        event.setEventType("login");

        DecisionExecutionException exception = assertThrows(DecisionExecutionException.class,
                () -> engine.evaluate(event));

        assertEquals("eventType not allowed by scene", exception.getMessage());
    }

    private SceneSnapshot sceneSnapshot(String sceneCode, String snapshotId, int version, String checksum) {
        SceneSnapshot snapshot = copy(DemoFixtures.demoSnapshot(), SceneSnapshot.class);
        snapshot.setSceneCode(sceneCode);
        snapshot.setSceneName(sceneCode);
        snapshot.setSnapshotId(snapshotId);
        snapshot.setVersion(version);
        snapshot.setChecksum(checksum);
        return snapshot;
    }

    private RiskEvent tradeEvent(String sceneCode,
                                 String eventId,
                                 String traceId,
                                 Instant eventTime,
                                 String userId,
                                 String deviceId,
                                 long amount) {
        RiskEvent event = copy(DemoFixtures.demoEvents().get(0), RiskEvent.class);
        event.setSceneCode(sceneCode);
        event.setEventId(eventId);
        event.setTraceId(traceId);
        event.setEventTime(eventTime);
        event.setUserId(userId);
        event.setDeviceId(deviceId);
        event.setAmount(BigDecimal.valueOf(amount));
        event.setResult("SUCCESS");
        event.setEventType("trade");
        return event;
    }

    private <T> T copy(Object value, Class<T> type) {
        return EngineJson.read(EngineJson.write(value), type);
    }

    private LocalDecisionEngine newEngine() {
        SceneRuntimeManager runtimeManager = new SceneRuntimeManager(new RuntimeCompiler(new DefaultScriptCompiler()));
        return new LocalDecisionEngine(runtimeManager,
                new InMemoryStreamFeatureStateStore(),
                InMemoryLookupService.demo(),
                new DecisionExecutor());
    }

}
