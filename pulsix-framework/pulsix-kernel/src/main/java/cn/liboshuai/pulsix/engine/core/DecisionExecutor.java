package cn.liboshuai.pulsix.engine.core;

import cn.liboshuai.pulsix.engine.context.EvalContext;
import cn.liboshuai.pulsix.engine.feature.LookupResult;
import cn.liboshuai.pulsix.engine.feature.LookupService;
import cn.liboshuai.pulsix.engine.feature.StreamFeatureStateStore;
import cn.liboshuai.pulsix.engine.model.ActionType;
import cn.liboshuai.pulsix.engine.model.DecisionMode;
import cn.liboshuai.pulsix.engine.model.DecisionResult;
import cn.liboshuai.pulsix.engine.model.EngineErrorRecord;
import cn.liboshuai.pulsix.engine.model.MatchedScoreBand;
import cn.liboshuai.pulsix.engine.model.EngineErrorTypes;
import cn.liboshuai.pulsix.engine.model.EventSchemaSpec;
import cn.liboshuai.pulsix.engine.model.PolicyRuleRefSpec;
import cn.liboshuai.pulsix.engine.model.PolicySpec;
import cn.liboshuai.pulsix.engine.model.RiskEvent;
import cn.liboshuai.pulsix.engine.model.RuleHit;
import cn.liboshuai.pulsix.engine.model.SceneSnapshot;
import cn.liboshuai.pulsix.engine.model.ScoreContribution;
import cn.liboshuai.pulsix.engine.model.SceneSpec;
import cn.liboshuai.pulsix.engine.model.ScoreBandSpec;
import cn.liboshuai.pulsix.engine.runtime.CompiledSceneRuntime;
import cn.liboshuai.pulsix.engine.support.TemplateRenderer;
import cn.liboshuai.pulsix.engine.support.ValueConverter;

