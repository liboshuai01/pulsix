package cn.liboshuai.pulsix.engine.runtime;

import cn.liboshuai.pulsix.engine.demo.DemoFixtures;
import cn.liboshuai.pulsix.engine.model.EngineErrorCodes;
import cn.liboshuai.pulsix.engine.model.PolicyRuleRefSpec;
import cn.liboshuai.pulsix.engine.model.RuleSpec;
import cn.liboshuai.pulsix.engine.model.StreamFeatureSpec;
import cn.liboshuai.pulsix.engine.model.SceneSnapshot;
import cn.liboshuai.pulsix.engine.script.DefaultScriptCompiler;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RuntimeCompilerTest {

    @Test
    void shouldRejectGroovyScriptWhenRuntimeHintsDisallowGroovy() {
        SceneSnapshot snapshot = DemoFixtures.demoSnapshot();
        snapshot.getRuntimeHints().setAllowGroovy(false);

        RuntimeCompileException exception = assertThrows(RuntimeCompileException.class,
                () -> new RuntimeCompiler(new DefaultScriptCompiler()).compile(snapshot));

        assertEquals(EngineErrorCodes.GROOVY_DISABLED, exception.getErrorCode());
        assertEquals("R003", exception.getRuleCode());
        assertTrue(exception.getMessage().contains("allowGroovy"));
    }

    @Test
    void shouldBuildStreamFeatureRoutingPlanByEntityDimension() {
        SceneSnapshot snapshot = DemoFixtures.demoSnapshot();

        CompiledSceneRuntime runtime = new RuntimeCompiler(new DefaultScriptCompiler()).compile(snapshot);

        CompiledSceneRuntime.StreamFeatureRoutingPlan routingPlan = runtime.getStreamFeatureRoutingPlan();
        assertEquals("TRADE_RISK", routingPlan.getSceneCode());
        assertTrue(runtime.hasMixedStreamFeatureRouting());
        assertEquals(2, routingPlan.getGroups().size());

        CompiledSceneRuntime.StreamFeatureGroupPlan userGroup = routingPlan.getGroups().get(0);
        assertEquals("USER", userGroup.getEntityType());
        assertEquals("userId", userGroup.getEntityKeyExpr());
        assertEquals(List.of("trade"), userGroup.getSourceEventTypes());
        assertEquals(List.of("user_trade_cnt_5m", "user_trade_amt_sum_30m"), userGroup.getFeatureCodes());

        CompiledSceneRuntime.StreamFeatureGroupPlan deviceGroup = routingPlan.getGroups().get(1);
        assertEquals("DEVICE", deviceGroup.getEntityType());
        assertEquals("deviceId", deviceGroup.getEntityKeyExpr());
        assertEquals(List.of("trade"), deviceGroup.getSourceEventTypes());
        assertEquals(List.of("device_bind_user_cnt_1h"), deviceGroup.getFeatureCodes());
    }

    @Test
    void shouldSplitRoutingGroupsWhenSourceEventTypesDiffer() {
        SceneSnapshot snapshot = DemoFixtures.demoSnapshot();
        StreamFeatureSpec secondUserFeature = snapshot.getStreamFeatures().get(1);
        secondUserFeature.setSourceEventTypes(List.of("refund"));
        snapshot.setStreamFeatures(List.of(snapshot.getStreamFeatures().get(0), secondUserFeature));

        CompiledSceneRuntime runtime = new RuntimeCompiler(new DefaultScriptCompiler()).compile(snapshot);

        CompiledSceneRuntime.StreamFeatureRoutingPlan routingPlan = runtime.getStreamFeatureRoutingPlan();
        assertEquals(2, routingPlan.getGroups().size());
        assertEquals(List.of("user_trade_cnt_5m"), routingPlan.getGroups().get(0).getFeatureCodes());
        assertEquals(List.of("trade"), routingPlan.getGroups().get(0).getSourceEventTypes());
        assertEquals(List.of("user_trade_amt_sum_30m"), routingPlan.getGroups().get(1).getFeatureCodes());
        assertEquals(List.of("refund"), routingPlan.getGroups().get(1).getSourceEventTypes());
    }

    @Test
    void shouldOrderRulesByPolicyRuleRefsWhenPresent() {
        SceneSnapshot snapshot = DemoFixtures.scoreCardSnapshot();
        PolicyRuleRefSpec secondRuleFirst = new PolicyRuleRefSpec();
        secondRuleFirst.setRuleCode("SR002");
        secondRuleFirst.setOrderNo(5);
        PolicyRuleRefSpec firstRuleSecond = new PolicyRuleRefSpec();
        firstRuleSecond.setRuleCode("SR001");
        firstRuleSecond.setOrderNo(10);
        snapshot.getPolicy().setRuleRefs(List.of(secondRuleFirst, firstRuleSecond));

        CompiledSceneRuntime runtime = new RuntimeCompiler(new DefaultScriptCompiler()).compile(snapshot);

        assertEquals(List.of("SR002", "SR001"), runtime.getOrderedRules().stream()
                .map(item -> item.getSpec().getCode())
                .toList());
    }

    @Test
    void shouldRejectDangerousGroovyRuleInSandboxAtCompileTime() {
        SceneSnapshot snapshot = DemoFixtures.demoSnapshot();
        RuleSpec rule = snapshot.getRules().stream()
                .filter(item -> "R003".equals(item.getCode()))
                .findFirst()
                .orElseThrow();
        rule.setWhenExpr("return new File('/tmp/pulsix-danger').text");

        RuntimeCompileException exception = assertThrows(RuntimeCompileException.class,
                () -> new RuntimeCompiler(new DefaultScriptCompiler()).compile(snapshot));

        assertEquals(EngineErrorCodes.GROOVY_SANDBOX_REJECTED, exception.getErrorCode());
        assertEquals("R003", exception.getRuleCode());
        assertTrue(exception.getMessage().contains("sandbox"));
    }

}
