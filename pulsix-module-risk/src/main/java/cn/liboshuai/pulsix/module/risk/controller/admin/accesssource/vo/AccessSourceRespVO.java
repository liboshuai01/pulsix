package cn.liboshuai.pulsix.module.risk.controller.admin.accesssource.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "管理后台 - 接入源 Response VO")
@Data
public class AccessSourceRespVO {

    @Schema(description = "接入源主键", example = "14001")
    private Long id;

    @Schema(description = "接入源编码", example = "PROMOTION_CENTER_HTTP")
    private String sourceCode;

    @Schema(description = "接入源名称", example = "营销中心 HTTP 接入")
    private String sourceName;

    @Schema(description = "接入源类型", example = "HTTP")
    private String sourceType;

    @Schema(description = "标准事件 Topic", example = "pulsix.event.standard")
    private String topicName;

    @Schema(description = "接入协议", example = "HTTP")
    private String accessProtocol;

    @Schema(description = "应用标识", example = "marketing-center")
    private String appId;

    @Schema(description = "负责人", example = "张三")
    private String ownerName;

    @Schema(description = "联系邮箱", example = "marketing-risk@example.com")
    private String contactEmail;

    @Schema(description = "限流 QPS", example = "300")
    private Integer rateLimitQps;

    @Schema(description = "允许场景编码列表")
    private List<String> allowedSceneCodes;

    @Schema(description = "IP 白名单")
    private List<String> ipWhitelist;

    @Schema(description = "状态", example = "1")
    private Integer status;

    @Schema(description = "描述", example = "服务营销受理事件的 HTTP 接入源")
    private String description;

    @Schema(description = "创建者", example = "admin")
    private String creator;

    @Schema(description = "创建时间", example = "2026-03-08T09:58:00")
    private LocalDateTime createTime;

    @Schema(description = "更新者", example = "admin")
    private String updater;

    @Schema(description = "更新时间", example = "2026-03-08T09:58:00")
    private LocalDateTime updateTime;

}
