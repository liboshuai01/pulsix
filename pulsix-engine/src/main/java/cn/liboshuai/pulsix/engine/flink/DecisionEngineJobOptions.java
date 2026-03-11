package cn.liboshuai.pulsix.engine.flink;

import cn.liboshuai.pulsix.engine.feature.RedisLookupConfig;
import cn.liboshuai.pulsix.engine.flink.snapshot.SceneSnapshotSourceOptions;
import cn.liboshuai.pulsix.engine.flink.snapshot.SceneSnapshotSourceType;
import org.apache.flink.api.java.utils.ParameterTool;
import org.apache.flink.connector.kafka.source.enumerator.initializer.OffsetsInitializer;

import java.io.Serializable;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

record DecisionEngineJobOptions(ParameterTool parameters,
                                RuntimeOptions runtimeOptions,
                                SceneSnapshotSourceOptions snapshotSourceOptions,
                                EventSourceOptions eventSourceOptions,
                                LookupOptions lookupOptions,
                                OutputOptions outputOptions) {

    private static final String DEFAULT_CONFIG_RESOURCE = "pulsix-engine.properties";
    private static final String CONFIG_FILE_KEY = "pulsix.engine.config-file";
    private static final String JOB_NAME_KEY = "pulsix.engine.job.name";
    private static final String PARALLELISM_KEY = "pulsix.engine.parallelism";
    private static final String OBJECT_REUSE_ENABLED_KEY = "pulsix.engine.object-reuse-enabled";
    private static final String CHECKPOINT_INTERVAL_MS_KEY = "pulsix.engine.checkpoint.interval-ms";
    private static final String CHECKPOINT_MIN_PAUSE_MS_KEY = "pulsix.engine.checkpoint.min-pause-ms";
    private static final String CHECKPOINT_TIMEOUT_MS_KEY = "pulsix.engine.checkpoint.timeout-ms";
    private static final String CHECKPOINT_TOLERABLE_FAILURE_NUMBER_KEY = "pulsix.engine.checkpoint.tolerable-failure-number";
    private static final String STATE_BACKEND_KEY = "pulsix.engine.state-backend";
    private static final String EVENT_OUT_OF_ORDERNESS_SECONDS_KEY = "pulsix.engine.event.out-of-orderness-seconds";
    private static final String TASK_CPU_CORES_KEY = "pulsix.engine.task.cpu-cores";
    private static final String TASK_SLOTS_KEY = "pulsix.engine.task.slots";
    private static final String TASK_HEAP_MB_KEY = "pulsix.engine.task.heap-mb";
    private static final String TASK_OFF_HEAP_MB_KEY = "pulsix.engine.task.off-heap-mb";
    private static final String TASK_NETWORK_MB_KEY = "pulsix.engine.task.network-mb";
    private static final String TASK_MANAGED_MB_KEY = "pulsix.engine.task.managed-mb";
    private static final String LOCAL_LOG_FILE_KEY = "pulsix.engine.local-log-file";
    private static final String BLOB_SERVER_PORT_RANGE_KEY = "pulsix.engine.blob-server-port-range";
    private static final String SNAPSHOT_SOURCE_KEY = "pulsix.engine.snapshot-source";
    private static final String SNAPSHOT_FILE_KEY = "pulsix.engine.snapshot-file";
    private static final String SNAPSHOT_POLL_MS_KEY = "pulsix.engine.snapshot-poll-ms";
    private static final String SNAPSHOT_JDBC_URL_KEY = "pulsix.engine.snapshot-jdbc-url";
    private static final String SNAPSHOT_JDBC_USER_KEY = "pulsix.engine.snapshot-jdbc-user";
    private static final String SNAPSHOT_JDBC_PASSWORD_KEY = "pulsix.engine.snapshot-jdbc-password";
    private static final String SNAPSHOT_SCENE_CODE_KEY = "pulsix.engine.snapshot-scene-code";
    private static final String SNAPSHOT_VERSION_KEY = "pulsix.engine.snapshot-version";
    private static final String SNAPSHOT_JDBC_QUERY_KEY = "pulsix.engine.snapshot-jdbc-query";
    private static final String SNAPSHOT_CDC_HOST_KEY = "pulsix.engine.snapshot-cdc-host";
    private static final String SNAPSHOT_CDC_PORT_KEY = "pulsix.engine.snapshot-cdc-port";
    private static final String SNAPSHOT_CDC_DATABASE_KEY = "pulsix.engine.snapshot-cdc-database";
    private static final String SNAPSHOT_CDC_TABLE_KEY = "pulsix.engine.snapshot-cdc-table";
    private static final String SNAPSHOT_CDC_USER_KEY = "pulsix.engine.snapshot-cdc-user";
    private static final String SNAPSHOT_CDC_PASSWORD_KEY = "pulsix.engine.snapshot-cdc-password";
    private static final String SNAPSHOT_CDC_SERVER_ID_KEY = "pulsix.engine.snapshot-cdc-server-id";
    private static final String SNAPSHOT_CDC_SERVER_TIME_ZONE_KEY = "pulsix.engine.snapshot-cdc-server-time-zone";
    private static final String EVENT_SOURCE_KEY = "pulsix.engine.event-source";
    private static final String KAFKA_BOOTSTRAP_SERVERS_KEY = "pulsix.engine.kafka-bootstrap-servers";
    private static final String EVENT_KAFKA_TOPIC_KEY = "pulsix.engine.event-kafka-topic";
    private static final String EVENT_KAFKA_GROUP_ID_KEY = "pulsix.engine.event-kafka-group-id";
    private static final String EVENT_KAFKA_STARTING_OFFSETS_KEY = "pulsix.engine.event-kafka-starting-offsets";
    private static final String LOOKUP_SOURCE_KEY = "pulsix.engine.lookup-source";
    private static final String LOOKUP_REDIS_HOST_KEY = "pulsix.engine.lookup-redis-host";
    private static final String LOOKUP_REDIS_PORT_KEY = "pulsix.engine.lookup-redis-port";
    private static final String LOOKUP_REDIS_DATABASE_KEY = "pulsix.engine.lookup-redis-database";
    private static final String LOOKUP_REDIS_USER_KEY = "pulsix.engine.lookup-redis-user";
    private static final String LOOKUP_REDIS_PASSWORD_KEY = "pulsix.engine.lookup-redis-password";
    private static final String LOOKUP_REDIS_SSL_KEY = "pulsix.engine.lookup-redis-ssl";
    private static final String LOOKUP_REDIS_CONNECT_TIMEOUT_MS_KEY = "pulsix.engine.lookup-redis-connect-timeout-ms";
    private static final String LOOKUP_REDIS_DEFAULT_TIMEOUT_MS_KEY = "pulsix.engine.lookup-redis-default-timeout-ms";
    private static final String OUTPUT_SINK_KEY = "pulsix.engine.output-sink";
    private static final String RESULT_SINK_KEY = "pulsix.engine.result-sink";
    private static final String LOG_SINK_KEY = "pulsix.engine.log-sink";
    private static final String ERROR_SINK_KEY = "pulsix.engine.error-sink";
    private static final String RESULT_KAFKA_TOPIC_KEY = "pulsix.engine.result-kafka-topic";
    private static final String LOG_KAFKA_TOPIC_KEY = "pulsix.engine.log-kafka-topic";
    private static final String ERROR_KAFKA_TOPIC_KEY = "pulsix.engine.error-kafka-topic";

    private static final Set<String> SUPPORTED_CLI_OPTIONS = Set.of(
            "--config-file", "--config",
            "--snapshot-source", "--snapshot-file", "--snapshot-poll-ms",
            "--snapshot-jdbc-url", "--snapshot-jdbc-user", "--snapshot-jdbc-password",
            "--snapshot-scene-code", "--snapshot-version", "--snapshot-jdbc-query",
            "--snapshot-cdc-host", "--snapshot-cdc-port", "--snapshot-cdc-database",
            "--snapshot-cdc-table", "--snapshot-cdc-user", "--snapshot-cdc-password",
            "--snapshot-cdc-server-id", "--snapshot-cdc-server-time-zone",
            "--event-source", "--kafka-bootstrap-servers", "--event-kafka-topic", "--event-topic",
            "--event-kafka-group-id", "--event-group-id",
            "--event-kafka-starting-offsets", "--event-starting-offsets",
            "--lookup-source", "--lookup-redis-host", "--lookup-redis-port", "--lookup-redis-database",
            "--lookup-redis-user", "--lookup-redis-password", "--lookup-redis-ssl",
            "--lookup-redis-connect-timeout-ms", "--lookup-redis-default-timeout-ms",
            "--output-sink", "--result-sink", "--log-sink", "--error-sink",
            "--result-kafka-topic", "--result-topic", "--log-kafka-topic", "--log-topic",
            "--error-kafka-topic", "--error-topic",
            "--job-name", "--parallelism", "--object-reuse-enabled",
            "--checkpoint-interval-ms", "--checkpoint-min-pause-ms", "--checkpoint-timeout-ms",
            "--checkpoint-tolerable-failure-number", "--state-backend",
            "--event-out-of-orderness-seconds", "--task-cpu-cores", "--task-slots",
            "--task-heap-mb", "--task-off-heap-mb", "--task-network-mb", "--task-managed-mb",
            "--local-log-file", "--blob-server-port-range", "--help", "-h"
    );

    static DecisionEngineJobOptions parse(String[] args) {
        validateArgs(args);
        ParameterTool argumentParameters = normalize(ParameterTool.fromArgs(args == null ? new String[0] : args));
        ParameterTool systemParameters = normalize(ParameterTool.fromSystemProperties());
        ParameterTool defaultParameters = normalize(loadDefaultParameters());
        ParameterTool externalParameters = normalize(loadExternalParameters(argumentParameters, systemParameters));
        ParameterTool parameters = defaultParameters
                .mergeWith(externalParameters)
                .mergeWith(systemParameters)
                .mergeWith(argumentParameters);

        SceneSnapshotSourceType snapshotSourceType = parseEnum(
                string(parameters, "demo", SNAPSHOT_SOURCE_KEY),
                SceneSnapshotSourceType.class,
                "snapshot source"
        );
        EventSourceType eventSourceType = parseEnum(
                string(parameters, "demo", EVENT_SOURCE_KEY),
                EventSourceType.class,
                "event source"
        );
        KafkaStartingOffsets startingOffsets = parseEnum(
                string(parameters, "latest", EVENT_KAFKA_STARTING_OFFSETS_KEY),
                KafkaStartingOffsets.class,
                "event kafka starting offsets"
        );
        LookupSourceType lookupSourceType = parseEnum(
                string(parameters, "demo", LOOKUP_SOURCE_KEY),
                LookupSourceType.class,
                "lookup source"
        );
        StreamSinkType outputSinkType = parseEnum(
                string(parameters, "print", OUTPUT_SINK_KEY),
                StreamSinkType.class,
                "output sink"
        );
        StreamSinkType resultSinkType = parseEnum(
                string(parameters, outputSinkType.name(), RESULT_SINK_KEY),
                StreamSinkType.class,
                "result sink"
        );
        StreamSinkType logSinkType = parseEnum(
                string(parameters, outputSinkType.name(), LOG_SINK_KEY),
                StreamSinkType.class,
                "log sink"
        );
        StreamSinkType errorSinkType = parseEnum(
                string(parameters, outputSinkType.name(), ERROR_SINK_KEY),
                StreamSinkType.class,
                "error sink"
        );
        StateBackendType stateBackendType = parseEnum(
                string(parameters, "hashmap", STATE_BACKEND_KEY),
                StateBackendType.class,
                "state backend"
        );

        RuntimeOptions runtimeOptions = new RuntimeOptions(
                string(parameters, "pulsix-engine-job", JOB_NAME_KEY),
                integer(parameters, 1, PARALLELISM_KEY),
                bool(parameters, true, OBJECT_REUSE_ENABLED_KEY),
                longValue(parameters, 30_000L, CHECKPOINT_INTERVAL_MS_KEY),
                longValue(parameters, 5_000L, CHECKPOINT_MIN_PAUSE_MS_KEY),
                longValue(parameters, 60_000L, CHECKPOINT_TIMEOUT_MS_KEY),
                integer(parameters, 3, CHECKPOINT_TOLERABLE_FAILURE_NUMBER_KEY),
                stateBackendType,
                longValue(parameters, 1L, EVENT_OUT_OF_ORDERNESS_SECONDS_KEY),
                doubleValue(parameters, 1.0d, TASK_CPU_CORES_KEY),
                integer(parameters, 1, TASK_SLOTS_KEY),
                integer(parameters, 256, TASK_HEAP_MB_KEY),
                integer(parameters, 128, TASK_OFF_HEAP_MB_KEY),
                integer(parameters, 64, TASK_NETWORK_MB_KEY),
                integer(parameters, 128, TASK_MANAGED_MB_KEY),
                string(parameters, null, LOCAL_LOG_FILE_KEY),
                string(parameters, "6124-6134", BLOB_SERVER_PORT_RANGE_KEY)
        );

        String kafkaBootstrapServers = string(parameters, "127.0.0.1:29092", KAFKA_BOOTSTRAP_SERVERS_KEY);
        SceneSnapshotSourceOptions snapshotSourceOptions = new SceneSnapshotSourceOptions(
                snapshotSourceType,
                path(parameters, SNAPSHOT_FILE_KEY),
                longValue(parameters, 1_000L, SNAPSHOT_POLL_MS_KEY),
                string(parameters, null, SNAPSHOT_JDBC_URL_KEY),
                string(parameters, null, SNAPSHOT_JDBC_USER_KEY),
                string(parameters, null, SNAPSHOT_JDBC_PASSWORD_KEY),
                string(parameters, null, SNAPSHOT_SCENE_CODE_KEY),
                optionalInteger(parameters, SNAPSHOT_VERSION_KEY),
                string(parameters, null, SNAPSHOT_JDBC_QUERY_KEY),
                string(parameters, null, SNAPSHOT_CDC_HOST_KEY),
                optionalInteger(parameters, SNAPSHOT_CDC_PORT_KEY),
                string(parameters, null, SNAPSHOT_CDC_DATABASE_KEY),
                string(parameters, "scene_release", SNAPSHOT_CDC_TABLE_KEY),
                string(parameters, null, SNAPSHOT_CDC_USER_KEY),
                string(parameters, null, SNAPSHOT_CDC_PASSWORD_KEY),
                string(parameters, null, SNAPSHOT_CDC_SERVER_ID_KEY),
                string(parameters, "UTC", SNAPSHOT_CDC_SERVER_TIME_ZONE_KEY)
        );
        EventSourceOptions eventSourceOptions = new EventSourceOptions(
                eventSourceType,
                kafkaBootstrapServers,
                string(parameters, "pulsix.event.standard", EVENT_KAFKA_TOPIC_KEY),
                string(parameters, "pulsix-engine", EVENT_KAFKA_GROUP_ID_KEY),
                startingOffsets
        );
        LookupOptions lookupOptions = new LookupOptions(
                lookupSourceType,
                new RedisLookupConfig(
                        string(parameters, "127.0.0.1", LOOKUP_REDIS_HOST_KEY),
                        integer(parameters, 6379, LOOKUP_REDIS_PORT_KEY),
                        integer(parameters, 0, LOOKUP_REDIS_DATABASE_KEY),
                        string(parameters, null, LOOKUP_REDIS_USER_KEY),
                        string(parameters, null, LOOKUP_REDIS_PASSWORD_KEY),
                        bool(parameters, false, LOOKUP_REDIS_SSL_KEY),
                        integer(parameters, 50, LOOKUP_REDIS_CONNECT_TIMEOUT_MS_KEY),
                        integer(parameters, 20, LOOKUP_REDIS_DEFAULT_TIMEOUT_MS_KEY)
                )
        );
        OutputOptions outputOptions = new OutputOptions(
                new StreamSinkOptions(resultSinkType,
                        kafkaBootstrapServers,
                        string(parameters, "pulsix.decision.result", RESULT_KAFKA_TOPIC_KEY)),
                new StreamSinkOptions(logSinkType,
                        kafkaBootstrapServers,
                        string(parameters, "pulsix.decision.log", LOG_KAFKA_TOPIC_KEY)),
                new StreamSinkOptions(errorSinkType,
                        kafkaBootstrapServers,
                        string(parameters, "pulsix.engine.error", ERROR_KAFKA_TOPIC_KEY))
        );
        return new DecisionEngineJobOptions(parameters,
                runtimeOptions,
                snapshotSourceOptions,
                eventSourceOptions,
                lookupOptions,
                outputOptions);
    }

    private static ParameterTool loadDefaultParameters() {
        try (InputStream inputStream = DecisionEngineJobOptions.class.getClassLoader()
                .getResourceAsStream(DEFAULT_CONFIG_RESOURCE)) {
            if (inputStream == null) {
                throw new IllegalStateException("default config resource not found: " + DEFAULT_CONFIG_RESOURCE);
            }
            return ParameterTool.fromPropertiesFile(inputStream);
        } catch (IOException exception) {
            throw new IllegalStateException("load default config failed: " + DEFAULT_CONFIG_RESOURCE, exception);
        }
    }

    private static ParameterTool loadExternalParameters(ParameterTool argumentParameters,
                                                        ParameterTool systemParameters) {
        String configFile = string(argumentParameters, null, CONFIG_FILE_KEY);
        if (configFile == null) {
            configFile = string(systemParameters, null, CONFIG_FILE_KEY);
        }
        if (configFile == null) {
            return ParameterTool.fromMap(Map.of());
        }
        try {
            return ParameterTool.fromPropertiesFile(Path.of(configFile).toFile());
        } catch (IOException exception) {
            throw new IllegalArgumentException("load config file failed: " + configFile, exception);
        }
    }

    private static ParameterTool normalize(ParameterTool parameters) {
        Map<String, String> normalized = new LinkedHashMap<>();
        parameters.toMap().forEach((key, value) -> normalized.put(canonicalKey(key), value));
        return ParameterTool.fromMap(normalized);
    }

    private static String canonicalKey(String key) {
        return switch (key) {
            case "config", "config-file", CONFIG_FILE_KEY -> CONFIG_FILE_KEY;
            case "job-name", JOB_NAME_KEY -> JOB_NAME_KEY;
            case "parallelism", PARALLELISM_KEY -> PARALLELISM_KEY;
            case "object-reuse-enabled", OBJECT_REUSE_ENABLED_KEY -> OBJECT_REUSE_ENABLED_KEY;
            case "checkpoint-interval-ms", CHECKPOINT_INTERVAL_MS_KEY -> CHECKPOINT_INTERVAL_MS_KEY;
            case "checkpoint-min-pause-ms", CHECKPOINT_MIN_PAUSE_MS_KEY -> CHECKPOINT_MIN_PAUSE_MS_KEY;
            case "checkpoint-timeout-ms", CHECKPOINT_TIMEOUT_MS_KEY -> CHECKPOINT_TIMEOUT_MS_KEY;
            case "checkpoint-tolerable-failure-number", CHECKPOINT_TOLERABLE_FAILURE_NUMBER_KEY -> CHECKPOINT_TOLERABLE_FAILURE_NUMBER_KEY;
            case "state-backend", STATE_BACKEND_KEY -> STATE_BACKEND_KEY;
            case "event-out-of-orderness-seconds", EVENT_OUT_OF_ORDERNESS_SECONDS_KEY -> EVENT_OUT_OF_ORDERNESS_SECONDS_KEY;
            case "task-cpu-cores", TASK_CPU_CORES_KEY -> TASK_CPU_CORES_KEY;
            case "task-slots", TASK_SLOTS_KEY -> TASK_SLOTS_KEY;
            case "task-heap-mb", TASK_HEAP_MB_KEY -> TASK_HEAP_MB_KEY;
            case "task-off-heap-mb", TASK_OFF_HEAP_MB_KEY -> TASK_OFF_HEAP_MB_KEY;
            case "task-network-mb", TASK_NETWORK_MB_KEY -> TASK_NETWORK_MB_KEY;
            case "task-managed-mb", TASK_MANAGED_MB_KEY -> TASK_MANAGED_MB_KEY;
            case "local-log-file", LOCAL_LOG_FILE_KEY -> LOCAL_LOG_FILE_KEY;
            case "blob-server-port-range", BLOB_SERVER_PORT_RANGE_KEY -> BLOB_SERVER_PORT_RANGE_KEY;
            case "snapshot-source", SNAPSHOT_SOURCE_KEY -> SNAPSHOT_SOURCE_KEY;
            case "snapshot-file", SNAPSHOT_FILE_KEY -> SNAPSHOT_FILE_KEY;
            case "snapshot-poll-ms", SNAPSHOT_POLL_MS_KEY -> SNAPSHOT_POLL_MS_KEY;
            case "snapshot-jdbc-url", SNAPSHOT_JDBC_URL_KEY -> SNAPSHOT_JDBC_URL_KEY;
            case "snapshot-jdbc-user", SNAPSHOT_JDBC_USER_KEY -> SNAPSHOT_JDBC_USER_KEY;
            case "snapshot-jdbc-password", SNAPSHOT_JDBC_PASSWORD_KEY -> SNAPSHOT_JDBC_PASSWORD_KEY;
            case "snapshot-scene-code", SNAPSHOT_SCENE_CODE_KEY -> SNAPSHOT_SCENE_CODE_KEY;
            case "snapshot-version", SNAPSHOT_VERSION_KEY -> SNAPSHOT_VERSION_KEY;
            case "snapshot-jdbc-query", SNAPSHOT_JDBC_QUERY_KEY -> SNAPSHOT_JDBC_QUERY_KEY;
            case "snapshot-cdc-host", SNAPSHOT_CDC_HOST_KEY -> SNAPSHOT_CDC_HOST_KEY;
            case "snapshot-cdc-port", SNAPSHOT_CDC_PORT_KEY -> SNAPSHOT_CDC_PORT_KEY;
            case "snapshot-cdc-database", SNAPSHOT_CDC_DATABASE_KEY -> SNAPSHOT_CDC_DATABASE_KEY;
            case "snapshot-cdc-table", SNAPSHOT_CDC_TABLE_KEY -> SNAPSHOT_CDC_TABLE_KEY;
            case "snapshot-cdc-user", SNAPSHOT_CDC_USER_KEY -> SNAPSHOT_CDC_USER_KEY;
            case "snapshot-cdc-password", SNAPSHOT_CDC_PASSWORD_KEY -> SNAPSHOT_CDC_PASSWORD_KEY;
            case "snapshot-cdc-server-id", SNAPSHOT_CDC_SERVER_ID_KEY -> SNAPSHOT_CDC_SERVER_ID_KEY;
            case "snapshot-cdc-server-time-zone", SNAPSHOT_CDC_SERVER_TIME_ZONE_KEY -> SNAPSHOT_CDC_SERVER_TIME_ZONE_KEY;
            case "event-source", EVENT_SOURCE_KEY -> EVENT_SOURCE_KEY;
            case "kafka-bootstrap-servers", KAFKA_BOOTSTRAP_SERVERS_KEY -> KAFKA_BOOTSTRAP_SERVERS_KEY;
            case "event-topic", "event-kafka-topic", EVENT_KAFKA_TOPIC_KEY -> EVENT_KAFKA_TOPIC_KEY;
            case "event-group-id", "event-kafka-group-id", EVENT_KAFKA_GROUP_ID_KEY -> EVENT_KAFKA_GROUP_ID_KEY;
            case "event-starting-offsets", "event-kafka-starting-offsets", EVENT_KAFKA_STARTING_OFFSETS_KEY -> EVENT_KAFKA_STARTING_OFFSETS_KEY;
            case "lookup-source", LOOKUP_SOURCE_KEY -> LOOKUP_SOURCE_KEY;
            case "lookup-redis-host", LOOKUP_REDIS_HOST_KEY -> LOOKUP_REDIS_HOST_KEY;
            case "lookup-redis-port", LOOKUP_REDIS_PORT_KEY -> LOOKUP_REDIS_PORT_KEY;
            case "lookup-redis-database", LOOKUP_REDIS_DATABASE_KEY -> LOOKUP_REDIS_DATABASE_KEY;
            case "lookup-redis-user", LOOKUP_REDIS_USER_KEY -> LOOKUP_REDIS_USER_KEY;
            case "lookup-redis-password", LOOKUP_REDIS_PASSWORD_KEY -> LOOKUP_REDIS_PASSWORD_KEY;
            case "lookup-redis-ssl", LOOKUP_REDIS_SSL_KEY -> LOOKUP_REDIS_SSL_KEY;
            case "lookup-redis-connect-timeout-ms", LOOKUP_REDIS_CONNECT_TIMEOUT_MS_KEY -> LOOKUP_REDIS_CONNECT_TIMEOUT_MS_KEY;
            case "lookup-redis-default-timeout-ms", LOOKUP_REDIS_DEFAULT_TIMEOUT_MS_KEY -> LOOKUP_REDIS_DEFAULT_TIMEOUT_MS_KEY;
            case "output-sink", OUTPUT_SINK_KEY -> OUTPUT_SINK_KEY;
            case "result-sink", RESULT_SINK_KEY -> RESULT_SINK_KEY;
            case "log-sink", LOG_SINK_KEY -> LOG_SINK_KEY;
            case "error-sink", ERROR_SINK_KEY -> ERROR_SINK_KEY;
            case "result-topic", "result-kafka-topic", RESULT_KAFKA_TOPIC_KEY -> RESULT_KAFKA_TOPIC_KEY;
            case "log-topic", "log-kafka-topic", LOG_KAFKA_TOPIC_KEY -> LOG_KAFKA_TOPIC_KEY;
            case "error-topic", "error-kafka-topic", ERROR_KAFKA_TOPIC_KEY -> ERROR_KAFKA_TOPIC_KEY;
            default -> key;
        };
    }

    private static void validateArgs(String[] args) {
        if (args == null || args.length == 0) {
            return;
        }
        for (int index = 0; index < args.length; index++) {
            String arg = args[index];
            if ("-h".equals(arg) || "--help".equals(arg)) {
                throw new IllegalArgumentException(usage());
            }
            if (!arg.startsWith("--")) {
                throw new IllegalArgumentException("unknown argument: " + arg + System.lineSeparator() + usage());
            }
            if (!SUPPORTED_CLI_OPTIONS.contains(arg)) {
                throw new IllegalArgumentException("unknown argument: " + arg + System.lineSeparator() + usage());
            }
            if (index + 1 >= args.length) {
                throw new IllegalArgumentException("missing value for " + arg + System.lineSeparator() + usage());
            }
            index++;
        }
    }

    private static String string(ParameterTool parameters, String defaultValue, String... keys) {
        for (String key : keys) {
            String value = parameters.get(key);
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return defaultValue;
    }

    private static Path path(ParameterTool parameters, String... keys) {
        String value = string(parameters, null, keys);
        return value == null ? null : Path.of(value);
    }

    private static Integer optionalInteger(ParameterTool parameters, String... keys) {
        String value = string(parameters, null, keys);
        return value == null ? null : Integer.parseInt(value);
    }

    private static int integer(ParameterTool parameters, int defaultValue, String... keys) {
        String value = string(parameters, null, keys);
        return value == null ? defaultValue : Integer.parseInt(value);
    }

    private static long longValue(ParameterTool parameters, long defaultValue, String... keys) {
        String value = string(parameters, null, keys);
        return value == null ? defaultValue : Long.parseLong(value);
    }

    private static boolean bool(ParameterTool parameters, boolean defaultValue, String... keys) {
        String value = string(parameters, null, keys);
        return value == null ? defaultValue : Boolean.parseBoolean(value);
    }

    private static double doubleValue(ParameterTool parameters, double defaultValue, String... keys) {
        String value = string(parameters, null, keys);
        return value == null ? defaultValue : Double.parseDouble(value);
    }

    private static <E extends Enum<E>> E parseEnum(String value, Class<E> enumType, String optionName) {
        try {
            return Enum.valueOf(enumType, normalizeEnumValue(value));
        } catch (Exception exception) {
            throw new IllegalArgumentException("invalid " + optionName + ": " + value
                    + ", expected one of " + allowedValues(enumType));
        }
    }

    private static String normalizeEnumValue(String value) {
        return value.trim().replace('-', '_').toUpperCase(Locale.ROOT);
    }

    private static <E extends Enum<E>> String allowedValues(Class<E> enumType) {
        StringBuilder builder = new StringBuilder();
        E[] constants = enumType.getEnumConstants();
        for (int index = 0; index < constants.length; index++) {
            if (index > 0) {
                builder.append(", ");
            }
            builder.append(constants[index].name().toLowerCase(Locale.ROOT).replace('_', '-'));
        }
        return builder.toString();
    }

    static String usage() {
        return "Usage: DecisionEngineJob [--config-file <path>] "
                + "[--event-source demo|kafka] [--kafka-bootstrap-servers <servers>] "
                + "[--event-kafka-topic <topic>] [--event-kafka-group-id <groupId>] "
                + "[--event-kafka-starting-offsets earliest|latest|committed] "
                + "[--lookup-source demo|redis] [--lookup-redis-host <host>] [--lookup-redis-port <port>] "
                + "[--lookup-redis-database <db>] [--lookup-redis-user <user>] [--lookup-redis-password <password>] "
                + "[--lookup-redis-ssl <true|false>] [--lookup-redis-connect-timeout-ms <ms>] "
                + "[--lookup-redis-default-timeout-ms <ms>] "
                + "[--output-sink print|kafka] [--result-sink print|kafka] "
                + "[--log-sink print|kafka] [--error-sink print|kafka] "
                + "[--result-kafka-topic <topic>] [--log-kafka-topic <topic>] "
                + "[--error-kafka-topic <topic>] [--job-name <name>] [--parallelism <n>] "
                + "[--state-backend hashmap|rocksdb] [--checkpoint-interval-ms <ms>] "
                + "[--task-cpu-cores <cores>] [--task-slots <n>] [--local-log-file <path>] "
                + "[--snapshot-source demo|file|jdbc|cdc] [--snapshot-file <path>] "
                + "[--snapshot-poll-ms <ms>] "
                + "[--snapshot-jdbc-url <url> --snapshot-jdbc-user <user> --snapshot-jdbc-password <password>] "
                + "[--snapshot-scene-code <sceneCode>] [--snapshot-version <version>] [--snapshot-jdbc-query <sql>] "
                + "[--snapshot-cdc-host <host> --snapshot-cdc-port <port> --snapshot-cdc-database <db> "
                + "--snapshot-cdc-table <table> --snapshot-cdc-user <user> --snapshot-cdc-password <password> "
                + "[--snapshot-cdc-server-id <serverId>] [--snapshot-cdc-server-time-zone <tz>]]. "
                + "Parameter priority: classpath defaults < external config file < -D system properties < CLI args.";
    }

    record RuntimeOptions(String jobName,
                          int parallelism,
                          boolean objectReuseEnabled,
                          long checkpointIntervalMs,
                          long checkpointMinPauseMs,
                          long checkpointTimeoutMs,
                          int checkpointTolerableFailureNumber,
                          StateBackendType stateBackendType,
                          long eventOutOfOrdernessSeconds,
                          double taskCpuCores,
                          int taskSlots,
                          int taskHeapMb,
                          int taskOffHeapMb,
                          int taskNetworkMb,
                          int taskManagedMb,
                          String localLogFile,
                          String blobServerPortRange) {
    }

    record EventSourceOptions(EventSourceType sourceType,
                              String kafkaBootstrapServers,
                              String kafkaTopic,
                              String kafkaGroupId,
                              KafkaStartingOffsets kafkaStartingOffsets) {
    }

    record LookupOptions(LookupSourceType sourceType,
                         RedisLookupConfig redisConfig) implements Serializable {
    }

    record OutputOptions(StreamSinkOptions decisionResultSinkOptions,
                         StreamSinkOptions decisionLogSinkOptions,
                         StreamSinkOptions engineErrorSinkOptions) {
    }

    record StreamSinkOptions(StreamSinkType sinkType,
                             String kafkaBootstrapServers,
                             String kafkaTopic) {
    }

    enum EventSourceType {
        DEMO,
        KAFKA
    }

    enum LookupSourceType {
        DEMO,
        REDIS
    }

    enum StreamSinkType {
        PRINT,
        KAFKA
    }

    enum StateBackendType {
        HASHMAP,
        ROCKSDB
    }

    enum KafkaStartingOffsets {
        EARLIEST,
        LATEST,
        COMMITTED;

        OffsetsInitializer toOffsetsInitializer() {
            return switch (this) {
                case EARLIEST -> OffsetsInitializer.earliest();
                case LATEST -> OffsetsInitializer.latest();
                case COMMITTED -> OffsetsInitializer.committedOffsets();
            };
        }
    }

}
