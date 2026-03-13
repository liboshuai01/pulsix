package cn.liboshuai.pulsix.access.ingest.service;

import cn.hutool.core.util.StrUtil;
import cn.liboshuai.pulsix.access.ingest.domain.config.IngestRuntimeConfig;
import cn.liboshuai.pulsix.access.ingest.domain.error.IngestErrorEvent;
import cn.liboshuai.pulsix.access.ingest.enums.IngestStageEnum;
import cn.liboshuai.pulsix.access.ingest.infra.kafka.IngestEventProducer;
import cn.liboshuai.pulsix.access.ingest.infra.kafka.IngestKafkaSendResult;
import cn.liboshuai.pulsix.access.ingest.service.auth.IngestAuthException;
import cn.liboshuai.pulsix.access.ingest.service.auth.IngestAuthService;
import cn.liboshuai.pulsix.access.ingest.service.config.IngestDesignConfigService;
import cn.liboshuai.pulsix.access.ingest.service.error.IngestErrorEventFactory;
import cn.liboshuai.pulsix.access.ingest.service.errorlog.IngestErrorLogWriter;
import cn.liboshuai.pulsix.access.ingest.service.normalize.StandardEventNormalizationService;
import cn.liboshuai.pulsix.framework.common.biz.risk.access.dto.AccessIngestRequestDTO;
import cn.liboshuai.pulsix.framework.common.biz.risk.access.dto.AccessIngestResponseDTO;
import cn.liboshuai.pulsix.framework.common.biz.risk.access.enums.AccessAckStatusEnum;
import cn.liboshuai.pulsix.framework.common.exception.ServiceException;
import cn.liboshuai.pulsix.framework.common.exception.enums.GlobalErrorCodeConstants;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import static cn.liboshuai.pulsix.access.ingest.enums.ErrorCodeConstants.INGEST_PAYLOAD_INVALID;
import static cn.liboshuai.pulsix.access.ingest.enums.ErrorCodeConstants.INGEST_REQUEST_INVALID;
import static cn.liboshuai.pulsix.access.ingest.enums.ErrorCodeConstants.INGEST_REQUIRED_FIELDS_MISSING;

@Service
@Slf4j
public class DefaultIngestPipelineService implements IngestPipelineService {

    private static final TypeReference<LinkedHashMap<String, Object>> MAP_TYPE = new TypeReference<>() {
    };

    @Resource
    private IngestDesignConfigService configService;

    @Resource
    private IngestAuthService authService;

    @Resource
    private StandardEventNormalizationService normalizationService;

    @Resource
    private IngestEventProducer eventProducer;

    @Resource
    private IngestErrorEventFactory errorEventFactory;

    @Resource
    private IngestErrorLogWriter errorLogWriter;

    @Resource
    private ObjectMapper objectMapper;

