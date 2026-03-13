package cn.liboshuai.pulsix.access.sdk.client;

import cn.hutool.core.util.StrUtil;
import cn.liboshuai.pulsix.access.sdk.buffer.BufferedSdkRequest;
import cn.liboshuai.pulsix.access.sdk.model.PulsixSdkSendRequest;
import cn.liboshuai.pulsix.framework.common.biz.risk.access.dto.AccessIngestRequestDTO;
import cn.liboshuai.pulsix.framework.common.biz.risk.access.dto.AccessIngestResponseDTO;
import cn.liboshuai.pulsix.framework.common.biz.risk.access.enums.AccessTransportTypeEnum;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.LengthFieldPrepender;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static cn.liboshuai.pulsix.access.sdk.enums.ErrorCodeConstants.SDK_BUFFER_FULL;
import static cn.liboshuai.pulsix.access.sdk.enums.ErrorCodeConstants.SDK_CLIENT_NOT_STARTED;
import static cn.liboshuai.pulsix.access.sdk.enums.ErrorCodeConstants.SDK_REQUEST_INVALID;
import static cn.liboshuai.pulsix.access.sdk.enums.ErrorCodeConstants.SDK_RETRY_EXHAUSTED;
import static cn.liboshuai.pulsix.access.sdk.enums.ErrorCodeConstants.SDK_SEND_FAILED;
import static cn.liboshuai.pulsix.access.sdk.enums.ErrorCodeConstants.SDK_TRANSPORT_NOT_READY;
import static cn.liboshuai.pulsix.framework.common.exception.util.ServiceExceptionUtil.exception;

@Slf4j
public class NettyPulsixSdkClient implements PulsixSdkClient {

    private final PulsixSdkOptions options;
    private final ObjectMapper objectMapper;
    private final LinkedBlockingDeque<BufferedSdkRequest> bufferQueue;
    private final ConcurrentMap<String, BufferedSdkRequest> inflightRequests = new ConcurrentHashMap<>();
    private final AtomicBoolean running = new AtomicBoolean(false);
    private final AtomicBoolean pumping = new AtomicBoolean(false);

    private volatile NioEventLoopGroup eventLoopGroup;
    private volatile Bootstrap bootstrap;
    private volatile ScheduledExecutorService scheduler;
    private volatile Channel channel;
    private volatile long lastReconnectAttemptMillis;

    public NettyPulsixSdkClient(PulsixSdkOptions options) {
        this(options, createObjectMapper());
    }

    NettyPulsixSdkClient(PulsixSdkOptions options, ObjectMapper objectMapper) {
        this.options = options.validate();
        this.objectMapper = objectMapper;
        this.bufferQueue = new LinkedBlockingDeque<>(this.options.getMaxBufferSize());
    }

    @Override
    public synchronized void start() {
        if (running.get()) {
            return;
        }
        initializeTransport();
        running.set(true);
        startPump();
        boolean connected = connectIfNecessary(true);
        if (!connected && Boolean.FALSE.equals(options.getAutoReconnect())) {
            close();
            throw exception(SDK_TRANSPORT_NOT_READY);
        }
    }

    @Override
    public boolean isStarted() {
        return running.get();
    }

    @Override
    public CompletableFuture<AccessIngestResponseDTO> sendAsync(PulsixSdkSendRequest request) {
        try {
            return sendAsync(convertRequest(request));
        } catch (RuntimeException ex) {
            return CompletableFuture.failedFuture(ex);
        }
    }

    @Override
    public CompletableFuture<AccessIngestResponseDTO> sendAsync(AccessIngestRequestDTO request) {
        if (!running.get()) {
            return CompletableFuture.failedFuture(exception(SDK_CLIENT_NOT_STARTED));
        }
        if (!isChannelReady() && Boolean.FALSE.equals(options.getAutoReconnect())) {
            return CompletableFuture.failedFuture(exception(SDK_TRANSPORT_NOT_READY));
        }
        final AccessIngestRequestDTO actualRequest;
        try {
            actualRequest = normalizeRequest(request);
        } catch (RuntimeException ex) {
            return CompletableFuture.failedFuture(ex);
        }
        BufferedSdkRequest bufferedRequest = new BufferedSdkRequest(actualRequest);
        if (!bufferQueue.offerLast(bufferedRequest)) {
            return CompletableFuture.failedFuture(exception(SDK_BUFFER_FULL));
        }
        if (isChannelReady()) {
            pumpSafely();
        }
        return bufferedRequest.getFuture();
    }

