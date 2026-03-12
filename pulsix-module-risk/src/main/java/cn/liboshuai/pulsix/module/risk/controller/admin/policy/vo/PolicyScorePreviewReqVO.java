package cn.liboshuai.pulsix.module.risk.controller.admin.policy.vo;

import cn.liboshuai.pulsix.framework.common.validation.InEnum;
import cn.liboshuai.pulsix.module.risk.enums.policy.RiskPolicyDecisionModeEnum;
import cn.liboshuai.pulsix.module.risk.enums.rule.RiskRuleHitActionEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Schema(description = "管理后台 - 策略评分卡预览 Request VO")
@Data
public class PolicyScorePreviewReqVO {

    @Schema(description = "决策模式", requiredMode = Schema.RequiredMode.REQUIRED, example = "SCORE_CARD")
    @NotBlank(message = "决策模式不能为空")
    @InEnum(value = RiskPolicyDecisionModeEnum.class, message = "决策模式必须是 {value}")
    private String decisionMode;

    @Schema(description = "默认动作", requiredMode = Schema.RequiredMode.REQUIRED, example = "PASS")
    @NotBlank(message = "默认动作不能为空")
    @InEnum(value = RiskRuleHitActionEnum.class, message = "默认动作必须是 {value}")
    private String defaultAction;

    @Schema(description = "总分", requiredMode = Schema.RequiredMode.REQUIRED, example = "95")
    @NotNull(message = "总分不能为空")
    @Min(value = 0, message = "总分不能小于 0")
    private Integer totalScore;

    @Schema(description = "评分段列表")
    @Valid
    private List<PolicyScoreBandSaveReqVO> scoreBands;

}
