package cn.liboshuai.pulsix.engine.snapshot;

import cn.liboshuai.pulsix.engine.json.EngineJson;
import cn.liboshuai.pulsix.engine.model.PublishType;
import cn.liboshuai.pulsix.engine.model.SceneSnapshot;
import cn.liboshuai.pulsix.engine.model.SceneSnapshotEnvelope;
import com.fasterxml.jackson.databind.JsonNode;

import java.time.Instant;
import java.util.Objects;

public final class SceneSnapshotEnvelopes {

    private SceneSnapshotEnvelopes() {
    }

    public static SceneSnapshotEnvelope parse(String payload) {
        String text = requireText(payload, "snapshot payload");
        JsonNode root = EngineJson.read(text, JsonNode.class);
        if (root == null || !root.isObject()) {
            throw new IllegalArgumentException("snapshot payload must be a JSON object");
        }
        if (looksLikeSceneRelease(root)) {
            return fromReleaseRecord(EngineJson.read(text, SceneReleaseRecord.class));
        }
        if (looksLikeEnvelope(root)) {
            return fromEnvelope(EngineJson.read(text, SceneSnapshotEnvelope.class));
        }
        return fromSnapshot(EngineJson.read(text, SceneSnapshot.class));
    }

    public static SceneSnapshotEnvelope fromSnapshot(SceneSnapshot snapshot) {
        if (snapshot == null) {
            throw new IllegalArgumentException("snapshot must not be null");
        }
        SceneSnapshotEnvelope envelope = new SceneSnapshotEnvelope();
        envelope.setSnapshot(snapshot);
        envelope.setPublishType(PublishType.PUBLISH);
        return fromEnvelope(envelope);
    }

    public static SceneSnapshotEnvelope fromEnvelope(SceneSnapshotEnvelope envelope) {
        if (envelope == null) {
            throw new IllegalArgumentException("snapshot envelope must not be null");
        }
        if (envelope.getSnapshot() == null) {
            throw new IllegalArgumentException("snapshot payload must not be null");
        }
        SceneSnapshot snapshot = Objects.requireNonNull(envelope.getSnapshot(), "snapshot");
        String sceneCode = requireNonBlank(mergeString("sceneCode", envelope.getSceneCode(), snapshot.getSceneCode()),
                "snapshot sceneCode must not be blank");
        Integer version = requirePositive(mergeInteger("version", envelope.getVersion(), snapshot.getVersion()),
                "snapshot version must be positive");
        String checksum = requireNonBlank(mergeString("checksum", envelope.getChecksum(), snapshot.getChecksum()),
                "snapshot checksum must not be blank");
        Instant publishedAt = mergeInstant("publishedAt", envelope.getPublishedAt(), snapshot.getPublishedAt());
        Instant effectiveFrom = mergeInstant("effectiveFrom", envelope.getEffectiveFrom(), snapshot.getEffectiveFrom());
        PublishType publishType = envelope.getPublishType() == null ? PublishType.PUBLISH : envelope.getPublishType();

        snapshot.setSceneCode(sceneCode);
        snapshot.setVersion(version);
        snapshot.setChecksum(checksum);
        snapshot.setPublishedAt(publishedAt);
        snapshot.setEffectiveFrom(effectiveFrom);

        SceneSnapshotEnvelope normalized = new SceneSnapshotEnvelope();
        normalized.setSceneCode(sceneCode);
        normalized.setVersion(version);
        normalized.setChecksum(checksum);
        normalized.setPublishType(publishType);
        normalized.setPublishedAt(publishedAt);
        normalized.setEffectiveFrom(effectiveFrom);
        normalized.setSnapshot(snapshot);
        return normalized;
    }

