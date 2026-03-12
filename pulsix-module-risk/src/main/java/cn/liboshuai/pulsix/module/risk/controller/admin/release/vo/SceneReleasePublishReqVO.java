package cn.liboshuai.pulsix.module.risk.controller.admin.release.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - 正式发布 Request VO")
@Data
public class SceneReleasePublishReqVO {

    @Schema(description = "发布记录主键", requiredMode = Schema.RequiredMode.REQUIRED, example = "2202")
    @NotNull(message = "发布记录不能为空")
    private Long id;

    @Schema(description = "计划生效时间；为空表示立即生效", example = "2026-03-12T20:30:00")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime effectiveFrom;

    @Schema(description = "发布说明", example = "S14 正式发布 v13，准备切主")
    @Size(max = 512, message = "发布说明长度不能超过 512 个字符")
    private String remark;

}
