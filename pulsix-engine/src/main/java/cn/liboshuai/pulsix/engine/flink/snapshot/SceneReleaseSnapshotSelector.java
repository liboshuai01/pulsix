package cn.liboshuai.pulsix.engine.flink.snapshot;

import cn.liboshuai.pulsix.engine.model.SceneSnapshotEnvelope;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class SceneReleaseSnapshotSelector {

    private SceneReleaseSnapshotSelector() {
    }

    static List<SceneSnapshotEnvelope> selectBootstrapSnapshots(List<SceneSnapshotEnvelope> envelopes,
                                                               SceneSnapshotSourceOptions options,
                                                               Instant now) {
        List<SceneSnapshotEnvelope> filtered = new ArrayList<>();
        for (SceneSnapshotEnvelope envelope : envelopes) {
            if (matchesSelection(envelope, options)) {
                filtered.add(envelope);
            }
        }
        if (options.snapshotVersion() != null || hasCustomJdbcQuery(options)) {
            return filtered;
        }
        Map<String, SceneSnapshotEnvelope> latestByScene = new LinkedHashMap<>();
        for (SceneSnapshotEnvelope envelope : filtered) {
            if (!isEffectiveNow(envelope, now)) {
                continue;
            }
            latestByScene.put(envelope.getSceneCode(), envelope);
        }
        return new ArrayList<>(latestByScene.values());
    }

    static boolean matchesSelection(SceneSnapshotEnvelope envelope, SceneSnapshotSourceOptions options) {
        if (envelope == null || envelope.getSceneCode() == null) {
            return false;
        }
        if (options == null) {
            return true;
        }
        if (options.snapshotSceneCode() != null && !options.snapshotSceneCode().isBlank()
                && !options.snapshotSceneCode().equals(envelope.getSceneCode())) {
            return false;
        }
        return options.snapshotVersion() == null || options.snapshotVersion().equals(envelope.getVersion());
    }

    static boolean isEffectiveNow(SceneSnapshotEnvelope envelope, Instant now) {
        if (envelope == null || envelope.getEffectiveFrom() == null) {
            return true;
        }
        Instant effectiveFrom = envelope.getEffectiveFrom();
        Instant reference = now == null ? Instant.now() : now;
        return !effectiveFrom.isAfter(reference);
    }

    static boolean hasCustomJdbcQuery(SceneSnapshotSourceOptions options) {
        return options != null && options.jdbcQuery() != null && !options.jdbcQuery().isBlank();
    }

}
