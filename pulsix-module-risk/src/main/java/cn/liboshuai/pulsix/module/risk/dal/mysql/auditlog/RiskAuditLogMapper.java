package cn.liboshuai.pulsix.module.risk.dal.mysql.auditlog;

import cn.liboshuai.pulsix.framework.mybatis.core.mapper.BaseMapperX;
import cn.liboshuai.pulsix.module.risk.dal.dataobject.auditlog.RiskAuditLogDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface RiskAuditLogMapper extends BaseMapperX<RiskAuditLogDO> {
}
