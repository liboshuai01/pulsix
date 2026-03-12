package cn.liboshuai.pulsix.module.risk.controller.admin.list.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - 名单集合 Response VO")
@Data
public class ListSetRespVO {

    @Schema(description = "主键", example = "1")
    private Long id;

    @Schema(description = "所属场景编码", example = "TRADE_RISK")
    private String sceneCode;

    @Schema(description = "名单编码", example = "DEVICE_BLACKLIST")
    private String listCode;

    @Schema(description = "名单名称", example = "设备黑名单")
    private String listName;

    @Schema(description = "匹配维度", example = "DEVICE")
    private String matchType;

    @Schema(description = "名单类型", example = "BLACK")
    private String listType;

    @Schema(description = "运行时存储形式", example = "REDIS_SET")
    private String storageType;

    @Schema(description = "同步模式", example = "FULL")
    private String syncMode;

    @Schema(description = "同步状态", example = "PENDING")
    private String syncStatus;

    @Schema(description = "最近同步时间")
    private LocalDateTime lastSyncTime;

    @Schema(description = "状态", example = "0")
    private Integer status;

    @Schema(description = "名单说明")
    private String description;

    @Schema(description = "Redis Key 前缀", example = "pulsix:list:black:device")
    private String redisKeyPrefix;

    @Schema(description = "创建者", example = "admin")
    private String creator;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新者", example = "admin")
    private String updater;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;

}
