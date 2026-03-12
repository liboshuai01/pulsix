package cn.liboshuai.pulsix.module.risk.controller.admin.featurederived.vo;

import cn.liboshuai.pulsix.framework.common.validation.InEnum;
import cn.liboshuai.pulsix.module.risk.enums.feature.RiskFeatureDerivedEngineTypeEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Schema(description = "管理后台 - 派生特征表达式校验 Request VO")
@Data
public class FeatureDerivedValidateReqVO {

    @Schema(description = "所属场景编码", requiredMode = Schema.RequiredMode.REQUIRED, example = "TRADE_RISK")
    @NotBlank(message = "所属场景编码不能为空")
    private String sceneCode;

    @Schema(description = "当前特征编码", example = "trade_burst_flag")
    private String featureCode;

    @Schema(description = "表达式引擎类型", requiredMode = Schema.RequiredMode.REQUIRED, example = "AVIATOR")
    @NotBlank(message = "表达式引擎类型不能为空")
    @InEnum(value = RiskFeatureDerivedEngineTypeEnum.class, message = "表达式引擎类型必须是 {value}")
    private String engineType;

    @Schema(description = "表达式内容", requiredMode = Schema.RequiredMode.REQUIRED, example = "user_trade_cnt_5m >= 3 && amount >= 5000")
    @NotBlank(message = "表达式内容不能为空")
    private String exprContent;

    @Schema(description = "依赖项编码列表", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty(message = "依赖项不能为空")
    private List<String> dependsOnJson;

    @Schema(description = "是否启用脚本沙箱：1-启用，0-关闭", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "脚本沙箱不能为空")
    private Integer sandboxFlag;

}
