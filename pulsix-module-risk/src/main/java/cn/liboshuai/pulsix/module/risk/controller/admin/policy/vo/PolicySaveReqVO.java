package cn.liboshuai.pulsix.module.risk.controller.admin.policy.vo;

import cn.liboshuai.pulsix.framework.common.enums.CommonStatusEnum;
import cn.liboshuai.pulsix.framework.common.validation.InEnum;
import cn.liboshuai.pulsix.module.risk.enums.rule.RiskRuleHitActionEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Schema(description = "管理后台 - 策略创建/修改 Request VO")
@Data
public class PolicySaveReqVO {

    @Schema(description = "主键", example = "1")
    private Long id;

    @Schema(description = "所属场景编码", requiredMode = Schema.RequiredMode.REQUIRED, example = "TRADE_RISK")
    @NotBlank(message = "所属场景编码不能为空")
    @Size(max = 64, message = "所属场景编码长度不能超过 64 个字符")
    private String sceneCode;

    @Schema(description = "策略编码", requiredMode = Schema.RequiredMode.REQUIRED, example = "TRADE_RISK_POLICY")
    @NotBlank(message = "策略编码不能为空")
    @Size(max = 64, message = "策略编码长度不能超过 64 个字符")
    @Pattern(regexp = "^[A-Za-z][A-Za-z0-9_]*$", message = "策略编码只允许字母、数字、下划线，且必须以字母开头")
    private String policyCode;

    @Schema(description = "策略名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "交易风控主策略")
    @NotBlank(message = "策略名称不能为空")
    @Size(max = 128, message = "策略名称长度不能超过 128 个字符")
    private String policyName;

    @Schema(description = "默认动作", requiredMode = Schema.RequiredMode.REQUIRED, example = "PASS")
    @NotBlank(message = "默认动作不能为空")
    @InEnum(value = RiskRuleHitActionEnum.class, message = "默认动作必须是 {value}")
    private String defaultAction;

    @Schema(description = "规则编码顺序列表", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty(message = "规则顺序不能为空")
    private List<String> ruleCodes;

    @Schema(description = "状态", requiredMode = Schema.RequiredMode.REQUIRED, example = "0")
    @NotNull(message = "状态不能为空")
    @InEnum(value = CommonStatusEnum.class, message = "状态必须是 {value}")
    private Integer status;

    @Schema(description = "策略说明")
    @Size(max = 512, message = "策略说明长度不能超过 512 个字符")
    private String description;

}