    public static SceneSnapshotEnvelope fromReleaseRecord(SceneReleaseRecord record) {
        if (record == null) {
            throw new IllegalArgumentException("scene release record must not be null");
        }
        SceneSnapshot snapshot = parseSnapshot(record.getSnapshotJson());
        String sceneCode = requireNonBlank(mergeString("sceneCode", record.getSceneCode(), snapshot.getSceneCode()),
                "snapshot sceneCode must not be blank");
        Integer version = requirePositive(mergeInteger("version", record.getVersionNo(), snapshot.getVersion()),
                "snapshot version must be positive");
        String checksum = requireNonBlank(mergeString("checksum", record.getChecksum(), snapshot.getChecksum()),
                "snapshot checksum must not be blank");
        Instant publishedAt = preferInstant(record.getPublishedAt(), snapshot.getPublishedAt());
        Instant effectiveFrom = preferInstant(record.getEffectiveFrom(), snapshot.getEffectiveFrom());

        snapshot.setSceneCode(sceneCode);
        snapshot.setVersion(version);
        snapshot.setChecksum(checksum);
        snapshot.setPublishedAt(publishedAt);
        snapshot.setEffectiveFrom(effectiveFrom);

        SceneSnapshotEnvelope envelope = new SceneSnapshotEnvelope();
        envelope.setSceneCode(sceneCode);
        envelope.setVersion(version);
        envelope.setChecksum(checksum);
        envelope.setPublishType(resolvePublishType(record));
        envelope.setPublishedAt(publishedAt);
        envelope.setEffectiveFrom(effectiveFrom);
        envelope.setSnapshot(snapshot);
        return fromEnvelope(envelope);
    }

    private static SceneSnapshot parseSnapshot(Object snapshotPayload) {
        if (snapshotPayload == null) {
            throw new IllegalArgumentException("scene release snapshot_json must not be null");
        }
        if (snapshotPayload instanceof SceneSnapshot snapshot) {
            return snapshot;
        }
        if (snapshotPayload instanceof String text) {
            return EngineJson.read(requireText(text, "scene release snapshot_json"), SceneSnapshot.class);
        }
        return EngineJson.read(EngineJson.write(snapshotPayload), SceneSnapshot.class);
    }

    private static PublishType resolvePublishType(SceneReleaseRecord record) {
        if (record.getPublishType() != null) {
            if (record.getRollbackFromVersion() != null && record.getPublishType() != PublishType.ROLLBACK) {
                throw new IllegalArgumentException("scene release publishType conflicts with rollbackFromVersion");
            }
            return record.getPublishType();
        }
        return record.getRollbackFromVersion() == null ? PublishType.PUBLISH : PublishType.ROLLBACK;
    }

    private static boolean looksLikeEnvelope(JsonNode root) {
        return root.has("snapshot")
                || root.has("publishType")
                || root.has("publish_type");
    }

    private static boolean looksLikeSceneRelease(JsonNode root) {
        return root.has("snapshot_json")
                || root.has("snapshotJson")
                || root.has("version_no")
                || root.has("versionNo")
                || root.has("rollback_from_version")
                || root.has("rollbackFromVersion");
    }

    private static String mergeString(String fieldName, String preferred, String fallback) {
        String left = normalizeBlank(preferred);
        String right = normalizeBlank(fallback);
        if (left == null) {
            return right;
        }
        if (right == null) {
            return left;
        }
        if (!left.equals(right)) {
            throw new IllegalArgumentException("snapshot " + fieldName + " conflict");
        }
        return left;
    }

    private static Integer mergeInteger(String fieldName, Integer preferred, Integer fallback) {
        if (preferred == null) {
            return fallback;
        }
        if (fallback == null) {
            return preferred;
        }
        if (!preferred.equals(fallback)) {
            throw new IllegalArgumentException("snapshot " + fieldName + " conflict");
        }
        return preferred;
    }

    private static Instant mergeInstant(String fieldName, Instant preferred, Instant fallback) {
        if (preferred == null) {
            return fallback;
        }
        if (fallback == null) {
            return preferred;
        }
        if (!preferred.equals(fallback)) {
            throw new IllegalArgumentException("snapshot " + fieldName + " conflict");
        }
        return preferred;
    }

    private static Instant preferInstant(Instant preferred, Instant fallback) {
        return preferred == null ? fallback : preferred;
    }

    private static Integer requirePositive(Integer value, String message) {
        if (value == null || value <= 0) {
            throw new IllegalArgumentException(message);
        }
        return value;
    }

    private static String requireNonBlank(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
        return value;
    }

    private static String requireText(String value, String fieldName) {
        String normalized = normalizeBlank(value);
        if (normalized == null) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return normalized;
    }

    private static String normalizeBlank(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

}
