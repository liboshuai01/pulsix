package cn.liboshuai.pulsix.access.sdk.client;

import cn.liboshuai.pulsix.access.sdk.model.PulsixSdkSendRequest;
import cn.liboshuai.pulsix.framework.common.biz.risk.access.dto.AccessIngestResponseDTO;
import cn.liboshuai.pulsix.framework.common.biz.risk.access.enums.AccessAckStatusEnum;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

class NettyPulsixSdkSmokeTest {

    private static final String DEFAULT_PAYLOAD = "{\"event_id\":\"E_SDK_SMOKE_9104\",\"occur_time_ms\":1773287100000,\"req\":{\"traceId\":\"T_SDK_SMOKE_9104\"},\"uid\":\" U9004 \",\"dev_id\":\"D9004\",\"client_ip\":\"88.66.55.45\",\"pay_amt\":128800,\"trade_result\":\"ok\"}";

    @Test
    void shouldSendOneEventToRealIngestServer() throws Exception {
        Assumptions.assumeTrue(Boolean.getBoolean("pulsix.access.sdk.smoke.enabled"),
                "manual smoke, pass -Dpulsix.access.sdk.smoke.enabled=true");

        String host = systemProperty("pulsix.access.sdk.smoke.host", "127.0.0.1");
        int port = integerProperty("pulsix.access.sdk.smoke.port", 19100);
        String sourceCode = systemProperty("pulsix.access.sdk.smoke.source-code", "trade_sdk_demo");
        String sceneCode = systemProperty("pulsix.access.sdk.smoke.scene-code", "TRADE_RISK");
        String eventCode = systemProperty("pulsix.access.sdk.smoke.event-code", "TRADE_EVENT");
        String requestId = systemProperty("pulsix.access.sdk.smoke.request-id", "REQ_SDK_SMOKE_9104");
        String authorization = systemProperty("pulsix.access.sdk.smoke.authorization", "Bearer token-demo-001");
        String payload = systemProperty("pulsix.access.sdk.smoke.payload", DEFAULT_PAYLOAD);

        Map<String, String> metadata = new LinkedHashMap<>();
        metadata.put("authorization", authorization);

        try (PulsixSdkClient client = new NettyPulsixSdkClient(PulsixSdkOptions.builder()
                .host(host)
                .port(port)
                .sourceCode(sourceCode)
                .connectTimeoutMillis(integerProperty("pulsix.access.sdk.smoke.connect-timeout-millis", 3000))
                .requestTimeoutMillis(integerProperty("pulsix.access.sdk.smoke.request-timeout-millis", 5000))
                .maxBatchSize(1)
                .maxBufferSize(4)
                .batchFlushIntervalMillis(50)
                .maxRetryCount(1)
                .reconnectIntervalMillis(500)
                .build())) {
            client.start();
            AccessIngestResponseDTO response = client.sendAsync(PulsixSdkSendRequest.builder()
                    .requestId(requestId)
                    .sceneCode(sceneCode)
                    .eventCode(eventCode)
                    .payload(payload)
                    .metadata(metadata)
                    .build()).get(10, TimeUnit.SECONDS);

            assertThat(response.getStatus()).isEqualTo(AccessAckStatusEnum.ACCEPTED.getStatus());
            assertThat(response.getStandardTopicName()).isEqualTo("pulsix.event.standard");
            assertThat(client.healthCheck().getStatus()).isEqualTo("UP");
            assertThat(client.getMetricsSnapshot().getSubmittedCount()).isEqualTo(1L);
            assertThat(client.getMetricsSnapshot().getAckCount()).isEqualTo(1L);
        }
    }

    private String systemProperty(String key, String defaultValue) {
        String value = System.getProperty(key);
        return value == null || value.isBlank() ? defaultValue : value;
    }

    private int integerProperty(String key, int defaultValue) {
        String value = System.getProperty(key);
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        return Integer.parseInt(value);
    }

}
