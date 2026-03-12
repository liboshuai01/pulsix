package cn.liboshuai.pulsix.module.risk.controller.admin.featurederived;

import cn.liboshuai.pulsix.framework.common.pojo.CommonResult;
import cn.liboshuai.pulsix.framework.common.pojo.PageResult;
import cn.liboshuai.pulsix.module.risk.controller.admin.featurederived.vo.FeatureDerivedDependencyOptionRespVO;
import cn.liboshuai.pulsix.module.risk.controller.admin.featurederived.vo.FeatureDerivedPageReqVO;
import cn.liboshuai.pulsix.module.risk.controller.admin.featurederived.vo.FeatureDerivedRespVO;
import cn.liboshuai.pulsix.module.risk.controller.admin.featurederived.vo.FeatureDerivedSaveReqVO;
import cn.liboshuai.pulsix.module.risk.controller.admin.featurederived.vo.FeatureDerivedValidateReqVO;
import cn.liboshuai.pulsix.module.risk.controller.admin.featurederived.vo.FeatureDerivedValidateRespVO;
import cn.liboshuai.pulsix.module.risk.service.featurederived.FeatureDerivedService;
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

import java.util.List;

import static cn.liboshuai.pulsix.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - 派生特征")
@RestController
@RequestMapping("/risk/feature-derived")
@Validated
public class FeatureDerivedController {

    @Resource
    private FeatureDerivedService featureDerivedService;

    @PostMapping("/create")
    @Operation(summary = "创建派生特征")
    @PreAuthorize("@ss.hasPermission('risk:feature-derived:create')")
    public CommonResult<Long> createFeatureDerived(@Valid @RequestBody FeatureDerivedSaveReqVO createReqVO) {
        return success(featureDerivedService.createFeatureDerived(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "修改派生特征")
    @PreAuthorize("@ss.hasPermission('risk:feature-derived:update')")
    public CommonResult<Boolean> updateFeatureDerived(@Valid @RequestBody FeatureDerivedSaveReqVO updateReqVO) {
        featureDerivedService.updateFeatureDerived(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除派生特征")
    @Parameter(name = "id", description = "编号", required = true, example = "1")
    @PreAuthorize("@ss.hasPermission('risk:feature-derived:delete')")
    public CommonResult<Boolean> deleteFeatureDerived(@RequestParam("id") Long id) {
        featureDerivedService.deleteFeatureDerived(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得派生特征详情")
    @Parameter(name = "id", description = "编号", required = true, example = "1")
    @PreAuthorize("@ss.hasPermission('risk:feature-derived:query')")
    public CommonResult<FeatureDerivedRespVO> getFeatureDerived(@RequestParam("id") Long id) {
        return success(featureDerivedService.getFeatureDerived(id));
    }

    @GetMapping("/page")
    @Operation(summary = "获得派生特征分页")
    @PreAuthorize("@ss.hasPermission('risk:feature-derived:query')")
    public CommonResult<PageResult<FeatureDerivedRespVO>> getFeatureDerivedPage(@Valid FeatureDerivedPageReqVO pageReqVO) {
        return success(featureDerivedService.getFeatureDerivedPage(pageReqVO));
    }

    @GetMapping("/dependency-options")
    @Operation(summary = "获得派生特征依赖候选项")
    @PreAuthorize("@ss.hasPermission('risk:feature-derived:query')")
    public CommonResult<List<FeatureDerivedDependencyOptionRespVO>> getDependencyOptions(
            @RequestParam("sceneCode") String sceneCode,
            @RequestParam(value = "currentFeatureCode", required = false) String currentFeatureCode) {
        return success(featureDerivedService.getDependencyOptions(sceneCode, currentFeatureCode));
    }

    @PostMapping("/validate")
    @Operation(summary = "校验派生特征表达式")
    @PreAuthorize("@ss.hasPermission('risk:feature-derived:validate')")
    public CommonResult<FeatureDerivedValidateRespVO> validateExpression(@Valid @RequestBody FeatureDerivedValidateReqVO reqVO) {
        return success(featureDerivedService.validateExpression(reqVO));
    }

}
