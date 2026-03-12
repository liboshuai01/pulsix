package cn.liboshuai.pulsix.module.risk.controller.admin.featurestream.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "管理后台 - 实体类型 Response VO")
@Data
public class EntityTypeRespVO {

    @Schema(description = "主键", example = "1")
    private Long id;

    @Schema(description = "实体类型编码", requiredMode = Schema.RequiredMode.REQUIRED, example = "USER")
    private String entityType;

    @Schema(description = "实体类型名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "用户")
    private String entityName;

    @Schema(description = "标准事件主键字段", requiredMode = Schema.RequiredMode.REQUIRED, example = "userId")
    private String keyFieldName;

    @Schema(description = "状态", example = "0")
    private Integer status;

    @Schema(description = "实体类型说明")
    private String description;

}
