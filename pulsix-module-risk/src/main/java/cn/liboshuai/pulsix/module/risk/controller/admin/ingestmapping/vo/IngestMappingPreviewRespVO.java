package cn.liboshuai.pulsix.module.risk.controller.admin.ingestmapping.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Schema(description = "管理后台 - 接入字段映射预览 Response VO")
@Data
public class IngestMappingPreviewRespVO {

    @Schema(description = "接入源编码", requiredMode = Schema.RequiredMode.REQUIRED, example = "trade_http_demo")
    private String sourceCode;

    @Schema(description = "所属场景编码", requiredMode = Schema.RequiredMode.REQUIRED, example = "TRADE_RISK")
    private String sceneCode;

    @Schema(description = "所属事件编码", requiredMode = Schema.RequiredMode.REQUIRED, example = "TRADE_EVENT")
    private String eventCode;

    @Schema(description = "原始报文 JSON")
    private Map<String, Object> rawEventJson;

    @Schema(description = "标准事件预览 JSON")
    private Map<String, Object> standardEventJson;

    @Schema(description = "缺失的必填字段编码")
    private List<String> missingRequiredFields;

    @Schema(description = "通过默认值补齐的字段编码")
    private List<String> defaultedFields;

    @Schema(description = "通过接入映射命中的字段编码")
    private List<String> mappedFields;

}
