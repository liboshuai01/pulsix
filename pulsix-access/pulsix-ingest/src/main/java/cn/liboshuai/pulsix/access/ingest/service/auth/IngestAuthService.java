package cn.liboshuai.pulsix.access.ingest.service.auth;

import cn.liboshuai.pulsix.access.ingest.domain.config.IngestRuntimeConfig;
import cn.liboshuai.pulsix.framework.common.biz.risk.access.dto.AccessIngestRequestDTO;

public interface IngestAuthService {

    void authenticate(AccessIngestRequestDTO request, IngestRuntimeConfig runtimeConfig);

}
