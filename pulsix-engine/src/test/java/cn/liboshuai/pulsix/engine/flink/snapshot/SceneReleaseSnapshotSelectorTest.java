package cn.liboshuai.pulsix.engine.flink.snapshot;

import cn.liboshuai.pulsix.engine.demo.DemoFixtures;
import cn.liboshuai.pulsix.engine.model.SceneSnapshot;
import cn.liboshuai.pulsix.engine.model.SceneSnapshotEnvelope;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SceneReleaseSnapshotSelectorTest {

    @Test
    void shouldKeepLatestEffectiveSnapshotPerSceneForBootstrap() {
        SceneSnapshotEnvelope version12 = envelope("TRADE_RISK", 12, "checksum-v12", Instant.parse("2026-03-07T20:00:10Z"));
        SceneSnapshotEnvelope version13 = envelope("TRADE_RISK", 13, "checksum-v13", Instant.parse("2026-03-07T20:05:10Z"));
        SceneSnapshotEnvelope version14 = envelope("TRADE_RISK", 14, "checksum-v14", Instant.parse("2026-03-12T20:05:10Z"));
        SceneSnapshotEnvelope fraudScene = envelope("FRAUD_RISK", 2, "fraud-v2", Instant.parse("2026-03-07T21:00:00Z"));
        SceneSnapshotSourceOptions options = new SceneSnapshotSourceOptions(SceneSnapshotSourceType.JDBC,
                null, 1_000L, "jdbc:mock", "user", "pwd", null, null, null,
                null, 3306, null, "scene_release", null, null, null, "UTC");

        List<SceneSnapshotEnvelope> selected = SceneReleaseSnapshotSelector.selectBootstrapSnapshots(
                List.of(version12, version13, version14, fraudScene),
                options,
                Instant.parse("2026-03-11T00:00:00Z"));

        assertEquals(List.of("TRADE_RISK", "FRAUD_RISK"), selected.stream().map(SceneSnapshotEnvelope::getSceneCode).toList());
        assertEquals(List.of(13, 2), selected.stream().map(SceneSnapshotEnvelope::getVersion).toList());
    }

    @Test
    void shouldKeepExactRequestedVersionEvenWhenEffectiveFromIsFuture() {
        SceneSnapshotEnvelope version14 = envelope("TRADE_RISK", 14, "checksum-v14", Instant.parse("2026-03-12T20:05:10Z"));
        SceneSnapshotSourceOptions options = new SceneSnapshotSourceOptions(SceneSnapshotSourceType.JDBC,
                null, 1_000L, "jdbc:mock", "user", "pwd", "TRADE_RISK", 14, null,
                null, 3306, null, "scene_release", null, null, null, "UTC");

        List<SceneSnapshotEnvelope> selected = SceneReleaseSnapshotSelector.selectBootstrapSnapshots(
                List.of(version14),
                options,
                Instant.parse("2026-03-11T00:00:00Z"));

        assertEquals(1, selected.size());
        assertEquals(14, selected.get(0).getVersion());
    }

    private SceneSnapshotEnvelope envelope(String sceneCode, int version, String checksum, Instant effectiveFrom) {
        SceneSnapshot snapshot = DemoFixtures.demoSnapshot();
        snapshot.setSceneCode(sceneCode);
        snapshot.setSnapshotId(sceneCode + "_v" + version);
        snapshot.setVersion(version);
        snapshot.setChecksum(checksum);
        snapshot.setEffectiveFrom(effectiveFrom);
        snapshot.setPublishedAt(effectiveFrom.minusSeconds(10));
        SceneSnapshotEnvelope envelope = new SceneSnapshotEnvelope();
        envelope.setSceneCode(sceneCode);
        envelope.setVersion(version);
        envelope.setChecksum(checksum);
        envelope.setPublishedAt(snapshot.getPublishedAt());
        envelope.setEffectiveFrom(effectiveFrom);
        envelope.setSnapshot(snapshot);
        return envelope;
    }

}
