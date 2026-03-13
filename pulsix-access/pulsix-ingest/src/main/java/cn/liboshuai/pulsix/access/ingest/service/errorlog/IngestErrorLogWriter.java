package cn.liboshuai.pulsix.access.ingest.service.errorlog;

import cn.liboshuai.pulsix.access.ingest.domain.error.IngestErrorEvent;

public interface IngestErrorLogWriter {

    String REPROCESS_STATUS_PENDING = "PENDING";
    String REPROCESS_STATUS_RETRY_FAILED = "RETRY_FAILED";

    void write(IngestErrorEvent errorEvent, String reprocessStatus);

}
