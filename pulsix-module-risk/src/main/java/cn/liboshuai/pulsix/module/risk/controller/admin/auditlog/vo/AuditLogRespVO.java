package cn.liboshuai.pulsix.module.risk.controller.admin.auditlog.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - 审计日志 Response VO")
@Data
public class AuditLogRespVO {

    @Schema(description = "编号", example = "10201")
    private Long id;

    @Schema(description = "链路号", example = "TRACE-S20-10201")
    private String traceId;

    @Schema(description = "所属场景编码", example = "TRADE_RISK")
    private String sceneCode;

    @Schema(description = "操作人编号", example = "1")
    private Long operatorId;

    @Schema(description = "操作人名称", example = "admin")
    private String operatorName;

    @Schema(description = "业务对象类型", example = "RULE")
    private String bizType;

    @Schema(description = "业务对象编码", example = "R002")
    private String bizCode;

    @Schema(description = "动作类型", example = "UPDATE")
    private String actionType;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "操作时间")
    private LocalDateTime operateTime;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

}
