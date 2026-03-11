package cn.liboshuai.pulsix.engine.flink.runtime;

import cn.liboshuai.pulsix.engine.model.PublishType;
import cn.liboshuai.pulsix.engine.model.SceneSnapshot;
import cn.liboshuai.pulsix.engine.model.SceneSnapshotEnvelope;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SceneReleaseTimelineTest {

    @Test
    void shouldKeepEarlierEffectiveReleasesWhenManyFutureVersionsExist() {
        SceneReleaseTimeline timeline = new SceneReleaseTimeline();
        for (int version = 1; version <= 40; version++) {
            timeline.add(envelope(version));
        }

        assertEquals(40, timeline.getReleases().size());
        assertEquals(5, timeline.effectiveAt(Instant.parse("2026-03-05T00:00:01Z")).orElseThrow().getVersion());
    }

    private SceneSnapshotEnvelope envelope(int version) {
        Instant activationTime = Instant.parse("2026-03-01T00:00:00Z").plusSeconds((long) (version - 1) * 86_400L);
        SceneSnapshot snapshot = new SceneSnapshot();
        snapshot.setSceneCode("TRADE_RISK");
        snapshot.setVersion(version);
        snapshot.setChecksum("checksum-v" + version);
        snapshot.setPublishedAt(activationTime.minusSeconds(60));
        snapshot.setEffectiveFrom(activationTime);

        SceneSnapshotEnvelope envelope = new SceneSnapshotEnvelope();
        envelope.setSceneCode(snapshot.getSceneCode());
        envelope.setVersion(snapshot.getVersion());
        envelope.setChecksum(snapshot.getChecksum());
        envelope.setPublishType(PublishType.PUBLISH);
        envelope.setPublishedAt(snapshot.getPublishedAt());
        envelope.setEffectiveFrom(snapshot.getEffectiveFrom());
        envelope.setSnapshot(snapshot);
        return envelope;
    }

}
