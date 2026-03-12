package cn.liboshuai.pulsix.module.risk.dal.mysql.entitytype;

import cn.liboshuai.pulsix.framework.mybatis.core.mapper.BaseMapperX;
import cn.liboshuai.pulsix.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.liboshuai.pulsix.module.risk.dal.dataobject.entitytype.EntityTypeDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.Collection;
import java.util.List;

@Mapper
public interface EntityTypeMapper extends BaseMapperX<EntityTypeDO> {

    default List<EntityTypeDO> selectListByEntityTypes(Collection<String> entityTypes) {
        return selectList(new LambdaQueryWrapperX<EntityTypeDO>()
                .inIfPresent(EntityTypeDO::getEntityType, entityTypes));
    }

    default List<EntityTypeDO> selectAllList() {
        return selectList(new LambdaQueryWrapperX<EntityTypeDO>()
                .orderByAsc(EntityTypeDO::getStatus)
                .orderByAsc(EntityTypeDO::getEntityType));
    }

}
