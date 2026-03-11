package cn.liboshuai.pulsix.engine.flink;

import cn.liboshuai.pulsix.engine.model.DecisionLogRecord;
import cn.liboshuai.pulsix.engine.model.DecisionResult;
import cn.liboshuai.pulsix.engine.model.EngineErrorRecord;
import org.apache.flink.api.common.serialization.SerializationSchema;

import java.nio.charset.StandardCharsets;

final class EngineKafkaKeySerializationSchema<T> implements SerializationSchema<T> {

    @Override
    public byte[] serialize(T element) {
        String key = extractKey(element);
        if (key == null || key.isBlank()) {
            return null;
        }
        return key.getBytes(StandardCharsets.UTF_8);
    }

    private String extractKey(T element) {
        if (element instanceof DecisionResult result) {
            return firstNonBlank(result.getTraceId(), result.getEventId());
        }
        if (element instanceof DecisionLogRecord record) {
            return firstNonBlank(record.getTraceId(), record.getEventId());
        }
        if (element instanceof EngineErrorRecord record) {
            return firstNonBlank(record.getTraceId(), record.getEventId());
        }
        return null;
    }

    private String firstNonBlank(String preferred, String fallback) {
        if (preferred != null && !preferred.isBlank()) {
            return preferred;
        }
        if (fallback != null && !fallback.isBlank()) {
            return fallback;
        }
        return null;
    }

}
