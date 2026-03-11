package cn.liboshuai.pulsix.engine.flink.snapshot;

import cn.liboshuai.pulsix.engine.model.SceneSnapshotEnvelope;
import org.apache.flink.streaming.api.functions.source.SourceFunction;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class JdbcPollingSceneSnapshotSource implements SourceFunction<SceneSnapshotEnvelope> {

    private final SceneSnapshotSourceOptions options;

    private volatile boolean running = true;

    public JdbcPollingSceneSnapshotSource(SceneSnapshotSourceOptions options) {
        this.options = options;
    }

    @Override
    public void run(SourceContext<SceneSnapshotEnvelope> context) throws Exception {
        SceneReleaseJdbcSnapshotLoader loader = new SceneReleaseJdbcSnapshotLoader(options);
        Map<String, SceneReleaseJdbcSnapshotLoader.SnapshotMarker> markers = new LinkedHashMap<>();
        while (running) {
            List<SceneSnapshotEnvelope> snapshots = loader.loadSnapshots();
            for (SceneSnapshotEnvelope envelope : snapshots) {
                if (!running) {
                    return;
                }
                if (!SceneReleaseJdbcSnapshotLoader.shouldEmit(markers.get(envelope.getSceneCode()), envelope)) {
                    continue;
                }
                synchronized (context.getCheckpointLock()) {
                    context.collect(envelope);
                }
            }
            markers = SceneReleaseJdbcSnapshotLoader.updateMarkers(snapshots, markers);
            sleepQuietly();
        }
    }

    @Override
    public void cancel() {
        this.running = false;
    }

    private void sleepQuietly() {
        try {
            Thread.sleep(Math.max(100L, options.pollIntervalMs()));
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            running = false;
        }
    }

}
