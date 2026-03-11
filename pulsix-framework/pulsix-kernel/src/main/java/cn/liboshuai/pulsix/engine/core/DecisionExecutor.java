package cn.liboshuai.pulsix.engine.core;

import cn.liboshuai.pulsix.engine.context.EvalContext;
import cn.liboshuai.pulsix.engine.feature.LookupService;
import cn.liboshuai.pulsix.engine.feature.StreamFeatureStateStore;
import cn.liboshuai.pulsix.engine.model.ActionType;
import cn.liboshuai.pulsix.engine.model.DecisionMode;
import cn.liboshuai.pulsix.engine.model.DecisionResult;
import cn.liboshuai.pulsix.engine.model.EventSchemaSpec;
import cn.liboshuai.pulsix.engine.model.PolicySpec;
import cn.liboshuai.pulsix.engine.model.RiskEvent;
import cn.liboshuai.pulsix.engine.model.RuleHit;
import cn.liboshuai.pulsix.engine.model.ScoreBandSpec;
import cn.liboshuai.pulsix.engine.runtime.CompiledSceneRuntime;
import cn.liboshuai.pulsix.engine.support.TemplateRenderer;
import cn.liboshuai.pulsix.engine.support.ValueConverter;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

public class DecisionExecutor {

    public DecisionResult execute(CompiledSceneRuntime runtime,
                                  RiskEvent event,
                                  StreamFeatureStateStore stateStore,
                                  LookupService lookupService) {
        long startedAt = System.nanoTime();
        validateEvent(runtime.getSnapshot().getEventSchema(), event);

        EvalContext context = new EvalContext();
        context.setSceneCode(runtime.sceneCode());
        context.setVersion(runtime.version());
        context.setEvent(event);
        context.getValues().putAll(event.toFlatMap());
        context.trace("load-event");

        for (CompiledSceneRuntime.CompiledStreamFeature feature : runtime.getStreamFeatures()) {
            Object value = executeStreamFeature(feature, context, stateStore);
            context.put(feature.getSpec().getCode(), value);
            context.trace("stream:" + feature.getSpec().getCode() + '=' + value);
        }
        for (CompiledSceneRuntime.CompiledLookupFeature feature : runtime.getLookupFeatures()) {
            Object value = executeLookupFeature(feature, context, lookupService);
            context.put(feature.getSpec().getCode(), value);
            context.trace("lookup:" + feature.getSpec().getCode() + '=' + value);
        }
        for (CompiledSceneRuntime.CompiledDerivedFeature feature : runtime.getOrderedDerivedFeatures()) {
            Object value = feature.getExpression().execute(context);
            context.put(feature.getSpec().getCode(), value);
            context.trace("derived:" + feature.getSpec().getCode() + '=' + value);
        }
        for (CompiledSceneRuntime.CompiledRule rule : runtime.getOrderedRules()) {
            context.getRuleHits().add(executeRule(rule, context));
        }

        PolicyOutcome outcome = applyPolicy(runtime.getPolicy(), context.getRuleHits());
        DecisionResult result = new DecisionResult();
        result.setEventId(event.getEventId());
        result.setTraceId(event.getTraceId());
        result.setSceneCode(event.getSceneCode());
        result.setVersion(runtime.version());
        result.setDecisionMode(runtime.getPolicy() != null ? runtime.getPolicy().getDecisionMode() : DecisionMode.FIRST_HIT);
        result.setFinalAction(outcome.action());
        result.setFinalScore(outcome.score());
        result.setRuleHits(context.getRuleHits());
        result.setFeatureSnapshot(snapshotFeatures(runtime, context));
        result.setTraceLogs(context.getTraceLogs());
        result.setLatencyMs(Duration.ofNanos(System.nanoTime() - startedAt).toMillis());
        return result;
    }

    private void validateEvent(EventSchemaSpec schema, RiskEvent event) {
        if (event == null) {
            throw new IllegalArgumentException("event must not be null");
        }
        if (schema == null) {
            return;
        }
        if (schema.getEventType() != null && !Objects.equals(schema.getEventType(), event.getEventType())) {
            throw new IllegalArgumentException("eventType not match snapshot schema");
        }
        Map<String, Object> values = event.toFlatMap();
        for (String field : Optional.ofNullable(schema.getRequiredFields()).orElse(List.of())) {
            Object value = values.get(field);
            if (value == null || value instanceof String stringValue && stringValue.isBlank()) {
                throw new IllegalArgumentException("required field missing: " + field);
            }
        }
    }

