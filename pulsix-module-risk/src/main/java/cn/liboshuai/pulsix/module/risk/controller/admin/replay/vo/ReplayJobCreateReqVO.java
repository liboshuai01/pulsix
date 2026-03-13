package cn.liboshuai.pulsix.module.risk.controller.admin.replay.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Schema(description = "管理后台 - 回放任务创建 Request VO")
@Data
public class ReplayJobCreateReqVO {

    @Schema(description = "所属场景编码", requiredMode = Schema.RequiredMode.REQUIRED, example = "TRADE_RISK")
    @NotBlank(message = "所属场景编码不能为空")
    private String sceneCode;

    @Schema(description = "基线版本号", requiredMode = Schema.RequiredMode.REQUIRED, example = "14")
    @NotNull(message = "基线版本号不能为空")
    private Integer baselineVersionNo;

    @Schema(description = "目标版本号", requiredMode = Schema.RequiredMode.REQUIRED, example = "15")
    @NotNull(message = "目标版本号不能为空")
    private Integer targetVersionNo;

    @Schema(description = "输入源类型，支持 DECISION_LOG_EXPORT、FILE、KAFKA_EXPORT", requiredMode = Schema.RequiredMode.REQUIRED, example = "FILE")
    @NotBlank(message = "输入源类型不能为空")
    private String inputSourceType;

    @Schema(description = "输入源引用，可填写 decision_log 编号列表、classpath:file 资源、服务端文件路径，或直接粘贴 JSON 文本", example = "classpath:risk/replay/trade-risk-events.json")
    private String inputRef;

    @Schema(description = "任务备注")
    private String remark;

}
