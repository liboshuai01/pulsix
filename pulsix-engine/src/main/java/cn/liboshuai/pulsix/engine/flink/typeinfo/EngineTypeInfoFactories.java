package cn.liboshuai.pulsix.engine.flink.typeinfo;

import cn.liboshuai.pulsix.engine.feature.AbstractStreamFeatureStateStore;
import cn.liboshuai.pulsix.engine.model.ActionType;
import cn.liboshuai.pulsix.engine.model.AggType;
import cn.liboshuai.pulsix.engine.model.DecisionLogRecord;
import cn.liboshuai.pulsix.engine.model.DecisionMode;
import cn.liboshuai.pulsix.engine.model.DecisionResult;
import cn.liboshuai.pulsix.engine.model.DerivedFeatureSpec;
import cn.liboshuai.pulsix.engine.model.EngineType;
import cn.liboshuai.pulsix.engine.model.EventSchemaSpec;
import cn.liboshuai.pulsix.engine.model.FeatureType;
import cn.liboshuai.pulsix.engine.model.LookupFeatureSpec;
import cn.liboshuai.pulsix.engine.model.LookupType;
import cn.liboshuai.pulsix.engine.model.PolicySpec;
import cn.liboshuai.pulsix.engine.model.PublishType;
import cn.liboshuai.pulsix.engine.model.RiskEvent;
import cn.liboshuai.pulsix.engine.model.RuleHit;
import cn.liboshuai.pulsix.engine.model.RuleSpec;
import cn.liboshuai.pulsix.engine.model.RuntimeHints;
import cn.liboshuai.pulsix.engine.model.SceneSnapshot;
import cn.liboshuai.pulsix.engine.model.SceneSnapshotEnvelope;
import cn.liboshuai.pulsix.engine.model.SceneSpec;
import cn.liboshuai.pulsix.engine.model.ScoreBandSpec;
import cn.liboshuai.pulsix.engine.model.StreamFeatureSpec;
import cn.liboshuai.pulsix.engine.model.WindowType;
import org.apache.flink.api.common.typeinfo.TypeInfoFactory;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.api.common.typeinfo.Types;

import java.lang.reflect.Type;
import java.util.LinkedHashMap;
import java.util.Map;

public final class EngineTypeInfoFactories {

    private EngineTypeInfoFactories() {
    }

    public static final class RiskEventTypeInfoFactory extends TypeInfoFactory<RiskEvent> {

        @Override
        public TypeInformation<RiskEvent> createTypeInfo(Type type, Map<String, TypeInformation<?>> genericParameters) {
            Map<String, TypeInformation<?>> fields = new LinkedHashMap<>();
            fields.put("eventId", Types.STRING);
            fields.put("traceId", Types.STRING);
            fields.put("sceneCode", Types.STRING);
            fields.put("eventType", Types.STRING);
            fields.put("eventTime", Types.INSTANT);
            fields.put("userId", Types.STRING);
            fields.put("deviceId", Types.STRING);
            fields.put("ip", Types.STRING);
            fields.put("amount", Types.BIG_DEC);
            fields.put("result", Types.STRING);
            fields.put("merchantId", Types.STRING);
            fields.put("channel", Types.STRING);
            fields.put("province", Types.STRING);
            fields.put("city", Types.STRING);
            fields.put("currency", Types.STRING);
            fields.put("ext", Types.MAP(Types.STRING, Types.STRING));
            return EnginePojoTypeInfos.pojo(RiskEvent.class, fields);
        }

    }

    public static final class SceneSpecTypeInfoFactory extends TypeInfoFactory<SceneSpec> {

        @Override
        public TypeInformation<SceneSpec> createTypeInfo(Type type, Map<String, TypeInformation<?>> genericParameters) {
            Map<String, TypeInformation<?>> fields = new LinkedHashMap<>();
            fields.put("defaultPolicyCode", Types.STRING);
            fields.put("allowedEventTypes", Types.LIST(Types.STRING));
            fields.put("decisionTimeoutMs", Types.INT);
            fields.put("logLevel", Types.STRING);
            return EnginePojoTypeInfos.pojo(SceneSpec.class, fields);
        }

    }

    public static final class EventSchemaSpecTypeInfoFactory extends TypeInfoFactory<EventSchemaSpec> {

        @Override
        public TypeInformation<EventSchemaSpec> createTypeInfo(Type type, Map<String, TypeInformation<?>> genericParameters) {
            Map<String, TypeInformation<?>> fields = new LinkedHashMap<>();
            fields.put("eventCode", Types.STRING);
            fields.put("eventType", Types.STRING);
            fields.put("requiredFields", Types.LIST(Types.STRING));
            fields.put("optionalFields", Types.LIST(Types.STRING));
            return EnginePojoTypeInfos.pojo(EventSchemaSpec.class, fields);
        }

    }

