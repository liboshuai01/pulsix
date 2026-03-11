package cn.liboshuai.pulsix.engine.flink;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DecisionEngineJobOptionsTest {

    private static final String[] PROPERTY_KEYS = {
            "pulsix.engine.config-file",
            "pulsix.engine.job.name",
            "pulsix.engine.parallelism",
            "pulsix.engine.object-reuse-enabled",
            "pulsix.engine.checkpoint.interval-ms",
            "pulsix.engine.checkpoint.min-pause-ms",
            "pulsix.engine.checkpoint.timeout-ms",
            "pulsix.engine.checkpoint.tolerable-failure-number",
            "pulsix.engine.state-backend",
            "pulsix.engine.event.out-of-orderness-seconds",
            "pulsix.engine.task.cpu-cores",
            "pulsix.engine.task.slots",
            "pulsix.engine.task.heap-mb",
            "pulsix.engine.task.off-heap-mb",
            "pulsix.engine.task.network-mb",
            "pulsix.engine.task.managed-mb",
            "pulsix.engine.local-log-file",
            "pulsix.engine.blob-server-port-range",
            "pulsix.engine.snapshot-source",
            "pulsix.engine.snapshot-file",
            "pulsix.engine.snapshot-poll-ms",
            "pulsix.engine.snapshot-jdbc-url",
            "pulsix.engine.snapshot-jdbc-user",
            "pulsix.engine.snapshot-jdbc-password",
            "pulsix.engine.snapshot-scene-code",
            "pulsix.engine.snapshot-version",
            "pulsix.engine.snapshot-jdbc-query",
            "pulsix.engine.snapshot-cdc-host",
            "pulsix.engine.snapshot-cdc-port",
            "pulsix.engine.snapshot-cdc-database",
            "pulsix.engine.snapshot-cdc-table",
            "pulsix.engine.snapshot-cdc-user",
            "pulsix.engine.snapshot-cdc-password",
            "pulsix.engine.snapshot-cdc-server-id",
            "pulsix.engine.snapshot-cdc-server-time-zone",
            "pulsix.engine.event-source",
            "pulsix.engine.kafka-bootstrap-servers",
            "pulsix.engine.event-kafka-topic",
            "pulsix.engine.event-kafka-group-id",
            "pulsix.engine.event-kafka-starting-offsets",
            "pulsix.engine.lookup-source",
            "pulsix.engine.lookup-redis-host",
            "pulsix.engine.lookup-redis-port",
            "pulsix.engine.lookup-redis-database",
            "pulsix.engine.lookup-redis-user",
            "pulsix.engine.lookup-redis-password",
            "pulsix.engine.lookup-redis-ssl",
            "pulsix.engine.lookup-redis-connect-timeout-ms",
            "pulsix.engine.lookup-redis-default-timeout-ms",
            "pulsix.engine.output-sink",
            "pulsix.engine.result-sink",
            "pulsix.engine.log-sink",
            "pulsix.engine.error-sink",
            "pulsix.engine.result-kafka-topic",
            "pulsix.engine.log-kafka-topic",
            "pulsix.engine.error-kafka-topic"
    };

    @AfterEach
    void tearDown() {
        for (String key : PROPERTY_KEYS) {
            System.clearProperty(key);
        }
    }

    @Test
    void shouldDefaultToDemoSourceAndPrintSinks() {
        DecisionEngineJobOptions options = DecisionEngineJobOptions.parse(new String[0]);

        assertEquals(DecisionEngineJobOptions.EventSourceType.DEMO, options.eventSourceOptions().sourceType());
        assertEquals("pulsix.event.standard", options.eventSourceOptions().kafkaTopic());
        assertEquals(DecisionEngineJobOptions.KafkaStartingOffsets.LATEST,
                options.eventSourceOptions().kafkaStartingOffsets());
        assertEquals(DecisionEngineJobOptions.LookupSourceType.REDIS, options.lookupOptions().sourceType());
        assertEquals("127.0.0.1", options.lookupOptions().redisConfig().host());
        assertEquals("pulsix_redis_123", options.lookupOptions().redisConfig().password());
        assertEquals(DecisionEngineJobOptions.StreamSinkType.PRINT,
                options.outputOptions().decisionResultSinkOptions().sinkType());
        assertEquals(DecisionEngineJobOptions.StreamSinkType.PRINT,
                options.outputOptions().decisionLogSinkOptions().sinkType());
        assertEquals(DecisionEngineJobOptions.StreamSinkType.PRINT,
                options.outputOptions().engineErrorSinkOptions().sinkType());
        assertEquals(1, options.runtimeOptions().parallelism());
        assertEquals(DecisionEngineJobOptions.StateBackendType.HASHMAP, options.runtimeOptions().stateBackendType());
    }

    @Test
    void shouldLoadFromConfigFileAndAllowCliOverride(@TempDir Path tempDir) throws IOException {
        Path configFile = tempDir.resolve("pulsix-engine-local.properties");
        Files.writeString(configFile, """
                pulsix.engine.event-source=kafka
                pulsix.engine.kafka-bootstrap-servers=127.0.0.1:39092
                pulsix.engine.event-kafka-topic=pulsix.event.standard
                pulsix.engine.event-kafka-group-id=pulsix-engine-config
                pulsix.engine.event-kafka-starting-offsets=earliest
                pulsix.engine.output-sink=kafka
                pulsix.engine.result-kafka-topic=pulsix.decision.result
                pulsix.engine.log-kafka-topic=pulsix.decision.log
                pulsix.engine.error-kafka-topic=pulsix.engine.error
                pulsix.engine.parallelism=2
                pulsix.engine.state-backend=rocksdb
                pulsix.engine.local-log-file=/tmp/pulsix-engine-config.log
                pulsix.engine.blob-server-port-range=6150-6155
                """);

        DecisionEngineJobOptions options = DecisionEngineJobOptions.parse(new String[]{
                "--config-file", configFile.toString(),
                "--result-sink", "print"
        });

        assertEquals(DecisionEngineJobOptions.EventSourceType.KAFKA, options.eventSourceOptions().sourceType());
        assertEquals("127.0.0.1:39092", options.eventSourceOptions().kafkaBootstrapServers());
        assertEquals("pulsix-engine-config", options.eventSourceOptions().kafkaGroupId());
        assertEquals(DecisionEngineJobOptions.KafkaStartingOffsets.EARLIEST,
                options.eventSourceOptions().kafkaStartingOffsets());
        assertEquals(DecisionEngineJobOptions.StreamSinkType.PRINT,
                options.outputOptions().decisionResultSinkOptions().sinkType());
        assertEquals(DecisionEngineJobOptions.StreamSinkType.KAFKA,
                options.outputOptions().decisionLogSinkOptions().sinkType());
        assertEquals(2, options.runtimeOptions().parallelism());
        assertEquals(DecisionEngineJobOptions.StateBackendType.ROCKSDB, options.runtimeOptions().stateBackendType());
        assertEquals("/tmp/pulsix-engine-config.log", options.runtimeOptions().localLogFile());
        assertEquals("6150-6155", options.runtimeOptions().blobServerPortRange());
    }

    @Test
    void shouldParseKafkaSourceAndPerStreamSinksFromCli() {
        DecisionEngineJobOptions options = DecisionEngineJobOptions.parse(new String[]{
                "--event-source", "kafka",
                "--kafka-bootstrap-servers", "kafka:9092",
                "--event-kafka-topic", "pulsix.event.standard",
                "--event-kafka-group-id", "pulsix-engine-test",
                "--event-kafka-starting-offsets", "earliest",
                "--output-sink", "print",
                "--result-sink", "kafka",
                "--log-sink", "kafka",
                "--error-sink", "kafka",
                "--parallelism", "3",
                "--state-backend", "rocksdb",
                "--result-kafka-topic", "pulsix.decision.result",
                "--log-kafka-topic", "pulsix.decision.log",
                "--error-kafka-topic", "pulsix.engine.error"
        });

        assertEquals(DecisionEngineJobOptions.EventSourceType.KAFKA, options.eventSourceOptions().sourceType());
        assertEquals("kafka:9092", options.eventSourceOptions().kafkaBootstrapServers());
        assertEquals("pulsix-engine-test", options.eventSourceOptions().kafkaGroupId());
        assertEquals(DecisionEngineJobOptions.KafkaStartingOffsets.EARLIEST,
                options.eventSourceOptions().kafkaStartingOffsets());
        assertEquals(3, options.runtimeOptions().parallelism());
        assertEquals(DecisionEngineJobOptions.StateBackendType.ROCKSDB, options.runtimeOptions().stateBackendType());
        assertEquals(DecisionEngineJobOptions.StreamSinkType.KAFKA,
                options.outputOptions().decisionResultSinkOptions().sinkType());
        assertEquals("pulsix.decision.result", options.outputOptions().decisionResultSinkOptions().kafkaTopic());
        assertEquals(DecisionEngineJobOptions.StreamSinkType.KAFKA,
                options.outputOptions().decisionLogSinkOptions().sinkType());
        assertEquals(DecisionEngineJobOptions.StreamSinkType.KAFKA,
                options.outputOptions().engineErrorSinkOptions().sinkType());
    }

    @Test
    void shouldParseRedisLookupOptionsFromCli() {
        DecisionEngineJobOptions options = DecisionEngineJobOptions.parse(new String[]{
                "--lookup-source", "redis",
                "--lookup-redis-host", "redis-host",
                "--lookup-redis-port", "6380",
                "--lookup-redis-database", "2",
                "--lookup-redis-user", "lookup-user",
                "--lookup-redis-password", "lookup-password",
                "--lookup-redis-ssl", "true",
                "--lookup-redis-connect-timeout-ms", "80",
                "--lookup-redis-default-timeout-ms", "35"
        });

        assertEquals(DecisionEngineJobOptions.LookupSourceType.REDIS, options.lookupOptions().sourceType());
        assertEquals("redis-host", options.lookupOptions().redisConfig().host());
        assertEquals(6380, options.lookupOptions().redisConfig().port());
        assertEquals(2, options.lookupOptions().redisConfig().database());
        assertEquals("lookup-user", options.lookupOptions().redisConfig().user());
        assertEquals("lookup-password", options.lookupOptions().redisConfig().password());
        assertTrue(options.lookupOptions().redisConfig().ssl());
        assertEquals(80, options.lookupOptions().redisConfig().connectTimeoutMs());
        assertEquals(35, options.lookupOptions().redisConfig().defaultTimeoutMs());
    }

    @Test
    void shouldRejectUnknownEventSource() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> DecisionEngineJobOptions.parse(new String[]{"--event-source", "mq"}));

        assertTrue(exception.getMessage().contains("invalid event source"));
        assertTrue(exception.getMessage().contains("demo"));
        assertTrue(exception.getMessage().contains("kafka"));
    }

    @Test
    void shouldRejectUnknownCliOption() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> DecisionEngineJobOptions.parse(new String[]{"--unknown-option", "value"}));

        assertTrue(exception.getMessage().contains("unknown argument"));
    }

}
