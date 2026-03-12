package cn.liboshuai.pulsix.module.risk.controller.admin.ingestsource.vo;

import cn.liboshuai.pulsix.framework.common.enums.CommonStatusEnum;
import cn.liboshuai.pulsix.framework.common.validation.InEnum;
import cn.liboshuai.pulsix.module.risk.enums.ingestsource.RiskIngestSourceAuthTypeEnum;
import cn.liboshuai.pulsix.module.risk.enums.ingestsource.RiskIngestSourceTypeEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Schema(description = "管理后台 - 接入源创建/修改 Request VO")
@Data
public class IngestSourceSaveReqVO {

    @Schema(description = "主键", example = "1")
    private Long id;

    @Schema(description = "接入源编码", requiredMode = Schema.RequiredMode.REQUIRED, example = "trade_http_demo")
    @NotBlank(message = "接入源编码不能为空")
    @Size(max = 64, message = "接入源编码长度不能超过 64 个字符")
    @Pattern(regexp = "^[A-Za-z][A-Za-z0-9_]*$", message = "接入源编码只允许字母、数字、下划线，且必须以字母开头")
    private String sourceCode;

    @Schema(description = "接入源名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "交易 HTTP Demo")
    @NotBlank(message = "接入源名称不能为空")
    @Size(max = 128, message = "接入源名称长度不能超过 128 个字符")
    private String sourceName;

    @Schema(description = "接入方式", requiredMode = Schema.RequiredMode.REQUIRED, example = "HTTP")
    @NotBlank(message = "接入方式不能为空")
    @InEnum(value = RiskIngestSourceTypeEnum.class, message = "接入方式必须是 {value}")
    private String sourceType;

    @Schema(description = "鉴权方式", requiredMode = Schema.RequiredMode.REQUIRED, example = "HMAC")
    @NotBlank(message = "鉴权方式不能为空")
    @InEnum(value = RiskIngestSourceAuthTypeEnum.class, message = "鉴权方式必须是 {value}")
    private String authType;

    @Schema(description = "鉴权配置 JSON")
    private Map<String, Object> authConfigJson;

    @Schema(description = "允许接入的场景范围 JSON")
    private List<String> sceneScopeJson;

    @Schema(description = "标准事件 Topic", requiredMode = Schema.RequiredMode.REQUIRED, example = "pulsix.event.standard")
    @NotBlank(message = "标准事件 Topic 不能为空")
    @Size(max = 128, message = "标准事件 Topic 长度不能超过 128 个字符")
    private String standardTopicName;

    @Schema(description = "异常事件 Topic", requiredMode = Schema.RequiredMode.REQUIRED, example = "pulsix.event.dlq")
    @NotBlank(message = "异常事件 Topic 不能为空")
    @Size(max = 128, message = "异常事件 Topic 长度不能超过 128 个字符")
    private String errorTopicName;

    @Schema(description = "限流阈值 QPS，0 表示不限制", requiredMode = Schema.RequiredMode.REQUIRED, example = "300")
    @NotNull(message = "限流阈值不能为空")
    @Min(value = 0, message = "限流阈值不能小于 0")
    private Integer rateLimitQps;

    @Schema(description = "状态，见 CommonStatusEnum 枚举", requiredMode = Schema.RequiredMode.REQUIRED, example = "0")
    @NotNull(message = "状态不能为空")
    @InEnum(value = CommonStatusEnum.class, message = "状态必须是 {value}")
    private Integer status;

    @Schema(description = "接入源说明")
    @Size(max = 512, message = "接入源说明长度不能超过 512 个字符")
    private String description;

}
