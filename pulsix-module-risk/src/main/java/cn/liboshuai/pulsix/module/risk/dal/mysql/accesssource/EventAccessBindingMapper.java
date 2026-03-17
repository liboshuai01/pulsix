package cn.liboshuai.pulsix.module.risk.dal.mysql.accesssource;

import cn.liboshuai.pulsix.framework.mybatis.core.mapper.BaseMapperX;
import cn.liboshuai.pulsix.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.liboshuai.pulsix.module.risk.dal.dataobject.accesssource.EventAccessBindingDO;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

@Mapper
public interface EventAccessBindingMapper extends BaseMapperX<EventAccessBindingDO> {

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

    @Delete("DELETE FROM event_access_binding WHERE event_code = #{eventCode}")
    int deleteByEventCodePhysically(@Param("eventCode") String eventCode);

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