    @Override
    public AccessIngestResponseDTO ingest(AccessIngestRequestDTO request) {
        long startTime = System.currentTimeMillis();
        String requestId = ensureRequestId(request);
        String sourceCode = normalize(request.getSourceCode());
        String sceneCode = normalize(getMetadata(request, "sceneCode"));
        String eventCode = normalize(getMetadata(request, "eventCode"));
        Object rawPayloadForError = request.getPayload();
        Map<String, Object> rawEventJson = null;
        Map<String, Object> standardEventJson = null;
        IngestRuntimeConfig runtimeConfig;

        if (StrUtil.isBlank(sourceCode) || StrUtil.isBlank(sceneCode) || StrUtil.isBlank(eventCode)) {
            return reject(requestId, null, null, null, IngestStageEnum.PARSE,
                    "REQUEST_ROUTE_MISSING", INGEST_REQUEST_INVALID.getCode(),
                    "sourceCode、sceneCode、eventCode 不能为空",
                    sourceCode, sceneCode, eventCode, rawPayloadForError, null, null, startTime);
        }

        try {
            runtimeConfig = configService.getConfig(sourceCode, sceneCode, eventCode);
        } catch (ServiceException ex) {
            return reject(requestId, null, null, null, IngestStageEnum.PARSE,
                    "CONFIG_LOAD_FAILED", ex.getCode(), ex.getMessage(),
                    sourceCode, sceneCode, eventCode, rawPayloadForError, null, null, startTime);
        }

        try {
            authService.authenticate(request, runtimeConfig);
        } catch (IngestAuthException ex) {
            return reject(requestId, null, null, null, IngestStageEnum.AUTH,
                    ex.getErrorCode(), ex.getCode(), ex.getMessage(),
                    sourceCode, sceneCode, eventCode, rawPayloadForError, null, runtimeConfig, startTime);
        }

        try {
            rawEventJson = objectMapper.readValue(request.getPayload(), MAP_TYPE);
            rawPayloadForError = rawEventJson;
        } catch (Exception ex) {
            return reject(requestId, null, null, null, IngestStageEnum.PARSE,
                    "PAYLOAD_JSON_INVALID", INGEST_PAYLOAD_INVALID.getCode(),
                    "请求体不是合法 JSON 对象", sourceCode, sceneCode, eventCode,
                    rawPayloadForError, null, runtimeConfig, startTime);
        }

        String traceId = extractTraceId(rawEventJson);
        String rawEventId = extractRawEventId(rawEventJson);

        try {
            var normalizeResult = normalizationService.normalize(sourceCode, sceneCode, eventCode, rawEventJson);
            standardEventJson = normalizeResult.getStandardEventJson();
            List<String> missingRequiredFields = normalizeResult.getMissingRequiredFields();
            if (!missingRequiredFields.isEmpty()) {
                return reject(requestId, traceId, rawEventId, null, IngestStageEnum.VALIDATE,
                        "REQUIRED_FIELD_MISSING", INGEST_REQUIRED_FIELDS_MISSING.getCode(),
                        "标准事件必填字段缺失: " + String.join(",", missingRequiredFields),
                        sourceCode, sceneCode, eventCode, rawEventJson, standardEventJson, runtimeConfig, startTime);
            }
        } catch (ServiceException ex) {
            return reject(requestId, traceId, rawEventId, null, IngestStageEnum.NORMALIZE,
                    "NORMALIZE_FAILED", ex.getCode(), ex.getMessage(),
                    sourceCode, sceneCode, eventCode, rawEventJson, standardEventJson, runtimeConfig, startTime);
        }

        try {
            IngestKafkaSendResult sendResult = eventProducer.sendStandardEvent(runtimeConfig.getSource(), standardEventJson);
            return AccessIngestResponseDTO.builder()
                    .requestId(requestId)
                    .traceId(readStringValue(standardEventJson, "traceId", traceId))
                    .eventId(readStringValue(standardEventJson, "eventId", rawEventId))
                    .status(AccessAckStatusEnum.ACCEPTED.getStatus())
                    .code(GlobalErrorCodeConstants.SUCCESS.getCode())
                    .message("accepted")
                    .standardTopicName(sendResult.getTopicName())
                    .processTimeMillis(System.currentTimeMillis() - startTime)
                    .build();
        } catch (ServiceException ex) {
            return reject(requestId, readStringValue(standardEventJson, "traceId", traceId),
                    readStringValue(standardEventJson, "eventId", rawEventId), null,
                    IngestStageEnum.PRODUCE, "STANDARD_TOPIC_SEND_FAILED", ex.getCode(), ex.getMessage(),
                    sourceCode, sceneCode, eventCode, rawEventJson, standardEventJson, runtimeConfig, startTime);
        }
    }

