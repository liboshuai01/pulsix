package cn.liboshuai.pulsix.engine.simulation;

import cn.liboshuai.pulsix.engine.demo.DemoFixtures;
import cn.liboshuai.pulsix.engine.json.EngineJson;
import cn.liboshuai.pulsix.engine.model.ActionType;
import cn.liboshuai.pulsix.engine.model.RiskEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LocalSimulationRunnerTest {

    @TempDir
    Path tempDir;

    @Test
    void shouldSimulateBlacklistedEventFromFiles() throws IOException {
        LocalSimulationRunner runner = new LocalSimulationRunner();
        Path snapshotFile = tempDir.resolve("snapshot-envelope.json");
        Path eventFile = tempDir.resolve("event.json");
        Files.writeString(snapshotFile, DemoFixtures.demoEnvelopeJson());
        Files.writeString(eventFile, DemoFixtures.toJson(DemoFixtures.blacklistedEvent()));

        LocalSimulationRunner.SimulationReport report = runner.simulate(snapshotFile, eventFile);

        assertEquals("TRADE_RISK_v12", report.getSnapshotId());
        assertEquals("TRADE_RISK", report.getSceneCode());
        assertEquals(12, report.getVersion());
        assertEquals(12, report.getUsedVersion());
        assertEquals(1, report.getEventCount());
        assertEquals(ActionType.REJECT, report.getFinalResult().getFinalAction());
        assertEquals(100, report.getFinalResult().getFinalScore());
        assertEquals(List.of("设备命中黑名单"), report.getFinalResult().getHitReasons());
        assertEquals("true", report.getFinalResult().getFeatureSnapshot().get("device_in_blacklist"));
        assertEquals(List.of("R001"), report.getFinalResult().getHitRules().stream()
                .map(LocalSimulationRunner.MatchedRule::getRuleCode)
                .toList());
    }

    @Test
    void shouldApplyGlobalStreamFeatureOverridesFromFile() throws IOException {
        LocalSimulationRunner runner = new LocalSimulationRunner();
        Path snapshotFile = tempDir.resolve("snapshot-envelope.json");
        Path eventFile = tempDir.resolve("high-amount-event.json");
        Path overridesFile = tempDir.resolve("simulation-overrides.json");
        Files.writeString(snapshotFile, DemoFixtures.demoEnvelopeJson());
        Files.writeString(eventFile, DemoFixtures.toJson(highAmountEvent()));
        Files.writeString(overridesFile, """
                {
                  "streamFeatures": {
                    "user_trade_cnt_5m": 3
                  }
                }
                """);

        LocalSimulationRunner.SimulationReport report = runner.simulate(snapshotFile, eventFile, overridesFile);

        assertTrue(report.isOverridesApplied());
        assertEquals(ActionType.REVIEW, report.getFinalResult().getFinalAction());
        assertEquals(60, report.getFinalResult().getFinalScore());
        assertEquals(List.of("R002"), report.getFinalResult().getHitRules().stream()
                .map(LocalSimulationRunner.MatchedRule::getRuleCode)
                .toList());
        assertEquals("3", report.getFinalResult().getFeatureSnapshot().get("user_trade_cnt_5m"));
    }

    @Test
    void shouldApplyPerEventLookupOverrides() {
        LocalSimulationRunner runner = new LocalSimulationRunner();
        String overridesJson = """
                {
                  "eventOverrides": {
                    "1": {
                      "lookupFeatures": {
                        "device_in_blacklist": true
                      }
                    }
                  }
                }
                """;

        LocalSimulationRunner.SimulationReport report = runner.simulate(
                DemoFixtures.demoEnvelopeJson(),
                DemoFixtures.toJson(List.of(normalPassEvent("E-001", "T-001"), normalPassEvent("E-002", "T-002"))),
                overridesJson);

        assertEquals(2, report.getEventCount());
        assertEquals(ActionType.PASS, report.getResults().get(0).getFinalAction());
        assertEquals(ActionType.REJECT, report.getResults().get(1).getFinalAction());
        assertEquals(List.of("R001"), report.getResults().get(1).getHitRules().stream()
                .map(LocalSimulationRunner.MatchedRule::getRuleCode)
                .toList());
    }

    @Test
    void shouldProduceDeterministicReportForSameSnapshotAndEventArray() {
        LocalSimulationRunner runner = new LocalSimulationRunner();
        String snapshotJson = DemoFixtures.toJson(DemoFixtures.demoSnapshot());
        String eventsJson = DemoFixtures.toJson(DemoFixtures.demoEvents());

        LocalSimulationRunner.SimulationReport first = runner.simulate(snapshotJson, eventsJson);
        LocalSimulationRunner.SimulationReport second = runner.simulate(snapshotJson, eventsJson);

        assertEquals(EngineJson.write(first), EngineJson.write(second));
        assertEquals(6, first.getEventCount());
        assertEquals(ActionType.REJECT, first.getFinalResult().getFinalAction());
        assertEquals(80, first.getFinalResult().getFinalScore());
        assertEquals(List.of("R003", "R002"), first.getFinalResult().getHitRules().stream()
                .map(LocalSimulationRunner.MatchedRule::getRuleCode)
                .toList());
        assertEquals(List.of("设备1小时关联用户数=4, 用户风险等级=H", "用户5分钟交易次数=3, 当前金额=6800"),
                first.getFinalResult().getHitReasons());
        assertEquals("4", first.getFinalResult().getFeatureSnapshot().get("device_bind_user_cnt_1h"));
        assertTrue(first.getFinalResult().getTrace().contains("rule:R003=true"));
    }

    @Test
    void shouldRejectBlankEventsPayload() {
        LocalSimulationRunner runner = new LocalSimulationRunner();

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> runner.simulate(DemoFixtures.demoEnvelopeJson(), "   "));

        assertEquals("eventsJson must not be blank", exception.getMessage());
    }

    @Test
    void shouldRejectEmptyEventArrayPayload() {
        LocalSimulationRunner runner = new LocalSimulationRunner();

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> runner.simulate(DemoFixtures.demoEnvelopeJson(), "[]"));

        assertEquals("events must not be empty", exception.getMessage());
    }

    @Test
    void shouldFallbackToDemoWhenMainRunsWithoutArguments() {
        PrintStream originalOut = System.out;
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        try {
            System.setOut(new PrintStream(outputStream, true, StandardCharsets.UTF_8));

            LocalSimulationRunner.main(new String[0]);

            String output = outputStream.toString(StandardCharsets.UTF_8);
            LocalSimulationRunner.SimulationReport report = EngineJson.read(output, LocalSimulationRunner.SimulationReport.class);
            assertEquals(6, report.getEventCount());
            assertEquals(ActionType.REJECT, report.getFinalResult().getFinalAction());
            assertEquals(80, report.getFinalResult().getFinalScore());
        } finally {
            System.setOut(originalOut);
        }
    }

    private RiskEvent highAmountEvent() {
        RiskEvent event = EngineJson.read(DemoFixtures.toJson(DemoFixtures.demoEvents().get(0)), RiskEvent.class);
        event.setEventId("E-HIGH-001");
        event.setTraceId("T-HIGH-001");
        event.setAmount(new java.math.BigDecimal("6800"));
        return event;
    }

    private RiskEvent normalPassEvent(String eventId, String traceId) {
        RiskEvent event = EngineJson.read(DemoFixtures.toJson(DemoFixtures.demoEvents().get(0)), RiskEvent.class);
        event.setEventId(eventId);
        event.setTraceId(traceId);
        event.setDeviceId("D-NORMAL-001");
        event.setUserId("U2002");
        event.setAmount(new java.math.BigDecimal("800"));
        return event;
    }

}
