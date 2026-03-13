package cn.liboshuai.pulsix.module.risk.controller.admin.eventschema;

import cn.liboshuai.pulsix.framework.common.pojo.CommonResult;
import cn.liboshuai.pulsix.framework.common.pojo.PageResult;
import cn.liboshuai.pulsix.framework.common.util.object.BeanUtils;
import cn.liboshuai.pulsix.module.risk.controller.admin.eventschema.vo.EventSchemaPageReqVO;
import cn.liboshuai.pulsix.module.risk.controller.admin.eventschema.vo.EventSchemaRespVO;
import cn.liboshuai.pulsix.module.risk.controller.admin.eventschema.vo.EventSchemaSaveReqVO;
import cn.liboshuai.pulsix.module.risk.dal.dataobject.eventschema.EventSchemaDO;
import cn.liboshuai.pulsix.module.risk.service.eventschema.EventSchemaService;
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

@Tag(name = "管理后台 - 事件 Schema")
@RestController
@RequestMapping("/risk/event-schema")
@Validated
public class EventSchemaController {

    @Resource
    private EventSchemaService eventSchemaService;

    @PostMapping("/create")
    @Operation(summary = "创建事件 Schema")
    @PreAuthorize("@ss.hasPermission('risk:event-schema:create')")
    public CommonResult<Long> createEventSchema(@Valid @RequestBody EventSchemaSaveReqVO createReqVO) {
        return success(eventSchemaService.createEventSchema(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "修改事件 Schema")
    @PreAuthorize("@ss.hasPermission('risk:event-schema:update')")
    public CommonResult<Boolean> updateEventSchema(@Valid @RequestBody EventSchemaSaveReqVO updateReqVO) {
        eventSchemaService.updateEventSchema(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除事件 Schema")
    @Parameter(name = "id", description = "编号", required = true, example = "1")
    @PreAuthorize("@ss.hasPermission('risk:event-schema:delete')")
    public CommonResult<Boolean> deleteEventSchema(@RequestParam("id") Long id) {
        eventSchemaService.deleteEventSchema(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得事件 Schema 详情")
    @Parameter(name = "id", description = "编号", required = true, example = "1")
    @PreAuthorize("@ss.hasPermission('risk:event-schema:get')")
    public CommonResult<EventSchemaRespVO> getEventSchema(@RequestParam("id") Long id) {
        EventSchemaDO eventSchema = eventSchemaService.getEventSchema(id);
        return success(BeanUtils.toBean(eventSchema, EventSchemaRespVO.class));
    }

    @GetMapping("/page")
    @Operation(summary = "获得事件 Schema 分页")
    @PreAuthorize("@ss.hasPermission('risk:event-schema:query')")
    public CommonResult<PageResult<EventSchemaRespVO>> getEventSchemaPage(@Valid EventSchemaPageReqVO pageReqVO) {
        PageResult<EventSchemaDO> pageResult = eventSchemaService.getEventSchemaPage(pageReqVO);
        return success(BeanUtils.toBean(pageResult, EventSchemaRespVO.class));
    }

}
