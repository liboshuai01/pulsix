package cn.liboshuai.pulsix.module.risk.controller.admin.simulation.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Schema(description = "管理后台 - 仿真用例新增/修改 Request VO")
@Data
public class SimulationCaseSaveReqVO {

    @Schema(description = "主键", example = "5101")
    private Long id;

    @Schema(description = "所属场景编码", requiredMode = Schema.RequiredMode.REQUIRED, example = "TRADE_RISK")
    @NotBlank(message = "所属场景不能为空")
    @Size(max = 64, message = "所属场景编码长度不能超过 64 个字符")
    private String sceneCode;

    @Schema(description = "用例编码", requiredMode = Schema.RequiredMode.REQUIRED, example = "SIM_REJECT_BLACKLIST")
    @NotBlank(message = "用例编码不能为空")
    @Size(max = 64, message = "用例编码长度不能超过 64 个字符")
    @Pattern(regexp = "^[A-Za-z][A-Za-z0-9_]*$", message = "用例编码只允许字母、数字、下划线，且必须以字母开头")
    private String caseCode;

    @Schema(description = "用例名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "设备黑名单直接拒绝")
    @NotBlank(message = "用例名称不能为空")
    @Size(max = 128, message = "用例名称长度不能超过 128 个字符")
    private String caseName;

    @Schema(description = "版本选择模式", requiredMode = Schema.RequiredMode.REQUIRED, example = "LATEST")
    @NotBlank(message = "版本选择模式不能为空")
    private String versionSelectMode;

    @Schema(description = "固定版本号；当 versionSelectMode=FIXED 时使用", example = "14")
    private Integer versionNo;

    @Schema(description = "标准事件输入 JSON", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "标准事件输入不能为空")
    private Map<String, Object> inputEventJson;

    @Schema(description = "模拟特征值 JSON，例如 {\"user_trade_cnt_5m\":3}")
    private Map<String, Object> mockFeatureJson;

    @Schema(description = "模拟 lookup 值 JSON，例如 {\"device_in_blacklist\":true}")
    private Map<String, Object> mockLookupJson;

    @Schema(description = "期望动作；留空表示不校验动作", example = "REJECT")
    @Size(max = 32, message = "期望动作长度不能超过 32 个字符")
    private String expectedAction;

    @Schema(description = "期望命中的规则编码列表", example = "[\"R001\"]")
    private List<String> expectedHitRules;

    @Schema(description = "状态", requiredMode = Schema.RequiredMode.REQUIRED, example = "0")
    @NotNull(message = "状态不能为空")
    private Integer status;

    @Schema(description = "用例说明", example = "验证黑名单设备命中后直接拒绝")
    @Size(max = 512, message = "用例说明长度不能超过 512 个字符")
    private String description;

}
