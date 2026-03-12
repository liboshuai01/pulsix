package cn.liboshuai.pulsix.module.risk.controller.admin.scene.vo;

import cn.liboshuai.pulsix.framework.common.enums.CommonStatusEnum;
import cn.liboshuai.pulsix.framework.common.validation.InEnum;
import cn.liboshuai.pulsix.module.risk.enums.scene.RiskSceneAccessModeEnum;
import cn.liboshuai.pulsix.module.risk.enums.scene.RiskSceneTypeEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Schema(description = "管理后台 - 风控场景创建/修改 Request VO")
@Data
public class SceneSaveReqVO {

    @Schema(description = "场景编号", example = "1")
    private Long id;

    @Schema(description = "场景编码", requiredMode = Schema.RequiredMode.REQUIRED, example = "TRADE_RISK")
    @NotBlank(message = "场景编码不能为空")
    @Size(max = 64, message = "场景编码长度不能超过 64 个字符")
    private String sceneCode;

    @Schema(description = "场景名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "交易风控")
    @NotBlank(message = "场景名称不能为空")
    @Size(max = 128, message = "场景名称长度不能超过 128 个字符")
    private String sceneName;

    @Schema(description = "场景类型", requiredMode = Schema.RequiredMode.REQUIRED, example = "TRADE_SECURITY")
    @NotBlank(message = "场景类型不能为空")
    @InEnum(value = RiskSceneTypeEnum.class, message = "场景类型必须是 {value}")
    private String sceneType;

    @Schema(description = "接入模式", requiredMode = Schema.RequiredMode.REQUIRED, example = "MIXED")
    @NotBlank(message = "接入模式不能为空")
    @InEnum(value = RiskSceneAccessModeEnum.class, message = "接入模式必须是 {value}")
    private String accessMode;

    @Schema(description = "默认事件编码", example = "TRADE_EVENT")
    @Size(max = 64, message = "默认事件编码长度不能超过 64 个字符")
    private String defaultEventCode;

    @Schema(description = "默认策略编码", example = "TRADE_RISK_POLICY")
    @Size(max = 64, message = "默认策略编码长度不能超过 64 个字符")
    private String defaultPolicyCode;

    @Schema(description = "标准事件 Topic", example = "pulsix.event.standard")
    @Size(max = 128, message = "标准事件 Topic 长度不能超过 128 个字符")
    private String standardTopicName;

    @Schema(description = "决策结果 Topic", example = "pulsix.decision.result")
    @Size(max = 128, message = "决策结果 Topic 长度不能超过 128 个字符")
    private String decisionTopicName;

    @Schema(description = "状态，见 CommonStatusEnum 枚举", requiredMode = Schema.RequiredMode.REQUIRED, example = "0")
    @NotNull(message = "状态不能为空")
    @InEnum(value = CommonStatusEnum.class, message = "状态必须是 {value}")
    private Integer status;

    @Schema(description = "场景说明")
    @Size(max = 512, message = "场景说明长度不能超过 512 个字符")
    private String description;

}

