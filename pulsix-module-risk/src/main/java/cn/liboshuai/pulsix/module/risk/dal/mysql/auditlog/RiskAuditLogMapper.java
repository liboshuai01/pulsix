package cn.liboshuai.pulsix.module.risk.dal.mysql.auditlog;

import cn.liboshuai.pulsix.framework.common.pojo.PageResult;
import cn.liboshuai.pulsix.framework.mybatis.core.mapper.BaseMapperX;
import cn.liboshuai.pulsix.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.liboshuai.pulsix.module.risk.controller.admin.auditlog.vo.AuditLogPageReqVO;
import cn.liboshuai.pulsix.module.risk.dal.dataobject.auditlog.RiskAuditLogDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface RiskAuditLogMapper extends BaseMapperX<RiskAuditLogDO> {

    default PageResult<RiskAuditLogDO> selectPage(AuditLogPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<RiskAuditLogDO>()
                .eqIfPresent(RiskAuditLogDO::getSceneCode, reqVO.getSceneCode())
                .eqIfPresent(RiskAuditLogDO::getBizType, reqVO.getBizType())
                .likeIfPresent(RiskAuditLogDO::getBizCode, reqVO.getBizCode())
                .eqIfPresent(RiskAuditLogDO::getActionType, reqVO.getActionType())
                .likeIfPresent(RiskAuditLogDO::getOperatorName, reqVO.getOperatorName())
                .betweenIfPresent(RiskAuditLogDO::getOperateTime, reqVO.getOperateTime())
                .orderByDesc(RiskAuditLogDO::getOperateTime)
                .orderByDesc(RiskAuditLogDO::getId));
    }

}
