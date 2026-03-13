package cn.liboshuai.pulsix.access.ingest.domain.config;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IngestRuntimeConfig {

    private String sourceCode;

    private String sceneCode;

    private String eventCode;

    private IngestSourceConfig source;

    @Builder.Default
    private List<IngestMappingConfig> mappings = Collections.emptyList();

    @Builder.Default
    private List<EventFieldConfig> eventFields = Collections.emptyList();

    private Instant loadedAt;

    public Map<String, IngestMappingConfig> mappingsByTargetField() {
        return mappings.stream().collect(Collectors.toMap(IngestMappingConfig::getTargetFieldCode,
                item -> item, (left, right) -> left, LinkedHashMap::new));
    }

    public Map<String, IngestMappingConfig> getMappingsByTargetField() {
        return mappingsByTargetField();
    }

}
