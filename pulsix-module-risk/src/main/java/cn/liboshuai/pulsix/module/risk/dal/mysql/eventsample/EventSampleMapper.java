package cn.liboshuai.pulsix.module.risk.dal.mysql.eventsample;

import cn.liboshuai.pulsix.framework.common.pojo.PageResult;
import cn.liboshuai.pulsix.framework.mybatis.core.mapper.BaseMapperX;
import cn.liboshuai.pulsix.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.liboshuai.pulsix.module.risk.controller.admin.eventsample.vo.EventSamplePageReqVO;
import cn.liboshuai.pulsix.module.risk.dal.dataobject.eventsample.EventSampleDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface EventSampleMapper extends BaseMapperX<EventSampleDO> {

    default PageResult<EventSampleDO> selectPage(EventSamplePageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<EventSampleDO>()
                .likeIfPresent(EventSampleDO::getSceneCode, reqVO.getSceneCode())
                .likeIfPresent(EventSampleDO::getEventCode, reqVO.getEventCode())
                .likeIfPresent(EventSampleDO::getSampleCode, reqVO.getSampleCode())
                .likeIfPresent(EventSampleDO::getSampleName, reqVO.getSampleName())
                .eqIfPresent(EventSampleDO::getSampleType, reqVO.getSampleType())
                .eqIfPresent(EventSampleDO::getStatus, reqVO.getStatus())
                .orderByAsc(EventSampleDO::getSortNo)
                .orderByAsc(EventSampleDO::getId));
    }

}
