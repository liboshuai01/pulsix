package cn.liboshuai.pulsix.module.risk.controller.admin.eventsample;

import cn.liboshuai.pulsix.framework.common.pojo.CommonResult;
import cn.liboshuai.pulsix.framework.common.pojo.PageResult;
import cn.liboshuai.pulsix.framework.common.util.object.BeanUtils;
import cn.liboshuai.pulsix.module.risk.controller.admin.eventsample.vo.EventSamplePageReqVO;
import cn.liboshuai.pulsix.module.risk.controller.admin.eventsample.vo.EventSamplePreviewRespVO;
import cn.liboshuai.pulsix.module.risk.controller.admin.eventsample.vo.EventSampleRespVO;
import cn.liboshuai.pulsix.module.risk.controller.admin.eventsample.vo.EventSampleSaveReqVO;
import cn.liboshuai.pulsix.module.risk.dal.dataobject.eventsample.EventSampleDO;
import cn.liboshuai.pulsix.module.risk.service.eventsample.EventSampleService;
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

@Tag(name = "管理后台 - 事件样例")
@RestController
@RequestMapping("/risk/event-sample")
@Validated
public class EventSampleController {

    @Resource
    private EventSampleService eventSampleService;

    @PostMapping("/create")
    @Operation(summary = "创建事件样例")
    @PreAuthorize("@ss.hasPermission('risk:event-sample:create')")
    public CommonResult<Long> createEventSample(@Valid @RequestBody EventSampleSaveReqVO createReqVO) {
        return success(eventSampleService.createEventSample(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "修改事件样例")
    @PreAuthorize("@ss.hasPermission('risk:event-sample:update')")
    public CommonResult<Boolean> updateEventSample(@Valid @RequestBody EventSampleSaveReqVO updateReqVO) {
        eventSampleService.updateEventSample(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除事件样例")
    @Parameter(name = "id", description = "编号", required = true, example = "1")
    @PreAuthorize("@ss.hasPermission('risk:event-sample:delete')")
    public CommonResult<Boolean> deleteEventSample(@RequestParam("id") Long id) {
        eventSampleService.deleteEventSample(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得事件样例详情")
    @Parameter(name = "id", description = "编号", required = true, example = "1")
    @PreAuthorize("@ss.hasPermission('risk:event-sample:query')")
    public CommonResult<EventSampleRespVO> getEventSample(@RequestParam("id") Long id) {
        EventSampleDO eventSample = eventSampleService.getEventSample(id);
        return success(BeanUtils.toBean(eventSample, EventSampleRespVO.class));
    }

    @GetMapping("/page")
    @Operation(summary = "获得事件样例分页")
    @PreAuthorize("@ss.hasPermission('risk:event-sample:query')")
    public CommonResult<PageResult<EventSampleRespVO>> getEventSamplePage(@Valid EventSamplePageReqVO pageReqVO) {
        PageResult<EventSampleDO> pageResult = eventSampleService.getEventSamplePage(pageReqVO);
        return success(BeanUtils.toBean(pageResult, EventSampleRespVO.class));
    }

    @GetMapping("/preview")
    @Operation(summary = "预览标准事件")
    @Parameter(name = "id", description = "编号", required = true, example = "1")
    @PreAuthorize("@ss.hasPermission('risk:event-sample:preview')")
    public CommonResult<EventSamplePreviewRespVO> previewEventSample(@RequestParam("id") Long id) {
        return success(eventSampleService.previewEventSample(id));
    }

}
