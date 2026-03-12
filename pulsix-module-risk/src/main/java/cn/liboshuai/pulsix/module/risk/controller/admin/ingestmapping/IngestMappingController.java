package cn.liboshuai.pulsix.module.risk.controller.admin.ingestmapping;

import cn.liboshuai.pulsix.framework.common.pojo.CommonResult;
import cn.liboshuai.pulsix.framework.common.pojo.PageResult;
import cn.liboshuai.pulsix.framework.common.util.object.BeanUtils;
import cn.liboshuai.pulsix.module.risk.controller.admin.ingestmapping.vo.IngestMappingPageReqVO;
import cn.liboshuai.pulsix.module.risk.controller.admin.ingestmapping.vo.IngestMappingPreviewReqVO;
import cn.liboshuai.pulsix.module.risk.controller.admin.ingestmapping.vo.IngestMappingPreviewRespVO;
import cn.liboshuai.pulsix.module.risk.controller.admin.ingestmapping.vo.IngestMappingRespVO;
import cn.liboshuai.pulsix.module.risk.controller.admin.ingestmapping.vo.IngestMappingSaveReqVO;
import cn.liboshuai.pulsix.module.risk.dal.dataobject.ingestmapping.IngestMappingDO;
import cn.liboshuai.pulsix.module.risk.service.ingestmapping.IngestMappingService;
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

@Tag(name = "管理后台 - 接入字段映射")
@RestController
@RequestMapping("/risk/ingest-mapping")
@Validated
public class IngestMappingController {

    @Resource
    private IngestMappingService ingestMappingService;

    @PostMapping("/create")
    @Operation(summary = "创建接入字段映射")
    @PreAuthorize("@ss.hasPermission('risk:ingest-mapping:create')")
    public CommonResult<Long> createIngestMapping(@Valid @RequestBody IngestMappingSaveReqVO createReqVO) {
        return success(ingestMappingService.createIngestMapping(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "修改接入字段映射")
    @PreAuthorize("@ss.hasPermission('risk:ingest-mapping:update')")
    public CommonResult<Boolean> updateIngestMapping(@Valid @RequestBody IngestMappingSaveReqVO updateReqVO) {
        ingestMappingService.updateIngestMapping(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除接入字段映射")
    @Parameter(name = "id", description = "编号", required = true, example = "1")
    @PreAuthorize("@ss.hasPermission('risk:ingest-mapping:delete')")
    public CommonResult<Boolean> deleteIngestMapping(@RequestParam("id") Long id) {
        ingestMappingService.deleteIngestMapping(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得接入字段映射详情")
    @Parameter(name = "id", description = "编号", required = true, example = "1")
    @PreAuthorize("@ss.hasPermission('risk:ingest-mapping:query')")
    public CommonResult<IngestMappingRespVO> getIngestMapping(@RequestParam("id") Long id) {
        IngestMappingDO ingestMapping = ingestMappingService.getIngestMapping(id);
        return success(BeanUtils.toBean(ingestMapping, IngestMappingRespVO.class));
    }

    @GetMapping("/page")
    @Operation(summary = "获得接入字段映射分页")
    @PreAuthorize("@ss.hasPermission('risk:ingest-mapping:query')")
    public CommonResult<PageResult<IngestMappingRespVO>> getIngestMappingPage(@Valid IngestMappingPageReqVO pageReqVO) {
        PageResult<IngestMappingDO> pageResult = ingestMappingService.getIngestMappingPage(pageReqVO);
        return success(BeanUtils.toBean(pageResult, IngestMappingRespVO.class));
    }

    @PostMapping("/preview")
    @Operation(summary = "预览接入映射结果")
    @PreAuthorize("@ss.hasPermission('risk:ingest-mapping:preview')")
    public CommonResult<IngestMappingPreviewRespVO> previewIngestMapping(@Valid @RequestBody IngestMappingPreviewReqVO reqVO) {
        return success(ingestMappingService.previewIngestMapping(reqVO));
    }

}
