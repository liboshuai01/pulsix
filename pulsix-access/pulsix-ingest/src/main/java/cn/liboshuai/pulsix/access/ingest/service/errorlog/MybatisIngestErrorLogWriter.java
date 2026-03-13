package cn.liboshuai.pulsix.access.ingest.service.errorlog;

import cn.hutool.core.util.StrUtil;
import cn.liboshuai.pulsix.access.ingest.dal.dataobject.ingesterror.IngestErrorLogDO;
import cn.liboshuai.pulsix.access.ingest.dal.mysql.ingesterror.IngestErrorLogMapper;
import cn.liboshuai.pulsix.access.ingest.domain.error.IngestErrorEvent;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

@Service
public class MybatisIngestErrorLogWriter implements IngestErrorLogWriter {

    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
    private static final int ACTIVE_STATUS = 1;

    @Resource
    private IngestErrorLogMapper ingestErrorLogMapper;

    @Resource
    private Clock clock;

    @Override
    public void write(IngestErrorEvent errorEvent, String reprocessStatus) {
        if (errorEvent == null) {
            return;
        }
        ingestErrorLogMapper.insert(convert(errorEvent, reprocessStatus));
    }

    private IngestErrorLogDO convert(IngestErrorEvent errorEvent, String reprocessStatus) {
        IngestErrorLogDO logDO = new IngestErrorLogDO();
        logDO.setTraceId(required(errorEvent.getTraceId()));
        logDO.setSourceCode(required(errorEvent.getSourceCode()));
        logDO.setSceneCode(optional(errorEvent.getSceneCode()));
        logDO.setEventCode(optional(errorEvent.getEventCode()));
        logDO.setRawEventId(optional(errorEvent.getRawEventId()));
        logDO.setIngestStage(errorEvent.getIngestStage() == null ? "" : required(errorEvent.getIngestStage().getStage()));
        logDO.setErrorCode(required(errorEvent.getErrorCode()));
        logDO.setErrorMessage(required(errorEvent.getErrorMessage()));
        logDO.setRawPayloadJson(errorEvent.getRawPayloadJson());
        logDO.setStandardPayloadJson(errorEvent.getStandardPayloadJson());
        logDO.setErrorTopicName(optional(errorEvent.getErrorTopicName()));
        logDO.setReprocessStatus(required(defaultIfBlank(reprocessStatus, REPROCESS_STATUS_PENDING)));
        logDO.setOccurTime(resolveOccurTime(errorEvent.getOccurTime()));
        logDO.setStatus(ACTIVE_STATUS);
        return logDO;
    }

    private LocalDateTime resolveOccurTime(String occurTime) {
        String normalized = optional(occurTime);
        if (normalized == null) {
            return LocalDateTime.now(clock);
        }
        try {
            return LocalDateTime.parse(normalized, DATETIME_FORMATTER);
        } catch (DateTimeParseException ex) {
            return LocalDateTime.now(clock);
        }
    }

    private String required(String value) {
        String normalized = optional(value);
        return normalized == null ? "" : normalized;
    }

    private String optional(String value) {
        String normalized = StrUtil.trim(value);
        return StrUtil.isBlank(normalized) ? null : normalized;
    }

    private String defaultIfBlank(String value, String defaultValue) {
        return StrUtil.isBlank(value) ? defaultValue : value;
    }

}
