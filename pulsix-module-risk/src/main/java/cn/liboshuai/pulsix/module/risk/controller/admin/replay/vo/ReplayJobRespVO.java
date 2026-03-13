package cn.liboshuai.pulsix.module.risk.controller.admin.replay.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - 回放任务 Response VO")
@Data
public class ReplayJobRespVO {

    @Schema(description = "编号", example = "11001")
    private Long id;

    @Schema(description = "任务编码", example = "REPLAY_TRADE_RISK_14_15_001")
    private String jobCode;

    @Schema(description = "所属场景编码", example = "TRADE_RISK")
    private String sceneCode;

    @Schema(description = "基线版本号", example = "14")
    private Integer baselineVersionNo;

    @Schema(description = "目标版本号", example = "15")
    private Integer targetVersionNo;

    @Schema(description = "输入源类型", example = "DECISION_LOG_EXPORT")
    private String inputSourceType;

    @Schema(description = "输入源引用", example = "7101,7102")
    private String inputRef;

    @Schema(description = "任务状态", example = "SUCCESS")
    private String jobStatus;

    @Schema(description = "参与回放事件数", example = "2")
    private Integer eventTotalCount;

    @Schema(description = "差异事件数", example = "2")
    private Integer diffEventCount;

    @Schema(description = "任务备注")
    private String remark;

    @Schema(description = "开始时间")
    private LocalDateTime startedAt;

    @Schema(description = "结束时间")
    private LocalDateTime finishedAt;

    @Schema(description = "创建者")
    private String creator;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新者")
    private String updater;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;

}
