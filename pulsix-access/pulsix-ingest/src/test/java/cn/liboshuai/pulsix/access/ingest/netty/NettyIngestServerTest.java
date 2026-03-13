package cn.liboshuai.pulsix.access.ingest.netty;

import cn.liboshuai.pulsix.access.ingest.config.PulsixIngestProperties;
import cn.liboshuai.pulsix.access.ingest.service.IngestPipelineService;
import cn.liboshuai.pulsix.access.ingest.service.error.IngestErrorDispatchService;
import cn.liboshuai.pulsix.framework.common.biz.risk.access.dto.AccessIngestRequestDTO;
import cn.liboshuai.pulsix.framework.common.biz.risk.access.dto.AccessIngestResponseDTO;
import com.fasterxml.jackson.databind.ObjectMapper;
import cn.liboshuai.pulsix.access.ingest.service.metrics.InMemoryIngestMetricsService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class NettyIngestServerTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void shouldAcceptOneMessageFromMockSocketClient() throws Exception {
        IngestPipelineService ingestPipelineService = mock(IngestPipelineService.class);
        IngestErrorDispatchService errorDispatchService = mock(IngestErrorDispatchService.class);
        when(ingestPipelineService.ingest(any())).thenReturn(AccessIngestResponseDTO.builder()
                .requestId("REQ_SOCKET_1")
                .traceId("TRACE_SOCKET_1")
                .eventId("E_SOCKET_1")
                .status("ACCEPTED")
                .code(0)
                .message("accepted")
                .standardTopicName("pulsix.event.standard")
                .processTimeMillis(5L)
                .build());

        PulsixIngestProperties properties = new PulsixIngestProperties();
        properties.getNetty().setEnabled(true);
        properties.getNetty().setPort(0);
        properties.getNetty().setBossThreads(1);
        properties.getNetty().setWorkerThreads(1);
        properties.getNetty().setMaxFrameLength(4096);
        properties.getNetty().setIdleTimeoutSeconds(30);

        NettyIngestServer server = new NettyIngestServer(properties,
                new NettyIngestRequestHandler(ingestPipelineService, errorDispatchService, objectMapper, new InMemoryIngestMetricsService()));
        server.start();
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress("127.0.0.1", server.getBoundPort()), 3000);
            socket.setSoTimeout(3000);

            AccessIngestRequestDTO request = AccessIngestRequestDTO.builder()
                    .requestId("REQ_SOCKET_1")
                    .sourceCode("trade_sdk_demo")
                    .payload("{\"event_id\":\"E_SOCKET_1\"}")
                    .metadata(Map.of(
                            "sceneCode", "TRADE_RISK",
                            "eventCode", "TRADE_EVENT",
                            "authorization", "Bearer token-demo-001"
                    ))
                    .build();
            writeFrame(socket, objectMapper.writeValueAsString(request));

            AccessIngestResponseDTO response = objectMapper.readValue(readFrame(socket), AccessIngestResponseDTO.class);
            assertThat(response.getRequestId()).isEqualTo("REQ_SOCKET_1");
            assertThat(response.getStatus()).isEqualTo("ACCEPTED");
            assertThat(response.getStandardTopicName()).isEqualTo("pulsix.event.standard");

            ArgumentCaptor<AccessIngestRequestDTO> captor = ArgumentCaptor.forClass(AccessIngestRequestDTO.class);
            verify(ingestPipelineService).ingest(captor.capture());
            AccessIngestRequestDTO actualRequest = captor.getValue();
            assertThat(actualRequest.getTransportType()).isEqualTo("SDK");
            assertThat(actualRequest.getRemoteAddress()).contains("127.0.0.1");
        } finally {
            server.stop();
        }
    }

    private void writeFrame(Socket socket, String jsonText) throws Exception {
        byte[] bytes = jsonText.getBytes(StandardCharsets.UTF_8);
        DataOutputStream outputStream = new DataOutputStream(socket.getOutputStream());
        outputStream.writeInt(bytes.length);
        outputStream.write(bytes);
        outputStream.flush();
    }

    private String readFrame(Socket socket) throws Exception {
        DataInputStream inputStream = new DataInputStream(socket.getInputStream());
        int length = inputStream.readInt();
        byte[] bytes = inputStream.readNBytes(length);
        return new String(bytes, StandardCharsets.UTF_8);
    }

}
