package cn.liboshuai.pulsix.engine.flink;

import cn.liboshuai.pulsix.engine.demo.DemoFixtures;
import cn.liboshuai.pulsix.engine.json.EngineJson;
import cn.liboshuai.pulsix.engine.model.ActionType;
import cn.liboshuai.pulsix.engine.model.DecisionResult;
import cn.liboshuai.pulsix.engine.model.EngineErrorCodes;
import cn.liboshuai.pulsix.engine.model.EngineErrorRecord;
import cn.liboshuai.pulsix.engine.model.EngineErrorTypes;
import cn.liboshuai.pulsix.engine.model.RiskEvent;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RiskEventJsonCodecTest {

    @Test
    void shouldReadAndValidateFixtureRiskEvent() {
        RiskEvent expected = DemoFixtures.blacklistedEvent();

        RiskEvent actual = RiskEventJsonCodec.read(EngineJson.write(expected));
        RiskEventJsonCodec.validate(actual);

        assertEquals(expected.getEventId(), actual.getEventId());
        assertEquals(expected.getTraceId(), actual.getTraceId());
        assertEquals(expected.getSceneCode(), actual.getSceneCode());
    }

    @Test
    void shouldReadRiskEventWithLocalDateTimeString() {
        String payload = """
                {
                  "eventId": "E_RAW_9103",
                  "traceId": "T_RAW_9103",
                  "sceneCode": "TRADE_RISK",
                  "eventType": "trade",
                  "eventTime": "2026-03-12T11:45:00",
                  "userId": "U9003",
                  "deviceId": "D9003",
                  "ip": "88.66.55.44",
                  "amount": 2568,
                  "result": "SUCCESS"
                }
                """;

        RiskEvent event = RiskEventJsonCodec.read(payload);

        assertEquals(Instant.parse("2026-03-12T03:45:00Z"), event.getEventTime());
        assertEquals("TRADE_RISK", event.getSceneCode());
    }

    @Test
    void shouldRejectRiskEventWithoutSceneCode() {
        RiskEvent event = DemoFixtures.blacklistedEvent();
        event.setSceneCode(" ");

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> RiskEventJsonCodec.validate(event));

        assertEquals("sceneCode must not be blank", exception.getMessage());
    }

    @Test
    void shouldCreateDeserializeErrorWithPayloadSnippet() {
        EngineErrorRecord record = RiskEventJsonCodec.deserializeError("{bad-json}",
                new IllegalStateException("read json failed"));

        assertEquals("event-deserialize", record.getStage());
        assertEquals(EngineErrorTypes.INPUT, record.getErrorType());
        assertEquals(EngineErrorCodes.EVENT_DESERIALIZE_FAILED, record.getErrorCode());
        assertTrue(record.getErrorMessage().contains("read json failed"));
        assertTrue(record.getErrorMessage().contains("payload={bad-json}"));
        assertNotNull(record.getOccurredAt());
    }

    @Test
    void shouldSerializeDecisionResultAsJsonBytes() {
        DecisionResult result = new DecisionResult();
        result.setEventId("E202603110001");
        result.setTraceId("T202603110001");
        result.setSceneCode("TRADE_RISK");
        result.setVersion(12);
        result.setFinalAction(ActionType.PASS);
        result.setFinalScore(88);
        result.setLatencyMs(15L);
        result.setTraceLogs(java.util.List.of("compiled", "executed"));
        result.setFeatureSnapshot(java.util.Map.of("device_blacklisted", "false"));

        EngineJsonSerializationSchema<DecisionResult> schema = new EngineJsonSerializationSchema<>();
        DecisionResult actual = EngineJson.read(new String(schema.serialize(result), StandardCharsets.UTF_8),
                DecisionResult.class);

        assertEquals(result.getEventId(), actual.getEventId());
        assertEquals(result.getFinalAction(), actual.getFinalAction());
        assertEquals(result.getFeatureSnapshot(), actual.getFeatureSnapshot());
    }

    @Test
    void shouldCreateValidationErrorFromEvent() {
        RiskEvent event = DemoFixtures.blacklistedEvent();
        EngineErrorRecord record = RiskEventJsonCodec.validationError(event,
                new IllegalArgumentException("sceneCode must not be blank"));
        record.setOccurredAt(Instant.now());

        assertEquals("event-validate", record.getStage());
        assertEquals(EngineErrorTypes.INPUT, record.getErrorType());
        assertEquals(EngineErrorCodes.EVENT_VALIDATION_FAILED, record.getErrorCode());
        assertEquals(event.getEventId(), record.getEventId());
        assertEquals(event.getTraceId(), record.getTraceId());
    }

}
