package cn.liboshuai.pulsix.module.risk.controller.admin.rule.vo;

import cn.liboshuai.pulsix.framework.common.enums.CommonStatusEnum;
import cn.liboshuai.pulsix.framework.common.validation.InEnum;
import cn.liboshuai.pulsix.module.risk.enums.rule.RiskRuleEngineTypeEnum;
import cn.liboshuai.pulsix.module.risk.enums.rule.RiskRuleHitActionEnum;
import cn.liboshuai.pulsix.module.risk.enums.rule.RiskRuleTypeEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Schema(description = "管理后台 - 规则创建/修改 Request VO")
@Data
public class RuleSaveReqVO {

    @Schema(description = "主键", example = "1")
    private Long id;

    @Schema(description = "所属场景编码", requiredMode = Schema.RequiredMode.REQUIRED, example = "TRADE_RISK")
    @NotBlank(message = "所属场景编码不能为空")
    @Size(max = 64, message = "所属场景编码长度不能超过 64 个字符")
    private String sceneCode;

    @Schema(description = "规则编码", requiredMode = Schema.RequiredMode.REQUIRED, example = "R001")
    @NotBlank(message = "规则编码不能为空")
    @Size(max = 64, message = "规则编码长度不能超过 64 个字符")
    @Pattern(regexp = "^[A-Za-z][A-Za-z0-9_]*$", message = "规则编码只允许字母、数字、下划线，且必须以字母开头")
    private String ruleCode;

    @Schema(description = "规则名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "黑名单设备直接拒绝")
    @NotBlank(message = "规则名称不能为空")
    @Size(max = 128, message = "规则名称长度不能超过 128 个字符")
    private String ruleName;

    @Schema(description = "规则类型", requiredMode = Schema.RequiredMode.REQUIRED, example = "NORMAL")
    @NotBlank(message = "规则类型不能为空")
    @InEnum(value = RiskRuleTypeEnum.class, message = "规则类型必须是 {value}")
    private String ruleType;

    @Schema(description = "表达式引擎类型", requiredMode = Schema.RequiredMode.REQUIRED, example = "AVIATOR")
    @NotBlank(message = "表达式引擎类型不能为空")
    @InEnum(value = RiskRuleEngineTypeEnum.class, message = "表达式引擎类型必须是 {value}")
    private String engineType;

    @Schema(description = "规则表达式", requiredMode = Schema.RequiredMode.REQUIRED, example = "device_in_blacklist == true")
    @NotBlank(message = "规则表达式不能为空")
    private String exprContent;

    @Schema(description = "规则优先级", requiredMode = Schema.RequiredMode.REQUIRED, example = "100")
    @NotNull(message = "规则优先级不能为空")
    @Min(value = 0, message = "规则优先级不能小于 0")
    private Integer priority;

    @Schema(description = "命中动作", requiredMode = Schema.RequiredMode.REQUIRED, example = "REJECT")
    @NotBlank(message = "命中动作不能为空")
    @InEnum(value = RiskRuleHitActionEnum.class, message = "命中动作必须是 {value}")
    private String hitAction;

    @Schema(description = "风险分值", example = "100")
    @NotNull(message = "风险分值不能为空")
    @Min(value = 0, message = "风险分值不能小于 0")
    private Integer riskScore;

    @Schema(description = "命中原因模板", example = "设备命中黑名单")
    @Size(max = 1024, message = "命中原因模板长度不能超过 1024 个字符")
    private String hitReasonTemplate;

    @Schema(description = "状态", requiredMode = Schema.RequiredMode.REQUIRED, example = "0")
    @NotNull(message = "状态不能为空")
    @InEnum(value = CommonStatusEnum.class, message = "状态必须是 {value}")
    private Integer status;

    @Schema(description = "规则说明")
    @Size(max = 512, message = "规则说明长度不能超过 512 个字符")
    private String description;

}
