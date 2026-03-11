package cn.liboshuai.pulsix.engine.flink.snapshot;

import cn.liboshuai.pulsix.engine.demo.DemoFixtures;
import cn.liboshuai.pulsix.engine.json.EngineJson;
import cn.liboshuai.pulsix.engine.model.SceneSnapshot;
import cn.liboshuai.pulsix.engine.model.SceneSnapshotEnvelope;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SceneReleaseCdcPayloadParserTest {

    @Test
    void shouldParseCdcAfterPayloadIntoSnapshotEnvelope() {
        SceneSnapshot snapshot = DemoFixtures.demoSnapshot();
        String payload = """
                {
                  "payload": {
                    "op": "u",
                    "after": {
                      "scene_code": "TRADE_RISK",
                      "version_no": 12,
                      "snapshot_json": %s,
                      "checksum": "8d2041a7cf8f47b4b6b0f91d2ab8d9d0",
                      "publish_status": "ACTIVE",
                      "published_at": "2026-03-07T20:00:00Z",
                      "effective_from": "2026-03-07T20:00:10Z",
                      "rollback_from_version": null
                    }
                  }
                }
                """.formatted(EngineJson.write(snapshot));
        SceneSnapshotSourceOptions options = new SceneSnapshotSourceOptions(SceneSnapshotSourceType.CDC,
                null, 1_000L, null, null, null, "TRADE_RISK", null, null,
                "127.0.0.1", 3306, "pulsix", "scene_release", "root", "pwd", null, "UTC");

        Optional<SceneSnapshotEnvelope> envelope = SceneReleaseCdcPayloadParser.parse(payload, options);

        assertTrue(envelope.isPresent());
        assertEquals("TRADE_RISK", envelope.orElseThrow().getSceneCode());
        assertEquals(12, envelope.orElseThrow().getVersion());
        assertEquals("8d2041a7cf8f47b4b6b0f91d2ab8d9d0", envelope.orElseThrow().getChecksum());
    }

    @Test
    void shouldIgnoreDeletePayloadAndUnsupportedPublishStatus() {
        String deletePayload = """
                {
                  "payload": {
                    "op": "d",
                    "before": {
                      "scene_code": "TRADE_RISK"
                    }
                  }
                }
                """;
        String draftPayload = """
                {
                  "payload": {
                    "op": "c",
                    "after": {
                      "scene_code": "TRADE_RISK",
                      "version_no": 12,
                      "snapshot_json": %s,
                      "checksum": "8d2041a7cf8f47b4b6b0f91d2ab8d9d0",
                      "publish_status": "DRAFT"
                    }
                  }
                }
                """.formatted(EngineJson.write(DemoFixtures.demoSnapshot()));
        SceneSnapshotSourceOptions options = new SceneSnapshotSourceOptions(SceneSnapshotSourceType.CDC,
                null, 1_000L, null, null, null, null, null, null,
                "127.0.0.1", 3306, "pulsix", "scene_release", "root", "pwd", null, "UTC");

        assertTrue(SceneReleaseCdcPayloadParser.parse(deletePayload, options).isEmpty());
        assertTrue(SceneReleaseCdcPayloadParser.parse(draftPayload, options).isEmpty());
    }

}
