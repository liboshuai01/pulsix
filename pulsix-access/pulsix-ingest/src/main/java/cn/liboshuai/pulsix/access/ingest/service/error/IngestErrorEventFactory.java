package cn.liboshuai.pulsix.access.ingest.service.error;

import cn.hutool.core.util.StrUtil;
import cn.liboshuai.pulsix.access.ingest.config.PulsixIngestProperties;
import cn.liboshuai.pulsix.access.ingest.domain.config.IngestSourceConfig;
import cn.liboshuai.pulsix.access.ingest.domain.error.IngestErrorEvent;
import cn.liboshuai.pulsix.access.ingest.enums.IngestStageEnum;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class IngestErrorEventFactory {

    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    @Resource
    private Clock clock;

    @Resource
    private PulsixIngestProperties properties;

    public IngestErrorEvent create(IngestStageEnum ingestStage,
                                   String errorCode,
                                   String errorMessage,
                                   String sourceCode,
                                   String sceneCode,
                                   String eventCode,
                                   String traceId,
                                   String rawEventId,
                                   Object rawPayloadJson,
                                   Object standardPayloadJson,
                                   IngestSourceConfig sourceConfig) {
        return IngestErrorEvent.builder()
                .traceId(normalize(traceId))
                .sourceCode(resolveSourceCode(sourceCode, sourceConfig))
                .sceneCode(normalize(sceneCode))
                .eventCode(normalize(eventCode))
                .rawEventId(normalize(rawEventId))
                .ingestStage(ingestStage)
                .errorCode(normalize(errorCode))
                .errorMessage(normalize(errorMessage))
                .rawPayloadJson(copyJsonValue(rawPayloadJson))
                .standardPayloadJson(copyJsonValue(standardPayloadJson))
                .errorTopicName(resolveErrorTopicName(sourceConfig))
                .occurTime(DATETIME_FORMATTER.format(LocalDateTime.now(clock)))
                .build();
    }

    private String resolveSourceCode(String sourceCode, IngestSourceConfig sourceConfig) {
        if (StrUtil.isNotBlank(sourceCode)) {
            return StrUtil.trim(sourceCode);
        }
        if (sourceConfig != null && StrUtil.isNotBlank(sourceConfig.getSourceCode())) {
            return StrUtil.trim(sourceConfig.getSourceCode());
        }
        return null;
    }

    private String resolveErrorTopicName(IngestSourceConfig sourceConfig) {
        if (sourceConfig != null && StrUtil.isNotBlank(sourceConfig.getErrorTopicName())) {
            return StrUtil.trim(sourceConfig.getErrorTopicName());
        }
        return StrUtil.trim(properties.getKafka().getDlqTopicName());
    }

    private String normalize(String value) {
        String normalized = StrUtil.trim(value);
        return StrUtil.isBlank(normalized) ? null : normalized;
    }

    @SuppressWarnings("unchecked")
    private Object copyJsonValue(Object value) {
        if (value instanceof Map<?, ?> mapValue) {
            return new LinkedHashMap<>((Map<String, Object>) mapValue);
        }
        if (value instanceof List<?> listValue) {
            return new ArrayList<>(listValue);
        }
        return value;
    }

}
