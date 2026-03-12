package cn.liboshuai.pulsix.module.risk.controller.admin.rule.vo;

import cn.liboshuai.pulsix.framework.common.enums.CommonStatusEnum;
import cn.liboshuai.pulsix.framework.common.pojo.PageParam;
import cn.liboshuai.pulsix.framework.common.validation.InEnum;
import cn.liboshuai.pulsix.module.risk.enums.rule.RiskRuleHitActionEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Schema(description = "管理后台 - 规则分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
public class RulePageReqVO extends PageParam {

    @Schema(description = "所属场景编码", example = "TRADE_RISK")
    private String sceneCode;

    @Schema(description = "规则编码，模糊匹配", example = "R001")
    private String ruleCode;

    @Schema(description = "规则名称，模糊匹配", example = "黑名单设备直接拒绝")
    private String ruleName;

    @Schema(description = "命中动作", example = "REJECT")
    @InEnum(value = RiskRuleHitActionEnum.class, message = "命中动作必须是 {value}")
    private String hitAction;

    @Schema(description = "状态", example = "0")
    @InEnum(value = CommonStatusEnum.class, message = "状态必须是 {value}")
    private Integer status;

}
