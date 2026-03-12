package cn.liboshuai.pulsix.module.risk.controller.admin.list.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;

@Schema(description = "管理后台 - 名单条目 Response VO")
@Data
public class ListItemRespVO {

    @Schema(description = "主键", example = "1")
    private Long id;

    @Schema(description = "所属场景编码", example = "TRADE_RISK")
    private String sceneCode;

    @Schema(description = "名单编码", example = "DEVICE_BLACKLIST")
    private String listCode;

    @Schema(description = "匹配键名", example = "deviceId")
    private String matchKey;

    @Schema(description = "匹配值", example = "D0009")
    private String matchValue;

    @Schema(description = "过期时间")
    private LocalDateTime expireAt;

    @Schema(description = "状态", example = "0")
    private Integer status;

    @Schema(description = "来源类型", example = "MANUAL")
    private String sourceType;

    @Schema(description = "导入批次号")
    private String batchNo;

    @Schema(description = "条目备注")
    private String remark;

    @Schema(description = "扩展信息 JSON")
    private Map<String, Object> extJson;

    @Schema(description = "创建者", example = "admin")
    private String creator;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新者", example = "admin")
    private String updater;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;

}
