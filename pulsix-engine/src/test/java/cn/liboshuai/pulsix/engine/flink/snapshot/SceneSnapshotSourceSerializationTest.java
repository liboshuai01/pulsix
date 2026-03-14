package cn.liboshuai.pulsix.engine.flink.snapshot;

import org.apache.flink.util.InstantiationUtil;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class SceneSnapshotSourceSerializationTest {

    @Test
    void shouldSerializeJdbcPollingSceneSnapshotSource() {
        SceneSnapshotSourceOptions options = new SceneSnapshotSourceOptions(SceneSnapshotSourceType.JDBC,
                null,
                1_000L,
                "jdbc:mysql://127.0.0.1:3306/pulsix",
                "pulsix",
                "pulsix_123",
                "TRADE_RISK",
                14,
                null,
                null,
                3306,
                null,
                "scene_release",
                null,
                null,
                null,
                "UTC");

        JdbcPollingSceneSnapshotSource source = new JdbcPollingSceneSnapshotSource(options);

        assertDoesNotThrow(() -> InstantiationUtil.serializeObject(source));
    }
}
