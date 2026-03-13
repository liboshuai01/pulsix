package cn.liboshuai.pulsix.framework.common.biz.risk.access.normalize;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class StandardEventNormalizerTest {

    @Test
    void shouldNormalizeTradeHttpDemoPayload() {
        Map<String, Object> rawEventJson = new LinkedHashMap<>();
        rawEventJson.put("event_id", "E_RAW_9103");
        rawEventJson.put("occur_time_ms", 1773287100000L);
        rawEventJson.put("req", Map.of("traceId", "T_RAW_9103"));
        rawEventJson.put("uid", " U9003 ");
        rawEventJson.put("dev_id", "D9003");
        rawEventJson.put("client_ip", "88.66.55.44");
        rawEventJson.put("pay_amt", 256800);
        rawEventJson.put("trade_result", "ok");

        StandardEventNormalizeResult result = StandardEventNormalizer.normalize(rawEventJson,
                List.of(
                        field("eventId", "STRING", "$.eventId", 1, null),
                        field("sceneCode", "STRING", "$.sceneCode", 1, "TRADE_RISK"),
                        field("eventType", "STRING", "$.eventType", 1, "trade"),
                        field("eventTime", "DATETIME", "$.eventTime", 1, null),
                        field("traceId", "STRING", "$.traceId", 0, null),
                        field("userId", "STRING", "$.userId", 1, null),
                        field("deviceId", "STRING", "$.deviceId", 1, null),
                        field("ip", "STRING", "$.ip", 1, null),
                        field("amount", "DECIMAL", "$.amount", 1, null),
                        field("result", "STRING", "$.result", 1, null)
                ),
                List.of(
                        mapping("$.event_id", "eventId", "DIRECT", null, null, Map.of()),
                        mapping("$.occur_time_ms", "eventTime", "TIME_MILLIS_TO_DATETIME", null, null, Map.of()),
                        mapping("$.req.traceId", "traceId", "DIRECT", null, null, Map.of()),
                        mapping("$.uid", "userId", "DIRECT", null, null, Map.of("trim", true)),
                        mapping("$.dev_id", "deviceId", "DIRECT", null, null, Map.of()),
                        mapping("$.client_ip", "ip", "DIRECT", null, null, Map.of()),
                        mapping("$.pay_amt", "amount", "DIVIDE_100", null, null, Map.of()),
                        mapping("$.trade_result", "result", "ENUM_MAP", "{\"ok\":\"SUCCESS\",\"fail\":\"FAIL\"}", null,
                                Map.of("lowerCase", true))
                ),
                ZoneId.of("Asia/Shanghai"));

        assertThat(result.getStandardEventJson()).containsExactlyEntriesOf(expectedTradeStandardEventJson());
        assertThat(result.getDefaultedFields()).containsExactly("sceneCode", "eventType");
        assertThat(result.getMappedFields()).containsExactly("eventId", "eventTime", "traceId", "userId",
                "deviceId", "ip", "amount", "result");
        assertThat(result.getMissingRequiredFields()).isEmpty();
    }

    @Test
    void shouldPreferMappingDefaultAndFallbackToFieldLookup() {
        Map<String, Object> rawEventJson = new LinkedHashMap<>();
        rawEventJson.put("req", Map.of("traceId", "T_1001"));

        StandardEventNormalizeResult result = StandardEventNormalizer.normalize(rawEventJson,
                List.of(
                        field("traceId", "STRING", "$.req.traceId", 1, null),
                        field("result", "STRING", "$.result", 1, "FIELD_DEFAULT"),
                        field("eventId", "STRING", "$.eventId", 1, null)
                ),
                List.of(mapping("$.trade_result", "result", "DIRECT", null, "MAPPING_DEFAULT", Map.of())),
                ZoneId.of("Asia/Shanghai"));

        Map<String, Object> standardEventJson = new LinkedHashMap<>();
        standardEventJson.put("req", Map.of("traceId", "T_1001"));
        standardEventJson.put("result", "MAPPING_DEFAULT");

        assertThat(result.getStandardEventJson()).containsExactlyEntriesOf(standardEventJson);
        assertThat(result.getDefaultedFields()).containsExactly("result");
        assertThat(result.getMappedFields()).containsExactly("result");
        assertThat(result.getMissingRequiredFields()).containsExactly("eventId");
    }

    private static Map<String, Object> expectedTradeStandardEventJson() {
        Map<String, Object> standardEventJson = new LinkedHashMap<>();
        standardEventJson.put("eventId", "E_RAW_9103");
        standardEventJson.put("sceneCode", "TRADE_RISK");
        standardEventJson.put("eventType", "trade");
        standardEventJson.put("eventTime", "2026-03-12T11:45:00");
        standardEventJson.put("traceId", "T_RAW_9103");
        standardEventJson.put("userId", "U9003");
        standardEventJson.put("deviceId", "D9003");
        standardEventJson.put("ip", "88.66.55.44");
        standardEventJson.put("amount", new BigDecimal("2568"));
        standardEventJson.put("result", "SUCCESS");
        return standardEventJson;
    }

    private static StandardEventFieldDefinition field(String fieldCode, String fieldType, String fieldPath,
                                                      Integer requiredFlag, String defaultValue) {
        return StandardEventFieldDefinition.builder()
                .fieldCode(fieldCode)
                .fieldType(fieldType)
                .fieldPath(fieldPath)
                .requiredFlag(requiredFlag)
                .defaultValue(defaultValue)
                .build();
    }

    private static StandardEventMappingDefinition mapping(String sourceFieldPath, String targetFieldCode,
                                                          String transformType, String transformExpr,
                                                          String defaultValue, Map<String, Object> cleanRuleJson) {
        return StandardEventMappingDefinition.builder()
                .sourceFieldPath(sourceFieldPath)
                .targetFieldCode(targetFieldCode)
                .transformType(transformType)
                .transformExpr(transformExpr)
                .defaultValue(defaultValue)
                .cleanRuleJson(cleanRuleJson)
                .build();
    }

}
