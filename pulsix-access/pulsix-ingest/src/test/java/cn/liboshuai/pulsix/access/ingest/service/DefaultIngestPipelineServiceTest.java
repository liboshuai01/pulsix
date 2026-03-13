package cn.liboshuai.pulsix.access.ingest.service;

import cn.liboshuai.pulsix.access.ingest.domain.config.IngestRuntimeConfig;
import cn.liboshuai.pulsix.access.ingest.domain.config.IngestSourceConfig;
import cn.liboshuai.pulsix.access.ingest.domain.error.IngestErrorEvent;
import cn.liboshuai.pulsix.access.ingest.enums.IngestStageEnum;
import cn.liboshuai.pulsix.access.ingest.infra.kafka.IngestEventProducer;
import cn.liboshuai.pulsix.access.ingest.infra.kafka.IngestKafkaSendResult;
import cn.liboshuai.pulsix.access.ingest.service.auth.IngestAuthException;
import cn.liboshuai.pulsix.access.ingest.service.auth.IngestAuthService;
import cn.liboshuai.pulsix.access.ingest.service.config.IngestDesignConfigService;
import cn.liboshuai.pulsix.access.ingest.service.error.IngestErrorEventFactory;
import cn.liboshuai.pulsix.access.ingest.service.errorlog.IngestErrorLogWriter;
import cn.liboshuai.pulsix.access.ingest.service.metrics.InMemoryIngestMetricsService;
import cn.liboshuai.pulsix.access.ingest.service.ratelimit.InMemoryIngestRateLimitService;
import cn.liboshuai.pulsix.access.ingest.service.normalize.StandardEventNormalizationService;
import cn.liboshuai.pulsix.framework.common.biz.risk.access.dto.AccessIngestRequestDTO;
import cn.liboshuai.pulsix.framework.common.biz.risk.access.dto.AccessIngestResponseDTO;
import cn.liboshuai.pulsix.framework.common.biz.risk.access.enums.AccessAckStatusEnum;
import cn.liboshuai.pulsix.framework.common.biz.risk.access.normalize.StandardEventNormalizeResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
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
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DefaultIngestPipelineServiceTest {

    private IngestDesignConfigService configService;
    private IngestAuthService authService;
    private StandardEventNormalizationService normalizationService;
    private IngestEventProducer eventProducer;
    private IngestErrorLogWriter errorLogWriter;
    private DefaultIngestPipelineService service;
    private InMemoryIngestMetricsService ingestMetricsService;
    private InMemoryIngestRateLimitService ingestRateLimitService;

    @BeforeEach
    void setUp() {
        configService = mock(IngestDesignConfigService.class);
        authService = mock(IngestAuthService.class);
        normalizationService = mock(StandardEventNormalizationService.class);
        eventProducer = mock(IngestEventProducer.class);
        errorLogWriter = mock(IngestErrorLogWriter.class);

        IngestErrorEventFactory errorEventFactory = new IngestErrorEventFactory();
        cn.liboshuai.pulsix.access.ingest.config.PulsixIngestProperties properties = new cn.liboshuai.pulsix.access.ingest.config.PulsixIngestProperties();
        ReflectionTestUtils.setField(errorEventFactory, "clock", Clock.fixed(Instant.parse("2026-03-13T02:31:00Z"), ZoneId.of("Asia/Shanghai")));
        ReflectionTestUtils.setField(errorEventFactory, "properties", properties);

        ingestMetricsService = new InMemoryIngestMetricsService();
        ingestRateLimitService = new InMemoryIngestRateLimitService();
        ReflectionTestUtils.setField(ingestRateLimitService, "clock", Clock.fixed(Instant.parse("2026-03-13T02:31:00Z"), ZoneId.of("UTC")));

        service = new DefaultIngestPipelineService();
        ReflectionTestUtils.setField(service, "configService", configService);
        ReflectionTestUtils.setField(service, "authService", authService);
        ReflectionTestUtils.setField(service, "normalizationService", normalizationService);
        ReflectionTestUtils.setField(service, "eventProducer", eventProducer);
        ReflectionTestUtils.setField(service, "errorEventFactory", errorEventFactory);
        ReflectionTestUtils.setField(service, "errorLogWriter", errorLogWriter);
        ReflectionTestUtils.setField(service, "objectMapper", new ObjectMapper());
        ReflectionTestUtils.setField(service, "ingestMetricsService", ingestMetricsService);
        ReflectionTestUtils.setField(service, "ingestRateLimitService", ingestRateLimitService);
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
        assertThat(ingestMetricsService.snapshot().getAcceptedCount()).isEqualTo(1L);
        assertThat(ingestMetricsService.snapshot().getSourceMetrics()).containsKey("http_none_demo");
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
        verify(errorLogWriter).write(any(IngestErrorEvent.class), eq(IngestErrorLogWriter.REPROCESS_STATUS_PENDING));
        assertThat(ingestMetricsService.snapshot().getRejectedCount()).isGreaterThanOrEqualTo(1L);
    }

    @Test
    void shouldRejectWhenHmacAuthFailsAndSendAuthDlqEvent() {
        IngestRuntimeConfig runtimeConfig = IngestRuntimeConfig.builder()
                .sourceCode("trade_http_demo")
                .sceneCode("TRADE_RISK")
                .eventCode("TRADE_EVENT")
                .source(IngestSourceConfig.builder().sourceCode("trade_http_demo").authType("HMAC")
                        .errorTopicName("pulsix.event.dlq").build())
                .build();

        when(configService.getConfig("trade_http_demo", "TRADE_RISK", "TRADE_EVENT")).thenReturn(runtimeConfig);
        doThrow(new IngestAuthException(1_006_001_012, "AUTH_SIGN_INVALID", "签名校验失败，拒绝写入标准事件 Topic"))
                .when(authService).authenticate(any(), eq(runtimeConfig));
        when(eventProducer.sendErrorEvent(any(IngestErrorEvent.class)))
                .thenReturn(IngestKafkaSendResult.builder().topicName("pulsix.event.dlq").messageKey("REQ_1003").build());

        AccessIngestResponseDTO response = service.ingest(AccessIngestRequestDTO.builder()
                .requestId("REQ_1003")
                .sourceCode("trade_http_demo")
                .payload("{\"event_id\":\"raw_trade_bad_8103\",\"uid\":\"U8103\"}")
                .metadata(Map.of("sceneCode", "TRADE_RISK", "eventCode", "TRADE_EVENT"))
                .build());

        assertThat(response.getStatus()).isEqualTo(AccessAckStatusEnum.REJECTED.getStatus());
        assertThat(response.getCode()).isEqualTo(1_006_001_012);
        assertThat(response.getMessage()).isEqualTo("签名校验失败，拒绝写入标准事件 Topic");

        ArgumentCaptor<IngestErrorEvent> captor = ArgumentCaptor.forClass(IngestErrorEvent.class);
        verify(eventProducer).sendErrorEvent(captor.capture());
        IngestErrorEvent errorEvent = captor.getValue();
        assertThat(errorEvent.getIngestStage()).isEqualTo(IngestStageEnum.AUTH);
        assertThat(errorEvent.getErrorCode()).isEqualTo("AUTH_SIGN_INVALID");
        assertThat(errorEvent.getSourceCode()).isEqualTo("trade_http_demo");
        assertThat(errorEvent.getSceneCode()).isEqualTo("TRADE_RISK");
        assertThat(errorEvent.getEventCode()).isEqualTo("TRADE_EVENT");
        assertThat(errorEvent.getErrorTopicName()).isEqualTo("pulsix.event.dlq");
        assertThat(errorEvent.getRawPayloadJson()).isEqualTo("{\"event_id\":\"raw_trade_bad_8103\",\"uid\":\"U8103\"}");
        verify(errorLogWriter).write(any(IngestErrorEvent.class), eq(IngestErrorLogWriter.REPROCESS_STATUS_PENDING));
        assertThat(ingestMetricsService.snapshot().getRejectedCount()).isGreaterThanOrEqualTo(1L);
    }

    @Test
    void shouldRejectWhenSourceRateLimitExceeded() {
        IngestRuntimeConfig runtimeConfig = IngestRuntimeConfig.builder()
                .sourceCode("http_none_demo")
                .sceneCode("TRADE_RISK")
                .eventCode("TRADE_EVENT")
                .source(IngestSourceConfig.builder().sourceCode("http_none_demo").authType("NONE")
                        .errorTopicName("pulsix.event.dlq").rateLimitQps(1).build())
                .build();
        StandardEventNormalizeResult normalizeResult = new StandardEventNormalizeResult();
        normalizeResult.setStandardEventJson(new LinkedHashMap<>(Map.of(
                "eventId", "E_9201",
                "traceId", "T_9201",
                "sceneCode", "TRADE_RISK",
                "eventType", "trade",
                "eventTime", "2026-03-13T10:31:00",
                "userId", "U9201"
        )));
        normalizeResult.setMissingRequiredFields(List.of());

        when(configService.getConfig("http_none_demo", "TRADE_RISK", "TRADE_EVENT")).thenReturn(runtimeConfig);
        doNothing().when(authService).authenticate(any(), eq(runtimeConfig));
        when(normalizationService.normalize(eq("http_none_demo"), eq("TRADE_RISK"), eq("TRADE_EVENT"), any()))
                .thenReturn(normalizeResult);
        when(eventProducer.sendStandardEvent(eq(runtimeConfig.getSource()), eq(normalizeResult.getStandardEventJson())))
                .thenReturn(IngestKafkaSendResult.builder().topicName("pulsix.event.standard").messageKey("TRADE_RISK").build());
        when(eventProducer.sendErrorEvent(any(IngestErrorEvent.class)))
                .thenReturn(IngestKafkaSendResult.builder().topicName("pulsix.event.dlq").messageKey("REQ_2002").build());

        AccessIngestResponseDTO firstResponse = service.ingest(AccessIngestRequestDTO.builder()
                .requestId("REQ_2001")
                .sourceCode("http_none_demo")
                .payload("{\"event_id\":\"E_9201\"}")
                .metadata(Map.of("sceneCode", "TRADE_RISK", "eventCode", "TRADE_EVENT"))
                .build());
        AccessIngestResponseDTO secondResponse = service.ingest(AccessIngestRequestDTO.builder()
                .requestId("REQ_2002")
                .sourceCode("http_none_demo")
                .payload("{\"event_id\":\"E_9202\"}")
                .metadata(Map.of("sceneCode", "TRADE_RISK", "eventCode", "TRADE_EVENT"))
                .build());

        assertThat(firstResponse.getStatus()).isEqualTo(AccessAckStatusEnum.ACCEPTED.getStatus());
        assertThat(secondResponse.getStatus()).isEqualTo(AccessAckStatusEnum.REJECTED.getStatus());
        assertThat(secondResponse.getCode()).isEqualTo(1_006_001_014);
        assertThat(secondResponse.getMessage()).contains("sourceCode=http_none_demo");

        ArgumentCaptor<IngestErrorEvent> captor = ArgumentCaptor.forClass(IngestErrorEvent.class);
        verify(eventProducer).sendErrorEvent(captor.capture());
        assertThat(captor.getValue().getIngestStage()).isEqualTo(IngestStageEnum.RATE_LIMIT);
        verify(errorLogWriter).write(any(IngestErrorEvent.class), eq(IngestErrorLogWriter.REPROCESS_STATUS_PENDING));
        assertThat(ingestMetricsService.snapshot().getRejectedCount()).isGreaterThanOrEqualTo(1L);
    }

    @Test
    void shouldWriteRetryFailedWhenDlqSendFails() {
        IngestRuntimeConfig runtimeConfig = IngestRuntimeConfig.builder()
                .sourceCode("http_none_demo")
                .sceneCode("TRADE_RISK")
                .eventCode("TRADE_EVENT")
                .source(IngestSourceConfig.builder().sourceCode("http_none_demo").authType("NONE")
                        .errorTopicName("pulsix.event.dlq").build())
                .build();
        StandardEventNormalizeResult normalizeResult = new StandardEventNormalizeResult();
        normalizeResult.setStandardEventJson(new LinkedHashMap<>(Map.of(
                "eventId", "raw_trade_bad_8102",
                "traceId", "TRACE-S18-8102",
                "sceneCode", "TRADE_RISK"
        )));
        normalizeResult.setMissingRequiredFields(List.of("eventTime"));

        when(configService.getConfig("http_none_demo", "TRADE_RISK", "TRADE_EVENT")).thenReturn(runtimeConfig);
        doNothing().when(authService).authenticate(any(), eq(runtimeConfig));
        when(normalizationService.normalize(eq("http_none_demo"), eq("TRADE_RISK"), eq("TRADE_EVENT"), any()))
                .thenReturn(normalizeResult);
        doThrow(new RuntimeException("kafka down")).when(eventProducer).sendErrorEvent(any(IngestErrorEvent.class));

        AccessIngestResponseDTO response = service.ingest(AccessIngestRequestDTO.builder()
                .requestId("REQ_1004")
                .sourceCode("http_none_demo")
                .payload("{\"event_id\":\"raw_trade_bad_8102\",\"req\":{\"traceId\":\"TRACE-S18-8102\"}}")
                .metadata(Map.of("sceneCode", "TRADE_RISK", "eventCode", "TRADE_EVENT"))
                .build());

        assertThat(response.getStatus()).isEqualTo(AccessAckStatusEnum.REJECTED.getStatus());
        verify(errorLogWriter).write(any(IngestErrorEvent.class), eq(IngestErrorLogWriter.REPROCESS_STATUS_RETRY_FAILED));
    }

}
