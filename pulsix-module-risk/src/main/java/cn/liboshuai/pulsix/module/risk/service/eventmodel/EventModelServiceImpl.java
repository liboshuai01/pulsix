package cn.liboshuai.pulsix.module.risk.service.eventmodel;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.liboshuai.pulsix.framework.common.pojo.PageResult;
import cn.liboshuai.pulsix.framework.common.util.json.JsonUtils;
import cn.liboshuai.pulsix.module.risk.controller.admin.eventmodel.vo.EventFieldItemVO;
import cn.liboshuai.pulsix.module.risk.controller.admin.eventmodel.vo.EventModelPreviewRespVO;
import cn.liboshuai.pulsix.module.risk.controller.admin.eventmodel.vo.EventModelSaveReqVO;
import cn.liboshuai.pulsix.module.risk.convert.eventmodel.EventModelConvert;
import cn.liboshuai.pulsix.module.risk.controller.admin.eventmodel.vo.EventModelPageReqVO;
import cn.liboshuai.pulsix.module.risk.dal.dataobject.accesssource.AccessSourceDO;
import cn.liboshuai.pulsix.module.risk.dal.dataobject.accesssource.EventAccessBindingDO;
import cn.liboshuai.pulsix.module.risk.dal.dataobject.eventmodel.EventFieldDefDO;
import cn.liboshuai.pulsix.module.risk.dal.dataobject.eventmodel.EventSchemaDO;
import cn.liboshuai.pulsix.module.risk.dal.dataobject.scene.SceneDO;
import cn.liboshuai.pulsix.module.risk.dal.mysql.accesssource.AccessSourceMapper;
import cn.liboshuai.pulsix.module.risk.dal.mysql.accesssource.EventAccessBindingMapper;
import cn.liboshuai.pulsix.module.risk.dal.mysql.eventmodel.EventFieldDefMapper;
import cn.liboshuai.pulsix.module.risk.dal.mysql.eventmodel.EventSchemaMapper;
import cn.liboshuai.pulsix.module.risk.dal.mysql.scene.SceneMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import static cn.liboshuai.pulsix.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.liboshuai.pulsix.module.risk.enums.ErrorCodeConstants.EVENT_MODEL_BINDING_DUPLICATE;
import static cn.liboshuai.pulsix.module.risk.enums.ErrorCodeConstants.EVENT_MODEL_BINDING_REQUIRED;
import static cn.liboshuai.pulsix.module.risk.enums.ErrorCodeConstants.EVENT_MODEL_BINDING_SCENE_MISMATCH;
import static cn.liboshuai.pulsix.module.risk.enums.ErrorCodeConstants.EVENT_MODEL_BINDING_SOURCE_NOT_EXISTS;
import static cn.liboshuai.pulsix.module.risk.enums.ErrorCodeConstants.EVENT_MODEL_CODE_DUPLICATE;
import static cn.liboshuai.pulsix.module.risk.enums.ErrorCodeConstants.EVENT_MODEL_DELETE_DENIED;
import static cn.liboshuai.pulsix.module.risk.enums.ErrorCodeConstants.EVENT_MODEL_FIELD_DUPLICATE;
import static cn.liboshuai.pulsix.module.risk.enums.ErrorCodeConstants.EVENT_MODEL_FIELD_INVALID;
import static cn.liboshuai.pulsix.module.risk.enums.ErrorCodeConstants.EVENT_MODEL_IDENTITY_IMMUTABLE;
import static cn.liboshuai.pulsix.module.risk.enums.ErrorCodeConstants.EVENT_MODEL_NOT_EXISTS;
import static cn.liboshuai.pulsix.module.risk.enums.ErrorCodeConstants.SCENE_NOT_EXISTS;

@Service
@Validated
public class EventModelServiceImpl implements EventModelService {

    private static final Object MISSING_VALUE = new Object();

