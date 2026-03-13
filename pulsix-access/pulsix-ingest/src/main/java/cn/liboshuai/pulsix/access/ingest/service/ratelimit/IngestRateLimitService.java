package cn.liboshuai.pulsix.access.ingest.service.ratelimit;

import cn.liboshuai.pulsix.access.ingest.domain.config.IngestRuntimeConfig;

public interface IngestRateLimitService {

    void checkAllowed(IngestRuntimeConfig runtimeConfig);

}
