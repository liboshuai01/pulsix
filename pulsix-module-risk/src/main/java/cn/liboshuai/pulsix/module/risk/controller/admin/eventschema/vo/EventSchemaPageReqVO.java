package cn.liboshuai.pulsix.module.risk.controller.admin.eventschema.vo;

import cn.liboshuai.pulsix.framework.common.pojo.PageParam;
import cn.liboshuai.pulsix.framework.common.validation.InEnum;
import cn.liboshuai.pulsix.module.risk.enums.eventschema.RiskEventSchemaTypeEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Schema(description = "管理后台 - 事件 Schema 分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
public class EventSchemaPageReqVO extends PageParam {

    @Schema(description = "所属场景编码", example = "TRADE_RISK")
    private String sceneCode;

    @Schema(description = "事件编码", example = "TRADE_EVENT")
    private String eventCode;

    @Schema(description = "事件名称", example = "交易标准事件")
    private String eventName;

    @Schema(description = "事件类别", example = "BUSINESS")
    @InEnum(value = RiskEventSchemaTypeEnum.class, message = "事件类别必须是 {value}")
    private String eventType;

}
