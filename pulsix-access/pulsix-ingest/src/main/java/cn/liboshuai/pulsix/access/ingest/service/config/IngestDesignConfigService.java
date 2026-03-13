package cn.liboshuai.pulsix.access.ingest.service.config;

import cn.liboshuai.pulsix.access.ingest.domain.config.IngestRuntimeConfig;

public interface IngestDesignConfigService {

    IngestRuntimeConfig getConfig(String sourceCode, String sceneCode, String eventCode);

    void invalidate(String sourceCode, String sceneCode, String eventCode);

    void clear();

}
