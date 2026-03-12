package cn.liboshuai.pulsix.module.risk.controller.admin.rule.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Schema(description = "管理后台 - 规则表达式校验 Response VO")
@Data
public class RuleValidateRespVO {

    @Schema(description = "是否校验通过", requiredMode = Schema.RequiredMode.REQUIRED, example = "true")
    private Boolean valid;

    @Schema(description = "校验结果消息", example = "规则表达式校验通过")
    private String message;

    @Schema(description = "命中原因模板中的非法占位符")
    private List<String> invalidPlaceholders;

}
