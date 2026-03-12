package cn.liboshuai.pulsix.module.risk.controller.admin.eventfield;

import cn.liboshuai.pulsix.framework.common.pojo.CommonResult;
import cn.liboshuai.pulsix.framework.common.pojo.PageResult;
import cn.liboshuai.pulsix.framework.common.util.object.BeanUtils;
import cn.liboshuai.pulsix.module.risk.controller.admin.eventfield.vo.EventFieldPageReqVO;
import cn.liboshuai.pulsix.module.risk.controller.admin.eventfield.vo.EventFieldRespVO;
import cn.liboshuai.pulsix.module.risk.controller.admin.eventfield.vo.EventFieldSaveReqVO;
import cn.liboshuai.pulsix.module.risk.controller.admin.eventfield.vo.EventFieldUpdateSortReqVO;
import cn.liboshuai.pulsix.module.risk.dal.dataobject.eventfield.EventFieldDO;
import cn.liboshuai.pulsix.module.risk.service.eventfield.EventFieldService;
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

@Tag(name = "管理后台 - 事件字段")
@RestController
@RequestMapping("/risk/event-field")
@Validated
public class EventFieldController {

    @Resource
    private EventFieldService eventFieldService;

    @PostMapping("/create")
    @Operation(summary = "创建事件字段")
    @PreAuthorize("@ss.hasPermission('risk:event-field:create')")
    public CommonResult<Long> createEventField(@Valid @RequestBody EventFieldSaveReqVO createReqVO) {
        return success(eventFieldService.createEventField(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "修改事件字段")
    @PreAuthorize("@ss.hasPermission('risk:event-field:update')")
    public CommonResult<Boolean> updateEventField(@Valid @RequestBody EventFieldSaveReqVO updateReqVO) {
        eventFieldService.updateEventField(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除事件字段")
    @Parameter(name = "id", description = "编号", required = true, example = "1")
    @PreAuthorize("@ss.hasPermission('risk:event-field:delete')")
    public CommonResult<Boolean> deleteEventField(@RequestParam("id") Long id) {
        eventFieldService.deleteEventField(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得事件字段详情")
    @Parameter(name = "id", description = "编号", required = true, example = "1")
    @PreAuthorize("@ss.hasPermission('risk:event-field:query')")
    public CommonResult<EventFieldRespVO> getEventField(@RequestParam("id") Long id) {
        EventFieldDO eventField = eventFieldService.getEventField(id);
        return success(BeanUtils.toBean(eventField, EventFieldRespVO.class));
    }

    @GetMapping("/page")
    @Operation(summary = "获得事件字段分页")
    @PreAuthorize("@ss.hasPermission('risk:event-field:query')")
    public CommonResult<PageResult<EventFieldRespVO>> getEventFieldPage(@Valid EventFieldPageReqVO pageReqVO) {
        PageResult<EventFieldDO> pageResult = eventFieldService.getEventFieldPage(pageReqVO);
        return success(BeanUtils.toBean(pageResult, EventFieldRespVO.class));
    }

    @PutMapping("/update-sort")
    @Operation(summary = "修改事件字段排序")
    @PreAuthorize("@ss.hasPermission('risk:event-field:sort')")
    public CommonResult<Boolean> updateEventFieldSort(@Valid @RequestBody EventFieldUpdateSortReqVO reqVO) {
        eventFieldService.updateEventFieldSort(reqVO.getId(), reqVO.getSortNo());
        return success(true);
    }

}
