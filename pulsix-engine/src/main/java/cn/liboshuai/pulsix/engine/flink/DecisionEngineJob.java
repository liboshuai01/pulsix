package cn.liboshuai.pulsix.engine.flink;

import cn.liboshuai.pulsix.engine.demo.DemoFixtures;
import cn.liboshuai.pulsix.engine.flink.snapshot.SceneSnapshotSourceFactory;
import cn.liboshuai.pulsix.engine.flink.snapshot.SceneSnapshotSourceOptions;
import cn.liboshuai.pulsix.engine.flink.snapshot.SceneSnapshotSourceType;
import cn.liboshuai.pulsix.engine.flink.typeinfo.EngineTypeInfos;
import cn.liboshuai.pulsix.engine.model.DecisionResult;
import cn.liboshuai.pulsix.engine.model.RiskEvent;
import cn.liboshuai.pulsix.engine.model.SceneSnapshotEnvelope;
import org.apache.flink.api.common.eventtime.SerializableTimestampAssigner;
import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.api.common.state.MapStateDescriptor;
import org.apache.flink.api.common.typeinfo.Types;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.configuration.MemorySize;
import org.apache.flink.configuration.SecurityOptions;
import org.apache.flink.configuration.TaskManagerOptions;
import org.apache.flink.configuration.WebOptions;
import org.apache.flink.runtime.state.hashmap.HashMapStateBackend;
import org.apache.flink.streaming.api.CheckpointingMode;
import org.apache.flink.streaming.api.datastream.BroadcastStream;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.datastream.KeyedStream;
import org.apache.flink.streaming.api.datastream.SingleOutputStreamOperator;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.functions.source.SourceFunction;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

public class DecisionEngineJob {

    public static void main(String[] args) throws Exception {
        JobOptions options = JobOptions.parse(args);
        Path localLogFile = prepareLocalLogFile();
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        configureRuntime(env, localLogFile);

        DataStream<RiskEvent> eventStream = buildDemoEventStream(env);
        DataStream<SceneSnapshotEnvelope> configStream = buildConfigStream(env, options.snapshotSourceOptions());

        MapStateDescriptor<String, SceneSnapshotEnvelope> snapshotStateDescriptor = new MapStateDescriptor<>(
                "scene-snapshot-broadcast-state",
                Types.STRING,
                EngineTypeInfos.sceneSnapshotEnvelope()
        );

        KeyedStream<RiskEvent, String> keyedEventStream = eventStream.keyBy(RiskEvent::routeKey);
        BroadcastStream<SceneSnapshotEnvelope> broadcastStream = configStream.broadcast(snapshotStateDescriptor);
        SingleOutputStreamOperator<DecisionResult> resultStream = keyedEventStream
                .connect(broadcastStream)
                .process(new DecisionBroadcastProcessFunction(snapshotStateDescriptor))
                .returns(EngineTypeInfos.decisionResult());

        resultStream.print("decision-result");
        resultStream.getSideOutput(EngineOutputTags.DECISION_LOG).print("decision-log");
        resultStream.getSideOutput(EngineOutputTags.ENGINE_ERROR).print("engine-error");

        env.execute("pulsix-engine-demo-job");
    }

    private static void configureRuntime(StreamExecutionEnvironment env, Path localLogFile) {
        env.setParallelism(1);
        env.getConfig().enableObjectReuse();
        env.configure(localExecutionConfiguration(localLogFile));
        env.enableCheckpointing(30_000L, CheckpointingMode.EXACTLY_ONCE);
        env.getCheckpointConfig().setMinPauseBetweenCheckpoints(5_000L);
        env.getCheckpointConfig().setCheckpointTimeout(60_000L);
        env.getCheckpointConfig().setTolerableCheckpointFailureNumber(3);
        String backend = System.getProperty("pulsix.engine.state-backend", "hashmap").trim().toLowerCase();
        if ("rocksdb".equals(backend)) {
            env.setStateBackend(new org.apache.flink.contrib.streaming.state.EmbeddedRocksDBStateBackend(true));
            return;
        }
        env.setStateBackend(new HashMapStateBackend());
    }

    private static DataStream<RiskEvent> buildDemoEventStream(StreamExecutionEnvironment env) {
        return env.addSource(new DemoRiskEventSource(), EngineTypeInfos.riskEvent())
                .assignTimestampsAndWatermarks(WatermarkStrategy
                        .<RiskEvent>forBoundedOutOfOrderness(Duration.ofSeconds(1))
                        .withTimestampAssigner((SerializableTimestampAssigner<RiskEvent>) (event, timestamp) -> {
                            Instant eventTime = event.getEventTime();
                            return eventTime != null ? eventTime.toEpochMilli() : timestamp;
                        }));
    }

