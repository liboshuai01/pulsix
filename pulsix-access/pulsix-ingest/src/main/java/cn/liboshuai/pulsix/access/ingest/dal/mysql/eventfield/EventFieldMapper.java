package cn.liboshuai.pulsix.access.ingest.dal.mysql.eventfield;

import cn.liboshuai.pulsix.access.ingest.dal.dataobject.eventfield.EventFieldDO;
import cn.liboshuai.pulsix.framework.mybatis.core.mapper.BaseMapperX;
import cn.liboshuai.pulsix.framework.mybatis.core.query.LambdaQueryWrapperX;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface EventFieldMapper extends BaseMapperX<EventFieldDO> {

    default List<EventFieldDO> selectOrderedList(String sceneCode, String eventCode) {
        return selectList(new LambdaQueryWrapperX<EventFieldDO>()
                .eq(EventFieldDO::getSceneCode, sceneCode)
                .eq(EventFieldDO::getEventCode, eventCode)
                .orderByAsc(EventFieldDO::getSortNo)
                .orderByAsc(EventFieldDO::getId));
    }

}
