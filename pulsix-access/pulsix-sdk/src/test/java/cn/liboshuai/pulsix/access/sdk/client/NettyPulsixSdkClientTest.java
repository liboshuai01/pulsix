package cn.liboshuai.pulsix.access.sdk.client;

import cn.liboshuai.pulsix.access.sdk.callback.PulsixSdkAckCallback;
import cn.liboshuai.pulsix.access.sdk.model.PulsixSdkSendRequest;
import cn.liboshuai.pulsix.framework.common.biz.risk.access.dto.AccessIngestRequestDTO;
import cn.liboshuai.pulsix.framework.common.biz.risk.access.dto.AccessIngestResponseDTO;
import cn.liboshuai.pulsix.framework.common.biz.risk.access.enums.AccessAckStatusEnum;
import cn.liboshuai.pulsix.framework.common.exception.ServiceException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class NettyPulsixSdkClientTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void shouldConnectSendAndReceiveAckWithCallback() throws Exception {
        try (MockAckServer server = new MockAckServer(objectMapper, AccessIngestResponseDTO.builder()
                .requestId("REQ_SDK_1")
                .traceId("TRACE_SDK_1")
                .eventId("E_SDK_1")
                .status(AccessAckStatusEnum.ACCEPTED.getStatus())
                .code(0)
                .message("accepted")
                .standardTopicName("pulsix.event.standard")
                .processTimeMillis(7L)
                .build())) {
            PulsixSdkClient client = new NettyPulsixSdkClient(PulsixSdkOptions.builder()
                    .host("127.0.0.1")
                    .port(server.getPort())
                    .sourceCode("trade_sdk_demo")
                    .requestTimeoutMillis(3000)
                    .build());
            client.start();
            try {
                CountDownLatch callbackLatch = new CountDownLatch(1);
                AtomicReference<AccessIngestResponseDTO> callbackResponse = new AtomicReference<>();
                CompletableFuture<AccessIngestResponseDTO> future = client.sendAsync(PulsixSdkSendRequest.builder()
                                .requestId("REQ_SDK_1")
                                .sceneCode("TRADE_RISK")
                                .eventCode("TRADE_EVENT")
                                .payload("{\"event_id\":\"E_SDK_1\"}")
                                .metadata(Map.of("authorization", "Bearer token-demo-001"))
                                .build(), new PulsixSdkAckCallback() {
                            @Override
                            public void onAck(AccessIngestResponseDTO response) {
                                callbackResponse.set(response);
                                callbackLatch.countDown();
                            }
                        });

                AccessIngestResponseDTO response = future.get(3, TimeUnit.SECONDS);
                assertThat(response.getStatus()).isEqualTo("ACCEPTED");
                assertThat(response.getStandardTopicName()).isEqualTo("pulsix.event.standard");
                assertThat(callbackLatch.await(3, TimeUnit.SECONDS)).isTrue();
                assertThat(callbackResponse.get().getRequestId()).isEqualTo("REQ_SDK_1");

                AccessIngestRequestDTO request = server.awaitRequest();
                assertThat(request.getSourceCode()).isEqualTo("trade_sdk_demo");
                assertThat(request.getTransportType()).isEqualTo("SDK");
                assertThat(request.getMetadata()).containsEntry("sceneCode", "TRADE_RISK");
                assertThat(request.getMetadata()).containsEntry("eventCode", "TRADE_EVENT");
                assertThat(request.getMetadata()).containsEntry("authorization", "Bearer token-demo-001");
            } finally {
                client.close();
            }
        }
    }

    @Test
    void shouldFailWhenClientNotStarted() {
        PulsixSdkClient client = new NettyPulsixSdkClient(PulsixSdkOptions.builder()
                .sourceCode("trade_sdk_demo")
                .build());

        CompletableFuture<AccessIngestResponseDTO> future = client.sendAsync(PulsixSdkSendRequest.builder()
                .sceneCode("TRADE_RISK")
                .eventCode("TRADE_EVENT")
                .payload("{\"event_id\":\"E_SDK_2\"}")
                .build());

        assertThatThrownBy(future::join)
                .hasCauseInstanceOf(ServiceException.class)
                .hasMessageContaining("SDK 客户端尚未启动");
    }

    private static final class MockAckServer implements AutoCloseable {

        private final ServerSocket serverSocket;
        private final ObjectMapper objectMapper;
        private final AccessIngestResponseDTO response;
        private final AtomicReference<AccessIngestRequestDTO> requestRef = new AtomicReference<>();
        private final CountDownLatch requestLatch = new CountDownLatch(1);
        private final Thread acceptThread;

        private MockAckServer(ObjectMapper objectMapper, AccessIngestResponseDTO response) throws Exception {
            this.objectMapper = objectMapper;
            this.response = response;
            this.serverSocket = new ServerSocket(0);
            this.acceptThread = new Thread(this::acceptLoop, "mock-sdk-ack-server");
            this.acceptThread.setDaemon(true);
            this.acceptThread.start();
        }

        private void acceptLoop() {
            try (Socket socket = serverSocket.accept()) {
                DataInputStream inputStream = new DataInputStream(socket.getInputStream());
                DataOutputStream outputStream = new DataOutputStream(socket.getOutputStream());
                int length = inputStream.readInt();
                byte[] bytes = inputStream.readNBytes(length);
                AccessIngestRequestDTO request = objectMapper.readValue(new String(bytes, StandardCharsets.UTF_8), AccessIngestRequestDTO.class);
                requestRef.set(request);
                requestLatch.countDown();

                byte[] responseBytes = objectMapper.writeValueAsBytes(response);
                outputStream.writeInt(responseBytes.length);
                outputStream.write(responseBytes);
                outputStream.flush();
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        }

        private int getPort() {
            return serverSocket.getLocalPort();
        }

        private AccessIngestRequestDTO awaitRequest() throws Exception {
            assertThat(requestLatch.await(3, TimeUnit.SECONDS)).isTrue();
            return requestRef.get();
        }

        @Override
        public void close() throws Exception {
            serverSocket.close();
            acceptThread.join(1000);
        }

    }

}
