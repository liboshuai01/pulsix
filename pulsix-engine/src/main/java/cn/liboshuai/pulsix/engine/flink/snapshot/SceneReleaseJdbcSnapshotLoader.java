package cn.liboshuai.pulsix.engine.flink.snapshot;

import cn.liboshuai.pulsix.engine.model.PublishType;
import cn.liboshuai.pulsix.engine.model.SceneSnapshotEnvelope;
import cn.liboshuai.pulsix.engine.snapshot.SceneReleaseRecord;
import cn.liboshuai.pulsix.engine.snapshot.SceneSnapshotEnvelopes;

import java.nio.charset.StandardCharsets;
import java.sql.Clob;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLXML;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

final class SceneReleaseJdbcSnapshotLoader {

    private static final String DEFAULT_QUERY_PREFIX = """
            SELECT scene_code,
                   version_no,
                   snapshot_json,
                   checksum,
                   publish_status,
                   published_at,
                   effective_from,
                   rollback_from_version
              FROM scene_release
             WHERE deleted = 0
               AND publish_status IN ('PUBLISHED', 'ACTIVE', 'ROLLED_BACK')
            """;

    private final SceneSnapshotSourceOptions options;

    SceneReleaseJdbcSnapshotLoader(SceneSnapshotSourceOptions options) {
        this.options = options;
    }

    List<SceneSnapshotEnvelope> loadSnapshots() throws SQLException {
        List<SceneSnapshotEnvelope> snapshots = new ArrayList<>();
        try (Connection connection = DriverManager.getConnection(options.jdbcUrl(), options.jdbcUsername(), options.jdbcPassword())) {
            connection.setReadOnly(true);
            try (PreparedStatement statement = connection.prepareStatement(resolveQuery())) {
                bindDefaultQueryParameters(statement);
                statement.setFetchSize(100);
                try (ResultSet resultSet = statement.executeQuery()) {
                    while (resultSet.next()) {
                        snapshots.add(SceneSnapshotEnvelopes.fromReleaseRecord(mapRecord(resultSet)));
                    }
                }
            }
        }
        return SceneReleaseSnapshotSelector.selectBootstrapSnapshots(snapshots, options, Instant.now());
    }

    private String resolveQuery() {
        if (SceneReleaseSnapshotSelector.hasCustomJdbcQuery(options)) {
            return options.jdbcQuery();
        }
        StringBuilder sql = new StringBuilder(DEFAULT_QUERY_PREFIX);
        if (options.snapshotSceneCode() != null && !options.snapshotSceneCode().isBlank()) {
            sql.append(" AND scene_code = ?");
        }
        if (options.snapshotVersion() != null) {
            sql.append(" AND version_no = ?");
        }
        sql.append(" ORDER BY scene_code ASC, version_no ASC");
        return sql.toString();
    }

    private void bindDefaultQueryParameters(PreparedStatement statement) throws SQLException {
        if (SceneReleaseSnapshotSelector.hasCustomJdbcQuery(options)) {
            return;
        }
        int index = 1;
        if (options.snapshotSceneCode() != null && !options.snapshotSceneCode().isBlank()) {
            statement.setString(index++, options.snapshotSceneCode());
        }
        if (options.snapshotVersion() != null) {
            statement.setInt(index, options.snapshotVersion());
        }
    }

    private SceneReleaseRecord mapRecord(ResultSet resultSet) throws SQLException {
        SceneReleaseRecord record = new SceneReleaseRecord();
        record.setSceneCode(resultSet.getString("scene_code"));
        int versionNo = resultSet.getInt("version_no");
        record.setVersionNo(resultSet.wasNull() ? null : versionNo);
        record.setSnapshotJson(readSnapshotPayload(resultSet));
        record.setChecksum(resultSet.getString("checksum"));
        record.setPublishStatus(resultSet.getString("publish_status"));
        record.setPublishedAt(readInstant(resultSet, "published_at"));
        record.setEffectiveFrom(readInstant(resultSet, "effective_from"));
        int rollbackFromVersion = resultSet.getInt("rollback_from_version");
        record.setRollbackFromVersion(resultSet.wasNull() ? null : rollbackFromVersion);
        return record;
    }

    private Object readSnapshotPayload(ResultSet resultSet) throws SQLException {
        Object value = resultSet.getObject("snapshot_json");
        if (value == null) {
            return null;
        }
        if (value instanceof byte[] bytes) {
            return new String(bytes, StandardCharsets.UTF_8);
        }
        if (value instanceof SQLXML sqlxml) {
            return sqlxml.getString();
        }
        if (value instanceof Clob clob) {
            return clob.getSubString(1, (int) clob.length());
        }
        return value.toString();
    }

    private Instant readInstant(ResultSet resultSet, String columnLabel) throws SQLException {
        Timestamp timestamp = resultSet.getTimestamp(columnLabel);
        return timestamp == null ? null : timestamp.toInstant();
    }

    static Map<SceneVersionKey, SnapshotMarker> updateMarkers(List<SceneSnapshotEnvelope> envelopes,
                                                              Map<SceneVersionKey, SnapshotMarker> markers) {
        Map<SceneVersionKey, SnapshotMarker> updatedMarkers = markers == null ? new LinkedHashMap<>() : new LinkedHashMap<>(markers);
        for (SceneSnapshotEnvelope envelope : envelopes) {
            updatedMarkers.put(new SceneVersionKey(envelope.getSceneCode(), envelope.getVersion()), SnapshotMarker.from(envelope));
        }
        return updatedMarkers;
    }

    static boolean shouldEmit(SnapshotMarker previous, SceneSnapshotEnvelope current) {
        if (previous == null) {
            return true;
        }
        return !previous.sameRelease(current);
    }

    record SceneVersionKey(String sceneCode, Integer version) {
    }

    record SnapshotMarker(String checksum,
                          PublishType publishType,
                          Instant publishedAt,
                          Instant effectiveFrom) {

        static SnapshotMarker from(SceneSnapshotEnvelope envelope) {
            return new SnapshotMarker(envelope.getChecksum(), envelope.getPublishType(), envelope.getPublishedAt(), envelope.getEffectiveFrom());
        }

        boolean sameRelease(SceneSnapshotEnvelope envelope) {
            return Objects.equals(checksum, envelope.getChecksum())
                    && Objects.equals(publishType, envelope.getPublishType())
                    && Objects.equals(publishedAt, envelope.getPublishedAt())
                    && Objects.equals(effectiveFrom, envelope.getEffectiveFrom());
        }

    }

}
