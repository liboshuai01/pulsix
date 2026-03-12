package cn.liboshuai.pulsix.module.risk.controller.admin.policy;

import cn.liboshuai.pulsix.framework.common.pojo.CommonResult;
import cn.liboshuai.pulsix.framework.common.pojo.PageResult;
import cn.liboshuai.pulsix.module.risk.controller.admin.policy.vo.PolicyPageReqVO;
import cn.liboshuai.pulsix.module.risk.controller.admin.policy.vo.PolicyRespVO;
import cn.liboshuai.pulsix.module.risk.controller.admin.policy.vo.PolicyRuleOptionRespVO;
import cn.liboshuai.pulsix.module.risk.controller.admin.policy.vo.PolicySaveReqVO;
import cn.liboshuai.pulsix.module.risk.controller.admin.policy.vo.PolicySortReqVO;
import cn.liboshuai.pulsix.module.risk.service.policy.PolicyService;
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

@Tag(name = "管理后台 - 策略中心")
@RestController
@RequestMapping("/risk/policy")
@Validated
public class PolicyController {

    @Resource
    private PolicyService policyService;

    @PostMapping("/create")
    @Operation(summary = "创建策略")
    @PreAuthorize("@ss.hasPermission('risk:policy:create')")
    public CommonResult<Long> createPolicy(@Valid @RequestBody PolicySaveReqVO createReqVO) {
        return success(policyService.createPolicy(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "修改策略")
    @PreAuthorize("@ss.hasPermission('risk:policy:update')")
    public CommonResult<Boolean> updatePolicy(@Valid @RequestBody PolicySaveReqVO updateReqVO) {
        policyService.updatePolicy(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除策略")
    @Parameter(name = "id", description = "编号", required = true, example = "1")
    @PreAuthorize("@ss.hasPermission('risk:policy:delete')")
    public CommonResult<Boolean> deletePolicy(@RequestParam("id") Long id) {
        policyService.deletePolicy(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得策略详情")
    @Parameter(name = "id", description = "编号", required = true, example = "1")
    @PreAuthorize("@ss.hasPermission('risk:policy:query')")
    public CommonResult<PolicyRespVO> getPolicy(@RequestParam("id") Long id) {
        return success(policyService.getPolicy(id));
    }

    @GetMapping("/page")
    @Operation(summary = "获得策略分页")
    @PreAuthorize("@ss.hasPermission('risk:policy:query')")
    public CommonResult<PageResult<PolicyRespVO>> getPolicyPage(@Valid PolicyPageReqVO pageReqVO) {
        return success(policyService.getPolicyPage(pageReqVO));
    }

    @GetMapping("/rule-options")
    @Operation(summary = "获得策略可选规则")
    @PreAuthorize("@ss.hasPermission('risk:policy:query')")
    public CommonResult<List<PolicyRuleOptionRespVO>> getRuleOptions(@RequestParam("sceneCode") String sceneCode) {
        return success(policyService.getRuleOptions(sceneCode));
    }

    @PostMapping("/sort-rules")
    @Operation(summary = "保存策略规则排序")
    @PreAuthorize("@ss.hasPermission('risk:policy:sort')")
    public CommonResult<Boolean> sortPolicyRules(@Valid @RequestBody PolicySortReqVO reqVO) {
        policyService.sortPolicyRules(reqVO);
        return success(true);
    }

}
