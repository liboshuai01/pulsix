package cn.liboshuai.pulsix.access.ingest.netty;

import cn.hutool.core.util.StrUtil;
import cn.liboshuai.pulsix.access.ingest.service.IngestPipelineService;
import cn.liboshuai.pulsix.access.ingest.service.metrics.IngestMetricsService;
import cn.liboshuai.pulsix.framework.common.biz.risk.access.dto.AccessIngestRequestDTO;
import cn.liboshuai.pulsix.framework.common.biz.risk.access.dto.AccessIngestResponseDTO;
import cn.liboshuai.pulsix.framework.common.biz.risk.access.enums.AccessAckStatusEnum;
import cn.liboshuai.pulsix.framework.common.biz.risk.access.enums.AccessTransportTypeEnum;
import cn.liboshuai.pulsix.framework.common.exception.enums.GlobalErrorCodeConstants;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.timeout.IdleStateEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.LinkedHashMap;

import static cn.liboshuai.pulsix.access.ingest.enums.ErrorCodeConstants.INGEST_REQUEST_INVALID;

@Component
@ChannelHandler.Sharable
@Slf4j
public class NettyIngestRequestHandler extends SimpleChannelInboundHandler<String> {

    private final IngestPipelineService ingestPipelineService;
    private final ObjectMapper objectMapper;
    private final IngestMetricsService ingestMetricsService;

    public NettyIngestRequestHandler(IngestPipelineService ingestPipelineService,
                                     ObjectMapper objectMapper,
                                     IngestMetricsService ingestMetricsService) {
        this.ingestPipelineService = ingestPipelineService;
        this.objectMapper = objectMapper;
        this.ingestMetricsService = ingestMetricsService;
    }

    @Override
    public void channelActive(ChannelHandlerContext context) throws Exception {
        ingestMetricsService.recordNettyConnectionOpened();
        super.channelActive(context);
    }

    @Override
    protected void channelRead0(ChannelHandlerContext context, String frameText) {
        String requestId = null;
        try {
            AccessIngestRequestDTO request = objectMapper.readValue(frameText, AccessIngestRequestDTO.class);
            requestId = request.getRequestId();
            enrichRequest(context, request);
            writeResponse(context, ingestPipelineService.ingest(request), false);
        } catch (JsonProcessingException ex) {
            log.warn("[channelRead0][SDK 请求帧不是合法 JSON]", ex);
            writeResponse(context, reject(requestId, INGEST_REQUEST_INVALID.getCode(), "SDK 请求帧不是合法 JSON"), false);
        } catch (Exception ex) {
            log.warn("[channelRead0][SDK 请求处理异常 requestId={}]", requestId, ex);
            writeResponse(context, reject(requestId, GlobalErrorCodeConstants.INTERNAL_SERVER_ERROR.getCode(), "SDK 服务端处理失败"), false);
        }
    }

    @Override
    public void channelInactive(ChannelHandlerContext context) throws Exception {
        ingestMetricsService.recordNettyConnectionClosed();
        super.channelInactive(context);
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext context, Object event) throws Exception {
        if (event instanceof IdleStateEvent) {
            log.debug("[userEventTriggered][关闭空闲 SDK 连接 remoteAddress={}]", resolveRemoteAddress(context));
            context.close();
            return;
        }
        super.userEventTriggered(context, event);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext context, Throwable cause) {
        log.warn("[exceptionCaught][SDK 连接异常 remoteAddress={}]", resolveRemoteAddress(context), cause);
        writeResponse(context, reject(null, GlobalErrorCodeConstants.BAD_REQUEST.getCode(), "SDK 请求帧处理失败"), true);
    }

    private void enrichRequest(ChannelHandlerContext context, AccessIngestRequestDTO request) {
        if (request.getMetadata() == null) {
            request.setMetadata(new LinkedHashMap<>());
        }
        request.setTransportType(AccessTransportTypeEnum.SDK.getType());
        if (request.getSendTimeMillis() == null) {
            request.setSendTimeMillis(System.currentTimeMillis());
        }
        if (StrUtil.isBlank(request.getRemoteAddress())) {
            request.setRemoteAddress(resolveRemoteAddress(context));
        }
    }

    private String resolveRemoteAddress(ChannelHandlerContext context) {
        SocketAddress remoteAddress = context.channel().remoteAddress();
        if (remoteAddress instanceof InetSocketAddress inetSocketAddress) {
            if (inetSocketAddress.getAddress() != null) {
                return inetSocketAddress.getAddress().getHostAddress();
            }
            return inetSocketAddress.getHostString();
        }
        return remoteAddress == null ? null : remoteAddress.toString();
    }

    private AccessIngestResponseDTO reject(String requestId, Integer code, String message) {
        return AccessIngestResponseDTO.builder()
                .requestId(StrUtil.trim(requestId))
                .status(AccessAckStatusEnum.REJECTED.getStatus())
                .code(code)
                .message(message)
                .processTimeMillis(0L)
                .build();
    }

    private void writeResponse(ChannelHandlerContext context, AccessIngestResponseDTO response, boolean closeAfterWrite) {
        try {
            String responseJson = objectMapper.writeValueAsString(response);
            if (closeAfterWrite) {
                context.writeAndFlush(responseJson).addListener(ChannelFutureListener.CLOSE);
            } else {
                context.writeAndFlush(responseJson);
            }
        } catch (JsonProcessingException ex) {
            log.warn("[writeResponse][序列化 SDK 响应失败]", ex);
            context.close();
        }
    }

}
