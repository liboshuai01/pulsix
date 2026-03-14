package cn.liboshuai.pulsix.module.risk.controller.admin.simulation.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Schema(description = "管理后台 - 仿真报告 Response VO")
@Data
public class SimulationReportRespVO {

    private Long id;

    private Long caseId;

    private String caseCode;

    private String caseName;

    private String sceneCode;

    private Integer versionNo;

    private String traceId;

    private Map<String, Object> resultJson;

    @Schema(description = "kernel 仿真报告中的 snapshotId")
    private String snapshotId;

    @Schema(description = "kernel 仿真报告中的 usedVersion")
    private Integer usedVersion;

    @Schema(description = "kernel 仿真报告中的 checksum")
    private String checksum;

    @Schema(description = "kernel 仿真报告中的 eventCount")
    private Integer eventCount;

    @Schema(description = "kernel 仿真报告中的 overridesApplied")
    private Boolean overridesApplied;

    @Schema(description = "kernel 仿真报告中的 finalResult")
    private Map<String, Object> finalResult;

    @Schema(description = "kernel 仿真报告中的 results")
    private List<Map<String, Object>> results;

    @Schema(description = "最终动作")
    private String finalAction;

    @Schema(description = "最终分数")
    private Integer finalScore;

    @Schema(description = "累计分数")
    private Integer totalScore;

    @Schema(description = "最终原因")
    private String reason;

    @Schema(description = "命中规则列表")
    private List<Map<String, Object>> hitRules;

    @Schema(description = "命中原因列表")
    private List<String> hitReasons;

    @Schema(description = "特征快照")
    private Map<String, Object> featureSnapshot;

    @Schema(description = "trace 列表")
    private List<String> trace;

    private Integer passFlag;

    private Long durationMs;

    private String creator;

    private LocalDateTime createTime;

    private String updater;

    private LocalDateTime updateTime;

}
