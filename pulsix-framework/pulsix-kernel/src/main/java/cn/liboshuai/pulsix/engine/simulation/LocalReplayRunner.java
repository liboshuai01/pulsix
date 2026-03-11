package cn.liboshuai.pulsix.engine.simulation;

import cn.liboshuai.pulsix.engine.json.EngineJson;
import cn.liboshuai.pulsix.engine.model.ActionType;
import cn.liboshuai.pulsix.engine.model.RiskEvent;
import cn.liboshuai.pulsix.engine.model.SceneSnapshotEnvelope;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class LocalReplayRunner {

    private static final String EVENT_ORDER = "EVENT_TIME_ASC";

    private final LocalSimulationRunner simulationRunner;

    public LocalReplayRunner() {
        this(new LocalSimulationRunner());
    }

    public LocalReplayRunner(LocalSimulationRunner simulationRunner) {
        this.simulationRunner = Objects.requireNonNull(simulationRunner, "simulationRunner");
    }

    public static void main(String[] args) {
        CliOptions options = CliOptions.parse(args);
        LocalReplayRunner runner = new LocalReplayRunner();
        Object output = switch (options.mode()) {
            case REPLAY -> runner.replay(options.snapshotPath(), options.eventsPath());
            case DIFF -> runner.diff(options.baselineSnapshotPath(), options.candidateSnapshotPath(), options.eventsPath());
            case CAPTURE_GOLDEN -> {
                ReplayGoldenCase goldenCase = runner.captureGoldenCase(options.caseId(),
                        options.description(),
                        options.snapshotPath(),
                        options.eventsPath());
                runner.writeGoldenCase(options.goldenCasePath(), goldenCase);
                yield goldenCase;
            }
            case VERIFY_GOLDEN -> runner.verifyGoldenCase(options.snapshotPath(), options.eventsPath(), options.goldenCasePath());
        };
        String outputJson = EngineJson.write(output);
        if (options.outputPath() != null) {
            writeText(options.outputPath(), outputJson);
            return;
        }
        System.out.println(outputJson);
    }

    public ReplayReport replay(Path snapshotPath, Path eventsPath) {
        return replay(readText(snapshotPath), readText(eventsPath));
    }

    public ReplayReport replay(String snapshotJson, String eventsJson) {
        return replay(simulationRunner.readSnapshotEnvelope(snapshotJson), simulationRunner.readEvents(eventsJson));
    }

    public ReplayReport replay(SceneSnapshotEnvelope envelope, List<RiskEvent> events) {
        List<RiskEvent> orderedEvents = orderEvents(events);
        LocalSimulationRunner.SimulationReport simulationReport = simulationRunner.simulate(envelope, orderedEvents);
        ReplayReport report = new ReplayReport();
        report.setSnapshotId(simulationReport.getSnapshotId());
        report.setSceneCode(simulationReport.getSceneCode());
        report.setVersion(simulationReport.getVersion());
        report.setChecksum(simulationReport.getChecksum());
        report.setEventCount(simulationReport.getEventCount());
        report.setEventOrder(EVENT_ORDER);
        report.setSummary(buildSummary(simulationReport.getResults()));
        report.setResults(simulationReport.getResults());
        report.setFinalResult(simulationReport.getFinalResult());
        return report;
    }

    public ReplayDiffReport diff(Path baselineSnapshotPath, Path candidateSnapshotPath, Path eventsPath) {
        String eventsJson = readText(eventsPath);
        return diff(readText(baselineSnapshotPath), readText(candidateSnapshotPath), eventsJson);
    }

    public ReplayDiffReport diff(String baselineSnapshotJson, String candidateSnapshotJson, String eventsJson) {
        SceneSnapshotEnvelope baselineEnvelope = simulationRunner.readSnapshotEnvelope(baselineSnapshotJson);
        SceneSnapshotEnvelope candidateEnvelope = simulationRunner.readSnapshotEnvelope(candidateSnapshotJson);
        List<RiskEvent> events = simulationRunner.readEvents(eventsJson);
        return diff(baselineEnvelope, candidateEnvelope, events);
    }

    public ReplayDiffReport diff(SceneSnapshotEnvelope baselineEnvelope,
                                 SceneSnapshotEnvelope candidateEnvelope,
                                 List<RiskEvent> events) {
        ReplayReport baselineReport = replay(baselineEnvelope, events);
        ReplayReport candidateReport = replay(candidateEnvelope, events);
        return buildDiff(baselineReport, candidateReport);
    }

    public ReplayGoldenCase captureGoldenCase(String caseId, String description, Path snapshotPath, Path eventsPath) {
        return captureGoldenCase(caseId, description, replay(snapshotPath, eventsPath));
    }

    public ReplayGoldenCase captureGoldenCase(String caseId, String description, ReplayReport report) {
        ReplayReport actualReport = Objects.requireNonNull(report, "report");
        ReplayGoldenCase goldenCase = new ReplayGoldenCase();
        goldenCase.setCaseId(requireText(caseId, "caseId"));
        goldenCase.setDescription(description);
        goldenCase.setSceneCode(actualReport.getSceneCode());
        goldenCase.setVersion(actualReport.getVersion());
        goldenCase.setChecksum(actualReport.getChecksum());
        goldenCase.setEventCount(actualReport.getEventCount());
        goldenCase.setExpectedSummary(actualReport.getSummary());

        List<GoldenEventExpectation> expectedEvents = new ArrayList<>(actualReport.getResults().size());
        for (LocalSimulationRunner.SimulationEventResult result : actualReport.getResults()) {
            GoldenEventExpectation expectation = new GoldenEventExpectation();
            expectation.setEventIndex(result.getEventIndex());
            expectation.setEventId(result.getEventId());
            expectation.setTraceId(result.getTraceId());
            expectation.setFinalAction(result.getFinalAction());
            expectation.setFinalScore(result.getFinalScore());
            expectation.setHitRuleCodes(result.getHitRules().stream()
                    .map(LocalSimulationRunner.MatchedRule::getRuleCode)
                    .toList());
            expectation.setHitReasons(result.getHitReasons());
            expectedEvents.add(expectation);
        }
        goldenCase.setExpectedEvents(expectedEvents);
        return goldenCase;
    }

    public ReplayGoldenCase readGoldenCase(Path goldenCasePath) {
        return readGoldenCase(readText(goldenCasePath));
    }

    public ReplayGoldenCase readGoldenCase(String goldenCaseJson) {
        return EngineJson.read(requireText(goldenCaseJson, "goldenCaseJson"), ReplayGoldenCase.class);
    }

    public void writeGoldenCase(Path goldenCasePath, ReplayGoldenCase goldenCase) {
        writeText(goldenCasePath, EngineJson.write(goldenCase));
    }

    public GoldenCaseVerification verifyGoldenCase(Path snapshotPath, Path eventsPath, Path goldenCasePath) {
        ReplayReport actualReport = replay(snapshotPath, eventsPath);
        ReplayGoldenCase goldenCase = readGoldenCase(goldenCasePath);
        return verifyGoldenCase(actualReport, goldenCase);
    }

    public GoldenCaseVerification verifyGoldenCase(ReplayReport actualReport, ReplayGoldenCase goldenCase) {
        ReplayReport report = Objects.requireNonNull(actualReport, "actualReport");
        ReplayGoldenCase expected = Objects.requireNonNull(goldenCase, "goldenCase");
        String mismatch = findGoldenMismatch(report, expected);
        if (mismatch != null) {
            throw new IllegalStateException("golden case drifted: caseId=" + expected.getCaseId() + ", " + mismatch);
        }
        GoldenCaseVerification verification = new GoldenCaseVerification();
        verification.setCaseId(expected.getCaseId());
        verification.setMatched(true);
        verification.setSceneCode(report.getSceneCode());
        verification.setVersion(report.getVersion());
        verification.setChecksum(report.getChecksum());
        verification.setEventCount(report.getEventCount());
        verification.setActualSummary(report.getSummary());
        return verification;
    }

    private ReplaySummary buildSummary(List<LocalSimulationRunner.SimulationEventResult> results) {
        EnumMap<ActionType, Integer> actionCounter = new EnumMap<>(ActionType.class);
        int matchedEventCount = 0;
        for (LocalSimulationRunner.SimulationEventResult result : results) {
            if (result.getFinalAction() != null) {
                actionCounter.merge(result.getFinalAction(), 1, Integer::sum);
            }
            if (!result.getHitRules().isEmpty()) {
                matchedEventCount++;
            }
        }
        ReplaySummary summary = new ReplaySummary();
        Map<String, Integer> finalActionCounts = new LinkedHashMap<>();
        for (ActionType actionType : ActionType.values()) {
            Integer count = actionCounter.get(actionType);
            if (count != null && count > 0) {
                finalActionCounts.put(actionType.name(), count);
            }
        }
        summary.setFinalActionCounts(finalActionCounts);
        summary.setMatchedEventCount(matchedEventCount);
        return summary;
    }

    private ReplayDiffReport buildDiff(ReplayReport baselineReport, ReplayReport candidateReport) {
        if (baselineReport.getEventCount() == null || !baselineReport.getEventCount().equals(candidateReport.getEventCount())) {
            throw new IllegalArgumentException("baseline and candidate event counts must match");
        }
        ReplayDiffReport diffReport = new ReplayDiffReport();
        diffReport.setSceneCode(firstNonBlank(candidateReport.getSceneCode(), baselineReport.getSceneCode()));
        diffReport.setEventCount(baselineReport.getEventCount());
        diffReport.setBaseline(toSnapshotRef(baselineReport));
        diffReport.setCandidate(toSnapshotRef(candidateReport));
        diffReport.setBaselineSummary(baselineReport.getSummary());
        diffReport.setCandidateSummary(candidateReport.getSummary());

        List<ReplayEventDiff> differences = new ArrayList<>();
        for (int index = 0; index < baselineReport.getResults().size(); index++) {
            LocalSimulationRunner.SimulationEventResult baselineResult = baselineReport.getResults().get(index);
            LocalSimulationRunner.SimulationEventResult candidateResult = candidateReport.getResults().get(index);
            List<String> changeTypes = detectChangeTypes(baselineResult, candidateResult);
            if (changeTypes.isEmpty()) {
                continue;
            }
            ReplayEventDiff eventDiff = new ReplayEventDiff();
            eventDiff.setEventIndex(baselineResult.getEventIndex());
            eventDiff.setEventId(firstNonBlank(baselineResult.getEventId(), candidateResult.getEventId()));
            eventDiff.setTraceId(firstNonBlank(baselineResult.getTraceId(), candidateResult.getTraceId()));
            eventDiff.setChangeTypes(changeTypes);
            eventDiff.setBaselineResult(baselineResult);
            eventDiff.setCandidateResult(candidateResult);
            differences.add(eventDiff);
        }
        diffReport.setDifferences(differences);
        diffReport.setChangedEventCount(differences.size());
        return diffReport;
    }

    private SnapshotRef toSnapshotRef(ReplayReport report) {
        SnapshotRef snapshotRef = new SnapshotRef();
        snapshotRef.setSnapshotId(report.getSnapshotId());
        snapshotRef.setVersion(report.getVersion());
        snapshotRef.setChecksum(report.getChecksum());
        return snapshotRef;
    }

    private List<String> detectChangeTypes(LocalSimulationRunner.SimulationEventResult baselineResult,
                                           LocalSimulationRunner.SimulationEventResult candidateResult) {
        List<String> changeTypes = new ArrayList<>();
        if (!Objects.equals(baselineResult.getFinalAction(), candidateResult.getFinalAction())) {
            changeTypes.add("FINAL_ACTION");
        }
        if (!Objects.equals(baselineResult.getFinalScore(), candidateResult.getFinalScore())) {
            changeTypes.add("FINAL_SCORE");
        }
        if (!Objects.equals(ruleCodesOf(baselineResult), ruleCodesOf(candidateResult))) {
            changeTypes.add("HIT_RULES");
        }
        if (!Objects.equals(baselineResult.getHitReasons(), candidateResult.getHitReasons())) {
            changeTypes.add("HIT_REASONS");
        }
        if (!Objects.equals(baselineResult.getFeatureSnapshot(), candidateResult.getFeatureSnapshot())) {
            changeTypes.add("FEATURE_SNAPSHOT");
        }
        if (!Objects.equals(baselineResult.getTrace(), candidateResult.getTrace())) {
            changeTypes.add("TRACE");
        }
        return changeTypes;
    }

    private List<RiskEvent> orderEvents(List<RiskEvent> events) {
        if (events == null || events.isEmpty()) {
            throw new IllegalArgumentException("events must not be empty");
        }
        List<RiskEvent> normalizedEvents = new ArrayList<>(events.size());
        for (RiskEvent event : events) {
            if (event == null) {
                throw new IllegalArgumentException("event must not be null");
            }
            normalizedEvents.add(event);
        }
        List<IndexedRiskEvent> indexedEvents = new ArrayList<>(normalizedEvents.size());
        for (int index = 0; index < normalizedEvents.size(); index++) {
            indexedEvents.add(new IndexedRiskEvent(index, normalizedEvents.get(index)));
        }
        indexedEvents.sort(Comparator
                .comparing((IndexedRiskEvent item) -> item.event().getEventTime(), Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(item -> normalizeSortValue(item.event().getEventId()))
                .thenComparing(item -> normalizeSortValue(item.event().getTraceId()))
                .thenComparingInt(IndexedRiskEvent::index));
        List<RiskEvent> orderedEvents = new ArrayList<>(indexedEvents.size());
        for (IndexedRiskEvent indexedEvent : indexedEvents) {
            orderedEvents.add(indexedEvent.event());
        }
        return orderedEvents;
    }

    private String findGoldenMismatch(ReplayReport actualReport, ReplayGoldenCase goldenCase) {
        if (!Objects.equals(goldenCase.getSceneCode(), actualReport.getSceneCode())) {
            return "expected sceneCode=" + goldenCase.getSceneCode() + ", actual sceneCode=" + actualReport.getSceneCode();
        }
        if (!Objects.equals(goldenCase.getVersion(), actualReport.getVersion())) {
            return "expected version=" + goldenCase.getVersion() + ", actual version=" + actualReport.getVersion();
        }
        if (!Objects.equals(goldenCase.getChecksum(), actualReport.getChecksum())) {
            return "expected checksum=" + goldenCase.getChecksum() + ", actual checksum=" + actualReport.getChecksum();
        }
        if (!Objects.equals(goldenCase.getEventCount(), actualReport.getEventCount())) {
            return "expected eventCount=" + goldenCase.getEventCount() + ", actual eventCount=" + actualReport.getEventCount();
        }
        if (!Objects.equals(goldenCase.getExpectedSummary(), actualReport.getSummary())) {
            return "expected summary=" + EngineJson.write(goldenCase.getExpectedSummary())
                    + ", actual summary=" + EngineJson.write(actualReport.getSummary());
        }
        if (goldenCase.getExpectedEvents().size() != actualReport.getResults().size()) {
            return "expected expectedEvents.size=" + goldenCase.getExpectedEvents().size()
                    + ", actual results.size=" + actualReport.getResults().size();
        }
        for (int index = 0; index < goldenCase.getExpectedEvents().size(); index++) {
            GoldenEventExpectation expectedEvent = goldenCase.getExpectedEvents().get(index);
            LocalSimulationRunner.SimulationEventResult actualEvent = actualReport.getResults().get(index);
            String mismatch = findGoldenEventMismatch(expectedEvent, actualEvent);
            if (mismatch != null) {
                return mismatch;
            }
        }
        return null;
    }

    private String findGoldenEventMismatch(GoldenEventExpectation expectedEvent,
                                           LocalSimulationRunner.SimulationEventResult actualEvent) {
        Integer eventIndex = expectedEvent.getEventIndex();
        if (!Objects.equals(expectedEvent.getEventIndex(), actualEvent.getEventIndex())) {
            return "eventIndex=" + eventIndex + ": expected eventIndex=" + expectedEvent.getEventIndex()
                    + ", actual eventIndex=" + actualEvent.getEventIndex();
        }
        if (!Objects.equals(expectedEvent.getEventId(), actualEvent.getEventId())) {
            return "eventIndex=" + eventIndex + ": expected eventId=" + expectedEvent.getEventId()
                    + ", actual eventId=" + actualEvent.getEventId();
        }
        if (!Objects.equals(expectedEvent.getTraceId(), actualEvent.getTraceId())) {
            return "eventIndex=" + eventIndex + ": expected traceId=" + expectedEvent.getTraceId()
                    + ", actual traceId=" + actualEvent.getTraceId();
        }
        if (!Objects.equals(expectedEvent.getFinalAction(), actualEvent.getFinalAction())) {
            return "eventIndex=" + eventIndex + ": expected finalAction=" + expectedEvent.getFinalAction()
                    + ", actual finalAction=" + actualEvent.getFinalAction();
        }
        if (!Objects.equals(expectedEvent.getFinalScore(), actualEvent.getFinalScore())) {
            return "eventIndex=" + eventIndex + ": expected finalScore=" + expectedEvent.getFinalScore()
                    + ", actual finalScore=" + actualEvent.getFinalScore();
        }
        List<String> actualRuleCodes = ruleCodesOf(actualEvent);
        if (!Objects.equals(expectedEvent.getHitRuleCodes(), actualRuleCodes)) {
            return "eventIndex=" + eventIndex + ": expected hitRuleCodes=" + expectedEvent.getHitRuleCodes()
                    + ", actual hitRuleCodes=" + actualRuleCodes;
        }
        if (!Objects.equals(expectedEvent.getHitReasons(), actualEvent.getHitReasons())) {
            return "eventIndex=" + eventIndex + ": expected hitReasons=" + expectedEvent.getHitReasons()
                    + ", actual hitReasons=" + actualEvent.getHitReasons();
        }
        return null;
    }

    private List<String> ruleCodesOf(LocalSimulationRunner.SimulationEventResult result) {
        return result.getHitRules().stream()
                .map(LocalSimulationRunner.MatchedRule::getRuleCode)
                .toList();
    }

    private String normalizeSortValue(String value) {
        return value == null ? "" : value;
    }

    private String firstNonBlank(String preferred, String fallback) {
        if (preferred != null && !preferred.isBlank()) {
            return preferred;
        }
        return fallback;
    }

    private String requireText(String text, String fieldName) {
        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return text.trim();
    }

    private String readText(Path path) {
        if (path == null) {
            throw new IllegalArgumentException("path must not be null");
        }
        try {
            return Files.readString(path);
        } catch (IOException exception) {
            throw new IllegalStateException("read file failed: " + path, exception);
        }
    }

    private static void writeText(Path path, String text) {
        try {
            Path parent = path.toAbsolutePath().getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            Files.writeString(path, text);
        } catch (IOException exception) {
            throw new IllegalStateException("write file failed: " + path, exception);
        }
    }

    private record IndexedRiskEvent(int index, RiskEvent event) {
    }

    @Data
    @NoArgsConstructor
    public static class ReplayReport {

        private String snapshotId;

        private String sceneCode;

        private Integer version;

        private String checksum;

        private Integer eventCount;

        private String eventOrder;

        private ReplaySummary summary;

        private LocalSimulationRunner.SimulationEventResult finalResult;

        private List<LocalSimulationRunner.SimulationEventResult> results = new ArrayList<>();

        public void setResults(List<LocalSimulationRunner.SimulationEventResult> results) {
            this.results = results == null ? new ArrayList<>() : new ArrayList<>(results);
        }
    }

    @Data
    @NoArgsConstructor
    public static class ReplaySummary {

        private Map<String, Integer> finalActionCounts = new LinkedHashMap<>();

        private Integer matchedEventCount;

        public void setFinalActionCounts(Map<String, Integer> finalActionCounts) {
            this.finalActionCounts = finalActionCounts == null ? new LinkedHashMap<>() : new LinkedHashMap<>(finalActionCounts);
        }
    }

    @Data
    @NoArgsConstructor
    public static class ReplayDiffReport {

        private String sceneCode;

        private Integer eventCount;

        private SnapshotRef baseline;

        private SnapshotRef candidate;

        private ReplaySummary baselineSummary;

        private ReplaySummary candidateSummary;

        private Integer changedEventCount;

        private List<ReplayEventDiff> differences = new ArrayList<>();

        public void setDifferences(List<ReplayEventDiff> differences) {
            this.differences = differences == null ? new ArrayList<>() : new ArrayList<>(differences);
        }
    }

    @Data
    @NoArgsConstructor
    public static class SnapshotRef {

        private String snapshotId;

        private Integer version;

        private String checksum;
    }

    @Data
    @NoArgsConstructor
    public static class ReplayEventDiff {

        private Integer eventIndex;

        private String eventId;

        private String traceId;

        private List<String> changeTypes = new ArrayList<>();

        private LocalSimulationRunner.SimulationEventResult baselineResult;

        private LocalSimulationRunner.SimulationEventResult candidateResult;

        public void setChangeTypes(List<String> changeTypes) {
            this.changeTypes = changeTypes == null ? new ArrayList<>() : new ArrayList<>(changeTypes);
        }
    }

    @Data
    @NoArgsConstructor
    public static class ReplayGoldenCase {

        private String caseId;

        private String description;

        private String sceneCode;

        private Integer version;

        private String checksum;

        private Integer eventCount;

        private ReplaySummary expectedSummary;

        private List<GoldenEventExpectation> expectedEvents = new ArrayList<>();

        public void setExpectedEvents(List<GoldenEventExpectation> expectedEvents) {
            this.expectedEvents = expectedEvents == null ? new ArrayList<>() : new ArrayList<>(expectedEvents);
        }
    }

    @Data
    @NoArgsConstructor
    public static class GoldenEventExpectation {

        private Integer eventIndex;

        private String eventId;

        private String traceId;

        private ActionType finalAction;

        private Integer finalScore;

        private List<String> hitRuleCodes = new ArrayList<>();

        private List<String> hitReasons = new ArrayList<>();

        public void setHitRuleCodes(List<String> hitRuleCodes) {
            this.hitRuleCodes = hitRuleCodes == null ? new ArrayList<>() : new ArrayList<>(hitRuleCodes);
        }

        public void setHitReasons(List<String> hitReasons) {
            this.hitReasons = hitReasons == null ? new ArrayList<>() : new ArrayList<>(hitReasons);
        }
    }

    @Data
    @NoArgsConstructor
    public static class GoldenCaseVerification {

        private String caseId;

        private boolean matched;

        private String sceneCode;

        private Integer version;

        private String checksum;

        private Integer eventCount;

        private ReplaySummary actualSummary;
    }

    private enum CliMode {

        REPLAY("replay"),
        DIFF("diff"),
        CAPTURE_GOLDEN("capture-golden"),
        VERIFY_GOLDEN("verify-golden");

        private final String optionValue;

        CliMode(String optionValue) {
            this.optionValue = optionValue;
        }

        private static CliMode parse(String text) {
            String value = text == null ? null : text.trim();
            for (CliMode mode : values()) {
                if (mode.optionValue.equals(value)) {
                    return mode;
                }
            }
            throw new IllegalArgumentException("unknown mode: " + text + System.lineSeparator() + CliOptions.usage());
        }
    }

    private record CliOptions(CliMode mode,
                              Path snapshotPath,
                              Path baselineSnapshotPath,
                              Path candidateSnapshotPath,
                              Path eventsPath,
                              Path goldenCasePath,
                              String caseId,
                              String description,
                              Path outputPath) {

        private static CliOptions parse(String[] args) {
            if (args == null || args.length == 0) {
                throw new IllegalArgumentException(usage());
            }
            CliMode mode = null;
            Path snapshotPath = null;
            Path baselineSnapshotPath = null;
            Path candidateSnapshotPath = null;
            Path eventsPath = null;
            Path goldenCasePath = null;
            String caseId = null;
            String description = null;
            Path outputPath = null;
            for (int index = 0; index < args.length; index++) {
                String arg = args[index];
                switch (arg) {
                    case "--mode" -> mode = CliMode.parse(requireValue(args, ++index, "--mode"));
                    case "--snapshot-file" -> snapshotPath = Path.of(requireValue(args, ++index, "--snapshot-file"));
                    case "--baseline-snapshot-file" -> baselineSnapshotPath = Path.of(requireValue(args, ++index, "--baseline-snapshot-file"));
                    case "--candidate-snapshot-file" -> candidateSnapshotPath = Path.of(requireValue(args, ++index, "--candidate-snapshot-file"));
                    case "--events-file" -> eventsPath = Path.of(requireValue(args, ++index, "--events-file"));
                    case "--golden-file" -> goldenCasePath = Path.of(requireValue(args, ++index, "--golden-file"));
                    case "--case-id" -> caseId = requireValue(args, ++index, "--case-id");
                    case "--description" -> description = requireValue(args, ++index, "--description");
                    case "--output-file" -> outputPath = Path.of(requireValue(args, ++index, "--output-file"));
                    case "--help", "-h" -> throw new IllegalArgumentException(usage());
                    default -> throw new IllegalArgumentException("unknown argument: " + arg + System.lineSeparator() + usage());
                }
            }
            if (mode == null) {
                throw new IllegalArgumentException("--mode is required" + System.lineSeparator() + usage());
            }
            return switch (mode) {
                case REPLAY -> new CliOptions(mode,
                        requirePath(snapshotPath, "--snapshot-file"),
                        null,
                        null,
                        requirePath(eventsPath, "--events-file"),
                        null,
                        null,
                        null,
                        outputPath);
                case DIFF -> new CliOptions(mode,
                        null,
                        requirePath(baselineSnapshotPath, "--baseline-snapshot-file"),
                        requirePath(candidateSnapshotPath, "--candidate-snapshot-file"),
                        requirePath(eventsPath, "--events-file"),
                        null,
                        null,
                        null,
                        outputPath);
                case CAPTURE_GOLDEN -> new CliOptions(mode,
                        requirePath(snapshotPath, "--snapshot-file"),
                        null,
                        null,
                        requirePath(eventsPath, "--events-file"),
                        requirePath(goldenCasePath, "--golden-file"),
                        requireText(caseId, "--case-id"),
                        description,
                        outputPath);
                case VERIFY_GOLDEN -> new CliOptions(mode,
                        requirePath(snapshotPath, "--snapshot-file"),
                        null,
                        null,
                        requirePath(eventsPath, "--events-file"),
                        requirePath(goldenCasePath, "--golden-file"),
                        null,
                        null,
                        outputPath);
            };
        }

        private static Path requirePath(Path path, String optionName) {
            if (path == null) {
                throw new IllegalArgumentException(optionName + " is required" + System.lineSeparator() + usage());
            }
            return path;
        }

        private static String requireValue(String[] args, int index, String optionName) {
            if (index >= args.length) {
                throw new IllegalArgumentException("missing value for " + optionName + System.lineSeparator() + usage());
            }
            return args[index];
        }

        private static String requireText(String text, String optionName) {
            if (text == null || text.isBlank()) {
                throw new IllegalArgumentException(optionName + " is required" + System.lineSeparator() + usage());
            }
            return text;
        }

        private static String usage() {
            return "Usage: LocalReplayRunner --mode <replay|diff|capture-golden|verify-golden> "
                    + "[--snapshot-file <path>] [--baseline-snapshot-file <path>] [--candidate-snapshot-file <path>] "
                    + "--events-file <path> [--golden-file <path>] [--case-id <id>] [--description <text>] [--output-file <path>]";
        }
    }

}
