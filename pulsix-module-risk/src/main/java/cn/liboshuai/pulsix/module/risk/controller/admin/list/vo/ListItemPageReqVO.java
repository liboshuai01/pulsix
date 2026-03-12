package cn.liboshuai.pulsix.module.risk.controller.admin.list.vo;

import cn.liboshuai.pulsix.framework.common.enums.CommonStatusEnum;
import cn.liboshuai.pulsix.framework.common.pojo.PageParam;
import cn.liboshuai.pulsix.framework.common.validation.InEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Schema(description = "管理后台 - 名单条目分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
public class ListItemPageReqVO extends PageParam {

    @Schema(description = "所属场景编码", requiredMode = Schema.RequiredMode.REQUIRED, example = "TRADE_RISK")
    private String sceneCode;

    @Schema(description = "名单编码", requiredMode = Schema.RequiredMode.REQUIRED, example = "DEVICE_BLACKLIST")
    private String listCode;

    @Schema(description = "匹配值", example = "D0009")
    private String matchValue;

    @Schema(description = "状态", example = "0")
    @InEnum(value = CommonStatusEnum.class, message = "状态必须是 {value}")
    private Integer status;

}
