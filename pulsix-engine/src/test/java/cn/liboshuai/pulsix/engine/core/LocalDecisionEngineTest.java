package cn.liboshuai.pulsix.engine.core;

import cn.liboshuai.pulsix.engine.demo.DemoFixtures;
import cn.liboshuai.pulsix.engine.feature.InMemoryLookupService;
import cn.liboshuai.pulsix.engine.feature.InMemoryStreamFeatureStateStore;
import cn.liboshuai.pulsix.engine.model.ActionType;
import cn.liboshuai.pulsix.engine.model.DecisionResult;
import cn.liboshuai.pulsix.engine.model.RiskEvent;
import cn.liboshuai.pulsix.engine.runtime.RuntimeCompiler;
import cn.liboshuai.pulsix.engine.runtime.SceneRuntimeManager;
import cn.liboshuai.pulsix.engine.script.DefaultScriptCompiler;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
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

    private LocalDecisionEngine newEngine() {
        SceneRuntimeManager runtimeManager = new SceneRuntimeManager(new RuntimeCompiler(new DefaultScriptCompiler()));
        return new LocalDecisionEngine(runtimeManager,
                new InMemoryStreamFeatureStateStore(),
                InMemoryLookupService.demo(),
                new DecisionExecutor());
    }

}
