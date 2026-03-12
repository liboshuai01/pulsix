package cn.liboshuai.pulsix.module.risk.controller.admin.policy.vo;

import cn.liboshuai.pulsix.framework.common.enums.CommonStatusEnum;
import cn.liboshuai.pulsix.framework.common.pojo.PageParam;
import cn.liboshuai.pulsix.framework.common.validation.InEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Schema(description = "管理后台 - 策略分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
public class PolicyPageReqVO extends PageParam {

    @Schema(description = "所属场景编码", example = "TRADE_RISK")
    private String sceneCode;

    @Schema(description = "策略编码，模糊匹配", example = "TRADE_RISK_POLICY")
    private String policyCode;

    @Schema(description = "策略名称，模糊匹配", example = "交易风控主策略")
    private String policyName;

    @Schema(description = "状态", example = "0")
    @InEnum(value = CommonStatusEnum.class, message = "状态必须是 {value}")
    private Integer status;

}
