package cn.liboshuai.pulsix.access.ingest.infra.kafka;

import cn.hutool.core.util.StrUtil;
import cn.liboshuai.pulsix.access.ingest.config.PulsixIngestProperties;
import cn.liboshuai.pulsix.access.ingest.domain.config.IngestSourceConfig;
import cn.liboshuai.pulsix.access.ingest.domain.error.IngestDlqPayload;
import cn.liboshuai.pulsix.access.ingest.domain.error.IngestErrorEvent;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.retry.support.RetryTemplateBuilder;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static cn.liboshuai.pulsix.access.ingest.enums.ErrorCodeConstants.INGEST_KAFKA_DISABLED;
import static cn.liboshuai.pulsix.access.ingest.enums.ErrorCodeConstants.INGEST_KAFKA_ERROR_SEND_FAILED;
import static cn.liboshuai.pulsix.access.ingest.enums.ErrorCodeConstants.INGEST_KAFKA_SEND_FAILED;
import static cn.liboshuai.pulsix.framework.common.exception.util.ServiceExceptionUtil.exception;

@Service
@Slf4j
public class KafkaIngestEventProducer implements IngestEventProducer {

    @Resource
    private KafkaTemplate<Object, Object> kafkaTemplate;

    @Resource
    private PulsixIngestProperties properties;

    @Override
    public IngestKafkaSendResult sendStandardEvent(IngestSourceConfig sourceConfig, Map<String, Object> standardEventJson) {
        String topicName = resolveStandardTopicName(sourceConfig);
        String messageKey = normalizeKey(readStringValue(standardEventJson, "sceneCode"));
        return send(topicName, messageKey, standardEventJson, true);
    }

    @Override
    public IngestKafkaSendResult sendErrorEvent(IngestSourceConfig sourceConfig, Map<String, Object> errorPayload) {
        String topicName = resolveErrorTopicName(sourceConfig);
        String messageKey = resolveErrorKey(errorPayload);
        return send(topicName, messageKey, errorPayload, false);
    }

    @Override
    public IngestKafkaSendResult sendErrorEvent(IngestErrorEvent errorEvent) {
        IngestDlqPayload payload = errorEvent.toDlqPayload();
        String topicName = normalizeKey(errorEvent.getErrorTopicName());
        if (topicName == null) {
            topicName = properties.getKafka().getDlqTopicName();
        }
        return send(topicName, errorEvent.resolveMessageKey(), payload, false);
    }

    private IngestKafkaSendResult send(String topicName, String messageKey, Object payload, boolean standardEvent) {
        ensureKafkaEnabled(topicName, messageKey, standardEvent);
        try {
            return buildRetryTemplate().execute(context -> doSend(topicName, messageKey, payload));
        } catch (Exception ex) {
            if (containsInterruptedException(ex)) {
                Thread.currentThread().interrupt();
            }
            log.error("[send][发送 Kafka 失败 topic={}, key={}, standardEvent={}]", topicName, messageKey, standardEvent, ex);
            throw standardEvent ? exception(INGEST_KAFKA_SEND_FAILED, topicName)
                    : exception(INGEST_KAFKA_ERROR_SEND_FAILED, topicName);
        }
    }

    private void ensureKafkaEnabled(String topicName, String messageKey, boolean standardEvent) {
        if (Boolean.TRUE.equals(properties.getKafka().getEnabled())) {
            return;
        }
        log.warn("[send][Kafka 开关已关闭，拒绝发送 topic={}, key={}, standardEvent={}]", topicName, messageKey, standardEvent);
        throw exception(INGEST_KAFKA_DISABLED);
    }

    private IngestKafkaSendResult doSend(String topicName, String messageKey, Object payload) throws Exception {
        Object messagePayload = payload == null ? Collections.emptyMap() : payload;
        CompletableFuture<SendResult<Object, Object>> future = kafkaTemplate.send(topicName, messageKey, messagePayload);
        future.get(properties.getKafka().getSendTimeoutMillis(), TimeUnit.MILLISECONDS);
        return IngestKafkaSendResult.builder()
                .topicName(topicName)
                .messageKey(messageKey)
                .build();
    }

    private RetryTemplate buildRetryTemplate() {
        RetryTemplateBuilder builder = new RetryTemplateBuilder()
                .maxAttempts(properties.getKafka().getSendMaxAttempts())
                .retryOn(Exception.class);
        Long sendBackoffMillis = properties.getKafka().getSendBackoffMillis();
        if (sendBackoffMillis != null && sendBackoffMillis > 0) {
            builder.fixedBackoff(sendBackoffMillis);
        } else {
            builder.noBackoff();
        }
        return builder.build();
    }

    private String resolveStandardTopicName(IngestSourceConfig sourceConfig) {
        if (sourceConfig != null && StrUtil.isNotBlank(sourceConfig.getStandardTopicName())) {
            return sourceConfig.getStandardTopicName();
        }
        return properties.getKafka().getStandardTopicName();
    }

    private String resolveErrorTopicName(IngestSourceConfig sourceConfig) {
        if (sourceConfig != null && StrUtil.isNotBlank(sourceConfig.getErrorTopicName())) {
            return sourceConfig.getErrorTopicName();
        }
        return properties.getKafka().getDlqTopicName();
    }

    private String resolveErrorKey(Map<String, Object> errorPayload) {
        String traceId = readStringValue(errorPayload, "traceId");
        if (StrUtil.isNotBlank(traceId)) {
            return traceId;
        }
        return normalizeKey(readStringValue(errorPayload, "rawEventId"));
    }

    private String readStringValue(Map<String, Object> payload, String key) {
        if (payload == null) {
            return null;
        }
        Object value = payload.get(key);
        return value == null ? null : String.valueOf(value);
    }

    private String normalizeKey(String value) {
        String normalized = StrUtil.trim(value);
        return StrUtil.isBlank(normalized) ? null : normalized;
    }

    private boolean containsInterruptedException(Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            if (current instanceof InterruptedException) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

}
