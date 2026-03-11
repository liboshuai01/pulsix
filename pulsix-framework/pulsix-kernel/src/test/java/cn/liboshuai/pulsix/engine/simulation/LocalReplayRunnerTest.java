package cn.liboshuai.pulsix.engine.simulation;

import cn.liboshuai.pulsix.engine.demo.DemoFixtures;
import cn.liboshuai.pulsix.engine.json.EngineJson;
import cn.liboshuai.pulsix.engine.model.ActionType;
import cn.liboshuai.pulsix.engine.model.RiskEvent;
import cn.liboshuai.pulsix.engine.model.RuleSpec;
import cn.liboshuai.pulsix.engine.model.SceneSnapshot;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LocalReplayRunnerTest {

    @TempDir
    Path tempDir;

    @Test
    void shouldReplayOutOfOrderEventsFromFiles() throws IOException {
        LocalReplayRunner runner = new LocalReplayRunner();
        Path snapshotFile = tempDir.resolve("trade-risk-v12.json");
        Path eventsFile = tempDir.resolve("trade-risk-events.json");
        List<RiskEvent> outOfOrderEvents = new ArrayList<>(DemoFixtures.demoEvents());
        java.util.Collections.reverse(outOfOrderEvents);
        Files.writeString(snapshotFile, DemoFixtures.demoEnvelopeJson());
        Files.writeString(eventsFile, DemoFixtures.toJson(outOfOrderEvents));

        LocalReplayRunner.ReplayReport report = runner.replay(snapshotFile, eventsFile);

        assertEquals("TRADE_RISK_v12", report.getSnapshotId());
        assertEquals("EVENT_TIME_ASC", report.getEventOrder());
        assertEquals(6, report.getEventCount());
        assertEquals(3, report.getSummary().getFinalActionCounts().get("PASS"));
        assertEquals(3, report.getSummary().getFinalActionCounts().get("REJECT"));
        assertEquals(3, report.getSummary().getMatchedEventCount());
        assertEquals("E202603070001", report.getResults().get(0).getEventId());
        assertEquals("E202603070006", report.getFinalResult().getEventId());
        assertEquals(ActionType.REJECT, report.getFinalResult().getFinalAction());
        assertEquals(80, report.getFinalResult().getFinalScore());
        assertEquals(List.of("R003", "R002"), report.getFinalResult().getHitRules().stream()
                .map(LocalSimulationRunner.MatchedRule::getRuleCode)
                .toList());
    }

    @Test
    void shouldDiffReplayResultsBetweenTwoSnapshotVersions() throws IOException {
        LocalReplayRunner runner = new LocalReplayRunner();
        Path baselineSnapshotFile = tempDir.resolve("trade-risk-v12-snapshot.json");
        Path candidateSnapshotFile = tempDir.resolve("trade-risk-v13-disable-r001.json");
        Path eventsFile = tempDir.resolve("blacklisted-event.json");
        Files.writeString(baselineSnapshotFile, DemoFixtures.toJson(DemoFixtures.demoSnapshot()));
        Files.writeString(candidateSnapshotFile, DemoFixtures.toJson(disableRule(copySnapshot(DemoFixtures.demoSnapshot()), "R001")));
        Files.writeString(eventsFile, DemoFixtures.toJson(DemoFixtures.blacklistedEvent()));

        LocalReplayRunner.ReplayDiffReport diffReport = runner.diff(baselineSnapshotFile, candidateSnapshotFile, eventsFile);

        assertEquals("TRADE_RISK", diffReport.getSceneCode());
        assertEquals(1, diffReport.getEventCount());
        assertEquals(12, diffReport.getBaseline().getVersion());
        assertEquals(13, diffReport.getCandidate().getVersion());
        assertEquals(1, diffReport.getChangedEventCount());
        assertEquals(1, diffReport.getBaselineSummary().getFinalActionCounts().get("REJECT"));
        assertEquals(1, diffReport.getCandidateSummary().getFinalActionCounts().get("PASS"));
        assertTrue(diffReport.getDifferences().get(0).getChangeTypes().contains("FINAL_ACTION"));
        assertTrue(diffReport.getDifferences().get(0).getChangeTypes().contains("HIT_RULES"));
        assertEquals(ActionType.REJECT, diffReport.getDifferences().get(0).getBaselineResult().getFinalAction());
        assertEquals(ActionType.PASS, diffReport.getDifferences().get(0).getCandidateResult().getFinalAction());
        assertEquals(List.of("R001"), diffReport.getDifferences().get(0).getBaselineResult().getHitRules().stream()
                .map(LocalSimulationRunner.MatchedRule::getRuleCode)
                .toList());
        assertEquals(List.of(), diffReport.getDifferences().get(0).getCandidateResult().getHitRules().stream()
                .map(LocalSimulationRunner.MatchedRule::getRuleCode)
                .toList());
    }

    @Test
    void shouldCaptureAndVerifyFixedGoldenCase() throws IOException {
        LocalReplayRunner runner = new LocalReplayRunner();
        Path snapshotFile = tempDir.resolve("trade-risk-v12-envelope.json");
        Path eventsFile = tempDir.resolve("trade-risk-events.json");
        Path goldenFile = tempDir.resolve("trade-risk-v12-golden.json");
        Files.writeString(snapshotFile, DemoFixtures.demoEnvelopeJson());
        Files.writeString(eventsFile, DemoFixtures.toJson(DemoFixtures.demoEvents()));

        LocalReplayRunner.ReplayGoldenCase goldenCase = runner.captureGoldenCase("trade-risk-demo-v12",
                "TRADE_RISK v12 demo event golden case",
                snapshotFile,
                eventsFile);

        assertEquals(expectedGoldenCase(), goldenCase);
        runner.writeGoldenCase(goldenFile, goldenCase);

        LocalReplayRunner.GoldenCaseVerification verification = runner.verifyGoldenCase(snapshotFile, eventsFile, goldenFile);

        assertEquals("trade-risk-demo-v12", verification.getCaseId());
        assertTrue(verification.isMatched());
        assertEquals(6, verification.getEventCount());
        assertEquals(goldenCase.getExpectedSummary(), verification.getActualSummary());
    }

    @Test
    void shouldFailWhenGoldenCaseDrifts() throws IOException {
        LocalReplayRunner runner = new LocalReplayRunner();
        Path snapshotFile = tempDir.resolve("trade-risk-v12-envelope.json");
        Path eventsFile = tempDir.resolve("trade-risk-events.json");
        Path goldenFile = tempDir.resolve("trade-risk-v12-bad-golden.json");
        Files.writeString(snapshotFile, DemoFixtures.demoEnvelopeJson());
        Files.writeString(eventsFile, DemoFixtures.toJson(DemoFixtures.demoEvents()));
        LocalReplayRunner.ReplayGoldenCase badGoldenCase = expectedGoldenCase();
        badGoldenCase.getExpectedEvents().get(5).setFinalAction(ActionType.PASS);
        Files.writeString(goldenFile, EngineJson.write(badGoldenCase));

        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> runner.verifyGoldenCase(snapshotFile, eventsFile, goldenFile));

        assertTrue(exception.getMessage().contains("golden case drifted"));
        assertTrue(exception.getMessage().contains("caseId=trade-risk-demo-v12"));
        assertTrue(exception.getMessage().contains("eventIndex=5"));
        assertTrue(exception.getMessage().contains("expected finalAction=PASS"));
    }

    @Test
    void shouldWriteReplayReportWhenMainRunsReplayMode() throws IOException {
        Path snapshotFile = tempDir.resolve("trade-risk-v12-envelope.json");
        Path eventsFile = tempDir.resolve("trade-risk-events.json");
        Path outputFile = tempDir.resolve("replay-report.json");
        Files.writeString(snapshotFile, DemoFixtures.demoEnvelopeJson());
        Files.writeString(eventsFile, DemoFixtures.toJson(DemoFixtures.demoEvents()));

        LocalReplayRunner.main(new String[]{
                "--mode", "replay",
                "--snapshot-file", snapshotFile.toString(),
                "--events-file", eventsFile.toString(),
                "--output-file", outputFile.toString()
        });

        LocalReplayRunner.ReplayReport report = EngineJson.read(Files.readString(outputFile), LocalReplayRunner.ReplayReport.class);
        assertEquals(6, report.getEventCount());
        assertEquals(ActionType.REJECT, report.getFinalResult().getFinalAction());
        assertEquals(80, report.getFinalResult().getFinalScore());
    }

    @Test
    void shouldWriteDiffReportWhenMainRunsDiffMode() throws IOException {
        Path baselineSnapshotFile = tempDir.resolve("trade-risk-v12-snapshot.json");
        Path candidateSnapshotFile = tempDir.resolve("trade-risk-v13-disable-r001.json");
        Path eventsFile = tempDir.resolve("blacklisted-event.json");
        Path outputFile = tempDir.resolve("replay-diff-report.json");
        Files.writeString(baselineSnapshotFile, DemoFixtures.toJson(DemoFixtures.demoSnapshot()));
        Files.writeString(candidateSnapshotFile, DemoFixtures.toJson(disableRule(copySnapshot(DemoFixtures.demoSnapshot()), "R001")));
        Files.writeString(eventsFile, DemoFixtures.toJson(DemoFixtures.blacklistedEvent()));

        LocalReplayRunner.main(new String[]{
                "--mode", "diff",
                "--baseline-snapshot-file", baselineSnapshotFile.toString(),
                "--candidate-snapshot-file", candidateSnapshotFile.toString(),
                "--events-file", eventsFile.toString(),
                "--output-file", outputFile.toString()
        });

        LocalReplayRunner.ReplayDiffReport diffReport = EngineJson.read(Files.readString(outputFile), LocalReplayRunner.ReplayDiffReport.class);
        assertEquals(1, diffReport.getChangedEventCount());
        assertEquals(ActionType.REJECT, diffReport.getDifferences().get(0).getBaselineResult().getFinalAction());
        assertEquals(ActionType.PASS, diffReport.getDifferences().get(0).getCandidateResult().getFinalAction());
    }

    @Test
    void shouldWriteGoldenAndVerificationWhenMainRunsGoldenModes() throws IOException {
        Path snapshotFile = tempDir.resolve("trade-risk-v12-envelope.json");
        Path eventsFile = tempDir.resolve("trade-risk-events.json");
        Path goldenFile = tempDir.resolve("trade-risk-v12-golden.json");
        Path captureFile = tempDir.resolve("trade-risk-v12-capture.json");
        Path verificationFile = tempDir.resolve("trade-risk-v12-verify.json");
        Files.writeString(snapshotFile, DemoFixtures.demoEnvelopeJson());
        Files.writeString(eventsFile, DemoFixtures.toJson(DemoFixtures.demoEvents()));

        LocalReplayRunner.main(new String[]{
                "--mode", "capture-golden",
                "--snapshot-file", snapshotFile.toString(),
                "--events-file", eventsFile.toString(),
                "--golden-file", goldenFile.toString(),
                "--case-id", "trade-risk-demo-v12",
                "--description", "TRADE_RISK v12 demo event golden case",
                "--output-file", captureFile.toString()
        });

        LocalReplayRunner.ReplayGoldenCase goldenCase = EngineJson.read(Files.readString(goldenFile), LocalReplayRunner.ReplayGoldenCase.class);
        assertEquals(expectedGoldenCase(), goldenCase);
        assertEquals(expectedGoldenCase(), EngineJson.read(Files.readString(captureFile), LocalReplayRunner.ReplayGoldenCase.class));

        LocalReplayRunner.main(new String[]{
                "--mode", "verify-golden",
                "--snapshot-file", snapshotFile.toString(),
                "--events-file", eventsFile.toString(),
                "--golden-file", goldenFile.toString(),
                "--output-file", verificationFile.toString()
        });

        LocalReplayRunner.GoldenCaseVerification verification = EngineJson.read(Files.readString(verificationFile), LocalReplayRunner.GoldenCaseVerification.class);
        assertTrue(verification.isMatched());
        assertEquals(6, verification.getEventCount());
        assertEquals("trade-risk-demo-v12", verification.getCaseId());
    }

    private SceneSnapshot copySnapshot(SceneSnapshot snapshot) {
        return EngineJson.read(EngineJson.write(snapshot), SceneSnapshot.class);
    }

    private SceneSnapshot disableRule(SceneSnapshot snapshot, String ruleCode) {
        snapshot.setSnapshotId("TRADE_RISK_v13");
        snapshot.setVersion(13);
        snapshot.setChecksum("8d2041a7cf8f47b4b6b0f91d2ab8d9d1");
        for (RuleSpec rule : snapshot.getRules()) {
            if (ruleCode.equals(rule.getCode())) {
                rule.setEnabled(false);
            }
        }
        return snapshot;
    }

    private LocalReplayRunner.ReplayGoldenCase expectedGoldenCase() {
        LocalReplayRunner.ReplayGoldenCase goldenCase = new LocalReplayRunner.ReplayGoldenCase();
        goldenCase.setCaseId("trade-risk-demo-v12");
        goldenCase.setDescription("TRADE_RISK v12 demo event golden case");
        goldenCase.setSceneCode("TRADE_RISK");
        goldenCase.setVersion(12);
        goldenCase.setChecksum("8d2041a7cf8f47b4b6b0f91d2ab8d9d0");
        goldenCase.setEventCount(6);
        goldenCase.setExpectedSummary(expectedSummary());
        goldenCase.setExpectedEvents(List.of(
                expectedEvent(0, "E202603070001", "T202603070001", ActionType.PASS, 0, List.of(), List.of()),
                expectedEvent(1, "E202603070002", "T202603070002", ActionType.PASS, 0, List.of(), List.of()),
                expectedEvent(2, "E202603070003", "T202603070003", ActionType.PASS, 0, List.of(), List.of()),
                expectedEvent(3, "E202603070004", "T202603070004", ActionType.REJECT, 80, List.of("R003"),
                        List.of("设备1小时关联用户数=4, 用户风险等级=M")),
                expectedEvent(4, "E202603070005", "T202603070005", ActionType.REJECT, 80, List.of("R003"),
                        List.of("设备1小时关联用户数=4, 用户风险等级=H")),
                expectedEvent(5, "E202603070006", "T202603070006", ActionType.REJECT, 80, List.of("R003", "R002"),
                        List.of("设备1小时关联用户数=4, 用户风险等级=H", "用户5分钟交易次数=3, 当前金额=6800"))
        ));
        return goldenCase;
    }

    private LocalReplayRunner.ReplaySummary expectedSummary() {
        LocalReplayRunner.ReplaySummary summary = new LocalReplayRunner.ReplaySummary();
        LinkedHashMap<String, Integer> actionCounts = new LinkedHashMap<>();
        actionCounts.put("PASS", 3);
        actionCounts.put("REJECT", 3);
        summary.setFinalActionCounts(actionCounts);
        summary.setMatchedEventCount(3);
        return summary;
    }

    private LocalReplayRunner.GoldenEventExpectation expectedEvent(int eventIndex,
                                                                   String eventId,
                                                                   String traceId,
                                                                   ActionType finalAction,
                                                                   int finalScore,
                                                                   List<String> hitRuleCodes,
                                                                   List<String> hitReasons) {
        LocalReplayRunner.GoldenEventExpectation expectedEvent = new LocalReplayRunner.GoldenEventExpectation();
        expectedEvent.setEventIndex(eventIndex);
        expectedEvent.setEventId(eventId);
        expectedEvent.setTraceId(traceId);
        expectedEvent.setFinalAction(finalAction);
        expectedEvent.setFinalScore(finalScore);
        expectedEvent.setHitRuleCodes(hitRuleCodes);
        expectedEvent.setHitReasons(hitReasons);
        return expectedEvent;
    }

}
