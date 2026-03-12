package cn.liboshuai.pulsix.module.risk.controller.admin.ingestsource;

import cn.liboshuai.pulsix.framework.common.pojo.CommonResult;
import cn.liboshuai.pulsix.framework.common.pojo.PageResult;
import cn.liboshuai.pulsix.framework.common.util.object.BeanUtils;
import cn.liboshuai.pulsix.module.risk.controller.admin.ingestsource.vo.IngestSourcePageReqVO;
import cn.liboshuai.pulsix.module.risk.controller.admin.ingestsource.vo.IngestSourceRespVO;
import cn.liboshuai.pulsix.module.risk.controller.admin.ingestsource.vo.IngestSourceSaveReqVO;
import cn.liboshuai.pulsix.module.risk.controller.admin.ingestsource.vo.IngestSourceUpdateStatusReqVO;
import cn.liboshuai.pulsix.module.risk.dal.dataobject.ingestsource.IngestSourceDO;
import cn.liboshuai.pulsix.module.risk.service.ingestsource.IngestSourceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import static cn.liboshuai.pulsix.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - 接入源")
@RestController
@RequestMapping("/risk/ingest-source")
@Validated
public class IngestSourceController {

    @Resource
    private IngestSourceService ingestSourceService;

    @PostMapping("/create")
    @Operation(summary = "创建接入源")
    @PreAuthorize("@ss.hasPermission('risk:ingest-source:create')")
    public CommonResult<Long> createIngestSource(@Valid @RequestBody IngestSourceSaveReqVO createReqVO) {
        return success(ingestSourceService.createIngestSource(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "修改接入源")
    @PreAuthorize("@ss.hasPermission('risk:ingest-source:update')")
    public CommonResult<Boolean> updateIngestSource(@Valid @RequestBody IngestSourceSaveReqVO updateReqVO) {
        ingestSourceService.updateIngestSource(updateReqVO);
        return success(true);
    }

    @PutMapping("/update-status")
    @Operation(summary = "修改接入源状态")
    @PreAuthorize("@ss.hasPermission('risk:ingest-source:update-status')")
    public CommonResult<Boolean> updateIngestSourceStatus(@Valid @RequestBody IngestSourceUpdateStatusReqVO reqVO) {
        ingestSourceService.updateIngestSourceStatus(reqVO.getId(), reqVO.getStatus());
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得接入源详情")
    @Parameter(name = "id", description = "编号", required = true, example = "1")
    @PreAuthorize("@ss.hasPermission('risk:ingest-source:get')")
    public CommonResult<IngestSourceRespVO> getIngestSource(@RequestParam("id") Long id) {
        IngestSourceDO ingestSource = ingestSourceService.getIngestSource(id);
        return success(BeanUtils.toBean(ingestSource, IngestSourceRespVO.class));
    }

    @GetMapping("/page")
    @Operation(summary = "获得接入源分页")
    @PreAuthorize("@ss.hasPermission('risk:ingest-source:query')")
    public CommonResult<PageResult<IngestSourceRespVO>> getIngestSourcePage(@Valid IngestSourcePageReqVO pageReqVO) {
        PageResult<IngestSourceDO> pageResult = ingestSourceService.getIngestSourcePage(pageReqVO);
        return success(BeanUtils.toBean(pageResult, IngestSourceRespVO.class));
    }

}
