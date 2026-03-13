package cn.liboshuai.pulsix.access.ingest.service;

import cn.liboshuai.pulsix.framework.common.biz.risk.access.dto.AccessIngestRequestDTO;
import cn.liboshuai.pulsix.framework.common.biz.risk.access.dto.AccessIngestResponseDTO;

public interface IngestPipelineService {

    AccessIngestResponseDTO ingest(AccessIngestRequestDTO request);

}
