package cn.liboshuai.pulsix.access.ingest.service.metrics;

import cn.liboshuai.pulsix.access.ingest.enums.IngestStageEnum;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class InMemoryIngestMetricsServiceTest {

    @Test
    void shouldAggregateSourceMetricsAndConnectionCount() {
        InMemoryIngestMetricsService service = new InMemoryIngestMetricsService();

        service.recordAccepted("trade_sdk_demo", 12L, 4L);
        service.recordRejected("trade_sdk_demo", IngestStageEnum.AUTH, 8L);
        service.recordAccepted("http_none_demo", 10L, 2L);
        service.recordNettyConnectionOpened();
        service.recordNettyConnectionOpened();
        service.recordNettyConnectionClosed();

        var snapshot = service.snapshot();
        assertThat(snapshot.getTotalCount()).isEqualTo(3L);
        assertThat(snapshot.getAcceptedCount()).isEqualTo(2L);
        assertThat(snapshot.getRejectedCount()).isEqualTo(1L);
        assertThat(snapshot.getErrorCount()).isEqualTo(1L);
        assertThat(snapshot.getNettyActiveConnectionCount()).isEqualTo(1);
        assertThat(snapshot.getSourceMetrics()).containsKeys("trade_sdk_demo", "http_none_demo");
        assertThat(snapshot.getSourceMetrics().get("trade_sdk_demo").getTotalCount()).isEqualTo(2L);
        assertThat(snapshot.getSourceMetrics().get("trade_sdk_demo").getErrorCount()).isEqualTo(1L);
    }

}
