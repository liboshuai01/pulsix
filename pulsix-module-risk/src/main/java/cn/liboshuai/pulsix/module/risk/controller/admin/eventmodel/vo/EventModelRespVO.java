package cn.liboshuai.pulsix.module.risk.controller.admin.eventmodel.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

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

    @Schema(description = "是否可删除", example = "false")
    private Boolean deletable;

    @Schema(description = "删除阻断原因", example = "当前存在关联接入映射，无法删除")
    private String deleteBlockedReason;

    @Schema(description = "创建者", example = "admin")
    private String creator;

    @Schema(description = "创建时间", example = "2026-03-08T10:00:00")
    private LocalDateTime createTime;

    @Schema(description = "更新者", example = "admin")
    private String updater;

    @Schema(description = "更新时间", example = "2026-03-08T10:00:00")
    private LocalDateTime updateTime;

}
