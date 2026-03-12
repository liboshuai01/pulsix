package cn.liboshuai.pulsix.module.risk.controller.admin.featurestream.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Schema(description = "管理后台 - 流式特征 Response VO")
@Data
public class FeatureStreamRespVO {

    @Schema(description = "主键", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    private Long id;

    @Schema(description = "所属场景编码", requiredMode = Schema.RequiredMode.REQUIRED, example = "TRADE_RISK")
    private String sceneCode;

    @Schema(description = "特征编码", requiredMode = Schema.RequiredMode.REQUIRED, example = "user_trade_cnt_5m")
    private String featureCode;

    @Schema(description = "特征名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "用户 5 分钟交易次数")
    private String featureName;

    @Schema(description = "特征类型", requiredMode = Schema.RequiredMode.REQUIRED, example = "STREAM")
    private String featureType;

    @Schema(description = "实体类型", requiredMode = Schema.RequiredMode.REQUIRED, example = "USER")
    private String entityType;

    @Schema(description = "实体类型名称", example = "用户")
    private String entityName;

    @Schema(description = "实体主键字段", example = "userId")
    private String entityKeyFieldName;

    @Schema(description = "主要来源事件编码", example = "TRADE_EVENT")
    private String eventCode;

    @Schema(description = "来源事件编码列表")
    private List<String> sourceEventCodes;

    @Schema(description = "实体键表达式", example = "userId")
    private String entityKeyExpr;

    @Schema(description = "聚合类型", requiredMode = Schema.RequiredMode.REQUIRED, example = "COUNT")
    private String aggType;

    @Schema(description = "特征值类型", requiredMode = Schema.RequiredMode.REQUIRED, example = "LONG")
    private String valueType;

    @Schema(description = "取值表达式")
    private String valueExpr;

    @Schema(description = "过滤表达式")
    private String filterExpr;

    @Schema(description = "窗口类型", requiredMode = Schema.RequiredMode.REQUIRED, example = "SLIDING")
    private String windowType;

    @Schema(description = "窗口大小", example = "5m")
    private String windowSize;

    @Schema(description = "窗口滑动步长", example = "1m")
    private String windowSlide;

    @Schema(description = "是否计入当前事件：1-计入，0-不计入", example = "1")
    private Integer includeCurrentEvent;

    @Schema(description = "状态 TTL（秒）", example = "600")
    private Long ttlSeconds;

    @Schema(description = "状态提示 JSON")
    private Map<String, Object> stateHintJson;

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
