package cn.liboshuai.pulsix.module.risk.controller.admin.eventschema.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - 事件 Schema Response VO")
@Data
public class EventSchemaRespVO {

    @Schema(description = "主键", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    private Long id;

    @Schema(description = "所属场景编码", requiredMode = Schema.RequiredMode.REQUIRED, example = "TRADE_RISK")
    private String sceneCode;

    @Schema(description = "事件编码", requiredMode = Schema.RequiredMode.REQUIRED, example = "TRADE_EVENT")
    private String eventCode;

    @Schema(description = "事件名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "交易标准事件")
    private String eventName;

    @Schema(description = "事件类别", requiredMode = Schema.RequiredMode.REQUIRED, example = "BUSINESS")
    private String eventType;

    @Schema(description = "默认接入方式", requiredMode = Schema.RequiredMode.REQUIRED, example = "MIXED")
    private String sourceType;

    @Schema(description = "原始事件 Topic")
    private String rawTopicName;

    @Schema(description = "标准事件 Topic")
    private String standardTopicName;

    @Schema(description = "事件模型版本号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    private Integer version;

    @Schema(description = "事件模型说明")
    private String description;

    @Schema(description = "创建者")
    private String creator;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新者")
    private String updater;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;

}
