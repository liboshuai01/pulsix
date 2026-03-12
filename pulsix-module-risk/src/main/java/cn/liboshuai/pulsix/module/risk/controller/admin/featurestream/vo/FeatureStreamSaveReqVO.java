package cn.liboshuai.pulsix.module.risk.controller.admin.featurestream.vo;

import cn.liboshuai.pulsix.framework.common.enums.CommonStatusEnum;
import cn.liboshuai.pulsix.framework.common.validation.InEnum;
import cn.liboshuai.pulsix.module.risk.enums.feature.RiskFeatureAggTypeEnum;
import cn.liboshuai.pulsix.module.risk.enums.feature.RiskFeatureValueTypeEnum;
import cn.liboshuai.pulsix.module.risk.enums.feature.RiskFeatureWindowTypeEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Schema(description = "管理后台 - 流式特征创建/修改 Request VO")
@Data
public class FeatureStreamSaveReqVO {

    @Schema(description = "主键", example = "1")
    private Long id;

    @Schema(description = "所属场景编码", requiredMode = Schema.RequiredMode.REQUIRED, example = "TRADE_RISK")
    @NotBlank(message = "所属场景编码不能为空")
    @Size(max = 64, message = "所属场景编码长度不能超过 64 个字符")
    private String sceneCode;

    @Schema(description = "特征编码", requiredMode = Schema.RequiredMode.REQUIRED, example = "user_trade_cnt_5m")
    @NotBlank(message = "特征编码不能为空")
    @Size(max = 64, message = "特征编码长度不能超过 64 个字符")
    @Pattern(regexp = "^[A-Za-z][A-Za-z0-9_]*$", message = "特征编码只允许字母、数字、下划线，且必须以字母开头")
    private String featureCode;

    @Schema(description = "特征名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "用户 5 分钟交易次数")
    @NotBlank(message = "特征名称不能为空")
    @Size(max = 128, message = "特征名称长度不能超过 128 个字符")
    private String featureName;

    @Schema(description = "实体类型", requiredMode = Schema.RequiredMode.REQUIRED, example = "USER")
    @NotBlank(message = "实体类型不能为空")
    @Size(max = 32, message = "实体类型长度不能超过 32 个字符")
    private String entityType;

    @Schema(description = "来源事件编码列表", requiredMode = Schema.RequiredMode.REQUIRED, example = "[\"TRADE_EVENT\"]")
    @NotEmpty(message = "来源事件不能为空")
    private List<String> sourceEventCodes;

    @Schema(description = "实体键表达式", example = "userId")
    @Size(max = 256, message = "实体键表达式长度不能超过 256 个字符")
    private String entityKeyExpr;

    @Schema(description = "聚合类型", requiredMode = Schema.RequiredMode.REQUIRED, example = "COUNT")
    @NotBlank(message = "聚合类型不能为空")
    @InEnum(value = RiskFeatureAggTypeEnum.class, message = "聚合类型必须是 {value}")
    private String aggType;

    @Schema(description = "特征值类型", requiredMode = Schema.RequiredMode.REQUIRED, example = "LONG")
    @NotBlank(message = "特征值类型不能为空")
    @InEnum(value = RiskFeatureValueTypeEnum.class, message = "特征值类型必须是 {value}")
    private String valueType;

    @Schema(description = "取值表达式", example = "amount")
    @Size(max = 512, message = "取值表达式长度不能超过 512 个字符")
    private String valueExpr;

    @Schema(description = "过滤表达式", example = "result == 'SUCCESS'")
    @Size(max = 1024, message = "过滤表达式长度不能超过 1024 个字符")
    private String filterExpr;

    @Schema(description = "窗口类型", requiredMode = Schema.RequiredMode.REQUIRED, example = "SLIDING")
    @NotBlank(message = "窗口类型不能为空")
    @InEnum(value = RiskFeatureWindowTypeEnum.class, message = "窗口类型必须是 {value}")
    private String windowType;

    @Schema(description = "窗口大小", requiredMode = Schema.RequiredMode.REQUIRED, example = "5m")
    @NotBlank(message = "窗口大小不能为空")
    @Pattern(regexp = "^\\d+[smhd]$", message = "窗口大小格式必须是数字加 s/m/h/d，例如 5m")
    private String windowSize;

    @Schema(description = "窗口滑动步长", example = "1m")
    @Pattern(regexp = "^$|^\\d+[smhd]$", message = "窗口滑动步长格式必须是数字加 s/m/h/d，例如 1m")
    private String windowSlide;

    @Schema(description = "是否计入当前事件：1-计入，0-不计入", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "是否计入当前事件不能为空")
    private Integer includeCurrentEvent;

    @Schema(description = "状态 TTL（秒）", example = "600")
    @Min(value = 1, message = "状态 TTL 必须大于 0")
    private Long ttlSeconds;

    @Schema(description = "状态提示 JSON")
    private Map<String, Object> stateHintJson;

    @Schema(description = "状态", requiredMode = Schema.RequiredMode.REQUIRED, example = "0")
    @NotNull(message = "状态不能为空")
    @InEnum(value = CommonStatusEnum.class, message = "状态必须是 {value}")
    private Integer status;

    @Schema(description = "特征说明")
    @Size(max = 512, message = "特征说明长度不能超过 512 个字符")
    private String description;

}
