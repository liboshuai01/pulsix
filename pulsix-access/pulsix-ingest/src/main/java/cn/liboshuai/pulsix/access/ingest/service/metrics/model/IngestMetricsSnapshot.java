package cn.liboshuai.pulsix.access.ingest.service.metrics.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IngestMetricsSnapshot implements Serializable {

    private Long timestamp;

    private Long totalCount;

    private Long acceptedCount;

    private Long rejectedCount;

    private Long errorCount;

    private Long avgProcessTimeMillis;

    private Long avgProduceLatencyMillis;

    private Integer nettyActiveConnectionCount;

    @Builder.Default
    private Map<String, IngestSourceMetricsSnapshot> sourceMetrics = new LinkedHashMap<>();

}
