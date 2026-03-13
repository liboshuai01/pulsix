package cn.liboshuai.pulsix.module.risk.controller.admin.ingesterror.vo;

import cn.liboshuai.pulsix.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

import static cn.liboshuai.pulsix.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND;

@Schema(description = "管理后台 - 接入异常分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
public class IngestErrorPageReqVO extends PageParam {

    @Schema(description = "所属场景编码", example = "TRADE_RISK")
    private String sceneCode;

    @Schema(description = "接入源编码", example = "trade_http_demo")
    private String sourceCode;

    @Schema(description = "链路号", example = "TRACE-S18-8101")
    private String traceId;

    @Schema(description = "原始事件编号", example = "raw_trade_bad_8101")
    private String rawEventId;

    @Schema(description = "异常阶段", example = "VALIDATE")
    private String ingestStage;

    @Schema(description = "错误码", example = "REQUIRED_FIELD_MISSING")
    private String errorCode;

    @Schema(description = "重处理状态", example = "PENDING")
    private String reprocessStatus;

    @Schema(description = "异常发生时间", example = "[2026-03-12 00:00:00, 2026-03-12 23:59:59]")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime[] occurTime;

}