    private AccessIngestResponseDTO reject(String requestId,
                                           String traceId,
                                           String rawEventId,
                                           String standardTopicName,
                                           IngestStageEnum stage,
                                           String errorCode,
                                           Integer code,
                                           String message,
                                           String sourceCode,
                                           String sceneCode,
                                           String eventCode,
                                           Object rawPayload,
                                           Object standardPayload,
                                           IngestRuntimeConfig runtimeConfig,
                                           long startTime) {
        IngestErrorEvent errorEvent = errorEventFactory.create(stage, errorCode, message,
                sourceCode, sceneCode, eventCode, traceId, rawEventId, rawPayload, standardPayload,
                runtimeConfig == null ? null : runtimeConfig.getSource());
        String reprocessStatus = IngestErrorLogWriter.REPROCESS_STATUS_PENDING;
        try {
            eventProducer.sendErrorEvent(errorEvent);
        } catch (Exception ex) {
            reprocessStatus = IngestErrorLogWriter.REPROCESS_STATUS_RETRY_FAILED;
            log.warn("[reject][发送 DLQ 失败 requestId={}, stage={}, errorCode={}]", requestId, stage, errorCode, ex);
        }
        try {
            errorLogWriter.write(errorEvent, reprocessStatus);
        } catch (Exception ex) {
            log.warn("[reject][写入 ingest_error_log 失败 requestId={}, stage={}, errorCode={}]", requestId, stage, errorCode, ex);
        }
        return AccessIngestResponseDTO.builder()
                .requestId(requestId)
                .traceId(traceId)
                .eventId(rawEventId)
                .status(AccessAckStatusEnum.REJECTED.getStatus())
                .code(code)
                .message(message)
                .standardTopicName(standardTopicName)
                .processTimeMillis(System.currentTimeMillis() - startTime)
                .build();
    }

    private String ensureRequestId(AccessIngestRequestDTO request) {
        String requestId = normalize(request.getRequestId());
        if (requestId == null) {
            requestId = UUID.randomUUID().toString();
            request.setRequestId(requestId);
        }
        return requestId;
    }

    private String getMetadata(AccessIngestRequestDTO request, String key) {
        if (request.getMetadata() == null) {
            return null;
        }
        String exactValue = request.getMetadata().get(key);
        if (StrUtil.isNotBlank(exactValue)) {
            return exactValue;
        }
        return request.getMetadata().get(key.toLowerCase(Locale.ROOT));
    }

    private String extractTraceId(Map<String, Object> rawEventJson) {
        return firstNonBlank(
                readString(rawEventJson, "traceId"),
                readString(rawEventJson, "trace_id"),
                readNestedString(rawEventJson, "req", "traceId"),
                readNestedString(rawEventJson, "req", "trace_id")
        );
    }

    private String extractRawEventId(Map<String, Object> rawEventJson) {
        return firstNonBlank(
                readString(rawEventJson, "eventId"),
                readString(rawEventJson, "event_id"),
                readString(rawEventJson, "rawEventId"),
                readNestedString(rawEventJson, "biz", "tradeId")
        );
    }

    @SuppressWarnings("unchecked")
    private String readNestedString(Map<String, Object> rawEventJson, String parentKey, String childKey) {
        if (rawEventJson == null) {
            return null;
        }
        Object parent = rawEventJson.get(parentKey);
        if (!(parent instanceof Map<?, ?> mapValue)) {
            return null;
        }
        Object value = ((Map<String, Object>) mapValue).get(childKey);
        return value == null ? null : String.valueOf(value);
    }

    private String readString(Map<String, Object> rawEventJson, String key) {
        if (rawEventJson == null) {
            return null;
        }
        Object value = rawEventJson.get(key);
        return value == null ? null : String.valueOf(value);
    }

    private String readStringValue(Map<String, Object> valueMap, String key, String fallback) {
        String value = readString(valueMap, key);
        return StrUtil.isBlank(value) ? fallback : value;
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            String normalized = normalize(value);
            if (normalized != null) {
                return normalized;
            }
        }
        return null;
    }

    private String normalize(String value) {
        String normalized = StrUtil.trim(value);
        return StrUtil.isBlank(normalized) ? null : normalized;
    }

}
