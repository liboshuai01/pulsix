package cn.liboshuai.pulsix.access.ingest.service.normalize;

import cn.liboshuai.pulsix.access.ingest.config.PulsixIngestProperties;
import cn.liboshuai.pulsix.access.ingest.domain.config.EventFieldConfig;
import cn.liboshuai.pulsix.access.ingest.domain.config.IngestMappingConfig;
import cn.liboshuai.pulsix.access.ingest.domain.config.IngestRuntimeConfig;
import cn.liboshuai.pulsix.access.ingest.service.config.IngestDesignConfigService;
import cn.liboshuai.pulsix.framework.common.biz.risk.access.normalize.StandardEventFieldDefinition;
import cn.liboshuai.pulsix.framework.common.biz.risk.access.normalize.StandardEventMappingDefinition;
import cn.liboshuai.pulsix.framework.common.biz.risk.access.normalize.StandardEventNormalizeResult;
import cn.liboshuai.pulsix.framework.common.biz.risk.access.normalize.StandardEventNormalizer;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.time.ZoneId;
import java.util.List;
import java.util.Map;

@Service
public class ConfigDrivenStandardEventNormalizationService implements StandardEventNormalizationService {

    @Resource
    private IngestDesignConfigService configService;

    @Resource
    private PulsixIngestProperties properties;

    @Override
    public StandardEventNormalizeResult normalize(String sourceCode, String sceneCode, String eventCode,
                                                  Map<String, Object> rawEventJson) {
        IngestRuntimeConfig runtimeConfig = configService.getConfig(sourceCode, sceneCode, eventCode);
        List<StandardEventFieldDefinition> fieldDefinitions = runtimeConfig.getEventFields().stream()
                .map(this::toFieldDefinition)
                .toList();
        List<StandardEventMappingDefinition> mappingDefinitions = runtimeConfig.getMappings().stream()
                .map(this::toMappingDefinition)
                .toList();
        return StandardEventNormalizer.normalize(rawEventJson, fieldDefinitions, mappingDefinitions,
                ZoneId.of(properties.getZoneId()));
    }

    private StandardEventFieldDefinition toFieldDefinition(EventFieldConfig fieldConfig) {
        return StandardEventFieldDefinition.builder()
                .fieldCode(fieldConfig.getFieldCode())
                .fieldType(fieldConfig.getFieldType())
                .fieldPath(fieldConfig.getFieldPath())
                .requiredFlag(fieldConfig.getRequiredFlag())
                .defaultValue(fieldConfig.getDefaultValue())
                .build();
    }

    private StandardEventMappingDefinition toMappingDefinition(IngestMappingConfig mappingConfig) {
        return StandardEventMappingDefinition.builder()
                .sourceFieldPath(mappingConfig.getSourceFieldPath())
                .targetFieldCode(mappingConfig.getTargetFieldCode())
                .transformType(mappingConfig.getTransformType())
                .transformExpr(mappingConfig.getTransformExpr())
                .defaultValue(mappingConfig.getDefaultValue())
                .cleanRuleJson(mappingConfig.getCleanRuleJson())
                .build();
    }

}
