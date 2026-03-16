package cn.liboshuai.pulsix.module.risk.controller.admin.eventmodel.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Schema(description = "管理后台 - 事件模型标准事件预览 Response VO")
@Data
public class EventModelPreviewRespVO {

    @Schema(description = "标准事件预览")
    private Map<String, Object> standardEventJson;

    @Schema(description = "必填字段")
    private List<String> requiredFields;

    @Schema(description = "可选字段")
    private List<String> optionalFields;

    @Schema(description = "字段类型摘要")
    private Map<String, String> fieldTypes;

    @Schema(description = "校验消息")
    private List<String> validationMessages;

}
