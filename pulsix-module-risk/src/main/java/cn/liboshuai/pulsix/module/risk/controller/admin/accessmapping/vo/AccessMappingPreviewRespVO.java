package cn.liboshuai.pulsix.module.risk.controller.admin.accessmapping.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Schema(description = "管理后台 - 接入映射标准事件预览 Response VO")
@Data
public class AccessMappingPreviewRespVO {

    @Schema(description = "标准事件预览")
    private Map<String, Object> standardEventJson;

    @Schema(description = "字段值来源")
    private Map<String, String> fieldSourceMap;

    @Schema(description = "预览消息")
    private List<String> messages;

}
