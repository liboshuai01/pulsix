package cn.liboshuai.pulsix.engine.flink;

import cn.liboshuai.pulsix.engine.model.DecisionLogRecord;
import cn.liboshuai.pulsix.engine.model.DecisionResult;
import cn.liboshuai.pulsix.engine.model.EngineErrorRecord;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class EngineKafkaKeySerializationSchemaTest {

    @Test
    void shouldUseTraceIdAsKafkaKeyForDecisionResult() {
        DecisionResult result = new DecisionResult();
        result.setTraceId("T-RESULT-001");
        result.setEventId("E-RESULT-001");

        byte[] key = new EngineKafkaKeySerializationSchema<DecisionResult>().serialize(result);

        assertArrayEquals("T-RESULT-001".getBytes(StandardCharsets.UTF_8), key);
    }

    @Test
    void shouldFallbackToEventIdWhenTraceIdMissing() {
        DecisionLogRecord record = new DecisionLogRecord();
        record.setEventId("E-LOG-001");
        record.setTraceId(" ");

        byte[] key = new EngineKafkaKeySerializationSchema<DecisionLogRecord>().serialize(record);

        assertArrayEquals("E-LOG-001".getBytes(StandardCharsets.UTF_8), key);
    }

    @Test
    void shouldReturnNullWhenTraceIdAndEventIdAreBothMissing() {
        EngineErrorRecord record = new EngineErrorRecord();

        byte[] key = new EngineKafkaKeySerializationSchema<EngineErrorRecord>().serialize(record);

        assertNull(key);
    }

}
