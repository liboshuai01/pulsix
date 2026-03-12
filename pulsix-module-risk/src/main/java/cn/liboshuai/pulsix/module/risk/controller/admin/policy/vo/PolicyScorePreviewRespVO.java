package cn.liboshuai.pulsix.module.risk.controller.admin.policy.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "管理后台 - 策略评分卡预览 Response VO")
@Data
public class PolicyScorePreviewRespVO {

    @Schema(description = "决策模式", example = "SCORE_CARD")
    private String decisionMode;

    @Schema(description = "总分", example = "95")
    private Integer totalScore;

    @Schema(description = "默认动作", example = "PASS")
    private String defaultAction;

    @Schema(description = "最终动作", example = "REVIEW")
    private String finalAction;

    @Schema(description = "是否命中评分段", example = "true")
    private Boolean matched;

    @Schema(description = "命中的分段顺序", example = "20")
    private Integer matchedBandNo;

    @Schema(description = "命中的最小分值", example = "60")
    private Integer matchedMinScore;

    @Schema(description = "命中的最大分值", example = "119")
    private Integer matchedMaxScore;

    @Schema(description = "预览原因")
    private String reason;

}