    @Resource
    private EventSchemaMapper eventSchemaMapper;
    @Resource
    private EventFieldDefMapper eventFieldDefMapper;
    @Resource
    private AccessSourceMapper accessSourceMapper;
    @Resource
    private EventAccessBindingMapper eventAccessBindingMapper;
    @Resource
    private SceneMapper sceneMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createEventModel(EventModelSaveReqVO createReqVO) {
        validateSceneExists(createReqVO.getSceneCode());
        validateEventCodeUnique(null, createReqVO.getEventCode());
        validateBindingSources(createReqVO.getSceneCode(), createReqVO.getBindingSourceCodes());

        DraftValidationResult validationResult = validateDraft(createReqVO);
        throwIfInvalid(validationResult);

        EventSchemaDO schema = EventModelConvert.INSTANCE.convert(createReqVO);
        schema.setVersion(1);
        eventSchemaMapper.insert(schema);
        insertFieldList(buildFieldDOList(createReqVO.getEventCode(), validationResult.normalizedFields()));
        insertBindingList(buildBindingDOList(createReqVO.getEventCode(), createReqVO.getBindingSourceCodes()));
        return schema.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateEventModel(EventModelSaveReqVO updateReqVO) {
        EventSchemaDO schema = validateEventModelExists(updateReqVO.getId());
        validateEventModelIdentityImmutable(schema, updateReqVO.getSceneCode(), updateReqVO.getEventCode(),
                updateReqVO.getEventType());
        validateSceneExists(updateReqVO.getSceneCode());
        validateEventCodeUnique(updateReqVO.getId(), updateReqVO.getEventCode());
        validateBindingSources(updateReqVO.getSceneCode(), updateReqVO.getBindingSourceCodes());

        DraftValidationResult validationResult = validateDraft(updateReqVO);
        throwIfInvalid(validationResult);

        EventSchemaDO updateObj = EventModelConvert.INSTANCE.convert(updateReqVO);
        updateObj.setVersion(schema.getVersion() == null ? 1 : schema.getVersion() + 1);
        updateObj.setStatus(schema.getStatus());
        eventSchemaMapper.updateById(updateObj);

        // The field table has a unique key on (event_code, field_name), so update must
        // physically remove old rows before re-inserting the normalized field list.
        eventFieldDefMapper.deleteByEventCodePhysically(schema.getEventCode());
        insertFieldList(buildFieldDOList(schema.getEventCode(), validationResult.normalizedFields()));
        eventAccessBindingMapper.deleteByEventCodePhysically(schema.getEventCode());
        insertBindingList(buildBindingDOList(schema.getEventCode(), updateReqVO.getBindingSourceCodes()));
    }

    @Override
    public void updateEventModelStatus(Long id, Integer status) {
        validateEventModelExists(id);

        EventSchemaDO updateObj = new EventSchemaDO();
        updateObj.setId(id);
        updateObj.setStatus(status);
        eventSchemaMapper.updateById(updateObj);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteEventModel(Long id) {
        EventSchemaDO schema = validateEventModelExists(id);
        validateEventModelDeleteAllowed(schema);
        eventFieldDefMapper.deleteByEventCodePhysically(schema.getEventCode());
        eventAccessBindingMapper.deleteByEventCodePhysically(schema.getEventCode());
        eventSchemaMapper.deleteById(id);
    }

    @Override
    public EventSchemaDO getEventModel(Long id) {
        return eventSchemaMapper.selectById(id);
    }

    @Override
    public List<EventFieldDefDO> getEventFieldList(String eventCode) {
        if (StrUtil.isBlank(eventCode)) {
            return Collections.emptyList();
        }
        return eventFieldDefMapper.selectListByEventCode(eventCode);
    }

    @Override
    public PageResult<EventSchemaDO> getEventModelPage(EventModelPageReqVO pageReqVO) {
        return eventSchemaMapper.selectPage(pageReqVO);
    }

    @Override
    public List<EventSchemaDO> getSimpleEventModelList(String sceneCode) {
        return eventSchemaMapper.selectEnabledList(sceneCode);
    }

    @Override
    public EventModelPreviewRespVO previewStandardEvent(EventModelSaveReqVO reqVO) {
        DraftValidationResult validationResult = validateDraft(reqVO);
        EventModelPreviewRespVO respVO = new EventModelPreviewRespVO();
        respVO.setStandardEventJson(validationResult.standardEventJson());
        respVO.setRequiredFields(validationResult.requiredFields());
        respVO.setOptionalFields(validationResult.optionalFields());
        respVO.setFieldTypes(validationResult.fieldTypes());
        return respVO;
    }

    private void validateSceneExists(String sceneCode) {
        SceneDO scene = sceneMapper.selectBySceneCode(sceneCode);
        if (scene == null) {
            throw exception(SCENE_NOT_EXISTS);
        }
    }

    private EventSchemaDO validateEventModelExists(Long id) {
        if (id == null) {
            throw exception(EVENT_MODEL_NOT_EXISTS);
        }
        EventSchemaDO schema = eventSchemaMapper.selectById(id);
        if (schema == null) {
            throw exception(EVENT_MODEL_NOT_EXISTS);
        }
        return schema;
    }

    private void validateEventCodeUnique(Long id, String eventCode) {
        EventSchemaDO schema = eventSchemaMapper.selectByEventCode(eventCode);
        if (schema == null) {
            return;
        }
        if (id == null || !ObjectUtil.equal(schema.getId(), id)) {
            throw exception(EVENT_MODEL_CODE_DUPLICATE, eventCode);
        }
    }

    private void validateEventModelIdentityImmutable(EventSchemaDO schema, String sceneCode, String eventCode,
                                                     String eventType) {
        if (!ObjectUtil.equal(schema.getSceneCode(), sceneCode)
                || !ObjectUtil.equal(schema.getEventCode(), eventCode)
                || !ObjectUtil.equal(schema.getEventType(), eventType)) {
            throw exception(EVENT_MODEL_IDENTITY_IMMUTABLE);
        }
    }

    private void validateEventModelDeleteAllowed(EventSchemaDO schema) {
        if (eventSchemaMapper.selectFeatureCountByEventCode(schema.getEventCode()) > 0) {
            throw exception(EVENT_MODEL_DELETE_DENIED, schema.getEventCode());
        }
    }

    private void validateBindingSources(String sceneCode, List<String> bindingSourceCodes) {
        if (bindingSourceCodes == null || bindingSourceCodes.isEmpty()) {
            throw exception(EVENT_MODEL_BINDING_REQUIRED);
        }

        Set<String> sourceCodeSet = new LinkedHashSet<>();
        for (String sourceCode : bindingSourceCodes) {
            if (!sourceCodeSet.add(sourceCode)) {
                throw exception(EVENT_MODEL_BINDING_DUPLICATE, sourceCode);
            }
        }

        Map<String, AccessSourceDO> sourceMap = new LinkedHashMap<>();
        for (AccessSourceDO source : accessSourceMapper.selectListBySourceCodes(sourceCodeSet)) {
            sourceMap.put(source.getSourceCode(), source);
        }

        for (String sourceCode : sourceCodeSet) {
            AccessSourceDO source = sourceMap.get(sourceCode);
            if (source == null) {
                throw exception(EVENT_MODEL_BINDING_SOURCE_NOT_EXISTS, sourceCode);
            }
            List<String> allowedSceneCodes = source.getAllowedSceneCodes();
            if (allowedSceneCodes == null || !allowedSceneCodes.contains(sceneCode)) {
                throw exception(EVENT_MODEL_BINDING_SCENE_MISMATCH, sourceCode, sceneCode);
            }
        }
    }

    private void throwIfInvalid(DraftValidationResult validationResult) {
        if (validationResult.duplicateFieldName() != null) {
            throw exception(EVENT_MODEL_FIELD_DUPLICATE, validationResult.duplicateFieldName());
        }
        if (!validationResult.messages().isEmpty()) {
            throw exception(EVENT_MODEL_FIELD_INVALID, validationResult.messages().get(0));
        }
    }

    private DraftValidationResult validateDraft(EventModelSaveReqVO reqVO) {
        List<EventFieldDefDO> normalizedFields = normalizeFields(reqVO.getFields());
        List<String> messages = new ArrayList<>();
        LinkedHashMap<String, EventFieldDefDO> fieldMap = new LinkedHashMap<>();
        String duplicateFieldName = null;

        if (sceneMapper.selectBySceneCode(reqVO.getSceneCode()) == null) {
            messages.add("场景编码【" + reqVO.getSceneCode() + "】不存在");
        }

        for (EventFieldDefDO field : normalizedFields) {
            EventFieldDefDO previous = fieldMap.putIfAbsent(field.getFieldName(), field);
            if (previous != null && duplicateFieldName == null) {
                duplicateFieldName = field.getFieldName();
                messages.add("字段【" + field.getFieldName() + "】重复定义");
            }
            validateFieldValueConfig(field, messages);
        }

        LinkedHashMap<String, Object> standardEventJson = new LinkedHashMap<>();
        List<String> requiredFields = new ArrayList<>();
        List<String> optionalFields = new ArrayList<>();
        LinkedHashMap<String, String> fieldTypes = new LinkedHashMap<>();

        for (EventFieldDefDO field : normalizedFields) {
            fieldTypes.put(field.getFieldName(), field.getFieldType());
            if (Objects.equals(field.getRequiredFlag(), 1)) {
                requiredFields.add(field.getFieldName());
            } else {
                optionalFields.add(field.getFieldName());
            }

            Object value = resolvePreviewFieldValue(field, reqVO.getSceneCode(), reqVO.getEventType(), messages);
            if (value != MISSING_VALUE) {
                standardEventJson.put(field.getFieldName(), value);
            } else if (Objects.equals(field.getRequiredFlag(), 1)) {
                messages.add("必填字段【" + field.getFieldName() + "】缺少可用值");
            }
        }

        return new DraftValidationResult(normalizedFields, standardEventJson, requiredFields, optionalFields,
                fieldTypes, messages, duplicateFieldName);
    }

    private List<EventFieldDefDO> normalizeFields(List<EventFieldItemVO> fields) {
        List<EventFieldDefDO> fieldDOs = EventModelConvert.INSTANCE.convertFieldDOList(fields);
        List<FieldOrderHolder> holders = new ArrayList<>(fieldDOs.size());
        for (int i = 0; i < fieldDOs.size(); i++) {
            EventFieldDefDO field = fieldDOs.get(i);
            Integer sortNo = field.getSortNo() == null ? i + 1 : field.getSortNo();
            field.setSortNo(sortNo);
            holders.add(new FieldOrderHolder(field, i));
        }
        holders.sort(Comparator.comparingInt((FieldOrderHolder holder) -> holder.field().getSortNo())
                .thenComparingInt(FieldOrderHolder::index));

        List<EventFieldDefDO> normalized = new ArrayList<>(holders.size());
        for (int i = 0; i < holders.size(); i++) {
            EventFieldDefDO field = holders.get(i).field();
            field.setSortNo(i + 1);
            normalized.add(field);
        }
        return normalized;
    }

    private List<EventFieldDefDO> buildFieldDOList(String eventCode, List<EventFieldDefDO> normalizedFields) {
        List<EventFieldDefDO> fields = new ArrayList<>(normalizedFields.size());
        for (EventFieldDefDO field : normalizedFields) {
            EventFieldDefDO clone = new EventFieldDefDO();
            clone.setEventCode(eventCode);
            clone.setFieldName(field.getFieldName());
            clone.setFieldLabel(field.getFieldLabel());
            clone.setFieldType(field.getFieldType());
            clone.setRequiredFlag(field.getRequiredFlag());
            clone.setDefaultValue(field.getDefaultValue());
            clone.setSampleValue(field.getSampleValue());
            clone.setDescription(field.getDescription());
            clone.setSortNo(field.getSortNo());
            clone.setExtJson(field.getExtJson());
            fields.add(clone);
        }
        return fields;
    }

    private List<EventAccessBindingDO> buildBindingDOList(String eventCode, List<String> sourceCodes) {
        List<EventAccessBindingDO> bindings = new ArrayList<>(sourceCodes.size());
        for (String sourceCode : new LinkedHashSet<>(sourceCodes)) {
            EventAccessBindingDO binding = new EventAccessBindingDO();
            binding.setEventCode(eventCode);
            binding.setSourceCode(sourceCode);
            bindings.add(binding);
        }
        return bindings;
    }

    private void insertFieldList(Collection<EventFieldDefDO> fields) {
        if (fields.isEmpty()) {
            return;
        }
        eventFieldDefMapper.insertBatch(fields);
    }

    private void insertBindingList(Collection<EventAccessBindingDO> bindings) {
        if (bindings.isEmpty()) {
            return;
        }
        eventAccessBindingMapper.insertBatch(bindings);
    }

    private void validateFieldValueConfig(EventFieldDefDO field, List<String> messages) {
        if (StrUtil.isNotBlank(field.getSampleValue())) {
            convertConfiguredValue(field, field.getSampleValue(), "字段样例值", messages);
        }
        if (StrUtil.isNotBlank(field.getDefaultValue())) {
            convertConfiguredValue(field, field.getDefaultValue(), "字段默认值", messages);
        }
    }

    private Object resolvePreviewFieldValue(EventFieldDefDO field, String sceneCode, String eventType,
                                            List<String> messages) {
        return resolveFromConfiguredSources(field, sceneCode, eventType, messages);
    }

    private Object resolveFromConfiguredSources(EventFieldDefDO field, String sceneCode, String eventType,
                                                List<String> messages) {
        if (StrUtil.isNotBlank(field.getSampleValue())) {
            return convertConfiguredValue(field, field.getSampleValue(), "字段样例值", messages);
        }
        if (StrUtil.isNotBlank(field.getDefaultValue())) {
            return convertConfiguredValue(field, field.getDefaultValue(), "字段默认值", messages);
        }
        if ("sceneCode".equals(field.getFieldName())) {
            return sceneCode;
        }
        if ("eventType".equals(field.getFieldName())) {
            return eventType;
        }
        return MISSING_VALUE;
    }

    private Object convertConfiguredValue(EventFieldDefDO field, String rawValue, String source, List<String> messages) {
        if (StrUtil.isBlank(rawValue)) {
            return MISSING_VALUE;
        }
        return switch (field.getFieldType()) {
            case "STRING", "DATETIME" -> rawValue;
            case "INTEGER" -> convertInteger(field.getFieldName(), rawValue, source, messages);
            case "LONG" -> convertLong(field.getFieldName(), rawValue, source, messages);
            case "DECIMAL" -> convertDecimal(field.getFieldName(), rawValue, source, messages);
            case "BOOLEAN" -> convertBoolean(field.getFieldName(), rawValue, source, messages);
            case "JSON" -> convertJson(field.getFieldName(), rawValue, source, messages);
            default -> handleUnsupportedType(field.getFieldName(), field.getFieldType(), messages);
        };
    }

    private Object handleUnsupportedType(String fieldName, String fieldType, List<String> messages) {
        messages.add("字段【" + fieldName + "】使用了不支持的字段类型【" + fieldType + "】");
        return MISSING_VALUE;
    }

    private Object convertInteger(String fieldName, Object rawValue, String source, List<String> messages) {
        BigDecimal decimal = toBigDecimal(fieldName, rawValue, source, messages);
        if (decimal == null) {
            return MISSING_VALUE;
        }
        try {
            return decimal.intValueExact();
        } catch (ArithmeticException ex) {
            messages.add(source + "中的字段【" + fieldName + "】必须是整数");
            return MISSING_VALUE;
        }
    }

    private Object convertLong(String fieldName, Object rawValue, String source, List<String> messages) {
        BigDecimal decimal = toBigDecimal(fieldName, rawValue, source, messages);
        if (decimal == null) {
            return MISSING_VALUE;
        }
        try {
            return decimal.longValueExact();
        } catch (ArithmeticException ex) {
            messages.add(source + "中的字段【" + fieldName + "】必须是长整数");
            return MISSING_VALUE;
        }
    }

    private Object convertDecimal(String fieldName, Object rawValue, String source, List<String> messages) {
        BigDecimal decimal = toBigDecimal(fieldName, rawValue, source, messages);
        return decimal == null ? MISSING_VALUE : decimal;
    }

    private Object convertBoolean(String fieldName, Object rawValue, String source, List<String> messages) {
        if (rawValue instanceof Boolean bool) {
            return bool;
        }
        if (rawValue instanceof Number number) {
            int value = number.intValue();
            if (value == 0 || value == 1) {
                return value == 1;
            }
        }
        if (rawValue instanceof String text) {
            if ("true".equalsIgnoreCase(text) || "1".equals(text)) {
                return true;
            }
            if ("false".equalsIgnoreCase(text) || "0".equals(text)) {
                return false;
            }
        }
        messages.add(source + "中的字段【" + fieldName + "】必须是布尔值");
        return MISSING_VALUE;
    }

    private Object convertJson(String fieldName, Object rawValue, String source, List<String> messages) {
        Object jsonValue = rawValue;
        if (rawValue instanceof String text) {
            try {
                jsonValue = JsonUtils.parseObject(text, Object.class);
            } catch (RuntimeException ex) {
                messages.add(source + "中的字段【" + fieldName + "】不是合法的 JSON");
                return MISSING_VALUE;
            }
        }
        if (!(jsonValue instanceof Map) && !(jsonValue instanceof List)) {
            messages.add(source + "中的字段【" + fieldName + "】必须是 JSON 对象或数组");
            return MISSING_VALUE;
        }
        return jsonValue;
    }

    private BigDecimal toBigDecimal(String fieldName, Object rawValue, String source, List<String> messages) {
        if (rawValue instanceof BigDecimal decimal) {
            return decimal;
        }
        if (rawValue instanceof Number number) {
            return new BigDecimal(number.toString());
        }
        if (rawValue instanceof String text) {
            try {
                return new BigDecimal(text.trim());
            } catch (NumberFormatException ex) {
                messages.add(source + "中的字段【" + fieldName + "】必须是数字");
                return null;
            }
        }
        messages.add(source + "中的字段【" + fieldName + "】必须是数字");
        return null;
    }

    private record DraftValidationResult(
            List<EventFieldDefDO> normalizedFields,
            LinkedHashMap<String, Object> standardEventJson,
            List<String> requiredFields,
            List<String> optionalFields,
            LinkedHashMap<String, String> fieldTypes,
            List<String> messages,
            String duplicateFieldName
    ) {
    }

    private record FieldOrderHolder(EventFieldDefDO field, int index) {
    }

}
