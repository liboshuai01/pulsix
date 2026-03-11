package cn.liboshuai.pulsix.engine.simulation;

import cn.liboshuai.pulsix.engine.core.DecisionExecutor;
import cn.liboshuai.pulsix.engine.core.LocalDecisionEngine;
import cn.liboshuai.pulsix.engine.demo.DemoFixtures;
import cn.liboshuai.pulsix.engine.feature.InMemoryLookupService;
import cn.liboshuai.pulsix.engine.feature.InMemoryStreamFeatureStateStore;
import cn.liboshuai.pulsix.engine.feature.LookupService;
import cn.liboshuai.pulsix.engine.feature.StreamFeatureStateStore;
import cn.liboshuai.pulsix.engine.json.EngineJson;
import cn.liboshuai.pulsix.engine.model.ActionType;
import cn.liboshuai.pulsix.engine.model.DecisionResult;
import cn.liboshuai.pulsix.engine.model.RiskEvent;
import cn.liboshuai.pulsix.engine.model.RuleHit;
import cn.liboshuai.pulsix.engine.model.SceneSnapshot;
import cn.liboshuai.pulsix.engine.model.SceneSnapshotEnvelope;
import cn.liboshuai.pulsix.engine.runtime.RuntimeCompiler;
import cn.liboshuai.pulsix.engine.runtime.SceneRuntimeManager;
import cn.liboshuai.pulsix.engine.script.DefaultScriptCompiler;
import cn.liboshuai.pulsix.engine.snapshot.SceneSnapshotEnvelopes;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;

public class LocalSimulationRunner {

    private final RuntimeCompiler runtimeCompiler;

    private final Supplier<StreamFeatureStateStore> stateStoreSupplier;

    private final Supplier<LookupService> lookupServiceSupplier;

    private final DecisionExecutor decisionExecutor;

    public LocalSimulationRunner() {
        this(new RuntimeCompiler(new DefaultScriptCompiler()),
                InMemoryStreamFeatureStateStore::new,
                InMemoryLookupService::demo,
                new DecisionExecutor());
    }

    public LocalSimulationRunner(RuntimeCompiler runtimeCompiler,
                                 Supplier<StreamFeatureStateStore> stateStoreSupplier,
                                 Supplier<LookupService> lookupServiceSupplier,
                                 DecisionExecutor decisionExecutor) {
        this.runtimeCompiler = Objects.requireNonNull(runtimeCompiler, "runtimeCompiler");
        this.stateStoreSupplier = Objects.requireNonNull(stateStoreSupplier, "stateStoreSupplier");
        this.lookupServiceSupplier = Objects.requireNonNull(lookupServiceSupplier, "lookupServiceSupplier");
        this.decisionExecutor = Objects.requireNonNull(decisionExecutor, "decisionExecutor");
    }

    public SimulationReport simulate(Path snapshotPath, Path eventsPath) {
        return simulate(readText(snapshotPath), readText(eventsPath));
    }

    public SimulationReport simulate(String snapshotJson, String eventsJson) {
        return simulate(readSnapshotEnvelope(snapshotJson), readEvents(eventsJson));
    }

    public SimulationReport simulate(SceneSnapshot snapshot, RiskEvent event) {
        return simulate(snapshot, List.of(event));
    }

    public SimulationReport simulate(SceneSnapshotEnvelope envelope, RiskEvent event) {
        return simulate(envelope, List.of(event));
    }

    public SimulationReport simulate(SceneSnapshot snapshot, List<RiskEvent> events) {
        return simulate(SceneSnapshotEnvelopes.fromSnapshot(snapshot), events);
    }

