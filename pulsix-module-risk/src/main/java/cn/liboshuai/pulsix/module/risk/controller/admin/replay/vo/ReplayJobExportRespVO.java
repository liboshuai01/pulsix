package cn.liboshuai.pulsix.module.risk.controller.admin.replay.vo;

import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - 回放任务导出 Response VO")
@Data
@ExcelIgnoreUnannotated
public class ReplayJobExportRespVO {

    @Schema(description = "所属场景编码", example = "TRADE_RISK")
    @ExcelProperty("所属场景")
    private String sceneCode;

    @Schema(description = "任务编码", example = "REPLAY_TRADE_RISK_14_15_001")
    @ExcelProperty("任务编码")
    private String jobCode;

    @Schema(description = "基线版本号", example = "14")
    @ExcelProperty("基线版本")
    private Integer baselineVersionNo;

    @Schema(description = "目标版本号", example = "15")
    @ExcelProperty("目标版本")
    private Integer targetVersionNo;

    @Schema(description = "输入源类型", example = "FILE")
    @ExcelProperty("输入源类型")
    private String inputSourceType;

    @Schema(description = "输入源引用", example = "classpath:risk/replay/trade-risk-events.json")
    @ExcelProperty("输入引用")
    private String inputRef;

    @Schema(description = "任务状态", example = "SUCCESS")
    @ExcelProperty("任务状态")
    private String jobStatus;

    @Schema(description = "参与回放事件数", example = "2")
    @ExcelProperty("参与回放事件数")
    private Integer eventTotalCount;

    @Schema(description = "差异事件数", example = "2")
    @ExcelProperty("差异事件数")
    private Integer diffEventCount;

    @Schema(description = "开始时间")
    @ExcelProperty("开始时间")
    private LocalDateTime startedAt;

    @Schema(description = "结束时间")
    @ExcelProperty("结束时间")
    private LocalDateTime finishedAt;

    @Schema(description = "任务备注")
    @ExcelProperty("任务备注")
    private String remark;

}
