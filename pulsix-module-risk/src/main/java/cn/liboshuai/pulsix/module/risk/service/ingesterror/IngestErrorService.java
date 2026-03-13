package cn.liboshuai.pulsix.module.risk.service.ingesterror;

import cn.liboshuai.pulsix.framework.common.pojo.PageResult;
import cn.liboshuai.pulsix.module.risk.controller.admin.ingesterror.vo.IngestErrorDetailRespVO;
import cn.liboshuai.pulsix.module.risk.controller.admin.ingesterror.vo.IngestErrorPageReqVO;
import cn.liboshuai.pulsix.module.risk.controller.admin.ingesterror.vo.IngestErrorRespVO;

public interface IngestErrorService {

    PageResult<IngestErrorRespVO> getIngestErrorPage(IngestErrorPageReqVO pageReqVO);

    IngestErrorDetailRespVO getIngestError(Long id);

}