    public SimulationReport simulate(SceneSnapshotEnvelope envelope, List<RiskEvent> events) {
        SceneSnapshotEnvelope normalizedEnvelope = SceneSnapshotEnvelopes.fromEnvelope(envelope);
        List<RiskEvent> normalizedEvents = normalizeEvents(events);

        LocalDecisionEngine engine = newEngine();
        engine.publish(normalizedEnvelope);

        SceneSnapshot snapshot = normalizedEnvelope.getSnapshot();
        SimulationReport report = new SimulationReport();
        report.setSnapshotId(snapshot.getSnapshotId());
        report.setSceneCode(snapshot.getSceneCode());
        report.setVersion(snapshot.getVersion());
        report.setChecksum(snapshot.getChecksum());
        report.setEventCount(normalizedEvents.size());

        List<SimulationEventResult> results = new ArrayList<>(normalizedEvents.size());
        for (int index = 0; index < normalizedEvents.size(); index++) {
            RiskEvent event = normalizedEvents.get(index);
            DecisionResult decisionResult = engine.evaluate(event);
            results.add(toSimulationEventResult(index, decisionResult));
        }
        report.setResults(results);
        report.setFinalResult(results.get(results.size() - 1));
        return report;
    }

    public SceneSnapshotEnvelope readSnapshotEnvelope(String snapshotJson) {
        return SceneSnapshotEnvelopes.parse(requireText(snapshotJson, "snapshotJson"));
    }

    public List<RiskEvent> readEvents(String eventsJson) {
        String payload = requireText(eventsJson, "eventsJson");
        if (payload.startsWith("[")) {
            return normalizeEvents(EngineJson.readList(payload, RiskEvent.class));
        }
        return normalizeEvents(List.of(EngineJson.read(payload, RiskEvent.class)));
    }

    public static void main(String[] args) {
        String[] effectiveArgs = args == null || args.length == 0 ? new String[]{"--demo"} : args;
        CliOptions options = CliOptions.parse(effectiveArgs);
        LocalSimulationRunner runner = new LocalSimulationRunner();
        SimulationReport report = options.demo()
                ? runner.simulate(DemoFixtures.demoEnvelope(), DemoFixtures.demoEvents())
                : runner.simulate(options.snapshotPath(), options.eventsPath());
        String output = EngineJson.write(report);
        if (options.outputPath() != null) {
            writeText(options.outputPath(), output);
            return;
        }
        System.out.println(output);
    }

    private LocalDecisionEngine newEngine() {
        return new LocalDecisionEngine(new SceneRuntimeManager(runtimeCompiler),
                stateStoreSupplier.get(),
                lookupServiceSupplier.get(),
                decisionExecutor);
    }

    private List<RiskEvent> normalizeEvents(List<RiskEvent> events) {
        if (events == null || events.isEmpty()) {
            throw new IllegalArgumentException("events must not be empty");
        }
        List<RiskEvent> normalized = new ArrayList<>(events.size());
        for (RiskEvent event : events) {
            if (event == null) {
                throw new IllegalArgumentException("event must not be null");
            }
            normalized.add(event);
        }
        return normalized;
    }

    private SimulationEventResult toSimulationEventResult(int eventIndex, DecisionResult decisionResult) {
        SimulationEventResult result = new SimulationEventResult();
        result.setEventIndex(eventIndex);
        result.setEventId(decisionResult.getEventId());
        result.setTraceId(decisionResult.getTraceId());
        result.setSceneCode(decisionResult.getSceneCode());
        result.setVersion(decisionResult.getVersion());
        result.setFinalAction(decisionResult.getFinalAction());
        result.setFinalScore(decisionResult.getFinalScore());
        result.setHitRules(decisionResult.getRuleHits().stream()
                .filter(ruleHit -> Boolean.TRUE.equals(ruleHit.getHit()))
                .map(this::toMatchedRule)
                .toList());
        result.setHitReasons(result.getHitRules().stream()
                .map(MatchedRule::getReason)
                .filter(Objects::nonNull)
                .toList());
        result.setFeatureSnapshot(decisionResult.getFeatureSnapshot());
        result.setTrace(decisionResult.getTraceLogs());
        return result;
    }

