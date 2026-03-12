package cn.liboshuai.pulsix.module.risk.controller.admin.decisionlog.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;
import java.util.Map;

@Schema(description = "管理后台 - 决策日志详情 Response VO")
@Data
@EqualsAndHashCode(callSuper = true)
public class DecisionLogDetailRespVO extends DecisionLogRespVO {

    @Schema(description = "标准事件输入 JSON")
    private Map<String, Object> inputJson;

    @Schema(description = "特征快照 JSON")
    private Map<String, Object> featureSnapshotJson;

    @Schema(description = "命中规则摘要 JSON")
    private List<Map<String, Object>> hitRulesJson;

    @Schema(description = "额外决策明细 JSON")
    private Map<String, Object> decisionDetailJson;

}
