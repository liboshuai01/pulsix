package cn.liboshuai.pulsix.engine.flink.runtime;

import cn.liboshuai.pulsix.engine.model.PublishType;
import cn.liboshuai.pulsix.engine.model.SceneSnapshotEnvelope;

import java.io.Serializable;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class SceneReleaseTimeline implements Serializable {

    private static final Comparator<SceneSnapshotEnvelope> RELEASE_ORDER = Comparator
            .comparing(SceneReleaseTimeline::activationTime)
            .thenComparing(SceneReleaseTimeline::publishedAt)
            .thenComparing(SceneSnapshotEnvelope::getVersion, Comparator.nullsLast(Comparator.naturalOrder()))
            .thenComparing(envelope -> envelope.getPublishType() == PublishType.ROLLBACK ? 1 : 0)
            .thenComparing(SceneSnapshotEnvelope::getChecksum, Comparator.nullsLast(String::compareTo));

    private String sceneCode;

    private List<SceneSnapshotEnvelope> releases = new ArrayList<>();

    public String getSceneCode() {
        return sceneCode;
    }

    public void setSceneCode(String sceneCode) {
        this.sceneCode = sceneCode;
    }

    public List<SceneSnapshotEnvelope> getReleases() {
        return new ArrayList<>(releases);
    }

    public void setReleases(List<SceneSnapshotEnvelope> releases) {
        this.releases = releases == null ? new ArrayList<>() : new ArrayList<>(releases);
    }

    public boolean hasVersionConflict(SceneSnapshotEnvelope incomingEnvelope) {
        if (incomingEnvelope == null || incomingEnvelope.getVersion() == null) {
            return false;
        }
        return releases.stream()
                .filter(existingEnvelope -> Objects.equals(existingEnvelope.getVersion(), incomingEnvelope.getVersion()))
                .anyMatch(existingEnvelope -> !Objects.equals(existingEnvelope.getChecksum(), incomingEnvelope.getChecksum()));
    }

    public boolean contains(SceneSnapshotEnvelope incomingEnvelope) {
        return releases.stream().anyMatch(existingEnvelope -> sameRelease(existingEnvelope, incomingEnvelope));
    }

    public void add(SceneSnapshotEnvelope incomingEnvelope) {
        if (incomingEnvelope == null || contains(incomingEnvelope)) {
            return;
        }
        if (sceneCode == null) {
            sceneCode = incomingEnvelope.getSceneCode();
        }
        releases.add(incomingEnvelope);
        releases.sort(RELEASE_ORDER);
    }

    public Optional<SceneSnapshotEnvelope> effectiveAt(Instant referenceTime) {
        Instant resolvedReferenceTime = referenceTime == null ? Instant.now() : referenceTime;
        return releases.stream()
                .filter(envelope -> isEffectiveAt(envelope, resolvedReferenceTime))
                .max(RELEASE_ORDER);
    }

    public void trimTo(int maxReleases) {
        int safeMaxReleases = Math.max(1, maxReleases);
        if (releases.size() <= safeMaxReleases) {
            return;
        }
        releases = new ArrayList<>(releases.subList(releases.size() - safeMaxReleases, releases.size()));
    }

    public static Instant activationTime(SceneSnapshotEnvelope envelope) {
        if (envelope == null) {
            return Instant.EPOCH;
        }
        if (envelope.getEffectiveFrom() != null) {
            return envelope.getEffectiveFrom();
        }
        if (envelope.getPublishedAt() != null) {
            return envelope.getPublishedAt();
        }
        return Instant.EPOCH;
    }

    private static Instant publishedAt(SceneSnapshotEnvelope envelope) {
        return envelope == null || envelope.getPublishedAt() == null ? Instant.EPOCH : envelope.getPublishedAt();
    }

    private static boolean isEffectiveAt(SceneSnapshotEnvelope envelope, Instant referenceTime) {
        if (envelope == null || envelope.getEffectiveFrom() == null) {
            return true;
        }
        return !envelope.getEffectiveFrom().isAfter(referenceTime);
    }

    private static boolean sameRelease(SceneSnapshotEnvelope left, SceneSnapshotEnvelope right) {
        if (left == null || right == null) {
            return false;
        }
        return Objects.equals(left.getSceneCode(), right.getSceneCode())
                && Objects.equals(left.getVersion(), right.getVersion())
                && Objects.equals(left.getChecksum(), right.getChecksum())
                && Objects.equals(left.getPublishType(), right.getPublishType())
                && Objects.equals(left.getPublishedAt(), right.getPublishedAt())
                && Objects.equals(left.getEffectiveFrom(), right.getEffectiveFrom());
    }

}
