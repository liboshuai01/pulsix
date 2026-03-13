package cn.liboshuai.pulsix.module.risk.controller.admin.auditlog.vo;

import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - 审计日志导出 Response VO")
@Data
@ExcelIgnoreUnannotated
public class AuditLogExportRespVO {

    @Schema(description = "所属场景编码", example = "TRADE_RISK")
    @ExcelProperty("所属场景")
    private String sceneCode;

    @Schema(description = "操作人名称", example = "admin")
    @ExcelProperty("操作人")
    private String operatorName;

    @Schema(description = "业务对象类型", example = "RULE")
    @ExcelProperty("对象类型")
    private String bizType;

    @Schema(description = "业务对象编码", example = "R002")
    @ExcelProperty("对象编码")
    private String bizCode;

    @Schema(description = "动作类型", example = "UPDATE")
    @ExcelProperty("动作类型")
    private String actionType;

    @Schema(description = "备注")
    @ExcelProperty("变更说明")
    private String remark;

    @Schema(description = "操作时间")
    @ExcelProperty("操作时间")
    private LocalDateTime operateTime;

}
