package cn.liboshuai.pulsix.module.risk.service.auditlog;

import cn.hutool.core.collection.CollUtil;
import cn.liboshuai.pulsix.framework.common.exception.util.ServiceExceptionUtil;
import cn.liboshuai.pulsix.framework.common.pojo.PageResult;
import cn.liboshuai.pulsix.framework.common.util.object.BeanUtils;
import cn.liboshuai.pulsix.module.risk.controller.admin.auditlog.vo.AuditLogDetailRespVO;
import cn.liboshuai.pulsix.module.risk.controller.admin.auditlog.vo.AuditLogPageReqVO;
import cn.liboshuai.pulsix.module.risk.controller.admin.auditlog.vo.AuditLogRespVO;
import cn.liboshuai.pulsix.module.risk.dal.dataobject.auditlog.RiskAuditLogDO;
import cn.liboshuai.pulsix.module.risk.dal.mysql.auditlog.RiskAuditLogMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

import static cn.liboshuai.pulsix.module.risk.enums.ErrorCodeConstants.AUDIT_LOG_NOT_EXISTS;

@Service
public class AuditLogServiceImpl implements AuditLogService {

    @Resource
    private RiskAuditLogMapper riskAuditLogMapper;

    @Override
    public PageResult<AuditLogRespVO> getAuditLogPage(AuditLogPageReqVO pageReqVO) {
        PageResult<RiskAuditLogDO> pageResult = riskAuditLogMapper.selectPage(pageReqVO);
        if (CollUtil.isEmpty(pageResult.getList())) {
            return new PageResult<>(Collections.emptyList(), pageResult.getTotal());
        }
        List<AuditLogRespVO> list = pageResult.getList().stream()
                .map(item -> BeanUtils.toBean(item, AuditLogRespVO.class))
                .toList();
        return new PageResult<>(list, pageResult.getTotal());
    }

    @Override
    public AuditLogDetailRespVO getAuditLog(Long id) {
        RiskAuditLogDO auditLog = validateAuditLogExists(id);
        return BeanUtils.toBean(auditLog, AuditLogDetailRespVO.class);
    }

    private RiskAuditLogDO validateAuditLogExists(Long id) {
        if (id == null) {
            throw ServiceExceptionUtil.exception(AUDIT_LOG_NOT_EXISTS);
        }
        RiskAuditLogDO auditLog = riskAuditLogMapper.selectById(id);
        if (auditLog == null) {
            throw ServiceExceptionUtil.exception(AUDIT_LOG_NOT_EXISTS);
        }
        return auditLog;
    }

}
