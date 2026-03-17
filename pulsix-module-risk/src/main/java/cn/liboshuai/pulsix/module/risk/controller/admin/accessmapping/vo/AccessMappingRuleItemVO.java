package cn.liboshuai.pulsix.module.risk.controller.admin.accessmapping.vo;

import cn.liboshuai.pulsix.framework.common.validation.InEnum;
import cn.liboshuai.pulsix.module.risk.enums.accessmapping.AccessMappingTypeEnum;
import cn.liboshuai.pulsix.module.risk.enums.accessmapping.AccessScriptEngineEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.Map;

@Schema(description = "管理后台 - 接入映射规则项")
@Data
public class AccessMappingRuleItemVO {

    private static final String FIELD_NAME_PATTERN = "^[A-Za-z][A-Za-z0-9_]*$";
    private static final String FIELD_PATH_PATTERN = "^[A-Za-z_][A-Za-z0-9_]*(\\[[0-9]+\\])?(\\.[A-Za-z_][A-Za-z0-9_]*(\\[[0-9]+\\])?)*$";

    @Schema(description = "目标标准字段名", requiredMode = Schema.RequiredMode.REQUIRED, example = "userId")
    @NotBlank(message = "目标标准字段名不能为空")
    @Size(max = 64, message = "目标标准字段名长度不能超过 64 个字符")
    @Pattern(regexp = FIELD_NAME_PATTERN, message = "目标标准字段名只能以字母开头，且仅支持字母、数字和下划线")
    private String targetFieldName;

    @Schema(description = "映射类型", requiredMode = Schema.RequiredMode.REQUIRED, example = "SOURCE_FIELD")
    @NotBlank(message = "映射类型不能为空")
    @InEnum(value = AccessMappingTypeEnum.class, message = "映射类型必须是 {value}")
    private String mappingType;

    @Schema(description = "源字段路径", example = "user.id")
    @Size(max = 255, message = "源字段路径长度不能超过 255 个字符")
    @Pattern(regexp = FIELD_PATH_PATTERN, message = "源字段路径格式不正确，仅支持 a.b.c 或 items[0].skuId")
    private String sourceFieldPath;

    @Schema(description = "常量值", example = "PROMOTION_RISK")
    @Size(max = 512, message = "常量值长度不能超过 512 个字符")
    private String constantValue;

    @Schema(description = "脚本引擎", example = "AVIATOR")
    @InEnum(value = AccessScriptEngineEnum.class, message = "脚本引擎必须是 {value}")
    private String scriptEngine;

    @Schema(description = "脚本内容", example = "rawPayload['amount']")
    private String scriptContent;

    @Schema(description = "时间格式", example = "yyyy-MM-dd'T'HH:mm:ss")
    @Size(max = 128, message = "时间格式长度不能超过 128 个字符")
    private String timePattern;

    @Schema(description = "枚举映射")
    private Map<String, String> enumMappingJson;

    @Schema(description = "描述", example = "映射用户ID")
    @Size(max = 512, message = "映射规则描述长度不能超过 512 个字符")
    private String description;

}
