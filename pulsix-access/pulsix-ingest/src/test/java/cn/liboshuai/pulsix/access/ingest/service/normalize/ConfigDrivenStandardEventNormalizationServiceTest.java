package cn.liboshuai.pulsix.access.ingest.service.normalize;

import cn.liboshuai.pulsix.access.ingest.config.PulsixIngestProperties;
import cn.liboshuai.pulsix.access.ingest.domain.config.EventFieldConfig;
import cn.liboshuai.pulsix.access.ingest.domain.config.IngestMappingConfig;
import cn.liboshuai.pulsix.access.ingest.domain.config.IngestRuntimeConfig;
import cn.liboshuai.pulsix.access.ingest.domain.config.IngestSourceConfig;
import cn.liboshuai.pulsix.access.ingest.service.config.IngestDesignConfigService;
import cn.liboshuai.pulsix.framework.common.biz.risk.access.normalize.StandardEventNormalizeResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ConfigDrivenStandardEventNormalizationServiceTest {

    private ConfigDrivenStandardEventNormalizationService service;

    @BeforeEach
    void setUp() {
        PulsixIngestProperties properties = new PulsixIngestProperties();
        properties.setZoneId("Asia/Shanghai");

        service = new ConfigDrivenStandardEventNormalizationService();
        ReflectionTestUtils.setField(service, "properties", properties);
        ReflectionTestUtils.setField(service, "configService", new FakeConfigService());
    }

    @Test
    void shouldNormalizeTradeHttpDemoPayloadWithConfig() {
        Map<String, Object> rawEventJson = new LinkedHashMap<>();
        rawEventJson.put("event_id", "E_RAW_9103");
        rawEventJson.put("occur_time_ms", 1773287100000L);
        rawEventJson.put("req", Map.of("traceId", "T_RAW_9103"));
        rawEventJson.put("uid", " U9003 ");
        rawEventJson.put("dev_id", "D9003");
        rawEventJson.put("client_ip", "88.66.55.44");
        rawEventJson.put("pay_amt", 256800);
        rawEventJson.put("trade_result", "ok");

        StandardEventNormalizeResult result = service.normalize("trade_http_demo", "TRADE_RISK", "TRADE_EVENT", rawEventJson);

        assertThat(result.getStandardEventJson()).containsExactlyEntriesOf(expectedStandardEventJson());
        assertThat(result.getDefaultedFields()).containsExactly("sceneCode", "eventType");
        assertThat(result.getMappedFields()).containsExactly("eventId", "eventTime", "traceId", "userId",
                "deviceId", "ip", "amount", "result");
        assertThat(result.getMissingRequiredFields()).isEmpty();
    }

    private Map<String, Object> expectedStandardEventJson() {
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

    private static final class FakeConfigService implements IngestDesignConfigService {

        @Override
        public IngestRuntimeConfig getConfig(String sourceCode, String sceneCode, String eventCode) {
            return IngestRuntimeConfig.builder()
                    .sourceCode(sourceCode)
                    .sceneCode(sceneCode)
                    .eventCode(eventCode)
                    .source(IngestSourceConfig.builder().sourceCode(sourceCode).build())
                    .mappings(List.of(
                            IngestMappingConfig.builder().sourceCode(sourceCode).sceneCode(sceneCode).eventCode(eventCode)
                                    .sourceFieldPath("$.event_id").targetFieldCode("eventId").transformType("DIRECT")
                                    .cleanRuleJson(Map.of()).build(),
                            IngestMappingConfig.builder().sourceCode(sourceCode).sceneCode(sceneCode).eventCode(eventCode)
                                    .sourceFieldPath("$.occur_time_ms").targetFieldCode("eventTime")
                                    .transformType("TIME_MILLIS_TO_DATETIME").cleanRuleJson(Map.of()).build(),
                            IngestMappingConfig.builder().sourceCode(sourceCode).sceneCode(sceneCode).eventCode(eventCode)
                                    .sourceFieldPath("$.req.traceId").targetFieldCode("traceId").transformType("DIRECT")
                                    .cleanRuleJson(Map.of()).build(),
                            IngestMappingConfig.builder().sourceCode(sourceCode).sceneCode(sceneCode).eventCode(eventCode)
                                    .sourceFieldPath("$.uid").targetFieldCode("userId").transformType("DIRECT")
                                    .cleanRuleJson(Map.of("trim", true)).build(),
                            IngestMappingConfig.builder().sourceCode(sourceCode).sceneCode(sceneCode).eventCode(eventCode)
                                    .sourceFieldPath("$.dev_id").targetFieldCode("deviceId").transformType("DIRECT")
                                    .cleanRuleJson(Map.of()).build(),
                            IngestMappingConfig.builder().sourceCode(sourceCode).sceneCode(sceneCode).eventCode(eventCode)
                                    .sourceFieldPath("$.client_ip").targetFieldCode("ip").transformType("DIRECT")
                                    .cleanRuleJson(Map.of()).build(),
                            IngestMappingConfig.builder().sourceCode(sourceCode).sceneCode(sceneCode).eventCode(eventCode)
                                    .sourceFieldPath("$.pay_amt").targetFieldCode("amount").transformType("DIVIDE_100")
                                    .cleanRuleJson(Map.of()).build(),
                            IngestMappingConfig.builder().sourceCode(sourceCode).sceneCode(sceneCode).eventCode(eventCode)
                                    .sourceFieldPath("$.trade_result").targetFieldCode("result").transformType("ENUM_MAP")
                                    .transformExpr("{\"ok\":\"SUCCESS\",\"fail\":\"FAIL\"}")
                                    .cleanRuleJson(Map.of("lowerCase", true)).build()
                    ))
                    .eventFields(List.of(
                            EventFieldConfig.builder().sceneCode(sceneCode).eventCode(eventCode).fieldCode("eventId")
                                    .fieldType("STRING").fieldPath("$.eventId").requiredFlag(1).sortNo(10).build(),
                            EventFieldConfig.builder().sceneCode(sceneCode).eventCode(eventCode).fieldCode("sceneCode")
                                    .fieldType("STRING").fieldPath("$.sceneCode").requiredFlag(1)
                                    .defaultValue("TRADE_RISK").sortNo(20).build(),
                            EventFieldConfig.builder().sceneCode(sceneCode).eventCode(eventCode).fieldCode("eventType")
                                    .fieldType("STRING").fieldPath("$.eventType").requiredFlag(1)
                                    .defaultValue("trade").sortNo(30).build(),
                            EventFieldConfig.builder().sceneCode(sceneCode).eventCode(eventCode).fieldCode("eventTime")
                                    .fieldType("DATETIME").fieldPath("$.eventTime").requiredFlag(1).sortNo(40).build(),
                            EventFieldConfig.builder().sceneCode(sceneCode).eventCode(eventCode).fieldCode("traceId")
                                    .fieldType("STRING").fieldPath("$.traceId").requiredFlag(0).sortNo(50).build(),
                            EventFieldConfig.builder().sceneCode(sceneCode).eventCode(eventCode).fieldCode("userId")
                                    .fieldType("STRING").fieldPath("$.userId").requiredFlag(1).sortNo(60).build(),
                            EventFieldConfig.builder().sceneCode(sceneCode).eventCode(eventCode).fieldCode("deviceId")
                                    .fieldType("STRING").fieldPath("$.deviceId").requiredFlag(1).sortNo(70).build(),
                            EventFieldConfig.builder().sceneCode(sceneCode).eventCode(eventCode).fieldCode("ip")
                                    .fieldType("STRING").fieldPath("$.ip").requiredFlag(1).sortNo(80).build(),
                            EventFieldConfig.builder().sceneCode(sceneCode).eventCode(eventCode).fieldCode("amount")
                                    .fieldType("DECIMAL").fieldPath("$.amount").requiredFlag(1).sortNo(90).build(),
                            EventFieldConfig.builder().sceneCode(sceneCode).eventCode(eventCode).fieldCode("result")
                                    .fieldType("STRING").fieldPath("$.result").requiredFlag(1).sortNo(100).build()
                    ))
                    .loadedAt(Instant.parse("2026-03-13T00:00:00Z"))
                    .build();
        }

        @Override
        public void invalidate(String sourceCode, String sceneCode, String eventCode) {
        }

        @Override
        public void clear() {
        }

    }

}
