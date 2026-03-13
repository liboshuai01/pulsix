package cn.liboshuai.pulsix.module.risk.controller.admin.dashboard;

import cn.liboshuai.pulsix.framework.apilog.core.annotation.ApiAccessLog;
import cn.liboshuai.pulsix.framework.common.pojo.CommonResult;
import cn.liboshuai.pulsix.framework.excel.core.util.ExcelUtils;
import cn.liboshuai.pulsix.module.risk.controller.admin.dashboard.vo.DashboardQueryReqVO;
import cn.liboshuai.pulsix.module.risk.controller.admin.dashboard.vo.DashboardSummaryRespVO;
import cn.liboshuai.pulsix.module.risk.controller.admin.dashboard.vo.DashboardTrendExportRespVO;
import cn.liboshuai.pulsix.module.risk.controller.admin.dashboard.vo.DashboardTrendPointRespVO;
import cn.liboshuai.pulsix.module.risk.service.dashboard.DashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.List;

import static cn.liboshuai.pulsix.framework.apilog.core.enums.OperateTypeEnum.EXPORT;
import static cn.liboshuai.pulsix.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - 监控大盘")
@RestController
@RequestMapping("/risk/dashboard")
@Validated
public class DashboardController {

    @Resource
    private DashboardService dashboardService;

    @GetMapping("/summary")
    @Operation(summary = "获得监控大盘汇总")
    @PreAuthorize("@ss.hasPermission('risk:dashboard:query')")
    public CommonResult<DashboardSummaryRespVO> getDashboardSummary(@Valid DashboardQueryReqVO reqVO) {
        return success(dashboardService.getDashboardSummary(reqVO));
    }

    @GetMapping("/export-excel")
    @Operation(summary = "导出监控大盘趋势 Excel")
    @PreAuthorize("@ss.hasPermission('risk:dashboard:export')")
    @ApiAccessLog(operateType = EXPORT)
    public void exportDashboardExcel(@Valid DashboardQueryReqVO reqVO,
                                     HttpServletResponse response) throws IOException {
        DashboardSummaryRespVO summaryRespVO = dashboardService.getDashboardSummary(reqVO);
        List<DashboardTrendExportRespVO> list = summaryRespVO.getTrends().stream()
                .map(item -> buildDashboardTrendExportRespVO(summaryRespVO, item))
                .toList();
        ExcelUtils.write(response, "监控大盘趋势.xls", "数据", DashboardTrendExportRespVO.class, list);
    }

    private DashboardTrendExportRespVO buildDashboardTrendExportRespVO(DashboardSummaryRespVO summaryRespVO,
                                                                       DashboardTrendPointRespVO trendPointRespVO) {
        DashboardTrendExportRespVO exportRespVO = new DashboardTrendExportRespVO();
        exportRespVO.setSceneCode(summaryRespVO.getSceneCode());
        exportRespVO.setStatGranularity(summaryRespVO.getStatGranularity());
        exportRespVO.setStatTime(trendPointRespVO.getStatTime());
        exportRespVO.setEventInTotal(trendPointRespVO.getEventInTotal());
        exportRespVO.setDecisionTotal(trendPointRespVO.getDecisionTotal());
        exportRespVO.setPassRate(trendPointRespVO.getPassRate());
        exportRespVO.setReviewRate(trendPointRespVO.getReviewRate());
        exportRespVO.setRejectRate(trendPointRespVO.getRejectRate());
        exportRespVO.setP95LatencyMs(trendPointRespVO.getP95LatencyMs());
        return exportRespVO;
    }

}
