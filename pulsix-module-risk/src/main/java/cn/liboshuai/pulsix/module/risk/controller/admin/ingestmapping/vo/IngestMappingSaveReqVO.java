package cn.liboshuai.pulsix.module.risk.controller.admin.ingestmapping.vo;

import cn.liboshuai.pulsix.framework.common.enums.CommonStatusEnum;
import cn.liboshuai.pulsix.framework.common.validation.InEnum;
import cn.liboshuai.pulsix.module.risk.enums.ingestmapping.RiskIngestMappingTransformTypeEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.Map;

@Schema(description = "管理后台 - 接入字段映射创建/修改 Request VO")
@Data
public class IngestMappingSaveReqVO {

    @Schema(description = "主键", example = "1")
    private Long id;

    @Schema(description = "接入源编码", requiredMode = Schema.RequiredMode.REQUIRED, example = "trade_http_demo")
    @NotBlank(message = "接入源编码不能为空")
    @Size(max = 64, message = "接入源编码长度不能超过 64 个字符")
    @Pattern(regexp = "^[A-Za-z][A-Za-z0-9_]*$", message = "接入源编码只允许字母、数字、下划线，且必须以字母开头")
    private String sourceCode;

    @Schema(description = "所属场景编码", requiredMode = Schema.RequiredMode.REQUIRED, example = "TRADE_RISK")
    @NotBlank(message = "所属场景编码不能为空")
    @Size(max = 64, message = "所属场景编码长度不能超过 64 个字符")
    private String sceneCode;

    @Schema(description = "所属事件编码", requiredMode = Schema.RequiredMode.REQUIRED, example = "TRADE_EVENT")
    @NotBlank(message = "所属事件编码不能为空")
    @Size(max = 64, message = "所属事件编码长度不能超过 64 个字符")
    private String eventCode;

    @Schema(description = "原始字段路径", example = "$.uid")
    @Size(max = 256, message = "原始字段路径长度不能超过 256 个字符")
    private String sourceFieldPath;

    @Schema(description = "目标字段编码", requiredMode = Schema.RequiredMode.REQUIRED, example = "userId")
    @NotBlank(message = "目标字段编码不能为空")
    @Size(max = 64, message = "目标字段编码长度不能超过 64 个字符")
    @Pattern(regexp = "^[A-Za-z][A-Za-z0-9_]*$", message = "目标字段编码只允许字母、数字、下划线，且必须以字母开头")
    private String targetFieldCode;

    @Schema(description = "目标字段名称", example = "用户编号")
    @Size(max = 128, message = "目标字段名称长度不能超过 128 个字符")
    private String targetFieldName;

    @Schema(description = "转换类型", requiredMode = Schema.RequiredMode.REQUIRED, example = "DIRECT")
    @NotBlank(message = "转换类型不能为空")
    @InEnum(value = RiskIngestMappingTransformTypeEnum.class, message = "转换类型必须是 {value}")
    private String transformType;

    @Schema(description = "转换表达式或常量值")
    @Size(max = 1024, message = "转换表达式长度不能超过 1024 个字符")
    private String transformExpr;

    @Schema(description = "默认值")
    @Size(max = 256, message = "默认值长度不能超过 256 个字符")
    private String defaultValue;

    @Schema(description = "是否必填：1-必填，0-非必填", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "是否必填不能为空")
    @Min(value = 0, message = "是否必填取值不正确")
    @Max(value = 1, message = "是否必填取值不正确")
    private Integer requiredFlag;

    @Schema(description = "清洗规则 JSON")
    private Map<String, Object> cleanRuleJson;

    @Schema(description = "排序号", requiredMode = Schema.RequiredMode.REQUIRED, example = "10")
    @NotNull(message = "排序号不能为空")
    private Integer sortNo;

    @Schema(description = "状态，见 CommonStatusEnum 枚举", requiredMode = Schema.RequiredMode.REQUIRED, example = "0")
    @NotNull(message = "状态不能为空")
    @InEnum(value = CommonStatusEnum.class, message = "状态必须是 {value}")
    private Integer status;

}
