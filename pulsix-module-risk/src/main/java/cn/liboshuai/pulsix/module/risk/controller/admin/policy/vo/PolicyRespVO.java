package cn.liboshuai.pulsix.module.risk.controller.admin.policy.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "管理后台 - 策略 Response VO")
@Data
public class PolicyRespVO {

    @Schema(description = "主键", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    private Long id;

    @Schema(description = "所属场景编码", example = "TRADE_RISK")
    private String sceneCode;

    @Schema(description = "策略编码", example = "TRADE_RISK_POLICY")
    private String policyCode;

    @Schema(description = "策略名称", example = "交易风控主策略")
    private String policyName;

    @Schema(description = "决策模式", example = "FIRST_HIT")
    private String decisionMode;

    @Schema(description = "默认动作", example = "PASS")
    private String defaultAction;

    @Schema(description = "分值计算模式", example = "SUM_HIT_SCORE")
    private String scoreCalcMode;

    @Schema(description = "状态", example = "0")
    private Integer status;

    @Schema(description = "设计态版本", example = "1")
    private Integer version;

    @Schema(description = "策略说明")
    private String description;

    @Schema(description = "规则编码顺序列表")
    private List<String> ruleCodes;

    @Schema(description = "规则引用列表")
    private List<PolicyRuleRefRespVO> ruleRefs;

    @Schema(description = "评分段列表")
    private List<PolicyScoreBandRespVO> scoreBands;

    @Schema(description = "创建者")
    private String creator;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新者")
    private String updater;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;

}
