package cn.liboshuai.pulsix.module.risk.controller.admin.eventfield.vo;

import cn.liboshuai.pulsix.framework.common.pojo.PageParam;
import cn.liboshuai.pulsix.framework.common.validation.InEnum;
import cn.liboshuai.pulsix.module.risk.enums.eventfield.RiskEventFieldTypeEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Schema(description = "管理后台 - 事件字段分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
public class EventFieldPageReqVO extends PageParam {

    @Schema(description = "所属场景编码", example = "TRADE_RISK")
    private String sceneCode;

    @Schema(description = "所属事件编码", example = "TRADE_EVENT")
    private String eventCode;

    @Schema(description = "字段编码", example = "userId")
    private String fieldCode;

    @Schema(description = "字段名称", example = "用户编号")
    private String fieldName;

    @Schema(description = "字段类型", example = "STRING")
    @InEnum(value = RiskEventFieldTypeEnum.class, message = "字段类型必须是 {value}")
    private String fieldType;

}
