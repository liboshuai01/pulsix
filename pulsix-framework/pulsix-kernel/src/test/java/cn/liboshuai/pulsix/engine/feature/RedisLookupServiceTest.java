package cn.liboshuai.pulsix.engine.feature;

import cn.liboshuai.pulsix.engine.context.EvalContext;
import cn.liboshuai.pulsix.engine.model.EngineType;
import cn.liboshuai.pulsix.engine.model.FeatureType;
import cn.liboshuai.pulsix.engine.model.LookupFeatureSpec;
import cn.liboshuai.pulsix.engine.model.LookupType;
import cn.liboshuai.pulsix.engine.model.RiskEvent;
import cn.liboshuai.pulsix.engine.runtime.CompiledSceneRuntime;
import cn.liboshuai.pulsix.engine.script.DefaultScriptCompiler;
import com.github.fppt.jedismock.RedisServer;
import org.junit.jupiter.api.Test;
import redis.clients.jedis.Jedis;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RedisLookupServiceTest {

    @Test
    void shouldUseFreshCacheThenStaleCacheWhenRedisBecomesUnavailable() throws Exception {
        MutableClock clock = new MutableClock(Instant.parse("2026-03-12T00:00:00Z"));
        RedisServer server = RedisServer.newRedisServer().start();
        try {
            try (Jedis jedis = new Jedis(server.getHost(), server.getBindPort())) {
                jedis.set("pulsix:list:black:device:D9001", "1");
            }
            RedisLookupService service = new RedisLookupService(
                    new RedisLookupConfig(server.getHost(), server.getBindPort(), 0, null, null, false, 200, 200),
                    clock);
            CompiledSceneRuntime.CompiledLookupFeature feature = compiledLookupFeature(
                    "device_in_blacklist",
                    LookupType.REDIS_SET,
                    "deviceId",
                    "pulsix:list:black:device",
                    "BOOLEAN",
                    false,
                    200,
                    30);

            LookupResult first = service.lookup(feature, context(event("U1001", "D9001")));
            assertEquals(Boolean.TRUE, first.getValue());
            assertFalse(first.hasError());

            server.stop();

            clock.advance(Duration.ofSeconds(10));
            LookupResult freshCacheHit = service.lookup(feature, context(event("U1001", "D9001")));
            assertEquals(Boolean.TRUE, freshCacheHit.getValue());
            assertFalse(freshCacheHit.hasError());

            clock.advance(Duration.ofSeconds(31));
            LookupResult staleCacheFallback = service.lookup(feature, context(event("U1001", "D9001")));
            assertEquals(Boolean.TRUE, staleCacheFallback.getValue());
            assertEquals(LookupResult.ERROR_CONNECTION_FAILED, staleCacheFallback.getErrorCode());
            assertEquals(LookupResult.FALLBACK_CACHE_VALUE, staleCacheFallback.getFallbackMode());
        } finally {
            if (server.isRunning()) {
                server.stop();
            }
        }
    }

    @Test
    void shouldReturnHashValueAndFallbackToDefaultWhenValueMissing() throws Exception {
        RedisServer server = RedisServer.newRedisServer().start();
        try {
            try (Jedis jedis = new Jedis(server.getHost(), server.getBindPort())) {
                jedis.hset("pulsix:profile:user:risk", "U1001", "H");
            }
            RedisLookupService service = new RedisLookupService(
                    new RedisLookupConfig(server.getHost(), server.getBindPort(), 0, null, null, false, 200, 200));
            CompiledSceneRuntime.CompiledLookupFeature feature = compiledLookupFeature(
                    "user_risk_level",
                    LookupType.REDIS_HASH,
                    "userId",
                    "pulsix:profile:user:risk",
                    "STRING",
                    "L",
                    200,
                    0);

            LookupResult hit = service.lookup(feature, context(event("U1001", "D0001")));
            assertEquals("H", hit.getValue());
            assertFalse(hit.hasError());

            LookupResult miss = service.lookup(feature, context(event("U4040", "D0001")));
            assertEquals("L", miss.getValue());
            assertEquals(LookupResult.ERROR_VALUE_MISSING, miss.getErrorCode());
            assertEquals(LookupResult.FALLBACK_DEFAULT_VALUE, miss.getFallbackMode());
        } finally {
            if (server.isRunning()) {
                server.stop();
            }
        }
    }

    @Test
    void shouldFallbackToDefaultWhenRedisLookupTimesOut() throws Exception {
        try (HangingRedisServer server = new HangingRedisServer()) {
            RedisLookupService service = new RedisLookupService(
                    new RedisLookupConfig("127.0.0.1", server.port(), 0, null, null, false, 50, 50));
            CompiledSceneRuntime.CompiledLookupFeature feature = compiledLookupFeature(
                    "device_in_blacklist",
                    LookupType.REDIS_SET,
                    "deviceId",
                    "pulsix:list:black:device",
                    "BOOLEAN",
                    false,
                    50,
                    0);

            LookupResult timedOut = service.lookup(feature, context(event("U1001", "D9001")));
            assertEquals(Boolean.FALSE, timedOut.getValue());
            assertEquals(LookupResult.ERROR_TIMEOUT, timedOut.getErrorCode());
            assertEquals(LookupResult.FALLBACK_DEFAULT_VALUE, timedOut.getFallbackMode());
        }
    }

    private CompiledSceneRuntime.CompiledLookupFeature compiledLookupFeature(String code,
                                                                             LookupType lookupType,
                                                                             String keyExpr,
                                                                             String sourceRef,
                                                                             String valueType,
                                                                             Object defaultValue,
                                                                             Integer timeoutMs,
                                                                             Integer cacheTtlSeconds) {
        LookupFeatureSpec spec = new LookupFeatureSpec();
        spec.setCode(code);
        spec.setName(code);
        spec.setType(FeatureType.LOOKUP);
        spec.setLookupType(lookupType);
        spec.setKeyExpr(keyExpr);
        spec.setSourceRef(sourceRef);
        spec.setValueType(valueType);
        spec.setDefaultValue(defaultValue);
        spec.setTimeoutMs(timeoutMs);
        spec.setCacheTtlSeconds(cacheTtlSeconds);

        CompiledSceneRuntime.CompiledLookupFeature feature = new CompiledSceneRuntime.CompiledLookupFeature();
        feature.setSpec(spec);
        feature.setKeyScript(new DefaultScriptCompiler().compile(EngineType.AVIATOR, keyExpr));
        return feature;
    }

    private EvalContext context(RiskEvent event) {
        EvalContext context = new EvalContext();
        context.setEvent(event);
        context.getValues().putAll(event.toFlatMap());
        return context;
    }

    private RiskEvent event(String userId, String deviceId) {
        RiskEvent event = new RiskEvent();
        event.setUserId(userId);
        event.setDeviceId(deviceId);
        event.setEventId("E-LOOKUP-001");
        event.setTraceId("T-LOOKUP-001");
        event.setSceneCode("TRADE_RISK");
        event.setEventType("trade");
        event.setEventTime(Instant.parse("2026-03-12T00:00:00Z"));
        return event;
    }

    private static final class MutableClock extends Clock {

        private Instant current;

        private MutableClock(Instant current) {
            this.current = current;
        }

        @Override
        public ZoneId getZone() {
            return ZoneId.of("UTC");
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return current;
        }

        private void advance(Duration duration) {
            current = current.plus(duration);
        }
    }

    private static final class HangingRedisServer implements AutoCloseable {

        private final ServerSocket serverSocket;

        private final Thread acceptThread;

        private HangingRedisServer() throws IOException {
            this.serverSocket = new ServerSocket(0);
            this.acceptThread = new Thread(this::acceptAndHang, "hanging-redis-server");
            this.acceptThread.setDaemon(true);
            this.acceptThread.start();
        }

        private int port() {
            return serverSocket.getLocalPort();
        }

        private void acceptAndHang() {
            try (Socket socket = serverSocket.accept()) {
                while (!Thread.currentThread().isInterrupted() && !socket.isClosed()) {
                    Thread.sleep(1_000L);
                }
            } catch (Exception ignored) {
            }
        }

        @Override
        public void close() throws IOException {
            acceptThread.interrupt();
            serverSocket.close();
        }
    }

}
