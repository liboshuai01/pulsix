package cn.liboshuai.pulsix.engine.flink.snapshot;

import cn.liboshuai.pulsix.engine.demo.DemoFixtures;
import cn.liboshuai.pulsix.engine.json.EngineJson;
import cn.liboshuai.pulsix.engine.model.SceneSnapshot;
import cn.liboshuai.pulsix.engine.model.SceneSnapshotEnvelope;
import cn.liboshuai.pulsix.engine.snapshot.SceneReleaseRecord;
import org.apache.flink.streaming.api.functions.source.SourceFunction;
import org.apache.flink.streaming.api.watermark.Watermark;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FilePollingSceneSnapshotSourceTest {

    @TempDir
    Path tempDir;

    @Test
    void shouldEmitUpdatedSnapshotsWhenFileChanges() throws Exception {
        Path snapshotFile = tempDir.resolve("scene-release.json");
        Files.writeString(snapshotFile, sceneReleaseRecordJson(DemoFixtures.demoSnapshot(), null));

        FilePollingSceneSnapshotSource source = new FilePollingSceneSnapshotSource(snapshotFile, 50L);
        CollectingSourceContext context = new CollectingSourceContext();
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Future<?> future = executor.submit(() -> {
            try {
                source.run(context);
            } catch (Exception exception) {
                throw new RuntimeException(exception);
            }
        });

        try {
            awaitCollected(context, 1);

            SceneSnapshot version13 = copySnapshot(DemoFixtures.demoSnapshot());
            version13.setSnapshotId("TRADE_RISK_v13");
            version13.setVersion(13);
            version13.setChecksum("8d2041a7cf8f47b4b6b0f91d2ab8d9d1");
            version13.setPublishedAt(Instant.parse("2026-03-07T20:05:00Z"));
            version13.setEffectiveFrom(Instant.parse("2026-03-07T20:05:10Z"));

            Thread.sleep(120L);
            Files.writeString(snapshotFile, sceneReleaseRecordJson(version13, null));

            awaitCollected(context, 2);
            assertEquals(List.of(12, 13), context.values().stream().map(SceneSnapshotEnvelope::getVersion).toList());
            assertEquals(List.of("8d2041a7cf8f47b4b6b0f91d2ab8d9d0", "8d2041a7cf8f47b4b6b0f91d2ab8d9d1"),
                    context.values().stream().map(SceneSnapshotEnvelope::getChecksum).toList());
        } finally {
            source.cancel();
            future.get(2, TimeUnit.SECONDS);
            executor.shutdownNow();
        }
    }

    private void awaitCollected(CollectingSourceContext context, int expectedCount) throws InterruptedException {
        long deadline = System.currentTimeMillis() + 3_000L;
        while (System.currentTimeMillis() < deadline) {
            if (context.values().size() >= expectedCount) {
                return;
            }
            Thread.sleep(25L);
        }
        assertEquals(expectedCount, context.values().size());
    }

    private String sceneReleaseRecordJson(SceneSnapshot snapshot, Integer rollbackFromVersion) {
        SceneReleaseRecord record = new SceneReleaseRecord();
        record.setSceneCode(snapshot.getSceneCode());
        record.setVersionNo(snapshot.getVersion());
        record.setChecksum(snapshot.getChecksum());
        record.setPublishedAt(snapshot.getPublishedAt());
        record.setEffectiveFrom(snapshot.getEffectiveFrom());
        record.setRollbackFromVersion(rollbackFromVersion);
        record.setSnapshotJson(EngineJson.read(EngineJson.write(snapshot), Object.class));
        return EngineJson.write(record);
    }

    private SceneSnapshot copySnapshot(SceneSnapshot snapshot) {
        return EngineJson.read(EngineJson.write(snapshot), SceneSnapshot.class);
    }

    private static final class CollectingSourceContext implements SourceFunction.SourceContext<SceneSnapshotEnvelope> {

        private final Object checkpointLock = new Object();

        private final List<SceneSnapshotEnvelope> values = Collections.synchronizedList(new ArrayList<>());

        @Override
        public void collect(SceneSnapshotEnvelope element) {
            values.add(copyEnvelope(element));
        }

        @Override
        public void collectWithTimestamp(SceneSnapshotEnvelope element, long timestamp) {
            collect(element);
        }

        @Override
        public void emitWatermark(Watermark mark) {
        }

        @Override
        public void markAsTemporarilyIdle() {
        }

        @Override
        public Object getCheckpointLock() {
            return checkpointLock;
        }

        @Override
        public void close() {
        }

        public List<SceneSnapshotEnvelope> values() {
            synchronized (values) {
                return new ArrayList<>(values);
            }
        }

        private static SceneSnapshotEnvelope copyEnvelope(SceneSnapshotEnvelope envelope) {
            return EngineJson.read(EngineJson.write(envelope), SceneSnapshotEnvelope.class);
        }
    }

}
