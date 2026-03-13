package cn.liboshuai.pulsix.module.risk.controller.admin.ingesterror.vo;

import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - 接入异常导出 Response VO")
@Data
@ExcelIgnoreUnannotated
public class IngestErrorExportRespVO {

    @Schema(description = "接入源编码", example = "trade_http_demo")
    @ExcelProperty("接入源")
    private String sourceCode;

    @Schema(description = "链路号", example = "TRACE-S18-8101")
    @ExcelProperty("链路号")
    private String traceId;

    @Schema(description = "原始事件编号", example = "raw_trade_bad_8101")
    @ExcelProperty("原始事件编号")
    private String rawEventId;

    @Schema(description = "所属场景编码", example = "TRADE_RISK")
    @ExcelProperty("所属场景")
    private String sceneCode;

    @Schema(description = "事件编码", example = "TRADE_EVENT")
    @ExcelProperty("事件编码")
    private String eventCode;

    @Schema(description = "异常阶段", example = "VALIDATE")
    @ExcelProperty("异常阶段")
    private String ingestStage;

    @Schema(description = "错误码", example = "REQUIRED_FIELD_MISSING")
    @ExcelProperty("错误码")
    private String errorCode;

    @Schema(description = "错误说明")
    @ExcelProperty("错误说明")
    private String errorMessage;

    @Schema(description = "异常 Topic", example = "pulsix.event.dlq")
    @ExcelProperty("DLQ Topic")
    private String errorTopicName;

    @Schema(description = "重处理状态", example = "PENDING")
    @ExcelProperty("重处理状态")
    private String reprocessStatus;

    @Schema(description = "记录状态", example = "1")
    @ExcelProperty("记录状态")
    private Integer status;

    @Schema(description = "异常发生时间")
    @ExcelProperty("发生时间")
    private LocalDateTime occurTime;

}
