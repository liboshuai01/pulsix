package cn.liboshuai.pulsix.module.risk.controller.admin.dashboard.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "管理后台 - 监控大盘汇总 Response VO")
@Data
public class DashboardSummaryRespVO {

    @Schema(description = "所属场景编码", example = "TRADE_RISK")
    private String sceneCode;

    @Schema(description = "统计粒度", example = "1m")
    private String statGranularity;

    @Schema(description = "最新统计时间")
    private LocalDateTime latestStatTime;

    @Schema(description = "最新事件总量", example = "160")
    private BigDecimal latestEventInTotal;

    @Schema(description = "最新决策总量", example = "156")
    private BigDecimal latestDecisionTotal;

    @Schema(description = "最新通过率", example = "0.68")
    private BigDecimal latestPassRate;

    @Schema(description = "最新复核率", example = "0.205")
    private BigDecimal latestReviewRate;

    @Schema(description = "最新拒绝率", example = "0.115")
    private BigDecimal latestRejectRate;

    @Schema(description = "最新 P95 延迟", example = "26")
    private BigDecimal latestP95LatencyMs;

    @Schema(description = "趋势点列表")
    private List<DashboardTrendPointRespVO> trends;

}
