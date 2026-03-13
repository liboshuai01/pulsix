package cn.liboshuai.pulsix.access.ingest.service.config;

import cn.liboshuai.pulsix.access.ingest.config.PulsixIngestProperties;
import cn.liboshuai.pulsix.access.ingest.domain.config.EventFieldConfig;
import cn.liboshuai.pulsix.access.ingest.domain.config.IngestMappingConfig;
import cn.liboshuai.pulsix.access.ingest.domain.config.IngestRuntimeConfig;
import cn.liboshuai.pulsix.access.ingest.domain.config.IngestSourceConfig;
import cn.liboshuai.pulsix.framework.common.exception.ServiceException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CachingIngestDesignConfigServiceTest {

    private MutableClock clock;
    private FakeRepository repository;
    private CachingIngestDesignConfigService service;
    private PulsixIngestProperties properties;

    @BeforeEach
    void setUp() {
        clock = new MutableClock(Instant.parse("2026-03-13T00:00:00Z"), ZoneId.of("UTC"));
        repository = new FakeRepository();
        properties = new PulsixIngestProperties();
        properties.getConfigCache().setRefreshIntervalSeconds(30);

        service = new CachingIngestDesignConfigService();
        ReflectionTestUtils.setField(service, "clock", clock);
        ReflectionTestUtils.setField(service, "repository", repository);
        ReflectionTestUtils.setField(service, "properties", properties);
    }

    @Test
    void shouldLoadCompleteTradeHttpDemoConfig() {
        IngestRuntimeConfig config = service.getConfig("trade_http_demo", "TRADE_RISK", "TRADE_EVENT");

        assertThat(config.getSourceCode()).isEqualTo("trade_http_demo");
        assertThat(config.getSceneCode()).isEqualTo("TRADE_RISK");
        assertThat(config.getEventCode()).isEqualTo("TRADE_EVENT");
        assertThat(config.getSource().getAuthType()).isEqualTo("HMAC");
        assertThat(config.getSource().getSceneScope()).containsExactly("TRADE_RISK");
        assertThat(config.getMappings()).hasSize(3);
        assertThat(config.getMappingsByTargetField()).containsKeys("eventId", "eventTime", "amount");
        assertThat(config.getEventFields()).extracting(EventFieldConfig::getFieldCode)
                .containsExactly("eventId", "eventTime", "amount");
        assertThat(repository.sourceQueries.get()).isEqualTo(1);
        assertThat(repository.mappingQueries.get()).isEqualTo(1);
        assertThat(repository.fieldQueries.get()).isEqualTo(1);
    }

    @Test
    void shouldReuseCacheWithinRefreshIntervalAndReloadAfterExpiry() {
        service.getConfig("trade_http_demo", "TRADE_RISK", "TRADE_EVENT");
        service.getConfig("trade_http_demo", "TRADE_RISK", "TRADE_EVENT");

        assertThat(repository.sourceQueries.get()).isEqualTo(1);
        assertThat(repository.mappingQueries.get()).isEqualTo(1);
        assertThat(repository.fieldQueries.get()).isEqualTo(1);

        clock.plusSeconds(31);
        service.getConfig("trade_http_demo", "TRADE_RISK", "TRADE_EVENT");

        assertThat(repository.sourceQueries.get()).isEqualTo(2);
        assertThat(repository.mappingQueries.get()).isEqualTo(2);
        assertThat(repository.fieldQueries.get()).isEqualTo(2);
    }

    @Test
    void shouldEvictOldestSourceWhenCacheExceedsMaxSourceEntries() {
        properties.getConfigCache().setMaxSourceEntries(1);

        service.getConfig("trade_http_demo", "TRADE_RISK", "TRADE_EVENT");
        service.getConfig("trade_sdk_demo", "TRADE_RISK", "TRADE_EVENT");
        service.getConfig("trade_http_demo", "TRADE_RISK", "TRADE_EVENT");

        assertThat(repository.sourceQueries.get()).isEqualTo(3);
        assertThat(repository.mappingQueries.get()).isEqualTo(3);
        assertThat(repository.fieldQueries.get()).isEqualTo(3);
    }

    @Test
    void shouldRejectSceneOutOfScope() {
        assertThatThrownBy(() -> service.getConfig("trade_http_demo", "LOGIN_RISK", "TRADE_EVENT"))
                .isInstanceOf(ServiceException.class)
                .hasMessage("接入源 trade_http_demo 未开放场景 LOGIN_RISK");
    }

    private static final class FakeRepository implements IngestDesignConfigRepository {

        private final AtomicInteger sourceQueries = new AtomicInteger();
        private final AtomicInteger mappingQueries = new AtomicInteger();
        private final AtomicInteger fieldQueries = new AtomicInteger();

        @Override
        public Optional<IngestSourceConfig> findSource(String sourceCode) {
            sourceQueries.incrementAndGet();
            if (!"trade_http_demo".equals(sourceCode) && !"trade_sdk_demo".equals(sourceCode)) {
                return Optional.empty();
            }
            return Optional.of(IngestSourceConfig.builder()
                    .sourceCode(sourceCode)
                    .sourceName(sourceCode)
                    .sourceType("trade_http_demo".equals(sourceCode) ? "HTTP" : "SDK")
                    .authType("trade_http_demo".equals(sourceCode) ? "HMAC" : "TOKEN")
                    .authConfigJson(Map.of("headerName", "X-Pulsix-Signature"))
                    .sceneScope(Set.of("TRADE_RISK"))
                    .standardTopicName("pulsix.event.standard")
                    .errorTopicName("pulsix.event.dlq")
                    .rateLimitQps(300)
                    .status(0)
                    .description("demo")
                    .build());
        }

        @Override
        public List<IngestMappingConfig> findEnabledMappings(String sourceCode, String sceneCode, String eventCode) {
            mappingQueries.incrementAndGet();
            return List.of(
                    IngestMappingConfig.builder().sourceCode(sourceCode).sceneCode(sceneCode).eventCode(eventCode)
                            .sourceFieldPath("$.event_id").targetFieldCode("eventId").transformType("DIRECT").sortNo(10).status(0).build(),
                    IngestMappingConfig.builder().sourceCode(sourceCode).sceneCode(sceneCode).eventCode(eventCode)
                            .sourceFieldPath("$.occur_time_ms").targetFieldCode("eventTime").transformType("TIME_MILLIS_TO_DATETIME").sortNo(20).status(0).build(),
                    IngestMappingConfig.builder().sourceCode(sourceCode).sceneCode(sceneCode).eventCode(eventCode)
                            .sourceFieldPath("$.pay_amt").targetFieldCode("amount").transformType("DIVIDE_100").sortNo(30).status(0).build()
            );
        }

        @Override
        public List<EventFieldConfig> findEventFields(String sceneCode, String eventCode) {
            fieldQueries.incrementAndGet();
            return List.of(
                    EventFieldConfig.builder().sceneCode(sceneCode).eventCode(eventCode).fieldCode("eventId").fieldType("STRING").requiredFlag(1).sortNo(10).build(),
                    EventFieldConfig.builder().sceneCode(sceneCode).eventCode(eventCode).fieldCode("eventTime").fieldType("DATETIME").requiredFlag(1).sortNo(20).build(),
                    EventFieldConfig.builder().sceneCode(sceneCode).eventCode(eventCode).fieldCode("amount").fieldType("DECIMAL").requiredFlag(1).sortNo(30).build()
            );
        }

    }

    private static final class MutableClock extends Clock {

        private Instant instant;
        private final ZoneId zoneId;

        private MutableClock(Instant instant, ZoneId zoneId) {
            this.instant = instant;
            this.zoneId = zoneId;
        }

        @Override
        public ZoneId getZone() {
            return zoneId;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return new MutableClock(instant, zone);
        }

        @Override
        public Instant instant() {
            return instant;
        }

        private void plusSeconds(long seconds) {
            instant = instant.plusSeconds(seconds);
        }

    }

}
