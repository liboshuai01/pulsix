package cn.liboshuai.pulsix.module.risk.service.accessmapping;

import cn.liboshuai.pulsix.framework.common.pojo.PageResult;
import cn.liboshuai.pulsix.module.risk.controller.admin.accessmapping.vo.AccessMappingPageReqVO;
import cn.liboshuai.pulsix.module.risk.controller.admin.accessmapping.vo.AccessMappingPreviewRespVO;
import cn.liboshuai.pulsix.module.risk.controller.admin.accessmapping.vo.AccessMappingSaveReqVO;
import cn.liboshuai.pulsix.module.risk.dal.dataobject.accessmapping.EventAccessMappingRuleDO;
import cn.liboshuai.pulsix.module.risk.dal.dataobject.accessmapping.EventAccessRawFieldDefDO;
import cn.liboshuai.pulsix.module.risk.dal.dataobject.accesssource.EventAccessBindingDO;
import cn.liboshuai.pulsix.module.risk.service.accessmapping.bo.AccessMappingRuntimeBO;

import java.util.List;

public interface AccessMappingService {

    Long createAccessMapping(AccessMappingSaveReqVO createReqVO);

    void updateAccessMapping(AccessMappingSaveReqVO updateReqVO);

    void deleteAccessMapping(Long id);

    EventAccessBindingDO getAccessMapping(Long id);

    List<EventAccessRawFieldDefDO> getRawFieldList(Long bindingId);

    List<EventAccessMappingRuleDO> getMappingRuleList(Long bindingId);

    PageResult<EventAccessBindingDO> getAccessMappingPage(AccessMappingPageReqVO pageReqVO);

    AccessMappingPreviewRespVO previewStandardEvent(AccessMappingSaveReqVO reqVO);

    AccessMappingRuntimeBO getRuntimeAccessMapping(String sourceCode, String eventType);

}
