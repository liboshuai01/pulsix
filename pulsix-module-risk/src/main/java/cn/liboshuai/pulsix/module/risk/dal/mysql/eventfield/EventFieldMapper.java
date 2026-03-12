package cn.liboshuai.pulsix.module.risk.dal.mysql.eventfield;

import cn.liboshuai.pulsix.framework.common.pojo.PageResult;
import cn.liboshuai.pulsix.framework.mybatis.core.mapper.BaseMapperX;
import cn.liboshuai.pulsix.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.liboshuai.pulsix.module.risk.controller.admin.eventfield.vo.EventFieldPageReqVO;
import cn.liboshuai.pulsix.module.risk.dal.dataobject.eventfield.EventFieldDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface EventFieldMapper extends BaseMapperX<EventFieldDO> {

    default PageResult<EventFieldDO> selectPage(EventFieldPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<EventFieldDO>()
                .likeIfPresent(EventFieldDO::getSceneCode, reqVO.getSceneCode())
                .likeIfPresent(EventFieldDO::getEventCode, reqVO.getEventCode())
                .likeIfPresent(EventFieldDO::getFieldCode, reqVO.getFieldCode())
                .likeIfPresent(EventFieldDO::getFieldName, reqVO.getFieldName())
                .eqIfPresent(EventFieldDO::getFieldType, reqVO.getFieldType())
                .orderByAsc(EventFieldDO::getSortNo)
                .orderByAsc(EventFieldDO::getId));
    }

}
