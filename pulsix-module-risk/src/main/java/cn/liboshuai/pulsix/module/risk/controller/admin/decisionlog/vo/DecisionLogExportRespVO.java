package cn.liboshuai.pulsix.module.risk.controller.admin.decisionlog.vo;

import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - 决策日志导出 Response VO")
@Data
@ExcelIgnoreUnannotated
public class DecisionLogExportRespVO {

    @Schema(description = "所属场景编码", example = "TRADE_RISK")
    @ExcelProperty("所属场景")
    private String sceneCode;

    @Schema(description = "链路号", example = "TRACE-S16-7101")
    @ExcelProperty("链路号")
    private String traceId;

    @Schema(description = "事件编号", example = "E_DECISION_7101")
    @ExcelProperty("事件编号")
    private String eventId;

    @Schema(description = "接入源编码", example = "trade_http_demo")
    @ExcelProperty("接入源")
    private String sourceCode;

    @Schema(description = "主体编号", example = "U9101")
    @ExcelProperty("主体编号")
    private String entityId;

    @Schema(description = "最终动作", example = "REJECT")
    @ExcelProperty("最终动作")
    private String finalAction;

    @Schema(description = "版本号", example = "14")
    @ExcelProperty("版本号")
    private Integer versionNo;

    @Schema(description = "命中规则编码摘要")
    @ExcelProperty("命中规则")
    private String hitRuleCodes;

    @Schema(description = "决策耗时", example = "12")
    @ExcelProperty("耗时(ms)")
    private Long latencyMs;

    @Schema(description = "事件时间")
    @ExcelProperty("事件时间")
    private LocalDateTime eventTime;

}
