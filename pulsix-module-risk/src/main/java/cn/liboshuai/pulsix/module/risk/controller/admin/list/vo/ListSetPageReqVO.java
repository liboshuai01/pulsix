package cn.liboshuai.pulsix.module.risk.controller.admin.list.vo;

import cn.liboshuai.pulsix.framework.common.enums.CommonStatusEnum;
import cn.liboshuai.pulsix.framework.common.pojo.PageParam;
import cn.liboshuai.pulsix.framework.common.validation.InEnum;
import cn.liboshuai.pulsix.module.risk.enums.list.RiskListMatchTypeEnum;
import cn.liboshuai.pulsix.module.risk.enums.list.RiskListSyncStatusEnum;
import cn.liboshuai.pulsix.module.risk.enums.list.RiskListTypeEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Schema(description = "管理后台 - 名单集合分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
public class ListSetPageReqVO extends PageParam {

    @Schema(description = "所属场景编码", example = "TRADE_RISK")
    private String sceneCode;

    @Schema(description = "名单编码", example = "DEVICE_BLACKLIST")
    private String listCode;

    @Schema(description = "名单名称", example = "设备黑名单")
    private String listName;

    @Schema(description = "匹配维度", example = "DEVICE")
    @InEnum(value = RiskListMatchTypeEnum.class, message = "匹配维度必须是 {value}")
    private String matchType;

    @Schema(description = "名单类型", example = "BLACK")
    @InEnum(value = RiskListTypeEnum.class, message = "名单类型必须是 {value}")
    private String listType;

    @Schema(description = "同步状态", example = "PENDING")
    @InEnum(value = RiskListSyncStatusEnum.class, message = "同步状态必须是 {value}")
    private String syncStatus;

    @Schema(description = "状态", example = "0")
    @InEnum(value = CommonStatusEnum.class, message = "状态必须是 {value}")
    private Integer status;

}
