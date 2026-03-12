package cn.liboshuai.pulsix.module.risk.controller.admin.eventfield.vo;

import cn.liboshuai.pulsix.framework.common.validation.InEnum;
import cn.liboshuai.pulsix.module.risk.enums.eventfield.RiskEventFieldTypeEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Schema(description = "管理后台 - 事件字段创建/修改 Request VO")
@Data
public class EventFieldSaveReqVO {

    @Schema(description = "主键", example = "1")
    private Long id;

    @Schema(description = "所属场景编码", requiredMode = Schema.RequiredMode.REQUIRED, example = "TRADE_RISK")
    @NotBlank(message = "所属场景编码不能为空")
    @Size(max = 64, message = "所属场景编码长度不能超过 64 个字符")
    private String sceneCode;

    @Schema(description = "所属事件编码", requiredMode = Schema.RequiredMode.REQUIRED, example = "TRADE_EVENT")
    @NotBlank(message = "所属事件编码不能为空")
    @Size(max = 64, message = "所属事件编码长度不能超过 64 个字符")
    private String eventCode;

    @Schema(description = "字段编码", requiredMode = Schema.RequiredMode.REQUIRED, example = "userId")
    @NotBlank(message = "字段编码不能为空")
    @Size(max = 64, message = "字段编码长度不能超过 64 个字符")
    @Pattern(regexp = "^[A-Za-z][A-Za-z0-9_]*$", message = "字段编码只允许字母、数字、下划线，且必须以字母开头")
    private String fieldCode;

    @Schema(description = "字段名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "用户编号")
    @NotBlank(message = "字段名称不能为空")
    @Size(max = 128, message = "字段名称长度不能超过 128 个字符")
    private String fieldName;

    @Schema(description = "字段类型", requiredMode = Schema.RequiredMode.REQUIRED, example = "STRING")
    @NotBlank(message = "字段类型不能为空")
    @InEnum(value = RiskEventFieldTypeEnum.class, message = "字段类型必须是 {value}")
    private String fieldType;

    @Schema(description = "字段 JSONPath", example = "$.userId")
    @Size(max = 256, message = "字段 JSONPath 长度不能超过 256 个字符")
    private String fieldPath;

    @Schema(description = "是否标准公共字段：1-是，0-否", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "是否标准公共字段不能为空")
    @Min(value = 0, message = "是否标准公共字段取值不正确")
    @Max(value = 1, message = "是否标准公共字段取值不正确")
    private Integer standardFieldFlag;

    @Schema(description = "是否必填：1-必填，0-非必填", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "是否必填不能为空")
    @Min(value = 0, message = "是否必填取值不正确")
    @Max(value = 1, message = "是否必填取值不正确")
    private Integer requiredFlag;

    @Schema(description = "是否允许为空：1-允许，0-不允许", requiredMode = Schema.RequiredMode.REQUIRED, example = "0")
    @NotNull(message = "是否允许为空不能为空")
    @Min(value = 0, message = "是否允许为空取值不正确")
    @Max(value = 1, message = "是否允许为空取值不正确")
    private Integer nullableFlag;

    @Schema(description = "默认值")
    @Size(max = 256, message = "默认值长度不能超过 256 个字符")
    private String defaultValue;

    @Schema(description = "示例值")
    @Size(max = 512, message = "示例值长度不能超过 512 个字符")
    private String sampleValue;

    @Schema(description = "字段说明")
    @Size(max = 512, message = "字段说明长度不能超过 512 个字符")
    private String description;

    @Schema(description = "排序号", requiredMode = Schema.RequiredMode.REQUIRED, example = "10")
    @NotNull(message = "排序号不能为空")
    private Integer sortNo;

}
