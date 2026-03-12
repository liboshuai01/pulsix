package cn.liboshuai.pulsix.module.risk.controller.admin.rule.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - 规则 Response VO")
@Data
public class RuleRespVO {

    @Schema(description = "主键", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    private Long id;

    @Schema(description = "所属场景编码", requiredMode = Schema.RequiredMode.REQUIRED, example = "TRADE_RISK")
    private String sceneCode;

    @Schema(description = "规则编码", requiredMode = Schema.RequiredMode.REQUIRED, example = "R001")
    private String ruleCode;

    @Schema(description = "规则名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "黑名单设备直接拒绝")
    private String ruleName;

    @Schema(description = "规则类型", example = "NORMAL")
    private String ruleType;

    @Schema(description = "表达式引擎类型", example = "AVIATOR")
    private String engineType;

    @Schema(description = "规则表达式")
    private String exprContent;

    @Schema(description = "规则优先级", example = "100")
    private Integer priority;

    @Schema(description = "命中动作", example = "REJECT")
    private String hitAction;

    @Schema(description = "风险分值", example = "100")
    private Integer riskScore;

    @Schema(description = "命中原因模板")
    private String hitReasonTemplate;

    @Schema(description = "状态", example = "0")
    private Integer status;

    @Schema(description = "设计态版本", example = "1")
    private Integer version;

    @Schema(description = "规则说明")
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
