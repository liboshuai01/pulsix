package cn.liboshuai.pulsix.access.ingest.domain.error;

import cn.hutool.core.util.StrUtil;
import cn.liboshuai.pulsix.access.ingest.enums.IngestStageEnum;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IngestErrorEvent {

    private String traceId;

    private String sourceCode;

    private String sceneCode;

    private String eventCode;

    private String rawEventId;

    private IngestStageEnum ingestStage;

    private String errorCode;

    private String errorMessage;

    private Object rawPayloadJson;

    private Object standardPayloadJson;

    private String errorTopicName;

    private String occurTime;

    public String resolveMessageKey() {
        String normalizedTraceId = normalize(traceId);
        if (normalizedTraceId != null) {
            return normalizedTraceId;
        }
        return normalize(rawEventId);
    }

    public IngestDlqPayload toDlqPayload() {
        return IngestDlqPayload.builder()
                .traceId(normalize(traceId))
                .rawEventId(normalize(rawEventId))
                .sourceCode(normalize(sourceCode))
                .sceneCode(normalize(sceneCode))
                .eventCode(normalize(eventCode))
                .ingestStage(ingestStage == null ? null : ingestStage.getStage())
                .errorCode(normalize(errorCode))
                .errorMessage(normalize(errorMessage))
                .rawPayload(copyJsonValue(rawPayloadJson))
                .standardPayload(copyJsonValue(standardPayloadJson))
                .occurTime(normalize(occurTime))
                .build();
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
