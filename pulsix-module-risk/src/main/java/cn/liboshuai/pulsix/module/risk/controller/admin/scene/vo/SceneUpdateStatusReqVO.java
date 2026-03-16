package cn.liboshuai.pulsix.module.risk.controller.admin.scene.vo;

import cn.liboshuai.pulsix.framework.common.enums.CommonStatusEnum;
import cn.liboshuai.pulsix.framework.common.validation.InEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Schema(description = "管理后台 - 风控场景状态修改 Request VO")
@Data
public class SceneUpdateStatusReqVO {

    @Schema(description = "场景主键", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "场景主键不能为空")
    private Long id;

    @Schema(description = "状态", requiredMode = Schema.RequiredMode.REQUIRED, example = "0")
    @NotNull(message = "状态不能为空")
    @InEnum(value = CommonStatusEnum.class, message = "状态必须是 {value}")
    private Integer status;

}
