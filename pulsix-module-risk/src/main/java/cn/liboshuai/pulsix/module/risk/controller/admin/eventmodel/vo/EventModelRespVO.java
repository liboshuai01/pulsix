package cn.liboshuai.pulsix.module.risk.controller.admin.eventmodel.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Schema(description = "管理后台 - 事件模型 Response VO")
@Data
public class EventModelRespVO {

    @Schema(description = "事件模型主键", example = "1")
    private Long id;

    @Schema(description = "场景编码", example = "PROMOTION_RISK")
    private String sceneCode;

    @Schema(description = "事件编码", example = "PROMOTION_EVENT")
    private String eventCode;

    @Schema(description = "事件名称", example = "营销受理事件")
    private String eventName;

    @Schema(description = "事件类型", example = "promotion_grant")
    private String eventType;

    @Schema(description = "样例事件")
    private Map<String, Object> sampleEventJson;

    @Schema(description = "版本", example = "1")
    private Integer version;

    @Schema(description = "状态", example = "0")
    private Integer status;

    @Schema(description = "描述", example = "营销受理标准事件模型")
    private String description;

    @Schema(description = "字段定义")
    private List<EventFieldItemVO> fields;

    @Schema(description = "接入绑定")
    private List<EventBindingSourceItemVO> bindingSources;

    @Schema(description = "创建者", example = "admin")
    private String creator;

    @Schema(description = "创建时间", example = "2026-03-08T10:00:00")
    private LocalDateTime createTime;

    @Schema(description = "更新者", example = "admin")
    private String updater;

    @Schema(description = "更新时间", example = "2026-03-08T10:00:00")
    private LocalDateTime updateTime;

}
