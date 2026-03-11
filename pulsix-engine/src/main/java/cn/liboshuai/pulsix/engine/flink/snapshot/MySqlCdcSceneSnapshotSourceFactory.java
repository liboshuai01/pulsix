package cn.liboshuai.pulsix.engine.flink.snapshot;

import cn.liboshuai.pulsix.engine.flink.typeinfo.EngineTypeInfos;
import cn.liboshuai.pulsix.engine.model.SceneSnapshotEnvelope;
import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.api.common.functions.FlatMapFunction;
import org.apache.flink.cdc.connectors.mysql.source.MySqlSource;
import org.apache.flink.cdc.connectors.mysql.source.MySqlSourceBuilder;
import org.apache.flink.cdc.connectors.mysql.table.StartupOptions;
import org.apache.flink.cdc.debezium.JsonDebeziumDeserializationSchema;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.util.Collector;

public final class MySqlCdcSceneSnapshotSourceFactory {

    private MySqlCdcSceneSnapshotSourceFactory() {
    }

    public static DataStream<SceneSnapshotEnvelope> build(StreamExecutionEnvironment env,
                                                          SceneSnapshotSourceOptions options) {
        SceneSnapshotSourceOptions bootstrapOptions = options.asJdbcBootstrapOptions();
        DataStream<SceneSnapshotEnvelope> bootstrapStream = env.addSource(
                        new JdbcBootstrapSceneSnapshotSource(bootstrapOptions),
                        EngineTypeInfos.sceneSnapshotEnvelope())
                .name("scene-snapshot-source-cdc-bootstrap")
                .uid("scene-snapshot-source-cdc-bootstrap");

        MySqlSourceBuilder<String> builder = MySqlSource.<String>builder()
                .hostname(options.cdcHostname())
                .port(options.cdcPort())
                .databaseList(options.cdcDatabase())
                .tableList(options.cdcQualifiedTable())
                .username(options.cdcUsername())
                .password(options.cdcPassword())
                .serverTimeZone(options.cdcServerTimeZone())
                .startupOptions(StartupOptions.latest())
                .deserializer(new JsonDebeziumDeserializationSchema(false));

        if (options.cdcServerId() != null && !options.cdcServerId().isBlank()) {
            builder.serverId(options.cdcServerId());
        }

        MySqlSource<String> cdcSource = builder.build();

        DataStream<SceneSnapshotEnvelope> changeStream = env
                .fromSource(cdcSource, WatermarkStrategy.noWatermarks(), "scene-snapshot-source-cdc")
                .flatMap(new CdcPayloadToEnvelopeFlatMap(options))
                .returns(EngineTypeInfos.sceneSnapshotEnvelope())
                .name("scene-snapshot-source-cdc")
                .uid("scene-snapshot-source-cdc");

        return bootstrapStream.union(changeStream);
    }

    private static final class CdcPayloadToEnvelopeFlatMap implements FlatMapFunction<String, SceneSnapshotEnvelope> {

        private final SceneSnapshotSourceOptions options;

        private CdcPayloadToEnvelopeFlatMap(SceneSnapshotSourceOptions options) {
            this.options = options;
        }

        @Override
        public void flatMap(String payload, Collector<SceneSnapshotEnvelope> collector) {
            SceneReleaseCdcPayloadParser.parse(payload, options).ifPresent(collector::collect);
        }
    }

}
