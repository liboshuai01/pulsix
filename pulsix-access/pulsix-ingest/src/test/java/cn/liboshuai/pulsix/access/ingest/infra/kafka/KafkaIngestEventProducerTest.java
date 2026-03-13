package cn.liboshuai.pulsix.access.ingest.infra.kafka;

import cn.liboshuai.pulsix.access.ingest.config.PulsixIngestProperties;
import cn.liboshuai.pulsix.access.ingest.domain.config.IngestSourceConfig;
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
