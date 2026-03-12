package cn.liboshuai.pulsix.module.risk.controller.admin.policy.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Schema(description = "管理后台 - 策略规则排序 Request VO")
@Data
public class PolicySortReqVO {

    @Schema(description = "策略主键", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "策略主键不能为空")
    private Long id;

    @Schema(description = "规则编码顺序列表", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty(message = "规则顺序不能为空")
    private List<String> ruleCodes;

}
