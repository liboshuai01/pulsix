package cn.liboshuai.pulsix.module.risk.controller.admin.scene.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - 风控场景 Response VO")
@Data
public class SceneRespVO {

    @Schema(description = "场景主键", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    private Long id;

    @Schema(description = "场景编码", requiredMode = Schema.RequiredMode.REQUIRED, example = "TRADE_RISK")
    private String sceneCode;

    @Schema(description = "场景名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "交易风控")
    private String sceneName;

    @Schema(description = "运行模式", requiredMode = Schema.RequiredMode.REQUIRED, example = "ASYNC_DECISION")
    private String runtimeMode;

    @Schema(description = "默认策略编码", example = "TRADE_RISK_POLICY_FIRST_HIT")
    private String defaultPolicyCode;

    @Schema(description = "状态", requiredMode = Schema.RequiredMode.REQUIRED, example = "0")
    private Integer status;

    @Schema(description = "描述", example = "支付成功后、发货前的异步订单风险拦截场景")
    private String description;

    @Schema(description = "创建者", example = "admin")
    private String creator;

    @Schema(description = "创建时间", example = "2026-03-08T09:30:00")
    private LocalDateTime createTime;

    @Schema(description = "更新者", example = "admin")
    private String updater;

    @Schema(description = "更新时间", example = "2026-03-08T09:30:00")
    private LocalDateTime updateTime;

}
