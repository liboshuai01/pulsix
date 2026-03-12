package cn.liboshuai.pulsix.module.risk.controller.admin.release;

import cn.liboshuai.pulsix.framework.common.pojo.CommonResult;
import cn.liboshuai.pulsix.framework.common.pojo.PageResult;
import cn.liboshuai.pulsix.module.risk.controller.admin.release.vo.SceneReleaseCompileReqVO;
import cn.liboshuai.pulsix.module.risk.controller.admin.release.vo.SceneReleasePageReqVO;
import cn.liboshuai.pulsix.module.risk.controller.admin.release.vo.SceneReleasePublishReqVO;
import cn.liboshuai.pulsix.module.risk.controller.admin.release.vo.SceneReleaseRespVO;
import cn.liboshuai.pulsix.module.risk.controller.admin.release.vo.SceneReleaseRollbackReqVO;
import cn.liboshuai.pulsix.module.risk.service.release.SceneReleaseService;
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

@Tag(name = "管理后台 - 发布中心")
@RestController
@RequestMapping("/risk/release")
@Validated
public class SceneReleaseController {

    @Resource
    private SceneReleaseService sceneReleaseService;

    @GetMapping("/page")
    @Operation(summary = "获得发布记录分页")
    @PreAuthorize("@ss.hasPermission('risk:release:query')")
    public CommonResult<PageResult<SceneReleaseRespVO>> getSceneReleasePage(@Valid SceneReleasePageReqVO pageReqVO) {
        return success(sceneReleaseService.getSceneReleasePage(pageReqVO));
    }

    @GetMapping("/get")
    @Operation(summary = "获得发布记录详情/预览")
    @Parameter(name = "id", description = "编号", required = true, example = "1")
    @PreAuthorize("@ss.hasPermission('risk:release:preview')")
    public CommonResult<SceneReleaseRespVO> getSceneRelease(@RequestParam("id") Long id) {
        return success(sceneReleaseService.getSceneRelease(id));
    }

    @PostMapping("/compile")
    @Operation(summary = "执行发布预检并生成候选版本")
    @PreAuthorize("@ss.hasPermission('risk:release:compile')")
    public CommonResult<SceneReleaseRespVO> compileSceneRelease(@Valid @RequestBody SceneReleaseCompileReqVO reqVO) {
        return success(sceneReleaseService.compileSceneRelease(reqVO));
    }

    @PostMapping("/publish")
    @Operation(summary = "正式发布候选版本")
    @PreAuthorize("@ss.hasPermission('risk:release:publish')")
    public CommonResult<SceneReleaseRespVO> publishSceneRelease(@Valid @RequestBody SceneReleasePublishReqVO reqVO) {
        return success(sceneReleaseService.publishSceneRelease(reqVO));
    }

    @PostMapping("/rollback")
    @Operation(summary = "回滚到指定历史版本")
    @PreAuthorize("@ss.hasPermission('risk:release:rollback')")
    public CommonResult<SceneReleaseRespVO> rollbackSceneRelease(@Valid @RequestBody SceneReleaseRollbackReqVO reqVO) {
        return success(sceneReleaseService.rollbackSceneRelease(reqVO));
    }

}
