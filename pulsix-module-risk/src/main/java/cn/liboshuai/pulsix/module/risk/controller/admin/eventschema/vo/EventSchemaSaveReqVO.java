package cn.liboshuai.pulsix.module.risk.controller.admin.eventschema.vo;

import cn.liboshuai.pulsix.framework.common.validation.InEnum;
import cn.liboshuai.pulsix.module.risk.enums.eventschema.RiskEventSchemaSourceTypeEnum;
import cn.liboshuai.pulsix.module.risk.enums.eventschema.RiskEventSchemaTypeEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Schema(description = "管理后台 - 事件 Schema 创建/修改 Request VO")
@Data
public class EventSchemaSaveReqVO {

    @Schema(description = "主键", example = "1")
    private Long id;

    @Schema(description = "所属场景编码", requiredMode = Schema.RequiredMode.REQUIRED, example = "TRADE_RISK")
    @NotBlank(message = "所属场景编码不能为空")
    @Size(max = 64, message = "所属场景编码长度不能超过 64 个字符")
    private String sceneCode;

    @Schema(description = "事件编码", requiredMode = Schema.RequiredMode.REQUIRED, example = "TRADE_EVENT")
    @NotBlank(message = "事件编码不能为空")
    @Size(max = 64, message = "事件编码长度不能超过 64 个字符")
    @Pattern(regexp = "^[A-Za-z][A-Za-z0-9_]*$", message = "事件编码只允许字母、数字、下划线，且必须以字母开头")
    private String eventCode;

    @Schema(description = "事件名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "交易标准事件")
    @NotBlank(message = "事件名称不能为空")
    @Size(max = 128, message = "事件名称长度不能超过 128 个字符")
    private String eventName;

    @Schema(description = "事件类别", requiredMode = Schema.RequiredMode.REQUIRED, example = "BUSINESS")
    @NotBlank(message = "事件类别不能为空")
    @InEnum(value = RiskEventSchemaTypeEnum.class, message = "事件类别必须是 {value}")
    private String eventType;

    @Schema(description = "默认接入方式", requiredMode = Schema.RequiredMode.REQUIRED, example = "MIXED")
    @NotBlank(message = "默认接入方式不能为空")
    @InEnum(value = RiskEventSchemaSourceTypeEnum.class, message = "默认接入方式必须是 {value}")
    private String sourceType;

    @Schema(description = "原始事件 Topic", example = "pulsix.event.raw.trade")
    @Size(max = 128, message = "原始事件 Topic 长度不能超过 128 个字符")
    private String rawTopicName;

    @Schema(description = "标准事件 Topic", example = "pulsix.event.standard")
    @Size(max = 128, message = "标准事件 Topic 长度不能超过 128 个字符")
    private String standardTopicName;

    @Schema(description = "事件模型说明")
    @Size(max = 512, message = "事件模型说明长度不能超过 512 个字符")
    private String description;

}
