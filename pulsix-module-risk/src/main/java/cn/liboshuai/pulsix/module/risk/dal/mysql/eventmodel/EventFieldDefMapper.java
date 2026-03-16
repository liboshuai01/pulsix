package cn.liboshuai.pulsix.module.risk.dal.mysql.eventmodel;

import cn.liboshuai.pulsix.framework.mybatis.core.mapper.BaseMapperX;
import cn.liboshuai.pulsix.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.liboshuai.pulsix.module.risk.dal.dataobject.eventmodel.EventFieldDefDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface EventFieldDefMapper extends BaseMapperX<EventFieldDefDO> {

    default List<EventFieldDefDO> selectListByEventCode(String eventCode) {
        return selectList(new LambdaQueryWrapperX<EventFieldDefDO>()
                .eq(EventFieldDefDO::getEventCode, eventCode)
                .orderByAsc(EventFieldDefDO::getSortNo)
                .orderByAsc(EventFieldDefDO::getId));
    }

    default int deleteByEventCode(String eventCode) {
        return delete(new LambdaQueryWrapperX<EventFieldDefDO>()
                .eq(EventFieldDefDO::getEventCode, eventCode));
    }

}
