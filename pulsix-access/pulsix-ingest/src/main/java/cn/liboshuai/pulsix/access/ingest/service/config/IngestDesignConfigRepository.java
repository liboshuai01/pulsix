package cn.liboshuai.pulsix.access.ingest.service.config;

import cn.liboshuai.pulsix.access.ingest.domain.config.EventFieldConfig;
import cn.liboshuai.pulsix.access.ingest.domain.config.IngestMappingConfig;
import cn.liboshuai.pulsix.access.ingest.domain.config.IngestSourceConfig;

import java.util.List;
import java.util.Optional;

public interface IngestDesignConfigRepository {

    Optional<IngestSourceConfig> findSource(String sourceCode);

    List<IngestMappingConfig> findEnabledMappings(String sourceCode, String sceneCode, String eventCode);

    List<EventFieldConfig> findEventFields(String sceneCode, String eventCode);

}
