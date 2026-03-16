package cn.liboshuai.pulsix.module.risk.controller.admin.eventmodel.vo;

import cn.liboshuai.pulsix.framework.common.validation.InEnum;
import cn.liboshuai.pulsix.module.risk.enums.eventmodel.EventFieldTypeEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.Map;

@Schema(description = "管理后台 - 事件模型字段项")
@Data
public class EventFieldItemVO {

    @Schema(description = "字段名", requiredMode = Schema.RequiredMode.REQUIRED, example = "userId")
    @NotBlank(message = "字段名不能为空")
    @Size(max = 64, message = "字段名长度不能超过 64 个字符")
    @Pattern(regexp = "^[A-Za-z][A-Za-z0-9_]*$", message = "字段名只能以字母开头，且仅支持字母、数字和下划线")
    private String fieldName;

    @Schema(description = "字段显示名", example = "用户ID")
    @Size(max = 128, message = "字段显示名长度不能超过 128 个字符")
    private String fieldLabel;

    @Schema(description = "字段类型", requiredMode = Schema.RequiredMode.REQUIRED, example = "STRING")
    @NotBlank(message = "字段类型不能为空")
    @InEnum(value = EventFieldTypeEnum.class, message = "字段类型必须是 {value}")
    private String fieldType;

    @Schema(description = "是否必填", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "必填标识不能为空")
    @Min(value = 0, message = "必填标识只能是 0 或 1")
    @Max(value = 1, message = "必填标识只能是 0 或 1")
    private Integer requiredFlag;

    @Schema(description = "默认值", example = "APP")
    @Size(max = 256, message = "默认值长度不能超过 256 个字符")
    private String defaultValue;

    @Schema(description = "样例值", example = "U10001")
    @Size(max = 512, message = "样例值长度不能超过 512 个字符")
    private String sampleValue;

    @Schema(description = "描述", example = "用户主键")
    @Size(max = 512, message = "描述长度不能超过 512 个字符")
    private String description;

    @Schema(description = "排序号", example = "10")
    @Min(value = 0, message = "排序号不能小于 0")
    private Integer sortNo;

    @Schema(description = "扩展配置")
    private Map<String, Object> extJson;

}
