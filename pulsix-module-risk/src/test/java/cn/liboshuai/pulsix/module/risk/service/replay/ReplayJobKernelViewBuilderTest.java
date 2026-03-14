package cn.liboshuai.pulsix.module.risk.service.replay;

import cn.liboshuai.pulsix.module.risk.controller.admin.replay.vo.ReplayJobDetailRespVO;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ReplayJobKernelViewBuilderTest {

    private final ReplayJobKernelViewBuilder builder = new ReplayJobKernelViewBuilder();

    @Test
    void shouldProjectKernelAlignedFieldsFromLegacySummaryAndSampleDiff() {
        ReplayJobDetailRespVO respVO = new ReplayJobDetailRespVO();
        respVO.setSceneCode("TRADE_RISK");
        respVO.setEventTotalCount(2);
        respVO.setDiffEventCount(1);
        respVO.setSummaryJson(buildSummaryJson());
        respVO.setSampleDiffJson(List.of(buildSampleDiff()));

        builder.apply(respVO);

        assertThat(respVO.getEventCount()).isEqualTo(2);
        assertThat(respVO.getChangedEventCount()).isEqualTo(1);
        assertThat(respVO.getChangeRate()).isEqualTo(0.5D);
        assertThat(respVO.getBaseline()).containsEntry("snapshotId", "TRADE_RISK_v12")
                .containsEntry("version", 12)
                .containsEntry("checksum", "checksum_v12");
        assertThat(respVO.getCandidate()).containsEntry("snapshotId", "TRADE_RISK_v13")
                .containsEntry("version", 13)
                .containsEntry("checksum", "checksum_v13");
        assertThat(respVO.getBaselineSummary()).containsEntry("matchedEventCount", 1);
        assertThat(respVO.getCandidateSummary()).containsEntry("matchedEventCount", 1);
        assertThat(respVO.getTopChangeTypes()).containsEntry("FINAL_ACTION", 1).containsEntry("HIT_RULES", 1);
        assertThat(respVO.getDifferences()).hasSize(1);
        assertThat(respVO.getDifferences().get(0)).containsEntry("eventIndex", 0)
                .containsEntry("eventId", "E202603070099")
                .containsEntry("traceId", "T202603070099");
        Map<String, Object> baselineResult = castObject(respVO.getDifferences().get(0).get("baselineResult"));
        Map<String, Object> candidateResult = castObject(respVO.getDifferences().get(0).get("candidateResult"));
        assertThat(baselineResult).containsEntry("finalAction", "REJECT");
        assertThat(candidateResult).containsEntry("finalAction", "REVIEW");
        assertThat(castListOfObject(baselineResult.get("hitRules"))).extracting(item -> item.get("ruleCode"))
                .containsExactly("R001");
        assertThat(castListOfObject(candidateResult.get("hitRules"))).extracting(item -> item.get("ruleCode"))
                .containsExactly("R002");
        assertThat(respVO.getGoldenCase()).containsEntry("caseId", "GC_TRADE_RISK_001")
                .containsEntry("sceneCode", "TRADE_RISK")
                .containsEntry("version", 13);
        assertThat(respVO.getGoldenVerification()).containsEntry("caseId", "GC_TRADE_RISK_001")
                .containsEntry("matched", true);
    }

    private Map<String, Object> buildSummaryJson() {
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("sceneCode", "TRADE_RISK");
        summary.put("eventCount", 2);
        summary.put("changedEventCount", 1);
        summary.put("changeRate", 0.5D);
        summary.put("baseline", buildSnapshotSummary("TRADE_RISK_v12", 12, "checksum_v12", Map.of("REJECT", 1, "PASS", 1), 1));
        summary.put("candidate", buildSnapshotSummary("TRADE_RISK_v13", 13, "checksum_v13", Map.of("REVIEW", 1, "PASS", 1), 1));
        summary.put("topChangeTypes", Map.of("FINAL_ACTION", 1, "HIT_RULES", 1));
        summary.put("goldenCase", Map.of(
                "caseId", "GC_TRADE_RISK_001",
                "description", "trade risk candidate golden case",
                "sceneCode", "TRADE_RISK",
                "version", 13,
                "checksum", "checksum_v13",
                "eventCount", 2,
                "expectedSummary", Map.of("matchedEventCount", 1)
        ));
        summary.put("goldenVerification", Map.of(
                "caseId", "GC_TRADE_RISK_001",
                "matched", true,
                "sceneCode", "TRADE_RISK",
                "version", 13,
                "checksum", "checksum_v13",
                "eventCount", 2,
                "actualSummary", Map.of("matchedEventCount", 1)
        ));
        return summary;
    }

    private Map<String, Object> buildSnapshotSummary(String snapshotId, Integer version, String checksum,
                                                     Map<String, Integer> finalActionCounts, Integer matchedEventCount) {
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("snapshotId", snapshotId);
        summary.put("version", version);
        summary.put("checksum", checksum);
        summary.put("finalActionCounts", finalActionCounts);
        summary.put("matchedEventCount", matchedEventCount);
        return summary;
    }

    private Map<String, Object> buildSampleDiff() {
        Map<String, Object> sampleDiff = new LinkedHashMap<>();
        sampleDiff.put("eventIndex", 0);
        sampleDiff.put("eventId", "E202603070099");
        sampleDiff.put("traceId", "T202603070099");
        sampleDiff.put("changeTypes", List.of("FINAL_ACTION", "HIT_RULES"));
        sampleDiff.put("baselineAction", "REJECT");
        sampleDiff.put("candidateAction", "REVIEW");
        sampleDiff.put("baselineHitRules", List.of("R001"));
        sampleDiff.put("candidateHitRules", List.of("R002"));
        return sampleDiff;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> castObject(Object value) {
        return (Map<String, Object>) value;
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> castListOfObject(Object value) {
        return (List<Map<String, Object>>) value;
    }

}
