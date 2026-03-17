package cn.liboshuai.pulsix.module.risk.controller.admin.accesssource.vo;

import cn.liboshuai.pulsix.framework.common.enums.CommonStatusEnum;
import cn.liboshuai.pulsix.framework.common.validation.InEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Schema(description = "管理后台 - 接入源更新状态 Request VO")
@Data
public class AccessSourceUpdateStatusReqVO {

    @Schema(description = "接入源编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "14001")
    @NotNull(message = "接入源编号不能为空")
    private Long id;

    @Schema(description = "状态", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "状态不能为空")
    @InEnum(value = CommonStatusEnum.class, message = "状态必须是 {value}")
    private Integer status;

}
