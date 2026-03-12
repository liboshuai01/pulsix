package cn.liboshuai.pulsix.module.risk.controller.admin.decisionlog.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;

@Schema(description = "管理后台 - 规则命中明细 Response VO")
@Data
public class RuleHitLogRespVO {

    @Schema(description = "编号", example = "7201")
    private Long id;

    @Schema(description = "决策日志编号", example = "7101")
    private Long decisionId;

    @Schema(description = "规则编码", example = "R001")
    private String ruleCode;

    @Schema(description = "规则名称", example = "黑名单设备直接拒绝")
    private String ruleName;

    @Schema(description = "规则顺序", example = "1")
    private Integer ruleOrderNo;

    @Schema(description = "是否命中：1-命中，0-未命中", example = "1")
    private Integer hitFlag;

    @Schema(description = "命中原因")
    private String hitReason;

    @Schema(description = "规则分值", example = "100")
    private Integer score;

    @Schema(description = "关键命中值快照 JSON")
    private Map<String, Object> hitValueJson;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

}
