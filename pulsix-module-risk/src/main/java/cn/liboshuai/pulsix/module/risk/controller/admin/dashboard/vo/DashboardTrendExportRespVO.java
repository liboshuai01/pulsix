package cn.liboshuai.pulsix.module.risk.controller.admin.dashboard.vo;

import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Schema(description = "管理后台 - 监控大盘趋势导出 Response VO")
@Data
@ExcelIgnoreUnannotated
public class DashboardTrendExportRespVO {

    @Schema(description = "所属场景编码", example = "TRADE_RISK")
    @ExcelProperty("所属场景")
    private String sceneCode;

    @Schema(description = "统计粒度", example = "1m")
    @ExcelProperty("统计粒度")
    private String statGranularity;

    @Schema(description = "统计时间点")
    @ExcelProperty("统计时间")
    private LocalDateTime statTime;

    @Schema(description = "事件总量", example = "160")
    @ExcelProperty("事件量")
    private BigDecimal eventInTotal;

    @Schema(description = "决策总量", example = "156")
    @ExcelProperty("决策量")
    private BigDecimal decisionTotal;

    @Schema(description = "通过率", example = "0.68")
    @ExcelProperty("通过率")
    private BigDecimal passRate;

    @Schema(description = "复核率", example = "0.205")
    @ExcelProperty("复核率")
    private BigDecimal reviewRate;

    @Schema(description = "拒绝率", example = "0.115")
    @ExcelProperty("拒绝率")
    private BigDecimal rejectRate;

    @Schema(description = "P95 延迟", example = "26")
    @ExcelProperty("P95 延迟(ms)")
    private BigDecimal p95LatencyMs;

}
