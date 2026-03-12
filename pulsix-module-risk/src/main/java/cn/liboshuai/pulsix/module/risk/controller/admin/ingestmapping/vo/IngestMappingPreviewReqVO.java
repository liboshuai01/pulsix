package cn.liboshuai.pulsix.module.risk.controller.admin.ingestmapping.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.Map;

@Schema(description = "管理后台 - 接入字段映射预览 Request VO")
@Data
public class IngestMappingPreviewReqVO {

    @Schema(description = "接入源编码", requiredMode = Schema.RequiredMode.REQUIRED, example = "trade_http_demo")
    @NotBlank(message = "接入源编码不能为空")
    private String sourceCode;

    @Schema(description = "所属场景编码", requiredMode = Schema.RequiredMode.REQUIRED, example = "TRADE_RISK")
    @NotBlank(message = "所属场景编码不能为空")
    private String sceneCode;

    @Schema(description = "所属事件编码", requiredMode = Schema.RequiredMode.REQUIRED, example = "TRADE_EVENT")
    @NotBlank(message = "所属事件编码不能为空")
    private String eventCode;

    @Schema(description = "原始报文 JSON", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "原始报文 JSON 不能为空")
    private Map<String, Object> rawEventJson;

}
