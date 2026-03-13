package cn.liboshuai.pulsix.module.risk.controller.admin.auditlog;

import cn.liboshuai.pulsix.framework.apilog.core.annotation.ApiAccessLog;
import cn.liboshuai.pulsix.framework.common.pojo.CommonResult;
import cn.liboshuai.pulsix.framework.common.pojo.PageParam;
import cn.liboshuai.pulsix.framework.common.pojo.PageResult;
import cn.liboshuai.pulsix.framework.common.util.object.BeanUtils;
import cn.liboshuai.pulsix.framework.excel.core.util.ExcelUtils;
import cn.liboshuai.pulsix.module.risk.controller.admin.auditlog.vo.AuditLogDetailRespVO;
import cn.liboshuai.pulsix.module.risk.controller.admin.auditlog.vo.AuditLogExportRespVO;
import cn.liboshuai.pulsix.module.risk.controller.admin.auditlog.vo.AuditLogPageReqVO;
import cn.liboshuai.pulsix.module.risk.controller.admin.auditlog.vo.AuditLogRespVO;
import cn.liboshuai.pulsix.module.risk.service.auditlog.AuditLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.List;

import static cn.liboshuai.pulsix.framework.apilog.core.enums.OperateTypeEnum.EXPORT;
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

    @GetMapping("/export-excel")
    @Operation(summary = "导出审计日志 Excel")
    @PreAuthorize("@ss.hasPermission('risk:audit-log:export')")
    @ApiAccessLog(operateType = EXPORT)
    public void exportAuditLogExcel(@Valid AuditLogPageReqVO pageReqVO,
                                    HttpServletResponse response) throws IOException {
        pageReqVO.setPageSize(PageParam.PAGE_SIZE_NONE);
        List<AuditLogRespVO> list = auditLogService.getAuditLogPage(pageReqVO).getList();
        ExcelUtils.write(response, "审计日志.xls", "数据", AuditLogExportRespVO.class,
                BeanUtils.toBean(list, AuditLogExportRespVO.class));
    }

    @GetMapping("/get")
    @Operation(summary = "获得审计日志详情")
    @Parameter(name = "id", description = "编号", required = true, example = "10201")
    @PreAuthorize("@ss.hasPermission('risk:audit-log:get')")
    public CommonResult<AuditLogDetailRespVO> getAuditLog(@RequestParam("id") Long id) {
        return success(auditLogService.getAuditLog(id));
    }

}
