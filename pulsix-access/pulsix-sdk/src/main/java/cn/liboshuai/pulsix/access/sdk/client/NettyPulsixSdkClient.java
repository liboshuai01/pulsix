package cn.liboshuai.pulsix.access.sdk.client;

import cn.hutool.core.util.StrUtil;
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
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static cn.liboshuai.pulsix.access.sdk.enums.ErrorCodeConstants.SDK_CLIENT_NOT_STARTED;
import static cn.liboshuai.pulsix.access.sdk.enums.ErrorCodeConstants.SDK_REQUEST_INVALID;
import static cn.liboshuai.pulsix.access.sdk.enums.ErrorCodeConstants.SDK_SEND_FAILED;
import static cn.liboshuai.pulsix.access.sdk.enums.ErrorCodeConstants.SDK_TRANSPORT_NOT_READY;
import static cn.liboshuai.pulsix.framework.common.exception.util.ServiceExceptionUtil.exception;

@Slf4j
public class NettyPulsixSdkClient implements PulsixSdkClient {

    private final PulsixSdkOptions options;
    private final ObjectMapper objectMapper;
    private final ConcurrentMap<String, CompletableFuture<AccessIngestResponseDTO>> pendingRequests = new ConcurrentHashMap<>();
    private final AtomicBoolean started = new AtomicBoolean(false);

    private volatile NioEventLoopGroup eventLoopGroup;
    private volatile Channel channel;

    public NettyPulsixSdkClient(PulsixSdkOptions options) {
        this(options, createObjectMapper());
    }

    NettyPulsixSdkClient(PulsixSdkOptions options, ObjectMapper objectMapper) {
        this.options = options.validate();
        this.objectMapper = objectMapper;
    }

    @Override
    public synchronized void start() {
        if (isStarted()) {
            return;
        }
        close();
        eventLoopGroup = new NioEventLoopGroup(1);
        Bootstrap bootstrap = new Bootstrap()
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
        try {
            var connectFuture = bootstrap.connect(options.getHost(), options.getPort());
            connectFuture.awaitUninterruptibly(options.getConnectTimeoutMillis());
            if (!connectFuture.isSuccess() || connectFuture.channel() == null || !connectFuture.channel().isActive()) {
                shutdownEventLoop();
                throw exception(SDK_TRANSPORT_NOT_READY);
            }
            channel = connectFuture.channel();
            started.set(true);
        } catch (RuntimeException ex) {
            shutdownEventLoop();
            throw ex;
        }
    }

    @Override
    public boolean isStarted() {
        return started.get() && channel != null && channel.isActive();
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
        if (!started.get()) {
            return CompletableFuture.failedFuture(exception(SDK_CLIENT_NOT_STARTED));
        }
        if (channel == null || !channel.isActive()) {
            return CompletableFuture.failedFuture(exception(SDK_TRANSPORT_NOT_READY));
        }
        final AccessIngestRequestDTO actualRequest;
        try {
            actualRequest = normalizeRequest(request);
        } catch (RuntimeException ex) {
            return CompletableFuture.failedFuture(ex);
        }
        String requestId = actualRequest.getRequestId();
        CompletableFuture<AccessIngestResponseDTO> responseFuture = new CompletableFuture<>();
        pendingRequests.put(requestId, responseFuture);
        try {
            String requestJson = objectMapper.writeValueAsString(actualRequest);
            channel.writeAndFlush(requestJson).addListener((ChannelFutureListener) future -> {
                if (!future.isSuccess()) {
                    pendingRequests.remove(requestId);
                    responseFuture.completeExceptionally(exception(SDK_SEND_FAILED));
                }
            });
        } catch (Exception ex) {
            pendingRequests.remove(requestId);
            return CompletableFuture.failedFuture(exception(SDK_SEND_FAILED));
        }
        responseFuture.orTimeout(options.getRequestTimeoutMillis(), TimeUnit.MILLISECONDS)
                .whenComplete((response, throwable) -> {
                    if (throwable != null) {
                        pendingRequests.remove(requestId);
                    }
                });
        return responseFuture;
    }

    @Override
    public synchronized void close() {
        started.set(false);
        if (channel != null) {
            channel.close().awaitUninterruptibly();
            channel = null;
        }
        failPending(exception(SDK_TRANSPORT_NOT_READY));
        shutdownEventLoop();
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

    private void failPending(RuntimeException exception) {
        pendingRequests.forEach((requestId, future) -> future.completeExceptionally(exception));
        pendingRequests.clear();
    }

    private void shutdownEventLoop() {
        if (eventLoopGroup != null) {
            eventLoopGroup.shutdownGracefully().awaitUninterruptibly();
            eventLoopGroup = null;
        }
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
            CompletableFuture<AccessIngestResponseDTO> future = requestId == null ? null : pendingRequests.remove(requestId);
            if (future != null) {
                future.complete(response);
                return;
            }
            log.debug("[channelRead0][收到未知 requestId 的 SDK 响应 requestId={}]", requestId);
        }

        @Override
        public void channelInactive(ChannelHandlerContext context) {
            started.set(false);
            failPending(exception(SDK_TRANSPORT_NOT_READY));
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext context, Throwable cause) {
            log.warn("[exceptionCaught][SDK 客户端连接异常]", cause);
            started.set(false);
            failPending(exception(SDK_SEND_FAILED));
            context.close();
        }

    }

}
