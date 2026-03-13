package cn.liboshuai.pulsix.module.risk.controller.admin.dashboard.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Schema(description = "管理后台 - 监控大盘趋势点 Response VO")
@Data
public class DashboardTrendPointRespVO {

    @Schema(description = "统计时间点")
    private LocalDateTime statTime;

    @Schema(description = "事件总量", example = "160")
    private BigDecimal eventInTotal;

    @Schema(description = "决策总量", example = "156")
    private BigDecimal decisionTotal;

    @Schema(description = "通过率", example = "0.68")
    private BigDecimal passRate;

    @Schema(description = "复核率", example = "0.205")
    private BigDecimal reviewRate;

    @Schema(description = "拒绝率", example = "0.115")
    private BigDecimal rejectRate;

    @Schema(description = "P95 延迟", example = "26")
    private BigDecimal p95LatencyMs;

}
