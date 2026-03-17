package cn.liboshuai.pulsix.module.risk.service.eventmodel;

import cn.liboshuai.pulsix.framework.common.pojo.PageResult;
import cn.liboshuai.pulsix.module.risk.controller.admin.eventmodel.vo.EventModelPageReqVO;
import cn.liboshuai.pulsix.module.risk.controller.admin.eventmodel.vo.EventModelPreviewRespVO;
import cn.liboshuai.pulsix.module.risk.controller.admin.eventmodel.vo.EventModelSaveReqVO;
import cn.liboshuai.pulsix.module.risk.dal.dataobject.eventmodel.EventFieldDefDO;
import cn.liboshuai.pulsix.module.risk.dal.dataobject.eventmodel.EventSchemaDO;

import java.util.List;

public interface EventModelService {

    Long createEventModel(EventModelSaveReqVO createReqVO);

    void updateEventModel(EventModelSaveReqVO updateReqVO);

    void updateEventModelStatus(Long id, Integer status);

    void deleteEventModel(Long id);

    EventSchemaDO getEventModel(Long id);

    EventSchemaDO getEventModelByCode(String eventCode);

    List<EventFieldDefDO> getEventFieldList(String eventCode);

    PageResult<EventSchemaDO> getEventModelPage(EventModelPageReqVO pageReqVO);

    List<EventSchemaDO> getSimpleEventModelList(String sceneCode);

    EventModelPreviewRespVO previewStandardEvent(EventModelSaveReqVO reqVO);

}
