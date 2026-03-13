package cn.liboshuai.pulsix.module.risk.service.eventschema;

import cn.liboshuai.pulsix.framework.common.pojo.PageResult;
import cn.liboshuai.pulsix.module.risk.controller.admin.eventschema.vo.EventSchemaPageReqVO;
import cn.liboshuai.pulsix.module.risk.controller.admin.eventschema.vo.EventSchemaSaveReqVO;
import cn.liboshuai.pulsix.module.risk.dal.dataobject.eventschema.EventSchemaDO;

public interface EventSchemaService {

    Long createEventSchema(EventSchemaSaveReqVO createReqVO);

    void updateEventSchema(EventSchemaSaveReqVO updateReqVO);

    void deleteEventSchema(Long id);

    EventSchemaDO getEventSchema(Long id);

    PageResult<EventSchemaDO> getEventSchemaPage(EventSchemaPageReqVO pageReqVO);

}
