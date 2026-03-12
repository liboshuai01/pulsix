package cn.liboshuai.pulsix.module.risk.service.eventsample;

import cn.liboshuai.pulsix.framework.common.pojo.PageResult;
import cn.liboshuai.pulsix.module.risk.controller.admin.eventsample.vo.EventSamplePageReqVO;
import cn.liboshuai.pulsix.module.risk.controller.admin.eventsample.vo.EventSamplePreviewRespVO;
import cn.liboshuai.pulsix.module.risk.controller.admin.eventsample.vo.EventSampleSaveReqVO;
import cn.liboshuai.pulsix.module.risk.dal.dataobject.eventsample.EventSampleDO;

public interface EventSampleService {

    Long createEventSample(EventSampleSaveReqVO createReqVO);

    void updateEventSample(EventSampleSaveReqVO updateReqVO);

    void deleteEventSample(Long id);

    EventSampleDO getEventSample(Long id);

    PageResult<EventSampleDO> getEventSamplePage(EventSamplePageReqVO pageReqVO);

    EventSamplePreviewRespVO previewEventSample(Long id);

}
