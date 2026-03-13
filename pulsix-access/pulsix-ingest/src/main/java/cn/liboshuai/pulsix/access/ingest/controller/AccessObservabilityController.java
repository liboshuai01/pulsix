package cn.liboshuai.pulsix.access.ingest.controller;

import cn.liboshuai.pulsix.access.ingest.config.PulsixIngestProperties;
import cn.liboshuai.pulsix.access.ingest.netty.NettyIngestServer;
import cn.liboshuai.pulsix.access.ingest.service.metrics.IngestMetricsService;
import cn.liboshuai.pulsix.access.ingest.service.metrics.model.IngestHealthSnapshot;
import cn.liboshuai.pulsix.access.ingest.service.metrics.model.IngestMetricsSnapshot;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AccessObservabilityController {

    private final IngestMetricsService ingestMetricsService;
    private final NettyIngestServer nettyIngestServer;
    private final PulsixIngestProperties properties;

    public AccessObservabilityController(IngestMetricsService ingestMetricsService,
                                         NettyIngestServer nettyIngestServer,
                                         PulsixIngestProperties properties) {
        this.ingestMetricsService = ingestMetricsService;
        this.nettyIngestServer = nettyIngestServer;
        this.properties = properties;
    }

    @GetMapping("/api/access/metrics/summary")
    public IngestMetricsSnapshot metricsSummary() {
        return ingestMetricsService.snapshot();
    }

    @GetMapping("/api/access/health")
    public IngestHealthSnapshot health() {
        IngestMetricsSnapshot snapshot = ingestMetricsService.snapshot();
        boolean httpEnabled = Boolean.TRUE.equals(properties.getHttp().getEnabled());
        boolean nettyEnabled = Boolean.TRUE.equals(properties.getNetty().getEnabled());
        boolean nettyRunning = nettyEnabled && nettyIngestServer.isRunning();
        String httpStatus = httpEnabled ? "UP" : "DISABLED";
        String nettyStatus = !nettyEnabled ? "DISABLED" : (nettyRunning ? "UP" : "DOWN");
        String status = resolveOverallStatus(httpEnabled, nettyEnabled, nettyRunning);
        return IngestHealthSnapshot.builder()
                .status(status)
                .timestamp(System.currentTimeMillis())
                .httpStatus(httpStatus)
                .nettyEnabled(nettyEnabled)
                .nettyStatus(nettyStatus)
                .nettyBoundPort(nettyRunning ? nettyIngestServer.getBoundPort() : -1)
                .nettyActiveConnectionCount(snapshot.getNettyActiveConnectionCount())
                .totalCount(snapshot.getTotalCount())
                .errorCount(snapshot.getErrorCount())
                .build();
    }

    private String resolveOverallStatus(boolean httpEnabled, boolean nettyEnabled, boolean nettyRunning) {
        boolean anyConfigured = httpEnabled || nettyEnabled;
        boolean anyAvailable = httpEnabled || (nettyEnabled && nettyRunning);
        if (!anyConfigured || !anyAvailable) {
            return "DOWN";
        }
        if (nettyEnabled && !nettyRunning) {
            return "DEGRADED";
        }
        return "UP";
    }

}
