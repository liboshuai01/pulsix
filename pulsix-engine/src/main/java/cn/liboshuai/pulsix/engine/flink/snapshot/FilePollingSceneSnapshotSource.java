package cn.liboshuai.pulsix.engine.flink.snapshot;

import cn.liboshuai.pulsix.engine.model.SceneSnapshotEnvelope;
import cn.liboshuai.pulsix.engine.snapshot.SceneSnapshotEnvelopes;
import org.apache.flink.streaming.api.functions.source.SourceFunction;

import java.nio.file.Files;
import java.nio.file.Path;

public class FilePollingSceneSnapshotSource implements SourceFunction<SceneSnapshotEnvelope> {

    private final Path filePath;

    private final long pollIntervalMs;

    private volatile boolean running = true;

    public FilePollingSceneSnapshotSource(Path filePath, long pollIntervalMs) {
        this.filePath = filePath;
        this.pollIntervalMs = Math.max(100L, pollIntervalMs);
    }

    @Override
    public void run(SourceContext<SceneSnapshotEnvelope> context) throws Exception {
        long lastModifiedAt = Long.MIN_VALUE;
        SceneSnapshotEnvelope lastEnvelope = null;
        while (running) {
            ensureFileExists();
            long currentModifiedAt = Files.getLastModifiedTime(filePath).toMillis();
            if (currentModifiedAt != lastModifiedAt) {
                SceneSnapshotEnvelope envelope = SceneSnapshotEnvelopes.parse(Files.readString(filePath));
                if (shouldEmit(lastEnvelope, envelope)) {
                    synchronized (context.getCheckpointLock()) {
                        context.collect(envelope);
                    }
                    lastEnvelope = envelope;
                }
                lastModifiedAt = currentModifiedAt;
            }
            sleepQuietly();
        }
    }

    @Override
    public void cancel() {
        this.running = false;
    }

    private boolean shouldEmit(SceneSnapshotEnvelope previous, SceneSnapshotEnvelope current) {
        if (previous == null) {
            return true;
        }
        return !current.getSceneCode().equals(previous.getSceneCode())
                || !current.getVersion().equals(previous.getVersion())
                || !current.getChecksum().equals(previous.getChecksum());
    }

    private void ensureFileExists() {
        if (Files.notExists(filePath)) {
            throw new IllegalStateException("snapshot file not found: " + filePath);
        }
    }

    private void sleepQuietly() {
        try {
            Thread.sleep(pollIntervalMs);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            running = false;
        }
    }

}
