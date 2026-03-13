package cn.liboshuai.pulsix.framework.common.biz.risk.access.normalize;

import cn.hutool.core.util.StrUtil;
import cn.liboshuai.pulsix.framework.common.util.json.JsonUtils;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public final class StandardEventNormalizer {

    private static final String FIELD_TYPE_STRING = "STRING";
    private static final String FIELD_TYPE_LONG = "LONG";
    private static final String FIELD_TYPE_DECIMAL = "DECIMAL";
    private static final String FIELD_TYPE_BOOLEAN = "BOOLEAN";
    private static final String FIELD_TYPE_DATETIME = "DATETIME";
    private static final String FIELD_TYPE_JSON = "JSON";

    private static final String TRANSFORM_DIRECT = "DIRECT";
    private static final String TRANSFORM_CONST = "CONST";
    private static final String TRANSFORM_TIME_MILLIS_TO_DATETIME = "TIME_MILLIS_TO_DATETIME";
    private static final String TRANSFORM_DIVIDE_100 = "DIVIDE_100";
    private static final String TRANSFORM_ENUM_MAP = "ENUM_MAP";

    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    private StandardEventNormalizer() {
    }

    public static StandardEventNormalizeResult normalize(Map<String, Object> rawEventJson,
                                                         List<StandardEventFieldDefinition> fieldDefinitions,
                                                         List<StandardEventMappingDefinition> mappingDefinitions,
                                                         ZoneId zoneId) {
        Map<String, Object> inputJson = rawEventJson == null ? new LinkedHashMap<>() : new LinkedHashMap<>(rawEventJson);
        List<StandardEventFieldDefinition> safeFieldDefinitions = fieldDefinitions == null ? Collections.emptyList() : fieldDefinitions;
        List<StandardEventMappingDefinition> safeMappingDefinitions = mappingDefinitions == null ? Collections.emptyList() : mappingDefinitions;
        ZoneId effectiveZoneId = zoneId == null ? ZoneId.systemDefault() : zoneId;

        Map<String, StandardEventMappingDefinition> mappingsByTargetField = safeMappingDefinitions.stream()
                .filter(item -> item != null && StrUtil.isNotBlank(item.getTargetFieldCode()))
                .collect(Collectors.toMap(StandardEventMappingDefinition::getTargetFieldCode,
                        Function.identity(), (left, right) -> left, LinkedHashMap::new));

        Map<String, Object> standardEventJson = new LinkedHashMap<>();
        List<String> missingRequiredFields = new ArrayList<>();
        List<String> defaultedFields = new ArrayList<>();
        List<String> mappedFields = new ArrayList<>();

        for (StandardEventFieldDefinition fieldDefinition : safeFieldDefinitions) {
            if (fieldDefinition == null || StrUtil.isBlank(fieldDefinition.getFieldCode())) {
                continue;
            }
            StandardEventMappingDefinition mappingDefinition = mappingsByTargetField.get(fieldDefinition.getFieldCode());
            FieldResolveResult resolveResult = mappingDefinition != null
                    ? resolveMappedValue(inputJson, mappingDefinition, fieldDefinition, effectiveZoneId)
                    : resolveByFieldDefinition(inputJson, fieldDefinition);

            Object value = resolveResult.getValue();
            boolean defaulted = resolveResult.isDefaulted();
            if (value == null && StrUtil.isNotBlank(fieldDefinition.getDefaultValue())) {
                value = convertDefaultValue(fieldDefinition.getFieldType(), fieldDefinition.getDefaultValue());
                defaulted = true;
            }
            if (value == null) {
                if (fieldDefinition.getRequiredFlag() != null && fieldDefinition.getRequiredFlag() == 1) {
                    missingRequiredFields.add(fieldDefinition.getFieldCode());
                }
                continue;
            }
            if (resolveResult.isMappingApplied() && resolveResult.getValue() != null) {
                mappedFields.add(fieldDefinition.getFieldCode());
            }
            if (defaulted) {
                defaultedFields.add(fieldDefinition.getFieldCode());
            }
            putFieldValue(standardEventJson, fieldDefinition, value);
        }

        StandardEventNormalizeResult result = new StandardEventNormalizeResult();
        result.setRawEventJson(inputJson);
        result.setStandardEventJson(standardEventJson);
        result.setMissingRequiredFields(missingRequiredFields);
        result.setDefaultedFields(defaultedFields);
        result.setMappedFields(mappedFields);
        return result;
    }

    private static FieldResolveResult resolveByFieldDefinition(Map<String, Object> inputJson,
                                                               StandardEventFieldDefinition fieldDefinition) {
        ValueLookupResult lookupResult = findFieldValue(inputJson, fieldDefinition);
        return new FieldResolveResult(lookupResult.getValue(), false, false);
    }

    private static FieldResolveResult resolveMappedValue(Map<String, Object> inputJson,
                                                         StandardEventMappingDefinition mappingDefinition,
                                                         StandardEventFieldDefinition fieldDefinition,
                                                         ZoneId zoneId) {
        Object value = null;

        if (TRANSFORM_CONST.equals(mappingDefinition.getTransformType())) {
            value = applyCleanRules(mappingDefinition.getTransformExpr(), mappingDefinition.getCleanRuleJson());
            value = convertValueByFieldType(fieldDefinition.getFieldType(), value);
        } else {
            ValueLookupResult lookupResult = lookupByPath(inputJson, normalizePath(mappingDefinition.getSourceFieldPath()));
            if (lookupResult.isFound()) {
                value = applyCleanRules(lookupResult.getValue(), mappingDefinition.getCleanRuleJson());
                value = applyTransformValue(value, mappingDefinition.getTransformType(), mappingDefinition.getTransformExpr(), zoneId);
                value = convertValueByFieldType(fieldDefinition.getFieldType(), value);
            }
        }

        boolean defaulted = false;
        if (value == null && StrUtil.isNotBlank(mappingDefinition.getDefaultValue())) {
            value = convertDefaultValue(fieldDefinition.getFieldType(), mappingDefinition.getDefaultValue());
            defaulted = true;
        }
        return new FieldResolveResult(value, true, defaulted);
    }

    private static ValueLookupResult findFieldValue(Map<String, Object> inputJson,
                                                    StandardEventFieldDefinition fieldDefinition) {
        ValueLookupResult byCode = lookupByPath(inputJson, fieldDefinition.getFieldCode());
        if (byCode.isFound()) {
            return byCode;
        }
        if (StrUtil.isBlank(fieldDefinition.getFieldPath())) {
            return ValueLookupResult.notFound();
        }
        return lookupByPath(inputJson, normalizePath(fieldDefinition.getFieldPath()));
    }

    @SuppressWarnings("unchecked")
    private static ValueLookupResult lookupByPath(Map<String, Object> inputJson, String path) {
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
    private static void putFieldValue(Map<String, Object> standardEventJson,
                                      StandardEventFieldDefinition fieldDefinition,
                                      Object value) {
        String[] segments = StrUtil.splitToArray(normalizePath(fieldDefinition.getFieldPath(), fieldDefinition.getFieldCode()), '.');
        if (segments == null || segments.length == 0) {
            standardEventJson.put(fieldDefinition.getFieldCode(), value);
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

    private static Object applyTransformValue(Object value, String transformType, String transformExpr, ZoneId zoneId) {
        if (value == null || StrUtil.isBlank(transformType)) {
            return value;
        }
        try {
            if (TRANSFORM_DIRECT.equals(transformType)) {
                return value;
            }
            if (TRANSFORM_DIVIDE_100.equals(transformType)) {
                BigDecimal decimalValue = parseToBigDecimal(value);
                return decimalValue == null ? null : decimalValue.divide(BigDecimal.valueOf(100));
            }
            if (TRANSFORM_TIME_MILLIS_TO_DATETIME.equals(transformType)) {
                BigDecimal decimalValue = parseToBigDecimal(value);
                if (decimalValue == null) {
                    return null;
                }
                LocalDateTime localDateTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(decimalValue.longValue()), zoneId);
                return DATETIME_FORMATTER.format(localDateTime);
            }
            if (TRANSFORM_ENUM_MAP.equals(transformType)) {
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

    private static Object applyCleanRules(Object value, Map<String, Object> cleanRuleJson) {
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

    private static boolean isRuleEnabled(Map<String, Object> cleanRuleJson, String key) {
        Object value = cleanRuleJson.get(key);
        if (value instanceof Boolean boolValue) {
            return boolValue;
        }
        if (value instanceof Number numberValue) {
            return numberValue.intValue() == 1;
        }
        return value != null && ("true".equalsIgnoreCase(String.valueOf(value)) || "1".equals(String.valueOf(value)));
    }

    private static Object convertDefaultValue(String fieldType, String defaultValue) {
        if (StrUtil.isBlank(defaultValue)) {
            return null;
        }
        return convertValueByFieldType(fieldType, defaultValue);
    }

    private static Object convertValueByFieldType(String fieldType, Object value) {
        if (value == null) {
            return null;
        }
        try {
            if (FIELD_TYPE_STRING.equals(fieldType)) {
                return String.valueOf(value);
            }
            if (FIELD_TYPE_LONG.equals(fieldType)) {
                BigDecimal decimalValue = parseToBigDecimal(value);
                return decimalValue == null ? null : decimalValue.longValue();
            }
            if (FIELD_TYPE_DECIMAL.equals(fieldType)) {
                return parseToBigDecimal(value);
            }
            if (FIELD_TYPE_BOOLEAN.equals(fieldType)) {
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
            if (FIELD_TYPE_JSON.equals(fieldType)) {
                if (value instanceof String stringValue && JsonUtils.isJson(stringValue)) {
                    return JsonUtils.parseObject(stringValue, Object.class);
                }
                return value;
            }
            if (FIELD_TYPE_DATETIME.equals(fieldType)) {
                return String.valueOf(value);
            }
        } catch (Exception ignored) {
            return value;
        }
        return value;
    }

    private static BigDecimal parseToBigDecimal(Object value) {
        if (value == null) {
            return null;
        }
        String strValue = StrUtil.trim(String.valueOf(value));
        if (StrUtil.isBlank(strValue)) {
            return null;
        }
        return new BigDecimal(strValue);
    }

    private static String normalizePath(String path) {
        return normalizePath(path, null);
    }

    private static String normalizePath(String path, String fallback) {
        if (StrUtil.isBlank(path)) {
            return fallback;
        }
        return StrUtil.removePrefix(path, "$." );
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

        private final Object value;
        private final boolean mappingApplied;
        private final boolean defaulted;

        private FieldResolveResult(Object value, boolean mappingApplied, boolean defaulted) {
            this.value = value;
            this.mappingApplied = mappingApplied;
            this.defaulted = defaulted;
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
