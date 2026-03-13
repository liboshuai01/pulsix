package cn.liboshuai.pulsix.access.ingest.infra.kafka;

import cn.liboshuai.pulsix.access.ingest.domain.config.IngestSourceConfig;

import java.util.Map;

public interface IngestEventProducer {

    IngestKafkaSendResult sendStandardEvent(IngestSourceConfig sourceConfig, Map<String, Object> standardEventJson);

    IngestKafkaSendResult sendErrorEvent(IngestSourceConfig sourceConfig, Map<String, Object> errorPayload);

}
