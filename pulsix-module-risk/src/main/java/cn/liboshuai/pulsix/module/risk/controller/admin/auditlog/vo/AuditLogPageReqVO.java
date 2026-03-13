package cn.liboshuai.pulsix.module.risk.controller.admin.auditlog.vo;

import cn.liboshuai.pulsix.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

import static cn.liboshuai.pulsix.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND;

@Schema(description = "管理后台 - 审计日志分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
public class AuditLogPageReqVO extends PageParam {

    @Schema(description = "所属场景编码", example = "TRADE_RISK")
    private String sceneCode;

    @Schema(description = "业务对象类型", example = "RULE")
    private String bizType;

    @Schema(description = "业务对象编码", example = "R002")
    private String bizCode;

    @Schema(description = "动作类型", example = "UPDATE")
    private String actionType;

    @Schema(description = "操作人名称", example = "admin")
    private String operatorName;

    @Schema(description = "操作时间", example = "[2026-03-12 00:00:00, 2026-03-12 23:59:59]")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime[] operateTime;

}
