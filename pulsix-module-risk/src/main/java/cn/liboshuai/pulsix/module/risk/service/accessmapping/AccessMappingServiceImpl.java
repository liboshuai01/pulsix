package cn.liboshuai.pulsix.module.risk.service.accessmapping;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.liboshuai.pulsix.framework.common.pojo.PageResult;
import cn.liboshuai.pulsix.framework.common.util.json.JsonUtils;
import cn.liboshuai.pulsix.module.risk.controller.admin.accessmapping.vo.AccessMappingPageReqVO;
import cn.liboshuai.pulsix.module.risk.controller.admin.accessmapping.vo.AccessMappingPreviewRespVO;
import cn.liboshuai.pulsix.module.risk.controller.admin.accessmapping.vo.AccessMappingRuleItemVO;
import cn.liboshuai.pulsix.module.risk.controller.admin.accessmapping.vo.AccessMappingSaveReqVO;
import cn.liboshuai.pulsix.module.risk.controller.admin.accessmapping.vo.AccessRawFieldItemVO;
import cn.liboshuai.pulsix.module.risk.convert.accessmapping.AccessMappingConvert;
import cn.liboshuai.pulsix.module.risk.dal.dataobject.accessmapping.EventAccessMappingRuleDO;
import cn.liboshuai.pulsix.module.risk.dal.dataobject.accessmapping.EventAccessRawFieldDefDO;
import cn.liboshuai.pulsix.module.risk.dal.dataobject.accesssource.AccessSourceDO;
import cn.liboshuai.pulsix.module.risk.dal.dataobject.accesssource.EventAccessBindingDO;
import cn.liboshuai.pulsix.module.risk.dal.dataobject.eventmodel.EventFieldDefDO;
import cn.liboshuai.pulsix.module.risk.dal.dataobject.eventmodel.EventSchemaDO;
import cn.liboshuai.pulsix.module.risk.dal.mysql.accessmapping.EventAccessMappingRuleMapper;
import cn.liboshuai.pulsix.module.risk.dal.mysql.accessmapping.EventAccessRawFieldDefMapper;
import cn.liboshuai.pulsix.module.risk.dal.mysql.accesssource.AccessSourceMapper;
import cn.liboshuai.pulsix.module.risk.dal.mysql.accesssource.EventAccessBindingMapper;
import cn.liboshuai.pulsix.module.risk.dal.mysql.eventmodel.EventFieldDefMapper;
import cn.liboshuai.pulsix.module.risk.dal.mysql.eventmodel.EventSchemaMapper;
import cn.liboshuai.pulsix.module.risk.enums.accessmapping.AccessMappingTypeEnum;
import cn.liboshuai.pulsix.module.risk.enums.accessmapping.AccessScriptEngineEnum;
import cn.liboshuai.pulsix.module.risk.service.accessmapping.bo.AccessMappingRuntimeBO;
import jakarta.annotation.Resource;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
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
import java.util.regex.Pattern;

import static cn.liboshuai.pulsix.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.liboshuai.pulsix.module.risk.enums.ErrorCodeConstants.ACCESS_MAPPING_BINDING_DUPLICATE;
import static cn.liboshuai.pulsix.module.risk.enums.ErrorCodeConstants.ACCESS_MAPPING_EVENT_NOT_EXISTS;
import static cn.liboshuai.pulsix.module.risk.enums.ErrorCodeConstants.ACCESS_MAPPING_NOT_EXISTS;
import static cn.liboshuai.pulsix.module.risk.enums.ErrorCodeConstants.ACCESS_MAPPING_PUBLIC_FIELD_INVALID;
import static cn.liboshuai.pulsix.module.risk.enums.ErrorCodeConstants.ACCESS_MAPPING_RAW_FIELD_PATH_DUPLICATE;
import static cn.liboshuai.pulsix.module.risk.enums.ErrorCodeConstants.ACCESS_MAPPING_RAW_FIELD_PATH_INVALID;
import static cn.liboshuai.pulsix.module.risk.enums.ErrorCodeConstants.ACCESS_MAPPING_ROUTE_CONFLICT;
import static cn.liboshuai.pulsix.module.risk.enums.ErrorCodeConstants.ACCESS_MAPPING_RULE_CONFIG_INVALID;
import static cn.liboshuai.pulsix.module.risk.enums.ErrorCodeConstants.ACCESS_MAPPING_SCENE_MISMATCH;
import static cn.liboshuai.pulsix.module.risk.enums.ErrorCodeConstants.ACCESS_MAPPING_SCRIPT_ENGINE_UNSUPPORTED;
import static cn.liboshuai.pulsix.module.risk.enums.ErrorCodeConstants.ACCESS_MAPPING_SOURCE_NOT_EXISTS;
import static cn.liboshuai.pulsix.module.risk.enums.ErrorCodeConstants.ACCESS_MAPPING_TARGET_FIELD_DUPLICATE;
import static cn.liboshuai.pulsix.module.risk.enums.ErrorCodeConstants.ACCESS_MAPPING_TARGET_FIELD_NOT_EXISTS;

