package cn.liboshuai.pulsix.access.ingest.service.config;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.liboshuai.pulsix.access.ingest.dal.dataobject.eventfield.EventFieldDO;
import cn.liboshuai.pulsix.access.ingest.dal.dataobject.ingestmapping.IngestMappingDO;
import cn.liboshuai.pulsix.access.ingest.dal.dataobject.ingestsource.IngestSourceDO;
import cn.liboshuai.pulsix.access.ingest.dal.mysql.eventfield.EventFieldMapper;
import cn.liboshuai.pulsix.access.ingest.dal.mysql.ingestmapping.IngestMappingMapper;
import cn.liboshuai.pulsix.access.ingest.dal.mysql.ingestsource.IngestSourceMapper;
import cn.liboshuai.pulsix.access.ingest.domain.config.EventFieldConfig;
import cn.liboshuai.pulsix.access.ingest.domain.config.IngestMappingConfig;
import cn.liboshuai.pulsix.access.ingest.domain.config.IngestSourceConfig;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Repository;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;

@Repository
public class MybatisIngestDesignConfigRepository implements IngestDesignConfigRepository {

    @Resource
    private IngestSourceMapper ingestSourceMapper;

    @Resource
    private IngestMappingMapper ingestMappingMapper;

    @Resource
    private EventFieldMapper eventFieldMapper;

    @Override
    public Optional<IngestSourceConfig> findSource(String sourceCode) {
        return Optional.ofNullable(ingestSourceMapper.selectBySourceCode(sourceCode)).map(this::convertSource);
    }

    @Override
    public List<IngestMappingConfig> findEnabledMappings(String sourceCode, String sceneCode, String eventCode) {
        return ingestMappingMapper.selectEnabledList(sourceCode, sceneCode, eventCode).stream()
                .map(this::convertMapping)
                .toList();
    }

    @Override
    public List<EventFieldConfig> findEventFields(String sceneCode, String eventCode) {
        return eventFieldMapper.selectOrderedList(sceneCode, eventCode).stream()
                .map(this::convertEventField)
                .toList();
    }

    private IngestSourceConfig convertSource(IngestSourceDO source) {
        return IngestSourceConfig.builder()
                .sourceCode(source.getSourceCode())
                .sourceName(source.getSourceName())
                .sourceType(source.getSourceType())
                .authType(source.getAuthType())
                .authConfigJson(ObjectUtil.defaultIfNull(source.getAuthConfigJson(), new LinkedHashMap<>()))
                .sceneScope(CollUtil.isEmpty(source.getSceneScopeJson()) ? new LinkedHashSet<>() : new LinkedHashSet<>(source.getSceneScopeJson()))
                .standardTopicName(source.getStandardTopicName())
                .errorTopicName(source.getErrorTopicName())
                .rateLimitQps(source.getRateLimitQps())
                .status(source.getStatus())
                .description(source.getDescription())
                .build();
    }

    private IngestMappingConfig convertMapping(IngestMappingDO mapping) {
        return IngestMappingConfig.builder()
                .sourceCode(mapping.getSourceCode())
                .sceneCode(mapping.getSceneCode())
                .eventCode(mapping.getEventCode())
                .sourceFieldPath(mapping.getSourceFieldPath())
                .targetFieldCode(mapping.getTargetFieldCode())
                .targetFieldName(mapping.getTargetFieldName())
                .transformType(mapping.getTransformType())
                .transformExpr(mapping.getTransformExpr())
                .defaultValue(mapping.getDefaultValue())
                .requiredFlag(mapping.getRequiredFlag())
                .cleanRuleJson(ObjectUtil.defaultIfNull(mapping.getCleanRuleJson(), new LinkedHashMap<>()))
                .sortNo(mapping.getSortNo())
                .status(mapping.getStatus())
                .build();
    }

    private EventFieldConfig convertEventField(EventFieldDO eventField) {
        return EventFieldConfig.builder()
                .sceneCode(eventField.getSceneCode())
                .eventCode(eventField.getEventCode())
                .fieldCode(eventField.getFieldCode())
                .fieldName(eventField.getFieldName())
                .fieldType(eventField.getFieldType())
                .fieldPath(eventField.getFieldPath())
                .standardFieldFlag(eventField.getStandardFieldFlag())
                .requiredFlag(eventField.getRequiredFlag())
                .nullableFlag(eventField.getNullableFlag())
                .defaultValue(eventField.getDefaultValue())
                .sampleValue(eventField.getSampleValue())
                .validationRuleJson(ObjectUtil.defaultIfNull(eventField.getValidationRuleJson(), new LinkedHashMap<>()))
                .description(eventField.getDescription())
                .sortNo(eventField.getSortNo())
                .status(eventField.getStatus())
                .build();
    }

}
