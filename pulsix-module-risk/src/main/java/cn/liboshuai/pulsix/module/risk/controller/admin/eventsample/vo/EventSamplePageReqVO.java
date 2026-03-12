package cn.liboshuai.pulsix.module.risk.controller.admin.eventsample.vo;

import cn.liboshuai.pulsix.framework.common.enums.CommonStatusEnum;
import cn.liboshuai.pulsix.framework.common.pojo.PageParam;
import cn.liboshuai.pulsix.framework.common.validation.InEnum;
import cn.liboshuai.pulsix.module.risk.enums.eventsample.RiskEventSampleTypeEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Schema(description = "管理后台 - 事件样例分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
public class EventSamplePageReqVO extends PageParam {

    @Schema(description = "所属场景编码", example = "TRADE_RISK")
    private String sceneCode;

    @Schema(description = "所属事件编码", example = "TRADE_EVENT")
    private String eventCode;

    @Schema(description = "样例编码", example = "TRADE_STD_SUCCESS")
    private String sampleCode;

    @Schema(description = "样例名称", example = "交易成功标准样例")
    private String sampleName;

    @Schema(description = "样例类型", example = "STANDARD")
    @InEnum(value = RiskEventSampleTypeEnum.class, message = "样例类型必须是 {value}")
    private String sampleType;

    @Schema(description = "状态，见 CommonStatusEnum 枚举", example = "0")
    @InEnum(value = CommonStatusEnum.class, message = "状态必须是 {value}")
    private Integer status;

}
