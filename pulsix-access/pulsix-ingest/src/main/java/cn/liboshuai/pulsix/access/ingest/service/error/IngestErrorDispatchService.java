package cn.liboshuai.pulsix.access.ingest.service.error;

import cn.liboshuai.pulsix.access.ingest.domain.config.IngestSourceConfig;
import cn.liboshuai.pulsix.access.ingest.domain.error.IngestErrorEvent;
import cn.liboshuai.pulsix.access.ingest.enums.IngestStageEnum;
import cn.liboshuai.pulsix.access.ingest.infra.kafka.IngestEventProducer;
import cn.liboshuai.pulsix.access.ingest.service.errorlog.IngestErrorLogWriter;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class IngestErrorDispatchService {

    @Resource
    private IngestErrorEventFactory errorEventFactory;

    @Resource
    private IngestEventProducer eventProducer;

    @Resource
    private IngestErrorLogWriter errorLogWriter;

    public IngestErrorEvent dispatch(IngestStageEnum stage,
                                     String errorCode,
                                     String errorMessage,
                                     String sourceCode,
                                     String sceneCode,
                                     String eventCode,
                                     String traceId,
                                     String rawEventId,
                                     Object rawPayload,
                                     Object standardPayload,
                                     IngestSourceConfig sourceConfig) {
        IngestErrorEvent errorEvent = errorEventFactory.create(stage, errorCode, errorMessage,
                sourceCode, sceneCode, eventCode, traceId, rawEventId, rawPayload, standardPayload, sourceConfig);
        String reprocessStatus = IngestErrorLogWriter.REPROCESS_STATUS_PENDING;
        try {
            eventProducer.sendErrorEvent(errorEvent);
        } catch (Exception ex) {
            reprocessStatus = IngestErrorLogWriter.REPROCESS_STATUS_RETRY_FAILED;
            log.warn("[dispatch][发送 DLQ 失败 stage={}, errorCode={}]", stage, errorCode, ex);
        }
        try {
            errorLogWriter.write(errorEvent, reprocessStatus);
        } catch (Exception ex) {
            log.warn("[dispatch][写入 ingest_error_log 失败 stage={}, errorCode={}]", stage, errorCode, ex);
        }
        return errorEvent;
    }

}
