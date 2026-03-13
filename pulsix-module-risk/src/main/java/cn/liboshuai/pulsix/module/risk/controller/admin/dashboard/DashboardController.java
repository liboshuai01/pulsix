package cn.liboshuai.pulsix.module.risk.controller.admin.dashboard;

import cn.liboshuai.pulsix.framework.common.pojo.CommonResult;
import cn.liboshuai.pulsix.module.risk.controller.admin.dashboard.vo.DashboardQueryReqVO;
import cn.liboshuai.pulsix.module.risk.controller.admin.dashboard.vo.DashboardSummaryRespVO;
import cn.liboshuai.pulsix.module.risk.service.dashboard.DashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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

}
