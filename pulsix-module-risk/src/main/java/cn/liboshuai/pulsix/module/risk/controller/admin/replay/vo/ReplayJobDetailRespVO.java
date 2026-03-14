package cn.liboshuai.pulsix.module.risk.controller.admin.replay.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;
import java.util.Map;

@Schema(description = "管理后台 - 回放任务详情 Response VO")
@Data
@EqualsAndHashCode(callSuper = true)
public class ReplayJobDetailRespVO extends ReplayJobRespVO {

    @Schema(description = "回放汇总 JSON")
    private Map<String, Object> summaryJson;

    @Schema(description = "样例差异 JSON")
    private List<Map<String, Object>> sampleDiffJson;

    @Schema(description = "kernel diff 报告中的 eventCount")
    private Integer eventCount;

    @Schema(description = "kernel diff 报告中的 changedEventCount")
    private Integer changedEventCount;

    @Schema(description = "差异占比")
    private Double changeRate;

    @Schema(description = "kernel diff 报告中的 baseline 快照引用")
    private Map<String, Object> baseline;

    @Schema(description = "kernel diff 报告中的 candidate 快照引用")
    private Map<String, Object> candidate;

    @Schema(description = "kernel diff 报告中的 baselineSummary")
    private Map<String, Object> baselineSummary;

    @Schema(description = "kernel diff 报告中的 candidateSummary")
    private Map<String, Object> candidateSummary;

    @Schema(description = "kernel diff 报告中的 differences；历史记录默认投影 sampleDiffJson 样例")
    private List<Map<String, Object>> differences;

    @Schema(description = "差异类型统计")
    private Map<String, Integer> topChangeTypes;

    @Schema(description = "kernel golden case 视图；当前待执行链路接入")
    private Map<String, Object> goldenCase;

    @Schema(description = "kernel golden verification 视图；当前待执行链路接入")
    private Map<String, Object> goldenVerification;

}