    public static final class PolicySpecTypeInfoFactory extends TypeInfoFactory<PolicySpec> {

        @Override
        public TypeInformation<PolicySpec> createTypeInfo(Type type, Map<String, TypeInformation<?>> genericParameters) {
            Map<String, TypeInformation<?>> fields = new LinkedHashMap<>();
            fields.put("policyCode", Types.STRING);
            fields.put("policyName", Types.STRING);
            fields.put("decisionMode", Types.ENUM(DecisionMode.class));
            fields.put("defaultAction", Types.ENUM(ActionType.class));
            fields.put("ruleOrder", Types.LIST(Types.STRING));
            fields.put("scoreBands", Types.LIST(TypeInformation.of(ScoreBandSpec.class)));
            return EnginePojoTypeInfos.pojo(PolicySpec.class, fields);
        }

    }

    public static final class RuntimeHintsTypeInfoFactory extends TypeInfoFactory<RuntimeHints> {

        @Override
        public TypeInformation<RuntimeHints> createTypeInfo(Type type, Map<String, TypeInformation<?>> genericParameters) {
            Map<String, TypeInformation<?>> fields = new LinkedHashMap<>();
            fields.put("requiredStreamFeatures", Types.LIST(Types.STRING));
            fields.put("requiredLookupFeatures", Types.LIST(Types.STRING));
            fields.put("requiredDerivedFeatures", Types.LIST(Types.STRING));
            fields.put("maxRuleExecutionCount", Types.INT);
            fields.put("allowGroovy", Types.BOOLEAN);
            fields.put("needFullDecisionLog", Types.BOOLEAN);
            return EnginePojoTypeInfos.pojo(RuntimeHints.class, fields);
        }

    }

    public static final class LookupFeatureSpecTypeInfoFactory extends TypeInfoFactory<LookupFeatureSpec> {

        @Override
        public TypeInformation<LookupFeatureSpec> createTypeInfo(Type type, Map<String, TypeInformation<?>> genericParameters) {
            Map<String, TypeInformation<?>> fields = featureSpecFields();
            fields.put("lookupType", Types.ENUM(LookupType.class));
            fields.put("keyExpr", Types.STRING);
            fields.put("sourceRef", Types.STRING);
            fields.put("defaultValue", Types.STRING);
            fields.put("timeoutMs", Types.INT);
            fields.put("cacheTtlSeconds", Types.INT);
            return EnginePojoTypeInfos.pojo(LookupFeatureSpec.class, fields);
        }

    }

    public static final class StreamFeatureSpecTypeInfoFactory extends TypeInfoFactory<StreamFeatureSpec> {

        @Override
        public TypeInformation<StreamFeatureSpec> createTypeInfo(Type type, Map<String, TypeInformation<?>> genericParameters) {
            Map<String, TypeInformation<?>> fields = featureSpecFields();
            fields.put("sourceEventTypes", Types.LIST(Types.STRING));
            fields.put("entityType", Types.STRING);
            fields.put("entityKeyExpr", Types.STRING);
            fields.put("aggType", Types.ENUM(AggType.class));
            fields.put("valueExpr", Types.STRING);
            fields.put("filterExpr", Types.STRING);
            fields.put("windowType", Types.ENUM(WindowType.class));
            fields.put("windowSize", Types.STRING);
            fields.put("windowSlide", Types.STRING);
            fields.put("includeCurrentEvent", Types.BOOLEAN);
            fields.put("ttl", Types.STRING);
            return EnginePojoTypeInfos.pojo(StreamFeatureSpec.class, fields);
        }

    }

    public static final class DerivedFeatureSpecTypeInfoFactory extends TypeInfoFactory<DerivedFeatureSpec> {

        @Override
        public TypeInformation<DerivedFeatureSpec> createTypeInfo(Type type, Map<String, TypeInformation<?>> genericParameters) {
            Map<String, TypeInformation<?>> fields = featureSpecFields();
            fields.put("engineType", Types.ENUM(EngineType.class));
            fields.put("expr", Types.STRING);
            fields.put("dependsOn", Types.LIST(Types.STRING));
            return EnginePojoTypeInfos.pojo(DerivedFeatureSpec.class, fields);
        }

    }

    public static final class RuleSpecTypeInfoFactory extends TypeInfoFactory<RuleSpec> {

