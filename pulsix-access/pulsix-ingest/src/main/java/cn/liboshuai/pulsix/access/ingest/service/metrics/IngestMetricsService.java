package cn.liboshuai.pulsix.access.ingest.service.metrics;

import cn.liboshuai.pulsix.access.ingest.enums.IngestStageEnum;
import cn.liboshuai.pulsix.access.ingest.service.metrics.model.IngestMetricsSnapshot;

public interface IngestMetricsService {

    void recordAccepted(String sourceCode, long processTimeMillis, long produceLatencyMillis);

    void recordRejected(String sourceCode, IngestStageEnum stage, long processTimeMillis);

    void recordNettyConnectionOpened();

    void recordNettyConnectionClosed();

    IngestMetricsSnapshot snapshot();

}
