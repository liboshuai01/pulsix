package cn.liboshuai.pulsix.module.risk.controller.admin.ingesterror;

import cn.liboshuai.pulsix.framework.apilog.core.annotation.ApiAccessLog;
import cn.liboshuai.pulsix.framework.common.pojo.CommonResult;
import cn.liboshuai.pulsix.framework.common.pojo.PageParam;
import cn.liboshuai.pulsix.framework.common.pojo.PageResult;
import cn.liboshuai.pulsix.framework.common.util.object.BeanUtils;
import cn.liboshuai.pulsix.framework.excel.core.util.ExcelUtils;
import cn.liboshuai.pulsix.module.risk.controller.admin.ingesterror.vo.IngestErrorDetailRespVO;
import cn.liboshuai.pulsix.module.risk.controller.admin.ingesterror.vo.IngestErrorExportRespVO;
import cn.liboshuai.pulsix.module.risk.controller.admin.ingesterror.vo.IngestErrorPageReqVO;
import cn.liboshuai.pulsix.module.risk.controller.admin.ingesterror.vo.IngestErrorRespVO;
import cn.liboshuai.pulsix.module.risk.service.ingesterror.IngestErrorService;
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

@Tag(name = "管理后台 - 接入异常")
@RestController
@RequestMapping("/risk/ingest-error")
@Validated
public class IngestErrorController {

    @Resource
    private IngestErrorService ingestErrorService;

    @GetMapping("/page")
    @Operation(summary = "获得接入异常分页")
    @PreAuthorize("@ss.hasPermission('risk:ingest-error:query')")
    public CommonResult<PageResult<IngestErrorRespVO>> getIngestErrorPage(@Valid IngestErrorPageReqVO pageReqVO) {
        return success(ingestErrorService.getIngestErrorPage(pageReqVO));
    }

    @GetMapping("/export-excel")
    @Operation(summary = "导出接入异常 Excel")
    @PreAuthorize("@ss.hasPermission('risk:ingest-error:export')")
    @ApiAccessLog(operateType = EXPORT)
    public void exportIngestErrorExcel(@Valid IngestErrorPageReqVO pageReqVO,
                                       HttpServletResponse response) throws IOException {
        pageReqVO.setPageSize(PageParam.PAGE_SIZE_NONE);
        List<IngestErrorRespVO> list = ingestErrorService.getIngestErrorPage(pageReqVO).getList();
        ExcelUtils.write(response, "接入异常.xls", "数据", IngestErrorExportRespVO.class,
                BeanUtils.toBean(list, IngestErrorExportRespVO.class));
    }

    @GetMapping("/get")
    @Operation(summary = "获得接入异常详情")
    @Parameter(name = "id", description = "编号", required = true, example = "8101")
    @PreAuthorize("@ss.hasPermission('risk:ingest-error:get')")
    public CommonResult<IngestErrorDetailRespVO> getIngestError(@RequestParam("id") Long id) {
        return success(ingestErrorService.getIngestError(id));
    }

}
