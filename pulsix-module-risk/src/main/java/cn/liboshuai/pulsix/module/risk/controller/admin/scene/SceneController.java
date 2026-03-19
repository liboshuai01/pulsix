package cn.liboshuai.pulsix.module.risk.controller.admin.scene;

import cn.liboshuai.pulsix.framework.common.pojo.CommonResult;
import cn.liboshuai.pulsix.framework.common.pojo.PageResult;
import cn.liboshuai.pulsix.module.risk.controller.admin.scene.vo.ScenePageReqVO;
import cn.liboshuai.pulsix.module.risk.controller.admin.scene.vo.SceneRespVO;
import cn.liboshuai.pulsix.module.risk.controller.admin.scene.vo.SceneSaveReqVO;
import cn.liboshuai.pulsix.module.risk.controller.admin.scene.vo.SceneSimpleRespVO;
import cn.liboshuai.pulsix.module.risk.controller.admin.scene.vo.SceneUpdateStatusReqVO;
import cn.liboshuai.pulsix.module.risk.convert.scene.SceneConvert;
import cn.liboshuai.pulsix.module.risk.dal.dataobject.scene.SceneDO;
import cn.liboshuai.pulsix.module.risk.service.scene.SceneService;
import cn.liboshuai.pulsix.module.system.api.user.AdminUserApi;
import cn.liboshuai.pulsix.module.system.api.user.dto.AdminUserRespDTO;
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

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static cn.liboshuai.pulsix.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - 风控场景")
@RestController
@RequestMapping("/risk/scene")
@Validated
public class SceneController {

    @Resource
    private SceneService sceneService;
    @Resource
    private AdminUserApi adminUserApi;

    @PostMapping("/create")
    @Operation(summary = "创建风控场景")
    @PreAuthorize("@ss.hasPermission('risk:scene:create')")
    public CommonResult<Long> createScene(@Valid @RequestBody SceneSaveReqVO createReqVO) {
        return success(sceneService.createScene(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "修改风控场景")
    @PreAuthorize("@ss.hasPermission('risk:scene:update')")
    public CommonResult<Boolean> updateScene(@Valid @RequestBody SceneSaveReqVO updateReqVO) {
        sceneService.updateScene(updateReqVO);
        return success(true);
    }

    @PutMapping("/update-status")
    @Operation(summary = "修改风控场景状态")
    @PreAuthorize("@ss.hasPermission('risk:scene:update')")
    public CommonResult<Boolean> updateSceneStatus(@Valid @RequestBody SceneUpdateStatusReqVO reqVO) {
        sceneService.updateSceneStatus(reqVO.getId(), reqVO.getStatus());
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除风控场景")
    @Parameter(name = "id", description = "编号", required = true, example = "1")
    @PreAuthorize("@ss.hasPermission('risk:scene:delete')")
    public CommonResult<Boolean> deleteScene(@RequestParam("id") Long id) {
        sceneService.deleteScene(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得风控场景详情")
    @Parameter(name = "id", description = "编号", required = true, example = "1")
    @PreAuthorize("@ss.hasPermission('risk:scene:query')")
    public CommonResult<SceneRespVO> getScene(@RequestParam("id") Long id) {
        SceneDO scene = sceneService.getScene(id);
        if (scene == null) {
            return success(null);
        }
        SceneRespVO respVO = SceneConvert.INSTANCE.convert(scene);
        fillDeleteState(Collections.singletonList(scene), Collections.singletonList(respVO));
        translateAuditUsers(Collections.singletonList(respVO));
        return success(respVO);
    }

    @GetMapping("/page")
    @Operation(summary = "获得风控场景分页")
    @PreAuthorize("@ss.hasPermission('risk:scene:query')")
    public CommonResult<PageResult<SceneRespVO>> getScenePage(@Valid ScenePageReqVO pageReqVO) {
        PageResult<SceneDO> pageResult = sceneService.getScenePage(pageReqVO);
        PageResult<SceneRespVO> respVOPage = SceneConvert.INSTANCE.convertPage(pageResult);
        fillDeleteState(pageResult.getList(), respVOPage.getList());
        translateAuditUsers(respVOPage.getList());
        return success(respVOPage);
    }

    @GetMapping("/simple-list")
    @Operation(summary = "获得启用中的风控场景精简列表")
    @PreAuthorize("@ss.hasPermission('risk:scene:query')")
    public CommonResult<List<SceneSimpleRespVO>> getSimpleSceneList() {
        return success(SceneConvert.INSTANCE.convertSimpleList(sceneService.getSimpleSceneList()));
    }

    private void fillDeleteState(List<SceneDO> scenes, List<SceneRespVO> respVOs) {
        if (scenes == null || respVOs == null) {
            return;
        }
        for (int i = 0; i < Math.min(scenes.size(), respVOs.size()); i++) {
            String blockedReason = sceneService.getDeleteBlockedReason(scenes.get(i));
            respVOs.get(i).setDeletable(blockedReason == null);
            respVOs.get(i).setDeleteBlockedReason(blockedReason);
        }
    }

    private void translateAuditUsers(List<SceneRespVO> scenes) {
        if (scenes == null || scenes.isEmpty()) {
            return;
        }

        Set<Long> userIds = new HashSet<>();
        for (SceneRespVO scene : scenes) {
            Long creatorId = parseUserId(scene.getCreator());
            if (creatorId != null) {
                userIds.add(creatorId);
            }
            Long updaterId = parseUserId(scene.getUpdater());
            if (updaterId != null) {
                userIds.add(updaterId);
            }
        }
        if (userIds.isEmpty()) {
            return;
        }

        Map<Long, AdminUserRespDTO> userMap = adminUserApi.getUserMap(userIds);
        for (SceneRespVO scene : scenes) {
            scene.setCreator(resolveUserNickname(scene.getCreator(), userMap));
            scene.setUpdater(resolveUserNickname(scene.getUpdater(), userMap));
        }
    }

    private String resolveUserNickname(String rawUserId, Map<Long, AdminUserRespDTO> userMap) {
        Long userId = parseUserId(rawUserId);
        if (userId == null) {
            return rawUserId;
        }
        AdminUserRespDTO user = userMap.get(userId);
        if (user == null || user.getNickname() == null || user.getNickname().isBlank()) {
            return rawUserId;
        }
        return user.getNickname();
    }

    private Long parseUserId(String rawUserId) {
        if (rawUserId == null || rawUserId.isBlank()) {
            return null;
        }
        try {
            return Long.valueOf(rawUserId);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

}
