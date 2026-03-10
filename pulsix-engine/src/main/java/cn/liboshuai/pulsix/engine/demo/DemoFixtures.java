package cn.liboshuai.pulsix.engine.demo;

import cn.liboshuai.pulsix.engine.model.RiskEvent;
import cn.liboshuai.pulsix.engine.model.SceneSnapshot;
import cn.liboshuai.pulsix.engine.model.SceneSnapshotEnvelope;
import cn.liboshuai.pulsix.engine.model.PublishType;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.time.Instant;
import java.util.List;

public final class DemoFixtures {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper().registerModule(new JavaTimeModule());

    private DemoFixtures() {
    }

    public static SceneSnapshotEnvelope demoEnvelope() {
        SceneSnapshot snapshot = readValue(demoSnapshotJson(), SceneSnapshot.class);
        SceneSnapshotEnvelope envelope = new SceneSnapshotEnvelope();
        envelope.setSceneCode(snapshot.getSceneCode());
        envelope.setVersion(snapshot.getVersion());
        envelope.setChecksum(snapshot.getChecksum());
        envelope.setPublishType(PublishType.PUBLISH);
        envelope.setPublishedAt(snapshot.getPublishedAt());
        envelope.setEffectiveFrom(snapshot.getEffectiveFrom());
        envelope.setSnapshot(snapshot);
        return envelope;
    }

    public static SceneSnapshot demoSnapshot() {
        return demoEnvelope().getSnapshot();
    }

    public static String demoEnvelopeJson() {
        try {
            return OBJECT_MAPPER.writeValueAsString(demoEnvelope());
        } catch (Exception exception) {
            throw new IllegalStateException("write demo envelope failed", exception);
        }
    }

    public static String toJson(Object value) {
        try {
            return OBJECT_MAPPER.writeValueAsString(value);
        } catch (Exception exception) {
            throw new IllegalStateException("write demo json failed", exception);
        }
    }

    public static List<RiskEvent> demoEvents() {
        return readValue(demoEventsJson(), new TypeReference<List<RiskEvent>>() {
        });
    }

    public static RiskEvent blacklistedEvent() {
        return readValue("""
                {
                  "eventId": "E202603070099",
                  "traceId": "T202603070099",
                  "sceneCode": "TRADE_RISK",
                  "eventType": "trade",
                  "eventTime": "2026-03-07T10:01:00Z",
                  "userId": "U1001",
                  "deviceId": "D0009",
                  "ip": "9.9.9.9",
                  "amount": 1000,
                  "currency": "CNY",
                  "result": "SUCCESS",
                  "channel": "APP"
                }
                """, RiskEvent.class);
    }

    public static RiskEvent finalDecisionEvent() {
        List<RiskEvent> events = demoEvents();
        return events.get(events.size() - 1);
    }

