package cn.liboshuai.pulsix.engine.flink;

import cn.liboshuai.pulsix.engine.demo.DemoFixtures;
import cn.liboshuai.pulsix.engine.feature.InMemoryLookupService;
import cn.liboshuai.pulsix.engine.feature.RedisLookupService;
import cn.liboshuai.pulsix.engine.flink.runtime.SceneReleaseTimeline;
import cn.liboshuai.pulsix.engine.flink.snapshot.SceneSnapshotSourceFactory;
import cn.liboshuai.pulsix.engine.flink.snapshot.SceneSnapshotSourceOptions;
import cn.liboshuai.pulsix.engine.flink.typeinfo.EngineTypeInfos;
import cn.liboshuai.pulsix.engine.model.DecisionLogRecord;
import cn.liboshuai.pulsix.engine.model.DecisionResult;
import cn.liboshuai.pulsix.engine.model.EngineErrorRecord;
import cn.liboshuai.pulsix.engine.model.RiskEvent;
import cn.liboshuai.pulsix.engine.model.SceneSnapshotEnvelope;
import org.apache.flink.api.common.eventtime.SerializableTimestampAssigner;
import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.api.common.state.MapStateDescriptor;
import org.apache.flink.api.common.typeinfo.Types;
import org.apache.flink.configuration.BlobServerOptions;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.configuration.MemorySize;
import org.apache.flink.configuration.SecurityOptions;
import org.apache.flink.configuration.TaskManagerOptions;
import org.apache.flink.configuration.WebOptions;
import org.apache.flink.connector.base.DeliveryGuarantee;
import org.apache.flink.connector.kafka.sink.KafkaRecordSerializationSchema;
import org.apache.flink.connector.kafka.sink.KafkaSink;
import org.apache.flink.connector.kafka.source.KafkaSource;
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
        DecisionEngineJobOptions options = DecisionEngineJobOptions.parse(args);
        Path localLogFile = prepareLocalLogFile(options.runtimeOptions());
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        env.getConfig().setGlobalJobParameters(options.parameters());
        configureRuntime(env, options.runtimeOptions(), localLogFile);

        EventSourceStreams eventSourceStreams = buildEventSourceStreams(env,
                options.eventSourceOptions(),
                options.runtimeOptions());
        DataStream<SceneSnapshotEnvelope> configStream = buildConfigStream(env, options.snapshotSourceOptions());

        MapStateDescriptor<String, SceneReleaseTimeline> snapshotStateDescriptor = new MapStateDescriptor<>(
                "scene-snapshot-broadcast-state",
                Types.STRING,
                EngineTypeInfos.sceneReleaseTimeline()
        );

        KeyedStream<RiskEvent, String> keyedEventStream = eventSourceStreams.eventStream().keyBy(RiskEvent::processingRouteKey);
        BroadcastStream<SceneSnapshotEnvelope> broadcastStream = configStream.broadcast(snapshotStateDescriptor);

        SingleOutputStreamOperator<StreamFeatureRouteEvent> routedFeatureStream = keyedEventStream
                .connect(broadcastStream)
                .process(new StreamFeatureRoutingProcessFunction(snapshotStateDescriptor))
                .returns(EngineTypeInfos.streamFeatureRouteEvent())
                .name("stream-feature-route");

        SingleOutputStreamOperator<PreparedStreamFeatureChunk> preparedStreamFeatureStream = routedFeatureStream
                .keyBy(StreamFeatureRouteEvent::getRouteExecutionKey)
                .process(new StreamFeaturePrepareProcessFunction())
                .returns(EngineTypeInfos.preparedStreamFeatureChunk())
                .name("stream-feature-prepare");

        SingleOutputStreamOperator<PreparedDecisionInput> aggregatedPreparedDecisionStream = preparedStreamFeatureStream
                .keyBy(PreparedStreamFeatureChunk::getEventJoinKey)
                .process(new PreparedDecisionAggregateProcessFunction())
                .returns(EngineTypeInfos.preparedDecisionInput())
                .name("prepared-decision-aggregate");

        DataStream<PreparedDecisionInput> preparedDecisionInputStream = aggregatedPreparedDecisionStream
                .union(routedFeatureStream.getSideOutput(StreamFeatureRoutingProcessFunction.PREPARED_DECISION_BYPASS));

        SingleOutputStreamOperator<DecisionResult> resultStream = preparedDecisionInputStream
                .process(new PreparedDecisionProcessFunction(buildLookupServiceFactory(options.lookupOptions())))
                .returns(EngineTypeInfos.decisionResult())
                .name("prepared-decision-main-chain");

        DataStream<DecisionLogRecord> decisionLogStream = resultStream.getSideOutput(EngineOutputTags.DECISION_LOG);
        DataStream<EngineErrorRecord> engineErrorStream = resultStream.getSideOutput(EngineOutputTags.ENGINE_ERROR)
                .union(routedFeatureStream.getSideOutput(EngineOutputTags.ENGINE_ERROR))
                .union(preparedStreamFeatureStream.getSideOutput(EngineOutputTags.ENGINE_ERROR))
                .union(aggregatedPreparedDecisionStream.getSideOutput(EngineOutputTags.ENGINE_ERROR));
        if (eventSourceStreams.inputErrorStream() != null) {
            engineErrorStream = eventSourceStreams.inputErrorStream().union(engineErrorStream);
        }

        sinkStream("decision-result", resultStream, options.outputOptions().decisionResultSinkOptions());
        sinkStream("decision-log", decisionLogStream, options.outputOptions().decisionLogSinkOptions());
        sinkStream("engine-error", engineErrorStream, options.outputOptions().engineErrorSinkOptions());

        env.execute(options.runtimeOptions().jobName());
    }

    private static void configureRuntime(StreamExecutionEnvironment env,
                                         DecisionEngineJobOptions.RuntimeOptions options,
                                         Path localLogFile) {
        env.setParallelism(options.parallelism());
        if (options.objectReuseEnabled()) {
            env.getConfig().enableObjectReuse();
        } else {
            env.getConfig().disableObjectReuse();
        }
        env.configure(localExecutionConfiguration(options, localLogFile));
        env.enableCheckpointing(options.checkpointIntervalMs(), CheckpointingMode.EXACTLY_ONCE);
        env.getCheckpointConfig().setMinPauseBetweenCheckpoints(options.checkpointMinPauseMs());
        env.getCheckpointConfig().setCheckpointTimeout(options.checkpointTimeoutMs());
        env.getCheckpointConfig().setTolerableCheckpointFailureNumber(options.checkpointTolerableFailureNumber());
        switch (options.stateBackendType()) {
            case ROCKSDB -> env.setStateBackend(new org.apache.flink.contrib.streaming.state.EmbeddedRocksDBStateBackend(true));
            case HASHMAP -> env.setStateBackend(new HashMapStateBackend());
        }
    }

    private static EventSourceStreams buildEventSourceStreams(StreamExecutionEnvironment env,
                                                              DecisionEngineJobOptions.EventSourceOptions options,
                                                              DecisionEngineJobOptions.RuntimeOptions runtimeOptions) {
        return switch (options.sourceType()) {
            case DEMO -> new EventSourceStreams(withEventTimeWatermarks(buildDemoEventStream(env), runtimeOptions), null);
            case KAFKA -> buildKafkaEventSourceStreams(env, options, runtimeOptions);
        };
    }

    private static DataStream<RiskEvent> buildDemoEventStream(StreamExecutionEnvironment env) {
        return env.addSource(new DemoRiskEventSource(), EngineTypeInfos.riskEvent())
                .name("demo-risk-event-source");
    }

    private static EventSourceStreams buildKafkaEventSourceStreams(StreamExecutionEnvironment env,
                                                                   DecisionEngineJobOptions.EventSourceOptions options,
                                                                   DecisionEngineJobOptions.RuntimeOptions runtimeOptions) {
        KafkaSource<String> kafkaSource = KafkaSource.<String>builder()
                .setBootstrapServers(options.kafkaBootstrapServers())
                .setTopics(options.kafkaTopic())
                .setGroupId(options.kafkaGroupId())
                .setStartingOffsets(options.kafkaStartingOffsets().toOffsetsInitializer())
                .setValueOnlyDeserializer(new org.apache.flink.api.common.serialization.SimpleStringSchema())
                .build();
        SingleOutputStreamOperator<RiskEvent> parsedEventStream = env.fromSource(
                        kafkaSource,
                        WatermarkStrategy.noWatermarks(),
                        "risk-event-kafka-source"
                )
                .process(new RiskEventJsonProcessFunction())
                .returns(EngineTypeInfos.riskEvent())
                .name("risk-event-json-parse");
        return new EventSourceStreams(
                withEventTimeWatermarks(parsedEventStream, runtimeOptions),
                parsedEventStream.getSideOutput(EngineOutputTags.ENGINE_ERROR)
        );
    }

    private static SingleOutputStreamOperator<RiskEvent> withEventTimeWatermarks(
            DataStream<RiskEvent> eventStream,
            DecisionEngineJobOptions.RuntimeOptions runtimeOptions) {
        Duration outOfOrderness = Duration.ofSeconds(Math.max(runtimeOptions.eventOutOfOrdernessSeconds(), 0L));
        return eventStream.assignTimestampsAndWatermarks(WatermarkStrategy
                .<RiskEvent>forBoundedOutOfOrderness(outOfOrderness)
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

    private static <T> void sinkStream(String streamName,
                                       DataStream<T> stream,
                                       DecisionEngineJobOptions.StreamSinkOptions sinkOptions) {
        if (sinkOptions.sinkType() == DecisionEngineJobOptions.StreamSinkType.KAFKA) {
            stream.sinkTo(buildKafkaSink(sinkOptions))
                    .name(streamName + "-kafka-sink");
            return;
        }
        stream.print(streamName);
    }

    private static <T> KafkaSink<T> buildKafkaSink(DecisionEngineJobOptions.StreamSinkOptions sinkOptions) {
        return KafkaSink.<T>builder()
                .setBootstrapServers(sinkOptions.kafkaBootstrapServers())
                .setDeliveryGuarantee(DeliveryGuarantee.AT_LEAST_ONCE)
                .setRecordSerializer(KafkaRecordSerializationSchema.<T>builder()
                        .setTopic(sinkOptions.kafkaTopic())
                        .setKeySerializationSchema(new EngineKafkaKeySerializationSchema<>())
                        .setValueSerializationSchema(new EngineJsonSerializationSchema<>())
                        .build())
                .build();
    }

    private static EngineLookupServiceFactory buildLookupServiceFactory(
            DecisionEngineJobOptions.LookupOptions lookupOptions) {
        if (lookupOptions == null || lookupOptions.sourceType() == DecisionEngineJobOptions.LookupSourceType.DEMO) {
            return InMemoryLookupService::demo;
        }
        return () -> new RedisLookupService(lookupOptions.redisConfig());
    }

    private static Configuration localExecutionConfiguration(DecisionEngineJobOptions.RuntimeOptions options,
                                                             Path localLogFile) {
        Configuration configuration = new Configuration();
        configuration.set(TaskManagerOptions.CPU_CORES, options.taskCpuCores());
        configuration.set(TaskManagerOptions.NUM_TASK_SLOTS, options.taskSlots());
        configuration.set(TaskManagerOptions.TASK_HEAP_MEMORY, MemorySize.ofMebiBytes(options.taskHeapMb()));
        configuration.set(TaskManagerOptions.TASK_OFF_HEAP_MEMORY, MemorySize.ofMebiBytes(options.taskOffHeapMb()));
        configuration.set(TaskManagerOptions.NETWORK_MEMORY_MIN, MemorySize.ofMebiBytes(options.taskNetworkMb()));
        configuration.set(TaskManagerOptions.NETWORK_MEMORY_MAX, MemorySize.ofMebiBytes(options.taskNetworkMb()));
        configuration.set(TaskManagerOptions.MANAGED_MEMORY_SIZE, MemorySize.ofMebiBytes(options.taskManagedMb()));
        configuration.set(BlobServerOptions.PORT, options.blobServerPortRange());
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

    private static Path prepareLocalLogFile(DecisionEngineJobOptions.RuntimeOptions options) throws IOException {
        Path logFile = options.localLogFile() == null || options.localLogFile().isBlank()
                ? Paths.get(System.getProperty("java.io.tmpdir"), "pulsix-engine-job.log")
                : Path.of(options.localLogFile());
        logFile = logFile.toAbsolutePath();
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

    private record EventSourceStreams(DataStream<RiskEvent> eventStream,
                                      DataStream<EngineErrorRecord> inputErrorStream) {
    }

}
