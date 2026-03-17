package cn.liboshuai.pulsix.module.risk.controller.admin.accessmapping;

import cn.liboshuai.pulsix.framework.common.pojo.CommonResult;
import cn.liboshuai.pulsix.framework.common.pojo.PageResult;
import cn.liboshuai.pulsix.module.risk.controller.admin.accessmapping.vo.AccessMappingPageReqVO;
import cn.liboshuai.pulsix.module.risk.controller.admin.accessmapping.vo.AccessMappingPreviewRespVO;
import cn.liboshuai.pulsix.module.risk.controller.admin.accessmapping.vo.AccessMappingRespVO;
import cn.liboshuai.pulsix.module.risk.controller.admin.accessmapping.vo.AccessMappingSaveReqVO;
import cn.liboshuai.pulsix.module.risk.convert.accessmapping.AccessMappingConvert;
import cn.liboshuai.pulsix.module.risk.dal.dataobject.accesssource.EventAccessBindingDO;
import cn.liboshuai.pulsix.module.risk.service.accessmapping.AccessMappingService;
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

@Tag(name = "管理后台 - 接入映射")
@RestController
@RequestMapping("/risk/access-mapping")
@Validated
public class AccessMappingController {

    @Resource
    private AccessMappingService accessMappingService;
    @Resource
    private AdminUserApi adminUserApi;

    @PostMapping("/create")
    @Operation(summary = "创建接入映射")
    @PreAuthorize("@ss.hasPermission('risk:access-mapping:create')")
    public CommonResult<Long> createAccessMapping(@Valid @RequestBody AccessMappingSaveReqVO createReqVO) {
        return success(accessMappingService.createAccessMapping(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "修改接入映射")
    @PreAuthorize("@ss.hasPermission('risk:access-mapping:update')")
    public CommonResult<Boolean> updateAccessMapping(@Valid @RequestBody AccessMappingSaveReqVO updateReqVO) {
        accessMappingService.updateAccessMapping(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除接入映射")
    @Parameter(name = "id", description = "编号", required = true, example = "14101")
    @PreAuthorize("@ss.hasPermission('risk:access-mapping:delete')")
    public CommonResult<Boolean> deleteAccessMapping(@RequestParam("id") Long id) {
        accessMappingService.deleteAccessMapping(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得接入映射详情")
    @Parameter(name = "id", description = "编号", required = true, example = "14101")
    @PreAuthorize("@ss.hasPermission('risk:access-mapping:query')")
    public CommonResult<AccessMappingRespVO> getAccessMapping(@RequestParam("id") Long id) {
        EventAccessBindingDO binding = accessMappingService.getAccessMapping(id);
        if (binding == null) {
            return success(null);
        }
        AccessMappingRespVO respVO = AccessMappingConvert.INSTANCE.convert(binding);
        respVO.setRawFields(AccessMappingConvert.INSTANCE.convertRawFieldList(accessMappingService.getRawFieldList(binding.getId())));
        respVO.setMappingRules(AccessMappingConvert.INSTANCE.convertMappingRuleList(accessMappingService.getMappingRuleList(binding.getId())));
        translateAuditUsers(Collections.singletonList(respVO));
        return success(respVO);
    }

    @GetMapping("/page")
    @Operation(summary = "获得接入映射分页")
    @PreAuthorize("@ss.hasPermission('risk:access-mapping:query')")
    public CommonResult<PageResult<AccessMappingRespVO>> getAccessMappingPage(@Valid AccessMappingPageReqVO pageReqVO) {
        PageResult<EventAccessBindingDO> pageResult = accessMappingService.getAccessMappingPage(pageReqVO);
        PageResult<AccessMappingRespVO> respVOPage = AccessMappingConvert.INSTANCE.convertPage(pageResult);
        for (AccessMappingRespVO respVO : respVOPage.getList()) {
            respVO.setRawFields(null);
            respVO.setMappingRules(null);
        }
        translateAuditUsers(respVOPage.getList());
        return success(respVOPage);
    }

    @PostMapping("/preview-standard")
    @Operation(summary = "预览标准事件")
    @PreAuthorize("@ss.hasPermission('risk:access-mapping:query')")
    public CommonResult<AccessMappingPreviewRespVO> previewStandardEvent(
            @Valid @RequestBody AccessMappingSaveReqVO reqVO) {
        return success(accessMappingService.previewStandardEvent(reqVO));
    }

    private void translateAuditUsers(List<AccessMappingRespVO> accessMappings) {
        if (accessMappings == null || accessMappings.isEmpty()) {
            return;
        }

        Set<Long> userIds = new HashSet<>();
        for (AccessMappingRespVO accessMapping : accessMappings) {
            Long creatorId = parseUserId(accessMapping.getCreator());
            if (creatorId != null) {
                userIds.add(creatorId);
            }
            Long updaterId = parseUserId(accessMapping.getUpdater());
            if (updaterId != null) {
                userIds.add(updaterId);
            }
        }
        if (userIds.isEmpty()) {
            return;
        }

        Map<Long, AdminUserRespDTO> userMap = adminUserApi.getUserMap(userIds);
        for (AccessMappingRespVO accessMapping : accessMappings) {
            accessMapping.setCreator(resolveUserNickname(accessMapping.getCreator(), userMap));
            accessMapping.setUpdater(resolveUserNickname(accessMapping.getUpdater(), userMap));
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
