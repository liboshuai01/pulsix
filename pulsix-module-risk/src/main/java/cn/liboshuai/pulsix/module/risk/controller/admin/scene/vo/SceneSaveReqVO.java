package cn.liboshuai.pulsix.module.risk.controller.admin.scene.vo;

import cn.liboshuai.pulsix.framework.common.enums.CommonStatusEnum;
import cn.liboshuai.pulsix.framework.common.validation.InEnum;
import cn.liboshuai.pulsix.module.risk.enums.scene.SceneRuntimeModeEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Schema(description = "管理后台 - 风控场景创建/修改 Request VO")
@Data
public class SceneSaveReqVO {

    @Schema(description = "场景主键", example = "1")
    private Long id;

    @Schema(description = "场景编码", requiredMode = Schema.RequiredMode.REQUIRED, example = "TRADE_RISK")
    @NotBlank(message = "场景编码不能为空")
    @Size(max = 64, message = "场景编码长度不能超过 64 个字符")
    @Pattern(regexp = "^[A-Z0-9_]+$", message = "场景编码只能包含大写字母、数字和下划线")
    private String sceneCode;

    @Schema(description = "场景名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "交易风控")
    @NotBlank(message = "场景名称不能为空")
    @Size(max = 128, message = "场景名称长度不能超过 128 个字符")
    private String sceneName;

    @Schema(description = "运行模式", requiredMode = Schema.RequiredMode.REQUIRED, example = "ASYNC_DECISION")
    @NotBlank(message = "运行模式不能为空")
    @InEnum(value = SceneRuntimeModeEnum.class, message = "运行模式必须是 {value}")
    private String runtimeMode;

    @Schema(description = "默认策略编码", example = "TRADE_RISK_POLICY_FIRST_HIT")
    @Size(max = 64, message = "默认策略编码长度不能超过 64 个字符")
    private String defaultPolicyCode;

    @Schema(description = "状态", requiredMode = Schema.RequiredMode.REQUIRED, example = "0")
    @NotNull(message = "状态不能为空")
    @InEnum(value = CommonStatusEnum.class, message = "状态必须是 {value}")
    private Integer status;

    @Schema(description = "描述", example = "支付成功后、发货前的异步订单风险拦截场景")
    @Size(max = 512, message = "描述长度不能超过 512 个字符")
    private String description;

}
