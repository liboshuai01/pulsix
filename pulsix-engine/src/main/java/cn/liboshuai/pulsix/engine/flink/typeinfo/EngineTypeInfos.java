package cn.liboshuai.pulsix.engine.flink.typeinfo;

import cn.liboshuai.pulsix.engine.feature.AbstractStreamFeatureStateStore;
import cn.liboshuai.pulsix.engine.flink.PreparedDecisionInput;
import cn.liboshuai.pulsix.engine.flink.PreparedStreamFeatureChunk;
import cn.liboshuai.pulsix.engine.flink.StreamFeatureRouteEvent;
import cn.liboshuai.pulsix.engine.flink.runtime.SceneReleaseTimeline;
import cn.liboshuai.pulsix.engine.model.DecisionLogRecord;
import cn.liboshuai.pulsix.engine.model.DecisionResult;
import cn.liboshuai.pulsix.engine.model.DerivedFeatureSpec;
import cn.liboshuai.pulsix.engine.model.EngineErrorRecord;
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
import cn.liboshuai.pulsix.engine.typeinfo.KernelTypeInfos;
import org.apache.flink.api.common.typeinfo.TypeInfoFactory;
import org.apache.flink.api.common.typeinfo.TypeInformation;

import java.util.Map;

public final class EngineTypeInfos {

    static {
        EngineTypeInfoRegistrar.registerAll();
    }

    private EngineTypeInfos() {
    }

    public static TypeInformation<RiskEvent> riskEvent() {
        return KernelTypeInfos.riskEvent();
    }

    public static TypeInformation<SceneSpec> sceneSpec() {
        return KernelTypeInfos.sceneSpec();
    }

    public static TypeInformation<EventSchemaSpec> eventSchemaSpec() {
        return KernelTypeInfos.eventSchemaSpec();
    }

    public static TypeInformation<PolicySpec> policySpec() {
        return KernelTypeInfos.policySpec();
    }

    public static TypeInformation<PolicyRuleRefSpec> policyRuleRefSpec() {
        return KernelTypeInfos.policyRuleRefSpec();
    }

    public static TypeInformation<ScoreBandSpec> scoreBandSpec() {
        return KernelTypeInfos.scoreBandSpec();
    }

    public static TypeInformation<MatchedScoreBand> matchedScoreBand() {
        return KernelTypeInfos.matchedScoreBand();
    }

    public static TypeInformation<ScoreContribution> scoreContribution() {
        return KernelTypeInfos.scoreContribution();
    }

    public static TypeInformation<RuntimeHints> runtimeHints() {
        return KernelTypeInfos.runtimeHints();
    }

    public static TypeInformation<LookupFeatureSpec> lookupFeatureSpec() {
        return KernelTypeInfos.lookupFeatureSpec();
    }

    public static TypeInformation<StreamFeatureSpec> streamFeatureSpec() {
        return KernelTypeInfos.streamFeatureSpec();
    }

    public static TypeInformation<DerivedFeatureSpec> derivedFeatureSpec() {
        return KernelTypeInfos.derivedFeatureSpec();
    }

    public static TypeInformation<RuleSpec> ruleSpec() {
        return KernelTypeInfos.ruleSpec();
    }

    public static TypeInformation<SceneSnapshot> sceneSnapshot() {
        return KernelTypeInfos.sceneSnapshot();
    }

    public static TypeInformation<SceneSnapshotEnvelope> sceneSnapshotEnvelope() {
        return KernelTypeInfos.sceneSnapshotEnvelope();
    }

    public static TypeInformation<SceneReleaseTimeline> sceneReleaseTimeline() {
        return create(new EngineTypeInfoFactories.SceneReleaseTimelineTypeInfoFactory(), SceneReleaseTimeline.class);
    }

    public static TypeInformation<RuleHit> ruleHit() {
        return KernelTypeInfos.ruleHit();
    }

    public static TypeInformation<DecisionResult> decisionResult() {
        return KernelTypeInfos.decisionResult();
    }

    public static TypeInformation<DecisionLogRecord> decisionLogRecord() {
        return KernelTypeInfos.decisionLogRecord();
    }

    public static TypeInformation<EngineErrorRecord> engineErrorRecord() {
        return KernelTypeInfos.engineErrorRecord();
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
