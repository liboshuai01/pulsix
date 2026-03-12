package cn.liboshuai.pulsix.module.risk.service.list;

import cn.liboshuai.pulsix.framework.common.pojo.PageResult;
import cn.liboshuai.pulsix.module.risk.controller.admin.list.vo.ListItemPageReqVO;
import cn.liboshuai.pulsix.module.risk.controller.admin.list.vo.ListItemSaveReqVO;
import cn.liboshuai.pulsix.module.risk.controller.admin.list.vo.ListSetPageReqVO;
import cn.liboshuai.pulsix.module.risk.controller.admin.list.vo.ListSetSaveReqVO;
import cn.liboshuai.pulsix.module.risk.controller.admin.list.vo.ListSyncRespVO;
import cn.liboshuai.pulsix.module.risk.dal.dataobject.list.ListItemDO;
import cn.liboshuai.pulsix.module.risk.dal.dataobject.list.ListSetDO;

public interface RiskListService {

    Long createListSet(ListSetSaveReqVO createReqVO);

    void updateListSet(ListSetSaveReqVO updateReqVO);

    void updateListSetStatus(Long id, Integer status);

    void deleteListSet(Long id);

    ListSetDO getListSet(Long id);

    PageResult<ListSetDO> getListSetPage(ListSetPageReqVO pageReqVO);

    ListSyncRespVO syncListSet(Long id);

    Long createListItem(ListItemSaveReqVO createReqVO);

    void updateListItem(ListItemSaveReqVO updateReqVO);

    void updateListItemStatus(Long id, Integer status);

    void deleteListItem(Long id);

    ListItemDO getListItem(Long id);

    PageResult<ListItemDO> getListItemPage(ListItemPageReqVO pageReqVO);

}
