package cn.liboshuai.pulsix.module.risk.service.eventsample;

import cn.hutool.core.util.StrUtil;
import cn.liboshuai.pulsix.framework.common.exception.util.ServiceExceptionUtil;
import cn.liboshuai.pulsix.framework.common.pojo.PageResult;
import cn.liboshuai.pulsix.framework.common.util.object.BeanUtils;
import cn.liboshuai.pulsix.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.liboshuai.pulsix.module.risk.controller.admin.eventsample.vo.EventSamplePageReqVO;
import cn.liboshuai.pulsix.module.risk.controller.admin.eventsample.vo.EventSamplePreviewRespVO;
import cn.liboshuai.pulsix.module.risk.controller.admin.eventsample.vo.EventSampleSaveReqVO;
import cn.liboshuai.pulsix.module.risk.dal.dataobject.eventfield.EventFieldDO;
import cn.liboshuai.pulsix.module.risk.dal.dataobject.eventsample.EventSampleDO;
import cn.liboshuai.pulsix.module.risk.dal.dataobject.eventschema.EventSchemaDO;
import cn.liboshuai.pulsix.module.risk.dal.mysql.eventfield.EventFieldMapper;
import cn.liboshuai.pulsix.module.risk.dal.mysql.eventsample.EventSampleMapper;
import cn.liboshuai.pulsix.module.risk.dal.mysql.eventschema.EventSchemaMapper;
import cn.liboshuai.pulsix.module.risk.enums.eventfield.RiskEventFieldTypeEnum;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static cn.liboshuai.pulsix.module.risk.enums.ErrorCodeConstants.EVENT_SAMPLE_CODE_DUPLICATE;
import static cn.liboshuai.pulsix.module.risk.enums.ErrorCodeConstants.EVENT_SAMPLE_NOT_EXISTS;
import static cn.liboshuai.pulsix.module.risk.enums.ErrorCodeConstants.EVENT_SCHEMA_NOT_EXISTS;

@Service
public class EventSampleServiceImpl implements EventSampleService {

    @Resource
    private EventSampleMapper eventSampleMapper;

    @Resource
    private EventSchemaMapper eventSchemaMapper;

    @Resource
    private EventFieldMapper eventFieldMapper;

    @Override
    public Long createEventSample(EventSampleSaveReqVO createReqVO) {
        validateEventSchemaExists(createReqVO.getSceneCode(), createReqVO.getEventCode());
        validateEventSampleCodeUnique(createReqVO.getSceneCode(), createReqVO.getEventCode(), createReqVO.getSampleCode(), null);
        EventSampleDO eventSample = BeanUtils.toBean(createReqVO, EventSampleDO.class);
        eventSampleMapper.insert(eventSample);
        return eventSample.getId();
    }

    @Override
    public void updateEventSample(EventSampleSaveReqVO updateReqVO) {
        EventSampleDO eventSample = validateEventSampleExists(updateReqVO.getId());
        EventSampleDO updateObj = BeanUtils.toBean(updateReqVO, EventSampleDO.class);
        updateObj.setSceneCode(eventSample.getSceneCode());
        updateObj.setEventCode(eventSample.getEventCode());
        updateObj.setSampleCode(eventSample.getSampleCode());
        eventSampleMapper.updateById(updateObj);
    }

    @Override
    public void deleteEventSample(Long id) {
        validateEventSampleExists(id);
        eventSampleMapper.deleteById(id);
    }

    @Override
    public EventSampleDO getEventSample(Long id) {
        return eventSampleMapper.selectById(id);
    }

    @Override
    public PageResult<EventSampleDO> getEventSamplePage(EventSamplePageReqVO pageReqVO) {
        return eventSampleMapper.selectPage(pageReqVO);
    }

    @Override
    public EventSamplePreviewRespVO previewEventSample(Long id) {
        EventSampleDO eventSample = validateEventSampleExists(id);
        List<EventFieldDO> eventFields = eventFieldMapper.selectList(new LambdaQueryWrapperX<EventFieldDO>()
                .eq(EventFieldDO::getSceneCode, eventSample.getSceneCode())
                .eq(EventFieldDO::getEventCode, eventSample.getEventCode())
                .orderByAsc(EventFieldDO::getSortNo)
                .orderByAsc(EventFieldDO::getId));

        Map<String, Object> sampleJson = eventSample.getSampleJson() == null
                ? new LinkedHashMap<>() : new LinkedHashMap<>(eventSample.getSampleJson());
        Map<String, Object> standardEventJson = new LinkedHashMap<>();
        List<String> missingRequiredFields = new ArrayList<>();
        List<String> defaultedFields = new ArrayList<>();

        for (EventFieldDO eventField : eventFields) {
            ValueLookupResult result = findFieldValue(sampleJson, eventField);
            Object value = result.getValue();
            if (!result.isFound() && StrUtil.isNotBlank(eventField.getDefaultValue())) {
                value = convertDefaultValue(eventField.getFieldType(), eventField.getDefaultValue());
                defaultedFields.add(eventField.getFieldCode());
            }
            if (value == null) {
                if (eventField.getRequiredFlag() != null && eventField.getRequiredFlag() == 1) {
                    missingRequiredFields.add(eventField.getFieldCode());
                }
                continue;
            }
            putFieldValue(standardEventJson, eventField, value);
        }

        EventSamplePreviewRespVO respVO = new EventSamplePreviewRespVO();
        respVO.setSampleId(eventSample.getId());
        respVO.setSampleCode(eventSample.getSampleCode());
        respVO.setSampleName(eventSample.getSampleName());
        respVO.setSampleType(eventSample.getSampleType());
        respVO.setSampleJson(sampleJson);
        respVO.setStandardEventJson(standardEventJson);
        respVO.setMissingRequiredFields(missingRequiredFields);
        respVO.setDefaultedFields(defaultedFields);
        return respVO;
    }

