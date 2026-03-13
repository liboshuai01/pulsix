package cn.liboshuai.pulsix.module.risk.controller.admin.ingesterror.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - 接入异常 Response VO")
@Data
public class IngestErrorRespVO {

    @Schema(description = "编号", example = "8101")
    private Long id;

    @Schema(description = "链路号", example = "TRACE-S18-8101")
    private String traceId;

    @Schema(description = "接入源编码", example = "trade_http_demo")
    private String sourceCode;

    @Schema(description = "所属场景编码", example = "TRADE_RISK")
    private String sceneCode;

    @Schema(description = "事件编码", example = "TRADE_EVENT")
    private String eventCode;

    @Schema(description = "原始事件编号", example = "raw_trade_bad_8101")
    private String rawEventId;

    @Schema(description = "异常阶段", example = "VALIDATE")
    private String ingestStage;

    @Schema(description = "错误码", example = "REQUIRED_FIELD_MISSING")
    private String errorCode;

    @Schema(description = "错误说明")
    private String errorMessage;

    @Schema(description = "异常 Topic", example = "pulsix.event.dlq")
    private String errorTopicName;

    @Schema(description = "重处理状态", example = "PENDING")
    private String reprocessStatus;

    @Schema(description = "记录状态", example = "1")
    private Integer status;

    @Schema(description = "异常发生时间")
    private LocalDateTime occurTime;

}
