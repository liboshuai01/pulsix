package cn.liboshuai.pulsix.module.risk.controller.admin.replay;

import cn.liboshuai.pulsix.framework.common.pojo.CommonResult;
import cn.liboshuai.pulsix.framework.common.pojo.PageResult;
import cn.liboshuai.pulsix.module.risk.controller.admin.replay.vo.ReplayJobCreateReqVO;
import cn.liboshuai.pulsix.module.risk.controller.admin.replay.vo.ReplayJobDetailRespVO;
import cn.liboshuai.pulsix.module.risk.controller.admin.replay.vo.ReplayJobExecuteReqVO;
import cn.liboshuai.pulsix.module.risk.controller.admin.replay.vo.ReplayJobPageReqVO;
import cn.liboshuai.pulsix.module.risk.controller.admin.replay.vo.ReplayJobRespVO;
import cn.liboshuai.pulsix.module.risk.service.replay.ReplayJobService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import static cn.liboshuai.pulsix.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - 回放对比")
@RestController
@RequestMapping("/risk/replay")
@Validated
public class ReplayJobController {

    @Resource
    private ReplayJobService replayJobService;

    @PostMapping("/create")
    @Operation(summary = "创建回放任务")
    @PreAuthorize("@ss.hasPermission('risk:replay:create')")
    public CommonResult<Long> createReplayJob(@Valid @RequestBody ReplayJobCreateReqVO createReqVO) {
        return success(replayJobService.createReplayJob(createReqVO));
    }

    @GetMapping("/page")
    @Operation(summary = "获得回放任务分页")
    @PreAuthorize("@ss.hasPermission('risk:replay:query')")
    public CommonResult<PageResult<ReplayJobRespVO>> getReplayJobPage(@Valid ReplayJobPageReqVO pageReqVO) {
        return success(replayJobService.getReplayJobPage(pageReqVO));
    }

    @GetMapping("/get")
    @Operation(summary = "获得回放任务详情")
    @Parameter(name = "id", description = "编号", required = true, example = "11001")
    @PreAuthorize("@ss.hasPermission('risk:replay:get')")
    public CommonResult<ReplayJobDetailRespVO> getReplayJob(@RequestParam("id") Long id) {
        return success(replayJobService.getReplayJob(id));
    }

    @PostMapping("/execute")
    @Operation(summary = "执行回放任务")
    @PreAuthorize("@ss.hasPermission('risk:replay:execute')")
    public CommonResult<ReplayJobDetailRespVO> executeReplayJob(@Valid @RequestBody ReplayJobExecuteReqVO reqVO) {
        return success(replayJobService.executeReplayJob(reqVO));
    }

}