        @Override
        public TypeInformation<RuleSpec> createTypeInfo(Type type, Map<String, TypeInformation<?>> genericParameters) {
            Map<String, TypeInformation<?>> fields = new LinkedHashMap<>();
            fields.put("code", Types.STRING);
            fields.put("name", Types.STRING);
            fields.put("engineType", Types.ENUM(EngineType.class));
            fields.put("priority", Types.INT);
            fields.put("whenExpr", Types.STRING);
            fields.put("dependsOn", Types.LIST(Types.STRING));
            fields.put("hitAction", Types.ENUM(ActionType.class));
            fields.put("riskScore", Types.INT);
            fields.put("hitReasonTemplate", Types.STRING);
            fields.put("enabled", Types.BOOLEAN);
            return EnginePojoTypeInfos.pojo(RuleSpec.class, fields);
        }

    }

    public static final class SceneSnapshotTypeInfoFactory extends TypeInfoFactory<SceneSnapshot> {

        @Override
        public TypeInformation<SceneSnapshot> createTypeInfo(Type type, Map<String, TypeInformation<?>> genericParameters) {
            Map<String, TypeInformation<?>> fields = new LinkedHashMap<>();
            fields.put("snapshotId", Types.STRING);
            fields.put("sceneCode", Types.STRING);
            fields.put("sceneName", Types.STRING);
            fields.put("version", Types.INT);
            fields.put("status", Types.STRING);
            fields.put("checksum", Types.STRING);
            fields.put("publishedAt", Types.INSTANT);
            fields.put("effectiveFrom", Types.INSTANT);
            fields.put("runtimeMode", Types.STRING);
            fields.put("scene", EngineTypeInfos.sceneSpec());
            fields.put("eventSchema", EngineTypeInfos.eventSchemaSpec());
            fields.put("variables", Types.MAP(Types.STRING, Types.LIST(Types.STRING)));
            fields.put("streamFeatures", Types.LIST(EngineTypeInfos.streamFeatureSpec()));
            fields.put("lookupFeatures", Types.LIST(EngineTypeInfos.lookupFeatureSpec()));
            fields.put("derivedFeatures", Types.LIST(EngineTypeInfos.derivedFeatureSpec()));
            fields.put("rules", Types.LIST(EngineTypeInfos.ruleSpec()));
            fields.put("policy", EngineTypeInfos.policySpec());
            fields.put("runtimeHints", EngineTypeInfos.runtimeHints());
            return EnginePojoTypeInfos.pojo(SceneSnapshot.class, fields);
        }

    }

    public static final class SceneSnapshotEnvelopeTypeInfoFactory extends TypeInfoFactory<SceneSnapshotEnvelope> {

        @Override
        public TypeInformation<SceneSnapshotEnvelope> createTypeInfo(Type type, Map<String, TypeInformation<?>> genericParameters) {
            Map<String, TypeInformation<?>> fields = new LinkedHashMap<>();
            fields.put("sceneCode", Types.STRING);
            fields.put("version", Types.INT);
            fields.put("checksum", Types.STRING);
            fields.put("publishType", Types.ENUM(PublishType.class));
            fields.put("publishedAt", Types.INSTANT);
            fields.put("effectiveFrom", Types.INSTANT);
            fields.put("snapshot", EngineTypeInfos.sceneSnapshot());
            return EnginePojoTypeInfos.pojo(SceneSnapshotEnvelope.class, fields);
        }

    }

    public static final class RuleHitTypeInfoFactory extends TypeInfoFactory<RuleHit> {

        @Override
        public TypeInformation<RuleHit> createTypeInfo(Type type, Map<String, TypeInformation<?>> genericParameters) {
            Map<String, TypeInformation<?>> fields = new LinkedHashMap<>();
            fields.put("ruleCode", Types.STRING);
            fields.put("ruleName", Types.STRING);
            fields.put("priority", Types.INT);
            fields.put("hit", Types.BOOLEAN);
            fields.put("action", Types.ENUM(ActionType.class));
            fields.put("score", Types.INT);
            fields.put("reason", Types.STRING);
            fields.put("detail", Types.MAP(Types.STRING, Types.STRING));
            return EnginePojoTypeInfos.pojo(RuleHit.class, fields);
        }

    }

    public static final class DecisionResultTypeInfoFactory extends TypeInfoFactory<DecisionResult> {

