package cn.liboshuai.pulsix.engine.flink.snapshot;

import cn.liboshuai.pulsix.engine.demo.DemoFixtures;
import cn.liboshuai.pulsix.engine.flink.typeinfo.EngineTypeInfos;
import cn.liboshuai.pulsix.engine.model.SceneSnapshotEnvelope;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.functions.source.SourceFunction;

public final class SceneSnapshotSourceFactory {

    private SceneSnapshotSourceFactory() {
    }

    public static DataStream<SceneSnapshotEnvelope> build(StreamExecutionEnvironment env,
                                                          SceneSnapshotSourceOptions options) {
        SceneSnapshotSourceOptions effectiveOptions = options == null ? SceneSnapshotSourceOptions.demo() : options;
        return switch (effectiveOptions.type()) {
            case CDC -> MySqlCdcSceneSnapshotSourceFactory.build(env, effectiveOptions);
            case DEMO, FILE, JDBC -> buildSourceFunctionStream(env, effectiveOptions);
        };
    }

    private static DataStream<SceneSnapshotEnvelope> buildSourceFunctionStream(StreamExecutionEnvironment env,
                                                                               SceneSnapshotSourceOptions options) {
        SourceFunction<SceneSnapshotEnvelope> source = switch (options.type()) {
            case DEMO -> new DemoSceneSnapshotSource();
            case FILE -> new FilePollingSceneSnapshotSource(options.filePath(), options.pollIntervalMs());
            case JDBC -> new JdbcPollingSceneSnapshotSource(options);
            case CDC -> throw new IllegalArgumentException("CDC source must be built via MySqlCdcSceneSnapshotSourceFactory");
        };
        String sourceName = "scene-snapshot-source-" + options.type().name().toLowerCase();
        return env.addSource(source, EngineTypeInfos.sceneSnapshotEnvelope())
                .name(sourceName)
                .uid(sourceName);
    }

    private static class DemoSceneSnapshotSource implements SourceFunction<SceneSnapshotEnvelope> {

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
