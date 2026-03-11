package cn.liboshuai.pulsix.engine.simulation;

import cn.liboshuai.pulsix.engine.demo.DemoFixtures;
import cn.liboshuai.pulsix.engine.json.EngineJson;
import cn.liboshuai.pulsix.engine.model.ActionType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.charset.StandardCharsets;
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

}
