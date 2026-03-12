package cn.liboshuai.pulsix.module.risk.service.ingestmapping;

import cn.liboshuai.pulsix.framework.common.pojo.PageResult;
import cn.liboshuai.pulsix.module.risk.controller.admin.ingestmapping.vo.IngestMappingPageReqVO;
import cn.liboshuai.pulsix.module.risk.controller.admin.ingestmapping.vo.IngestMappingPreviewReqVO;
import cn.liboshuai.pulsix.module.risk.controller.admin.ingestmapping.vo.IngestMappingPreviewRespVO;
import cn.liboshuai.pulsix.module.risk.controller.admin.ingestmapping.vo.IngestMappingSaveReqVO;
import cn.liboshuai.pulsix.module.risk.dal.dataobject.ingestmapping.IngestMappingDO;

public interface IngestMappingService {

    Long createIngestMapping(IngestMappingSaveReqVO createReqVO);

    void updateIngestMapping(IngestMappingSaveReqVO updateReqVO);

    void deleteIngestMapping(Long id);

    IngestMappingDO getIngestMapping(Long id);

    PageResult<IngestMappingDO> getIngestMappingPage(IngestMappingPageReqVO pageReqVO);

    IngestMappingPreviewRespVO previewIngestMapping(IngestMappingPreviewReqVO reqVO);

}
