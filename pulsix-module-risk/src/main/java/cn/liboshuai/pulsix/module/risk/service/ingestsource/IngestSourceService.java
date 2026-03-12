package cn.liboshuai.pulsix.module.risk.service.ingestsource;

import cn.liboshuai.pulsix.framework.common.pojo.PageResult;
import cn.liboshuai.pulsix.module.risk.controller.admin.ingestsource.vo.IngestSourcePageReqVO;
import cn.liboshuai.pulsix.module.risk.controller.admin.ingestsource.vo.IngestSourceSaveReqVO;
import cn.liboshuai.pulsix.module.risk.dal.dataobject.ingestsource.IngestSourceDO;

public interface IngestSourceService {

    Long createIngestSource(IngestSourceSaveReqVO createReqVO);

    void updateIngestSource(IngestSourceSaveReqVO updateReqVO);

    void updateIngestSourceStatus(Long id, Integer status);

    IngestSourceDO getIngestSource(Long id);

    PageResult<IngestSourceDO> getIngestSourcePage(IngestSourcePageReqVO pageReqVO);

}
