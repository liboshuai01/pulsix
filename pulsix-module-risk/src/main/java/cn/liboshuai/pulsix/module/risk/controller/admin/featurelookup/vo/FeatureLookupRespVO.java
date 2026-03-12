package cn.liboshuai.pulsix.module.risk.controller.admin.featurelookup.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;

@Schema(description = "管理后台 - 查询特征 Response VO")
@Data
public class FeatureLookupRespVO {

    @Schema(description = "主键", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    private Long id;

    @Schema(description = "所属场景编码", requiredMode = Schema.RequiredMode.REQUIRED, example = "TRADE_RISK")
    private String sceneCode;

    @Schema(description = "特征编码", requiredMode = Schema.RequiredMode.REQUIRED, example = "device_in_blacklist")
    private String featureCode;

    @Schema(description = "特征名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "设备是否命中黑名单")
    private String featureName;

    @Schema(description = "特征类型", requiredMode = Schema.RequiredMode.REQUIRED, example = "LOOKUP")
    private String featureType;

    @Schema(description = "查询类型", requiredMode = Schema.RequiredMode.REQUIRED, example = "REDIS_SET")
    private String lookupType;

    @Schema(description = "构造 lookup key 的表达式", example = "deviceId")
    private String keyExpr;

    @Schema(description = "查询来源引用", example = "pulsix:list:black:device")
    private String sourceRef;

    @Schema(description = "默认值")
    private String defaultValue;

    @Schema(description = "特征值类型", requiredMode = Schema.RequiredMode.REQUIRED, example = "BOOLEAN")
    private String valueType;

    @Schema(description = "本地缓存 TTL（秒）", example = "30")
    private Long cacheTtlSeconds;

    @Schema(description = "单次 lookup 超时时间（毫秒）", example = "20")
    private Integer timeoutMs;

    @Schema(description = "扩展配置 JSON")
    private Map<String, Object> extraJson;

    @Schema(description = "状态", example = "0")
    private Integer status;

    @Schema(description = "设计态版本", example = "1")
    private Integer version;

    @Schema(description = "特征说明")
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
