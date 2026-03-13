package cn.liboshuai.pulsix.module.risk.controller.admin.auditlog.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Map;

@Schema(description = "管理后台 - 审计日志详情 Response VO")
@Data
@EqualsAndHashCode(callSuper = true)
public class AuditLogDetailRespVO extends AuditLogRespVO {

    @Schema(description = "变更前快照 JSON")
    private Map<String, Object> beforeJson;

    @Schema(description = "变更后快照 JSON")
    private Map<String, Object> afterJson;

}
