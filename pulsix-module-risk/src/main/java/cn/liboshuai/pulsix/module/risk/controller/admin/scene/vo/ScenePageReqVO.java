package cn.liboshuai.pulsix.module.risk.controller.admin.scene.vo;

import cn.liboshuai.pulsix.framework.common.enums.CommonStatusEnum;
import cn.liboshuai.pulsix.framework.common.pojo.PageParam;
import cn.liboshuai.pulsix.framework.common.validation.InEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Schema(description = "管理后台 - 风控场景分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
public class ScenePageReqVO extends PageParam {

    @Schema(description = "场景编码，模糊匹配", example = "TRADE_RISK")
    private String sceneCode;

    @Schema(description = "场景名称，模糊匹配", example = "交易风控")
    private String sceneName;

    @Schema(description = "状态，见 CommonStatusEnum 枚举", example = "0")
    @InEnum(value = CommonStatusEnum.class, message = "状态必须是 {value}")
    private Integer status;

}

