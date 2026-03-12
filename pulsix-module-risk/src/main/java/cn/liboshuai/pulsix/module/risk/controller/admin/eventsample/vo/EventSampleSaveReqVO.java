package cn.liboshuai.pulsix.module.risk.controller.admin.eventsample.vo;

import cn.liboshuai.pulsix.framework.common.enums.CommonStatusEnum;
import cn.liboshuai.pulsix.framework.common.validation.InEnum;
import cn.liboshuai.pulsix.module.risk.enums.eventsample.RiskEventSampleTypeEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.Map;

@Schema(description = "管理后台 - 事件样例创建/修改 Request VO")
@Data
public class EventSampleSaveReqVO {

    @Schema(description = "主键", example = "1")
    private Long id;

    @Schema(description = "所属场景编码", requiredMode = Schema.RequiredMode.REQUIRED, example = "TRADE_RISK")
    @NotBlank(message = "所属场景编码不能为空")
    @Size(max = 64, message = "所属场景编码长度不能超过 64 个字符")
    private String sceneCode;

    @Schema(description = "所属事件编码", requiredMode = Schema.RequiredMode.REQUIRED, example = "TRADE_EVENT")
    @NotBlank(message = "所属事件编码不能为空")
    @Size(max = 64, message = "所属事件编码长度不能超过 64 个字符")
    private String eventCode;

    @Schema(description = "样例编码", requiredMode = Schema.RequiredMode.REQUIRED, example = "TRADE_STD_SUCCESS")
    @NotBlank(message = "样例编码不能为空")
    @Size(max = 64, message = "样例编码长度不能超过 64 个字符")
    @Pattern(regexp = "^[A-Za-z][A-Za-z0-9_]*$", message = "样例编码只允许字母、数字、下划线，且必须以字母开头")
    private String sampleCode;

    @Schema(description = "样例名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "交易成功标准样例")
    @NotBlank(message = "样例名称不能为空")
    @Size(max = 128, message = "样例名称长度不能超过 128 个字符")
    private String sampleName;

    @Schema(description = "样例类型", requiredMode = Schema.RequiredMode.REQUIRED, example = "STANDARD")
    @NotBlank(message = "样例类型不能为空")
    @InEnum(value = RiskEventSampleTypeEnum.class, message = "样例类型必须是 {value}")
    private String sampleType;

    @Schema(description = "来源接入源编码", example = "trade_http_demo")
    @Size(max = 64, message = "来源接入源编码长度不能超过 64 个字符")
    private String sourceCode;

    @Schema(description = "样例报文 JSON", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "样例报文 JSON 不能为空")
    private Map<String, Object> sampleJson;

    @Schema(description = "样例说明")
    @Size(max = 512, message = "样例说明长度不能超过 512 个字符")
    private String description;

    @Schema(description = "排序号", requiredMode = Schema.RequiredMode.REQUIRED, example = "10")
    @NotNull(message = "排序号不能为空")
    private Integer sortNo;

    @Schema(description = "状态，见 CommonStatusEnum 枚举", requiredMode = Schema.RequiredMode.REQUIRED, example = "0")
    @NotNull(message = "状态不能为空")
    @InEnum(value = CommonStatusEnum.class, message = "状态必须是 {value}")
    private Integer status;

}
