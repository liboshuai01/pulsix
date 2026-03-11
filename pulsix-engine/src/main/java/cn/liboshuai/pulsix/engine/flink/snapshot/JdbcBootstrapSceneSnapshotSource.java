package cn.liboshuai.pulsix.engine.flink.snapshot;

import cn.liboshuai.pulsix.engine.model.SceneSnapshotEnvelope;
import org.apache.flink.streaming.api.functions.source.SourceFunction;

public class JdbcBootstrapSceneSnapshotSource implements SourceFunction<SceneSnapshotEnvelope> {

    private final SceneSnapshotSourceOptions options;

    private volatile boolean running = true;

    public JdbcBootstrapSceneSnapshotSource(SceneSnapshotSourceOptions options) {
        this.options = options;
    }

    @Override
    public void run(SourceContext<SceneSnapshotEnvelope> context) throws Exception {
        if (!running) {
            return;
        }
        SceneReleaseJdbcSnapshotLoader loader = new SceneReleaseJdbcSnapshotLoader(options);
        for (SceneSnapshotEnvelope envelope : loader.loadSnapshots()) {
            if (!running) {
                return;
            }
            synchronized (context.getCheckpointLock()) {
                context.collect(envelope);
            }
        }
    }

    @Override
    public void cancel() {
        this.running = false;
    }

}
