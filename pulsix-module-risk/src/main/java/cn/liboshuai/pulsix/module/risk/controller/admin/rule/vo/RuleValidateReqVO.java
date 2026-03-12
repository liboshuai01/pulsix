package cn.liboshuai.pulsix.module.risk.controller.admin.rule.vo;

import cn.liboshuai.pulsix.framework.common.validation.InEnum;
import cn.liboshuai.pulsix.module.risk.enums.rule.RiskRuleEngineTypeEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Schema(description = "管理后台 - 规则表达式校验 Request VO")
@Data
public class RuleValidateReqVO {

    @Schema(description = "所属场景编码", requiredMode = Schema.RequiredMode.REQUIRED, example = "TRADE_RISK")
    @NotBlank(message = "所属场景编码不能为空")
    private String sceneCode;

    @Schema(description = "表达式引擎类型", requiredMode = Schema.RequiredMode.REQUIRED, example = "AVIATOR")
    @NotBlank(message = "表达式引擎类型不能为空")
    @InEnum(value = RiskRuleEngineTypeEnum.class, message = "表达式引擎类型必须是 {value}")
    private String engineType;

    @Schema(description = "规则表达式", requiredMode = Schema.RequiredMode.REQUIRED, example = "device_in_blacklist == true")
    @NotBlank(message = "规则表达式不能为空")
    private String exprContent;

    @Schema(description = "命中原因模板", example = "设备命中黑名单")
    private String hitReasonTemplate;

}
