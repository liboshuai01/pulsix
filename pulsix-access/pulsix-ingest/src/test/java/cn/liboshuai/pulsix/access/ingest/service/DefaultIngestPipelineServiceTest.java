package cn.liboshuai.pulsix.access.ingest.service;

import cn.liboshuai.pulsix.access.ingest.domain.config.IngestRuntimeConfig;
import cn.liboshuai.pulsix.access.ingest.domain.config.IngestSourceConfig;
import cn.liboshuai.pulsix.access.ingest.domain.error.IngestErrorEvent;
import cn.liboshuai.pulsix.access.ingest.infra.kafka.IngestEventProducer;
import cn.liboshuai.pulsix.access.ingest.infra.kafka.IngestKafkaSendResult;
import cn.liboshuai.pulsix.access.ingest.service.auth.IngestAuthService;
import cn.liboshuai.pulsix.access.ingest.service.config.IngestDesignConfigService;
import cn.liboshuai.pulsix.access.ingest.service.error.IngestErrorEventFactory;
import cn.liboshuai.pulsix.access.ingest.service.normalize.StandardEventNormalizationService;
import cn.liboshuai.pulsix.framework.common.biz.risk.access.dto.AccessIngestRequestDTO;
import cn.liboshuai.pulsix.framework.common.biz.risk.access.dto.AccessIngestResponseDTO;
import cn.liboshuai.pulsix.framework.common.biz.risk.access.enums.AccessAckStatusEnum;
import cn.liboshuai.pulsix.framework.common.biz.risk.access.normalize.StandardEventNormalizeResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DefaultIngestPipelineServiceTest {

    private IngestDesignConfigService configService;
    private IngestAuthService authService;
    private StandardEventNormalizationService normalizationService;
    private IngestEventProducer eventProducer;
    private DefaultIngestPipelineService service;

    @BeforeEach
    void setUp() {
        configService = mock(IngestDesignConfigService.class);
        authService = mock(IngestAuthService.class);
        normalizationService = mock(StandardEventNormalizationService.class);
        eventProducer = mock(IngestEventProducer.class);

        IngestErrorEventFactory errorEventFactory = new IngestErrorEventFactory();
        cn.liboshuai.pulsix.access.ingest.config.PulsixIngestProperties properties = new cn.liboshuai.pulsix.access.ingest.config.PulsixIngestProperties();
        ReflectionTestUtils.setField(errorEventFactory, "clock", Clock.fixed(Instant.parse("2026-03-13T02:31:00Z"), ZoneId.of("Asia/Shanghai")));
        ReflectionTestUtils.setField(errorEventFactory, "properties", properties);

        service = new DefaultIngestPipelineService();
        ReflectionTestUtils.setField(service, "configService", configService);
        ReflectionTestUtils.setField(service, "authService", authService);
        ReflectionTestUtils.setField(service, "normalizationService", normalizationService);
        ReflectionTestUtils.setField(service, "eventProducer", eventProducer);
        ReflectionTestUtils.setField(service, "errorEventFactory", errorEventFactory);
        ReflectionTestUtils.setField(service, "objectMapper", new ObjectMapper());
    }

    @Test
    void shouldAcceptHttpRequestAndSendStandardEvent() {
        IngestRuntimeConfig runtimeConfig = IngestRuntimeConfig.builder()
                .sourceCode("http_none_demo")
                .sceneCode("TRADE_RISK")
                .eventCode("TRADE_EVENT")
                .source(IngestSourceConfig.builder().sourceCode("http_none_demo").authType("NONE")
                        .standardTopicName("pulsix.event.standard").errorTopicName("pulsix.event.dlq").build())
                .build();
        StandardEventNormalizeResult normalizeResult = new StandardEventNormalizeResult();
        normalizeResult.setStandardEventJson(new LinkedHashMap<>(Map.of(
                "eventId", "E_9101",
                "traceId", "T_9101",
                "sceneCode", "TRADE_RISK",
                "eventType", "trade",
                "eventTime", "2026-03-13T10:31:00",
                "userId", "U9001"
        )));
        normalizeResult.setMissingRequiredFields(List.of());

        when(configService.getConfig("http_none_demo", "TRADE_RISK", "TRADE_EVENT")).thenReturn(runtimeConfig);
        doNothing().when(authService).authenticate(any(), eq(runtimeConfig));
        when(normalizationService.normalize(eq("http_none_demo"), eq("TRADE_RISK"), eq("TRADE_EVENT"), any()))
                .thenReturn(normalizeResult);
        when(eventProducer.sendStandardEvent(eq(runtimeConfig.getSource()), eq(normalizeResult.getStandardEventJson())))
                .thenReturn(IngestKafkaSendResult.builder().topicName("pulsix.event.standard").messageKey("TRADE_RISK").build());

        AccessIngestResponseDTO response = service.ingest(AccessIngestRequestDTO.builder()
                .requestId("REQ_1001")
                .sourceCode("http_none_demo")
                .payload("{\"event_id\":\"E_9101\",\"req\":{\"traceId\":\"T_9101\"}}")
                .metadata(Map.of("sceneCode", "TRADE_RISK", "eventCode", "TRADE_EVENT"))
                .build());

        assertThat(response.getRequestId()).isEqualTo("REQ_1001");
        assertThat(response.getStatus()).isEqualTo(AccessAckStatusEnum.ACCEPTED.getStatus());
        assertThat(response.getTraceId()).isEqualTo("T_9101");
        assertThat(response.getEventId()).isEqualTo("E_9101");
        assertThat(response.getStandardTopicName()).isEqualTo("pulsix.event.standard");
        verify(eventProducer).sendStandardEvent(runtimeConfig.getSource(), normalizeResult.getStandardEventJson());
    }

    @Test
    void shouldRejectWhenRequiredFieldsMissingAndSendDlq() {
        IngestRuntimeConfig runtimeConfig = IngestRuntimeConfig.builder()
                .sourceCode("http_none_demo")
                .sceneCode("TRADE_RISK")
                .eventCode("TRADE_EVENT")
                .source(IngestSourceConfig.builder().sourceCode("http_none_demo").authType("NONE")
                        .errorTopicName("pulsix.event.dlq").build())
                .build();
        StandardEventNormalizeResult normalizeResult = new StandardEventNormalizeResult();
        normalizeResult.setStandardEventJson(new LinkedHashMap<>(Map.of(
                "eventId", "raw_trade_bad_8101",
                "traceId", "TRACE-S18-8101",
                "sceneCode", "TRADE_RISK"
        )));
        normalizeResult.setMissingRequiredFields(List.of("eventTime", "userId"));

        when(configService.getConfig("http_none_demo", "TRADE_RISK", "TRADE_EVENT")).thenReturn(runtimeConfig);
        doNothing().when(authService).authenticate(any(), eq(runtimeConfig));
        when(normalizationService.normalize(eq("http_none_demo"), eq("TRADE_RISK"), eq("TRADE_EVENT"), any()))
                .thenReturn(normalizeResult);
        when(eventProducer.sendErrorEvent(any(IngestErrorEvent.class)))
                .thenReturn(IngestKafkaSendResult.builder().topicName("pulsix.event.dlq").messageKey("TRACE-S18-8101").build());

        AccessIngestResponseDTO response = service.ingest(AccessIngestRequestDTO.builder()
                .requestId("REQ_1002")
                .sourceCode("http_none_demo")
                .payload("{\"event_id\":\"raw_trade_bad_8101\",\"req\":{\"traceId\":\"TRACE-S18-8101\"}}")
                .metadata(Map.of("sceneCode", "TRADE_RISK", "eventCode", "TRADE_EVENT"))
                .build());

        assertThat(response.getStatus()).isEqualTo(AccessAckStatusEnum.REJECTED.getStatus());
        assertThat(response.getCode()).isEqualTo(1_006_001_013);
        assertThat(response.getMessage()).contains("eventTime,userId");
        verify(eventProducer).sendErrorEvent(any(IngestErrorEvent.class));
    }

}
