package cn.liboshuai.pulsix.module.risk.controller.admin.list.vo;

import cn.liboshuai.pulsix.framework.common.enums.CommonStatusEnum;
import cn.liboshuai.pulsix.framework.common.validation.InEnum;
import cn.liboshuai.pulsix.module.risk.enums.list.RiskListMatchTypeEnum;
import cn.liboshuai.pulsix.module.risk.enums.list.RiskListStorageTypeEnum;
import cn.liboshuai.pulsix.module.risk.enums.list.RiskListSyncModeEnum;
import cn.liboshuai.pulsix.module.risk.enums.list.RiskListTypeEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Schema(description = "管理后台 - 名单集合创建/修改 Request VO")
@Data
public class ListSetSaveReqVO {

    @Schema(description = "主键", example = "1")
    private Long id;

    @Schema(description = "所属场景编码", requiredMode = Schema.RequiredMode.REQUIRED, example = "TRADE_RISK")
    @NotBlank(message = "所属场景编码不能为空")
    @Size(max = 64, message = "所属场景编码长度不能超过 64 个字符")
    private String sceneCode;

    @Schema(description = "名单编码", requiredMode = Schema.RequiredMode.REQUIRED, example = "DEVICE_BLACKLIST")
    @NotBlank(message = "名单编码不能为空")
    @Size(max = 64, message = "名单编码长度不能超过 64 个字符")
    @Pattern(regexp = "^[A-Za-z][A-Za-z0-9_]*$", message = "名单编码只允许字母、数字、下划线，且必须以字母开头")
    private String listCode;

    @Schema(description = "名单名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "设备黑名单")
    @NotBlank(message = "名单名称不能为空")
    @Size(max = 128, message = "名单名称长度不能超过 128 个字符")
    private String listName;

    @Schema(description = "匹配维度", requiredMode = Schema.RequiredMode.REQUIRED, example = "DEVICE")
    @NotBlank(message = "匹配维度不能为空")
    @InEnum(value = RiskListMatchTypeEnum.class, message = "匹配维度必须是 {value}")
    private String matchType;

    @Schema(description = "名单类型", requiredMode = Schema.RequiredMode.REQUIRED, example = "BLACK")
    @NotBlank(message = "名单类型不能为空")
    @InEnum(value = RiskListTypeEnum.class, message = "名单类型必须是 {value}")
    private String listType;

    @Schema(description = "运行时存储形式", requiredMode = Schema.RequiredMode.REQUIRED, example = "REDIS_SET")
    @NotBlank(message = "运行时存储形式不能为空")
    @InEnum(value = RiskListStorageTypeEnum.class, message = "运行时存储形式必须是 {value}")
    private String storageType;

    @Schema(description = "同步模式", requiredMode = Schema.RequiredMode.REQUIRED, example = "FULL")
    @NotBlank(message = "同步模式不能为空")
    @InEnum(value = RiskListSyncModeEnum.class, message = "同步模式必须是 {value}")
    private String syncMode;

    @Schema(description = "状态", requiredMode = Schema.RequiredMode.REQUIRED, example = "0")
    @NotNull(message = "状态不能为空")
    @InEnum(value = CommonStatusEnum.class, message = "状态必须是 {value}")
    private Integer status;

    @Schema(description = "名单说明")
    @Size(max = 512, message = "名单说明长度不能超过 512 个字符")
    private String description;

}
