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
import cn.liboshuai.pulsix.engine.typeinfo.KernelTypeInfoFactories;
import org.apache.flink.api.java.typeutils.TypeExtractor;

public final class EngineTypeInfoRegistrar {

    private static volatile boolean registered;

    private EngineTypeInfoRegistrar() {
    }

    public static void registerAll() {
        if (registered) {
            return;
        }
        synchronized (EngineTypeInfoRegistrar.class) {
            if (registered) {
                return;
            }
            registerKernelFactories();
            registerEngineFactories();
            registered = true;
        }
    }

    private static void registerKernelFactories() {
        TypeExtractor.registerFactory(RiskEvent.class, KernelTypeInfoFactories.RiskEventTypeInfoFactory.class);
        TypeExtractor.registerFactory(SceneSpec.class, KernelTypeInfoFactories.SceneSpecTypeInfoFactory.class);
        TypeExtractor.registerFactory(EventSchemaSpec.class, KernelTypeInfoFactories.EventSchemaSpecTypeInfoFactory.class);
        TypeExtractor.registerFactory(PolicySpec.class, KernelTypeInfoFactories.PolicySpecTypeInfoFactory.class);
        TypeExtractor.registerFactory(PolicyRuleRefSpec.class, KernelTypeInfoFactories.PolicyRuleRefSpecTypeInfoFactory.class);
        TypeExtractor.registerFactory(ScoreBandSpec.class, KernelTypeInfoFactories.ScoreBandSpecTypeInfoFactory.class);
        TypeExtractor.registerFactory(MatchedScoreBand.class, KernelTypeInfoFactories.MatchedScoreBandTypeInfoFactory.class);
        TypeExtractor.registerFactory(ScoreContribution.class, KernelTypeInfoFactories.ScoreContributionTypeInfoFactory.class);
        TypeExtractor.registerFactory(RuntimeHints.class, KernelTypeInfoFactories.RuntimeHintsTypeInfoFactory.class);
        TypeExtractor.registerFactory(LookupFeatureSpec.class, KernelTypeInfoFactories.LookupFeatureSpecTypeInfoFactory.class);
        TypeExtractor.registerFactory(StreamFeatureSpec.class, KernelTypeInfoFactories.StreamFeatureSpecTypeInfoFactory.class);
        TypeExtractor.registerFactory(DerivedFeatureSpec.class, KernelTypeInfoFactories.DerivedFeatureSpecTypeInfoFactory.class);
        TypeExtractor.registerFactory(RuleSpec.class, KernelTypeInfoFactories.RuleSpecTypeInfoFactory.class);
        TypeExtractor.registerFactory(SceneSnapshot.class, KernelTypeInfoFactories.SceneSnapshotTypeInfoFactory.class);
        TypeExtractor.registerFactory(SceneSnapshotEnvelope.class, KernelTypeInfoFactories.SceneSnapshotEnvelopeTypeInfoFactory.class);
        TypeExtractor.registerFactory(RuleHit.class, KernelTypeInfoFactories.RuleHitTypeInfoFactory.class);
        TypeExtractor.registerFactory(DecisionResult.class, KernelTypeInfoFactories.DecisionResultTypeInfoFactory.class);
        TypeExtractor.registerFactory(DecisionLogRecord.class, KernelTypeInfoFactories.DecisionLogRecordTypeInfoFactory.class);
        TypeExtractor.registerFactory(EngineErrorRecord.class, KernelTypeInfoFactories.EngineErrorRecordTypeInfoFactory.class);
    }

    private static void registerEngineFactories() {
        TypeExtractor.registerFactory(SceneReleaseTimeline.class, EngineTypeInfoFactories.SceneReleaseTimelineTypeInfoFactory.class);
        TypeExtractor.registerFactory(StreamFeatureRouteEvent.class, EngineTypeInfoFactories.StreamFeatureRouteEventTypeInfoFactory.class);
        TypeExtractor.registerFactory(PreparedStreamFeatureChunk.class, EngineTypeInfoFactories.PreparedStreamFeatureChunkTypeInfoFactory.class);
        TypeExtractor.registerFactory(PreparedDecisionInput.class, EngineTypeInfoFactories.PreparedDecisionInputTypeInfoFactory.class);
        TypeExtractor.registerFactory(AbstractStreamFeatureStateStore.NumericWindowState.class,
                EngineTypeInfoFactories.NumericWindowStateTypeInfoFactory.class);
        TypeExtractor.registerFactory(AbstractStreamFeatureStateStore.LatestValueState.class,
                EngineTypeInfoFactories.LatestValueStateTypeInfoFactory.class);
        TypeExtractor.registerFactory(AbstractStreamFeatureStateStore.DistinctWindowState.class,
                EngineTypeInfoFactories.DistinctWindowStateTypeInfoFactory.class);
    }

}
