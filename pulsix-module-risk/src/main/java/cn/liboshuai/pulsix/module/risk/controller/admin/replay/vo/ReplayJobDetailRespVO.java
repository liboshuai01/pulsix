package cn.liboshuai.pulsix.module.risk.controller.admin.replay.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;
import java.util.Map;

@Schema(description = "管理后台 - 回放任务详情 Response VO")
@Data
@EqualsAndHashCode(callSuper = true)
public class ReplayJobDetailRespVO extends ReplayJobRespVO {

    @Schema(description = "回放汇总 JSON")
    private Map<String, Object> summaryJson;

    @Schema(description = "样例差异 JSON")
    private List<Map<String, Object>> sampleDiffJson;

}
