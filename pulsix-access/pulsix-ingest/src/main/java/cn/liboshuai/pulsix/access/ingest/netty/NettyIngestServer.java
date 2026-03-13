package cn.liboshuai.pulsix.access.ingest.netty;

import cn.liboshuai.pulsix.access.ingest.config.PulsixIngestProperties;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.LengthFieldPrepender;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;
import io.netty.handler.timeout.IdleStateHandler;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class NettyIngestServer {

    private final PulsixIngestProperties properties;
    private final NettyIngestRequestHandler requestHandler;

    private volatile EventLoopGroup bossGroup;
    private volatile EventLoopGroup workerGroup;
    private volatile Channel serverChannel;

    public NettyIngestServer(PulsixIngestProperties properties, NettyIngestRequestHandler requestHandler) {
        this.properties = properties;
        this.requestHandler = requestHandler;
    }

    @PostConstruct
    public void autoStart() {
        if (Boolean.TRUE.equals(properties.getNetty().getEnabled())) {
            start();
        }
    }

    @PreDestroy
    public void autoStop() {
        stop();
    }

    public synchronized void start() {
        if (isRunning() || !Boolean.TRUE.equals(properties.getNetty().getEnabled())) {
            return;
        }
        try {
            bossGroup = new NioEventLoopGroup(properties.getNetty().getBossThreads());
            workerGroup = new NioEventLoopGroup(properties.getNetty().getWorkerThreads());
            ServerBootstrap bootstrap = new ServerBootstrap()
                    .group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .option(ChannelOption.SO_BACKLOG, 128)
                    .childOption(ChannelOption.SO_KEEPALIVE, true)
                    .childOption(ChannelOption.TCP_NODELAY, true)
                    .childHandler(new io.netty.channel.ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel channel) {
                            channel.pipeline()
                                    .addLast(new LengthFieldBasedFrameDecoder(properties.getNetty().getMaxFrameLength(), 0, 4, 0, 4))
                                    .addLast(new StringDecoder(StandardCharsets.UTF_8))
                                    .addLast(new IdleStateHandler(properties.getNetty().getIdleTimeoutSeconds(), 0, 0, TimeUnit.SECONDS))
                                    .addLast(new LengthFieldPrepender(4))
                                    .addLast(new StringEncoder(StandardCharsets.UTF_8))
                                    .addLast(requestHandler);
                        }
                    });
            serverChannel = bootstrap.bind(properties.getNetty().getPort()).syncUninterruptibly().channel();
            log.info("[start][Netty ingest server started on port={}]", getBoundPort());
        } catch (RuntimeException ex) {
            shutdownGroups();
            throw ex;
        }
    }

    public synchronized void stop() {
        if (!isRunning() && bossGroup == null && workerGroup == null) {
            return;
        }
        try {
            if (serverChannel != null) {
                serverChannel.close().syncUninterruptibly();
            }
        } finally {
            serverChannel = null;
            shutdownGroups();
        }
    }

    public boolean isRunning() {
        return serverChannel != null && serverChannel.isActive();
    }

    public int getBoundPort() {
        if (serverChannel == null || !(serverChannel.localAddress() instanceof InetSocketAddress inetSocketAddress)) {
            return -1;
        }
        return inetSocketAddress.getPort();
    }

    private void shutdownGroups() {
        if (bossGroup != null) {
            bossGroup.shutdownGracefully().syncUninterruptibly();
            bossGroup = null;
        }
        if (workerGroup != null) {
            workerGroup.shutdownGracefully().syncUninterruptibly();
            workerGroup = null;
        }
    }

}
