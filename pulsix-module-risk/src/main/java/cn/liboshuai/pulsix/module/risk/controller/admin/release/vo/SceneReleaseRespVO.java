package cn.liboshuai.pulsix.module.risk.controller.admin.release.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;

@Schema(description = "管理后台 - 发布记录 Response VO")
@Data
public class SceneReleaseRespVO {

    @Schema(description = "主键", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    private Long id;

    @Schema(description = "所属场景编码", requiredMode = Schema.RequiredMode.REQUIRED, example = "TRADE_RISK")
    private String sceneCode;

    @Schema(description = "版本号", requiredMode = Schema.RequiredMode.REQUIRED, example = "13")
    private Integer versionNo;

    @Schema(description = "快照 JSON")
    private Map<String, Object> snapshotJson;

    @Schema(description = "快照摘要", example = "3d20b3dc601b9d3e0de7d2ca40488760")
    private String checksum;

    @Schema(description = "发布状态", example = "DRAFT")
    private String publishStatus;

    @Schema(description = "预检状态", example = "PASSED")
    private String validationStatus;

    @Schema(description = "预检报告 JSON")
    private Map<String, Object> validationReportJson;

    @Schema(description = "依赖摘要 JSON")
    private Map<String, Object> dependencyDigestJson;

    @Schema(description = "编译耗时（毫秒）", example = "48")
    private Long compileDurationMs;

    @Schema(description = "编译出的特征数量", example = "7")
    private Integer compiledFeatureCount;

    @Schema(description = "编译出的规则数量", example = "3")
    private Integer compiledRuleCount;

    @Schema(description = "编译出的策略数量", example = "1")
    private Integer compiledPolicyCount;

    @Schema(description = "发布人")
    private String publishedBy;

    @Schema(description = "发布时间")
    private LocalDateTime publishedAt;

    @Schema(description = "生效时间")
    private LocalDateTime effectiveFrom;

    @Schema(description = "回滚来源版本号")
    private Integer rollbackFromVersion;

    @Schema(description = "版本说明")
    private String remark;

    @Schema(description = "创建者")
    private String creator;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新者")
    private String updater;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;

}
