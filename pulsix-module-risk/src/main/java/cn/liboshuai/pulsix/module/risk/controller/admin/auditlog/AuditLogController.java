package cn.liboshuai.pulsix.module.risk.controller.admin.auditlog;

import cn.liboshuai.pulsix.framework.common.pojo.CommonResult;
import cn.liboshuai.pulsix.framework.common.pojo.PageResult;
import cn.liboshuai.pulsix.module.risk.controller.admin.auditlog.vo.AuditLogDetailRespVO;
import cn.liboshuai.pulsix.module.risk.controller.admin.auditlog.vo.AuditLogPageReqVO;
import cn.liboshuai.pulsix.module.risk.controller.admin.auditlog.vo.AuditLogRespVO;
import cn.liboshuai.pulsix.module.risk.service.auditlog.AuditLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import static cn.liboshuai.pulsix.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - 审计日志")
@RestController
@RequestMapping("/risk/audit-log")
@Validated
public class AuditLogController {

    @Resource
    private AuditLogService auditLogService;

    @GetMapping("/page")
    @Operation(summary = "获得审计日志分页")
    @PreAuthorize("@ss.hasPermission('risk:audit-log:query')")
    public CommonResult<PageResult<AuditLogRespVO>> getAuditLogPage(@Valid AuditLogPageReqVO pageReqVO) {
        return success(auditLogService.getAuditLogPage(pageReqVO));
    }

    @GetMapping("/get")
    @Operation(summary = "获得审计日志详情")
    @Parameter(name = "id", description = "编号", required = true, example = "10201")
    @PreAuthorize("@ss.hasPermission('risk:audit-log:get')")
    public CommonResult<AuditLogDetailRespVO> getAuditLog(@RequestParam("id") Long id) {
        return success(auditLogService.getAuditLog(id));
    }

}
