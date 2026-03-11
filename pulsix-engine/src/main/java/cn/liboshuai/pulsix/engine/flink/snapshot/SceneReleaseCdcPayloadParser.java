package cn.liboshuai.pulsix.engine.flink.snapshot;

import cn.liboshuai.pulsix.engine.json.EngineJson;
import cn.liboshuai.pulsix.engine.model.SceneSnapshotEnvelope;
import cn.liboshuai.pulsix.engine.snapshot.SceneReleaseRecord;
import cn.liboshuai.pulsix.engine.snapshot.SceneSnapshotEnvelopes;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.Optional;
import java.util.Set;

final class SceneReleaseCdcPayloadParser {

    private static final Set<String> SUPPORTED_PUBLISH_STATUS = Set.of("PUBLISHED", "ACTIVE", "ROLLED_BACK");

    private SceneReleaseCdcPayloadParser() {
    }

    static Optional<SceneSnapshotEnvelope> parse(String cdcPayload,
                                                 SceneSnapshotSourceOptions options) {
        if (cdcPayload == null || cdcPayload.isBlank()) {
            return Optional.empty();
        }
        JsonNode root = EngineJson.read(cdcPayload, JsonNode.class);
        if (root == null || root.isNull()) {
            return Optional.empty();
        }
        JsonNode payload = root.has("payload") ? root.get("payload") : root;
        if (payload == null || payload.isNull()) {
            return Optional.empty();
        }
        String op = textValue(payload.get("op"));
        if ("d".equalsIgnoreCase(op) || "t".equalsIgnoreCase(op)) {
            return Optional.empty();
        }
        JsonNode row = payload.hasNonNull("after") ? payload.get("after") : payload.get("before");
        if (row == null || row.isNull() || row.isMissingNode()) {
            return Optional.empty();
        }
        String rowJson = EngineJson.write(row);
        SceneReleaseRecord releaseRecord = EngineJson.read(rowJson, SceneReleaseRecord.class);
        if (!SUPPORTED_PUBLISH_STATUS.contains(normalizeStatus(releaseRecord.getPublishStatus()))) {
            return Optional.empty();
        }
        SceneSnapshotEnvelope envelope = SceneSnapshotEnvelopes.fromReleaseRecord(releaseRecord);
        if (!SceneReleaseSnapshotSelector.matchesSelection(envelope, options)) {
            return Optional.empty();
        }
        return Optional.of(envelope);
    }

    private static String normalizeStatus(String value) {
        return value == null ? "" : value.trim().toUpperCase();
    }

    private static String textValue(JsonNode node) {
        return node == null || node.isNull() ? null : node.asText();
    }

}
