package cn.liboshuai.pulsix.module.risk.dal.mysql.ingesterror;

import cn.liboshuai.pulsix.framework.common.pojo.PageResult;
import cn.liboshuai.pulsix.framework.mybatis.core.mapper.BaseMapperX;
import cn.liboshuai.pulsix.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.liboshuai.pulsix.module.risk.controller.admin.ingesterror.vo.IngestErrorPageReqVO;
import cn.liboshuai.pulsix.module.risk.dal.dataobject.ingesterror.IngestErrorLogDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface IngestErrorLogMapper extends BaseMapperX<IngestErrorLogDO> {

    default PageResult<IngestErrorLogDO> selectPage(IngestErrorPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<IngestErrorLogDO>()
                .eqIfPresent(IngestErrorLogDO::getSceneCode, reqVO.getSceneCode())
                .eqIfPresent(IngestErrorLogDO::getSourceCode, reqVO.getSourceCode())
                .likeIfPresent(IngestErrorLogDO::getTraceId, reqVO.getTraceId())
                .likeIfPresent(IngestErrorLogDO::getRawEventId, reqVO.getRawEventId())
                .eqIfPresent(IngestErrorLogDO::getIngestStage, reqVO.getIngestStage())
                .likeIfPresent(IngestErrorLogDO::getErrorCode, reqVO.getErrorCode())
                .eqIfPresent(IngestErrorLogDO::getReprocessStatus, reqVO.getReprocessStatus())
                .betweenIfPresent(IngestErrorLogDO::getOccurTime, reqVO.getOccurTime())
                .orderByDesc(IngestErrorLogDO::getOccurTime)
                .orderByDesc(IngestErrorLogDO::getId));
    }

}
