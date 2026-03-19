package cn.liboshuai.pulsix.module.risk.convert.eventmodel;

import cn.liboshuai.pulsix.framework.common.pojo.PageResult;
import cn.liboshuai.pulsix.module.risk.controller.admin.eventmodel.vo.EventFieldItemVO;
import cn.liboshuai.pulsix.module.risk.controller.admin.eventmodel.vo.EventModelRespVO;
import cn.liboshuai.pulsix.module.risk.controller.admin.eventmodel.vo.EventModelSaveReqVO;
import cn.liboshuai.pulsix.module.risk.controller.admin.eventmodel.vo.EventModelSimpleRespVO;
import cn.liboshuai.pulsix.module.risk.dal.dataobject.eventmodel.EventFieldDefDO;
import cn.liboshuai.pulsix.module.risk.dal.dataobject.eventmodel.EventSchemaDO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

import java.util.List;

@Mapper
public interface EventModelConvert {

    EventModelConvert INSTANCE = Mappers.getMapper(EventModelConvert.class);

    EventSchemaDO convert(EventModelSaveReqVO bean);

    EventFieldDefDO convert(EventFieldItemVO bean);

    List<EventFieldDefDO> convertFieldDOList(List<EventFieldItemVO> list);

    @Mapping(target = "fields", ignore = true)
    @Mapping(target = "bindingSources", ignore = true)
    @Mapping(target = "deletable", ignore = true)
    @Mapping(target = "deleteBlockedReason", ignore = true)
    EventModelRespVO convert(EventSchemaDO bean);

    EventFieldItemVO convert(EventFieldDefDO bean);

    List<EventFieldItemVO> convertFieldList(List<EventFieldDefDO> list);

    EventModelSimpleRespVO convertSimple(EventSchemaDO bean);

    List<EventModelSimpleRespVO> convertSimpleList(List<EventSchemaDO> list);

    PageResult<EventModelRespVO> convertPage(PageResult<EventSchemaDO> page);

}
