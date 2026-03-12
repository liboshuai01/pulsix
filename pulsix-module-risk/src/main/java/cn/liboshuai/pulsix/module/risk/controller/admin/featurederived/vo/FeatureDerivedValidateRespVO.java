package cn.liboshuai.pulsix.module.risk.controller.admin.featurederived.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Schema(description = "管理后台 - 派生特征表达式校验 Response VO")
@Data
public class FeatureDerivedValidateRespVO {

    @Schema(description = "是否校验通过", requiredMode = Schema.RequiredMode.REQUIRED, example = "true")
    private Boolean valid;

    @Schema(description = "校验结果消息", example = "表达式校验通过")
    private String message;

    @Schema(description = "缺失依赖项")
    private List<String> missingDependencies;

    @Schema(description = "是否检测到循环依赖", example = "false")
    private Boolean cycleDetected;

}
