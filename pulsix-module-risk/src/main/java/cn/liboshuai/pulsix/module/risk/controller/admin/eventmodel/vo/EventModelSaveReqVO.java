package cn.liboshuai.pulsix.module.risk.controller.admin.eventmodel.vo;

import cn.liboshuai.pulsix.framework.common.enums.CommonStatusEnum;
import cn.liboshuai.pulsix.framework.common.validation.InEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Schema(description = "管理后台 - 事件模型创建/修改 Request VO")
@Data
public class EventModelSaveReqVO {

    @Schema(description = "事件模型主键", example = "1")
    private Long id;

    @Schema(description = "场景编码", requiredMode = Schema.RequiredMode.REQUIRED, example = "PROMOTION_RISK")
    @NotBlank(message = "场景编码不能为空")
    @Size(max = 64, message = "场景编码长度不能超过 64 个字符")
    @Pattern(regexp = "^[A-Z0-9_]+$", message = "场景编码只能包含大写字母、数字和下划线")
    private String sceneCode;

    @Schema(description = "事件编码", requiredMode = Schema.RequiredMode.REQUIRED, example = "PROMOTION_EVENT")
    @NotBlank(message = "事件编码不能为空")
    @Size(max = 64, message = "事件编码长度不能超过 64 个字符")
    @Pattern(regexp = "^[A-Z0-9_]+$", message = "事件编码只能包含大写字母、数字和下划线")
    private String eventCode;

    @Schema(description = "事件名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "营销受理事件")
    @NotBlank(message = "事件名称不能为空")
    @Size(max = 128, message = "事件名称长度不能超过 128 个字符")
    private String eventName;

    @Schema(description = "事件类型", requiredMode = Schema.RequiredMode.REQUIRED, example = "promotion_grant")
    @NotBlank(message = "事件类型不能为空")
    @Size(max = 64, message = "事件类型长度不能超过 64 个字符")
    private String eventType;

    @Schema(description = "状态", requiredMode = Schema.RequiredMode.REQUIRED, example = "0")
    @NotNull(message = "状态不能为空")
    @InEnum(value = CommonStatusEnum.class, message = "状态必须是 {value}")
    private Integer status;

    @Schema(description = "描述", example = "营销受理标准事件模型")
    @Size(max = 512, message = "描述长度不能超过 512 个字符")
    private String description;

    @Schema(description = "字段定义", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty(message = "字段定义不能为空")
    @Valid
    private List<EventFieldItemVO> fields;

}
