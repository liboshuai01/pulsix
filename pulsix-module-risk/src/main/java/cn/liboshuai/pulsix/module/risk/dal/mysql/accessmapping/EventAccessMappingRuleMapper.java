package cn.liboshuai.pulsix.module.risk.dal.mysql.accessmapping;

import cn.liboshuai.pulsix.framework.mybatis.core.mapper.BaseMapperX;
import cn.liboshuai.pulsix.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.liboshuai.pulsix.module.risk.dal.dataobject.accessmapping.EventAccessMappingRuleDO;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface EventAccessMappingRuleMapper extends BaseMapperX<EventAccessMappingRuleDO> {

    default List<EventAccessMappingRuleDO> selectListByBindingId(Long bindingId) {
        return selectList(new LambdaQueryWrapperX<EventAccessMappingRuleDO>()
                .eq(EventAccessMappingRuleDO::getBindingId, bindingId)
                .orderByAsc(EventAccessMappingRuleDO::getId));
    }

    default long selectCountByBindingId(Long bindingId) {
        return selectCount(new LambdaQueryWrapperX<EventAccessMappingRuleDO>()
                .eq(EventAccessMappingRuleDO::getBindingId, bindingId));
    }

    @Delete("DELETE FROM event_access_mapping_rule WHERE binding_id = #{bindingId}")
    int deleteByBindingIdPhysically(@Param("bindingId") Long bindingId);

}
