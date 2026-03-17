package cn.liboshuai.pulsix.module.risk.controller.admin.eventmodel.vo;

import cn.liboshuai.pulsix.framework.common.enums.CommonStatusEnum;
import cn.liboshuai.pulsix.framework.common.pojo.PageParam;
import cn.liboshuai.pulsix.framework.common.validation.InEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "管理后台 - 事件模型分页 Request VO")
@Data
public class EventModelPageReqVO extends PageParam {

    @Schema(description = "场景编码，模糊匹配", example = "PROMOTION_RISK")
    private String sceneCode;

    @Schema(description = "事件编码，模糊匹配", example = "PROMOTION_EVENT")
    private String eventCode;

    @Schema(description = "事件名称，模糊匹配", example = "营销受理")
    private String eventName;

    @Schema(description = "事件类型，模糊匹配", example = "promotion_grant")
    private String eventType;

    @Schema(description = "状态", example = "0")
    @InEnum(value = CommonStatusEnum.class, message = "状态必须是 {value}")
    private Integer status;

}
