package cn.liboshuai.pulsix.access.ingest.infra.kafka;

import cn.liboshuai.pulsix.access.ingest.config.PulsixIngestProperties;
import cn.liboshuai.pulsix.access.ingest.domain.config.IngestSourceConfig;
import cn.liboshuai.pulsix.access.ingest.domain.error.IngestDlqPayload;
import cn.liboshuai.pulsix.access.ingest.domain.error.IngestErrorEvent;
import cn.liboshuai.pulsix.access.ingest.enums.IngestStageEnum;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class KafkaIngestEventProducerTest {

    private KafkaTemplate<Object, Object> kafkaTemplate;
    private KafkaIngestEventProducer producer;
    private PulsixIngestProperties properties;

    @BeforeEach
    void setUp() {
        kafkaTemplate = mock(KafkaTemplate.class);
        properties = new PulsixIngestProperties();
        properties.getKafka().setSendTimeoutMillis(100);
        properties.getKafka().setSendMaxAttempts(2);
        properties.getKafka().setSendBackoffMillis(0L);

        producer = new KafkaIngestEventProducer();
        ReflectionTestUtils.setField(producer, "kafkaTemplate", kafkaTemplate);
        ReflectionTestUtils.setField(producer, "properties", properties);
    }

    @Test
    void shouldSendStandardEventUsingSourceTopicAndSceneCodeKey() {
        Map<String, Object> standardEventJson = new LinkedHashMap<>();
        standardEventJson.put("sceneCode", "TRADE_RISK");
        standardEventJson.put("eventId", "E_1001");

        CompletableFuture<SendResult<Object, Object>> future = CompletableFuture.completedFuture(mock(SendResult.class));
        when(kafkaTemplate.send("pulsix.event.standard.custom", "TRADE_RISK", standardEventJson)).thenReturn(future);

        IngestKafkaSendResult result = producer.sendStandardEvent(IngestSourceConfig.builder()
                .sourceCode("trade_http_demo")
                .standardTopicName("pulsix.event.standard.custom")
                .build(), standardEventJson);

        assertThat(result.getTopicName()).isEqualTo("pulsix.event.standard.custom");
        assertThat(result.getMessageKey()).isEqualTo("TRADE_RISK");
        verify(kafkaTemplate).send("pulsix.event.standard.custom", "TRADE_RISK", standardEventJson);
    }

    @Test
    void shouldSendErrorEventUsingTraceIdAndFallbackDlqTopic() {
        Map<String, Object> errorPayload = new LinkedHashMap<>();
        errorPayload.put("traceId", "TRACE_2001");
        errorPayload.put("rawEventId", "RAW_2001");

        CompletableFuture<SendResult<Object, Object>> future = CompletableFuture.completedFuture(mock(SendResult.class));
        when(kafkaTemplate.send("pulsix.event.dlq", "TRACE_2001", errorPayload)).thenReturn(future);

        IngestKafkaSendResult result = producer.sendErrorEvent(IngestSourceConfig.builder()
                .sourceCode("trade_http_demo")
                .build(), errorPayload);

        assertThat(result.getTopicName()).isEqualTo("pulsix.event.dlq");
        assertThat(result.getMessageKey()).isEqualTo("TRACE_2001");
        verify(kafkaTemplate).send("pulsix.event.dlq", "TRACE_2001", errorPayload);
    }

    @Test
    void shouldSendErrorModelAsDlqPayload() {
        IngestErrorEvent errorEvent = IngestErrorEvent.builder()
                .traceId("TRACE_4001")
                .sourceCode("trade_http_demo")
                .sceneCode("TRADE_RISK")
                .eventCode("TRADE_EVENT")
                .rawEventId("RAW_4001")
                .ingestStage(IngestStageEnum.VALIDATE)
                .errorCode("REQUIRED_FIELD_MISSING")
                .errorMessage("eventTime 缺失")
                .rawPayloadJson(Map.of("event_id", "RAW_4001"))
                .standardPayloadJson(Map.of("eventId", "RAW_4001"))
                .errorTopicName("pulsix.event.dlq")
                .occurTime("2026-03-13T10:31:00")
                .build();
        IngestDlqPayload payload = errorEvent.toDlqPayload();

        CompletableFuture<SendResult<Object, Object>> future = CompletableFuture.completedFuture(mock(SendResult.class));
        when(kafkaTemplate.send("pulsix.event.dlq", "TRACE_4001", payload)).thenReturn(future);

        IngestKafkaSendResult result = producer.sendErrorEvent(errorEvent);

        assertThat(result.getTopicName()).isEqualTo("pulsix.event.dlq");
        assertThat(result.getMessageKey()).isEqualTo("TRACE_4001");
        verify(kafkaTemplate).send("pulsix.event.dlq", "TRACE_4001", payload);
    }

    @Test
    void shouldRetryErrorEventSendAndFallbackToRawEventIdKey() {
        Map<String, Object> errorPayload = new LinkedHashMap<>();
        errorPayload.put("rawEventId", "RAW_3001");

        CompletableFuture<SendResult<Object, Object>> failedFuture = new CompletableFuture<>();
        failedFuture.completeExceptionally(new ExecutionException(new RuntimeException("boom")));
        CompletableFuture<SendResult<Object, Object>> successFuture = CompletableFuture.completedFuture(mock(SendResult.class));
        when(kafkaTemplate.send("pulsix.event.dlq", "RAW_3001", errorPayload)).thenReturn(failedFuture, successFuture);

        IngestKafkaSendResult result = producer.sendErrorEvent(null, errorPayload);

        assertThat(result.getTopicName()).isEqualTo("pulsix.event.dlq");
        assertThat(result.getMessageKey()).isEqualTo("RAW_3001");
        verify(kafkaTemplate, times(2)).send("pulsix.event.dlq", "RAW_3001", errorPayload);
    }

}
