package cn.liboshuai.pulsix.module.risk.controller.admin.eventmodel;

import cn.liboshuai.pulsix.framework.common.pojo.CommonResult;
import cn.liboshuai.pulsix.framework.common.pojo.PageResult;
import cn.liboshuai.pulsix.module.risk.convert.accesssource.AccessSourceConvert;
import cn.liboshuai.pulsix.module.risk.controller.admin.eventmodel.vo.EventModelPageReqVO;
import cn.liboshuai.pulsix.module.risk.controller.admin.eventmodel.vo.EventModelPreviewRespVO;
import cn.liboshuai.pulsix.module.risk.controller.admin.eventmodel.vo.EventModelRespVO;
import cn.liboshuai.pulsix.module.risk.controller.admin.eventmodel.vo.EventModelSaveReqVO;
import cn.liboshuai.pulsix.module.risk.controller.admin.eventmodel.vo.EventModelSimpleRespVO;
import cn.liboshuai.pulsix.module.risk.controller.admin.eventmodel.vo.EventModelUpdateStatusReqVO;
import cn.liboshuai.pulsix.module.risk.convert.eventmodel.EventModelConvert;
import cn.liboshuai.pulsix.module.risk.dal.dataobject.accesssource.AccessSourceDO;
import cn.liboshuai.pulsix.module.risk.dal.dataobject.eventmodel.EventSchemaDO;
import cn.liboshuai.pulsix.module.risk.service.accesssource.AccessSourceService;
import cn.liboshuai.pulsix.module.risk.service.eventmodel.EventModelService;
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

@Tag(name = "管理后台 - 事件模型")
@RestController
@RequestMapping("/risk/event-model")
@Validated
public class EventModelController {

    @Resource
    private EventModelService eventModelService;
    @Resource
    private AccessSourceService accessSourceService;
    @Resource
    private AdminUserApi adminUserApi;

    @PostMapping("/create")
    @Operation(summary = "创建事件模型")
    @PreAuthorize("@ss.hasPermission('risk:event-model:create')")
    public CommonResult<Long> createEventModel(@Valid @RequestBody EventModelSaveReqVO createReqVO) {
        return success(eventModelService.createEventModel(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "修改事件模型")
    @PreAuthorize("@ss.hasPermission('risk:event-model:update')")
    public CommonResult<Boolean> updateEventModel(@Valid @RequestBody EventModelSaveReqVO updateReqVO) {
        eventModelService.updateEventModel(updateReqVO);
        return success(true);
    }

    @PutMapping("/update-status")
    @Operation(summary = "修改事件模型状态")
    @PreAuthorize("@ss.hasPermission('risk:event-model:update')")
    public CommonResult<Boolean> updateEventModelStatus(@Valid @RequestBody EventModelUpdateStatusReqVO reqVO) {
        eventModelService.updateEventModelStatus(reqVO.getId(), reqVO.getStatus());
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除事件模型")
    @Parameter(name = "id", description = "编号", required = true, example = "1")
    @PreAuthorize("@ss.hasPermission('risk:event-model:delete')")
    public CommonResult<Boolean> deleteEventModel(@RequestParam("id") Long id) {
        eventModelService.deleteEventModel(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得事件模型详情")
    @Parameter(name = "id", description = "编号", required = true, example = "1")
    @PreAuthorize("@ss.hasPermission('risk:event-model:query')")
    public CommonResult<EventModelRespVO> getEventModel(@RequestParam("id") Long id) {
        EventSchemaDO schema = eventModelService.getEventModel(id);
        if (schema == null) {
            return success(null);
        }
        EventModelRespVO respVO = EventModelConvert.INSTANCE.convert(schema);
        respVO.setFields(EventModelConvert.INSTANCE.convertFieldList(eventModelService.getEventFieldList(schema.getEventCode())));
        fillBindingSources(Collections.singletonList(respVO));
        translateAuditUsers(Collections.singletonList(respVO));
        return success(respVO);
    }

    @GetMapping("/page")
    @Operation(summary = "获得事件模型分页")
    @PreAuthorize("@ss.hasPermission('risk:event-model:query')")
    public CommonResult<PageResult<EventModelRespVO>> getEventModelPage(@Valid EventModelPageReqVO pageReqVO) {
        PageResult<EventSchemaDO> pageResult = eventModelService.getEventModelPage(pageReqVO);
        PageResult<EventModelRespVO> respVOPage = EventModelConvert.INSTANCE.convertPage(pageResult);
        for (EventModelRespVO respVO : respVOPage.getList()) {
            respVO.setFields(null);
            respVO.setSampleEventJson(null);
        }
        fillBindingSources(respVOPage.getList());
        translateAuditUsers(respVOPage.getList());
        return success(respVOPage);
    }

    @GetMapping("/simple-list")
    @Operation(summary = "获得启用中的事件模型精简列表")
    @PreAuthorize("@ss.hasPermission('risk:event-model:query')")
    public CommonResult<List<EventModelSimpleRespVO>> getSimpleEventModelList(
            @RequestParam(value = "sceneCode", required = false) String sceneCode) {
        return success(EventModelConvert.INSTANCE.convertSimpleList(eventModelService.getSimpleEventModelList(sceneCode)));
    }

    @PostMapping("/preview-standard")
    @Operation(summary = "预览标准事件")
    @PreAuthorize("@ss.hasPermission('risk:event-model:query')")
    public CommonResult<EventModelPreviewRespVO> previewStandardEvent(@Valid @RequestBody EventModelSaveReqVO reqVO) {
        return success(eventModelService.previewStandardEvent(reqVO));
    }

    private void fillBindingSources(List<EventModelRespVO> eventModels) {
        if (eventModels == null || eventModels.isEmpty()) {
            return;
        }

        Set<String> eventCodes = new HashSet<>();
        for (EventModelRespVO eventModel : eventModels) {
            if (eventModel.getEventCode() != null) {
                eventCodes.add(eventModel.getEventCode());
            }
        }
        if (eventCodes.isEmpty()) {
            return;
        }

        Map<String, List<AccessSourceDO>> bindingSourceMap = accessSourceService.getBindingSourceMap(eventCodes);
        for (EventModelRespVO eventModel : eventModels) {
            List<AccessSourceDO> bindingSources = bindingSourceMap.getOrDefault(eventModel.getEventCode(), Collections.emptyList());
            eventModel.setBindingSources(AccessSourceConvert.INSTANCE.convertBindingItemList(bindingSources));
        }
    }

    private void translateAuditUsers(List<EventModelRespVO> eventModels) {
        if (eventModels == null || eventModels.isEmpty()) {
            return;
        }

        Set<Long> userIds = new HashSet<>();
        for (EventModelRespVO eventModel : eventModels) {
            Long creatorId = parseUserId(eventModel.getCreator());
            if (creatorId != null) {
                userIds.add(creatorId);
            }
            Long updaterId = parseUserId(eventModel.getUpdater());
            if (updaterId != null) {
                userIds.add(updaterId);
            }
        }
        if (userIds.isEmpty()) {
            return;
        }

        Map<Long, AdminUserRespDTO> userMap = adminUserApi.getUserMap(userIds);
        for (EventModelRespVO eventModel : eventModels) {
            eventModel.setCreator(resolveUserNickname(eventModel.getCreator(), userMap));
            eventModel.setUpdater(resolveUserNickname(eventModel.getUpdater(), userMap));
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
