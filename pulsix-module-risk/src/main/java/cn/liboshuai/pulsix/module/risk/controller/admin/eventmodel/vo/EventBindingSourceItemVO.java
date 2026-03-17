package cn.liboshuai.pulsix.module.risk.controller.admin.eventmodel.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "管理后台 - 事件模型绑定接入源 Response VO")
@Data
public class EventBindingSourceItemVO {

    @Schema(description = "接入源编码", example = "ORDER_CENTER_SDK")
    private String sourceCode;

    @Schema(description = "接入源名称", example = "订单中心 SDK 接入")
    private String sourceName;

    @Schema(description = "接入源类型", example = "SDK")
    private String sourceType;

    @Schema(description = "标准事件 Topic", example = "pulsix.event.standard")
    private String topicName;

    @Schema(description = "状态", example = "1")
    private Integer status;

}
