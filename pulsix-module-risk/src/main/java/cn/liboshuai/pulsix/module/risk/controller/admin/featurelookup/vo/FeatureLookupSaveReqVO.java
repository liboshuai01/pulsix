package cn.liboshuai.pulsix.module.risk.controller.admin.featurelookup.vo;

import cn.liboshuai.pulsix.framework.common.enums.CommonStatusEnum;
import cn.liboshuai.pulsix.framework.common.validation.InEnum;
import cn.liboshuai.pulsix.module.risk.enums.feature.RiskFeatureLookupTypeEnum;
import cn.liboshuai.pulsix.module.risk.enums.feature.RiskFeatureValueTypeEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.Map;

@Schema(description = "管理后台 - 查询特征创建/修改 Request VO")
@Data
public class FeatureLookupSaveReqVO {

    @Schema(description = "主键", example = "1")
    private Long id;

    @Schema(description = "所属场景编码", requiredMode = Schema.RequiredMode.REQUIRED, example = "TRADE_RISK")
    @NotBlank(message = "所属场景编码不能为空")
    @Size(max = 64, message = "所属场景编码长度不能超过 64 个字符")
    private String sceneCode;

    @Schema(description = "特征编码", requiredMode = Schema.RequiredMode.REQUIRED, example = "device_in_blacklist")
    @NotBlank(message = "特征编码不能为空")
    @Size(max = 64, message = "特征编码长度不能超过 64 个字符")
    @Pattern(regexp = "^[A-Za-z][A-Za-z0-9_]*$", message = "特征编码只允许字母、数字、下划线，且必须以字母开头")
    private String featureCode;

    @Schema(description = "特征名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "设备是否命中黑名单")
    @NotBlank(message = "特征名称不能为空")
    @Size(max = 128, message = "特征名称长度不能超过 128 个字符")
    private String featureName;

    @Schema(description = "查询类型", requiredMode = Schema.RequiredMode.REQUIRED, example = "REDIS_SET")
    @NotBlank(message = "查询类型不能为空")
    @InEnum(value = RiskFeatureLookupTypeEnum.class, message = "查询类型必须是 {value}")
    private String lookupType;

    @Schema(description = "构造 lookup key 的表达式", requiredMode = Schema.RequiredMode.REQUIRED, example = "deviceId")
    @NotBlank(message = "key 表达式不能为空")
    @Size(max = 256, message = "key 表达式长度不能超过 256 个字符")
    private String keyExpr;

    @Schema(description = "查询来源引用", requiredMode = Schema.RequiredMode.REQUIRED, example = "pulsix:list:black:device")
    @NotBlank(message = "sourceRef 不能为空")
    @Size(max = 256, message = "sourceRef 长度不能超过 256 个字符")
    private String sourceRef;

    @Schema(description = "默认值", example = "false")
    @Size(max = 256, message = "默认值长度不能超过 256 个字符")
    private String defaultValue;

    @Schema(description = "特征值类型", requiredMode = Schema.RequiredMode.REQUIRED, example = "BOOLEAN")
    @NotBlank(message = "特征值类型不能为空")
    @InEnum(value = RiskFeatureValueTypeEnum.class, message = "特征值类型必须是 {value}")
    private String valueType;

    @Schema(description = "本地缓存 TTL（秒）", example = "30")
    @Min(value = 1, message = "本地缓存 TTL 必须大于 0")
    private Long cacheTtlSeconds;

    @Schema(description = "单次 lookup 超时时间（毫秒）", example = "20")
    @Min(value = 1, message = "超时时间必须大于 0")
    private Integer timeoutMs;

    @Schema(description = "扩展配置 JSON")
    private Map<String, Object> extraJson;

    @Schema(description = "状态", requiredMode = Schema.RequiredMode.REQUIRED, example = "0")
    @NotNull(message = "状态不能为空")
    @InEnum(value = CommonStatusEnum.class, message = "状态必须是 {value}")
    private Integer status;

    @Schema(description = "特征说明")
    @Size(max = 512, message = "特征说明长度不能超过 512 个字符")
    private String description;

}
