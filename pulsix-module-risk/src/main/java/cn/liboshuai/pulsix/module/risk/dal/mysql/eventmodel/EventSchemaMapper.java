package cn.liboshuai.pulsix.module.risk.dal.mysql.eventmodel;

import cn.liboshuai.pulsix.framework.common.enums.CommonStatusEnum;
import cn.liboshuai.pulsix.framework.common.pojo.PageResult;
import cn.liboshuai.pulsix.framework.mybatis.core.mapper.BaseMapperX;
import cn.liboshuai.pulsix.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.liboshuai.pulsix.module.risk.controller.admin.eventmodel.vo.EventModelPageReqVO;
import cn.liboshuai.pulsix.module.risk.dal.dataobject.eventmodel.EventSchemaDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface EventSchemaMapper extends BaseMapperX<EventSchemaDO> {

    default EventSchemaDO selectByEventCode(String eventCode) {
        return selectOne(EventSchemaDO::getEventCode, eventCode);
    }

    default PageResult<EventSchemaDO> selectPage(EventModelPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<EventSchemaDO>()
                .likeIfPresent(EventSchemaDO::getSceneCode, reqVO.getSceneCode())
                .likeIfPresent(EventSchemaDO::getEventCode, reqVO.getEventCode())
                .likeIfPresent(EventSchemaDO::getEventName, reqVO.getEventName())
                .likeIfPresent(EventSchemaDO::getEventType, reqVO.getEventType())
                .eqIfPresent(EventSchemaDO::getStatus, reqVO.getStatus())
                .orderByDesc(EventSchemaDO::getId));
    }

    default List<EventSchemaDO> selectEnabledList(String sceneCode) {
        return selectList(new LambdaQueryWrapperX<EventSchemaDO>()
                .eqIfPresent(EventSchemaDO::getSceneCode, sceneCode)
                .eq(EventSchemaDO::getStatus, CommonStatusEnum.ENABLE.getStatus())
                .orderByAsc(EventSchemaDO::getSceneCode)
                .orderByAsc(EventSchemaDO::getEventCode));
    }

    @Select("SELECT COUNT(1) FROM feature_def WHERE event_code = #{eventCode} AND deleted = b'0'")
    long selectFeatureCountByEventCode(@Param("eventCode") String eventCode);

}