@Service
@Validated
public class AccessMappingServiceImpl implements AccessMappingService {

    private static final Object MISSING_VALUE = new Object();
    private static final Pattern FIELD_PATH_PATTERN = Pattern.compile("^[A-Za-z_][A-Za-z0-9_]*(\\[[0-9]+\\])?(\\.[A-Za-z_][A-Za-z0-9_]*(\\[[0-9]+\\])?)*$");
    private static final String FIELD_SOURCE_MAPPING = "MAPPING";
    private static final String FIELD_SOURCE_EVENT_DEFAULT = "EVENT_DEFAULT";
    private static final String FIELD_SOURCE_SYSTEM_FILL = "SYSTEM_FILL";
    private static final Set<String> FIXED_PUBLIC_FIELDS = Set.of("sceneCode", "eventType");
    private static final ExpressionParser EXPRESSION_PARSER = new SpelExpressionParser();

    @Resource
    private EventAccessBindingMapper eventAccessBindingMapper;
    @Resource
    private EventAccessRawFieldDefMapper eventAccessRawFieldDefMapper;
    @Resource
    private EventAccessMappingRuleMapper eventAccessMappingRuleMapper;
    @Resource
    private EventSchemaMapper eventSchemaMapper;
    @Resource
    private EventFieldDefMapper eventFieldDefMapper;
    @Resource
    private AccessSourceMapper accessSourceMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createAccessMapping(AccessMappingSaveReqVO createReqVO) {
        EventSchemaDO eventSchema = validateEventExists(createReqVO.getEventCode());
        AccessSourceDO accessSource = validateSourceExists(createReqVO.getSourceCode());
        validateSceneCompatibility(eventSchema, accessSource);
        validateBindingUnique(null, eventSchema.getEventCode(), accessSource.getSourceCode());
        validateRouteUnique(null, accessSource.getSourceCode(), eventSchema.getEventType());

        DraftValidationResult validationResult = validateDraft(createReqVO, eventSchema, accessSource);
        throwIfInvalid(validationResult);

        EventAccessBindingDO binding = AccessMappingConvert.INSTANCE.convert(createReqVO);
        binding.setEventCode(eventSchema.getEventCode());
        binding.setSourceCode(accessSource.getSourceCode());
        eventAccessBindingMapper.insert(binding);
        insertRawFieldList(buildRawFieldDOList(binding.getId(), validationResult.normalizedRawFields()));
        insertMappingRuleList(buildMappingRuleDOList(binding.getId(), validationResult.normalizedMappingRules()));
        return binding.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateAccessMapping(AccessMappingSaveReqVO updateReqVO) {
        EventAccessBindingDO existing = validateAccessMappingExists(updateReqVO.getId());
        EventSchemaDO eventSchema = validateEventExists(updateReqVO.getEventCode());
        AccessSourceDO accessSource = validateSourceExists(updateReqVO.getSourceCode());
        validateSceneCompatibility(eventSchema, accessSource);
        validateBindingUnique(existing.getId(), eventSchema.getEventCode(), accessSource.getSourceCode());
        validateRouteUnique(existing.getId(), accessSource.getSourceCode(), eventSchema.getEventType());

        DraftValidationResult validationResult = validateDraft(updateReqVO, eventSchema, accessSource);
        throwIfInvalid(validationResult);

        EventAccessBindingDO updateObj = AccessMappingConvert.INSTANCE.convert(updateReqVO);
        updateObj.setId(existing.getId());
        updateObj.setEventCode(eventSchema.getEventCode());
        updateObj.setSourceCode(accessSource.getSourceCode());
        eventAccessBindingMapper.updateById(updateObj);

        eventAccessRawFieldDefMapper.deleteByBindingIdPhysically(existing.getId());
        eventAccessMappingRuleMapper.deleteByBindingIdPhysically(existing.getId());
        insertRawFieldList(buildRawFieldDOList(existing.getId(), validationResult.normalizedRawFields()));
        insertMappingRuleList(buildMappingRuleDOList(existing.getId(), validationResult.normalizedMappingRules()));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteAccessMapping(Long id) {
        EventAccessBindingDO binding = validateAccessMappingExists(id);
        eventAccessRawFieldDefMapper.deleteByBindingIdPhysically(binding.getId());
        eventAccessMappingRuleMapper.deleteByBindingIdPhysically(binding.getId());
        eventAccessBindingMapper.deleteByIdPhysically(binding.getId());
    }

    @Override
    public EventAccessBindingDO getAccessMapping(Long id) {
        EventAccessBindingDO binding = eventAccessBindingMapper.selectAccessMappingDetail(id);
        fillCountField(binding);
        return binding;
    }

    @Override
    public List<EventAccessRawFieldDefDO> getRawFieldList(Long bindingId) {
        if (bindingId == null) {
            return Collections.emptyList();
        }
        return eventAccessRawFieldDefMapper.selectListByBindingId(bindingId);
    }

    @Override
    public List<EventAccessMappingRuleDO> getMappingRuleList(Long bindingId) {
        if (bindingId == null) {
            return Collections.emptyList();
        }
        return eventAccessMappingRuleMapper.selectListByBindingId(bindingId);
    }

    @Override
    public PageResult<EventAccessBindingDO> getAccessMappingPage(AccessMappingPageReqVO pageReqVO) {
        PageResult<EventAccessBindingDO> pageResult = eventAccessBindingMapper.selectAccessMappingPage(pageReqVO);
        if (pageResult.getList() != null) {
            pageResult.getList().forEach(this::fillCountField);
        }
        return pageResult;
    }

    @Override
    public AccessMappingPreviewRespVO previewStandardEvent(AccessMappingSaveReqVO reqVO) {
        EventSchemaDO eventSchema = validateEventExists(reqVO.getEventCode());
        AccessSourceDO accessSource = validateSourceExists(reqVO.getSourceCode());
        validateSceneCompatibility(eventSchema, accessSource);

        DraftValidationResult validationResult = validateDraft(reqVO, eventSchema, accessSource);
        AccessMappingPreviewRespVO respVO = new AccessMappingPreviewRespVO();
        respVO.setStandardEventJson(validationResult.standardEventJson());
        respVO.setFieldSourceMap(validationResult.fieldSourceMap());
        respVO.setMessages(validationResult.messages());
        return respVO;
    }

    @Override
    public AccessMappingRuntimeBO getRuntimeAccessMapping(String sourceCode, String eventType) {
        if (StrUtil.isBlank(sourceCode) || StrUtil.isBlank(eventType)) {
            return null;
        }
        List<EventAccessBindingDO> bindings = eventAccessBindingMapper.selectAccessMappingListBySourceCodeAndEventType(sourceCode, eventType);
        if (bindings.isEmpty()) {
            return null;
        }
        if (bindings.size() > 1) {
            throw new IllegalStateException("接入映射路由冲突，sourceCode=" + sourceCode + ", eventType=" + eventType);
        }

        EventAccessBindingDO binding = bindings.get(0);
        AccessMappingRuntimeBO runtimeBO = new AccessMappingRuntimeBO();
        runtimeBO.setBinding(binding);
        runtimeBO.setStandardFields(getStandardFieldList(binding.getEventCode()));
        runtimeBO.setRawFields(getRawFieldList(binding.getId()));
        runtimeBO.setMappingRules(getMappingRuleList(binding.getId()));
        return runtimeBO;
    }

    private void fillCountField(EventAccessBindingDO binding) {
        if (binding == null || binding.getId() == null) {
            return;
        }
        binding.setRawFieldCount((int) eventAccessRawFieldDefMapper.selectCountByBindingId(binding.getId()));
        binding.setMappingRuleCount((int) eventAccessMappingRuleMapper.selectCountByBindingId(binding.getId()));
    }

    private EventAccessBindingDO validateAccessMappingExists(Long id) {
        if (id == null) {
            throw exception(ACCESS_MAPPING_NOT_EXISTS);
        }
        EventAccessBindingDO binding = eventAccessBindingMapper.selectById(id);
        if (binding == null) {
            throw exception(ACCESS_MAPPING_NOT_EXISTS);
        }
        return binding;
    }

    private EventSchemaDO validateEventExists(String eventCode) {
        EventSchemaDO eventSchema = eventSchemaMapper.selectByEventCode(eventCode);
        if (eventSchema == null) {
            throw exception(ACCESS_MAPPING_EVENT_NOT_EXISTS, eventCode);
        }
        return eventSchema;
    }

    private AccessSourceDO validateSourceExists(String sourceCode) {
        AccessSourceDO accessSource = accessSourceMapper.selectBySourceCode(sourceCode);
        if (accessSource == null) {
            throw exception(ACCESS_MAPPING_SOURCE_NOT_EXISTS, sourceCode);
        }
        return accessSource;
    }

    private void validateSceneCompatibility(EventSchemaDO eventSchema, AccessSourceDO accessSource) {
        List<String> allowedSceneCodes = accessSource.getAllowedSceneCodes();
        if (allowedSceneCodes == null || !allowedSceneCodes.contains(eventSchema.getSceneCode())) {
            throw exception(ACCESS_MAPPING_SCENE_MISMATCH, accessSource.getSourceCode(), eventSchema.getSceneCode());
        }
    }

    private void validateBindingUnique(Long id, String eventCode, String sourceCode) {
        EventAccessBindingDO binding = eventAccessBindingMapper.selectByEventCodeAndSourceCode(eventCode, sourceCode);
        if (binding == null) {
            return;
        }
        if (id == null || !ObjectUtil.equal(binding.getId(), id)) {
            throw exception(ACCESS_MAPPING_BINDING_DUPLICATE, eventCode, sourceCode);
        }
    }

    private void validateRouteUnique(Long id, String sourceCode, String eventType) {
        for (EventAccessBindingDO binding : eventAccessBindingMapper.selectAccessMappingListBySourceCodeAndEventType(sourceCode, eventType)) {
            if (id == null || !ObjectUtil.equal(binding.getId(), id)) {
                throw exception(ACCESS_MAPPING_ROUTE_CONFLICT, sourceCode, eventType);
            }
        }
    }

    private void throwIfInvalid(DraftValidationResult validationResult) {
        if (validationResult.duplicateRawFieldPath() != null) {
            throw exception(ACCESS_MAPPING_RAW_FIELD_PATH_DUPLICATE, validationResult.duplicateRawFieldPath());
        }
        if (validationResult.invalidRawFieldPath() != null) {
            throw exception(ACCESS_MAPPING_RAW_FIELD_PATH_INVALID, validationResult.invalidRawFieldPath());
        }
        if (validationResult.duplicateTargetFieldName() != null) {
            throw exception(ACCESS_MAPPING_TARGET_FIELD_DUPLICATE, validationResult.duplicateTargetFieldName());
        }
        if (validationResult.missingTargetFieldName() != null) {
            throw exception(ACCESS_MAPPING_TARGET_FIELD_NOT_EXISTS, validationResult.missingTargetFieldName());
        }
        if (validationResult.invalidPublicFieldName() != null) {
            throw exception(ACCESS_MAPPING_PUBLIC_FIELD_INVALID,
                    validationResult.invalidPublicFieldName(), validationResult.invalidPublicFieldExpectedValue());
        }
        if (!validationResult.messages().isEmpty()) {
            throw exception(ACCESS_MAPPING_RULE_CONFIG_INVALID, validationResult.messages().get(0));
        }
    }

    private DraftValidationResult validateDraft(AccessMappingSaveReqVO reqVO, EventSchemaDO eventSchema, AccessSourceDO accessSource) {
        List<EventAccessRawFieldDefDO> normalizedRawFields = normalizeRawFields(reqVO.getRawFields());
        List<EventAccessMappingRuleDO> normalizedMappingRules = normalizeMappingRules(reqVO.getMappingRules());
        List<EventFieldDefDO> standardFields = getStandardFieldList(eventSchema.getEventCode());

        List<String> messages = new ArrayList<>();
        LinkedHashMap<String, EventFieldDefDO> standardFieldMap = new LinkedHashMap<>();
        for (EventFieldDefDO field : standardFields) {
            standardFieldMap.put(field.getFieldName(), field);
        }

        String duplicateRawFieldPath = null;
        String invalidRawFieldPath = null;
        Set<String> rawFieldPathSet = new LinkedHashSet<>();
        for (EventAccessRawFieldDefDO rawField : normalizedRawFields) {
            if (!rawFieldPathSet.add(rawField.getFieldPath()) && duplicateRawFieldPath == null) {
                duplicateRawFieldPath = rawField.getFieldPath();
            }
            if (!isValidFieldPath(rawField.getFieldPath()) && invalidRawFieldPath == null) {
                invalidRawFieldPath = rawField.getFieldPath();
            }
        }

        String duplicateTargetFieldName = null;
        String missingTargetFieldName = null;
        LinkedHashMap<String, EventAccessMappingRuleDO> ruleMap = new LinkedHashMap<>();
        for (EventAccessMappingRuleDO rule : normalizedMappingRules) {
            EventAccessMappingRuleDO previous = ruleMap.putIfAbsent(rule.getTargetFieldName(), rule);
            if (previous != null && duplicateTargetFieldName == null) {
                duplicateTargetFieldName = rule.getTargetFieldName();
            }
            EventFieldDefDO targetField = standardFieldMap.get(rule.getTargetFieldName());
            if (targetField == null && missingTargetFieldName == null) {
                missingTargetFieldName = rule.getTargetFieldName();
            }
            validateMappingRule(rule, targetField, messages);
        }

        LinkedHashMap<String, Object> standardEventJson = new LinkedHashMap<>();
        LinkedHashMap<String, String> fieldSourceMap = new LinkedHashMap<>();
        EvaluationContextBundle contextBundle = new EvaluationContextBundle(reqVO.getRawSampleJson(),
                reqVO.getSampleHeadersJson(), accessSource.getSourceCode(), eventSchema.getSceneCode(),
                eventSchema.getEventCode(), eventSchema.getEventType());

        for (EventFieldDefDO standardField : standardFields) {
            EventAccessMappingRuleDO rule = ruleMap.get(standardField.getFieldName());
            Object value = MISSING_VALUE;
            if (rule != null) {
                value = resolveMappingRuleValue(standardField, rule, contextBundle, messages);
                if (value != MISSING_VALUE) {
                    standardEventJson.put(standardField.getFieldName(), value);
                    fieldSourceMap.put(standardField.getFieldName(), FIELD_SOURCE_MAPPING);
                    continue;
                }
            }

            if (StrUtil.isNotBlank(standardField.getDefaultValue())) {
                value = convertEventFieldValue(standardField, standardField.getDefaultValue(),
                        null, "事件模型字段默认值", messages);
                if (value != MISSING_VALUE) {
                    standardEventJson.put(standardField.getFieldName(), value);
                    fieldSourceMap.put(standardField.getFieldName(), FIELD_SOURCE_EVENT_DEFAULT);
                    continue;
                }
            }

            value = resolveSystemFieldValue(standardField, eventSchema);
            if (value != MISSING_VALUE) {
                standardEventJson.put(standardField.getFieldName(), value);
                fieldSourceMap.put(standardField.getFieldName(), FIELD_SOURCE_SYSTEM_FILL);
                continue;
            }

            if (Objects.equals(standardField.getRequiredFlag(), 1)) {
                messages.add("必填字段【" + standardField.getFieldName() + "】缺少可用值");
            }
        }

        String invalidPublicFieldName = null;
        String invalidPublicFieldExpectedValue = null;
        for (String fieldName : FIXED_PUBLIC_FIELDS) {
            if (!ruleMap.containsKey(fieldName)) {
                continue;
            }
            String expectedValue = switch (fieldName) {
                case "sceneCode" -> eventSchema.getSceneCode();
                case "eventType" -> eventSchema.getEventType();
                default -> null;
            };
            Object actualValue = standardEventJson.get(fieldName);
            if (!ObjectUtil.equal(expectedValue, actualValue == null ? null : String.valueOf(actualValue))) {
                invalidPublicFieldName = fieldName;
                invalidPublicFieldExpectedValue = expectedValue;
                messages.add("公共字段【" + fieldName + "】映射结果必须等于固定值【" + expectedValue + "】");
                break;
            }
        }

        return new DraftValidationResult(normalizedRawFields, normalizedMappingRules, standardEventJson, fieldSourceMap,
                messages, duplicateRawFieldPath, invalidRawFieldPath, duplicateTargetFieldName, missingTargetFieldName,
                invalidPublicFieldName, invalidPublicFieldExpectedValue);
    }

    private List<EventFieldDefDO> getStandardFieldList(String eventCode) {
        List<EventFieldDefDO> fields = eventFieldDefMapper.selectListByEventCode(eventCode);
        if (fields.isEmpty()) {
            return Collections.emptyList();
        }
        List<EventFieldDefDO> filtered = new ArrayList<>(fields.size());
        for (EventFieldDefDO field : fields) {
            if (!"ext".equalsIgnoreCase(StrUtil.trim(field.getFieldName()))) {
                filtered.add(field);
            }
        }
        return filtered;
    }

    private void validateMappingRule(EventAccessMappingRuleDO rule, EventFieldDefDO targetField, List<String> messages) {
        if (targetField == null) {
            return;
        }
        String mappingType = rule.getMappingType();
        if (AccessMappingTypeEnum.SOURCE_FIELD.getType().equals(mappingType)) {
            if (StrUtil.isBlank(rule.getSourceFieldPath())) {
                messages.add("目标字段【" + rule.getTargetFieldName() + "】配置为源字段映射时，源字段路径不能为空");
            } else if (!isValidFieldPath(rule.getSourceFieldPath())) {
                messages.add("目标字段【" + rule.getTargetFieldName() + "】的源字段路径格式不正确");
            }
            return;
        }
        if (AccessMappingTypeEnum.CONSTANT.getType().equals(mappingType)) {
            if (StrUtil.isBlank(rule.getConstantValue())) {
                messages.add("目标字段【" + rule.getTargetFieldName() + "】配置为常量映射时，常量值不能为空");
            }
            return;
        }
        if (AccessMappingTypeEnum.SCRIPT.getType().equals(mappingType)) {
            if (StrUtil.isBlank(rule.getScriptEngine())) {
                messages.add("目标字段【" + rule.getTargetFieldName() + "】配置为脚本映射时，脚本引擎不能为空");
                return;
            }
            if (!AccessScriptEngineEnum.EXPRESSION.getType().equals(rule.getScriptEngine())) {
                throw exception(ACCESS_MAPPING_SCRIPT_ENGINE_UNSUPPORTED, rule.getScriptEngine());
            }
            if (StrUtil.isBlank(rule.getScriptContent())) {
                messages.add("目标字段【" + rule.getTargetFieldName() + "】配置为脚本映射时，脚本内容不能为空");
            }
            return;
        }
        messages.add("目标字段【" + rule.getTargetFieldName() + "】使用了不支持的映射类型【" + mappingType + "】");
    }

    private Object resolveMappingRuleValue(EventFieldDefDO targetField, EventAccessMappingRuleDO rule,
                                           EvaluationContextBundle contextBundle, List<String> messages) {
        String source = "映射规则【" + targetField.getFieldName() + "】";
        Object rawValue = switch (rule.getMappingType()) {
            case "SOURCE_FIELD" -> extractValueByPath(contextBundle.rawPayload(), rule.getSourceFieldPath());
            case "CONSTANT" -> StrUtil.isBlank(rule.getConstantValue()) ? MISSING_VALUE : rule.getConstantValue();
            case "SCRIPT" -> evaluateExpression(rule.getScriptContent(), contextBundle, targetField.getFieldName(), messages);
            default -> MISSING_VALUE;
        };
        if (rawValue == MISSING_VALUE || rawValue == null) {
            return MISSING_VALUE;
        }
        Object enumMappedValue = applyEnumMapping(rule.getEnumMappingJson(), rawValue);
        return convertEventFieldValue(targetField, enumMappedValue, rule.getTimePattern(), source, messages);
    }

    private Object applyEnumMapping(Map<String, String> enumMappingJson, Object rawValue) {
        if (enumMappingJson == null || enumMappingJson.isEmpty() || rawValue == null) {
            return rawValue;
        }
        String key = String.valueOf(rawValue);
        return enumMappingJson.getOrDefault(key, key);
    }

    private Object evaluateExpression(String scriptContent, EvaluationContextBundle contextBundle,
                                      String targetFieldName, List<String> messages) {
        if (StrUtil.isBlank(scriptContent)) {
            return MISSING_VALUE;
        }
        try {
            Expression expression = EXPRESSION_PARSER.parseExpression(scriptContent);
            StandardEvaluationContext context = new StandardEvaluationContext();
            Map<String, Object> root = new LinkedHashMap<>();
            root.put("rawPayload", contextBundle.rawPayload());
            root.put("headers", contextBundle.headers());
            root.put("sourceCode", contextBundle.sourceCode());
            root.put("sceneCode", contextBundle.sceneCode());
            root.put("eventCode", contextBundle.eventCode());
            root.put("eventType", contextBundle.eventType());
            context.setRootObject(root);
            context.setVariables(root);
            return expression.getValue(context);
        } catch (Exception ex) {
            messages.add("目标字段【" + targetFieldName + "】的表达式执行失败：" + ex.getMessage());
            return MISSING_VALUE;
        }
    }

    private Object resolveSystemFieldValue(EventFieldDefDO field, EventSchemaDO eventSchema) {
        return switch (field.getFieldName()) {
            case "sceneCode" -> eventSchema.getSceneCode();
            case "eventType" -> eventSchema.getEventType();
            case "eventId" -> "AUTO_" + eventSchema.getEventType() + "_EVENT_ID";
            case "traceId" -> "AUTO_" + eventSchema.getEventType() + "_TRACE_ID";
            case "eventTime" -> LocalDateTime.now().withNano(0).toString();
            default -> MISSING_VALUE;
        };
    }

    private Object convertEventFieldValue(EventFieldDefDO field, Object rawValue, String timePattern,
                                          String source, List<String> messages) {
        if (rawValue == null) {
            return MISSING_VALUE;
        }
        return switch (field.getFieldType()) {
            case "STRING" -> convertString(rawValue);
            case "INTEGER" -> convertInteger(field.getFieldName(), rawValue, source, messages);
            case "LONG" -> convertLong(field.getFieldName(), rawValue, source, messages);
            case "DECIMAL" -> convertDecimal(field.getFieldName(), rawValue, source, messages);
            case "BOOLEAN" -> convertBoolean(field.getFieldName(), rawValue, source, messages);
            case "DATETIME" -> convertDateTime(field.getFieldName(), rawValue, timePattern, source, messages);
            case "JSON" -> convertJson(field.getFieldName(), rawValue, source, messages);
            default -> {
                messages.add(source + "中的字段【" + field.getFieldName() + "】使用了不支持的字段类型【" + field.getFieldType() + "】");
                yield MISSING_VALUE;
            }
        };
    }

    private Object convertString(Object rawValue) {
        if (rawValue instanceof Map || rawValue instanceof List) {
            return JsonUtils.toJsonString(rawValue);
        }
        return String.valueOf(rawValue);
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

    private Object convertDateTime(String fieldName, Object rawValue, String timePattern, String source,
                                   List<String> messages) {
        if (rawValue instanceof LocalDateTime localDateTime) {
            return localDateTime.withNano(0).toString();
        }
        if (rawValue instanceof Number number) {
            return LocalDateTime.ofInstant(Instant.ofEpochMilli(number.longValue()), ZoneId.systemDefault())
                    .withNano(0)
                    .toString();
        }
        if (rawValue instanceof String text) {
            String trimmed = text.trim();
            if (trimmed.isEmpty()) {
                return MISSING_VALUE;
            }
            if (StrUtil.isBlank(timePattern)) {
                return trimmed;
            }
            try {
                return LocalDateTime.parse(trimmed, DateTimeFormatter.ofPattern(timePattern))
                        .withNano(0)
                        .toString();
            } catch (DateTimeParseException ex) {
                messages.add(source + "中的字段【" + fieldName + "】不符合时间格式【" + timePattern + "】");
                return MISSING_VALUE;
            }
        }
        messages.add(source + "中的字段【" + fieldName + "】必须是日期时间");
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

    private Object extractValueByPath(Object root, String path) {
        if (root == null || StrUtil.isBlank(path) || !isValidFieldPath(path)) {
            return MISSING_VALUE;
        }
        Object current = root;
        for (String segment : path.split("\\.")) {
            if (current == null) {
                return MISSING_VALUE;
            }
            String fieldName = segment;
            Integer index = null;
            int bracketIndex = segment.indexOf('[');
            if (bracketIndex >= 0) {
                fieldName = segment.substring(0, bracketIndex);
                int endIndex = segment.indexOf(']', bracketIndex);
                if (endIndex < 0) {
                    return MISSING_VALUE;
                }
                index = Integer.parseInt(segment.substring(bracketIndex + 1, endIndex));
            }
            if (!(current instanceof Map<?, ?> currentMap)) {
                return MISSING_VALUE;
            }
            current = currentMap.get(fieldName);
            if (index != null) {
                if (!(current instanceof List<?> currentList) || index < 0 || index >= currentList.size()) {
                    return MISSING_VALUE;
                }
                current = currentList.get(index);
            }
        }
        return current == null ? MISSING_VALUE : current;
    }

    private boolean isValidFieldPath(String fieldPath) {
        return StrUtil.isNotBlank(fieldPath) && FIELD_PATH_PATTERN.matcher(fieldPath).matches();
    }

    private List<EventAccessRawFieldDefDO> normalizeRawFields(List<AccessRawFieldItemVO> rawFields) {
        List<EventAccessRawFieldDefDO> fieldDOs = AccessMappingConvert.INSTANCE.convertRawFieldDOList(rawFields);
        List<FieldOrderHolder> holders = new ArrayList<>(fieldDOs.size());
        for (int i = 0; i < fieldDOs.size(); i++) {
            EventAccessRawFieldDefDO field = fieldDOs.get(i);
            Integer sortNo = field.getSortNo() == null ? i + 1 : field.getSortNo();
            field.setSortNo(sortNo);
            holders.add(new FieldOrderHolder(field, i));
        }
        holders.sort(Comparator.comparingInt((FieldOrderHolder holder) -> holder.field().getSortNo())
                .thenComparingInt(FieldOrderHolder::index));

        List<EventAccessRawFieldDefDO> normalized = new ArrayList<>(holders.size());
        for (int i = 0; i < holders.size(); i++) {
            EventAccessRawFieldDefDO field = holders.get(i).field();
            field.setSortNo(i + 1);
            normalized.add(field);
        }
        return normalized;
    }

    private List<EventAccessMappingRuleDO> normalizeMappingRules(List<AccessMappingRuleItemVO> mappingRules) {
        List<EventAccessMappingRuleDO> ruleDOs = AccessMappingConvert.INSTANCE.convertMappingRuleDOList(mappingRules);
        List<EventAccessMappingRuleDO> normalized = new ArrayList<>(ruleDOs.size());
        for (EventAccessMappingRuleDO rule : ruleDOs) {
            normalized.add(rule);
        }
        return normalized;
    }

    private List<EventAccessRawFieldDefDO> buildRawFieldDOList(Long bindingId, List<EventAccessRawFieldDefDO> normalizedRawFields) {
        List<EventAccessRawFieldDefDO> rawFields = new ArrayList<>(normalizedRawFields.size());
        for (EventAccessRawFieldDefDO rawField : normalizedRawFields) {
            EventAccessRawFieldDefDO clone = new EventAccessRawFieldDefDO();
            clone.setBindingId(bindingId);
            clone.setFieldName(rawField.getFieldName());
            clone.setFieldLabel(rawField.getFieldLabel());
            clone.setFieldPath(rawField.getFieldPath());
            clone.setFieldType(rawField.getFieldType());
            clone.setRequiredFlag(rawField.getRequiredFlag());
            clone.setSampleValue(rawField.getSampleValue());
            clone.setDescription(rawField.getDescription());
            clone.setSortNo(rawField.getSortNo());
            rawFields.add(clone);
        }
        return rawFields;
    }

    private List<EventAccessMappingRuleDO> buildMappingRuleDOList(Long bindingId, List<EventAccessMappingRuleDO> normalizedMappingRules) {
        List<EventAccessMappingRuleDO> rules = new ArrayList<>(normalizedMappingRules.size());
        for (EventAccessMappingRuleDO rule : normalizedMappingRules) {
            EventAccessMappingRuleDO clone = new EventAccessMappingRuleDO();
            clone.setBindingId(bindingId);
            clone.setTargetFieldName(rule.getTargetFieldName());
            clone.setMappingType(rule.getMappingType());
            clone.setSourceFieldPath(rule.getSourceFieldPath());
            clone.setConstantValue(rule.getConstantValue());
            clone.setScriptEngine(rule.getScriptEngine());
            clone.setScriptContent(rule.getScriptContent());
            clone.setTimePattern(rule.getTimePattern());
            clone.setEnumMappingJson(rule.getEnumMappingJson());
            clone.setDescription(rule.getDescription());
            rules.add(clone);
        }
        return rules;
    }

    private void insertRawFieldList(Collection<EventAccessRawFieldDefDO> rawFields) {
        if (rawFields == null || rawFields.isEmpty()) {
            return;
        }
        eventAccessRawFieldDefMapper.insertBatch(rawFields);
    }

    private void insertMappingRuleList(Collection<EventAccessMappingRuleDO> mappingRules) {
        if (mappingRules == null || mappingRules.isEmpty()) {
            return;
        }
        eventAccessMappingRuleMapper.insertBatch(mappingRules);
    }

    private record EvaluationContextBundle(
            Map<String, Object> rawPayload,
            Map<String, Object> headers,
            String sourceCode,
            String sceneCode,
            String eventCode,
            String eventType
    ) {
        private EvaluationContextBundle {
            rawPayload = rawPayload == null ? Collections.emptyMap() : rawPayload;
            headers = headers == null ? Collections.emptyMap() : headers;
        }
    }

    private record DraftValidationResult(
            List<EventAccessRawFieldDefDO> normalizedRawFields,
            List<EventAccessMappingRuleDO> normalizedMappingRules,
            LinkedHashMap<String, Object> standardEventJson,
            LinkedHashMap<String, String> fieldSourceMap,
            List<String> messages,
            String duplicateRawFieldPath,
            String invalidRawFieldPath,
            String duplicateTargetFieldName,
            String missingTargetFieldName,
            String invalidPublicFieldName,
            String invalidPublicFieldExpectedValue
    ) {
    }

    private record FieldOrderHolder(EventAccessRawFieldDefDO field, int index) {
    }

}
