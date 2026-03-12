package cn.liboshuai.pulsix.module.risk.controller.admin.ingestsource.vo;

import cn.liboshuai.pulsix.framework.common.enums.CommonStatusEnum;
import cn.liboshuai.pulsix.framework.common.pojo.PageParam;
import cn.liboshuai.pulsix.framework.common.validation.InEnum;
import cn.liboshuai.pulsix.module.risk.enums.ingestsource.RiskIngestSourceAuthTypeEnum;
import cn.liboshuai.pulsix.module.risk.enums.ingestsource.RiskIngestSourceTypeEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Schema(description = "管理后台 - 接入源分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
public class IngestSourcePageReqVO extends PageParam {

    @Schema(description = "接入源编码", example = "trade_http_demo")
    private String sourceCode;

    @Schema(description = "接入源名称", example = "交易 HTTP Demo")
    private String sourceName;

    @Schema(description = "接入方式", example = "HTTP")
    @InEnum(value = RiskIngestSourceTypeEnum.class, message = "接入方式必须是 {value}")
    private String sourceType;

    @Schema(description = "鉴权方式", example = "HMAC")
    @InEnum(value = RiskIngestSourceAuthTypeEnum.class, message = "鉴权方式必须是 {value}")
    private String authType;

    @Schema(description = "状态，见 CommonStatusEnum 枚举", example = "0")
    @InEnum(value = CommonStatusEnum.class, message = "状态必须是 {value}")
    private Integer status;

}
