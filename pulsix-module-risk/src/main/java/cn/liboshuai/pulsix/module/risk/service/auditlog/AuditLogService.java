package cn.liboshuai.pulsix.module.risk.service.auditlog;

import cn.liboshuai.pulsix.framework.common.pojo.PageResult;
import cn.liboshuai.pulsix.module.risk.controller.admin.auditlog.vo.AuditLogDetailRespVO;
import cn.liboshuai.pulsix.module.risk.controller.admin.auditlog.vo.AuditLogPageReqVO;
import cn.liboshuai.pulsix.module.risk.controller.admin.auditlog.vo.AuditLogRespVO;

public interface AuditLogService {

    PageResult<AuditLogRespVO> getAuditLogPage(AuditLogPageReqVO pageReqVO);

    AuditLogDetailRespVO getAuditLog(Long id);

}
