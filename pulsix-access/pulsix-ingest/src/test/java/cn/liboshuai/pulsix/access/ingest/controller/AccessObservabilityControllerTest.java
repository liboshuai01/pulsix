package cn.liboshuai.pulsix.access.ingest.controller;

import cn.liboshuai.pulsix.access.ingest.config.PulsixIngestProperties;
import cn.liboshuai.pulsix.access.ingest.netty.NettyIngestServer;
import cn.liboshuai.pulsix.access.ingest.service.metrics.IngestMetricsService;
import cn.liboshuai.pulsix.access.ingest.service.metrics.model.IngestMetricsSnapshot;
import cn.liboshuai.pulsix.access.ingest.service.metrics.model.IngestSourceMetricsSnapshot;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AccessObservabilityController.class)
@Import(PulsixIngestProperties.class)
class AccessObservabilityControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PulsixIngestProperties properties;

    @MockBean
    private IngestMetricsService ingestMetricsService;

    @MockBean
    private NettyIngestServer nettyIngestServer;

    @Test
    void shouldReturnMetricsSummary() throws Exception {
        when(ingestMetricsService.snapshot()).thenReturn(IngestMetricsSnapshot.builder()
                .timestamp(1_763_333_100_000L)
                .totalCount(12L)
                .acceptedCount(10L)
                .rejectedCount(2L)
                .errorCount(2L)
                .avgProcessTimeMillis(9L)
                .avgProduceLatencyMillis(3L)
                .nettyActiveConnectionCount(4)
                .sourceMetrics(Map.of(
                        "trade_sdk_demo", IngestSourceMetricsSnapshot.builder()
                                .sourceCode("trade_sdk_demo")
                                .totalCount(7L)
                                .acceptedCount(6L)
                                .rejectedCount(1L)
                                .errorCount(1L)
                                .avgProcessTimeMillis(8L)
                                .avgProduceLatencyMillis(2L)
                                .lastRequestAtMillis(1_763_333_100_000L)
                                .build()
                ))
                .build());

        mockMvc.perform(get("/api/access/metrics/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCount").value(12))
                .andExpect(jsonPath("$.acceptedCount").value(10))
                .andExpect(jsonPath("$.nettyActiveConnectionCount").value(4))
                .andExpect(jsonPath("$.sourceMetrics.trade_sdk_demo.acceptedCount").value(6));
    }

    @Test
    void shouldReturnDegradedHealthWhenNettyEnabledButStopped() throws Exception {
        properties.getHttp().setEnabled(true);
        properties.getNetty().setEnabled(true);
        when(ingestMetricsService.snapshot()).thenReturn(IngestMetricsSnapshot.builder()
                .totalCount(5L)
                .errorCount(1L)
                .nettyActiveConnectionCount(0)
                .build());
        when(nettyIngestServer.isRunning()).thenReturn(false);

        mockMvc.perform(get("/api/access/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DEGRADED"))
                .andExpect(jsonPath("$.httpStatus").value("UP"))
                .andExpect(jsonPath("$.nettyStatus").value("DOWN"))
                .andExpect(jsonPath("$.totalCount").value(5));
    }

    @Test
    void shouldReturnUpHealthWhenHttpDisabledButNettyRunning() throws Exception {
        properties.getHttp().setEnabled(false);
        properties.getNetty().setEnabled(true);
        when(ingestMetricsService.snapshot()).thenReturn(IngestMetricsSnapshot.builder()
                .totalCount(2L)
                .errorCount(0L)
                .nettyActiveConnectionCount(1)
                .build());
        when(nettyIngestServer.isRunning()).thenReturn(true);
        when(nettyIngestServer.getBoundPort()).thenReturn(19100);

        mockMvc.perform(get("/api/access/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.httpStatus").value("DISABLED"))
                .andExpect(jsonPath("$.nettyStatus").value("UP"))
                .andExpect(jsonPath("$.nettyBoundPort").value(19100));
    }

}
