package cn.liboshuai.pulsix.module.risk.controller.admin.policy.vo;

import cn.liboshuai.pulsix.framework.common.validation.InEnum;
import cn.liboshuai.pulsix.module.risk.enums.rule.RiskRuleHitActionEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Schema(description = "管理后台 - 策略评分段保存 Request VO")
@Data
public class PolicyScoreBandSaveReqVO {

    @Schema(description = "最小分值（含）", requiredMode = Schema.RequiredMode.REQUIRED, example = "0")
    @NotNull(message = "最小分值不能为空")
    @Min(value = 0, message = "最小分值不能小于 0")
    private Integer minScore;

    @Schema(description = "最大分值（含）", requiredMode = Schema.RequiredMode.REQUIRED, example = "59")
    @NotNull(message = "最大分值不能为空")
    @Min(value = 0, message = "最大分值不能小于 0")
    private Integer maxScore;

    @Schema(description = "命中动作", requiredMode = Schema.RequiredMode.REQUIRED, example = "PASS")
    @NotBlank(message = "命中动作不能为空")
    @InEnum(value = RiskRuleHitActionEnum.class, message = "命中动作必须是 {value}")
    private String hitAction;

    @Schema(description = "命中原因模板", example = "累计分值 {totalScore}，低风险放行")
    @Size(max = 1024, message = "命中原因模板长度不能超过 1024 个字符")
    private String hitReasonTemplate;

}
