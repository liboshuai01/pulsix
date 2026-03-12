package cn.liboshuai.pulsix.module.risk.controller.admin.featurederived.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Schema(description = "管理后台 - 派生特征 Response VO")
@Data
public class FeatureDerivedRespVO {

    @Schema(description = "主键", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    private Long id;

    @Schema(description = "所属场景编码", requiredMode = Schema.RequiredMode.REQUIRED, example = "TRADE_RISK")
    private String sceneCode;

    @Schema(description = "特征编码", requiredMode = Schema.RequiredMode.REQUIRED, example = "high_amt_flag")
    private String featureCode;

    @Schema(description = "特征名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "高金额标记")
    private String featureName;

    @Schema(description = "特征类型", requiredMode = Schema.RequiredMode.REQUIRED, example = "DERIVED")
    private String featureType;

    @Schema(description = "表达式引擎类型", requiredMode = Schema.RequiredMode.REQUIRED, example = "AVIATOR")
    private String engineType;

    @Schema(description = "表达式内容")
    private String exprContent;

    @Schema(description = "依赖项编码列表")
    private List<String> dependsOnJson;

    @Schema(description = "派生特征值类型", requiredMode = Schema.RequiredMode.REQUIRED, example = "BOOLEAN")
    private String valueType;

    @Schema(description = "是否启用脚本沙箱：1-启用，0-关闭", example = "1")
    private Integer sandboxFlag;

    @Schema(description = "执行超时时间（毫秒）", example = "50")
    private Integer timeoutMs;

    @Schema(description = "扩展配置 JSON")
    private Map<String, Object> extraJson;

    @Schema(description = "状态", example = "0")
    private Integer status;

    @Schema(description = "设计态版本", example = "1")
    private Integer version;

    @Schema(description = "特征说明")
    private String description;

    @Schema(description = "创建者")
    private String creator;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新者")
    private String updater;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;

}
