package cn.liboshuai.pulsix.module.risk.controller.admin.dashboard.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

import static cn.liboshuai.pulsix.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND;

@Schema(description = "管理后台 - 监控大盘查询 Request VO")
@Data
public class DashboardQueryReqVO {

    @Schema(description = "所属场景编码", example = "TRADE_RISK")
    private String sceneCode;

    @Schema(description = "统计粒度", example = "1m")
    private String statGranularity;

    @Schema(description = "统计时间范围", example = "[2026-03-12 10:40:00, 2026-03-12 10:45:00]")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime[] statTime;

}
