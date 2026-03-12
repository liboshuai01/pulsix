package cn.liboshuai.pulsix.module.risk.service.eventfield;

import cn.liboshuai.pulsix.framework.common.pojo.PageResult;
import cn.liboshuai.pulsix.module.risk.controller.admin.eventfield.vo.EventFieldPageReqVO;
import cn.liboshuai.pulsix.module.risk.controller.admin.eventfield.vo.EventFieldSaveReqVO;
import cn.liboshuai.pulsix.module.risk.dal.dataobject.eventfield.EventFieldDO;

public interface EventFieldService {

    Long createEventField(EventFieldSaveReqVO createReqVO);

    void updateEventField(EventFieldSaveReqVO updateReqVO);

    void deleteEventField(Long id);

    EventFieldDO getEventField(Long id);

    PageResult<EventFieldDO> getEventFieldPage(EventFieldPageReqVO pageReqVO);

    void updateEventFieldSort(Long id, Integer sortNo);

}
