package cn.liboshuai.pulsix.engine.flink.typeinfo;

import cn.liboshuai.pulsix.engine.feature.AbstractStreamFeatureStateStore;
import cn.liboshuai.pulsix.engine.flink.PreparedDecisionInput;
import cn.liboshuai.pulsix.engine.flink.PreparedStreamFeatureChunk;
import cn.liboshuai.pulsix.engine.flink.StreamFeatureRouteEvent;
import cn.liboshuai.pulsix.engine.flink.runtime.SceneReleaseTimeline;
import cn.liboshuai.pulsix.engine.model.DecisionLogRecord;
import cn.liboshuai.pulsix.engine.model.DecisionResult;
import cn.liboshuai.pulsix.engine.model.DerivedFeatureSpec;
import cn.liboshuai.pulsix.engine.model.EventSchemaSpec;
import cn.liboshuai.pulsix.engine.model.LookupFeatureSpec;
import cn.liboshuai.pulsix.engine.model.MatchedScoreBand;
import cn.liboshuai.pulsix.engine.model.PolicyRuleRefSpec;
import cn.liboshuai.pulsix.engine.model.PolicySpec;
import cn.liboshuai.pulsix.engine.model.RiskEvent;
import cn.liboshuai.pulsix.engine.model.RuleHit;
import cn.liboshuai.pulsix.engine.model.RuleSpec;
import cn.liboshuai.pulsix.engine.model.RuntimeHints;
import cn.liboshuai.pulsix.engine.model.SceneSnapshot;
import cn.liboshuai.pulsix.engine.model.SceneSnapshotEnvelope;
import cn.liboshuai.pulsix.engine.model.SceneSpec;
import cn.liboshuai.pulsix.engine.model.ScoreBandSpec;
import cn.liboshuai.pulsix.engine.model.ScoreContribution;
import cn.liboshuai.pulsix.engine.model.StreamFeatureSpec;
import org.apache.flink.api.common.typeinfo.TypeInfoFactory;
import org.apache.flink.api.common.typeinfo.TypeInformation;

import java.util.Map;

public final class EngineTypeInfos {

    private EngineTypeInfos() {
    }

    public static TypeInformation<RiskEvent> riskEvent() {
        return create(new EngineTypeInfoFactories.RiskEventTypeInfoFactory(), RiskEvent.class);
    }

    public static TypeInformation<SceneSpec> sceneSpec() {
        return create(new EngineTypeInfoFactories.SceneSpecTypeInfoFactory(), SceneSpec.class);
    }

    public static TypeInformation<EventSchemaSpec> eventSchemaSpec() {
        return create(new EngineTypeInfoFactories.EventSchemaSpecTypeInfoFactory(), EventSchemaSpec.class);
    }

    public static TypeInformation<PolicySpec> policySpec() {
        return create(new EngineTypeInfoFactories.PolicySpecTypeInfoFactory(), PolicySpec.class);
    }

    public static TypeInformation<PolicyRuleRefSpec> policyRuleRefSpec() {
        return create(new EngineTypeInfoFactories.PolicyRuleRefSpecTypeInfoFactory(), PolicyRuleRefSpec.class);
    }

    public static TypeInformation<ScoreBandSpec> scoreBandSpec() {
        return create(new EngineTypeInfoFactories.ScoreBandSpecTypeInfoFactory(), ScoreBandSpec.class);
    }

    public static TypeInformation<MatchedScoreBand> matchedScoreBand() {
        return create(new EngineTypeInfoFactories.MatchedScoreBandTypeInfoFactory(), MatchedScoreBand.class);
    }

    public static TypeInformation<ScoreContribution> scoreContribution() {
        return create(new EngineTypeInfoFactories.ScoreContributionTypeInfoFactory(), ScoreContribution.class);
    }

    public static TypeInformation<RuntimeHints> runtimeHints() {
        return create(new EngineTypeInfoFactories.RuntimeHintsTypeInfoFactory(), RuntimeHints.class);
    }

