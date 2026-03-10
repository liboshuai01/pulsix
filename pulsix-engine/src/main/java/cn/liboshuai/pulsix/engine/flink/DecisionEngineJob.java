package cn.liboshuai.pulsix.engine.flink;

import cn.liboshuai.pulsix.engine.demo.DemoFixtures;
import cn.liboshuai.pulsix.engine.model.DecisionResult;
import cn.liboshuai.pulsix.engine.model.RiskEvent;
import cn.liboshuai.pulsix.engine.model.SceneSnapshotEnvelope;
import org.apache.flink.api.common.eventtime.SerializableTimestampAssigner;
import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.api.common.state.MapStateDescriptor;
import org.apache.flink.api.common.typeinfo.TypeInformation;
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
        Path localLogFile = prepareLocalLogFile();
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        configureRuntime(env, localLogFile);

        DataStream<RiskEvent> eventStream = buildDemoEventStream(env);
        DataStream<SceneSnapshotEnvelope> configStream = buildDemoConfigStream(env);

        MapStateDescriptor<String, SceneSnapshotEnvelope> snapshotStateDescriptor = new MapStateDescriptor<>(
                "scene-snapshot-broadcast-state",
                TypeInformation.of(String.class),
                TypeInformation.of(SceneSnapshotEnvelope.class)
        );

        KeyedStream<RiskEvent, String> keyedEventStream = eventStream.keyBy(RiskEvent::routeKey);
        BroadcastStream<SceneSnapshotEnvelope> broadcastStream = configStream.broadcast(snapshotStateDescriptor);
        SingleOutputStreamOperator<DecisionResult> resultStream = keyedEventStream
                .connect(broadcastStream)
                .process(new DecisionBroadcastProcessFunction(snapshotStateDescriptor));

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
        return env.addSource(new DemoRiskEventSource(), TypeInformation.of(RiskEvent.class))
                .assignTimestampsAndWatermarks(WatermarkStrategy
                        .<RiskEvent>forBoundedOutOfOrderness(Duration.ofSeconds(1))
                        .withTimestampAssigner((SerializableTimestampAssigner<RiskEvent>) (event, timestamp) -> {
                            Instant eventTime = event.getEventTime();
                            return eventTime != null ? eventTime.toEpochMilli() : timestamp;
                        }));
    }

    private static DataStream<SceneSnapshotEnvelope> buildDemoConfigStream(StreamExecutionEnvironment env) {
        return env.addSource(new DemoSnapshotSource(), TypeInformation.of(SceneSnapshotEnvelope.class))
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

    private static class DemoSnapshotSource implements SourceFunction<SceneSnapshotEnvelope> {

        private volatile boolean running = true;

        @Override
        public void run(SourceContext<SceneSnapshotEnvelope> context) {
            if (!running) {
                return;
            }
            synchronized (context.getCheckpointLock()) {
                context.collect(DemoFixtures.demoEnvelope());
            }
        }

        @Override
        public void cancel() {
            this.running = false;
        }
    }

}
