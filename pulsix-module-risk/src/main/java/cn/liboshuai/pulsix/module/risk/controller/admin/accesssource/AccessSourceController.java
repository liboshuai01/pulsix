package cn.liboshuai.pulsix.module.risk.controller.admin.accesssource;

import cn.liboshuai.pulsix.framework.common.pojo.CommonResult;
import cn.liboshuai.pulsix.framework.common.pojo.PageResult;
import cn.liboshuai.pulsix.module.risk.controller.admin.accesssource.vo.AccessSourcePageReqVO;
import cn.liboshuai.pulsix.module.risk.controller.admin.accesssource.vo.AccessSourceRespVO;
import cn.liboshuai.pulsix.module.risk.controller.admin.accesssource.vo.AccessSourceSaveReqVO;
import cn.liboshuai.pulsix.module.risk.controller.admin.accesssource.vo.AccessSourceSimpleRespVO;
import cn.liboshuai.pulsix.module.risk.controller.admin.accesssource.vo.AccessSourceUpdateStatusReqVO;
import cn.liboshuai.pulsix.module.risk.convert.accesssource.AccessSourceConvert;
import cn.liboshuai.pulsix.module.risk.dal.dataobject.accesssource.AccessSourceDO;
import cn.liboshuai.pulsix.module.risk.service.accesssource.AccessSourceService;
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

@Tag(name = "管理后台 - 接入源")
@RestController
@RequestMapping("/risk/access-source")
@Validated
public class AccessSourceController {

    @Resource
    private AccessSourceService accessSourceService;
    @Resource
    private AdminUserApi adminUserApi;

    @PostMapping("/create")
    @Operation(summary = "创建接入源")
    @PreAuthorize("@ss.hasPermission('risk:access-source:create')")
    public CommonResult<Long> createAccessSource(@Valid @RequestBody AccessSourceSaveReqVO createReqVO) {
        return success(accessSourceService.createAccessSource(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "修改接入源")
    @PreAuthorize("@ss.hasPermission('risk:access-source:update')")
    public CommonResult<Boolean> updateAccessSource(@Valid @RequestBody AccessSourceSaveReqVO updateReqVO) {
        accessSourceService.updateAccessSource(updateReqVO);
        return success(true);
    }

    @PutMapping("/update-status")
    @Operation(summary = "修改接入源状态")
    @PreAuthorize("@ss.hasPermission('risk:access-source:update')")
    public CommonResult<Boolean> updateAccessSourceStatus(@Valid @RequestBody AccessSourceUpdateStatusReqVO reqVO) {
        accessSourceService.updateAccessSourceStatus(reqVO.getId(), reqVO.getStatus());
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除接入源")
    @Parameter(name = "id", description = "编号", required = true, example = "14001")
    @PreAuthorize("@ss.hasPermission('risk:access-source:delete')")
    public CommonResult<Boolean> deleteAccessSource(@RequestParam("id") Long id) {
        accessSourceService.deleteAccessSource(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得接入源详情")
    @Parameter(name = "id", description = "编号", required = true, example = "14001")
    @PreAuthorize("@ss.hasPermission('risk:access-source:query')")
    public CommonResult<AccessSourceRespVO> getAccessSource(@RequestParam("id") Long id) {
        AccessSourceDO accessSource = accessSourceService.getAccessSource(id);
        if (accessSource == null) {
            return success(null);
        }
        AccessSourceRespVO respVO = AccessSourceConvert.INSTANCE.convert(accessSource);
        translateAuditUsers(Collections.singletonList(respVO));
        return success(respVO);
    }

    @GetMapping("/page")
    @Operation(summary = "获得接入源分页")
    @PreAuthorize("@ss.hasPermission('risk:access-source:query')")
    public CommonResult<PageResult<AccessSourceRespVO>> getAccessSourcePage(@Valid AccessSourcePageReqVO pageReqVO) {
        PageResult<AccessSourceDO> pageResult = accessSourceService.getAccessSourcePage(pageReqVO);
        PageResult<AccessSourceRespVO> respVOPage = AccessSourceConvert.INSTANCE.convertPage(pageResult);
        translateAuditUsers(respVOPage.getList());
        return success(respVOPage);
    }

    @GetMapping("/simple-list")
    @Operation(summary = "获得启用中的接入源精简列表")
    @PreAuthorize("@ss.hasPermission('risk:access-source:query')")
    public CommonResult<List<AccessSourceSimpleRespVO>> getSimpleAccessSourceList(
            @RequestParam(value = "sceneCode", required = false) String sceneCode) {
        return success(AccessSourceConvert.INSTANCE.convertSimpleList(accessSourceService.getSimpleAccessSourceList(sceneCode)));
    }

    private void translateAuditUsers(List<AccessSourceRespVO> accessSources) {
        if (accessSources == null || accessSources.isEmpty()) {
            return;
        }

        Set<Long> userIds = new HashSet<>();
        for (AccessSourceRespVO accessSource : accessSources) {
            Long creatorId = parseUserId(accessSource.getCreator());
            if (creatorId != null) {
                userIds.add(creatorId);
            }
            Long updaterId = parseUserId(accessSource.getUpdater());
            if (updaterId != null) {
                userIds.add(updaterId);
            }
        }
        if (userIds.isEmpty()) {
            return;
        }

        Map<Long, AdminUserRespDTO> userMap = adminUserApi.getUserMap(userIds);
        for (AccessSourceRespVO accessSource : accessSources) {
            accessSource.setCreator(resolveUserNickname(accessSource.getCreator(), userMap));
            accessSource.setUpdater(resolveUserNickname(accessSource.getUpdater(), userMap));
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
