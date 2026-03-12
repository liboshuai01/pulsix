package cn.liboshuai.pulsix.module.risk.controller.admin.decisionlog.vo;

import cn.liboshuai.pulsix.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Schema(description = "管理后台 - 决策日志分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
public class DecisionLogPageReqVO extends PageParam {

    @Schema(description = "所属场景编码", example = "TRADE_RISK")
    private String sceneCode;

    @Schema(description = "链路号", example = "TRACE-S16-7101")
    private String traceId;

    @Schema(description = "事件编号", example = "E_DECISION_7101")
    private String eventId;

    @Schema(description = "最终动作", example = "REJECT")
    private String finalAction;

    @Schema(description = "版本号", example = "14")
    private Integer versionNo;

}
