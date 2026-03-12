package cn.liboshuai.pulsix.module.risk.controller.admin.policy.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "管理后台 - 策略规则引用 Response VO")
@Data
public class PolicyRuleRefRespVO {

    @Schema(description = "规则编码", example = "R001")
    private String ruleCode;

    @Schema(description = "规则名称", example = "黑名单设备直接拒绝")
    private String ruleName;

    @Schema(description = "排序号", example = "10")
    private Integer orderNo;

    @Schema(description = "命中动作", example = "REJECT")
    private String hitAction;

    @Schema(description = "规则优先级", example = "100")
    private Integer priority;

    @Schema(description = "规则状态", example = "0")
    private Integer status;

}
