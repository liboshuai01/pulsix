package cn.liboshuai.pulsix.access.ingest.service.error;

import cn.liboshuai.pulsix.access.ingest.config.PulsixIngestProperties;
import cn.liboshuai.pulsix.access.ingest.domain.config.IngestSourceConfig;
import cn.liboshuai.pulsix.access.ingest.domain.error.IngestDlqPayload;
import cn.liboshuai.pulsix.access.ingest.domain.error.IngestErrorEvent;
import cn.liboshuai.pulsix.access.ingest.enums.IngestStageEnum;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class IngestErrorEventFactoryTest {

    private IngestErrorEventFactory factory;

    @BeforeEach
    void setUp() {
        PulsixIngestProperties properties = new PulsixIngestProperties();
        properties.getKafka().setDlqTopicName("pulsix.event.dlq");

        factory = new IngestErrorEventFactory();
        ReflectionTestUtils.setField(factory, "clock", Clock.fixed(Instant.parse("2026-03-13T02:31:00Z"), ZoneId.of("Asia/Shanghai")));
        ReflectionTestUtils.setField(factory, "properties", properties);
    }

    @Test
    void shouldCreateValidateErrorEventAndConvertToDlqPayload() {
        Map<String, Object> rawPayload = new LinkedHashMap<>();
        rawPayload.put("event_id", "raw_trade_bad_8101");
        rawPayload.put("uid", "U8101");
        Map<String, Object> standardPayload = new LinkedHashMap<>();
        standardPayload.put("eventId", "raw_trade_bad_8101");
        standardPayload.put("sceneCode", "TRADE_RISK");

        IngestErrorEvent errorEvent = factory.create(IngestStageEnum.VALIDATE,
                "REQUIRED_FIELD_MISSING",
                "eventTime 缺失，且接入层无法从 occur_time_ms 中解析出合法时间",
                "trade_http_demo",
                "TRADE_RISK",
                "TRADE_EVENT",
                "TRACE-S18-8101",
                "raw_trade_bad_8101",
                rawPayload,
                standardPayload,
                IngestSourceConfig.builder()
                        .sourceCode("trade_http_demo")
                        .errorTopicName("pulsix.event.dlq")
                        .build());

        rawPayload.put("uid", "CHANGED");
        standardPayload.put("sceneCode", "CHANGED");

        assertThat(errorEvent.getSourceCode()).isEqualTo("trade_http_demo");
        assertThat(errorEvent.getIngestStage()).isEqualTo(IngestStageEnum.VALIDATE);
        assertThat(errorEvent.getErrorTopicName()).isEqualTo("pulsix.event.dlq");
        assertThat(errorEvent.getOccurTime()).isEqualTo("2026-03-13T10:31:00");
        assertThat(((Map<?, ?>) errorEvent.getRawPayloadJson()).get("uid")).isEqualTo("U8101");
        assertThat(((Map<?, ?>) errorEvent.getStandardPayloadJson()).get("sceneCode")).isEqualTo("TRADE_RISK");
        assertThat(errorEvent.resolveMessageKey()).isEqualTo("TRACE-S18-8101");

        IngestDlqPayload dlqPayload = errorEvent.toDlqPayload();

        assertThat(dlqPayload.getTraceId()).isEqualTo("TRACE-S18-8101");
        assertThat(dlqPayload.getRawEventId()).isEqualTo("raw_trade_bad_8101");
        assertThat(dlqPayload.getSourceCode()).isEqualTo("trade_http_demo");
        assertThat(dlqPayload.getSceneCode()).isEqualTo("TRADE_RISK");
        assertThat(dlqPayload.getEventCode()).isEqualTo("TRADE_EVENT");
        assertThat(dlqPayload.getIngestStage()).isEqualTo("VALIDATE");
        assertThat(dlqPayload.getOccurTime()).isEqualTo("2026-03-13T10:31:00");
    }

    @Test
    void shouldFallbackToSourceConfigAndDefaultDlqTopic() {
        IngestErrorEvent errorEvent = factory.create(IngestStageEnum.AUTH,
                "AUTH_SIGN_INVALID",
                "签名校验失败，拒绝写入标准事件 Topic",
                null,
                null,
                null,
                null,
                "raw_trade_bad_8103",
                "RAW_TEXT_BAD_JSON",
                null,
                IngestSourceConfig.builder().sourceCode("trade_http_demo").build());

        assertThat(errorEvent.getSourceCode()).isEqualTo("trade_http_demo");
        assertThat(errorEvent.getErrorTopicName()).isEqualTo("pulsix.event.dlq");
        assertThat(errorEvent.resolveMessageKey()).isEqualTo("raw_trade_bad_8103");
        assertThat(errorEvent.toDlqPayload().getStandardPayload()).isNull();
    }

}
