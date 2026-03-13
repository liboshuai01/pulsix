package cn.liboshuai.pulsix.module.risk.controller.admin.replay.vo;

import cn.liboshuai.pulsix.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Schema(description = "管理后台 - 回放任务分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
public class ReplayJobPageReqVO extends PageParam {

    @Schema(description = "所属场景编码", example = "TRADE_RISK")
    private String sceneCode;

    @Schema(description = "任务编码", example = "REPLAY_TRADE_RISK_14_15_001")
    private String jobCode;

    @Schema(description = "任务状态", example = "SUCCESS")
    private String jobStatus;

}
