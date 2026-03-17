package cn.liboshuai.pulsix.module.risk.controller.admin.accessmapping.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Schema(description = "管理后台 - 接入映射 Response VO")
@Data
public class AccessMappingRespVO {

    @Schema(description = "接入映射主键", example = "14101")
    private Long id;

    @Schema(description = "场景编码", example = "PROMOTION_RISK")
    private String sceneCode;

    @Schema(description = "事件编码", example = "PROMOTION_EVENT")
    private String eventCode;

    @Schema(description = "事件名称", example = "营销受理事件")
    private String eventName;

    @Schema(description = "接入源编码", example = "PROMOTION_CENTER_HTTP")
    private String sourceCode;

    @Schema(description = "接入源名称", example = "营销中心 HTTP 接入")
    private String sourceName;

    @Schema(description = "接入源类型", example = "HTTP")
    private String sourceType;

    @Schema(description = "标准 Topic", example = "pulsix.event.standard")
    private String topicName;

    @Schema(description = "描述", example = "营销中心受理事件接入映射")
    private String description;

    @Schema(description = "原始样例报文")
    private Map<String, Object> rawSampleJson;

    @Schema(description = "样例请求头")
    private Map<String, Object> sampleHeadersJson;

    @Schema(description = "原始字段数量", example = "7")
    private Integer rawFieldCount;

    @Schema(description = "标准化规则数量", example = "9")
    private Integer mappingRuleCount;

    @Schema(description = "原始字段定义")
    private List<AccessRawFieldItemVO> rawFields;

    @Schema(description = "标准化映射规则")
    private List<AccessMappingRuleItemVO> mappingRules;

    @Schema(description = "创建者", example = "admin")
    private String creator;

    @Schema(description = "创建时间", example = "2026-03-08T10:00:00")
    private LocalDateTime createTime;

    @Schema(description = "更新者", example = "admin")
    private String updater;

    @Schema(description = "更新时间", example = "2026-03-08T10:00:00")
    private LocalDateTime updateTime;

}
