package cn.liboshuai.pulsix.access.ingest.netty;

import cn.liboshuai.pulsix.access.ingest.domain.error.IngestErrorEvent;
import cn.liboshuai.pulsix.access.ingest.service.IngestPipelineService;
import cn.liboshuai.pulsix.access.ingest.service.error.IngestErrorDispatchService;
import cn.liboshuai.pulsix.access.ingest.service.error.IngestErrorEventFactory;
import cn.liboshuai.pulsix.access.ingest.service.errorlog.IngestErrorLogWriter;
import cn.liboshuai.pulsix.access.ingest.infra.kafka.IngestEventProducer;
import cn.liboshuai.pulsix.access.ingest.infra.kafka.IngestKafkaSendResult;
import cn.liboshuai.pulsix.framework.common.biz.risk.access.dto.AccessIngestRequestDTO;
import cn.liboshuai.pulsix.framework.common.biz.risk.access.dto.AccessIngestResponseDTO;
import com.fasterxml.jackson.databind.ObjectMapper;
import cn.liboshuai.pulsix.access.ingest.service.metrics.InMemoryIngestMetricsService;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.LengthFieldPrepender;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.springframework.test.util.ReflectionTestUtils;

class NettyIngestRequestHandlerTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void shouldDispatchSdkFrameIntoPipeline() throws Exception {
        IngestPipelineService ingestPipelineService = mock(IngestPipelineService.class);
        IngestErrorDispatchService errorDispatchService = mock(IngestErrorDispatchService.class);
        when(ingestPipelineService.ingest(any())).thenReturn(AccessIngestResponseDTO.builder()
                .requestId("REQ_NETTY_1")
                .traceId("TRACE_NETTY_1")
                .eventId("E_NETTY_1")
                .status("ACCEPTED")
                .code(0)
                .message("accepted")
                .standardTopicName("pulsix.event.standard")
                .processTimeMillis(6L)
                .build());

        EmbeddedChannel channel = new EmbeddedChannel(
                new LengthFieldBasedFrameDecoder(4096, 0, 4, 0, 4),
                new StringDecoder(StandardCharsets.UTF_8),
                new LengthFieldPrepender(4),
                new StringEncoder(StandardCharsets.UTF_8),
                new NettyIngestRequestHandler(ingestPipelineService, errorDispatchService, objectMapper, new InMemoryIngestMetricsService())
        );

        String requestJson = objectMapper.writeValueAsString(AccessIngestRequestDTO.builder()
                .requestId("REQ_NETTY_1")
                .sourceCode("trade_sdk_demo")
                .payload("{\"event_id\":\"E_NETTY_1\"}")
                .metadata(Map.of(
                        "sceneCode", "TRADE_RISK",
                        "eventCode", "TRADE_EVENT",
                        "authorization", "Bearer token-demo-001"
                ))
                .build());

        channel.writeInbound(frame(requestJson));

        ArgumentCaptor<AccessIngestRequestDTO> captor = ArgumentCaptor.forClass(AccessIngestRequestDTO.class);
        verify(ingestPipelineService).ingest(captor.capture());
        AccessIngestRequestDTO request = captor.getValue();
        assertThat(request.getTransportType()).isEqualTo("SDK");
        assertThat(request.getSendTimeMillis()).isNotNull();
        assertThat(request.getMetadata()).containsEntry("sceneCode", "TRADE_RISK");

        AccessIngestResponseDTO response = objectMapper.readValue(unframe(readOutboundFrame(channel)), AccessIngestResponseDTO.class);
        assertThat(response.getRequestId()).isEqualTo("REQ_NETTY_1");
        assertThat(response.getStatus()).isEqualTo("ACCEPTED");
        assertThat(response.getStandardTopicName()).isEqualTo("pulsix.event.standard");