    public static TypeInformation<LookupFeatureSpec> lookupFeatureSpec() {
        return create(new EngineTypeInfoFactories.LookupFeatureSpecTypeInfoFactory(), LookupFeatureSpec.class);
    }

    public static TypeInformation<StreamFeatureSpec> streamFeatureSpec() {
        return create(new EngineTypeInfoFactories.StreamFeatureSpecTypeInfoFactory(), StreamFeatureSpec.class);
    }

    public static TypeInformation<DerivedFeatureSpec> derivedFeatureSpec() {
        return create(new EngineTypeInfoFactories.DerivedFeatureSpecTypeInfoFactory(), DerivedFeatureSpec.class);
    }

    public static TypeInformation<RuleSpec> ruleSpec() {
        return create(new EngineTypeInfoFactories.RuleSpecTypeInfoFactory(), RuleSpec.class);
    }

    public static TypeInformation<SceneSnapshot> sceneSnapshot() {
        return create(new EngineTypeInfoFactories.SceneSnapshotTypeInfoFactory(), SceneSnapshot.class);
    }

    public static TypeInformation<SceneSnapshotEnvelope> sceneSnapshotEnvelope() {
        return create(new EngineTypeInfoFactories.SceneSnapshotEnvelopeTypeInfoFactory(), SceneSnapshotEnvelope.class);
    }

    public static TypeInformation<SceneReleaseTimeline> sceneReleaseTimeline() {
        return create(new EngineTypeInfoFactories.SceneReleaseTimelineTypeInfoFactory(), SceneReleaseTimeline.class);
    }

    public static TypeInformation<RuleHit> ruleHit() {
        return create(new EngineTypeInfoFactories.RuleHitTypeInfoFactory(), RuleHit.class);
    }

    public static TypeInformation<DecisionResult> decisionResult() {
        return create(new EngineTypeInfoFactories.DecisionResultTypeInfoFactory(), DecisionResult.class);
    }

    public static TypeInformation<DecisionLogRecord> decisionLogRecord() {
        return create(new EngineTypeInfoFactories.DecisionLogRecordTypeInfoFactory(), DecisionLogRecord.class);
    }


    public static TypeInformation<StreamFeatureRouteEvent> streamFeatureRouteEvent() {
        return create(new EngineTypeInfoFactories.StreamFeatureRouteEventTypeInfoFactory(), StreamFeatureRouteEvent.class);
    }

    public static TypeInformation<PreparedStreamFeatureChunk> preparedStreamFeatureChunk() {
        return create(new EngineTypeInfoFactories.PreparedStreamFeatureChunkTypeInfoFactory(), PreparedStreamFeatureChunk.class);
    }

    public static TypeInformation<PreparedDecisionInput> preparedDecisionInput() {
        return create(new EngineTypeInfoFactories.PreparedDecisionInputTypeInfoFactory(), PreparedDecisionInput.class);
    }

    public static TypeInformation<AbstractStreamFeatureStateStore.NumericWindowState> numericWindowState() {
        return create(new EngineTypeInfoFactories.NumericWindowStateTypeInfoFactory(),
                AbstractStreamFeatureStateStore.NumericWindowState.class);
    }

    public static TypeInformation<AbstractStreamFeatureStateStore.LatestValueState> latestValueState() {
        return create(new EngineTypeInfoFactories.LatestValueStateTypeInfoFactory(),
                AbstractStreamFeatureStateStore.LatestValueState.class);
    }

    public static TypeInformation<AbstractStreamFeatureStateStore.DistinctWindowState> distinctWindowState() {
        return create(new EngineTypeInfoFactories.DistinctWindowStateTypeInfoFactory(),
                AbstractStreamFeatureStateStore.DistinctWindowState.class);
    }

    private static <T> TypeInformation<T> create(TypeInfoFactory<T> factory, Class<T> type) {
        return factory.createTypeInfo(type, Map.of());
    }

}
