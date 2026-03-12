package cn.liboshuai.pulsix.module.risk.controller.admin.ingestmapping.vo;

import cn.liboshuai.pulsix.framework.common.enums.CommonStatusEnum;
import cn.liboshuai.pulsix.framework.common.pojo.PageParam;
import cn.liboshuai.pulsix.framework.common.validation.InEnum;
import cn.liboshuai.pulsix.module.risk.enums.ingestmapping.RiskIngestMappingTransformTypeEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Schema(description = "管理后台 - 接入字段映射分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
public class IngestMappingPageReqVO extends PageParam {

    @Schema(description = "接入源编码", example = "trade_http_demo")
    private String sourceCode;

    @Schema(description = "所属场景编码", example = "TRADE_RISK")
    private String sceneCode;

    @Schema(description = "所属事件编码", example = "TRADE_EVENT")
    private String eventCode;

    @Schema(description = "目标字段编码", example = "userId")
    private String targetFieldCode;

    @Schema(description = "转换类型", example = "DIRECT")
    @InEnum(value = RiskIngestMappingTransformTypeEnum.class, message = "转换类型必须是 {value}")
    private String transformType;

    @Schema(description = "状态，见 CommonStatusEnum 枚举", example = "0")
    @InEnum(value = CommonStatusEnum.class, message = "状态必须是 {value}")
    private Integer status;

}
