package cn.liboshuai.pulsix.module.risk.controller.admin.decisionlog.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "管理后台 - 决策日志 Response VO")
@Data
public class DecisionLogRespVO {

    @Schema(description = "编号", example = "7101")
    private Long id;

    @Schema(description = "链路号", example = "TRACE-S16-7101")
    private String traceId;

    @Schema(description = "事件编号", example = "E_DECISION_7101")
    private String eventId;

    @Schema(description = "所属场景编码", example = "TRADE_RISK")
    private String sceneCode;

    @Schema(description = "接入源编码", example = "trade_http_demo")
    private String sourceCode;

    @Schema(description = "事件编码", example = "TRADE_EVENT")
    private String eventCode;

    @Schema(description = "主体编号", example = "U9101")
    private String entityId;

    @Schema(description = "策略编码", example = "TRADE_RISK_POLICY")
    private String policyCode;

    @Schema(description = "最终动作", example = "REJECT")
    private String finalAction;

    @Schema(description = "最终分数", example = "100")
    private Integer finalScore;

    @Schema(description = "版本号", example = "14")
    private Integer versionNo;

    @Schema(description = "决策耗时", example = "12")
    private Long latencyMs;

    @Schema(description = "事件时间")
    private LocalDateTime eventTime;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "命中规则编码摘要")
    private List<String> hitRuleCodes;

}
