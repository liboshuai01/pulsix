package cn.liboshuai.pulsix.engine.flink;

import cn.liboshuai.pulsix.engine.json.EngineJson;
import cn.liboshuai.pulsix.engine.model.EngineErrorRecord;
import cn.liboshuai.pulsix.engine.model.RiskEvent;

import java.time.Instant;

final class RiskEventJsonCodec {

    private static final int MAX_PAYLOAD_LENGTH = 256;

    private RiskEventJsonCodec() {
    }

    static RiskEvent read(String text) {
        return EngineJson.read(text, RiskEvent.class);
    }

    static void validate(RiskEvent event) {
        if (event == null) {
            throw new IllegalArgumentException("risk event must not be null");
        }
        if (event.getSceneCode() == null || event.getSceneCode().isBlank()) {
            throw new IllegalArgumentException("sceneCode must not be blank");
        }
    }

    static EngineErrorRecord deserializeError(String text, Throwable throwable) {
        EngineErrorRecord record = new EngineErrorRecord();
        record.setStage("event-deserialize");
        record.setErrorMessage(buildErrorMessage(text, throwable));
        record.setOccurredAt(Instant.now());
        return record;
    }

    static EngineErrorRecord validationError(RiskEvent event, Throwable throwable) {
        return EngineErrorRecord.of("event-validate", event, null, throwable);
    }

    private static String buildErrorMessage(String text, Throwable throwable) {
        String payload = text == null ? null : text.trim();
        if (payload != null && payload.length() > MAX_PAYLOAD_LENGTH) {
            payload = payload.substring(0, MAX_PAYLOAD_LENGTH) + "...";
        }
        String baseMessage = throwable == null ? null : throwable.getMessage();
        if (payload == null || payload.isBlank()) {
            return baseMessage;
        }
        if (baseMessage == null || baseMessage.isBlank()) {
            return "payload=" + payload;
        }
        return baseMessage + ", payload=" + payload;
    }

}
