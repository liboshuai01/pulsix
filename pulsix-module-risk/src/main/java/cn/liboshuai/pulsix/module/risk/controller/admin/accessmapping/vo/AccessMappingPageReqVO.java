package cn.liboshuai.pulsix.module.risk.controller.admin.accessmapping.vo;

import cn.liboshuai.pulsix.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "管理后台 - 接入映射分页 Request VO")
@Data
public class AccessMappingPageReqVO extends PageParam {

    @Schema(description = "场景编码，模糊匹配", example = "PROMOTION_RISK")
    private String sceneCode;

    @Schema(description = "事件编码，模糊匹配", example = "PROMOTION_EVENT")
    private String eventCode;

    @Schema(description = "事件类型，模糊匹配", example = "promotion_grant")
    private String eventType;

    @Schema(description = "接入源编码，模糊匹配", example = "PROMOTION_CENTER_HTTP")
    private String sourceCode;

    @Schema(description = "接入源类型", example = "HTTP")
    private String sourceType;

}
