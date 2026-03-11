package cn.liboshuai.pulsix.engine.flink.snapshot;

import java.nio.file.Path;

public record SceneSnapshotSourceOptions(SceneSnapshotSourceType type,
                                         Path filePath,
                                         long pollIntervalMs,
                                         String jdbcUrl,
                                         String jdbcUsername,
                                         String jdbcPassword,
                                         String snapshotSceneCode,
                                         Integer snapshotVersion,
                                         String jdbcQuery,
                                         String cdcHostname,
                                         Integer cdcPort,
                                         String cdcDatabase,
                                         String cdcTable,
                                         String cdcUsername,
                                         String cdcPassword,
                                         String cdcServerId,
                                         String cdcServerTimeZone) {

    public SceneSnapshotSourceOptions {
        type = type == null ? SceneSnapshotSourceType.DEMO : type;
        pollIntervalMs = pollIntervalMs <= 0 ? 1_000L : pollIntervalMs;
        cdcPort = cdcPort == null || cdcPort <= 0 ? 3306 : cdcPort;
        cdcTable = normalizeBlank(cdcTable) == null ? "scene_release" : cdcTable.trim();
        cdcServerTimeZone = normalizeBlank(cdcServerTimeZone) == null ? "UTC" : cdcServerTimeZone.trim();
        if (type == SceneSnapshotSourceType.FILE && filePath == null) {
            throw new IllegalArgumentException("snapshot filePath is required for FILE source");
        }
        if (type == SceneSnapshotSourceType.JDBC && normalizeBlank(jdbcUrl) == null) {
            throw new IllegalArgumentException("snapshot jdbcUrl is required for JDBC source");
        }
        if (type == SceneSnapshotSourceType.CDC) {
            if (normalizeBlank(cdcHostname) == null) {
                throw new IllegalArgumentException("snapshot cdcHostname is required for CDC source");
            }
            if (normalizeBlank(cdcDatabase) == null) {
                throw new IllegalArgumentException("snapshot cdcDatabase is required for CDC source");
            }
            if (normalizeBlank(cdcUsername) == null) {
                throw new IllegalArgumentException("snapshot cdcUsername is required for CDC source");
            }
        }
    }

    public static SceneSnapshotSourceOptions demo() {
        return new SceneSnapshotSourceOptions(SceneSnapshotSourceType.DEMO, null, 1_000L,
                null, null, null, null, null, null,
                null, 3306, null, "scene_release", null, null, null, "UTC");
    }

    public String cdcQualifiedTable() {
        return cdcDatabase + "." + cdcTable;
    }

    public String cdcBootstrapJdbcUrl() {
        if (normalizeBlank(jdbcUrl) != null) {
            return jdbcUrl.trim();
        }
        return "jdbc:mysql://" + cdcHostname + ":" + cdcPort + "/" + cdcDatabase
                + "?useSSL=false&characterEncoding=UTF-8&serverTimezone=" + cdcServerTimeZone;
    }

    public SceneSnapshotSourceOptions asJdbcBootstrapOptions() {
        return new SceneSnapshotSourceOptions(SceneSnapshotSourceType.JDBC,
                null,
                pollIntervalMs,
                cdcBootstrapJdbcUrl(),
                cdcUsername,
                cdcPassword,
                snapshotSceneCode,
                snapshotVersion,
                null,
                cdcHostname,
                cdcPort,
                cdcDatabase,
                cdcTable,
                cdcUsername,
                cdcPassword,
                cdcServerId,
                cdcServerTimeZone);
    }

    private static String normalizeBlank(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

}
