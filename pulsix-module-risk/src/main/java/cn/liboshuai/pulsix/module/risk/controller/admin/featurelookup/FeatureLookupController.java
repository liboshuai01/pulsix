package cn.liboshuai.pulsix.module.risk.controller.admin.featurelookup;

import cn.liboshuai.pulsix.framework.common.pojo.CommonResult;
import cn.liboshuai.pulsix.framework.common.pojo.PageResult;
import cn.liboshuai.pulsix.module.risk.controller.admin.featurelookup.vo.FeatureLookupPageReqVO;
import cn.liboshuai.pulsix.module.risk.controller.admin.featurelookup.vo.FeatureLookupRespVO;
import cn.liboshuai.pulsix.module.risk.controller.admin.featurelookup.vo.FeatureLookupSaveReqVO;
import cn.liboshuai.pulsix.module.risk.service.featurelookup.FeatureLookupService;
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

@Tag(name = "管理后台 - 查询特征")
@RestController
@RequestMapping("/risk/feature-lookup")
@Validated
public class FeatureLookupController {

    @Resource
    private FeatureLookupService featureLookupService;

    @PostMapping("/create")
    @Operation(summary = "创建查询特征")
    @PreAuthorize("@ss.hasPermission('risk:feature-lookup:create')")
    public CommonResult<Long> createFeatureLookup(@Valid @RequestBody FeatureLookupSaveReqVO createReqVO) {
        return success(featureLookupService.createFeatureLookup(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "修改查询特征")
    @PreAuthorize("@ss.hasPermission('risk:feature-lookup:update')")
    public CommonResult<Boolean> updateFeatureLookup(@Valid @RequestBody FeatureLookupSaveReqVO updateReqVO) {
        featureLookupService.updateFeatureLookup(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除查询特征")
    @Parameter(name = "id", description = "编号", required = true, example = "1")
    @PreAuthorize("@ss.hasPermission('risk:feature-lookup:delete')")
    public CommonResult<Boolean> deleteFeatureLookup(@RequestParam("id") Long id) {
        featureLookupService.deleteFeatureLookup(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得查询特征详情")
    @Parameter(name = "id", description = "编号", required = true, example = "1")
    @PreAuthorize("@ss.hasPermission('risk:feature-lookup:get')")
    public CommonResult<FeatureLookupRespVO> getFeatureLookup(@RequestParam("id") Long id) {
        return success(featureLookupService.getFeatureLookup(id));
    }

    @GetMapping("/page")
    @Operation(summary = "获得查询特征分页")
    @PreAuthorize("@ss.hasPermission('risk:feature-lookup:query')")
    public CommonResult<PageResult<FeatureLookupRespVO>> getFeatureLookupPage(@Valid FeatureLookupPageReqVO pageReqVO) {
        return success(featureLookupService.getFeatureLookupPage(pageReqVO));
    }

}