    private MatchedRule toMatchedRule(RuleHit ruleHit) {
        MatchedRule matchedRule = new MatchedRule();
        matchedRule.setRuleCode(ruleHit.getRuleCode());
        matchedRule.setRuleName(ruleHit.getRuleName());
        matchedRule.setAction(ruleHit.getAction());
        matchedRule.setScore(ruleHit.getScore());
        matchedRule.setReason(ruleHit.getReason());
        matchedRule.setDetail(ruleHit.getDetail());
        return matchedRule;
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

    private String requireText(String text, String fieldName) {
        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return text.trim();
    }

    @Data
    @NoArgsConstructor
    public static class SimulationReport {

        private String snapshotId;

        private String sceneCode;

        private Integer version;

        private String checksum;

        private Integer eventCount;

        private SimulationEventResult finalResult;

        private List<SimulationEventResult> results = new ArrayList<>();

        public void setResults(List<SimulationEventResult> results) {
            this.results = results == null ? new ArrayList<>() : new ArrayList<>(results);
        }
    }

    @Data
    @NoArgsConstructor
    public static class SimulationEventResult {

        private Integer eventIndex;

        private String eventId;

        private String traceId;

        private String sceneCode;

        private Integer version;

        private ActionType finalAction;

        private Integer finalScore;

        private List<MatchedRule> hitRules = new ArrayList<>();

        private List<String> hitReasons = new ArrayList<>();

        private Map<String, String> featureSnapshot = new LinkedHashMap<>();

        private List<String> trace = new ArrayList<>();

        public void setHitRules(List<MatchedRule> hitRules) {
            this.hitRules = hitRules == null ? new ArrayList<>() : new ArrayList<>(hitRules);
        }

        public void setHitReasons(List<String> hitReasons) {
            this.hitReasons = hitReasons == null ? new ArrayList<>() : new ArrayList<>(hitReasons);
        }

        public void setFeatureSnapshot(Map<String, String> featureSnapshot) {
            this.featureSnapshot = featureSnapshot == null ? new LinkedHashMap<>() : new LinkedHashMap<>(featureSnapshot);
        }

        public void setTrace(List<String> trace) {
            this.trace = trace == null ? new ArrayList<>() : new ArrayList<>(trace);
        }
    }

    @Data
    @NoArgsConstructor
    public static class MatchedRule {

        private String ruleCode;

        private String ruleName;

        private ActionType action;

        private Integer score;

        private String reason;

        private Map<String, String> detail = new LinkedHashMap<>();

        public void setDetail(Map<String, String> detail) {
            this.detail = detail == null ? new LinkedHashMap<>() : new LinkedHashMap<>(detail);
        }
    }

    private record CliOptions(boolean demo, Path snapshotPath, Path eventsPath, Path outputPath) {

        private static CliOptions parse(String[] args) {
            if (args == null || args.length == 0) {
                throw new IllegalArgumentException(usage());
            }
            boolean demo = false;
            Path snapshotPath = null;
            Path eventsPath = null;
            Path outputPath = null;
            for (int index = 0; index < args.length; index++) {
                String arg = args[index];
                switch (arg) {
                    case "--demo" -> demo = true;
                    case "--snapshot-file" -> snapshotPath = Path.of(requireValue(args, ++index, "--snapshot-file"));
                    case "--events-file" -> eventsPath = Path.of(requireValue(args, ++index, "--events-file"));
                    case "--output-file" -> outputPath = Path.of(requireValue(args, ++index, "--output-file"));
                    case "--help", "-h" -> throw new IllegalArgumentException(usage());
                    default -> throw new IllegalArgumentException("unknown argument: " + arg + System.lineSeparator() + usage());
                }
            }
            if (demo) {
                if (snapshotPath != null || eventsPath != null) {
                    throw new IllegalArgumentException("--demo can not be used with --snapshot-file or --events-file");
                }
                return new CliOptions(true, null, null, outputPath);
            }
            if (snapshotPath == null || eventsPath == null) {
                throw new IllegalArgumentException("--snapshot-file and --events-file are required" + System.lineSeparator() + usage());
            }
            return new CliOptions(false, snapshotPath, eventsPath, outputPath);
        }

        private static String requireValue(String[] args, int index, String optionName) {
            if (index >= args.length) {
                throw new IllegalArgumentException("missing value for " + optionName + System.lineSeparator() + usage());
            }
            return args[index];
        }

        private static String usage() {
            return "Usage: LocalSimulationRunner --demo | --snapshot-file <path> --events-file <path> [--output-file <path>]";
        }
    }
}
