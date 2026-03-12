package cn.liboshuai.pulsix.module.risk.controller.admin.list.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "管理后台 - 名单同步 Response VO")
@Data
public class ListSyncRespVO {

    @Schema(description = "名单编码", example = "DEVICE_BLACKLIST")
    private String listCode;

    @Schema(description = "Redis Key 前缀", example = "pulsix:list:black:device")
    private String redisKeyPrefix;

    @Schema(description = "本次同步条目数", example = "2")
    private Integer syncedItemCount;

    @Schema(description = "运行时存储形式", example = "REDIS_SET")
    private String storageType;

}
