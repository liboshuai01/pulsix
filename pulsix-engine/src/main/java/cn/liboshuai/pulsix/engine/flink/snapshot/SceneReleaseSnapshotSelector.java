package cn.liboshuai.pulsix.engine.flink.snapshot;

import cn.liboshuai.pulsix.engine.flink.runtime.SceneReleaseTimeline;
import cn.liboshuai.pulsix.engine.model.SceneSnapshotEnvelope;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class SceneReleaseSnapshotSelector {

    private static final Comparator<SceneSnapshotEnvelope> RELEASE_ORDER = Comparator
            .comparing(SceneReleaseTimeline::activationTime)
            .thenComparing(SceneSnapshotEnvelope::getPublishedAt, Comparator.nullsLast(Comparator.naturalOrder()))
            .thenComparing(SceneSnapshotEnvelope::getVersion, Comparator.nullsLast(Comparator.naturalOrder()));

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
        Map<String, List<SceneSnapshotEnvelope>> byScene = new LinkedHashMap<>();
        for (SceneSnapshotEnvelope envelope : filtered) {
            byScene.computeIfAbsent(envelope.getSceneCode(), ignored -> new ArrayList<>()).add(envelope);
        }
        List<SceneSnapshotEnvelope> selected = new ArrayList<>();
        for (List<SceneSnapshotEnvelope> sceneEnvelopes : byScene.values()) {
            sceneEnvelopes.sort(RELEASE_ORDER);
            SceneSnapshotEnvelope latestEffective = null;
            List<SceneSnapshotEnvelope> futureEnvelopes = new ArrayList<>();
            for (SceneSnapshotEnvelope envelope : sceneEnvelopes) {
                if (isEffectiveNow(envelope, now)) {
                    latestEffective = envelope;
                    continue;
                }
                futureEnvelopes.add(envelope);
            }
            if (latestEffective != null) {
                selected.add(latestEffective);
            }
            selected.addAll(futureEnvelopes);
        }
        return selected;
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