    private static DataStream<SceneSnapshotEnvelope> buildConfigStream(StreamExecutionEnvironment env,
                                                                       SceneSnapshotSourceOptions options) {
        return SceneSnapshotSourceFactory.build(env, options)
                .assignTimestampsAndWatermarks(WatermarkStrategy.noWatermarks());
    }

    private static Configuration localExecutionConfiguration(Path localLogFile) {
        Configuration configuration = new Configuration();
        configuration.set(TaskManagerOptions.CPU_CORES, 1.0);
        configuration.set(TaskManagerOptions.NUM_TASK_SLOTS, 1);
        configuration.set(TaskManagerOptions.TASK_HEAP_MEMORY, MemorySize.ofMebiBytes(256));
        configuration.set(TaskManagerOptions.TASK_OFF_HEAP_MEMORY, MemorySize.ofMebiBytes(128));
        configuration.set(TaskManagerOptions.NETWORK_MEMORY_MIN, MemorySize.ofMebiBytes(64));
        configuration.set(TaskManagerOptions.NETWORK_MEMORY_MAX, MemorySize.ofMebiBytes(64));
        configuration.set(TaskManagerOptions.MANAGED_MEMORY_SIZE, MemorySize.ofMebiBytes(128));
        configuration.set(SecurityOptions.DELEGATION_TOKENS_ENABLED, false);
        SecurityOptions.forProvider(configuration, "hadoopfs")
                .set(SecurityOptions.DELEGATION_TOKEN_PROVIDER_ENABLED, false);
        SecurityOptions.forProvider(configuration, "hbase")
                .set(SecurityOptions.DELEGATION_TOKEN_PROVIDER_ENABLED, false);
        String localLogPath = localLogFile.toAbsolutePath().toString();
        configuration.set(WebOptions.LOG_PATH, localLogPath);
        configuration.set(TaskManagerOptions.TASK_MANAGER_LOG_PATH, localLogPath);
        return configuration;
    }

    private static Path prepareLocalLogFile() throws IOException {
        Path logFile = Paths.get(System.getProperty("java.io.tmpdir"), "pulsix-engine-demo.log")
                .toAbsolutePath();
        Path parent = logFile.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        if (Files.notExists(logFile)) {
            Files.createFile(logFile);
        }
        System.setProperty("log.file", logFile.toString());
        return logFile;
    }

    private static class DemoRiskEventSource implements SourceFunction<RiskEvent> {

        private volatile boolean running = true;

        @Override
        public void run(SourceContext<RiskEvent> context) throws InterruptedException {
            Thread.sleep(100L);
            List<RiskEvent> events = DemoFixtures.demoEvents();
            for (RiskEvent event : events) {
                if (!running) {
                    return;
                }
                synchronized (context.getCheckpointLock()) {
                    context.collect(event);
                }
            }
        }

        @Override
        public void cancel() {
            this.running = false;
        }
    }

    private record JobOptions(SceneSnapshotSourceOptions snapshotSourceOptions) {

