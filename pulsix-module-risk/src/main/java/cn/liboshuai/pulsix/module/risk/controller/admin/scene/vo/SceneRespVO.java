package cn.liboshuai.pulsix.module.risk.controller.admin.scene.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - 风控场景 Response VO")
@Data
public class SceneRespVO {

    @Schema(description = "场景编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    private Long id;

    @Schema(description = "场景编码", requiredMode = Schema.RequiredMode.REQUIRED, example = "TRADE_RISK")
    private String sceneCode;

    @Schema(description = "场景名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "交易风控")
    private String sceneName;

    @Schema(description = "场景类型", requiredMode = Schema.RequiredMode.REQUIRED, example = "TRADE_SECURITY")
    private String sceneType;

    @Schema(description = "接入模式", requiredMode = Schema.RequiredMode.REQUIRED, example = "MIXED")
    private String accessMode;

    @Schema(description = "默认事件编码", example = "TRADE_EVENT")
    private String defaultEventCode;

    @Schema(description = "默认策略编码", example = "TRADE_RISK_POLICY")
    private String defaultPolicyCode;

    @Schema(description = "标准事件 Topic", example = "pulsix.event.standard")
    private String standardTopicName;

    @Schema(description = "决策结果 Topic", example = "pulsix.decision.result")
    private String decisionTopicName;

    @Schema(description = "状态，见 CommonStatusEnum 枚举", requiredMode = Schema.RequiredMode.REQUIRED, example = "0")
    private Integer status;

    @Schema(description = "场景说明")
    private String description;

    @Schema(description = "创建者")
    private String creator;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新者")
    private String updater;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;

}

