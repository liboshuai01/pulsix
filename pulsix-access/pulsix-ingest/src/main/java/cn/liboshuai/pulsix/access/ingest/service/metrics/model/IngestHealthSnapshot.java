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
public class IngestHealthSnapshot implements Serializable {

    private String status;

    private Long timestamp;

    private String httpStatus;

    private Boolean nettyEnabled;

    private String nettyStatus;

    private Integer nettyBoundPort;

    private Integer nettyActiveConnectionCount;

    private Long totalCount;

    private Long errorCount;

}