        private static JobOptions parse(String[] args) {
            String snapshotSource = property("pulsix.engine.snapshot-source", "demo");
            Path snapshotFile = pathProperty("pulsix.engine.snapshot-file");
            long snapshotPollMs = longProperty("pulsix.engine.snapshot-poll-ms", 1_000L);
            String jdbcUrl = property("pulsix.engine.snapshot-jdbc-url", null);
            String jdbcUser = property("pulsix.engine.snapshot-jdbc-user", null);
            String jdbcPassword = property("pulsix.engine.snapshot-jdbc-password", null);
            String snapshotSceneCode = property("pulsix.engine.snapshot-scene-code", null);
            Integer snapshotVersion = integerProperty("pulsix.engine.snapshot-version");
            String jdbcQuery = property("pulsix.engine.snapshot-jdbc-query", null);
            String cdcHost = property("pulsix.engine.snapshot-cdc-host", null);
            Integer cdcPort = integerProperty("pulsix.engine.snapshot-cdc-port");
            String cdcDatabase = property("pulsix.engine.snapshot-cdc-database", null);
            String cdcTable = property("pulsix.engine.snapshot-cdc-table", "scene_release");
            String cdcUser = property("pulsix.engine.snapshot-cdc-user", null);
            String cdcPassword = property("pulsix.engine.snapshot-cdc-password", null);
            String cdcServerId = property("pulsix.engine.snapshot-cdc-server-id", null);
            String cdcServerTimeZone = property("pulsix.engine.snapshot-cdc-server-time-zone", "UTC");

            if (args != null) {
                for (int index = 0; index < args.length; index++) {
                    String arg = args[index];
                    switch (arg) {
                        case "--snapshot-source" -> snapshotSource = requireValue(args, ++index, arg);
                        case "--snapshot-file" -> snapshotFile = Path.of(requireValue(args, ++index, arg));
                        case "--snapshot-poll-ms" -> snapshotPollMs = Long.parseLong(requireValue(args, ++index, arg));
                        case "--snapshot-jdbc-url" -> jdbcUrl = requireValue(args, ++index, arg);
                        case "--snapshot-jdbc-user" -> jdbcUser = requireValue(args, ++index, arg);
                        case "--snapshot-jdbc-password" -> jdbcPassword = requireValue(args, ++index, arg);
                        case "--snapshot-scene-code" -> snapshotSceneCode = requireValue(args, ++index, arg);
                        case "--snapshot-version" -> snapshotVersion = Integer.parseInt(requireValue(args, ++index, arg));
                        case "--snapshot-jdbc-query" -> jdbcQuery = requireValue(args, ++index, arg);
                        case "--snapshot-cdc-host" -> cdcHost = requireValue(args, ++index, arg);
                        case "--snapshot-cdc-port" -> cdcPort = Integer.parseInt(requireValue(args, ++index, arg));
                        case "--snapshot-cdc-database" -> cdcDatabase = requireValue(args, ++index, arg);
                        case "--snapshot-cdc-table" -> cdcTable = requireValue(args, ++index, arg);
                        case "--snapshot-cdc-user" -> cdcUser = requireValue(args, ++index, arg);
                        case "--snapshot-cdc-password" -> cdcPassword = requireValue(args, ++index, arg);
                        case "--snapshot-cdc-server-id" -> cdcServerId = requireValue(args, ++index, arg);
                        case "--snapshot-cdc-server-time-zone" -> cdcServerTimeZone = requireValue(args, ++index, arg);
                        case "--help", "-h" -> throw new IllegalArgumentException(usage());
                        default -> throw new IllegalArgumentException("unknown argument: " + arg + System.lineSeparator() + usage());
                    }
                }
            }

            SceneSnapshotSourceType sourceType = SceneSnapshotSourceType.valueOf(snapshotSource.trim().toUpperCase());
            SceneSnapshotSourceOptions options = new SceneSnapshotSourceOptions(sourceType,
                    snapshotFile,
                    snapshotPollMs,
                    jdbcUrl,
                    jdbcUser,
                    jdbcPassword,
                    snapshotSceneCode,
                    snapshotVersion,
                    jdbcQuery,
                    cdcHost,
                    cdcPort,
                    cdcDatabase,
                    cdcTable,
                    cdcUser,
                    cdcPassword,
                    cdcServerId,
                    cdcServerTimeZone);
            return new JobOptions(options);
        }

        private static String property(String key, String defaultValue) {
            String value = System.getProperty(key);
            return value == null || value.isBlank() ? defaultValue : value.trim();
        }

        private static Path pathProperty(String key) {
            String value = property(key, null);
            return value == null ? null : Path.of(value);
        }

        private static long longProperty(String key, long defaultValue) {
            String value = property(key, null);
            return value == null ? defaultValue : Long.parseLong(value);
        }

        private static Integer integerProperty(String key) {
            String value = property(key, null);
            return value == null ? null : Integer.parseInt(value);
        }

        private static String requireValue(String[] args, int index, String optionName) {
            if (index >= args.length) {
                throw new IllegalArgumentException("missing value for " + optionName + System.lineSeparator() + usage());
            }
            return args[index];
        }

        private static String usage() {
            return "Usage: DecisionEngineJob [--snapshot-source demo|file|jdbc|cdc] "
                    + "[--snapshot-file <path>] [--snapshot-poll-ms <ms>] "
                    + "[--snapshot-jdbc-url <url> --snapshot-jdbc-user <user> --snapshot-jdbc-password <password>] "
                    + "[--snapshot-scene-code <sceneCode>] [--snapshot-version <version>] [--snapshot-jdbc-query <sql>] "
                    + "[--snapshot-cdc-host <host> --snapshot-cdc-port <port> --snapshot-cdc-database <db> "
                    + "--snapshot-cdc-table <table> --snapshot-cdc-user <user> --snapshot-cdc-password <password> "
                    + "[--snapshot-cdc-server-id <serverId>] [--snapshot-cdc-server-time-zone <tz>]]";
        }
    }

}