    private static String demoSnapshotJson() {
        return """
                {
                  "snapshotId": "TRADE_RISK_v12",
                  "sceneCode": "TRADE_RISK",
                  "sceneName": "交易风控",
                  "version": 12,
                  "status": "ACTIVE",
                  "checksum": "8d2041a7cf8f47b4b6b0f91d2ab8d9d0",
                  "publishedAt": "2026-03-07T20:00:00Z",
                  "effectiveFrom": "2026-03-07T20:00:10Z",
                  "runtimeMode": "ASYNC_DECISION",
                  "scene": {
                    "defaultPolicyCode": "TRADE_RISK_POLICY",
                    "allowedEventTypes": ["trade"],
                    "decisionTimeoutMs": 500,
                    "logLevel": "FULL"
                  },
                  "eventSchema": {
                    "eventCode": "TRADE_EVENT",
                    "eventType": "trade",
                    "requiredFields": [
                      "eventId",
                      "sceneCode",
                      "eventType",
                      "eventTime",
                      "userId",
                      "deviceId",
                      "ip",
                      "amount",
                      "result"
                    ],
                    "optionalFields": ["merchantId", "channel", "province", "city", "ext"]
                  },
                  "variables": {
                    "baseFields": ["eventId", "sceneCode", "eventType", "eventTime", "traceId", "userId", "deviceId", "ip", "amount", "result", "merchantId", "channel", "province", "city"]
                  },
                  "streamFeatures": [
                    {
                      "code": "user_trade_cnt_5m",
                      "name": "用户5分钟交易次数",
                      "type": "STREAM",
                      "sourceEventTypes": ["trade"],
                      "entityType": "USER",
                      "entityKeyExpr": "userId",
                      "aggType": "COUNT",
                      "valueExpr": "1",
                      "filterExpr": "result == 'SUCCESS'",
                      "windowType": "SLIDING",
                      "windowSize": "5m",
                      "windowSlide": "1m",
                      "includeCurrentEvent": true,
                      "ttl": "10m",
                      "valueType": "LONG"
                    },
                    {
                      "code": "user_trade_amt_sum_30m",
                      "name": "用户30分钟交易金额和",
                      "type": "STREAM",
                      "sourceEventTypes": ["trade"],
                      "entityType": "USER",
                      "entityKeyExpr": "userId",
                      "aggType": "SUM",
                      "valueExpr": "amount",
                      "filterExpr": "result == 'SUCCESS'",
                      "windowType": "SLIDING",
                      "windowSize": "30m",
                      "windowSlide": "1m",
                      "includeCurrentEvent": true,
                      "ttl": "40m",
                      "valueType": "DECIMAL"
                    },
                    {
                      "code": "device_bind_user_cnt_1h",
                      "name": "设备1小时关联用户数",
                      "type": "STREAM",
                      "sourceEventTypes": ["trade"],
                      "entityType": "DEVICE",
                      "entityKeyExpr": "deviceId",
                      "aggType": "DISTINCT_COUNT",
                      "valueExpr": "userId",
                      "filterExpr": "deviceId != nil && userId != nil",
                      "windowType": "SLIDING",
                      "windowSize": "1h",
                      "windowSlide": "5m",
                      "includeCurrentEvent": true,
                      "ttl": "2h",
                      "valueType": "LONG"
                    }
                  ],
                  "lookupFeatures": [
                    {
                      "code": "device_in_blacklist",
                      "name": "设备是否命中黑名单",
                      "type": "LOOKUP",
                      "lookupType": "REDIS_SET",
                      "keyExpr": "deviceId",
                      "sourceRef": "pulsix:list:black:device",
                      "defaultValue": false,
                      "valueType": "BOOLEAN",
                      "timeoutMs": 20,
                      "cacheTtlSeconds": 30
                    },
                    {
                      "code": "user_risk_level",
                      "name": "用户风险等级",
                      "type": "LOOKUP",
                      "lookupType": "REDIS_HASH",
                      "keyExpr": "userId",
                      "sourceRef": "pulsix:profile:user:risk",
                      "defaultValue": "L",
                      "valueType": "STRING",
                      "timeoutMs": 20,
                      "cacheTtlSeconds": 30
                    }
                  ],
                  "derivedFeatures": [
                    {
                      "code": "high_amt_flag",
                      "name": "高金额标记",
                      "type": "DERIVED",
                      "engineType": "AVIATOR",
                      "expr": "amount >= 5000",
                      "dependsOn": ["amount"],
                      "valueType": "BOOLEAN"
                    },
                    {
                      "code": "trade_burst_flag",
                      "name": "短时高频交易标记",
                      "type": "DERIVED",
                      "engineType": "AVIATOR",
                      "expr": "user_trade_cnt_5m >= 3 && amount >= 5000",
                      "dependsOn": ["user_trade_cnt_5m", "amount"],
                      "valueType": "BOOLEAN"
                    }
                  ],
                  "rules": [
                    {
                      "code": "R001",
                      "name": "黑名单设备直接拒绝",
                      "engineType": "AVIATOR",
                      "priority": 100,
                      "whenExpr": "device_in_blacklist == true",
                      "dependsOn": ["device_in_blacklist"],
                      "hitAction": "REJECT",
                      "riskScore": 100,
                      "hitReasonTemplate": "设备命中黑名单",
                      "enabled": true
                    },
                    {
                      "code": "R002",
                      "name": "大额且短时高频交易",
                      "engineType": "AVIATOR",
                      "priority": 90,
                      "whenExpr": "trade_burst_flag == true",
                      "dependsOn": ["trade_burst_flag"],
                      "hitAction": "REVIEW",
                      "riskScore": 60,
                      "hitReasonTemplate": "用户5分钟交易次数={user_trade_cnt_5m}, 当前金额={amount}",
                      "enabled": true
                    },
                    {
                      "code": "R003",
                      "name": "高风险用户多账号设备",
                      "engineType": "GROOVY",
                      "priority": 80,
                      "whenExpr": "return device_bind_user_cnt_1h >= 4 && ['M','H'].contains(user_risk_level)",
                      "dependsOn": ["device_bind_user_cnt_1h", "user_risk_level"],
                      "hitAction": "REJECT",
                      "riskScore": 80,
                      "hitReasonTemplate": "设备1小时关联用户数={device_bind_user_cnt_1h}, 用户风险等级={user_risk_level}",
                      "enabled": true
                    }
                  ],
                  "policy": {
                    "policyCode": "TRADE_RISK_POLICY",
                    "policyName": "交易风控主策略",
                    "decisionMode": "FIRST_HIT",
                    "defaultAction": "PASS",
                    "ruleOrder": ["R001", "R003", "R002"]
                  },
                  "runtimeHints": {
                    "requiredStreamFeatures": ["user_trade_cnt_5m", "user_trade_amt_sum_30m", "device_bind_user_cnt_1h"],
                    "requiredLookupFeatures": ["device_in_blacklist", "user_risk_level"],
                    "requiredDerivedFeatures": ["high_amt_flag", "trade_burst_flag"],
                    "maxRuleExecutionCount": 100,
                    "allowGroovy": true,
                    "needFullDecisionLog": true
                  }
                }
                """;
    }

