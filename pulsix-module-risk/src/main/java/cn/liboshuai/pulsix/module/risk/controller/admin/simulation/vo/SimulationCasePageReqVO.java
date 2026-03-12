package cn.liboshuai.pulsix.module.risk.controller.admin.simulation.vo;

import cn.liboshuai.pulsix.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Schema(description = "管理后台 - 仿真用例分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
public class SimulationCasePageReqVO extends PageParam {

    @Schema(description = "所属场景编码", example = "TRADE_RISK")
    private String sceneCode;

    @Schema(description = "用例编码", example = "SIM_REJECT_BLACKLIST")
    private String caseCode;

    @Schema(description = "用例名称", example = "设备黑名单直接拒绝")
    private String caseName;

    @Schema(description = "版本选择模式", example = "LATEST")
    private String versionSelectMode;

    @Schema(description = "状态", example = "0")
    private Integer status;

}
