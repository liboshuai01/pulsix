package cn.liboshuai.pulsix.engine.core;

import cn.liboshuai.pulsix.engine.context.EvalContext;
import cn.liboshuai.pulsix.engine.demo.DemoFixtures;
import cn.liboshuai.pulsix.engine.feature.InMemoryLookupService;
import cn.liboshuai.pulsix.engine.feature.InMemoryStreamFeatureStateStore;
import cn.liboshuai.pulsix.engine.json.EngineJson;
import cn.liboshuai.pulsix.engine.model.DecisionResult;
import cn.liboshuai.pulsix.engine.model.RiskEvent;
import cn.liboshuai.pulsix.engine.runtime.CompiledSceneRuntime;
import cn.liboshuai.pulsix.engine.runtime.RuntimeCompiler;
import cn.liboshuai.pulsix.engine.script.DefaultScriptCompiler;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DecisionExecutorTest {

    @Test
    void shouldExecutePreparedDecisionSameAsInlineExecution() {
        CompiledSceneRuntime runtime = compileDemoRuntime();
        DecisionExecutor executor = new DecisionExecutor();
        InMemoryStreamFeatureStateStore baselineStateStore = new InMemoryStreamFeatureStateStore();
        InMemoryStreamFeatureStateStore preparedStateStore = new InMemoryStreamFeatureStateStore();

        for (RiskEvent sourceEvent : DemoFixtures.demoEvents()) {
            RiskEvent baselineEvent = copy(sourceEvent, RiskEvent.class);
            RiskEvent preparedEvent = copy(sourceEvent, RiskEvent.class);

            DecisionResult baseline = executor.execute(runtime,
                    baselineEvent,
                    baselineStateStore,
                    InMemoryLookupService.demo());
            DecisionExecutor.PreparedDecisionContext preparedContext = executor.prepare(runtime,
                    preparedEvent,
                    preparedStateStore);
            DecisionResult prepared = executor.executePrepared(runtime,
                    preparedContext,
                    InMemoryLookupService.demo(),
                    record -> {
                    });

            assertEquals(baseline.getFinalAction(), prepared.getFinalAction());
            assertEquals(baseline.getFinalScore(), prepared.getFinalScore());
            assertEquals(baseline.getTotalScore(), prepared.getTotalScore());
            assertEquals(baseline.getReason(), prepared.getReason());
            assertEquals(baseline.getMatchedScoreBand(), prepared.getMatchedScoreBand());
            assertEquals(baseline.getScoreContributions(), prepared.getScoreContributions());
            assertEquals(baseline.getFeatureSnapshot(), prepared.getFeatureSnapshot());
            assertEquals(baseline.getRuleHits(), prepared.getRuleHits());
        }
    }

    @Test
    void shouldRejectPreparedDecisionWhenStreamFeatureCoverageIncomplete() {
        CompiledSceneRuntime runtime = compileDemoRuntime();
        RiskEvent event = copy(DemoFixtures.demoEvents().get(0), RiskEvent.class);
        EvalContext context = new EvalContext();
        context.setSceneCode(runtime.sceneCode());
        context.setVersion(runtime.version());
        context.setEvent(event);
        context.getValues().putAll(event.toFlatMap());
        context.put("user_trade_cnt_5m", 1L);
        DecisionExecutor.PreparedDecisionContext preparedContext = new DecisionExecutor.PreparedDecisionContext(
                event,
                context,
                System.nanoTime());

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> new DecisionExecutor().executePrepared(runtime,
                        preparedContext,
                        InMemoryLookupService.demo(),
                        record -> {
                        }));

        assertTrue(exception.getMessage().contains("coverage mismatch"));
    }

    private CompiledSceneRuntime compileDemoRuntime() {
        return new RuntimeCompiler(new DefaultScriptCompiler()).compile(DemoFixtures.demoSnapshot());
    }

    private <T> T copy(Object value, Class<T> type) {
        return EngineJson.read(EngineJson.write(value), type);
    }

}
