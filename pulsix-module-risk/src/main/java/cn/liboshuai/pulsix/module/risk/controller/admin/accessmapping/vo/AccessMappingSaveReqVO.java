package cn.liboshuai.pulsix.module.risk.controller.admin.accessmapping.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Schema(description = "管理后台 - 接入映射创建/修改 Request VO")
@Data
public class AccessMappingSaveReqVO {

    @Schema(description = "接入映射主键", example = "14101")
    private Long id;

    @Schema(description = "事件编码", requiredMode = Schema.RequiredMode.REQUIRED, example = "PROMOTION_EVENT")
    @NotBlank(message = "事件编码不能为空")
    @Size(max = 64, message = "事件编码长度不能超过 64 个字符")
    @Pattern(regexp = "^[A-Z0-9_]+$", message = "事件编码只能包含大写字母、数字和下划线")
    private String eventCode;

    @Schema(description = "接入源编码", requiredMode = Schema.RequiredMode.REQUIRED, example = "PROMOTION_CENTER_HTTP")
    @NotBlank(message = "接入源编码不能为空")
    @Size(max = 64, message = "接入源编码长度不能超过 64 个字符")
    @Pattern(regexp = "^[A-Z0-9_]+$", message = "接入源编码只能包含大写字母、数字和下划线")
    private String sourceCode;

    @Schema(description = "描述", example = "营销中心受理事件接入映射")
    @Size(max = 512, message = "描述长度不能超过 512 个字符")
    private String description;

    @Schema(description = "原始样例报文", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "原始样例报文不能为空")
    private Map<String, Object> rawSampleJson;

    @Schema(description = "样例请求头")
    private Map<String, Object> sampleHeadersJson;

    @Schema(description = "原始字段定义", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty(message = "原始字段定义不能为空")
    @Valid
    private List<AccessRawFieldItemVO> rawFields;

    @Schema(description = "标准化映射规则", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty(message = "标准化映射规则不能为空")
    @Valid
    private List<AccessMappingRuleItemVO> mappingRules;

}
