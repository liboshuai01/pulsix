package cn.liboshuai.pulsix.module.risk.service.auditlog;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.liboshuai.pulsix.framework.common.util.json.JsonUtils;
import cn.liboshuai.pulsix.framework.common.util.monitor.TracerUtils;
import cn.liboshuai.pulsix.framework.common.exception.util.ServiceExceptionUtil;
import cn.liboshuai.pulsix.framework.common.pojo.PageResult;
import cn.liboshuai.pulsix.framework.common.util.object.BeanUtils;
import cn.liboshuai.pulsix.framework.security.core.util.SecurityFrameworkUtils;
import cn.liboshuai.pulsix.module.risk.controller.admin.auditlog.vo.AuditLogDetailRespVO;
import cn.liboshuai.pulsix.module.risk.controller.admin.auditlog.vo.AuditLogPageReqVO;
import cn.liboshuai.pulsix.module.risk.controller.admin.auditlog.vo.AuditLogRespVO;
import cn.liboshuai.pulsix.module.risk.dal.dataobject.auditlog.RiskAuditLogDO;
import cn.liboshuai.pulsix.module.risk.dal.mysql.auditlog.RiskAuditLogMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static cn.liboshuai.pulsix.module.risk.enums.ErrorCodeConstants.AUDIT_LOG_NOT_EXISTS;

@Service
public class AuditLogServiceImpl implements AuditLogService {

    private static final TypeReference<Map<String, Object>> MAP_TYPE_REFERENCE = new TypeReference<>() {
    };

    @Resource
    private RiskAuditLogMapper riskAuditLogMapper;

    @Override
    public void createAuditLog(String sceneCode, String bizType, String bizCode, String actionType,
                               Object beforePayload, Object afterPayload, String remark) {
        RiskAuditLogDO auditLog = new RiskAuditLogDO();
        auditLog.setTraceId(StrUtil.blankToDefault(TracerUtils.getTraceId(), UUID.randomUUID().toString()));
        auditLog.setSceneCode(StrUtil.blankToDefault(StrUtil.trim(sceneCode), ""));
        auditLog.setOperatorId(ObjectUtil.defaultIfNull(SecurityFrameworkUtils.getLoginUserId(), 0L));
        auditLog.setOperatorName(resolveOperatorName());
        auditLog.setBizType(StrUtil.blankToDefault(StrUtil.trim(bizType), ""));
        auditLog.setBizCode(StrUtil.blankToDefault(StrUtil.trim(bizCode), ""));
        auditLog.setActionType(StrUtil.blankToDefault(StrUtil.trim(actionType), ""));
        auditLog.setBeforeJson(toPayloadMap(beforePayload));
        auditLog.setAfterJson(toPayloadMap(afterPayload));
        auditLog.setRemark(trimToNull(remark));
        auditLog.setOperateTime(LocalDateTime.now());
        riskAuditLogMapper.insert(auditLog);
    }

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

    private Map<String, Object> toPayloadMap(Object payload) {
        if (payload == null) {
            return null;
        }
        if (payload instanceof Map<?, ?> map) {
            return JsonUtils.convertObject(map, MAP_TYPE_REFERENCE);
        }
        return JsonUtils.convertObject(payload, MAP_TYPE_REFERENCE);
    }

    private String resolveOperatorName() {
        String nickname = trimToNull(SecurityFrameworkUtils.getLoginUserNickname());
        if (nickname != null) {
            return nickname;
        }
        Long userId = SecurityFrameworkUtils.getLoginUserId();
        return userId != null ? String.valueOf(userId) : "system";
    }

    private String trimToNull(String text) {
        return StrUtil.emptyToNull(StrUtil.trim(text));
    }

}
