package cn.liboshuai.pulsix.module.risk.service.accesssource;

import cn.liboshuai.pulsix.framework.common.pojo.PageResult;
import cn.liboshuai.pulsix.module.risk.controller.admin.accesssource.vo.AccessSourcePageReqVO;
import cn.liboshuai.pulsix.module.risk.controller.admin.accesssource.vo.AccessSourceSaveReqVO;
import cn.liboshuai.pulsix.module.risk.dal.dataobject.accesssource.AccessSourceDO;

import java.util.Collection;
import java.util.List;
import java.util.Map;

public interface AccessSourceService {

    Long createAccessSource(AccessSourceSaveReqVO createReqVO);

    void updateAccessSource(AccessSourceSaveReqVO updateReqVO);

    void updateAccessSourceStatus(Long id, Integer status);

    void deleteAccessSource(Long id);

    AccessSourceDO getAccessSource(Long id);

    PageResult<AccessSourceDO> getAccessSourcePage(AccessSourcePageReqVO pageReqVO);

    List<AccessSourceDO> getSimpleAccessSourceList(String sceneCode);

    Map<String, List<AccessSourceDO>> getBindingSourceMap(Collection<String> eventCodes);

    String getDeleteBlockedReason(AccessSourceDO accessSource);

}
