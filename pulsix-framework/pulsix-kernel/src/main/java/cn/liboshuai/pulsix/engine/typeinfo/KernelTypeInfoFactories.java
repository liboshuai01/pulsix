package cn.liboshuai.pulsix.engine.typeinfo;

import cn.liboshuai.pulsix.engine.model.ActionType;
import cn.liboshuai.pulsix.engine.model.AggType;
import cn.liboshuai.pulsix.engine.model.DecisionLogRecord;
import cn.liboshuai.pulsix.engine.model.DecisionMode;
import cn.liboshuai.pulsix.engine.model.DecisionResult;
import cn.liboshuai.pulsix.engine.model.DerivedFeatureSpec;
import cn.liboshuai.pulsix.engine.model.EngineErrorRecord;
import cn.liboshuai.pulsix.engine.model.EngineType;
import cn.liboshuai.pulsix.engine.model.EventSchemaSpec;
import cn.liboshuai.pulsix.engine.model.FeatureType;
import cn.liboshuai.pulsix.engine.model.LookupFeatureSpec;
import cn.liboshuai.pulsix.engine.model.LookupType;
import cn.liboshuai.pulsix.engine.model.MatchedScoreBand;
import cn.liboshuai.pulsix.engine.model.PolicyRuleRefSpec;
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
import cn.liboshuai.pulsix.engine.model.ScoreContribution;
import cn.liboshuai.pulsix.engine.model.StreamFeatureSpec;
import cn.liboshuai.pulsix.engine.model.WindowType;
import org.apache.flink.api.common.typeinfo.TypeInfoFactory;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.api.common.typeinfo.Types;

import java.lang.reflect.Type;
import java.util.LinkedHashMap;
import java.util.Map;

public final class KernelTypeInfoFactories {

