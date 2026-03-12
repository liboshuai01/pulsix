package cn.liboshuai.pulsix.module.risk.controller.admin.policy.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "管理后台 - 策略评分段 Response VO")
@Data
public class PolicyScoreBandRespVO {

    @Schema(description = "分段顺序", example = "10")
    private Integer bandNo;

    @Schema(description = "最小分值（含）", example = "0")
    private Integer minScore;

    @Schema(description = "最大分值（含）", example = "59")
    private Integer maxScore;

    @Schema(description = "命中动作", example = "PASS")
    private String hitAction;

    @Schema(description = "命中原因模板")
    private String hitReasonTemplate;

}
