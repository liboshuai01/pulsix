package cn.liboshuai.pulsix.module.risk.controller.admin.featurederived.vo;

import cn.liboshuai.pulsix.framework.common.enums.CommonStatusEnum;
import cn.liboshuai.pulsix.framework.common.validation.InEnum;
import cn.liboshuai.pulsix.module.risk.enums.feature.RiskFeatureDerivedEngineTypeEnum;
import cn.liboshuai.pulsix.module.risk.enums.feature.RiskFeatureValueTypeEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Schema(description = "管理后台 - 派生特征创建/修改 Request VO")
@Data
public class FeatureDerivedSaveReqVO {

    @Schema(description = "主键", example = "1")
    private Long id;

    @Schema(description = "所属场景编码", requiredMode = Schema.RequiredMode.REQUIRED, example = "TRADE_RISK")
    @NotBlank(message = "所属场景编码不能为空")
    @Size(max = 64, message = "所属场景编码长度不能超过 64 个字符")
    private String sceneCode;

    @Schema(description = "特征编码", requiredMode = Schema.RequiredMode.REQUIRED, example = "high_amt_flag")
    @NotBlank(message = "特征编码不能为空")
    @Size(max = 64, message = "特征编码长度不能超过 64 个字符")
    @Pattern(regexp = "^[A-Za-z][A-Za-z0-9_]*$", message = "特征编码只允许字母、数字、下划线，且必须以字母开头")
    private String featureCode;

    @Schema(description = "特征名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "高金额标记")
    @NotBlank(message = "特征名称不能为空")
    @Size(max = 128, message = "特征名称长度不能超过 128 个字符")
    private String featureName;

    @Schema(description = "表达式引擎类型", requiredMode = Schema.RequiredMode.REQUIRED, example = "AVIATOR")
    @NotBlank(message = "表达式引擎类型不能为空")
    @InEnum(value = RiskFeatureDerivedEngineTypeEnum.class, message = "表达式引擎类型必须是 {value}")
    private String engineType;

    @Schema(description = "表达式内容", requiredMode = Schema.RequiredMode.REQUIRED, example = "amount >= 5000")
    @NotBlank(message = "表达式内容不能为空")
    private String exprContent;

    @Schema(description = "依赖项编码列表", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty(message = "依赖项不能为空")
    private List<String> dependsOnJson;

    @Schema(description = "派生特征值类型", requiredMode = Schema.RequiredMode.REQUIRED, example = "BOOLEAN")
    @NotBlank(message = "派生特征值类型不能为空")
    @InEnum(value = RiskFeatureValueTypeEnum.class, message = "派生特征值类型必须是 {value}")
    private String valueType;

    @Schema(description = "是否启用脚本沙箱：1-启用，0-关闭", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "脚本沙箱不能为空")
    private Integer sandboxFlag;

    @Schema(description = "执行超时时间（毫秒）", example = "50")
    @Min(value = 1, message = "执行超时时间必须大于 0")
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