    private Object executeStreamFeature(CompiledSceneRuntime.CompiledStreamFeature feature,
                                        EvalContext context,
                                        StreamFeatureStateStore stateStore) {
        return stateStore.evaluate(feature, context);
    }

    private Object executeLookupFeature(CompiledSceneRuntime.CompiledLookupFeature feature,
                                        EvalContext context,
                                        LookupService lookupService) {
        Object value = lookupService.lookup(feature, context);
        return value != null ? value : ValueConverter.coerce(feature.getSpec().getDefaultValue(), feature.getSpec().getValueType());
    }

    private RuleHit executeRule(CompiledSceneRuntime.CompiledRule rule, EvalContext context) {
        RuleHit hit = new RuleHit();
        hit.setRuleCode(rule.getSpec().getCode());
        hit.setRuleName(rule.getSpec().getName());
        hit.setPriority(rule.getSpec().getPriority());
        hit.setAction(rule.getSpec().getHitAction());
        hit.setScore(rule.getSpec().getRiskScore());
        if (Boolean.FALSE.equals(rule.getSpec().getEnabled())) {
            hit.setHit(false);
            return hit;
        }
        boolean matched = ValueConverter.asBoolean(rule.getCondition().execute(context));
        hit.setHit(matched);
        if (matched) {
            hit.setReason(TemplateRenderer.render(rule.getSpec().getHitReasonTemplate(), context.getValues()));
            hit.getDetail().put("engineType", String.valueOf(rule.getSpec().getEngineType()));
            hit.getDetail().put("expr", rule.getCondition().rawExpression());
        }
        context.trace("rule:" + rule.getSpec().getCode() + '=' + matched);
        return hit;
    }

    private PolicyOutcome applyPolicy(PolicySpec policy, List<RuleHit> ruleHits) {
        PolicySpec safePolicy = policy == null ? new PolicySpec() : policy;
        if (safePolicy.getDecisionMode() == DecisionMode.SCORE_CARD) {
            int totalScore = ruleHits.stream()
                    .filter(hit -> Boolean.TRUE.equals(hit.getHit()))
                    .map(RuleHit::getScore)
                    .filter(Objects::nonNull)
                    .mapToInt(Integer::intValue)
                    .sum();
            for (ScoreBandSpec scoreBand : Optional.ofNullable(safePolicy.getScoreBands()).orElse(List.of())) {
                int minScore = scoreBand.getMinScore() == null ? Integer.MIN_VALUE : scoreBand.getMinScore();
                int maxScore = scoreBand.getMaxScore() == null ? Integer.MAX_VALUE : scoreBand.getMaxScore();
                if (totalScore >= minScore && totalScore <= maxScore) {
                    return new PolicyOutcome(scoreBand.getAction(), totalScore);
                }
            }
            return new PolicyOutcome(defaultAction(safePolicy), totalScore);
        }
        Map<String, RuleHit> hitMap = ruleHits.stream().collect(Collectors.toMap(RuleHit::getRuleCode, item -> item, (left, right) -> left));
        for (String code : Optional.ofNullable(safePolicy.getRuleOrder()).orElse(List.of())) {
            RuleHit hit = hitMap.get(code);
            if (hit != null && Boolean.TRUE.equals(hit.getHit())) {
                return new PolicyOutcome(hit.getAction(), Optional.ofNullable(hit.getScore()).orElse(0));
            }
        }
        RuleHit firstHit = ruleHits.stream()
                .filter(item -> Boolean.TRUE.equals(item.getHit()))
                .findFirst()
                .orElse(null);
        if (firstHit != null) {
            return new PolicyOutcome(firstHit.getAction(), Optional.ofNullable(firstHit.getScore()).orElse(0));
        }
        return new PolicyOutcome(defaultAction(safePolicy), 0);
    }

    private ActionType defaultAction(PolicySpec policy) {
        return policy.getDefaultAction() == null ? ActionType.PASS : policy.getDefaultAction();
    }

    private Map<String, Object> snapshotFeatures(CompiledSceneRuntime runtime, EvalContext context) {
        Map<String, Object> featureSnapshot = new LinkedHashMap<>();
        for (String featureCode : runtime.featureCodes()) {
            featureSnapshot.put(featureCode, context.get(featureCode));
        }
        return featureSnapshot;
    }

    private record PolicyOutcome(ActionType action, int score) {
    }

}
