package cn.liboshuai.pulsix.module.risk.controller.admin.replay.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Schema(description = "管理后台 - 回放任务执行 Request VO")
@Data
public class ReplayJobExecuteReqVO {

    @Schema(description = "任务编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "11001")
    @NotNull(message = "任务编号不能为空")
    private Long id;

}
