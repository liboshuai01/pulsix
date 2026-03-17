package cn.liboshuai.pulsix.module.risk.dal.mysql.accesssource;

import cn.hutool.core.collection.CollUtil;
import cn.liboshuai.pulsix.framework.common.pojo.PageResult;
import cn.liboshuai.pulsix.framework.mybatis.core.mapper.BaseMapperX;
import cn.liboshuai.pulsix.framework.mybatis.core.query.MPJLambdaWrapperX;
import cn.liboshuai.pulsix.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.liboshuai.pulsix.module.risk.controller.admin.accessmapping.vo.AccessMappingPageReqVO;
import cn.liboshuai.pulsix.module.risk.dal.dataobject.accesssource.AccessSourceDO;
import cn.liboshuai.pulsix.module.risk.dal.dataobject.accesssource.EventAccessBindingDO;
import cn.liboshuai.pulsix.module.risk.dal.dataobject.eventmodel.EventSchemaDO;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

@Mapper
public interface EventAccessBindingMapper extends BaseMapperX<EventAccessBindingDO> {

    default EventAccessBindingDO selectByEventCodeAndSourceCode(String eventCode, String sourceCode) {
        return selectFirstOne(EventAccessBindingDO::getEventCode, eventCode, EventAccessBindingDO::getSourceCode, sourceCode);
    }

    default List<EventAccessBindingDO> selectListByEventCode(String eventCode) {
        return selectList(new LambdaQueryWrapperX<EventAccessBindingDO>()
                .eq(EventAccessBindingDO::getEventCode, eventCode)
                .orderByAsc(EventAccessBindingDO::getId));
    }

    default List<EventAccessBindingDO> selectListByEventCodes(Collection<String> eventCodes) {
        if (eventCodes == null || eventCodes.isEmpty()) {
            return Collections.emptyList();
        }
        return selectList(new LambdaQueryWrapperX<EventAccessBindingDO>()
                .in(EventAccessBindingDO::getEventCode, eventCodes)
                .orderByAsc(EventAccessBindingDO::getId));
    }

    default long selectCountBySourceCode(String sourceCode) {
        return selectCount(new LambdaQueryWrapperX<EventAccessBindingDO>()
                .eq(EventAccessBindingDO::getSourceCode, sourceCode));
    }

    default long selectCountByEventCode(String eventCode) {
        return selectCount(new LambdaQueryWrapperX<EventAccessBindingDO>()
                .eq(EventAccessBindingDO::getEventCode, eventCode));
    }

    default PageResult<EventAccessBindingDO> selectAccessMappingPage(AccessMappingPageReqVO reqVO) {
        MPJLambdaWrapperX<EventAccessBindingDO> wrapper = new MPJLambdaWrapperX<EventAccessBindingDO>()
                .selectAll(EventAccessBindingDO.class)
                .selectAs(EventSchemaDO::getSceneCode, EventAccessBindingDO::getSceneCode)
                .selectAs(EventSchemaDO::getEventName, EventAccessBindingDO::getEventName)
                .selectAs(AccessSourceDO::getSourceName, EventAccessBindingDO::getSourceName)
                .selectAs(AccessSourceDO::getSourceType, EventAccessBindingDO::getSourceType)
                .selectAs(AccessSourceDO::getTopicName, EventAccessBindingDO::getTopicName)
                .innerJoin(EventSchemaDO.class, EventSchemaDO::getEventCode, EventAccessBindingDO::getEventCode)
                .innerJoin(AccessSourceDO.class, AccessSourceDO::getSourceCode, EventAccessBindingDO::getSourceCode)
                .likeIfPresent(EventSchemaDO::getSceneCode, reqVO.getSceneCode())
                .likeIfPresent(EventAccessBindingDO::getEventCode, reqVO.getEventCode())
                .likeIfPresent(EventAccessBindingDO::getSourceCode, reqVO.getSourceCode())
                .eqIfPresent(AccessSourceDO::getSourceType, reqVO.getSourceType())
                .orderByDesc(EventAccessBindingDO::getId);
        return selectJoinPage(reqVO, EventAccessBindingDO.class, wrapper);
    }

    default EventAccessBindingDO selectAccessMappingDetail(Long id) {
        List<EventAccessBindingDO> list = selectJoinList(EventAccessBindingDO.class, new MPJLambdaWrapperX<EventAccessBindingDO>()
                .selectAll(EventAccessBindingDO.class)
                .selectAs(EventSchemaDO::getSceneCode, EventAccessBindingDO::getSceneCode)
                .selectAs(EventSchemaDO::getEventName, EventAccessBindingDO::getEventName)
                .selectAs(AccessSourceDO::getSourceName, EventAccessBindingDO::getSourceName)
                .selectAs(AccessSourceDO::getSourceType, EventAccessBindingDO::getSourceType)
                .selectAs(AccessSourceDO::getTopicName, EventAccessBindingDO::getTopicName)
                .innerJoin(EventSchemaDO.class, EventSchemaDO::getEventCode, EventAccessBindingDO::getEventCode)
                .innerJoin(AccessSourceDO.class, AccessSourceDO::getSourceCode, EventAccessBindingDO::getSourceCode)
                .eq(EventAccessBindingDO::getId, id)
                .last("LIMIT 1"));
        return CollUtil.getFirst(list);
    }

    default List<EventAccessBindingDO> selectAccessMappingListBySourceCodeAndEventCode(String sourceCode, String eventCode) {
        return selectJoinList(EventAccessBindingDO.class, new MPJLambdaWrapperX<EventAccessBindingDO>()
                .selectAll(EventAccessBindingDO.class)
                .selectAs(EventSchemaDO::getSceneCode, EventAccessBindingDO::getSceneCode)
                .selectAs(EventSchemaDO::getEventName, EventAccessBindingDO::getEventName)
                .selectAs(AccessSourceDO::getSourceName, EventAccessBindingDO::getSourceName)
                .selectAs(AccessSourceDO::getSourceType, EventAccessBindingDO::getSourceType)
                .selectAs(AccessSourceDO::getTopicName, EventAccessBindingDO::getTopicName)
                .innerJoin(EventSchemaDO.class, EventSchemaDO::getEventCode, EventAccessBindingDO::getEventCode)
                .innerJoin(AccessSourceDO.class, AccessSourceDO::getSourceCode, EventAccessBindingDO::getSourceCode)
                .eq(EventAccessBindingDO::getSourceCode, sourceCode)
                .eq(EventAccessBindingDO::getEventCode, eventCode)
                .orderByAsc(EventAccessBindingDO::getId));
    }

    @Delete("DELETE FROM event_access_binding WHERE event_code = #{eventCode}")
    int deleteByEventCodePhysically(@Param("eventCode") String eventCode);

    @Delete("DELETE FROM event_access_binding WHERE id = #{id}")
    int deleteByIdPhysically(@Param("id") Long id);

    @Select("""
            SELECT DISTINCT es.scene_code
            FROM event_access_binding eab
            INNER JOIN event_schema es ON es.event_code = eab.event_code AND es.deleted = b'0'
            WHERE eab.deleted = b'0'
              AND eab.source_code = #{sourceCode}
            ORDER BY es.scene_code ASC
            """)
    List<String> selectBoundSceneCodesBySourceCode(@Param("sourceCode") String sourceCode);

}
