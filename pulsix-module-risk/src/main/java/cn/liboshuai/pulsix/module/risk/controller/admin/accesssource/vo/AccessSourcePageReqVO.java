package cn.liboshuai.pulsix.module.risk.controller.admin.accesssource.vo;

import cn.liboshuai.pulsix.framework.common.enums.CommonStatusEnum;
import cn.liboshuai.pulsix.framework.common.pojo.PageParam;
import cn.liboshuai.pulsix.framework.common.validation.InEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "管理后台 - 接入源分页 Request VO")
@Data
public class AccessSourcePageReqVO extends PageParam {

    @Schema(description = "接入源编码，模糊匹配", example = "ORDER_CENTER")
    private String sourceCode;

    @Schema(description = "接入源名称，模糊匹配", example = "订单中心")
    private String sourceName;

    @Schema(description = "接入源类型", example = "SDK")
    private String sourceType;

    @Schema(description = "标准事件 Topic", example = "pulsix.event.standard")
    private String topicName;

    @Schema(description = "状态", example = "1")
    @InEnum(value = CommonStatusEnum.class, message = "状态必须是 {value}")
    private Integer status;

}
