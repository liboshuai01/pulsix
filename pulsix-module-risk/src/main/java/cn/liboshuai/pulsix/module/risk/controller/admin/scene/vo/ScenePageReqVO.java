package cn.liboshuai.pulsix.module.risk.controller.admin.scene.vo;

import cn.liboshuai.pulsix.framework.common.enums.CommonStatusEnum;
import cn.liboshuai.pulsix.framework.common.pojo.PageParam;
import cn.liboshuai.pulsix.framework.common.validation.InEnum;
import cn.liboshuai.pulsix.module.risk.enums.scene.SceneRuntimeModeEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "管理后台 - 风控场景分页 Request VO")
@Data
public class ScenePageReqVO extends PageParam {

    @Schema(description = "场景编码，模糊匹配", example = "TRADE_RISK")
    private String sceneCode;

    @Schema(description = "场景名称，模糊匹配", example = "交易风控")
    private String sceneName;

    @Schema(description = "状态，参见 CommonStatusEnum 枚举类", example = "0")
    @InEnum(value = CommonStatusEnum.class, message = "状态必须是 {value}")
    private Integer status;

    @Schema(description = "运行模式，参见 SceneRuntimeModeEnum 枚举类", example = "ASYNC_DECISION")
    @InEnum(value = SceneRuntimeModeEnum.class, message = "运行模式必须是 {value}")
    private String runtimeMode;

}
