package cn.liboshuai.pulsix.engine.model;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RiskEventTest {

    @Test
    void shouldRouteBySceneAndPreferredEntity() {
        RiskEvent event = new RiskEvent();
        event.setSceneCode("TRADE_RISK");
        event.setUserId("U1001");
        event.setDeviceId("D9001");
        event.setIp("1.1.1.1");

        assertEquals("TRADE_RISK|device:D9001", event.routeKey());

        event.setDeviceId(null);
        assertEquals("TRADE_RISK|user:U1001", event.routeKey());

        event.setUserId(null);
        assertEquals("TRADE_RISK|ip:1.1.1.1", event.routeKey());
    }

    @Test
    void shouldBuildDedicatedProcessingRouteKey() {
        RiskEvent event = new RiskEvent();
        event.setSceneCode("TRADE_RISK");
        event.setEventId("E1001");
        event.setTraceId("T1001");
        event.setUserId("U1001");
        event.setDeviceId("D9001");
        event.setEventTime(Instant.parse("2026-03-12T10:15:30Z"));

        String processingRouteKey = event.processingRouteKey();
        assertTrue(processingRouteKey.startsWith("TRADE_RISK|bucket:"));
        assertNotEquals(event.routeKey(), processingRouteKey);
        assertEquals(processingRouteKey, event.processingRouteKey());
    }

    @Test
    void shouldFallbackProcessingRouteKeyToDefaultScene() {
        RiskEvent event = new RiskEvent();
        event.setEventId("E1002");

        assertTrue(event.processingRouteKey().startsWith("scene:default|bucket:"));
    }

}
