package cn.liboshuai.pulsix.engine.simulation;

import cn.liboshuai.pulsix.engine.context.EvalContext;
import cn.liboshuai.pulsix.engine.core.DecisionExecutor;
import cn.liboshuai.pulsix.engine.core.LocalDecisionEngine;
import cn.liboshuai.pulsix.engine.demo.DemoFixtures;
import cn.liboshuai.pulsix.engine.feature.InMemoryLookupService;
import cn.liboshuai.pulsix.engine.feature.LookupResult;
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
import cn.liboshuai.pulsix.engine.runtime.CompiledSceneRuntime;
import cn.liboshuai.pulsix.engine.runtime.RuntimeCompiler;
import cn.liboshuai.pulsix.engine.runtime.SceneRuntimeManager;
import cn.liboshuai.pulsix.engine.script.DefaultScriptCompiler;
import cn.liboshuai.pulsix.engine.snapshot.SceneSnapshotEnvelopes;
import cn.liboshuai.pulsix.engine.support.ValueConverter;
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
        return simulate(snapshotPath, eventsPath, null);
    }

    public SimulationReport simulate(Path snapshotPath, Path eventsPath, Path overridesPath) {
        return simulate(readText(snapshotPath), readText(eventsPath), overridesPath == null ? null : readText(overridesPath));
    }

    public SimulationReport simulate(String snapshotJson, String eventsJson) {
        return simulate(snapshotJson, eventsJson, null);
    }

    public SimulationReport simulate(String snapshotJson, String eventsJson, String overridesJson) {
        return simulate(readSnapshotEnvelope(snapshotJson), readEvents(eventsJson), readOverrides(overridesJson));
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
        return simulate(envelope, events, SimulationOverrides.empty());
    }

    public SimulationReport simulate(SceneSnapshotEnvelope envelope,
                                     List<RiskEvent> events,
                                     SimulationOverrides overrides) {
        SceneSnapshotEnvelope normalizedEnvelope = SceneSnapshotEnvelopes.fromEnvelope(envelope);
        List<RiskEvent> normalizedEvents = normalizeEvents(events);
        SimulationOverrides normalizedOverrides = overrides == null ? SimulationOverrides.empty() : overrides.normalized();

        SimulationOverrideStreamFeatureStateStore stateStore =
                new SimulationOverrideStreamFeatureStateStore(stateStoreSupplier.get());
        SimulationOverrideLookupService lookupService =
                new SimulationOverrideLookupService(lookupServiceSupplier.get());
        LocalDecisionEngine engine = newEngine(stateStore, lookupService);
        engine.publish(normalizedEnvelope);

        SceneSnapshot snapshot = normalizedEnvelope.getSnapshot();
        SimulationReport report = new SimulationReport();
        report.setSnapshotId(snapshot.getSnapshotId());
        report.setSceneCode(snapshot.getSceneCode());
        report.setVersion(snapshot.getVersion());
        report.setUsedVersion(snapshot.getVersion());
        report.setChecksum(snapshot.getChecksum());
        report.setEventCount(normalizedEvents.size());
        report.setOverridesApplied(!normalizedOverrides.isEmpty());

        List<SimulationEventResult> results = new ArrayList<>(normalizedEvents.size());
        for (int index = 0; index < normalizedEvents.size(); index++) {
            RiskEvent event = normalizedEvents.get(index);
            EventOverrides eventOverrides = normalizedOverrides.resolve(index);
            stateStore.setActiveOverrides(eventOverrides);
            lookupService.setActiveOverrides(eventOverrides);
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

    public SimulationOverrides readOverrides(String overridesJson) {
        if (overridesJson == null || overridesJson.isBlank()) {
            return SimulationOverrides.empty();
        }
        return EngineJson.read(requireText(overridesJson, "overridesJson"), SimulationOverrides.class).normalized();
    }

    public static void main(String[] args) {
        String[] effectiveArgs = args == null || args.length == 0 ? new String[]{"--demo"} : args;
        CliOptions options = CliOptions.parse(effectiveArgs);
        LocalSimulationRunner runner = new LocalSimulationRunner();
        SimulationReport report = options.demo()
                ? runner.simulate(DemoFixtures.demoEnvelope(), DemoFixtures.demoEvents(), SimulationOverrides.empty())
                : runner.simulate(options.snapshotPath(), options.eventsPath(), options.overridesPath());
        String output = EngineJson.write(report);
        if (options.outputPath() != null) {
            writeText(options.outputPath(), output);
            return;
        }
        System.out.println(output);
    }

    private LocalDecisionEngine newEngine(StreamFeatureStateStore stateStore, LookupService lookupService) {
        return new LocalDecisionEngine(new SceneRuntimeManager(runtimeCompiler),
                stateStore,
                lookupService,
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
        result.setUsedVersion(decisionResult.getVersion());
        result.setFinalAction(decisionResult.getFinalAction());
        result.setFinalScore(decisionResult.getFinalScore());
        result.setTotalScore(decisionResult.getTotalScore());
        result.setReason(decisionResult.getReason());
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

        private Integer usedVersion;

        private String checksum;

        private Integer eventCount;

        private boolean overridesApplied;

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

        private Integer usedVersion;

        private ActionType finalAction;

        private Integer finalScore;

        private Integer totalScore;

        private String reason;

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

    @Data
    @NoArgsConstructor
    public static class SimulationOverrides {

        private Map<String, Object> lookupFeatures = new LinkedHashMap<>();

        private Map<String, Object> streamFeatures = new LinkedHashMap<>();

        private Map<Integer, EventOverrides> eventOverrides = new LinkedHashMap<>();

        public static SimulationOverrides empty() {
            return new SimulationOverrides();
        }

        public SimulationOverrides normalized() {
            SimulationOverrides normalized = new SimulationOverrides();
            normalized.setLookupFeatures(lookupFeatures);
            normalized.setStreamFeatures(streamFeatures);
            normalized.setEventOverrides(eventOverrides);
            return normalized;
        }

        public boolean isEmpty() {
            return lookupFeatures.isEmpty() && streamFeatures.isEmpty() && eventOverrides.isEmpty();
        }

        public EventOverrides resolve(int eventIndex) {
            EventOverrides merged = new EventOverrides();
            merged.getLookupFeatures().putAll(lookupFeatures);
            merged.getStreamFeatures().putAll(streamFeatures);
            EventOverrides specificOverrides = eventOverrides.get(eventIndex);
            if (specificOverrides != null) {
                merged.getLookupFeatures().putAll(specificOverrides.getLookupFeatures());
                merged.getStreamFeatures().putAll(specificOverrides.getStreamFeatures());
            }
            return merged;
        }

        public void setLookupFeatures(Map<String, Object> lookupFeatures) {
            this.lookupFeatures = copyObjectMap(lookupFeatures);
        }

        public void setStreamFeatures(Map<String, Object> streamFeatures) {
            this.streamFeatures = copyObjectMap(streamFeatures);
        }

        public void setEventOverrides(Map<Integer, EventOverrides> eventOverrides) {
            this.eventOverrides = eventOverrides == null ? new LinkedHashMap<>() : new LinkedHashMap<>(eventOverrides);
        }
    }

    @Data
    @NoArgsConstructor
    public static class EventOverrides {

        private Map<String, Object> lookupFeatures = new LinkedHashMap<>();

        private Map<String, Object> streamFeatures = new LinkedHashMap<>();

        public void setLookupFeatures(Map<String, Object> lookupFeatures) {
            this.lookupFeatures = copyObjectMap(lookupFeatures);
        }

        public void setStreamFeatures(Map<String, Object> streamFeatures) {
            this.streamFeatures = copyObjectMap(streamFeatures);
        }
    }

    private record CliOptions(boolean demo,
                              Path snapshotPath,
                              Path eventsPath,
                              Path overridesPath,
                              Path outputPath) {

        private static CliOptions parse(String[] args) {
            if (args == null || args.length == 0) {
                throw new IllegalArgumentException(usage());
            }
            boolean demo = false;
            Path snapshotPath = null;
            Path eventsPath = null;
            Path overridesPath = null;
            Path outputPath = null;
            for (int index = 0; index < args.length; index++) {
                String arg = args[index];
                switch (arg) {
                    case "--demo" -> demo = true;
                    case "--snapshot-file" -> snapshotPath = Path.of(requireValue(args, ++index, "--snapshot-file"));
                    case "--events-file" -> eventsPath = Path.of(requireValue(args, ++index, "--events-file"));
                    case "--overrides-file" -> overridesPath = Path.of(requireValue(args, ++index, "--overrides-file"));
                    case "--output-file" -> outputPath = Path.of(requireValue(args, ++index, "--output-file"));
                    case "--help", "-h" -> throw new IllegalArgumentException(usage());
                    default -> throw new IllegalArgumentException("unknown argument: " + arg + System.lineSeparator() + usage());
                }
            }
            if (demo) {
                if (snapshotPath != null || eventsPath != null || overridesPath != null) {
                    throw new IllegalArgumentException("--demo can not be used with --snapshot-file, --events-file or --overrides-file");
                }
                return new CliOptions(true, null, null, null, outputPath);
            }
            if (snapshotPath == null || eventsPath == null) {
                throw new IllegalArgumentException("--snapshot-file and --events-file are required" + System.lineSeparator() + usage());
            }
            return new CliOptions(false, snapshotPath, eventsPath, overridesPath, outputPath);
        }

        private static String requireValue(String[] args, int index, String optionName) {
            if (index >= args.length) {
                throw new IllegalArgumentException("missing value for " + optionName + System.lineSeparator() + usage());
            }
            return args[index];
        }

        private static String usage() {
            return "Usage: LocalSimulationRunner --demo | --snapshot-file <path> --events-file <path> "
                    + "[--overrides-file <path>] [--output-file <path>]";
        }
    }

    private static Map<String, Object> copyObjectMap(Map<String, Object> source) {
        return source == null ? new LinkedHashMap<>() : new LinkedHashMap<>(source);
    }

    private static final class SimulationOverrideStreamFeatureStateStore implements StreamFeatureStateStore {

        private final StreamFeatureStateStore delegate;

        private EventOverrides activeOverrides = new EventOverrides();

        private SimulationOverrideStreamFeatureStateStore(StreamFeatureStateStore delegate) {
            this.delegate = Objects.requireNonNull(delegate, "delegate");
        }

        private void setActiveOverrides(EventOverrides activeOverrides) {
            this.activeOverrides = activeOverrides == null ? new EventOverrides() : activeOverrides;
        }

        @Override
        public Object evaluate(CompiledSceneRuntime.CompiledStreamFeature feature, EvalContext context) {
            if (feature != null && feature.getSpec() != null) {
                String featureCode = feature.getSpec().getCode();
                if (activeOverrides.getStreamFeatures().containsKey(featureCode)) {
                    return ValueConverter.coerce(activeOverrides.getStreamFeatures().get(featureCode), feature.getSpec().getValueType());
                }
            }
            return delegate.evaluate(feature, context);
        }

        @Override
        public void bindExecutionContext(StreamFeatureExecutionContext executionContext) {
            delegate.bindExecutionContext(executionContext);
        }

        @Override
        public void clearExecutionContext() {
            delegate.clearExecutionContext();
        }

        @Override
        public void onTimer(long timestamp) {
            delegate.onTimer(timestamp);
        }
    }

    private static final class SimulationOverrideLookupService implements LookupService {

        private final LookupService delegate;

        private EventOverrides activeOverrides = new EventOverrides();

        private SimulationOverrideLookupService(LookupService delegate) {
            this.delegate = Objects.requireNonNull(delegate, "delegate");
        }

        private void setActiveOverrides(EventOverrides activeOverrides) {
            this.activeOverrides = activeOverrides == null ? new EventOverrides() : activeOverrides;
        }

        @Override
        public Object lookup(cn.liboshuai.pulsix.engine.model.LookupType lookupType, String sourceRef, String key) {
            return delegate.lookup(lookupType, sourceRef, key);
        }

        @Override
        public LookupResult lookup(CompiledSceneRuntime.CompiledLookupFeature feature, EvalContext context) {
            if (feature != null && feature.getSpec() != null) {
                String featureCode = feature.getSpec().getCode();
                if (activeOverrides.getLookupFeatures().containsKey(featureCode)) {
                    String lookupKey = feature.getKeyScript() == null ? null
                            : ValueConverter.asString(feature.getKeyScript().execute(context));
                    return LookupResult.success(
                            ValueConverter.coerce(activeOverrides.getLookupFeatures().get(featureCode), feature.getSpec().getValueType()),
                            lookupKey);
                }
            }
            return delegate.lookup(feature, context);
        }
    }
}
