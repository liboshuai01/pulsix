package cn.liboshuai.pulsix.module.risk.service.preview;

import cn.hutool.core.util.StrUtil;
import cn.liboshuai.pulsix.framework.common.util.json.JsonUtils;
import cn.liboshuai.pulsix.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.liboshuai.pulsix.module.risk.dal.dataobject.eventfield.EventFieldDO;
import cn.liboshuai.pulsix.module.risk.dal.dataobject.ingestmapping.IngestMappingDO;
import cn.liboshuai.pulsix.module.risk.dal.mysql.eventfield.EventFieldMapper;
import cn.liboshuai.pulsix.module.risk.dal.mysql.ingestmapping.IngestMappingMapper;
import cn.liboshuai.pulsix.module.risk.enums.eventfield.RiskEventFieldTypeEnum;
import cn.liboshuai.pulsix.module.risk.enums.ingestmapping.RiskIngestMappingTransformTypeEnum;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class StandardEventPreviewService {

    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    @Resource
    private EventFieldMapper eventFieldMapper;

    @Resource
    private IngestMappingMapper ingestMappingMapper;

    public StandardEventPreviewResult preview(String sceneCode, String eventCode, String sourceCode, Map<String, Object> rawEventJson) {
        List<EventFieldDO> eventFields = eventFieldMapper.selectList(new LambdaQueryWrapperX<EventFieldDO>()
                .eq(EventFieldDO::getSceneCode, sceneCode)
                .eq(EventFieldDO::getEventCode, eventCode)
                .orderByAsc(EventFieldDO::getSortNo)
                .orderByAsc(EventFieldDO::getId));

        Map<String, Object> inputJson = rawEventJson == null ? new LinkedHashMap<>() : new LinkedHashMap<>(rawEventJson);
        Map<String, IngestMappingDO> mappingsByTargetField = StrUtil.isBlank(sourceCode)
                ? Map.of()
                : ingestMappingMapper.selectEnabledList(sourceCode, sceneCode, eventCode).stream()
                .collect(Collectors.toMap(IngestMappingDO::getTargetFieldCode, Function.identity(), (left, right) -> left, LinkedHashMap::new));

        Map<String, Object> standardEventJson = new LinkedHashMap<>();
        List<String> missingRequiredFields = new ArrayList<>();
        List<String> defaultedFields = new ArrayList<>();
        List<String> mappedFields = new ArrayList<>();

        for (EventFieldDO eventField : eventFields) {
            IngestMappingDO ingestMapping = mappingsByTargetField.get(eventField.getFieldCode());
            FieldResolveResult resolveResult = ingestMapping != null
                    ? resolveMappedValue(inputJson, ingestMapping, eventField)
                    : resolveByFieldDefinition(inputJson, eventField);

            Object value = resolveResult.getValue();
            boolean defaulted = resolveResult.isDefaulted();
            if (value == null && StrUtil.isNotBlank(eventField.getDefaultValue())) {
                value = convertDefaultValue(eventField.getFieldType(), eventField.getDefaultValue());
                defaulted = true;
            }
            if (value == null) {
                if (eventField.getRequiredFlag() != null && eventField.getRequiredFlag() == 1) {
                    missingRequiredFields.add(eventField.getFieldCode());
                }
                continue;
            }
            if (resolveResult.isMappingApplied() && resolveResult.getValue() != null) {
                mappedFields.add(eventField.getFieldCode());
            }
            if (defaulted) {
                defaultedFields.add(eventField.getFieldCode());
            }
            putFieldValue(standardEventJson, eventField, value);
        }

        StandardEventPreviewResult result = new StandardEventPreviewResult();
        result.setRawEventJson(inputJson);
        result.setStandardEventJson(standardEventJson);
        result.setMissingRequiredFields(missingRequiredFields);
        result.setDefaultedFields(defaultedFields);
        result.setMappedFields(mappedFields);
        return result;
    }

    private FieldResolveResult resolveByFieldDefinition(Map<String, Object> inputJson, EventFieldDO eventField) {
        ValueLookupResult lookupResult = findFieldValue(inputJson, eventField);
        return new FieldResolveResult(lookupResult.isFound(), lookupResult.getValue(), false, false);
    }

    private FieldResolveResult resolveMappedValue(Map<String, Object> inputJson, IngestMappingDO ingestMapping, EventFieldDO eventField) {
        Object value = null;
        boolean found = false;
        boolean defaulted = false;

        if (RiskIngestMappingTransformTypeEnum.CONST.getType().equals(ingestMapping.getTransformType())) {
            found = ingestMapping.getTransformExpr() != null;
            value = applyCleanRules(ingestMapping.getTransformExpr(), ingestMapping.getCleanRuleJson());
            value = convertValueByFieldType(eventField.getFieldType(), value);
        } else {
            ValueLookupResult lookupResult = lookupByPath(inputJson, normalizePath(ingestMapping.getSourceFieldPath()));
            found = lookupResult.isFound();
            if (found) {
                value = applyCleanRules(lookupResult.getValue(), ingestMapping.getCleanRuleJson());
                value = applyTransformValue(value, ingestMapping.getTransformType(), ingestMapping.getTransformExpr());
                value = convertValueByFieldType(eventField.getFieldType(), value);
            }
        }

        if (value == null && StrUtil.isNotBlank(ingestMapping.getDefaultValue())) {
            value = convertDefaultValue(eventField.getFieldType(), ingestMapping.getDefaultValue());
            found = true;
            defaulted = true;
        }
        return new FieldResolveResult(found, value, true, defaulted);
    }

    private ValueLookupResult findFieldValue(Map<String, Object> inputJson, EventFieldDO eventField) {
        ValueLookupResult byCode = lookupByPath(inputJson, eventField.getFieldCode());
        if (byCode.isFound()) {
            return byCode;
        }
        if (StrUtil.isBlank(eventField.getFieldPath())) {
            return ValueLookupResult.notFound();
        }
        return lookupByPath(inputJson, normalizePath(eventField.getFieldPath()));
    }

    @SuppressWarnings("unchecked")
    private ValueLookupResult lookupByPath(Map<String, Object> inputJson, String path) {
        if (inputJson == null || StrUtil.isBlank(path)) {
            return ValueLookupResult.notFound();
        }
        String[] segments = StrUtil.splitToArray(path, '.');
        Object current = inputJson;
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
        String[] segments = StrUtil.splitToArray(normalizePath(eventField.getFieldPath(), eventField.getFieldCode()), '.');
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

    private Object applyTransformValue(Object value, String transformType, String transformExpr) {
        if (value == null || StrUtil.isBlank(transformType)) {
            return value;
        }
        try {
            if (RiskIngestMappingTransformTypeEnum.DIRECT.getType().equals(transformType)) {
                return value;
            }
            if (RiskIngestMappingTransformTypeEnum.DIVIDE_100.getType().equals(transformType)) {
                BigDecimal decimalValue = parseToBigDecimal(value);
                return decimalValue == null ? null : decimalValue.divide(BigDecimal.valueOf(100));
            }
            if (RiskIngestMappingTransformTypeEnum.TIME_MILLIS_TO_DATETIME.getType().equals(transformType)) {
                BigDecimal decimalValue = parseToBigDecimal(value);
                if (decimalValue == null) {
                    return null;
                }
                LocalDateTime localDateTime = LocalDateTime.ofInstant(
                        Instant.ofEpochMilli(decimalValue.longValue()), ZoneId.systemDefault());
                return DATETIME_FORMATTER.format(localDateTime);
            }
            if (RiskIngestMappingTransformTypeEnum.ENUM_MAP.getType().equals(transformType)) {
                if (!JsonUtils.isJsonObject(transformExpr)) {
                    return value;
                }
                Map<String, Object> enumMap = JsonUtils.parseObject(transformExpr, Map.class);
                return enumMap.getOrDefault(String.valueOf(value), value);
            }
        } catch (Exception ignored) {
            return value;
        }
        return value;
    }

    private Object applyCleanRules(Object value, Map<String, Object> cleanRuleJson) {
        if (value == null || cleanRuleJson == null || cleanRuleJson.isEmpty()) {
            return value;
        }
        if (!(value instanceof String stringValue)) {
            return value;
        }
        String result = stringValue;
        if (isRuleEnabled(cleanRuleJson, "trim")) {
            result = StrUtil.trim(result);
        }
        if (isRuleEnabled(cleanRuleJson, "blankToNull") && StrUtil.isBlank(result)) {
            return null;
        }
        if (isRuleEnabled(cleanRuleJson, "upperCase")) {
            result = result.toUpperCase();
        } else if (isRuleEnabled(cleanRuleJson, "lowerCase")) {
            result = result.toLowerCase();
        }
        return result;
    }

    private boolean isRuleEnabled(Map<String, Object> cleanRuleJson, String key) {
        Object value = cleanRuleJson.get(key);
        if (value instanceof Boolean boolValue) {
            return boolValue;
        }
        if (value instanceof Number numberValue) {
            return numberValue.intValue() == 1;
        }
        return value != null && ("true".equalsIgnoreCase(String.valueOf(value)) || "1".equals(String.valueOf(value)));
    }

    private Object convertDefaultValue(String fieldType, String defaultValue) {
        if (StrUtil.isBlank(defaultValue)) {
            return null;
        }
        return convertValueByFieldType(fieldType, defaultValue);
    }

    private Object convertValueByFieldType(String fieldType, Object value) {
        if (value == null) {
            return null;
        }
        try {
            if (RiskEventFieldTypeEnum.STRING.getType().equals(fieldType)) {
                return String.valueOf(value);
            }
            if (RiskEventFieldTypeEnum.LONG.getType().equals(fieldType)) {
                BigDecimal decimalValue = parseToBigDecimal(value);
                return decimalValue == null ? null : decimalValue.longValue();
            }
            if (RiskEventFieldTypeEnum.DECIMAL.getType().equals(fieldType)) {
                return parseToBigDecimal(value);
            }
            if (RiskEventFieldTypeEnum.BOOLEAN.getType().equals(fieldType)) {
                if (value instanceof Boolean) {
                    return value;
                }
                String strValue = StrUtil.trim(String.valueOf(value));
                if (StrUtil.isBlank(strValue)) {
                    return null;
                }
                if ("1".equals(strValue) || "true".equalsIgnoreCase(strValue) || "yes".equalsIgnoreCase(strValue)
                        || "y".equalsIgnoreCase(strValue)) {
                    return Boolean.TRUE;
                }
                if ("0".equals(strValue) || "false".equalsIgnoreCase(strValue) || "no".equalsIgnoreCase(strValue)
                        || "n".equalsIgnoreCase(strValue)) {
                    return Boolean.FALSE;
                }
                return Boolean.valueOf(strValue);
            }
            if (RiskEventFieldTypeEnum.JSON.getType().equals(fieldType)) {
                if (value instanceof String stringValue && JsonUtils.isJson(stringValue)) {
                    return JsonUtils.parseObject(stringValue, Object.class);
                }
                return value;
            }
            if (RiskEventFieldTypeEnum.DATETIME.getType().equals(fieldType)) {
                return String.valueOf(value);
            }
        } catch (Exception ignored) {
            return value;
        }
        return value;
    }

    private BigDecimal parseToBigDecimal(Object value) {
        if (value == null) {
            return null;
        }
        String strValue = StrUtil.trim(String.valueOf(value));
        if (StrUtil.isBlank(strValue)) {
            return null;
        }
        return new BigDecimal(strValue);
    }

    private String normalizePath(String path) {
        return normalizePath(path, null);
    }

    private String normalizePath(String path, String fallback) {
        if (StrUtil.isBlank(path)) {
            return fallback;
        }
        return StrUtil.removePrefix(path, "$.");
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

        public boolean isFound() {
            return found;
        }

        public Object getValue() {
            return value;
        }

    }

    private static final class FieldResolveResult {

        private final boolean found;
        private final Object value;
        private final boolean mappingApplied;
        private final boolean defaulted;

        private FieldResolveResult(boolean found, Object value, boolean mappingApplied, boolean defaulted) {
            this.found = found;
            this.value = value;
            this.mappingApplied = mappingApplied;
            this.defaulted = defaulted;
        }

        public boolean isFound() {
            return found;
        }

        public Object getValue() {
            return value;
        }

        public boolean isMappingApplied() {
            return mappingApplied;
        }

        public boolean isDefaulted() {
            return defaulted;
        }

    }

}
