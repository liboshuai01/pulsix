package cn.liboshuai.pulsix.module.risk.controller.admin.eventfield.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - 事件字段 Response VO")
@Data
public class EventFieldRespVO {

    @Schema(description = "主键", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    private Long id;

    @Schema(description = "所属场景编码", requiredMode = Schema.RequiredMode.REQUIRED, example = "TRADE_RISK")
    private String sceneCode;

    @Schema(description = "所属事件编码", requiredMode = Schema.RequiredMode.REQUIRED, example = "TRADE_EVENT")
    private String eventCode;

    @Schema(description = "字段编码", requiredMode = Schema.RequiredMode.REQUIRED, example = "userId")
    private String fieldCode;

    @Schema(description = "字段名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "用户编号")
    private String fieldName;

    @Schema(description = "字段类型", requiredMode = Schema.RequiredMode.REQUIRED, example = "STRING")
    private String fieldType;

    @Schema(description = "字段 JSONPath", example = "$.userId")
    private String fieldPath;

    @Schema(description = "是否标准公共字段：1-是，0-否", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    private Integer standardFieldFlag;

    @Schema(description = "是否必填：1-必填，0-非必填", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    private Integer requiredFlag;

    @Schema(description = "是否允许为空：1-允许，0-不允许", requiredMode = Schema.RequiredMode.REQUIRED, example = "0")
    private Integer nullableFlag;

    @Schema(description = "默认值")
    private String defaultValue;

    @Schema(description = "示例值")
    private String sampleValue;

    @Schema(description = "字段说明")
    private String description;

    @Schema(description = "排序号", requiredMode = Schema.RequiredMode.REQUIRED, example = "10")
    private Integer sortNo;

    @Schema(description = "创建者")
    private String creator;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新者")
    private String updater;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;

}
