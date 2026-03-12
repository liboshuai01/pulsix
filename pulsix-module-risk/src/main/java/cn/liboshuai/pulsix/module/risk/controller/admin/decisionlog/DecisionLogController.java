package cn.liboshuai.pulsix.module.risk.controller.admin.decisionlog;

import cn.liboshuai.pulsix.framework.common.pojo.CommonResult;
import cn.liboshuai.pulsix.framework.common.pojo.PageResult;
import cn.liboshuai.pulsix.module.risk.controller.admin.decisionlog.vo.DecisionLogDetailRespVO;
import cn.liboshuai.pulsix.module.risk.controller.admin.decisionlog.vo.DecisionLogPageReqVO;
import cn.liboshuai.pulsix.module.risk.controller.admin.decisionlog.vo.DecisionLogRespVO;
import cn.liboshuai.pulsix.module.risk.controller.admin.decisionlog.vo.RuleHitLogRespVO;
import cn.liboshuai.pulsix.module.risk.service.decisionlog.DecisionLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static cn.liboshuai.pulsix.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - 决策日志")
@RestController
@RequestMapping("/risk/decision-log")
@Validated
public class DecisionLogController {

    @Resource
    private DecisionLogService decisionLogService;

    @GetMapping("/page")
    @Operation(summary = "获得决策日志分页")
    @PreAuthorize("@ss.hasPermission('risk:decision-log:query')")
    public CommonResult<PageResult<DecisionLogRespVO>> getDecisionLogPage(@Valid DecisionLogPageReqVO pageReqVO) {
        return success(decisionLogService.getDecisionLogPage(pageReqVO));
    }

    @GetMapping("/get")
    @Operation(summary = "获得决策日志详情")
    @Parameter(name = "id", description = "编号", required = true, example = "7101")
    @PreAuthorize("@ss.hasPermission('risk:decision-log:get')")
    public CommonResult<DecisionLogDetailRespVO> getDecisionLog(@RequestParam("id") Long id) {
        return success(decisionLogService.getDecisionLog(id));
    }

    @GetMapping("/hit-log/list")
    @Operation(summary = "获得规则命中明细")
    @Parameter(name = "decisionId", description = "决策日志编号", required = true, example = "7101")
    @PreAuthorize("@ss.hasPermission('risk:decision-log:detail')")
    public CommonResult<List<RuleHitLogRespVO>> getRuleHitLogList(@RequestParam("decisionId") Long decisionId) {
        return success(decisionLogService.getRuleHitLogList(decisionId));
    }

}
