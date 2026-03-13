package cn.liboshuai.pulsix.module.risk.controller.admin.ingestsource.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Schema(description = "管理后台 - 接入源 Response VO")
@Data
public class IngestSourceRespVO {

    @Schema(description = "主键", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    private Long id;

    @Schema(description = "接入源编码", requiredMode = Schema.RequiredMode.REQUIRED, example = "trade_http_demo")
    private String sourceCode;

    @Schema(description = "接入源名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "交易 HTTP Demo")
    private String sourceName;

    @Schema(description = "接入方式", requiredMode = Schema.RequiredMode.REQUIRED, example = "HTTP")
    private String sourceType;

    @Schema(description = "鉴权方式", requiredMode = Schema.RequiredMode.REQUIRED, example = "HMAC")
    private String authType;

    @Schema(description = "鉴权配置 JSON")
    private Map<String, Object> authConfigJson;

    @Schema(description = "允许接入的场景范围 JSON")
    private List<String> sceneScopeJson;

    @Schema(description = "标准事件 Topic", example = "pulsix.event.standard")
    private String standardTopicName;

    @Schema(description = "异常 / DLQ Topic", example = "pulsix.event.dlq")
    private String errorTopicName;

    @Schema(description = "限流阈值 QPS", example = "300")
    private Integer rateLimitQps;

    @Schema(description = "状态，见 CommonStatusEnum 枚举", requiredMode = Schema.RequiredMode.REQUIRED, example = "0")
    private Integer status;

    @Schema(description = "接入源说明")
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
