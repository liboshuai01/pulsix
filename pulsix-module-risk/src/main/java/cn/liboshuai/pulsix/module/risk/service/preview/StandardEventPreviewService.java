package cn.liboshuai.pulsix.module.risk.service.preview;

import cn.hutool.core.util.StrUtil;
import cn.liboshuai.pulsix.framework.common.biz.risk.access.normalize.StandardEventFieldDefinition;
import cn.liboshuai.pulsix.framework.common.biz.risk.access.normalize.StandardEventMappingDefinition;
import cn.liboshuai.pulsix.framework.common.biz.risk.access.normalize.StandardEventNormalizeResult;
import cn.liboshuai.pulsix.framework.common.biz.risk.access.normalize.StandardEventNormalizer;
import cn.liboshuai.pulsix.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.liboshuai.pulsix.module.risk.dal.dataobject.eventfield.EventFieldDO;
import cn.liboshuai.pulsix.module.risk.dal.dataobject.ingestmapping.IngestMappingDO;
import cn.liboshuai.pulsix.module.risk.dal.mysql.eventfield.EventFieldMapper;
import cn.liboshuai.pulsix.module.risk.dal.mysql.ingestmapping.IngestMappingMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class StandardEventPreviewService {

    @Resource
    private EventFieldMapper eventFieldMapper;

    @Resource
    private IngestMappingMapper ingestMappingMapper;

    public StandardEventPreviewResult preview(String sceneCode, String eventCode, String sourceCode,
                                              Map<String, Object> rawEventJson) {
        List<EventFieldDO> eventFields = eventFieldMapper.selectList(new LambdaQueryWrapperX<EventFieldDO>()
                .eq(EventFieldDO::getSceneCode, sceneCode)
                .eq(EventFieldDO::getEventCode, eventCode)
                .orderByAsc(EventFieldDO::getSortNo)
                .orderByAsc(EventFieldDO::getId));

        List<IngestMappingDO> ingestMappings = StrUtil.isBlank(sourceCode)
                ? List.of()
                : ingestMappingMapper.selectEnabledList(sourceCode, sceneCode, eventCode);

        StandardEventNormalizeResult normalizeResult = StandardEventNormalizer.normalize(
                rawEventJson == null ? new LinkedHashMap<>() : new LinkedHashMap<>(rawEventJson),
                eventFields.stream().map(this::toFieldDefinition).toList(),
                ingestMappings.stream().map(this::toMappingDefinition).toList(),
                ZoneId.systemDefault());

        StandardEventPreviewResult result = new StandardEventPreviewResult();
        result.setRawEventJson(normalizeResult.getRawEventJson());
        result.setStandardEventJson(normalizeResult.getStandardEventJson());
        result.setMissingRequiredFields(normalizeResult.getMissingRequiredFields());
        result.setDefaultedFields(normalizeResult.getDefaultedFields());
        result.setMappedFields(normalizeResult.getMappedFields());
        return result;
    }

    private StandardEventFieldDefinition toFieldDefinition(EventFieldDO eventField) {
        return StandardEventFieldDefinition.builder()
                .fieldCode(eventField.getFieldCode())
                .fieldType(eventField.getFieldType())
                .fieldPath(eventField.getFieldPath())
                .requiredFlag(eventField.getRequiredFlag())
                .defaultValue(eventField.getDefaultValue())
                .build();
    }

    private StandardEventMappingDefinition toMappingDefinition(IngestMappingDO ingestMapping) {
        return StandardEventMappingDefinition.builder()
                .sourceFieldPath(ingestMapping.getSourceFieldPath())
                .targetFieldCode(ingestMapping.getTargetFieldCode())
                .transformType(ingestMapping.getTransformType())
                .transformExpr(ingestMapping.getTransformExpr())
                .defaultValue(ingestMapping.getDefaultValue())
                .cleanRuleJson(ingestMapping.getCleanRuleJson())
                .build();
    }

}
