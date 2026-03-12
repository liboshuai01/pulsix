package cn.liboshuai.pulsix.module.risk.controller.admin.release.vo;

import cn.liboshuai.pulsix.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Schema(description = "管理后台 - 发布记录分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
public class SceneReleasePageReqVO extends PageParam {

    @Schema(description = "所属场景编码", example = "TRADE_RISK")
    private String sceneCode;

    @Schema(description = "发布状态", example = "DRAFT")
    private String publishStatus;

    @Schema(description = "预检状态", example = "PASSED")
    private String validationStatus;

}
