package cn.liboshuai.pulsix.module.risk.dal.mysql.accessmapping;

import cn.liboshuai.pulsix.framework.mybatis.core.mapper.BaseMapperX;
import cn.liboshuai.pulsix.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.liboshuai.pulsix.module.risk.dal.dataobject.accessmapping.EventAccessRawFieldDefDO;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface EventAccessRawFieldDefMapper extends BaseMapperX<EventAccessRawFieldDefDO> {

    default List<EventAccessRawFieldDefDO> selectListByBindingId(Long bindingId) {
        return selectList(new LambdaQueryWrapperX<EventAccessRawFieldDefDO>()
                .eq(EventAccessRawFieldDefDO::getBindingId, bindingId)
                .orderByAsc(EventAccessRawFieldDefDO::getSortNo)
                .orderByAsc(EventAccessRawFieldDefDO::getId));
    }

    default long selectCountByBindingId(Long bindingId) {
        return selectCount(new LambdaQueryWrapperX<EventAccessRawFieldDefDO>()
                .eq(EventAccessRawFieldDefDO::getBindingId, bindingId));
    }

    @Delete("DELETE FROM event_access_raw_field_def WHERE binding_id = #{bindingId}")
    int deleteByBindingIdPhysically(@Param("bindingId") Long bindingId);

}