        @Override
        public TypeInformation<DecisionResult> createTypeInfo(Type type, Map<String, TypeInformation<?>> genericParameters) {
            Map<String, TypeInformation<?>> fields = new LinkedHashMap<>();
            fields.put("eventId", Types.STRING);
            fields.put("traceId", Types.STRING);
            fields.put("sceneCode", Types.STRING);
            fields.put("version", Types.INT);
            fields.put("decisionMode", Types.ENUM(DecisionMode.class));
            fields.put("finalAction", Types.ENUM(ActionType.class));
            fields.put("finalScore", Types.INT);
            fields.put("latencyMs", Types.LONG);
            fields.put("ruleHits", Types.LIST(EngineTypeInfos.ruleHit()));
            fields.put("featureSnapshot", Types.MAP(Types.STRING, Types.STRING));
            fields.put("traceLogs", Types.LIST(Types.STRING));
            fields.put("errorMessage", Types.STRING);
            return EnginePojoTypeInfos.pojo(DecisionResult.class, fields);
        }

    }

    public static final class DecisionLogRecordTypeInfoFactory extends TypeInfoFactory<DecisionLogRecord> {

        @Override
        public TypeInformation<DecisionLogRecord> createTypeInfo(Type type, Map<String, TypeInformation<?>> genericParameters) {
            Map<String, TypeInformation<?>> fields = new LinkedHashMap<>();
            fields.put("eventId", Types.STRING);
            fields.put("traceId", Types.STRING);
            fields.put("sceneCode", Types.STRING);
            fields.put("version", Types.INT);
            fields.put("finalAction", Types.ENUM(ActionType.class));
            fields.put("finalScore", Types.INT);
            fields.put("latencyMs", Types.LONG);
            fields.put("ruleHits", Types.LIST(EngineTypeInfos.ruleHit()));
            fields.put("featureSnapshot", Types.MAP(Types.STRING, Types.STRING));
            fields.put("traceLogs", Types.LIST(Types.STRING));
            return EnginePojoTypeInfos.pojo(DecisionLogRecord.class, fields);
        }

    }

    public static final class NumericWindowStateTypeInfoFactory extends TypeInfoFactory<AbstractStreamFeatureStateStore.NumericWindowState> {

        @Override
        public TypeInformation<AbstractStreamFeatureStateStore.NumericWindowState> createTypeInfo(Type type,
                                                                                                   Map<String, TypeInformation<?>> genericParameters) {
            Map<String, TypeInformation<?>> fields = new LinkedHashMap<>();
            fields.put("retentionMs", Types.LONG);
            fields.put("bucketSizeMs", Types.LONG);
            fields.put("buckets", Types.MAP(Types.LONG, Types.BIG_DEC));
            return EnginePojoTypeInfos.pojo(AbstractStreamFeatureStateStore.NumericWindowState.class, fields);
        }

    }

    public static final class LatestValueStateTypeInfoFactory extends TypeInfoFactory<AbstractStreamFeatureStateStore.LatestValueState> {

        @Override
        public TypeInformation<AbstractStreamFeatureStateStore.LatestValueState> createTypeInfo(Type type,
                                                                                                 Map<String, TypeInformation<?>> genericParameters) {
            Map<String, TypeInformation<?>> fields = new LinkedHashMap<>();
            fields.put("retentionMs", Types.LONG);
            fields.put("latestEventTimeMs", Types.LONG);
            fields.put("latestValueRaw", Types.STRING);
            fields.put("latestValueType", Types.STRING);
            return EnginePojoTypeInfos.pojo(AbstractStreamFeatureStateStore.LatestValueState.class, fields);
        }

    }

    public static final class DistinctWindowStateTypeInfoFactory extends TypeInfoFactory<AbstractStreamFeatureStateStore.DistinctWindowState> {

        @Override
        public TypeInformation<AbstractStreamFeatureStateStore.DistinctWindowState> createTypeInfo(Type type,
                                                                                                    Map<String, TypeInformation<?>> genericParameters) {
            Map<String, TypeInformation<?>> fields = new LinkedHashMap<>();
            fields.put("retentionMs", Types.LONG);
            fields.put("memberLastSeenMs", Types.MAP(Types.STRING, Types.LONG));
            return EnginePojoTypeInfos.pojo(AbstractStreamFeatureStateStore.DistinctWindowState.class, fields);
        }

    }

    private static Map<String, TypeInformation<?>> featureSpecFields() {
        Map<String, TypeInformation<?>> fields = new LinkedHashMap<>();
        fields.put("code", Types.STRING);
        fields.put("name", Types.STRING);
        fields.put("type", Types.ENUM(FeatureType.class));
        fields.put("valueType", Types.STRING);
        fields.put("description", Types.STRING);
        return fields;
    }

}
