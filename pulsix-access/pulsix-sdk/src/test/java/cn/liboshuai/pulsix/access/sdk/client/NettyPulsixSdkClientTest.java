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
import java.io.EOFException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class NettyPulsixSdkClientTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void shouldConnectSendAndReceiveAckWithCallback() throws Exception {
        try (ScriptedAckServer server = new ScriptedAckServer(0, objectMapper, false)) {
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
                CompletableFuture<AccessIngestResponseDTO> future = client.sendAsync(buildRequest("REQ_SDK_1"), new PulsixSdkAckCallback() {
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

                List<AccessIngestRequestDTO> requests = server.awaitRequests(1, 3, TimeUnit.SECONDS);
                AccessIngestRequestDTO request = requests.get(0);
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
    void shouldBufferBatchRequestsUntilServerBecomesAvailable() throws Exception {
        int port = reservePort();
        PulsixSdkClient client = new NettyPulsixSdkClient(PulsixSdkOptions.builder()
                .host("127.0.0.1")
                .port(port)
                .sourceCode("trade_sdk_demo")
                .connectTimeoutMillis(200)
                .requestTimeoutMillis(500)
                .maxBatchSize(2)
                .maxBufferSize(10)
                .batchFlushIntervalMillis(50)
                .reconnectIntervalMillis(100)
                .maxRetryCount(2)
                .build());
        client.start();
        try {
            List<CompletableFuture<AccessIngestResponseDTO>> futures = client.sendBatchAsync(List.of(
                    buildRequest("REQ_SDK_B1"),
                    buildRequest("REQ_SDK_B2"),
                    buildRequest("REQ_SDK_B3")
            ));

            try (ScriptedAckServer server = new ScriptedAckServer(port, objectMapper, false)) {
                List<AccessIngestResponseDTO> responses = waitAll(futures, 5, TimeUnit.SECONDS);
                assertThat(responses).hasSize(3);
                assertThat(responses).allMatch(response -> "ACCEPTED".equals(response.getStatus()));
                List<AccessIngestRequestDTO> requests = server.awaitRequests(3, 5, TimeUnit.SECONDS);
                assertThat(requests).extracting(AccessIngestRequestDTO::getRequestId)
                        .containsExactly("REQ_SDK_B1", "REQ_SDK_B2", "REQ_SDK_B3");
            }
        } finally {
            client.close();
        }
    }

    @Test
    void shouldReconnectAndRetryAfterConnectionDrop() throws Exception {
        try (ScriptedAckServer server = new ScriptedAckServer(0, objectMapper, true)) {
            PulsixSdkClient client = new NettyPulsixSdkClient(PulsixSdkOptions.builder()
                    .host("127.0.0.1")
                    .port(server.getPort())
                    .sourceCode("trade_sdk_demo")
                    .connectTimeoutMillis(300)
                    .requestTimeoutMillis(250)
                    .batchFlushIntervalMillis(50)
                    .reconnectIntervalMillis(100)
                    .maxRetryCount(2)
                    .build());
            client.start();
            try {
                AccessIngestResponseDTO response = client.sendAsync(buildRequest("REQ_SDK_RETRY")).get(5, TimeUnit.SECONDS);

                assertThat(response.getStatus()).isEqualTo("ACCEPTED");
                assertThat(server.getConnectionCount()).isGreaterThanOrEqualTo(2);
                List<AccessIngestRequestDTO> requests = server.awaitRequests(2, 5, TimeUnit.SECONDS);
                assertThat(requests).extracting(AccessIngestRequestDTO::getRequestId)
                        .containsExactly("REQ_SDK_RETRY", "REQ_SDK_RETRY");
            } finally {
                client.close();
            }
        }
    }

    @Test
    void shouldRejectWhenBufferIsFull() {
        int port = reservePort();
        PulsixSdkClient client = new NettyPulsixSdkClient(PulsixSdkOptions.builder()
                .host("127.0.0.1")
                .port(port)
                .sourceCode("trade_sdk_demo")
                .connectTimeoutMillis(100)
                .requestTimeoutMillis(500)
                .maxBatchSize(1)
                .maxBufferSize(1)
                .batchFlushIntervalMillis(50)
                .reconnectIntervalMillis(300)
                .maxRetryCount(1)
                .build());
        client.start();
        try {
            CompletableFuture<AccessIngestResponseDTO> firstFuture = client.sendAsync(buildRequest("REQ_SDK_FULL_1"));
            CompletableFuture<AccessIngestResponseDTO> secondFuture = client.sendAsync(buildRequest("REQ_SDK_FULL_2"));

            assertThatThrownBy(secondFuture::join)
                    .hasCauseInstanceOf(ServiceException.class)
                    .hasMessageContaining("SDK 内存缓冲已满");
            assertThat(firstFuture).isNotCompleted();
        } finally {
            client.close();
        }
    }

    @Test
    void shouldFailWhenClientNotStarted() {
        PulsixSdkClient client = new NettyPulsixSdkClient(PulsixSdkOptions.builder()
                .sourceCode("trade_sdk_demo")
                .build());

        CompletableFuture<AccessIngestResponseDTO> future = client.sendAsync(buildRequest("REQ_SDK_2"));

        assertThatThrownBy(future::join)
                .hasCauseInstanceOf(ServiceException.class)
                .hasMessageContaining("SDK 客户端尚未启动");
    }

    private PulsixSdkSendRequest buildRequest(String requestId) {
        return PulsixSdkSendRequest.builder()
                .requestId(requestId)
                .sceneCode("TRADE_RISK")
                .eventCode("TRADE_EVENT")
                .payload("{\"event_id\":\"" + requestId + "\"}")
                .metadata(Map.of("authorization", "Bearer token-demo-001"))
                .build();
    }

    private List<AccessIngestResponseDTO> waitAll(List<CompletableFuture<AccessIngestResponseDTO>> futures,
                                                  long timeout,
                                                  TimeUnit timeUnit) throws Exception {
        List<AccessIngestResponseDTO> responses = new ArrayList<>(futures.size());
        for (CompletableFuture<AccessIngestResponseDTO> future : futures) {
            responses.add(future.get(timeout, timeUnit));
        }
        return responses;
    }

    private int reservePort() {
        try (ServerSocket serverSocket = new ServerSocket(0)) {
            return serverSocket.getLocalPort();
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    private static final class ScriptedAckServer implements AutoCloseable {

        private final ServerSocket serverSocket;
        private final ObjectMapper objectMapper;
        private final boolean dropFirstConnectionWithoutAck;
        private final List<AccessIngestRequestDTO> requests = new CopyOnWriteArrayList<>();
        private final AtomicBoolean closed = new AtomicBoolean(false);
        private final AtomicInteger connectionCount = new AtomicInteger(0);
        private final ExecutorService connectionExecutor = Executors.newCachedThreadPool();
        private final Thread acceptThread;

        private ScriptedAckServer(int port, ObjectMapper objectMapper, boolean dropFirstConnectionWithoutAck) throws Exception {
            this.objectMapper = objectMapper;
            this.dropFirstConnectionWithoutAck = dropFirstConnectionWithoutAck;
            this.serverSocket = new ServerSocket(port);
            this.acceptThread = new Thread(this::acceptLoop, "mock-sdk-server-accept");
            this.acceptThread.setDaemon(true);
            this.acceptThread.start();
        }

        private void acceptLoop() {
            while (!closed.get()) {
                try {
                    Socket socket = serverSocket.accept();
                    int currentConnection = connectionCount.incrementAndGet();
                    connectionExecutor.submit(() -> handleConnection(socket, currentConnection));
                } catch (SocketException ex) {
                    if (!closed.get()) {
                        throw new RuntimeException(ex);
                    }
                    return;
                } catch (Exception ex) {
                    throw new RuntimeException(ex);
                }
            }
        }

        private void handleConnection(Socket socket, int currentConnection) {
            try (Socket ignored = socket;
                 DataInputStream inputStream = new DataInputStream(socket.getInputStream());
                 DataOutputStream outputStream = new DataOutputStream(socket.getOutputStream())) {
                while (!closed.get()) {
                    int length;
                    try {
                        length = inputStream.readInt();
                    } catch (EOFException ex) {
                        return;
                    }
                    byte[] bytes = inputStream.readNBytes(length);
                    if (bytes.length < length) {
                        return;
                    }
                    AccessIngestRequestDTO request = objectMapper.readValue(new String(bytes, StandardCharsets.UTF_8), AccessIngestRequestDTO.class);
                    requests.add(request);
                    if (dropFirstConnectionWithoutAck && currentConnection == 1) {
                        return;
                    }
                    byte[] responseBytes = objectMapper.writeValueAsBytes(AccessIngestResponseDTO.builder()
                            .requestId(request.getRequestId())
                            .traceId("TRACE_" + request.getRequestId())
                            .eventId("EVENT_" + request.getRequestId())
                            .status(AccessAckStatusEnum.ACCEPTED.getStatus())
                            .code(0)
                            .message("accepted")
                            .standardTopicName("pulsix.event.standard")
                            .processTimeMillis(5L)
                            .build());
                    outputStream.writeInt(responseBytes.length);
                    outputStream.write(responseBytes);
                    outputStream.flush();
                }
            } catch (SocketException ex) {
                if (!closed.get()) {
                    throw new RuntimeException(ex);
                }
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        }

        private int getPort() {
            return serverSocket.getLocalPort();
        }

        private int getConnectionCount() {
            return connectionCount.get();
        }

        private List<AccessIngestRequestDTO> awaitRequests(int expected,
                                                           long timeout,
                                                           TimeUnit timeUnit) throws Exception {
            long deadline = System.nanoTime() + timeUnit.toNanos(timeout);
            while (System.nanoTime() < deadline) {
                if (requests.size() >= expected) {
                    return new ArrayList<>(requests.subList(0, expected));
                }
                Thread.sleep(20L);
            }
            assertThat(requests).hasSizeGreaterThanOrEqualTo(expected);
            return new ArrayList<>(requests.subList(0, expected));
        }

        @Override
        public void close() throws Exception {
            closed.set(true);
            serverSocket.close();
            connectionExecutor.shutdownNow();
            acceptThread.join(1000L);
        }

    }

}
