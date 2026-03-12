package cn.liboshuai.pulsix.module.risk.controller.admin.eventsample.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Schema(description = "管理后台 - 事件样例标准事件预览 Response VO")
@Data
public class EventSamplePreviewRespVO {

    @Schema(description = "样例编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    private Long sampleId;

    @Schema(description = "样例编码", requiredMode = Schema.RequiredMode.REQUIRED, example = "TRADE_STD_SUCCESS")
    private String sampleCode;

    @Schema(description = "样例名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "交易成功标准样例")
    private String sampleName;

    @Schema(description = "样例类型", requiredMode = Schema.RequiredMode.REQUIRED, example = "STANDARD")
    private String sampleType;

    @Schema(description = "原始样例 JSON")
    private Map<String, Object> sampleJson;

    @Schema(description = "标准事件预览 JSON")
    private Map<String, Object> standardEventJson;

    @Schema(description = "缺失的必填字段编码")
    private List<String> missingRequiredFields;

    @Schema(description = "通过默认值补齐的字段编码")
    private List<String> defaultedFields;

    @Schema(description = "通过接入映射命中的字段编码")
    private List<String> mappedFields;

}
