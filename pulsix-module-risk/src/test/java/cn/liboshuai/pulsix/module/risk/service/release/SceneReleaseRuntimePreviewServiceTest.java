package cn.liboshuai.pulsix.module.risk.service.release;

import cn.liboshuai.pulsix.engine.demo.DemoFixtures;
import cn.liboshuai.pulsix.engine.model.EngineErrorCodes;
import cn.liboshuai.pulsix.engine.model.SceneSnapshot;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class SceneReleaseRuntimePreviewServiceTest {

    private final SceneReleaseRuntimePreviewService previewService = new SceneReleaseRuntimePreviewService();

    @Test
    void shouldBuildRuntimePreviewFromCompiledSnapshot() {
        SceneReleaseRuntimePreviewResult result = previewService.preview(DemoFixtures.demoSnapshot());

        assertThat(result.isValid()).isTrue();
        assertThat(result.getOrderedDerivedFeatureCodes()).containsExactlyInAnyOrder("high_amt_flag", "trade_burst_flag");
        assertThat(result.getOrderedRuleCodes()).startsWith("R001");
        assertThat(result.getOrderedRuleCodes()).contains("R002", "R003");
        assertThat(result.getRoutingGroupCount()).isEqualTo(2);
        assertThat(result.isMixedStreamFeatureRouting()).isTrue();
        assertThat(result.getFeatureCodes()).contains("user_trade_cnt_5m", "device_in_blacklist", "trade_burst_flag");

        Map<String, Object> dependencyPreview = result.toDependencyPreviewMap();
        assertThat(dependencyPreview).containsEntry("valid", true);
        assertThat((Map<String, Object>) dependencyPreview.get("runtimeHints"))
                .containsEntry("allowGroovy", true)
                .containsEntry("needFullDecisionLog", true);

        List<Map<String, Object>> streamFeatureGroups = result.getStreamFeatureGroups();
        assertThat(streamFeatureGroups).anySatisfy(group -> {
            assertThat(group).containsEntry("entityType", "USER");
            assertThat((List<String>) group.get("featureCodes")).containsExactly("user_trade_cnt_5m", "user_trade_amt_sum_30m");
        });
        assertThat(streamFeatureGroups).anySatisfy(group -> {
            assertThat(group).containsEntry("entityType", "DEVICE");
            assertThat((List<String>) group.get("featureCodes")).containsExactly("device_bind_user_cnt_1h");
        });
    }

    @Test
    void shouldExposeStructuredCompileFailureWhenRuleCompilationFails() {
        SceneSnapshot snapshot = DemoFixtures.demoSnapshot();
        snapshot.getRules().get(0).setWhenExpr("amount >");

        SceneReleaseRuntimePreviewResult result = previewService.preview(snapshot);

        assertThat(result.isValid()).isFalse();
        assertThat(result.getStage()).isEqualTo("snapshot-compile");
        assertThat(result.getErrorCode()).isEqualTo(EngineErrorCodes.SNAPSHOT_COMPILE_FAILED);
        assertThat(result.getRuleCode()).isEqualTo("R001");
        assertThat(result.getEngineType()).isEqualTo("AVIATOR");
        assertThat(result.summaryMessage()).contains("compile rule failed: R001");

        Map<String, Object> validationPreview = result.toValidationPreviewMap();
        assertThat(validationPreview).containsEntry("valid", false);
        assertThat(validationPreview).containsEntry("ruleCode", "R001");
        assertThat(validationPreview).containsEntry("errorCode", EngineErrorCodes.SNAPSHOT_COMPILE_FAILED);
    }

}
