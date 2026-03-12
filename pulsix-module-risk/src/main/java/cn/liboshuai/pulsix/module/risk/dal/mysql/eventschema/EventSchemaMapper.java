package cn.liboshuai.pulsix.module.risk.dal.mysql.eventschema;

import cn.liboshuai.pulsix.framework.common.pojo.PageResult;
import cn.liboshuai.pulsix.framework.mybatis.core.mapper.BaseMapperX;
import cn.liboshuai.pulsix.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.liboshuai.pulsix.module.risk.controller.admin.eventschema.vo.EventSchemaPageReqVO;
import cn.liboshuai.pulsix.module.risk.dal.dataobject.eventschema.EventSchemaDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface EventSchemaMapper extends BaseMapperX<EventSchemaDO> {

    default PageResult<EventSchemaDO> selectPage(EventSchemaPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<EventSchemaDO>()
                .likeIfPresent(EventSchemaDO::getSceneCode, reqVO.getSceneCode())
                .likeIfPresent(EventSchemaDO::getEventCode, reqVO.getEventCode())
                .likeIfPresent(EventSchemaDO::getEventName, reqVO.getEventName())
                .eqIfPresent(EventSchemaDO::getEventType, reqVO.getEventType())
                .orderByDesc(EventSchemaDO::getUpdateTime)
                .orderByDesc(EventSchemaDO::getId));
    }

}
