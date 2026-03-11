package cn.liboshuai.pulsix.engine.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

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

}
