package cn.liboshuai.pulsix.module.risk.controller.admin.eventmodel.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "管理后台 - 事件模型精简 Response VO")
@Data
public class EventModelSimpleRespVO {

    @Schema(description = "场景编码", example = "PROMOTION_RISK")
    private String sceneCode;

    @Schema(description = "事件编码", example = "PROMOTION_EVENT")
    private String eventCode;

    @Schema(description = "事件名称", example = "营销受理事件")
    private String eventName;

}
