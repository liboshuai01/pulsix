package cn.liboshuai.pulsix.access.ingest.service.metrics.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IngestSourceMetricsSnapshot implements Serializable {

    private String sourceCode;

    private Long totalCount;

    private Long acceptedCount;

    private Long rejectedCount;

    private Long errorCount;

    private Long avgProcessTimeMillis;

    private Long avgProduceLatencyMillis;

    private Long lastRequestAtMillis;

}
