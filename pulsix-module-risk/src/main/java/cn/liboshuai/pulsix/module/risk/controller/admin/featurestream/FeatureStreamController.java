package cn.liboshuai.pulsix.module.risk.controller.admin.featurestream;

import cn.liboshuai.pulsix.framework.common.pojo.CommonResult;
import cn.liboshuai.pulsix.framework.common.pojo.PageResult;
import cn.liboshuai.pulsix.module.risk.controller.admin.featurestream.vo.EntityTypeRespVO;
import cn.liboshuai.pulsix.module.risk.controller.admin.featurestream.vo.FeatureStreamPageReqVO;
import cn.liboshuai.pulsix.module.risk.controller.admin.featurestream.vo.FeatureStreamRespVO;
import cn.liboshuai.pulsix.module.risk.controller.admin.featurestream.vo.FeatureStreamSaveReqVO;
import cn.liboshuai.pulsix.module.risk.service.featurestream.FeatureStreamService;
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

@Tag(name = "管理后台 - 流式特征")
@RestController
@RequestMapping("/risk/feature-stream")
@Validated
public class FeatureStreamController {

    @Resource
    private FeatureStreamService featureStreamService;

    @PostMapping("/create")
    @Operation(summary = "创建流式特征")
    @PreAuthorize("@ss.hasPermission('risk:feature-stream:create')")
    public CommonResult<Long> createFeatureStream(@Valid @RequestBody FeatureStreamSaveReqVO createReqVO) {
        return success(featureStreamService.createFeatureStream(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "修改流式特征")
    @PreAuthorize("@ss.hasPermission('risk:feature-stream:update')")
    public CommonResult<Boolean> updateFeatureStream(@Valid @RequestBody FeatureStreamSaveReqVO updateReqVO) {
        featureStreamService.updateFeatureStream(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除流式特征")
    @Parameter(name = "id", description = "编号", required = true, example = "1")
    @PreAuthorize("@ss.hasPermission('risk:feature-stream:delete')")
    public CommonResult<Boolean> deleteFeatureStream(@RequestParam("id") Long id) {
        featureStreamService.deleteFeatureStream(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得流式特征详情")
    @Parameter(name = "id", description = "编号", required = true, example = "1")
    @PreAuthorize("@ss.hasPermission('risk:feature-stream:get')")
    public CommonResult<FeatureStreamRespVO> getFeatureStream(@RequestParam("id") Long id) {
        return success(featureStreamService.getFeatureStream(id));
    }

    @GetMapping("/page")
    @Operation(summary = "获得流式特征分页")
    @PreAuthorize("@ss.hasPermission('risk:feature-stream:query')")
    public CommonResult<PageResult<FeatureStreamRespVO>> getFeatureStreamPage(@Valid FeatureStreamPageReqVO pageReqVO) {
        return success(featureStreamService.getFeatureStreamPage(pageReqVO));
    }

    @GetMapping("/entity-type-list")
    @Operation(summary = "获得实体类型列表")
    @PreAuthorize("@ss.hasPermission('risk:feature-stream:query')")
    public CommonResult<List<EntityTypeRespVO>> getEntityTypeList() {
        return success(featureStreamService.getEntityTypeList());
    }

}
