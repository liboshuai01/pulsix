package cn.liboshuai.pulsix.module.risk.controller.admin.list.vo;

import cn.liboshuai.pulsix.framework.common.enums.CommonStatusEnum;
import cn.liboshuai.pulsix.framework.common.validation.InEnum;
import cn.liboshuai.pulsix.module.risk.enums.list.RiskListItemSourceTypeEnum;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;

@Schema(description = "管理后台 - 名单条目创建/修改 Request VO")
@Data
public class ListItemSaveReqVO {

    @Schema(description = "主键", example = "1")
    private Long id;

    @Schema(description = "所属场景编码", requiredMode = Schema.RequiredMode.REQUIRED, example = "TRADE_RISK")
    @NotBlank(message = "所属场景编码不能为空")
    @Size(max = 64, message = "所属场景编码长度不能超过 64 个字符")
    private String sceneCode;

    @Schema(description = "名单编码", requiredMode = Schema.RequiredMode.REQUIRED, example = "DEVICE_BLACKLIST")
    @NotBlank(message = "名单编码不能为空")
    @Size(max = 64, message = "名单编码长度不能超过 64 个字符")
    private String listCode;

    @Schema(description = "匹配键名", example = "deviceId")
    @Size(max = 128, message = "匹配键名长度不能超过 128 个字符")
    private String matchKey;

    @Schema(description = "匹配值", requiredMode = Schema.RequiredMode.REQUIRED, example = "D0009")
    @NotBlank(message = "匹配值不能为空")
    @Size(max = 512, message = "匹配值长度不能超过 512 个字符")
    private String matchValue;

    @Schema(description = "过期时间", example = "2026-03-12T23:59:59")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime expireAt;

    @Schema(description = "状态", requiredMode = Schema.RequiredMode.REQUIRED, example = "0")
    @NotNull(message = "状态不能为空")
    @InEnum(value = CommonStatusEnum.class, message = "状态必须是 {value}")
    private Integer status;

    @Schema(description = "来源类型", requiredMode = Schema.RequiredMode.REQUIRED, example = "MANUAL")
    @NotBlank(message = "来源类型不能为空")
    @InEnum(value = RiskListItemSourceTypeEnum.class, message = "来源类型必须是 {value}")
    private String sourceType;

    @Schema(description = "导入批次号")
    @Size(max = 64, message = "导入批次号长度不能超过 64 个字符")
    private String batchNo;

    @Schema(description = "条目备注")
    @Size(max = 512, message = "条目备注长度不能超过 512 个字符")
    private String remark;

    @Schema(description = "扩展信息 JSON")
    private Map<String, Object> extJson;

}