    private KernelTypeInfoFactories() {
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
            return Types.POJO(RiskEvent.class, fields);
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
            return Types.POJO(SceneSpec.class, fields);
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
            return Types.POJO(EventSchemaSpec.class, fields);
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
            fields.put("ruleRefs", Types.LIST(KernelTypeInfos.policyRuleRefSpec()));
            fields.put("scoreBands", Types.LIST(KernelTypeInfos.scoreBandSpec()));
            return Types.POJO(PolicySpec.class, fields);
        }

    }

    public static final class PolicyRuleRefSpecTypeInfoFactory extends TypeInfoFactory<PolicyRuleRefSpec> {

        @Override
        public TypeInformation<PolicyRuleRefSpec> createTypeInfo(Type type, Map<String, TypeInformation<?>> genericParameters) {
            Map<String, TypeInformation<?>> fields = new LinkedHashMap<>();
            fields.put("ruleCode", Types.STRING);
            fields.put("orderNo", Types.INT);
            fields.put("enabled", Types.BOOLEAN);
            fields.put("scoreWeight", Types.INT);
            fields.put("stopOnHit", Types.BOOLEAN);
            fields.put("branchExpr", Types.STRING);
            return Types.POJO(PolicyRuleRefSpec.class, fields);
        }

    }

    public static final class ScoreBandSpecTypeInfoFactory extends TypeInfoFactory<ScoreBandSpec> {

        @Override
        public TypeInformation<ScoreBandSpec> createTypeInfo(Type type, Map<String, TypeInformation<?>> genericParameters) {
            Map<String, TypeInformation<?>> fields = new LinkedHashMap<>();
            fields.put("code", Types.STRING);
            fields.put("minScore", Types.INT);
            fields.put("maxScore", Types.INT);
            fields.put("action", Types.ENUM(ActionType.class));
            fields.put("reason", Types.STRING);
            fields.put("reasonTemplate", Types.STRING);
            return Types.POJO(ScoreBandSpec.class, fields);
        }

    }

    public static final class MatchedScoreBandTypeInfoFactory extends TypeInfoFactory<MatchedScoreBand> {

        @Override
        public TypeInformation<MatchedScoreBand> createTypeInfo(Type type, Map<String, TypeInformation<?>> genericParameters) {
            Map<String, TypeInformation<?>> fields = new LinkedHashMap<>();
            fields.put("code", Types.STRING);
            fields.put("minScore", Types.INT);
            fields.put("maxScore", Types.INT);
            fields.put("action", Types.ENUM(ActionType.class));
            fields.put("reason", Types.STRING);
            return Types.POJO(MatchedScoreBand.class, fields);
        }

    }

    public static final class ScoreContributionTypeInfoFactory extends TypeInfoFactory<ScoreContribution> {

        @Override
        public TypeInformation<ScoreContribution> createTypeInfo(Type type, Map<String, TypeInformation<?>> genericParameters) {
            Map<String, TypeInformation<?>> fields = new LinkedHashMap<>();
            fields.put("ruleCode", Types.STRING);
            fields.put("ruleName", Types.STRING);
            fields.put("action", Types.ENUM(ActionType.class));
            fields.put("rawScore", Types.INT);
            fields.put("scoreWeight", Types.INT);
            fields.put("weightedScore", Types.INT);
            fields.put("stopOnHit", Types.BOOLEAN);
            fields.put("reason", Types.STRING);
            return Types.POJO(ScoreContribution.class, fields);
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
            return Types.POJO(RuntimeHints.class, fields);
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
            return Types.POJO(LookupFeatureSpec.class, fields);
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
            return Types.POJO(StreamFeatureSpec.class, fields);
        }

    }

    public static final class DerivedFeatureSpecTypeInfoFactory extends TypeInfoFactory<DerivedFeatureSpec> {

        @Override
        public TypeInformation<DerivedFeatureSpec> createTypeInfo(Type type, Map<String, TypeInformation<?>> genericParameters) {
            Map<String, TypeInformation<?>> fields = featureSpecFields();
            fields.put("engineType", Types.ENUM(EngineType.class));
            fields.put("expr", Types.STRING);
            fields.put("dependsOn", Types.LIST(Types.STRING));
            return Types.POJO(DerivedFeatureSpec.class, fields);
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
            return Types.POJO(RuleSpec.class, fields);
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
            fields.put("scene", KernelTypeInfos.sceneSpec());
            fields.put("eventSchema", KernelTypeInfos.eventSchemaSpec());
            fields.put("variables", Types.MAP(Types.STRING, Types.LIST(Types.STRING)));
            fields.put("streamFeatures", Types.LIST(KernelTypeInfos.streamFeatureSpec()));
            fields.put("lookupFeatures", Types.LIST(KernelTypeInfos.lookupFeatureSpec()));
            fields.put("derivedFeatures", Types.LIST(KernelTypeInfos.derivedFeatureSpec()));
            fields.put("rules", Types.LIST(KernelTypeInfos.ruleSpec()));
            fields.put("policy", KernelTypeInfos.policySpec());
            fields.put("runtimeHints", KernelTypeInfos.runtimeHints());
            return Types.POJO(SceneSnapshot.class, fields);
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
            fields.put("snapshot", KernelTypeInfos.sceneSnapshot());
            return Types.POJO(SceneSnapshotEnvelope.class, fields);
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
            return Types.POJO(RuleHit.class, fields);
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
            fields.put("snapshotId", Types.STRING);
            fields.put("snapshotChecksum", Types.STRING);
            fields.put("decisionMode", Types.ENUM(DecisionMode.class));
            fields.put("finalAction", Types.ENUM(ActionType.class));
            fields.put("finalScore", Types.INT);
            fields.put("totalScore", Types.INT);
            fields.put("reason", Types.STRING);
            fields.put("matchedScoreBand", KernelTypeInfos.matchedScoreBand());
            fields.put("scoreContributions", Types.LIST(KernelTypeInfos.scoreContribution()));
            fields.put("latencyMs", Types.LONG);
            fields.put("ruleHits", Types.LIST(KernelTypeInfos.ruleHit()));
            fields.put("featureSnapshot", Types.MAP(Types.STRING, Types.STRING));
            fields.put("traceLogs", Types.LIST(Types.STRING));
            fields.put("errorMessage", Types.STRING);
            return Types.POJO(DecisionResult.class, fields);
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
            fields.put("snapshotId", Types.STRING);
            fields.put("snapshotChecksum", Types.STRING);
            fields.put("finalAction", Types.ENUM(ActionType.class));
            fields.put("finalScore", Types.INT);
            fields.put("latencyMs", Types.LONG);
            fields.put("ruleHits", Types.LIST(KernelTypeInfos.ruleHit()));
            fields.put("featureSnapshot", Types.MAP(Types.STRING, Types.STRING));
            fields.put("traceLogs", Types.LIST(Types.STRING));
            return Types.POJO(DecisionLogRecord.class, fields);
        }

    }

    public static final class EngineErrorRecordTypeInfoFactory extends TypeInfoFactory<EngineErrorRecord> {

        @Override
        public TypeInformation<EngineErrorRecord> createTypeInfo(Type type, Map<String, TypeInformation<?>> genericParameters) {
            Map<String, TypeInformation<?>> fields = new LinkedHashMap<>();
            fields.put("stage", Types.STRING);
            fields.put("errorType", Types.STRING);
            fields.put("sceneCode", Types.STRING);
            fields.put("version", Types.INT);
            fields.put("snapshotId", Types.STRING);
            fields.put("snapshotChecksum", Types.STRING);
            fields.put("eventId", Types.STRING);
            fields.put("traceId", Types.STRING);
            fields.put("errorCode", Types.STRING);
            fields.put("errorMessage", Types.STRING);
            fields.put("exceptionClass", Types.STRING);
            fields.put("featureCode", Types.STRING);
            fields.put("ruleCode", Types.STRING);
            fields.put("engineType", Types.STRING);
            fields.put("lookupType", Types.STRING);
            fields.put("sourceRef", Types.STRING);
            fields.put("lookupKey", Types.STRING);
            fields.put("fallbackMode", Types.STRING);
            fields.put("occurredAt", Types.INSTANT);
            return Types.POJO(EngineErrorRecord.class, fields);
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
