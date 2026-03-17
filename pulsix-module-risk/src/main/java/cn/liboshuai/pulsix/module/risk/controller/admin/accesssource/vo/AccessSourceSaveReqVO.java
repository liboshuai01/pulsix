package cn.liboshuai.pulsix.module.risk.controller.admin.accesssource.vo;

import cn.liboshuai.pulsix.framework.common.enums.CommonStatusEnum;
import cn.liboshuai.pulsix.framework.common.validation.InEnum;
import cn.liboshuai.pulsix.framework.dict.validation.InDict;
import cn.liboshuai.pulsix.module.risk.enums.DictTypeConstants;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Schema(description = "管理后台 - 接入源创建/修改 Request VO")
@Data
public class AccessSourceSaveReqVO {

    @Schema(description = "接入源主键", example = "14001")
    private Long id;

    @Schema(description = "接入源编码", requiredMode = Schema.RequiredMode.REQUIRED, example = "ORDER_CENTER_SDK")
    @NotBlank(message = "接入源编码不能为空")
    @Size(max = 64, message = "接入源编码长度不能超过 64 个字符")
    @Pattern(regexp = "^[A-Z0-9_]+$", message = "接入源编码只能包含大写字母、数字和下划线")
    private String sourceCode;

    @Schema(description = "接入源名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "订单中心 SDK 接入")
    @NotBlank(message = "接入源名称不能为空")
    @Size(max = 128, message = "接入源名称长度不能超过 128 个字符")
    private String sourceName;

    @Schema(description = "接入源类型", requiredMode = Schema.RequiredMode.REQUIRED, example = "SDK")
    @NotBlank(message = "接入源类型不能为空")
    @InDict(type = DictTypeConstants.RISK_ACCESS_SOURCE_TYPE)
    private String sourceType;

    @Schema(description = "标准事件 Topic", requiredMode = Schema.RequiredMode.REQUIRED, example = "pulsix.event.standard")
    @NotBlank(message = "标准事件 Topic 不能为空")
    @InDict(type = DictTypeConstants.RISK_ACCESS_TOPIC_NAME)
    private String topicName;

    @Schema(description = "接入协议", requiredMode = Schema.RequiredMode.REQUIRED, example = "TCP")
    @NotBlank(message = "接入协议不能为空")
    @Size(max = 32, message = "接入协议长度不能超过 32 个字符")
    private String accessProtocol;

    @Schema(description = "应用标识", example = "order-center")
    @Size(max = 64, message = "应用标识长度不能超过 64 个字符")
    private String appId;

    @Schema(description = "负责人", example = "王五")
    @Size(max = 64, message = "负责人长度不能超过 64 个字符")
    private String ownerName;

    @Schema(description = "联系邮箱", example = "order-risk@example.com")
    @Size(max = 128, message = "联系邮箱长度不能超过 128 个字符")
    @Email(message = "联系邮箱格式不正确")
    private String contactEmail;

    @Schema(description = "限流 QPS", example = "500")
    @Positive(message = "限流 QPS 必须大于 0")
    private Integer rateLimitQps;

    @Schema(description = "允许场景编码列表", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty(message = "允许场景不能为空")
    private List<String> allowedSceneCodes;

    @Schema(description = "IP 白名单")
    private List<String> ipWhitelist;

    @Schema(description = "状态", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "状态不能为空")
    @InEnum(value = CommonStatusEnum.class, message = "状态必须是 {value}")
    private Integer status;

    @Schema(description = "描述", example = "服务订单支付事件的后端 SDK 接入源")
    @Size(max = 512, message = "描述长度不能超过 512 个字符")
    private String description;

}