        channel.finishAndReleaseAll();
    }

    @Test
    void shouldReturnRejectedResponseWhenFrameIsInvalidJson() throws Exception {
        IngestPipelineService ingestPipelineService = mock(IngestPipelineService.class);
        IngestEventProducer eventProducer = mock(IngestEventProducer.class);
        IngestErrorLogWriter errorLogWriter = mock(IngestErrorLogWriter.class);
        IngestErrorEventFactory errorEventFactory = new IngestErrorEventFactory();
        ReflectionTestUtils.setField(errorEventFactory, "clock",
                Clock.fixed(Instant.parse("2026-03-13T02:31:00Z"), ZoneId.of("Asia/Shanghai")));
        ReflectionTestUtils.setField(errorEventFactory, "properties",
                new cn.liboshuai.pulsix.access.ingest.config.PulsixIngestProperties());
        IngestErrorDispatchService errorDispatchService = new IngestErrorDispatchService();
        ReflectionTestUtils.setField(errorDispatchService, "errorEventFactory", errorEventFactory);
        ReflectionTestUtils.setField(errorDispatchService, "eventProducer", eventProducer);
        ReflectionTestUtils.setField(errorDispatchService, "errorLogWriter", errorLogWriter);
        InMemoryIngestMetricsService ingestMetricsService = new InMemoryIngestMetricsService();
        when(eventProducer.sendErrorEvent(any(IngestErrorEvent.class)))
                .thenReturn(IngestKafkaSendResult.builder().topicName("pulsix.event.dlq").messageKey(null).build());

        EmbeddedChannel channel = new EmbeddedChannel(
                new LengthFieldBasedFrameDecoder(4096, 0, 4, 0, 4),
                new StringDecoder(StandardCharsets.UTF_8),
                new LengthFieldPrepender(4),
                new StringEncoder(StandardCharsets.UTF_8),
                new NettyIngestRequestHandler(ingestPipelineService, errorDispatchService, objectMapper, ingestMetricsService)
        );

        channel.writeInbound(frame("not-json"));

        AccessIngestResponseDTO response = objectMapper.readValue(unframe(readOutboundFrame(channel)), AccessIngestResponseDTO.class);
        assertThat(response.getStatus()).isEqualTo("REJECTED");
        assertThat(response.getCode()).isEqualTo(1_006_001_010);
        assertThat(response.getMessage()).isEqualTo("SDK 请求帧不是合法 JSON");
        assertThat(response.getProcessTimeMillis()).isGreaterThanOrEqualTo(0L);

        ArgumentCaptor<IngestErrorEvent> captor = ArgumentCaptor.forClass(IngestErrorEvent.class);
        verify(eventProducer).sendErrorEvent(captor.capture());
        IngestErrorEvent errorEvent = captor.getValue();
        assertThat(errorEvent.getIngestStage()).isEqualTo(cn.liboshuai.pulsix.access.ingest.enums.IngestStageEnum.PARSE);
        assertThat(errorEvent.getErrorCode()).isEqualTo("SDK_FRAME_JSON_INVALID");
        assertThat(errorEvent.getRawPayloadJson()).isInstanceOf(Map.class);
        @SuppressWarnings("unchecked")
        Map<String, Object> rawPayload = (Map<String, Object>) errorEvent.getRawPayloadJson();
        assertThat(rawPayload).containsEntry("transportType", "SDK");
        assertThat(rawPayload).containsEntry("rawFrameText", "not-json");
        verify(errorLogWriter).write(any(IngestErrorEvent.class), eq(IngestErrorLogWriter.REPROCESS_STATUS_PENDING));
        assertThat(ingestMetricsService.snapshot().getRejectedCount()).isEqualTo(1L);

        channel.finishAndReleaseAll();
    }

    private ByteBuf frame(String jsonText) {
        byte[] bytes = jsonText.getBytes(StandardCharsets.UTF_8);
        return Unpooled.buffer(4 + bytes.length).writeInt(bytes.length).writeBytes(bytes);
    }

    private ByteBuf readOutboundFrame(EmbeddedChannel channel) {
        ByteBuf merged = Unpooled.buffer();
        ByteBuf next;
        while ((next = channel.readOutbound()) != null) {
            merged.writeBytes(next);
            next.release();
        }
        return merged;
    }

    private String unframe(ByteBuf byteBuf) {
        int length = byteBuf.readInt();
        byte[] bytes = new byte[length];
        byteBuf.readBytes(bytes);
        return new String(bytes, StandardCharsets.UTF_8);
    }

}
