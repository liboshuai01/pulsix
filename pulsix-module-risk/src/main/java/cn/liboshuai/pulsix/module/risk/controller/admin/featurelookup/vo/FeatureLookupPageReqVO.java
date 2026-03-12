package cn.liboshuai.pulsix.module.risk.controller.admin.featurelookup.vo;

import cn.liboshuai.pulsix.framework.common.enums.CommonStatusEnum;
import cn.liboshuai.pulsix.framework.common.pojo.PageParam;
import cn.liboshuai.pulsix.framework.common.validation.InEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Schema(description = "管理后台 - 查询特征分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
public class FeatureLookupPageReqVO extends PageParam {

    @Schema(description = "所属场景编码", example = "TRADE_RISK")
    private String sceneCode;

    @Schema(description = "特征编码，模糊匹配", example = "device_in_blacklist")
    private String featureCode;

    @Schema(description = "特征名称，模糊匹配", example = "设备是否命中黑名单")
    private String featureName;

    @Schema(description = "状态", example = "0")
    @InEnum(value = CommonStatusEnum.class, message = "状态必须是 {value}")
    private Integer status;

}
