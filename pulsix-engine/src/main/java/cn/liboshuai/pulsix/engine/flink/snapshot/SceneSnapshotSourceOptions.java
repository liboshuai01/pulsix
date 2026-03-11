package cn.liboshuai.pulsix.engine.flink.snapshot;

import java.nio.file.Path;

public record SceneSnapshotSourceOptions(SceneSnapshotSourceType type,
                                         Path filePath,
                                         long pollIntervalMs,
                                         String jdbcUrl,
                                         String jdbcUsername,
                                         String jdbcPassword,
                                         String jdbcSceneCode,
                                         Integer jdbcVersion,
                                         String jdbcQuery) {

    public SceneSnapshotSourceOptions {
        type = type == null ? SceneSnapshotSourceType.DEMO : type;
        pollIntervalMs = pollIntervalMs <= 0 ? 1_000L : pollIntervalMs;
        if (type == SceneSnapshotSourceType.FILE && filePath == null) {
            throw new IllegalArgumentException("snapshot filePath is required for FILE source");
        }
        if (type == SceneSnapshotSourceType.JDBC && (jdbcUrl == null || jdbcUrl.isBlank())) {
            throw new IllegalArgumentException("snapshot jdbcUrl is required for JDBC source");
        }
    }

    public static SceneSnapshotSourceOptions demo() {
        return new SceneSnapshotSourceOptions(SceneSnapshotSourceType.DEMO, null, 1_000L,
                null, null, null, null, null, null);
    }

}
