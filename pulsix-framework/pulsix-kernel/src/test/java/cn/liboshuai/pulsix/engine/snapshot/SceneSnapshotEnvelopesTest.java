package cn.liboshuai.pulsix.engine.snapshot;

import cn.liboshuai.pulsix.engine.demo.DemoFixtures;
import cn.liboshuai.pulsix.engine.json.EngineJson;
import cn.liboshuai.pulsix.engine.model.PublishType;
import cn.liboshuai.pulsix.engine.model.SceneSnapshot;
import cn.liboshuai.pulsix.engine.model.SceneSnapshotEnvelope;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SceneSnapshotEnvelopesTest {

    @Test
    void shouldParseRawSnapshotJsonIntoEnvelope() {
        SceneSnapshotEnvelope envelope = SceneSnapshotEnvelopes.parse(EngineJson.write(DemoFixtures.demoSnapshot()));

        assertEquals("TRADE_RISK", envelope.getSceneCode());
        assertEquals(12, envelope.getVersion());
        assertEquals("8d2041a7cf8f47b4b6b0f91d2ab8d9d0", envelope.getChecksum());
        assertEquals(PublishType.PUBLISH, envelope.getPublishType());
        assertEquals(envelope.getEffectiveFrom(), envelope.getSnapshot().getEffectiveFrom());
    }

    @Test
    void shouldParseSceneReleaseRecordAndInferRollbackPublishType() {
        SceneSnapshot snapshot = DemoFixtures.demoSnapshot();
        SceneReleaseRecord record = new SceneReleaseRecord();
        record.setSceneCode(snapshot.getSceneCode());
        record.setVersionNo(snapshot.getVersion());
        record.setChecksum(snapshot.getChecksum());
        record.setPublishedAt(snapshot.getPublishedAt());
        record.setEffectiveFrom(snapshot.getEffectiveFrom());
        record.setRollbackFromVersion(11);
        record.setSnapshotJson(EngineJson.read(EngineJson.write(snapshot), Object.class));

        SceneSnapshotEnvelope envelope = SceneSnapshotEnvelopes.parse(EngineJson.write(record));

        assertEquals(snapshot.getSceneCode(), envelope.getSceneCode());
        assertEquals(snapshot.getVersion(), envelope.getVersion());
        assertEquals(snapshot.getChecksum(), envelope.getChecksum());
        assertEquals(PublishType.ROLLBACK, envelope.getPublishType());
        assertEquals(snapshot.getEffectiveFrom(), envelope.getEffectiveFrom());
    }

    @Test
    void shouldRejectChecksumConflictBetweenReleaseRowAndSnapshotPayload() {
        SceneSnapshot snapshot = DemoFixtures.demoSnapshot();
        SceneReleaseRecord record = new SceneReleaseRecord();
        record.setSceneCode(snapshot.getSceneCode());
        record.setVersionNo(snapshot.getVersion());
        record.setChecksum("checksum-conflict");
        record.setSnapshotJson(snapshot);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> SceneSnapshotEnvelopes.fromReleaseRecord(record));

        assertEquals("snapshot checksum conflict", exception.getMessage());
    }

    @Test
    void shouldRejectEnvelopeAndSnapshotEffectiveFromConflict() {
        SceneSnapshot snapshot = DemoFixtures.demoSnapshot();
        SceneSnapshotEnvelope envelope = new SceneSnapshotEnvelope();
        envelope.setSnapshot(snapshot);
        envelope.setEffectiveFrom(snapshot.getEffectiveFrom().plusSeconds(30));

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> SceneSnapshotEnvelopes.fromEnvelope(envelope));

        assertEquals("snapshot effectiveFrom conflict", exception.getMessage());
    }

}
