package cn.liboshuai.pulsix.module.risk.controller.admin.simulation.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Schema(description = "管理后台 - 执行仿真 Request VO")
@Data
public class SimulationExecuteReqVO {

    @Schema(description = "仿真用例主键", requiredMode = Schema.RequiredMode.REQUIRED, example = "5101")
    @NotNull(message = "仿真用例不能为空")
    private Long caseId;

}
