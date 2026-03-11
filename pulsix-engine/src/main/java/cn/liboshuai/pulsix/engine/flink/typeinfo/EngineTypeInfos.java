package cn.liboshuai.pulsix.engine.flink.typeinfo;

import cn.liboshuai.pulsix.engine.model.DecisionLogRecord;
import cn.liboshuai.pulsix.engine.model.DecisionResult;
import cn.liboshuai.pulsix.engine.model.DerivedFeatureSpec;
import cn.liboshuai.pulsix.engine.model.EngineErrorRecord;
import cn.liboshuai.pulsix.engine.model.EventSchemaSpec;
import cn.liboshuai.pulsix.engine.model.LookupFeatureSpec;
import cn.liboshuai.pulsix.engine.model.PolicySpec;
import cn.liboshuai.pulsix.engine.model.RiskEvent;
import cn.liboshuai.pulsix.engine.model.RuleHit;
import cn.liboshuai.pulsix.engine.model.RuleSpec;
import cn.liboshuai.pulsix.engine.model.RuntimeHints;
import cn.liboshuai.pulsix.engine.model.SceneSnapshot;
import cn.liboshuai.pulsix.engine.model.SceneSnapshotEnvelope;
import cn.liboshuai.pulsix.engine.model.SceneSpec;
import cn.liboshuai.pulsix.engine.model.StreamFeatureSpec;
import org.apache.flink.api.common.typeinfo.TypeInfoFactory;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.api.java.typeutils.TypeExtractor;

import java.util.Map;

public final class EngineTypeInfos {

    static {
        TypeExtractor.registerFactory(RiskEvent.class, EngineTypeInfoFactories.RiskEventTypeInfoFactory.class);
        TypeExtractor.registerFactory(SceneSpec.class, EngineTypeInfoFactories.SceneSpecTypeInfoFactory.class);
        TypeExtractor.registerFactory(EventSchemaSpec.class, EngineTypeInfoFactories.EventSchemaSpecTypeInfoFactory.class);
        TypeExtractor.registerFactory(PolicySpec.class, EngineTypeInfoFactories.PolicySpecTypeInfoFactory.class);
        TypeExtractor.registerFactory(RuntimeHints.class, EngineTypeInfoFactories.RuntimeHintsTypeInfoFactory.class);
        TypeExtractor.registerFactory(LookupFeatureSpec.class, EngineTypeInfoFactories.LookupFeatureSpecTypeInfoFactory.class);
        TypeExtractor.registerFactory(StreamFeatureSpec.class, EngineTypeInfoFactories.StreamFeatureSpecTypeInfoFactory.class);
        TypeExtractor.registerFactory(DerivedFeatureSpec.class, EngineTypeInfoFactories.DerivedFeatureSpecTypeInfoFactory.class);
        TypeExtractor.registerFactory(RuleSpec.class, EngineTypeInfoFactories.RuleSpecTypeInfoFactory.class);
        TypeExtractor.registerFactory(SceneSnapshot.class, EngineTypeInfoFactories.SceneSnapshotTypeInfoFactory.class);
        TypeExtractor.registerFactory(SceneSnapshotEnvelope.class, EngineTypeInfoFactories.SceneSnapshotEnvelopeTypeInfoFactory.class);
        TypeExtractor.registerFactory(RuleHit.class, EngineTypeInfoFactories.RuleHitTypeInfoFactory.class);
        TypeExtractor.registerFactory(DecisionResult.class, EngineTypeInfoFactories.DecisionResultTypeInfoFactory.class);
        TypeExtractor.registerFactory(DecisionLogRecord.class, EngineTypeInfoFactories.DecisionLogRecordTypeInfoFactory.class);
    }

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

    public static TypeInformation<RuleHit> ruleHit() {
        return create(new EngineTypeInfoFactories.RuleHitTypeInfoFactory(), RuleHit.class);
    }

    public static TypeInformation<DecisionResult> decisionResult() {
        return create(new EngineTypeInfoFactories.DecisionResultTypeInfoFactory(), DecisionResult.class);
    }

    public static TypeInformation<DecisionLogRecord> decisionLogRecord() {
        return create(new EngineTypeInfoFactories.DecisionLogRecordTypeInfoFactory(), DecisionLogRecord.class);
    }

    public static TypeInformation<EngineErrorRecord> engineErrorRecord() {
        return TypeInformation.of(EngineErrorRecord.class);
    }

    private static <T> TypeInformation<T> create(TypeInfoFactory<T> factory, Class<T> type) {
        return factory.createTypeInfo(type, Map.of());
    }

}
