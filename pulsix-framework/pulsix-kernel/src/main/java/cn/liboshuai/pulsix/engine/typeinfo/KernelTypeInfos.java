package cn.liboshuai.pulsix.engine.typeinfo;

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
import org.apache.flink.api.common.typeinfo.TypeInfoFactory;
import org.apache.flink.api.common.typeinfo.TypeInformation;

import java.util.Map;

public final class KernelTypeInfos {

    private KernelTypeInfos() {
    }

    public static TypeInformation<RiskEvent> riskEvent() {
        return create(new KernelTypeInfoFactories.RiskEventTypeInfoFactory(), RiskEvent.class);
    }

    public static TypeInformation<SceneSpec> sceneSpec() {
        return create(new KernelTypeInfoFactories.SceneSpecTypeInfoFactory(), SceneSpec.class);
    }

    public static TypeInformation<EventSchemaSpec> eventSchemaSpec() {
        return create(new KernelTypeInfoFactories.EventSchemaSpecTypeInfoFactory(), EventSchemaSpec.class);
    }

    public static TypeInformation<PolicySpec> policySpec() {
        return create(new KernelTypeInfoFactories.PolicySpecTypeInfoFactory(), PolicySpec.class);
    }

    public static TypeInformation<PolicyRuleRefSpec> policyRuleRefSpec() {
        return create(new KernelTypeInfoFactories.PolicyRuleRefSpecTypeInfoFactory(), PolicyRuleRefSpec.class);
    }

    public static TypeInformation<ScoreBandSpec> scoreBandSpec() {
        return create(new KernelTypeInfoFactories.ScoreBandSpecTypeInfoFactory(), ScoreBandSpec.class);
    }

    public static TypeInformation<MatchedScoreBand> matchedScoreBand() {
        return create(new KernelTypeInfoFactories.MatchedScoreBandTypeInfoFactory(), MatchedScoreBand.class);
    }

    public static TypeInformation<ScoreContribution> scoreContribution() {
        return create(new KernelTypeInfoFactories.ScoreContributionTypeInfoFactory(), ScoreContribution.class);
    }

    public static TypeInformation<RuntimeHints> runtimeHints() {
        return create(new KernelTypeInfoFactories.RuntimeHintsTypeInfoFactory(), RuntimeHints.class);
    }

    public static TypeInformation<LookupFeatureSpec> lookupFeatureSpec() {
        return create(new KernelTypeInfoFactories.LookupFeatureSpecTypeInfoFactory(), LookupFeatureSpec.class);
    }

    public static TypeInformation<StreamFeatureSpec> streamFeatureSpec() {
        return create(new KernelTypeInfoFactories.StreamFeatureSpecTypeInfoFactory(), StreamFeatureSpec.class);
    }

    public static TypeInformation<DerivedFeatureSpec> derivedFeatureSpec() {
        return create(new KernelTypeInfoFactories.DerivedFeatureSpecTypeInfoFactory(), DerivedFeatureSpec.class);
    }

    public static TypeInformation<RuleSpec> ruleSpec() {
        return create(new KernelTypeInfoFactories.RuleSpecTypeInfoFactory(), RuleSpec.class);
    }

    public static TypeInformation<SceneSnapshot> sceneSnapshot() {
        return create(new KernelTypeInfoFactories.SceneSnapshotTypeInfoFactory(), SceneSnapshot.class);
    }

    public static TypeInformation<SceneSnapshotEnvelope> sceneSnapshotEnvelope() {
        return create(new KernelTypeInfoFactories.SceneSnapshotEnvelopeTypeInfoFactory(), SceneSnapshotEnvelope.class);
    }

    public static TypeInformation<RuleHit> ruleHit() {
        return create(new KernelTypeInfoFactories.RuleHitTypeInfoFactory(), RuleHit.class);
    }

    public static TypeInformation<DecisionResult> decisionResult() {
        return create(new KernelTypeInfoFactories.DecisionResultTypeInfoFactory(), DecisionResult.class);
    }

    public static TypeInformation<DecisionLogRecord> decisionLogRecord() {
        return create(new KernelTypeInfoFactories.DecisionLogRecordTypeInfoFactory(), DecisionLogRecord.class);
    }

    public static TypeInformation<EngineErrorRecord> engineErrorRecord() {
        return create(new KernelTypeInfoFactories.EngineErrorRecordTypeInfoFactory(), EngineErrorRecord.class);
    }

    private static <T> TypeInformation<T> create(TypeInfoFactory<T> factory, Class<T> type) {
        return factory.createTypeInfo(type, Map.of());
    }

}
