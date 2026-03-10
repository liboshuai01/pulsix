package cn.liboshuai.pulsix.engine.json;

import cn.liboshuai.pulsix.engine.demo.DemoFixtures;
import cn.liboshuai.pulsix.engine.model.RiskEvent;
import cn.liboshuai.pulsix.engine.model.SceneSnapshot;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class EngineJsonTest {

    @AfterEach
    void tearDown() {
        EngineJson.resetCodec();
    }

    @Test
    void shouldSerializeAndDeserializeWithDefaultCodec() {
        RiskEvent event = DemoFixtures.blacklistedEvent();

        String json = EngineJson.write(event);
        RiskEvent actual = EngineJson.read(json, RiskEvent.class);

        assertEquals(event.getEventId(), actual.getEventId());
        assertEquals(event.getEventTime(), actual.getEventTime());
        assertEquals(event.getAmount(), actual.getAmount());
    }

    @Test
    void shouldKeepFixtureParsingCompatible() {
        SceneSnapshot snapshot = DemoFixtures.demoSnapshot();
        List<RiskEvent> events = DemoFixtures.demoEvents();

        assertEquals("false", snapshot.getLookupFeatures().get(0).getDefaultValue());
        assertEquals("L", snapshot.getLookupFeatures().get(1).getDefaultValue());
        assertEquals(6, events.size());
    }

    @Test
    void shouldAllowReplacingCodec() {
        JsonCodec codec = new JsonCodec() {
            @Override
            public <T> T read(String text, Class<T> type) {
                return type.cast("mock-read");
            }

            @Override
            public <T> List<T> readList(String text, Class<T> elementType) {
                return List.of(elementType.cast("mock-item"));
            }

            @Override
            public String write(Object value) {
                return "mock-write";
            }
        };

        EngineJson.setCodec(codec);

        assertSame(codec, EngineJson.codec());
        assertEquals("mock-read", EngineJson.read("ignored", String.class));
        assertEquals(List.of("mock-item"), EngineJson.readList("ignored", String.class));
        assertEquals("mock-write", EngineJson.write(new Object()));
    }

}