    @Override
    public synchronized void close() {
        running.set(false);
        shutdownScheduler();
        closeChannel();
        failInflight(exception(SDK_TRANSPORT_NOT_READY));
        failBuffered(exception(SDK_TRANSPORT_NOT_READY));
        shutdownEventLoop();
        bootstrap = null;
        lastReconnectAttemptMillis = 0L;
    }

    private void initializeTransport() {
        eventLoopGroup = new NioEventLoopGroup(1);
        bootstrap = new Bootstrap()
                .group(eventLoopGroup)
                .channel(NioSocketChannel.class)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, options.getConnectTimeoutMillis())
                .option(ChannelOption.SO_KEEPALIVE, true)
                .option(ChannelOption.TCP_NODELAY, true)
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) {
                        ch.pipeline()
                                .addLast(new LengthFieldBasedFrameDecoder(256 * 1024, 0, 4, 0, 4))
                                .addLast(new StringDecoder(StandardCharsets.UTF_8))
                                .addLast(new LengthFieldPrepender(4))
                                .addLast(new StringEncoder(StandardCharsets.UTF_8))
                                .addLast(new ClientResponseHandler());
                    }
                });
    }

    private void startPump() {
        scheduler = Executors.newSingleThreadScheduledExecutor(runnable -> {
            Thread thread = new Thread(runnable, "pulsix-sdk-pump");
            thread.setDaemon(true);
            return thread;
        });
        scheduler.scheduleWithFixedDelay(this::pumpSafely,
                0,
                options.getBatchFlushIntervalMillis(),
                TimeUnit.MILLISECONDS);
    }

    private void pumpSafely() {
        if (!running.get() || !pumping.compareAndSet(false, true)) {
            return;
        }
        try {
            handleInflightTimeouts();
            if (!isChannelReady()) {
                if (Boolean.TRUE.equals(options.getAutoReconnect())) {
                    connectIfNecessary(false);
                }
                if (!isChannelReady()) {
                    return;
                }
            }
            flushBufferedRequests();
        } catch (Exception ex) {
            log.warn("[pumpSafely][SDK 批量发送循环异常]", ex);
        } finally {
            pumping.set(false);
        }
    }

    private boolean connectIfNecessary(boolean force) {
        if (!running.get() || isChannelReady() || bootstrap == null) {
            return isChannelReady();
        }
        long now = System.currentTimeMillis();
        if (!force && now - lastReconnectAttemptMillis < options.getReconnectIntervalMillis()) {
            return false;
        }
        lastReconnectAttemptMillis = now;
        try {
            ChannelFuture connectFuture = bootstrap.connect(options.getHost(), options.getPort());
            connectFuture.awaitUninterruptibly(options.getConnectTimeoutMillis());
            if (connectFuture.isSuccess() && connectFuture.channel() != null && connectFuture.channel().isActive()) {
                channel = connectFuture.channel();
                return true;
            }
            if (connectFuture.channel() != null) {
                connectFuture.channel().close().awaitUninterruptibly();
            }
        } catch (RuntimeException ex) {
            log.debug("[connectIfNecessary][SDK 重连失败 host={} port={}]", options.getHost(), options.getPort(), ex);
        }
        return false;
    }

    private boolean isChannelReady() {
        return channel != null && channel.isActive();
    }

    private void flushBufferedRequests() {
        while (running.get() && isChannelReady()) {
            List<BufferedSdkRequest> batch = pollBatch();
            if (batch.isEmpty()) {
                return;
            }
            sendBatch(batch);
            if (batch.size() < options.getMaxBatchSize()) {
                return;
            }
        }
    }

    private List<BufferedSdkRequest> pollBatch() {
        List<BufferedSdkRequest> batch = new ArrayList<>(options.getMaxBatchSize());
        while (batch.size() < options.getMaxBatchSize()) {
            BufferedSdkRequest request = bufferQueue.pollFirst();
            if (request == null) {
                break;
            }
            if (request.getFuture().isDone()) {
                continue;
            }
            batch.add(request);
        }
        return batch;
    }

    private void sendBatch(List<BufferedSdkRequest> batch) {
        Channel activeChannel = channel;
        if (activeChannel == null || !activeChannel.isActive()) {
            requeueBatchToFront(batch);
            return;
        }
        List<AbstractMap.SimpleImmutableEntry<BufferedSdkRequest, String>> payloads = new ArrayList<>(batch.size());
        for (BufferedSdkRequest request : batch) {
            if (request.getFuture().isDone()) {
                continue;
            }
            try {
                payloads.add(new AbstractMap.SimpleImmutableEntry<>(request, objectMapper.writeValueAsString(request.getRequest())));
            } catch (Exception ex) {
                request.getFuture().completeExceptionally(exception(SDK_SEND_FAILED));
            }
        }
        if (payloads.isEmpty()) {
            return;
        }
        long now = System.currentTimeMillis();
        for (int index = 0; index < payloads.size(); index++) {
            AbstractMap.SimpleImmutableEntry<BufferedSdkRequest, String> payload = payloads.get(index);
            BufferedSdkRequest request = payload.getKey();
            request.markSent(now);
            inflightRequests.put(request.getRequestId(), request);
            ChannelFuture future = index == payloads.size() - 1
                    ? activeChannel.writeAndFlush(payload.getValue())
                    : activeChannel.write(payload.getValue());
            if (index == payloads.size() - 1) {
                future.addListener((ChannelFutureListener) sendFuture -> {
                    if (!sendFuture.isSuccess()) {
                        recoverBatch(payloads, exception(SDK_SEND_FAILED));
                        closeChannel();
                    }
                });
            }
        }
    }

    private void handleInflightTimeouts() {
        long now = System.currentTimeMillis();
        List<BufferedSdkRequest> timeoutRequests = new ArrayList<>();
        for (BufferedSdkRequest request : inflightRequests.values()) {
            if (request.getFuture().isDone()) {
                inflightRequests.remove(request.getRequestId(), request);
                continue;
            }
            if (request.getLastSendTimeMillis() <= 0) {
                continue;
            }
            if (now - request.getLastSendTimeMillis() >= options.getRequestTimeoutMillis()
                    && inflightRequests.remove(request.getRequestId(), request)) {
                timeoutRequests.add(request);
            }
        }
        for (BufferedSdkRequest request : timeoutRequests) {
            retryOrFail(request, exception(SDK_SEND_FAILED));
        }
    }

    private void recoverBatch(List<AbstractMap.SimpleImmutableEntry<BufferedSdkRequest, String>> payloads,
                              RuntimeException failure) {
        List<BufferedSdkRequest> requests = new ArrayList<>(payloads.size());
        for (AbstractMap.SimpleImmutableEntry<BufferedSdkRequest, String> payload : payloads) {
            requests.add(payload.getKey());
        }
        recoverRequests(requests, failure);
    }

    private void recoverRequests(List<BufferedSdkRequest> requests, RuntimeException failure) {
        for (BufferedSdkRequest request : requests) {
            if (inflightRequests.remove(request.getRequestId(), request)) {
                retryOrFail(request, failure);
            }
        }
    }

    private void retryOrFail(BufferedSdkRequest request, RuntimeException failure) {
        if (request.getFuture().isDone()) {
            return;
        }
        if (!running.get()) {
            request.getFuture().completeExceptionally(failure);
            return;
        }
        if (!Boolean.TRUE.equals(options.getAutoReconnect()) && !isChannelReady()) {
            request.getFuture().completeExceptionally(failure);
            return;
        }
        if (request.canRetry(options.getMaxRetryCount())) {
            if (bufferQueue.offerFirst(request)) {
                return;
            }
            request.getFuture().completeExceptionally(exception(SDK_BUFFER_FULL));
            return;
        }
        request.getFuture().completeExceptionally(exception(SDK_RETRY_EXHAUSTED));
    }

    private void requeueBatchToFront(List<BufferedSdkRequest> batch) {
        for (int index = batch.size() - 1; index >= 0; index--) {
            BufferedSdkRequest request = batch.get(index);
            if (!request.getFuture().isDone() && !bufferQueue.offerFirst(request)) {
                request.getFuture().completeExceptionally(exception(SDK_BUFFER_FULL));
            }
        }
    }

    private void failInflight(RuntimeException failure) {
        List<BufferedSdkRequest> requests = new ArrayList<>(inflightRequests.values());
        inflightRequests.clear();
        for (BufferedSdkRequest request : requests) {
            request.getFuture().completeExceptionally(failure);
        }
    }

    private void failBuffered(RuntimeException failure) {
        BufferedSdkRequest request;
        while ((request = bufferQueue.pollFirst()) != null) {
            request.getFuture().completeExceptionally(failure);
        }
    }

    private void closeChannel() {
        Channel activeChannel = channel;
        channel = null;
        if (activeChannel != null) {
            ChannelFuture closeFuture = activeChannel.close();
            if (!activeChannel.eventLoop().inEventLoop()) {
                closeFuture.awaitUninterruptibly();
            }
        }
    }

    private void shutdownScheduler() {
        if (scheduler != null) {
            scheduler.shutdownNow();
            scheduler = null;
        }
    }

    private void shutdownEventLoop() {
        if (eventLoopGroup != null) {
            eventLoopGroup.shutdownGracefully().awaitUninterruptibly();
            eventLoopGroup = null;
        }
    }

    private AccessIngestRequestDTO convertRequest(PulsixSdkSendRequest request) {
        PulsixSdkSendRequest actualRequest = request == null ? null : request.validate();
        if (actualRequest == null) {
            throw exception(SDK_REQUEST_INVALID, "request 不能为空");
        }
        Map<String, String> metadata = new LinkedHashMap<>(actualRequest.getMetadata());
        metadata.put("sceneCode", actualRequest.getSceneCode());
        metadata.put("eventCode", actualRequest.getEventCode());
        return AccessIngestRequestDTO.builder()
                .requestId(actualRequest.getRequestId())
                .sourceCode(options.getSourceCode())
                .transportType(AccessTransportTypeEnum.SDK.getType())
                .payload(actualRequest.getPayload())
                .metadata(metadata)
                .build();
    }

    private AccessIngestRequestDTO normalizeRequest(AccessIngestRequestDTO request) {
        if (request == null) {
            throw exception(SDK_REQUEST_INVALID, "request 不能为空");
        }
        Map<String, String> metadata = request.getMetadata() == null ? new LinkedHashMap<>() : new LinkedHashMap<>(request.getMetadata());
        String sceneCode = getMetadata(metadata, "sceneCode");
        String eventCode = getMetadata(metadata, "eventCode");
        if (StrUtil.isBlank(sceneCode)) {
            throw exception(SDK_REQUEST_INVALID, "sceneCode 不能为空");
        }
        if (StrUtil.isBlank(eventCode)) {
            throw exception(SDK_REQUEST_INVALID, "eventCode 不能为空");
        }
        if (StrUtil.isBlank(request.getPayload())) {
            throw exception(SDK_REQUEST_INVALID, "payload 不能为空");
        }
        return AccessIngestRequestDTO.builder()
                .requestId(defaultIfBlank(request.getRequestId(), UUID.randomUUID().toString()))
                .sourceCode(defaultIfBlank(request.getSourceCode(), options.getSourceCode()))
                .transportType(AccessTransportTypeEnum.SDK.getType())
                .payload(request.getPayload())
                .metadata(metadata)
                .sendTimeMillis(request.getSendTimeMillis() == null ? System.currentTimeMillis() : request.getSendTimeMillis())
                .remoteAddress(request.getRemoteAddress())
                .build();
    }

    private String getMetadata(Map<String, String> metadata, String key) {
        String exactValue = metadata.get(key);
        if (StrUtil.isNotBlank(exactValue)) {
            return exactValue;
        }
        return metadata.get(key.toLowerCase());
    }

    private String defaultIfBlank(String value, String defaultValue) {
        return StrUtil.isBlank(value) ? defaultValue : StrUtil.trim(value);
    }

    private static ObjectMapper createObjectMapper() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        return objectMapper;
    }

    private final class ClientResponseHandler extends SimpleChannelInboundHandler<String> {

        @Override
        protected void channelRead0(ChannelHandlerContext context, String responseJson) throws Exception {
            AccessIngestResponseDTO response = objectMapper.readValue(responseJson, AccessIngestResponseDTO.class);
            String requestId = StrUtil.trim(response.getRequestId());
            BufferedSdkRequest request = requestId == null ? null : inflightRequests.remove(requestId);
            if (request != null) {
                request.getFuture().complete(response);
                return;
            }
            log.debug("[channelRead0][收到未知 requestId 的 SDK 响应 requestId={}]", requestId);
        }

        @Override
        public void channelInactive(ChannelHandlerContext context) {
            channel = null;
            recoverRequests(new ArrayList<>(inflightRequests.values()), exception(SDK_TRANSPORT_NOT_READY));
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext context, Throwable cause) {
            log.warn("[exceptionCaught][SDK 客户端连接异常]", cause);
            channel = null;
            recoverRequests(new ArrayList<>(inflightRequests.values()), exception(SDK_SEND_FAILED));
            context.close();
        }

    }

}