    private EventSampleDO validateEventSampleExists(Long id) {
        if (id == null) {
            throw ServiceExceptionUtil.exception(EVENT_SAMPLE_NOT_EXISTS);
        }
        EventSampleDO eventSample = eventSampleMapper.selectById(id);
        if (eventSample == null) {
            throw ServiceExceptionUtil.exception(EVENT_SAMPLE_NOT_EXISTS);
        }
        return eventSample;
    }

    private void validateEventSchemaExists(String sceneCode, String eventCode) {
        EventSchemaDO eventSchema = eventSchemaMapper.selectOne(EventSchemaDO::getSceneCode, sceneCode,
                EventSchemaDO::getEventCode, eventCode);
        if (eventSchema == null) {
            throw ServiceExceptionUtil.exception(EVENT_SCHEMA_NOT_EXISTS);
        }
    }

    private void validateEventSampleCodeUnique(String sceneCode, String eventCode, String sampleCode, Long id) {
        EventSampleDO eventSample = eventSampleMapper.selectOne(EventSampleDO::getSceneCode, sceneCode,
                EventSampleDO::getEventCode, eventCode, EventSampleDO::getSampleCode, sampleCode);
        if (eventSample == null) {
            return;
        }
        if (id == null || !id.equals(eventSample.getId())) {
            throw ServiceExceptionUtil.exception(EVENT_SAMPLE_CODE_DUPLICATE);
        }
    }

    private ValueLookupResult findFieldValue(Map<String, Object> sampleJson, EventFieldDO eventField) {
        ValueLookupResult byCode = lookupByPath(sampleJson, eventField.getFieldCode());
        if (byCode.isFound()) {
            return byCode;
        }
        if (StrUtil.isBlank(eventField.getFieldPath())) {
            return ValueLookupResult.notFound();
        }
        return lookupByPath(sampleJson, normalizeFieldPath(eventField));
    }

    private String normalizeFieldPath(EventFieldDO eventField) {
        String fieldPath = eventField.getFieldPath();
        if (StrUtil.isBlank(fieldPath)) {
            return eventField.getFieldCode();
        }
        return StrUtil.removePrefix(fieldPath, "$." );
    }

    @SuppressWarnings("unchecked")
    private ValueLookupResult lookupByPath(Map<String, Object> sampleJson, String path) {
        if (sampleJson == null || StrUtil.isBlank(path)) {
            return ValueLookupResult.notFound();
        }
        String[] segments = StrUtil.splitToArray(path, '.');
        Object current = sampleJson;
        for (String segment : segments) {
            if (!(current instanceof Map<?, ?> currentMap) || !currentMap.containsKey(segment)) {
                return ValueLookupResult.notFound();
            }
            current = ((Map<String, Object>) currentMap).get(segment);
        }
        return ValueLookupResult.found(current);
    }

    @SuppressWarnings("unchecked")
    private void putFieldValue(Map<String, Object> standardEventJson, EventFieldDO eventField, Object value) {
        String[] segments = StrUtil.splitToArray(normalizeFieldPath(eventField), '.');
        if (segments == null || segments.length == 0) {
            standardEventJson.put(eventField.getFieldCode(), value);
            return;
        }
        Map<String, Object> current = standardEventJson;
        for (int i = 0; i < segments.length - 1; i++) {
            Object next = current.get(segments[i]);
            if (!(next instanceof Map<?, ?>)) {
                next = new LinkedHashMap<String, Object>();
                current.put(segments[i], next);
            }
            current = (Map<String, Object>) next;
        }
        current.put(segments[segments.length - 1], value);
    }

    private Object convertDefaultValue(String fieldType, String defaultValue) {
        if (StrUtil.isBlank(defaultValue)) {
            return null;
        }
        try {
            if (RiskEventFieldTypeEnum.LONG.getType().equals(fieldType)) {
                return Long.valueOf(defaultValue);
            }
            if (RiskEventFieldTypeEnum.DECIMAL.getType().equals(fieldType)) {
                return new BigDecimal(defaultValue);
            }
            if (RiskEventFieldTypeEnum.BOOLEAN.getType().equals(fieldType)) {
                if ("1".equals(defaultValue)) {
                    return Boolean.TRUE;
                }
                if ("0".equals(defaultValue)) {
                    return Boolean.FALSE;
                }
                return Boolean.valueOf(defaultValue);
            }
            if (RiskEventFieldTypeEnum.JSON.getType().equals(fieldType) && cn.liboshuai.pulsix.framework.common.util.json.JsonUtils.isJson(defaultValue)) {
                return cn.liboshuai.pulsix.framework.common.util.json.JsonUtils.parseObject(defaultValue, Object.class);
            }
        } catch (Exception ignored) {
            return defaultValue;
        }
        return defaultValue;
    }

    private static final class ValueLookupResult {

        private final boolean found;
        private final Object value;

        private ValueLookupResult(boolean found, Object value) {
            this.found = found;
            this.value = value;
        }

        public static ValueLookupResult found(Object value) {
            return new ValueLookupResult(true, value);
        }

        public static ValueLookupResult notFound() {
            return new ValueLookupResult(false, null);
        }

        public Boolean getFound() {
            return found;
        }

        public boolean isFound() {
            return found;
        }

        public Object getValue() {
            return value;
        }

    }

}
