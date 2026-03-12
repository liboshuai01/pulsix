package cn.liboshuai.pulsix.module.risk.dal.mysql.decisionlog;

import cn.liboshuai.pulsix.framework.common.pojo.PageResult;
import cn.liboshuai.pulsix.framework.mybatis.core.mapper.BaseMapperX;
import cn.liboshuai.pulsix.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.liboshuai.pulsix.module.risk.controller.admin.decisionlog.vo.DecisionLogPageReqVO;
import cn.liboshuai.pulsix.module.risk.dal.dataobject.decisionlog.DecisionLogDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface DecisionLogMapper extends BaseMapperX<DecisionLogDO> {

    default PageResult<DecisionLogDO> selectPage(DecisionLogPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<DecisionLogDO>()
                .eqIfPresent(DecisionLogDO::getSceneCode, reqVO.getSceneCode())
                .likeIfPresent(DecisionLogDO::getTraceId, reqVO.getTraceId())
                .likeIfPresent(DecisionLogDO::getEventId, reqVO.getEventId())
                .eqIfPresent(DecisionLogDO::getFinalAction, reqVO.getFinalAction())
                .eqIfPresent(DecisionLogDO::getVersionNo, reqVO.getVersionNo())
                .orderByDesc(DecisionLogDO::getEventTime)
                .orderByDesc(DecisionLogDO::getId));
    }

}
