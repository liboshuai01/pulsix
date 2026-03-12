package cn.liboshuai.pulsix.module.risk.controller.admin.rule;

import cn.liboshuai.pulsix.framework.common.pojo.CommonResult;
import cn.liboshuai.pulsix.framework.common.pojo.PageResult;
import cn.liboshuai.pulsix.module.risk.controller.admin.rule.vo.RulePageReqVO;
import cn.liboshuai.pulsix.module.risk.controller.admin.rule.vo.RuleRespVO;
import cn.liboshuai.pulsix.module.risk.controller.admin.rule.vo.RuleSaveReqVO;
import cn.liboshuai.pulsix.module.risk.controller.admin.rule.vo.RuleValidateReqVO;
import cn.liboshuai.pulsix.module.risk.controller.admin.rule.vo.RuleValidateRespVO;
import cn.liboshuai.pulsix.module.risk.service.rule.RuleService;
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

@Tag(name = "管理后台 - 规则中心")
@RestController
@RequestMapping("/risk/rule")
@Validated
public class RuleController {

    @Resource
    private RuleService ruleService;

    @PostMapping("/create")
    @Operation(summary = "创建规则")
    @PreAuthorize("@ss.hasPermission('risk:rule:create')")
    public CommonResult<Long> createRule(@Valid @RequestBody RuleSaveReqVO createReqVO) {
        return success(ruleService.createRule(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "修改规则")
    @PreAuthorize("@ss.hasPermission('risk:rule:update')")
    public CommonResult<Boolean> updateRule(@Valid @RequestBody RuleSaveReqVO updateReqVO) {
        ruleService.updateRule(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除规则")
    @Parameter(name = "id", description = "编号", required = true, example = "1")
    @PreAuthorize("@ss.hasPermission('risk:rule:delete')")
    public CommonResult<Boolean> deleteRule(@RequestParam("id") Long id) {
        ruleService.deleteRule(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得规则详情")
    @Parameter(name = "id", description = "编号", required = true, example = "1")
    @PreAuthorize("@ss.hasPermission('risk:rule:query')")
    public CommonResult<RuleRespVO> getRule(@RequestParam("id") Long id) {
        return success(ruleService.getRule(id));
    }

    @GetMapping("/page")
    @Operation(summary = "获得规则分页")
    @PreAuthorize("@ss.hasPermission('risk:rule:query')")
    public CommonResult<PageResult<RuleRespVO>> getRulePage(@Valid RulePageReqVO pageReqVO) {
        return success(ruleService.getRulePage(pageReqVO));
    }

    @PostMapping("/validate")
    @Operation(summary = "校验规则表达式")
    @PreAuthorize("@ss.hasPermission('risk:rule:validate')")
    public CommonResult<RuleValidateRespVO> validateRule(@Valid @RequestBody RuleValidateReqVO reqVO) {
        return success(ruleService.validateRule(reqVO));
    }

}
