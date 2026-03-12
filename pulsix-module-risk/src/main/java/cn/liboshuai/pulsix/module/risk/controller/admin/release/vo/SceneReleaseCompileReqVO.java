package cn.liboshuai.pulsix.module.risk.controller.admin.release.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Schema(description = "管理后台 - 发布预检/编译 Request VO")
@Data
public class SceneReleaseCompileReqVO {

    @Schema(description = "所属场景编码", requiredMode = Schema.RequiredMode.REQUIRED, example = "TRADE_RISK")
    @NotBlank(message = "所属场景编码不能为空")
    @Size(max = 64, message = "所属场景编码长度不能超过 64 个字符")
    private String sceneCode;

    @Schema(description = "候选版本说明", example = "S13 发布预检候选版本")
    @Size(max = 512, message = "候选版本说明长度不能超过 512 个字符")
    private String remark;

}
