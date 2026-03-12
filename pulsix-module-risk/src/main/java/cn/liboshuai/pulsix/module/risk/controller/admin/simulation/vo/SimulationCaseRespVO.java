package cn.liboshuai.pulsix.module.risk.controller.admin.simulation.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Schema(description = "管理后台 - 仿真用例 Response VO")
@Data
public class SimulationCaseRespVO {

    private Long id;

    private String sceneCode;

    private String caseCode;

    private String caseName;

    private String versionSelectMode;

    private Integer versionNo;

    private Map<String, Object> inputEventJson;

    private Map<String, Object> mockFeatureJson;

    private Map<String, Object> mockLookupJson;

    private String expectedAction;

    private List<String> expectedHitRules;

    private Integer status;

    private String description;

    private String creator;

    private LocalDateTime createTime;

    private String updater;

    private LocalDateTime updateTime;

    @Schema(description = "最近一次报告编号")
    private Long latestReportId;

    @Schema(description = "最近一次执行版本号")
    private Integer latestReportVersionNo;

    @Schema(description = "最近一次执行最终动作")
    private String latestFinalAction;

    @Schema(description = "最近一次执行命中规则")
    private List<String> latestHitRules;

    @Schema(description = "最近一次执行是否符合预期：1-符合，0-不符合")
    private Integer latestPassFlag;

    @Schema(description = "最近一次执行耗时")
    private Long latestDurationMs;

    @Schema(description = "最近一次执行时间")
    private LocalDateTime latestReportTime;

}
