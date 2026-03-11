package cn.liboshuai.pulsix.engine.flink.snapshot;

import cn.liboshuai.pulsix.engine.model.SceneSnapshotEnvelope;
import cn.liboshuai.pulsix.engine.snapshot.SceneReleaseRecord;
import cn.liboshuai.pulsix.engine.snapshot.SceneSnapshotEnvelopes;
import org.apache.flink.streaming.api.functions.source.SourceFunction;

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

public class JdbcPollingSceneSnapshotSource implements SourceFunction<SceneSnapshotEnvelope> {

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

    private volatile boolean running = true;

    public JdbcPollingSceneSnapshotSource(SceneSnapshotSourceOptions options) {
        this.options = options;
    }

    @Override
    public void run(SourceContext<SceneSnapshotEnvelope> context) throws Exception {
        Map<String, SnapshotMarker> markers = new LinkedHashMap<>();
        while (running) {
            for (SceneSnapshotEnvelope envelope : loadSnapshots()) {
                if (!running) {
                    return;
                }
                if (!shouldEmit(markers.get(envelope.getSceneCode()), envelope)) {
                    continue;
                }
                synchronized (context.getCheckpointLock()) {
                    context.collect(envelope);
                }
                markers.put(envelope.getSceneCode(), new SnapshotMarker(envelope.getVersion(), envelope.getChecksum()));
            }
            sleepQuietly();
        }
    }

    @Override
    public void cancel() {
        this.running = false;
    }

    private List<SceneSnapshotEnvelope> loadSnapshots() throws SQLException {
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
        return snapshots;
    }

    private String resolveQuery() {
        if (options.jdbcQuery() != null && !options.jdbcQuery().isBlank()) {
            return options.jdbcQuery();
        }
        StringBuilder sql = new StringBuilder(DEFAULT_QUERY_PREFIX);
        if (options.jdbcSceneCode() != null && !options.jdbcSceneCode().isBlank()) {
            sql.append(" AND scene_code = ?");
        }
        if (options.jdbcVersion() != null) {
            sql.append(" AND version_no = ?");
        }
        sql.append(" ORDER BY scene_code ASC, version_no ASC");
        return sql.toString();
    }

    private void bindDefaultQueryParameters(PreparedStatement statement) throws SQLException {
        if (options.jdbcQuery() != null && !options.jdbcQuery().isBlank()) {
            return;
        }
        int index = 1;
        if (options.jdbcSceneCode() != null && !options.jdbcSceneCode().isBlank()) {
            statement.setString(index++, options.jdbcSceneCode());
        }
        if (options.jdbcVersion() != null) {
            statement.setInt(index, options.jdbcVersion());
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

    private boolean shouldEmit(SnapshotMarker previous, SceneSnapshotEnvelope current) {
        if (previous == null) {
            return true;
        }
        if (current.getVersion() > previous.version()) {
            return true;
        }
        return current.getVersion().equals(previous.version())
                && !current.getChecksum().equals(previous.checksum());
    }

    private void sleepQuietly() {
        try {
            Thread.sleep(Math.max(100L, options.pollIntervalMs()));
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            running = false;
        }
    }

    private record SnapshotMarker(Integer version, String checksum) {
    }

}