    private static String demoEventsJson() {
        return """
                [
                  {
                    "eventId": "E202603070001",
                    "traceId": "T202603070001",
                    "sceneCode": "TRADE_RISK",
                    "eventType": "trade",
                    "eventTime": "2026-03-07T09:56:00Z",
                    "userId": "U1001",
                    "deviceId": "D9001",
                    "ip": "1.1.1.1",
                    "amount": 120,
                    "currency": "CNY",
                    "result": "SUCCESS",
                    "channel": "APP"
                  },
                  {
                    "eventId": "E202603070002",
                    "traceId": "T202603070002",
                    "sceneCode": "TRADE_RISK",
                    "eventType": "trade",
                    "eventTime": "2026-03-07T09:57:30Z",
                    "userId": "U2002",
                    "deviceId": "D9001",
                    "ip": "2.2.2.2",
                    "amount": 88,
                    "currency": "CNY",
                    "result": "SUCCESS",
                    "channel": "APP"
                  },
                  {
                    "eventId": "E202603070003",
                    "traceId": "T202603070003",
                    "sceneCode": "TRADE_RISK",
                    "eventType": "trade",
                    "eventTime": "2026-03-07T09:58:10Z",
                    "userId": "U3003",
                    "deviceId": "D9001",
                    "ip": "3.3.3.3",
                    "amount": 96,
                    "currency": "CNY",
                    "result": "SUCCESS",
                    "channel": "APP"
                  },
                  {
                    "eventId": "E202603070004",
                    "traceId": "T202603070004",
                    "sceneCode": "TRADE_RISK",
                    "eventType": "trade",
                    "eventTime": "2026-03-07T09:58:40Z",
                    "userId": "U4004",
                    "deviceId": "D9001",
                    "ip": "4.4.4.4",
                    "amount": 135,
                    "currency": "CNY",
                    "result": "SUCCESS",
                    "channel": "APP"
                  },
                  {
                    "eventId": "E202603070005",
                    "traceId": "T202603070005",
                    "sceneCode": "TRADE_RISK",
                    "eventType": "trade",
                    "eventTime": "2026-03-07T09:59:20Z",
                    "userId": "U1001",
                    "deviceId": "D9001",
                    "ip": "1.1.1.1",
                    "amount": 260,
                    "currency": "CNY",
                    "result": "SUCCESS",
                    "channel": "APP"
                  },
                  {
                    "eventId": "E202603070006",
                    "traceId": "T202603070006",
                    "sceneCode": "TRADE_RISK",
                    "eventType": "trade",
                    "eventTime": "2026-03-07T10:00:00Z",
                    "userId": "U1001",
                    "deviceId": "D9001",
                    "ip": "1.2.3.4",
                    "amount": 6800,
                    "currency": "CNY",
                    "result": "SUCCESS",
                    "channel": "APP"
                  }
                ]
                """;
    }

    private static <T> T readValue(String text, Class<T> clazz) {
        try {
            return OBJECT_MAPPER.readValue(text, clazz);
        } catch (Exception exception) {
            throw new IllegalStateException("read demo fixture failed", exception);
        }
    }

    private static <T> T readValue(String text, TypeReference<T> typeReference) {
        try {
            return OBJECT_MAPPER.readValue(text, typeReference);
        } catch (Exception exception) {
            throw new IllegalStateException("read demo fixture failed", exception);
        }
    }

}
