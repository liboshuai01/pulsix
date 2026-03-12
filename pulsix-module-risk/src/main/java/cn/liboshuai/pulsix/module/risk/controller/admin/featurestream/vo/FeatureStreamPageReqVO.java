package cn.liboshuai.pulsix.module.risk.controller.admin.featurestream.vo;

import cn.liboshuai.pulsix.framework.common.enums.CommonStatusEnum;
import cn.liboshuai.pulsix.framework.common.pojo.PageParam;
import cn.liboshuai.pulsix.framework.common.validation.InEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Schema(description = "管理后台 - 流式特征分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
public class FeatureStreamPageReqVO extends PageParam {

    @Schema(description = "所属场景编码", example = "TRADE_RISK")
    private String sceneCode;

    @Schema(description = "特征编码，模糊匹配", example = "user_trade_cnt_5m")
    private String featureCode;

    @Schema(description = "特征名称，模糊匹配", example = "用户 5 分钟交易次数")
    private String featureName;

    @Schema(description = "实体类型", example = "USER")
    private String entityType;

    @Schema(description = "状态", example = "0")
    @InEnum(value = CommonStatusEnum.class, message = "状态必须是 {value}")
    private Integer status;

}