import java.time.Duration;
import java.util.ArrayList;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class DecisionExecutor {

    public PreparedDecisionContext prepare(CompiledSceneRuntime runtime,
                                           RiskEvent event,
                                           StreamFeatureStateStore stateStore) {
        long startedAt = System.nanoTime();
        EvalContext context = initializeContext(runtime, event);
        prepareStreamFeatures(runtime, context, stateStore);
        return new PreparedDecisionContext(event, context, startedAt);
    }

    public Map<String, String> prepareStreamFeatureSnapshot(CompiledSceneRuntime runtime,
                                                            RiskEvent event,
                                                            StreamFeatureStateStore stateStore,
                                                            List<String> featureCodes) {
        EvalContext context = initializeContext(runtime, event);
        prepareStreamFeatures(runtime, context, stateStore, orderedFeatureCodes(runtime, featureCodes));
        return snapshotPreparedStreamFeatures(runtime, context, orderedFeatureCodes(runtime, featureCodes));
    }

    public PreparedDecisionContext restorePreparedContext(CompiledSceneRuntime runtime,
                                                          RiskEvent event,
                                                          Map<String, String> preparedFeatureSnapshot,
                                                          long startedAtNanos) {
        EvalContext context = initializeContext(runtime, event);
        LinkedHashSet<String> expectedFeatureCodes = expectedStreamFeatureCodes(runtime);
        for (CompiledSceneRuntime.CompiledStreamFeature feature : Optional.ofNullable(runtime.getStreamFeatures()).orElse(List.of())) {
            if (feature == null || feature.getSpec() == null || feature.getSpec().getCode() == null) {
                continue;
            }
            String featureCode = feature.getSpec().getCode();
            if (!expectedFeatureCodes.contains(featureCode)) {
                continue;
            }
            if (preparedFeatureSnapshot == null || !preparedFeatureSnapshot.containsKey(featureCode)) {
                continue;
            }
            Object value = ValueConverter.coerce(preparedFeatureSnapshot.get(featureCode), feature.getSpec().getValueType());
            context.put(featureCode, value);
            context.trace("stream:" + featureCode + '=' + value);
        }
        return new PreparedDecisionContext(event, context, startedAtNanos);
    }

    public DecisionResult execute(CompiledSceneRuntime runtime,
                                  RiskEvent event,
                                  StreamFeatureStateStore stateStore,
                                  LookupService lookupService) {
        return execute(runtime, event, stateStore, lookupService, record -> {
        });
    }

    public DecisionResult execute(CompiledSceneRuntime runtime,
                                  RiskEvent event,
                                  StreamFeatureStateStore stateStore,
                                  LookupService lookupService,
                                  Consumer<EngineErrorRecord> errorCollector) {
        PreparedDecisionContext preparedContext = prepare(runtime, event, stateStore);
        return executePrepared(runtime, preparedContext, lookupService, errorCollector);
    }

    public DecisionResult executePrepared(CompiledSceneRuntime runtime,
                                          PreparedDecisionContext preparedContext,
                                          LookupService lookupService) {
        return executePrepared(runtime, preparedContext, lookupService, record -> {
        });
    }

    public DecisionResult executePrepared(CompiledSceneRuntime runtime,
                                          PreparedDecisionContext preparedContext,
                                          LookupService lookupService,
                                          Consumer<EngineErrorRecord> errorCollector) {
        PreparedDecisionContext safePreparedContext = requirePreparedContext(preparedContext);
        RiskEvent event = safePreparedContext.event();
        EvalContext context = safePreparedContext.evalContext();
        Consumer<EngineErrorRecord> safeErrorCollector = errorCollector == null ? record -> {
        } : errorCollector;

        validatePreparedStreamFeatureCoverage(runtime, context);

        for (CompiledSceneRuntime.CompiledLookupFeature feature : runtime.getLookupFeatures()) {
            Object value = executeLookupFeature(runtime, event, feature, context, lookupService, safeErrorCollector);
            context.put(feature.getSpec().getCode(), value);
            context.trace("lookup:" + feature.getSpec().getCode() + '=' + value);
        }
        for (CompiledSceneRuntime.CompiledDerivedFeature feature : runtime.getOrderedDerivedFeatures()) {
            Object value;
            try {
                value = feature.getExpression().execute(context);
            } catch (RuntimeException exception) {
                throw DecisionExecutionException.derivedFeature(feature == null || feature.getSpec() == null
                                ? null
                                : feature.getSpec().getCode(),
                        feature == null || feature.getSpec() == null ? null : feature.getSpec().getEngineType(),
                        exception);
            }
            context.put(feature.getSpec().getCode(), value);
            context.trace("derived:" + feature.getSpec().getCode() + '=' + value);
        }
        Map<String, PolicyRuleRefSpec> policyRuleRefs = policyRuleRefMap(runtime.getPolicy());
        int maxRuleExecutionCount = resolveMaxRuleExecutionCount(runtime);
        int executedRuleCount = 0;
        for (CompiledSceneRuntime.CompiledRule rule : runtime.getOrderedRules()) {
            if (executedRuleCount >= maxRuleExecutionCount) {
                context.trace("rule-limit:" + maxRuleExecutionCount);
                break;
            }
            RuleHit ruleHit;
            try {
                ruleHit = executeRule(rule, context);
                context.getRuleHits().add(ruleHit);
            } catch (RuntimeException exception) {
                throw DecisionExecutionException.ruleEvaluation(rule == null || rule.getSpec() == null
                                ? null
                                : rule.getSpec().getCode(),
                        rule == null || rule.getSpec() == null ? null : rule.getSpec().getEngineType(),
                        exception);
            }
            executedRuleCount++;
            if (shouldStopScoreCardEvaluation(runtime.getPolicy(), policyRuleRefs.get(ruleHit.getRuleCode()), ruleHit)) {
                context.trace("policy-stop-on-hit:" + ruleHit.getRuleCode());
                break;
            }
        }

        PolicyOutcome outcome = applyPolicy(runtime.getPolicy(), context.getRuleHits(), context);
        DecisionResult result = new DecisionResult();
        result.setEventId(event.getEventId());
        result.setTraceId(event.getTraceId());
        result.setSceneCode(event.getSceneCode());
        result.setVersion(runtime.version());
        result.setSnapshotId(runtime.getSnapshot() == null ? null : runtime.getSnapshot().getSnapshotId());
        result.setSnapshotChecksum(runtime.getSnapshot() == null ? null : runtime.getSnapshot().getChecksum());
        result.setDecisionMode(runtime.getPolicy() != null ? runtime.getPolicy().getDecisionMode() : DecisionMode.FIRST_HIT);
        result.setFinalAction(outcome.action());
        result.setFinalScore(outcome.finalScore());
        result.setTotalScore(outcome.totalScore());
        result.setReason(outcome.reason());
        result.setMatchedScoreBand(outcome.matchedScoreBand());
        result.setScoreContributions(outcome.scoreContributions());
        result.setRuleHits(context.getRuleHits());
        result.setFeatureSnapshot(snapshotFeatures(runtime, context));
        result.setTraceLogs(context.getTraceLogs());
        long safeStartedAt = safePreparedContext.startedAtNanos() > 0L ? safePreparedContext.startedAtNanos() : System.nanoTime();
        result.setLatencyMs(Duration.ofNanos(System.nanoTime() - safeStartedAt).toMillis());
        return result;
    }

    private PreparedDecisionContext requirePreparedContext(PreparedDecisionContext preparedContext) {
        if (preparedContext == null || preparedContext.event() == null || preparedContext.evalContext() == null) {
            throw new IllegalArgumentException("prepared decision context must not be null");
        }
        return preparedContext;
    }

    private EvalContext initializeContext(CompiledSceneRuntime runtime, RiskEvent event) {
        try {
            validateEvent(runtime.getSnapshot(), event);
        } catch (RuntimeException exception) {
            throw DecisionExecutionException.eventValidation(exception);
        }
        EvalContext context = new EvalContext();
        context.setSceneCode(runtime.sceneCode());
        context.setVersion(runtime.version());
        context.setEvent(event);
        context.getValues().putAll(event.toFlatMap());
        context.trace("load-event");
        return context;
    }

    private void prepareStreamFeatures(CompiledSceneRuntime runtime,
                                       EvalContext context,
                                       StreamFeatureStateStore stateStore) {
        prepareStreamFeatures(runtime, context, stateStore, orderedFeatureCodes(runtime, null));
    }

    private void prepareStreamFeatures(CompiledSceneRuntime runtime,
                                       EvalContext context,
                                       StreamFeatureStateStore stateStore,
                                       List<String> orderedFeatureCodes) {
        if (runtime == null || runtime.getStreamFeatures() == null || runtime.getStreamFeatures().isEmpty()) {
            return;
        }
        Map<String, CompiledSceneRuntime.CompiledStreamFeature> streamFeaturesByCode = runtime.getStreamFeatures().stream()
                .filter(Objects::nonNull)
                .filter(feature -> feature.getSpec() != null && feature.getSpec().getCode() != null)
                .collect(Collectors.toMap(feature -> feature.getSpec().getCode(),
                        feature -> feature,
                        (left, right) -> left,
                        LinkedHashMap::new));
        Set<String> preparedFeatureCodes = new LinkedHashSet<>();
        for (String featureCode : orderedFeatureCodes) {
            CompiledSceneRuntime.CompiledStreamFeature feature = streamFeaturesByCode.get(featureCode);
            if (feature == null) {
                throw new IllegalArgumentException("stream feature routing plan references missing feature: " + featureCode);
            }
            prepareSingleStreamFeature(feature, context, stateStore, preparedFeatureCodes);
        }
        validatePreparedStreamFeatureCoverage(runtime, preparedFeatureCodes, orderedFeatureCodes);
    }

    private void prepareSingleStreamFeature(CompiledSceneRuntime.CompiledStreamFeature feature,
                                            EvalContext context,
                                            StreamFeatureStateStore stateStore,
                                            Set<String> preparedFeatureCodes) {
        if (feature == null || feature.getSpec() == null || feature.getSpec().getCode() == null) {
            throw new IllegalArgumentException("compiled stream feature code must not be blank");
        }
        String featureCode = feature.getSpec().getCode();
        if (!preparedFeatureCodes.add(featureCode)) {
            throw new IllegalArgumentException("duplicate prepared stream feature: " + featureCode);
        }
        Object value;
        try {
            value = executeStreamFeature(feature, context, stateStore);
        } catch (RuntimeException exception) {
            throw DecisionExecutionException.stateAccess(featureCode, exception);
        }
        context.put(featureCode, value);
        context.trace("stream:" + featureCode + '=' + value);
    }

    private void validatePreparedStreamFeatureCoverage(CompiledSceneRuntime runtime,
                                                       Set<String> preparedFeatureCodes) {
        validatePreparedStreamFeatureCoverage(runtime, preparedFeatureCodes, orderedFeatureCodes(runtime, null));
    }

    private void validatePreparedStreamFeatureCoverage(CompiledSceneRuntime runtime,
                                                       Set<String> preparedFeatureCodes,
                                                       List<String> expectedOrderedFeatureCodes) {
        LinkedHashSet<String> expectedFeatureCodes = new LinkedHashSet<>(expectedOrderedFeatureCodes);
        if (!expectedFeatureCodes.equals(preparedFeatureCodes)) {
            throw new IllegalArgumentException("prepared stream feature coverage mismatch");
        }
    }

    private void validatePreparedStreamFeatureCoverage(CompiledSceneRuntime runtime,
                                                       EvalContext context) {
        LinkedHashSet<String> expectedFeatureCodes = expectedStreamFeatureCodes(runtime);
        LinkedHashSet<String> actualFeatureCodes = new LinkedHashSet<>();
        if (context != null && context.getValues() != null) {
            for (String featureCode : expectedFeatureCodes) {
                if (context.getValues().containsKey(featureCode)) {
                    actualFeatureCodes.add(featureCode);
                }
            }
        }
        if (!expectedFeatureCodes.equals(actualFeatureCodes)) {
            throw new IllegalArgumentException("prepared stream feature coverage mismatch");
        }
    }

    private LinkedHashSet<String> expectedStreamFeatureCodes(CompiledSceneRuntime runtime) {
        LinkedHashSet<String> expectedFeatureCodes = new LinkedHashSet<>();
        if (runtime == null || runtime.getStreamFeatures() == null) {
            return expectedFeatureCodes;
        }
        for (CompiledSceneRuntime.CompiledStreamFeature feature : runtime.getStreamFeatures()) {
            if (feature == null || feature.getSpec() == null || feature.getSpec().getCode() == null) {
                continue;
            }
            expectedFeatureCodes.add(feature.getSpec().getCode());
        }
        return expectedFeatureCodes;
    }

    private List<String> orderedFeatureCodes(CompiledSceneRuntime runtime, List<String> requestedFeatureCodes) {
        List<String> routingOrderedFeatureCodes = new java.util.ArrayList<>();
        if (requestedFeatureCodes != null && !requestedFeatureCodes.isEmpty()) {
            for (String featureCode : requestedFeatureCodes) {
                if (featureCode != null && !featureCode.isBlank()) {
                    routingOrderedFeatureCodes.add(featureCode);
                }
            }
            return routingOrderedFeatureCodes;
        }
        if (runtime == null || runtime.getStreamFeatures() == null || runtime.getStreamFeatures().isEmpty()) {
            return routingOrderedFeatureCodes;
        }
        CompiledSceneRuntime.StreamFeatureRoutingPlan routingPlan = runtime.getStreamFeatureRoutingPlan();
        if (routingPlan != null && routingPlan.getGroups() != null && !routingPlan.getGroups().isEmpty()) {
            for (CompiledSceneRuntime.StreamFeatureGroupPlan groupPlan : routingPlan.getGroups()) {
                if (groupPlan == null || groupPlan.getFeatureCodes() == null) {
                    continue;
                }
                for (String featureCode : groupPlan.getFeatureCodes()) {
                    if (featureCode != null && !featureCode.isBlank()) {
                        routingOrderedFeatureCodes.add(featureCode);
                    }
                }
            }
            return routingOrderedFeatureCodes;
        }
        for (CompiledSceneRuntime.CompiledStreamFeature feature : runtime.getStreamFeatures()) {
            if (feature == null || feature.getSpec() == null || feature.getSpec().getCode() == null) {
                continue;
            }
            routingOrderedFeatureCodes.add(feature.getSpec().getCode());
        }
        return routingOrderedFeatureCodes;
    }

    private Map<String, String> snapshotPreparedStreamFeatures(CompiledSceneRuntime runtime,
                                                               EvalContext context,
                                                               List<String> orderedFeatureCodes) {
        Map<String, String> featureSnapshot = new LinkedHashMap<>();
        LinkedHashSet<String> expectedFeatureCodes = new LinkedHashSet<>(orderedFeatureCodes);
        for (CompiledSceneRuntime.CompiledStreamFeature feature : Optional.ofNullable(runtime.getStreamFeatures()).orElse(List.of())) {
            if (feature == null || feature.getSpec() == null || feature.getSpec().getCode() == null) {
                continue;
            }
            String featureCode = feature.getSpec().getCode();
            if (!expectedFeatureCodes.contains(featureCode) || !context.getValues().containsKey(featureCode)) {
                continue;
            }
            featureSnapshot.put(featureCode, ValueConverter.asString(context.get(featureCode)));
        }
        return featureSnapshot;
    }

    private void validateEvent(SceneSnapshot snapshot, RiskEvent event) {
        if (event == null) {
            throw new IllegalArgumentException("event must not be null");
        }
        if (snapshot == null) {
            return;
        }
        validateAllowedEventTypes(snapshot.getScene(), event);
        EventSchemaSpec schema = snapshot.getEventSchema();
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

    private void validateAllowedEventTypes(SceneSpec scene, RiskEvent event) {
        if (scene == null || scene.getAllowedEventTypes() == null || scene.getAllowedEventTypes().isEmpty()) {
            return;
        }
        if (!scene.getAllowedEventTypes().contains(event.getEventType())) {
            throw new IllegalArgumentException("eventType not allowed by scene");
        }
    }

    private int resolveMaxRuleExecutionCount(CompiledSceneRuntime runtime) {
        Integer maxRuleExecutionCount = runtime == null ? null : runtime.maxRuleExecutionCount();
        if (maxRuleExecutionCount == null) {
            return Integer.MAX_VALUE;
        }
        if (maxRuleExecutionCount < 0) {
            return Integer.MAX_VALUE;
        }
        return maxRuleExecutionCount;
    }

    private Object executeStreamFeature(CompiledSceneRuntime.CompiledStreamFeature feature,
                                        EvalContext context,
                                        StreamFeatureStateStore stateStore) {
        return stateStore.evaluate(feature, context);
    }

    private Object executeLookupFeature(CompiledSceneRuntime runtime,
                                        RiskEvent event,
                                        CompiledSceneRuntime.CompiledLookupFeature feature,
                                        EvalContext context,
                                        LookupService lookupService,
                                        Consumer<EngineErrorRecord> errorCollector) {
        LookupResult lookupResult;
        try {
            lookupResult = lookupService.lookup(feature, context);
        } catch (RuntimeException exception) {
            LookupResult fallbackResult = LookupResult.fallback(
                    ValueConverter.coerce(feature.getSpec().getDefaultValue(), feature.getSpec().getValueType()),
                    safeLookupKey(feature, context),
                    LookupResult.ERROR_EXECUTION_FAILED,
                    exception.getMessage(),
                    LookupResult.FALLBACK_DEFAULT_VALUE);
            errorCollector.accept(lookupError(runtime, event, feature, fallbackResult));
            return fallbackResult.getValue();
        }
        if (lookupResult == null) {
            return ValueConverter.coerce(feature.getSpec().getDefaultValue(), feature.getSpec().getValueType());
        }
        if (lookupResult.hasError()) {
            errorCollector.accept(lookupError(runtime, event, feature, lookupResult));
        }
        return lookupResult.getValue();
    }

    private String safeLookupKey(CompiledSceneRuntime.CompiledLookupFeature feature,
                                 EvalContext context) {
        if (feature == null || feature.getKeyScript() == null) {
            return null;
        }
        try {
            return ValueConverter.asString(feature.getKeyScript().execute(context));
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    private EngineErrorRecord lookupError(CompiledSceneRuntime runtime,
                                          RiskEvent event,
                                          CompiledSceneRuntime.CompiledLookupFeature feature,
                                          LookupResult lookupResult) {
        EngineErrorRecord record = new EngineErrorRecord();
        record.setStage("decision-lookup");
        record.setErrorType(EngineErrorTypes.LOOKUP);
        record.setSceneCode(event != null ? event.getSceneCode() : runtime.sceneCode());
        record.setVersion(runtime.version());
        record.setSnapshotId(runtime.getSnapshot() == null ? null : runtime.getSnapshot().getSnapshotId());
        record.setSnapshotChecksum(runtime.getSnapshot() == null ? null : runtime.getSnapshot().getChecksum());
        record.setEventId(event != null ? event.getEventId() : null);
        record.setTraceId(event != null ? event.getTraceId() : null);
        record.setErrorCode(lookupResult.getErrorCode());
        record.setErrorMessage(lookupResult.getErrorMessage());
        record.setExceptionClass(null);
        record.setFeatureCode(feature != null && feature.getSpec() != null ? feature.getSpec().getCode() : null);
        record.setLookupType(feature != null && feature.getSpec() != null && feature.getSpec().getLookupType() != null
                ? feature.getSpec().getLookupType().name()
                : null);
        record.setSourceRef(feature != null && feature.getSpec() != null ? feature.getSpec().getSourceRef() : null);
        record.setLookupKey(lookupResult.getLookupKey());
        record.setFallbackMode(lookupResult.getFallbackMode());
        record.setOccurredAt(Instant.now());
        return record;
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

    private PolicyOutcome applyPolicy(PolicySpec policy, List<RuleHit> ruleHits, EvalContext context) {
        PolicySpec safePolicy = policy == null ? new PolicySpec() : policy;
        if (safePolicy.getDecisionMode() == DecisionMode.SCORE_CARD) {
            return applyScoreCardPolicy(safePolicy, ruleHits, context);
        }
        return applyFirstHitPolicy(safePolicy, ruleHits);
    }

    private PolicyOutcome applyScoreCardPolicy(PolicySpec policy, List<RuleHit> ruleHits, EvalContext context) {
        List<ScoreContribution> scoreContributions = new ArrayList<>();
        Map<String, PolicyRuleRefSpec> ruleRefs = policyRuleRefMap(policy);
        int totalScore = 0;
        for (RuleHit hit : Optional.ofNullable(ruleHits).orElse(List.of())) {
            if (hit == null || !Boolean.TRUE.equals(hit.getHit())) {
                continue;
            }
            PolicyRuleRefSpec ruleRef = ruleRefs.get(hit.getRuleCode());
            int rawScore = Optional.ofNullable(hit.getScore()).orElse(0);
            int scoreWeight = ruleRef != null && ruleRef.getScoreWeight() != null ? ruleRef.getScoreWeight() : 1;
            int weightedScore = rawScore * scoreWeight;
            totalScore += weightedScore;

            hit.getDetail().put("rawScore", String.valueOf(rawScore));
            hit.getDetail().put("scoreWeight", String.valueOf(scoreWeight));
            hit.getDetail().put("weightedScore", String.valueOf(weightedScore));
            if (ruleRef != null && ruleRef.getStopOnHit() != null) {
                hit.getDetail().put("stopOnHit", String.valueOf(ruleRef.getStopOnHit()));
            }

            ScoreContribution contribution = new ScoreContribution();
            contribution.setRuleCode(hit.getRuleCode());
            contribution.setRuleName(hit.getRuleName());
            contribution.setAction(hit.getAction());
            contribution.setRawScore(rawScore);
            contribution.setScoreWeight(scoreWeight);
            contribution.setWeightedScore(weightedScore);
            contribution.setStopOnHit(ruleRef == null ? null : ruleRef.getStopOnHit());
            contribution.setReason(hit.getReason());
            scoreContributions.add(contribution);
        }

        ScoreBandSpec matchedBandSpec = findMatchedScoreBand(policy, totalScore);
        MatchedScoreBand matchedScoreBand = matchedBandSpec == null ? null : toMatchedScoreBand(matchedBandSpec, totalScore, scoreContributions.size(), context);
        ActionType finalAction = matchedScoreBand != null && matchedScoreBand.getAction() != null
                ? matchedScoreBand.getAction()
                : defaultAction(policy);
        String reason = matchedScoreBand == null ? null : blankToNull(matchedScoreBand.getReason());
        context.trace("policy:score-card-total=" + totalScore);
        if (matchedScoreBand != null && matchedScoreBand.getCode() != null) {
            context.trace("policy:score-card-band=" + matchedScoreBand.getCode());
        }
        return new PolicyOutcome(finalAction, totalScore, totalScore, reason, matchedScoreBand, scoreContributions);
    }

    private PolicyOutcome applyFirstHitPolicy(PolicySpec policy, List<RuleHit> ruleHits) {
        Map<String, RuleHit> hitMap = Optional.ofNullable(ruleHits)
                .orElse(List.of())
                .stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(RuleHit::getRuleCode, item -> item, (left, right) -> left));
        for (String code : Optional.ofNullable(policy.getRuleOrder()).orElse(List.of())) {
            RuleHit hit = hitMap.get(code);
            if (hit != null && Boolean.TRUE.equals(hit.getHit())) {
                return new PolicyOutcome(hit.getAction(), Optional.ofNullable(hit.getScore()).orElse(0), null, blankToNull(hit.getReason()), null, List.of());
            }
        }
        RuleHit firstHit = Optional.ofNullable(ruleHits)
                .orElse(List.of())
                .stream()
                .filter(item -> item != null && Boolean.TRUE.equals(item.getHit()))
                .findFirst()
                .orElse(null);
        if (firstHit != null) {
            return new PolicyOutcome(firstHit.getAction(), Optional.ofNullable(firstHit.getScore()).orElse(0), null, blankToNull(firstHit.getReason()), null, List.of());
        }
        return new PolicyOutcome(defaultAction(policy), 0, null, null, null, List.of());
    }

    private ScoreBandSpec findMatchedScoreBand(PolicySpec policy, int totalScore) {
        for (ScoreBandSpec scoreBand : Optional.ofNullable(policy.getScoreBands()).orElse(List.of())) {
            if (scoreBand == null) {
                continue;
            }
            int minScore = scoreBand.getMinScore() == null ? Integer.MIN_VALUE : scoreBand.getMinScore();
            int maxScore = scoreBand.getMaxScore() == null ? Integer.MAX_VALUE : scoreBand.getMaxScore();
            if (totalScore >= minScore && totalScore <= maxScore) {
                return scoreBand;
            }
        }
        return null;
    }

    private MatchedScoreBand toMatchedScoreBand(ScoreBandSpec scoreBand,
                                                int totalScore,
                                                int hitRuleCount,
                                                EvalContext context) {
        MatchedScoreBand matchedScoreBand = new MatchedScoreBand();
        matchedScoreBand.setCode(scoreBand.getCode());
        matchedScoreBand.setMinScore(scoreBand.getMinScore());
        matchedScoreBand.setMaxScore(scoreBand.getMaxScore());
        matchedScoreBand.setAction(scoreBand.getAction());
        matchedScoreBand.setReason(blankToNull(TemplateRenderer.render(scoreBand.resolvedReasonTemplate(),
                scoreBandTemplateValues(context, scoreBand, totalScore, hitRuleCount))));
        return matchedScoreBand;
    }

    private Map<String, Object> scoreBandTemplateValues(EvalContext context,
                                                        ScoreBandSpec scoreBand,
                                                        int totalScore,
                                                        int hitRuleCount) {
        Map<String, Object> values = new LinkedHashMap<>();
        if (context != null && context.getValues() != null) {
            values.putAll(context.getValues());
        }
        values.put("totalScore", totalScore);
        values.put("hitRuleCount", hitRuleCount);
        values.put("matchedBandCode", scoreBand == null ? null : scoreBand.getCode());
        values.put("matchedBandAction", scoreBand == null || scoreBand.getAction() == null ? null : scoreBand.getAction().name());
        return values;
    }

    private Map<String, PolicyRuleRefSpec> policyRuleRefMap(PolicySpec policy) {
        Map<String, PolicyRuleRefSpec> ruleRefs = new LinkedHashMap<>();
        if (policy == null || policy.getRuleRefs() == null) {
            return ruleRefs;
        }
        for (PolicyRuleRefSpec ruleRef : policy.getRuleRefs()) {
            if (ruleRef == null || ruleRef.getRuleCode() == null || Boolean.FALSE.equals(ruleRef.getEnabled())) {
                continue;
            }
            ruleRefs.put(ruleRef.getRuleCode(), ruleRef);
        }
        return ruleRefs;
    }

    private boolean shouldStopScoreCardEvaluation(PolicySpec policy,
                                                  PolicyRuleRefSpec ruleRef,
                                                  RuleHit ruleHit) {
        return policy != null
                && policy.getDecisionMode() == DecisionMode.SCORE_CARD
                && ruleHit != null
                && Boolean.TRUE.equals(ruleHit.getHit())
                && ruleRef != null
                && Boolean.TRUE.equals(ruleRef.getStopOnHit());
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
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

    public record PreparedDecisionContext(RiskEvent event,
                                          EvalContext evalContext,
                                          long startedAtNanos) {

        public Map<String, Object> streamFeatureSnapshot(CompiledSceneRuntime runtime) {
            Map<String, Object> featureSnapshot = new LinkedHashMap<>();
            if (runtime == null || evalContext == null || runtime.getStreamFeatures() == null) {
                return featureSnapshot;
            }
            for (CompiledSceneRuntime.CompiledStreamFeature feature : runtime.getStreamFeatures()) {
                if (feature == null || feature.getSpec() == null || feature.getSpec().getCode() == null) {
                    continue;
                }
                String featureCode = feature.getSpec().getCode();
                if (evalContext.getValues().containsKey(featureCode)) {
                    featureSnapshot.put(featureCode, evalContext.get(featureCode));
                }
            }
            return featureSnapshot;
        }

    }

    private record PolicyOutcome(ActionType action,
                                 int finalScore,
                                 Integer totalScore,
                                 String reason,
                                 MatchedScoreBand matchedScoreBand,
                                 List<ScoreContribution> scoreContributions) {
    }

}
