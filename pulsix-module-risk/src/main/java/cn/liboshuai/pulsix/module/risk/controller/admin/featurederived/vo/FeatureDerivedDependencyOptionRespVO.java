package cn.liboshuai.pulsix.module.risk.controller.admin.featurederived.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "管理后台 - 派生特征依赖候选 Response VO")
@Data
public class FeatureDerivedDependencyOptionRespVO {

    @Schema(description = "依赖编码", requiredMode = Schema.RequiredMode.REQUIRED, example = "amount")
    private String code;

    @Schema(description = "依赖名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "交易金额")
    private String name;

    @Schema(description = "依赖来源类型：FIELD、STREAM、LOOKUP、DERIVED", requiredMode = Schema.RequiredMode.REQUIRED, example = "FIELD")
    private String dependencyType;

    @Schema(description = "值类型", example = "DECIMAL")
    private String valueType;

    @Schema(description = "辅助说明")
    private String hint;

}
