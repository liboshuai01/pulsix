package cn.liboshuai.pulsix.module.risk.controller.admin.simulation;

import cn.liboshuai.pulsix.framework.common.pojo.CommonResult;
import cn.liboshuai.pulsix.framework.common.pojo.PageResult;
import cn.liboshuai.pulsix.module.risk.controller.admin.simulation.vo.SimulationCasePageReqVO;
import cn.liboshuai.pulsix.module.risk.controller.admin.simulation.vo.SimulationCaseRespVO;
import cn.liboshuai.pulsix.module.risk.controller.admin.simulation.vo.SimulationCaseSaveReqVO;
import cn.liboshuai.pulsix.module.risk.controller.admin.simulation.vo.SimulationExecuteReqVO;
import cn.liboshuai.pulsix.module.risk.controller.admin.simulation.vo.SimulationReportRespVO;
import cn.liboshuai.pulsix.module.risk.service.simulation.RiskSimulationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import static cn.liboshuai.pulsix.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - 仿真测试")
@RestController
@RequestMapping("/risk/simulation")
@Validated
public class RiskSimulationController {

    @Resource
    private RiskSimulationService riskSimulationService;

    @PostMapping("/create")
    @Operation(summary = "创建仿真用例")
    @PreAuthorize("@ss.hasPermission('risk:simulation:create')")
    public CommonResult<Long> createSimulationCase(@Valid @RequestBody SimulationCaseSaveReqVO createReqVO) {
        return success(riskSimulationService.createSimulationCase(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "修改仿真用例")
    @PreAuthorize("@ss.hasPermission('risk:simulation:update')")
    public CommonResult<Boolean> updateSimulationCase(@Valid @RequestBody SimulationCaseSaveReqVO updateReqVO) {
        riskSimulationService.updateSimulationCase(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除仿真用例")
    @Parameter(name = "id", description = "编号", required = true, example = "5101")
    @PreAuthorize("@ss.hasPermission('risk:simulation:delete')")
    public CommonResult<Boolean> deleteSimulationCase(@RequestParam("id") Long id) {
        riskSimulationService.deleteSimulationCase(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得仿真用例详情")
    @Parameter(name = "id", description = "编号", required = true, example = "5101")
    @PreAuthorize("@ss.hasPermission('risk:simulation:query')")
    public CommonResult<SimulationCaseRespVO> getSimulationCase(@RequestParam("id") Long id) {
        return success(riskSimulationService.getSimulationCase(id));
    }

    @GetMapping("/page")
    @Operation(summary = "获得仿真用例分页")
    @PreAuthorize("@ss.hasPermission('risk:simulation:query')")
    public CommonResult<PageResult<SimulationCaseRespVO>> getSimulationCasePage(@Valid SimulationCasePageReqVO pageReqVO) {
        return success(riskSimulationService.getSimulationCasePage(pageReqVO));
    }

    @PostMapping("/execute")
    @Operation(summary = "执行仿真")
    @PreAuthorize("@ss.hasPermission('risk:simulation:execute')")
    public CommonResult<SimulationReportRespVO> executeSimulation(@Valid @RequestBody SimulationExecuteReqVO reqVO) {
        return success(riskSimulationService.executeSimulation(reqVO));
    }

    @GetMapping("/report/get")
    @Operation(summary = "获得仿真报告详情")
    @Parameter(name = "id", description = "编号", required = true, example = "6101")
    @PreAuthorize("@ss.hasPermission('risk:simulation:query')")
    public CommonResult<SimulationReportRespVO> getSimulationReport(@RequestParam("id") Long id) {
        return success(riskSimulationService.getSimulationReport(id));
    }

}
