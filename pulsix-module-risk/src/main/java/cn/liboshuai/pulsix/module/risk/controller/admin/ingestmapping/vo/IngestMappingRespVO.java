package cn.liboshuai.pulsix.module.risk.controller.admin.ingestmapping.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;

@Schema(description = "管理后台 - 接入字段映射 Response VO")
@Data
public class IngestMappingRespVO {

    @Schema(description = "主键", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    private Long id;

    @Schema(description = "接入源编码", requiredMode = Schema.RequiredMode.REQUIRED, example = "trade_http_demo")
    private String sourceCode;

    @Schema(description = "所属场景编码", requiredMode = Schema.RequiredMode.REQUIRED, example = "TRADE_RISK")
    private String sceneCode;

    @Schema(description = "所属事件编码", requiredMode = Schema.RequiredMode.REQUIRED, example = "TRADE_EVENT")
    private String eventCode;

    @Schema(description = "原始字段路径", example = "$.uid")
    private String sourceFieldPath;

    @Schema(description = "目标字段编码", requiredMode = Schema.RequiredMode.REQUIRED, example = "userId")
    private String targetFieldCode;

    @Schema(description = "目标字段名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "用户编号")
    private String targetFieldName;

    @Schema(description = "转换类型", requiredMode = Schema.RequiredMode.REQUIRED, example = "DIRECT")
    private String transformType;

    @Schema(description = "转换表达式或常量值")
    private String transformExpr;

    @Schema(description = "默认值")
    private String defaultValue;

    @Schema(description = "是否必填：1-必填，0-非必填", example = "1")
    private Integer requiredFlag;

    @Schema(description = "清洗规则 JSON")
    private Map<String, Object> cleanRuleJson;

    @Schema(description = "排序号", example = "10")
    private Integer sortNo;

    @Schema(description = "状态，见 CommonStatusEnum 枚举", example = "0")
    private Integer status;

    @Schema(description = "创建者", example = "admin")
    private String creator;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新者", example = "admin")
    private String updater;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;

}
