package cn.liboshuai.pulsix.access.ingest.service.ratelimit;

import cn.liboshuai.pulsix.access.ingest.domain.config.IngestRuntimeConfig;
import cn.liboshuai.pulsix.access.ingest.domain.config.IngestSourceConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class InMemoryIngestRateLimitServiceTest {

    private final InMemoryIngestRateLimitService service = new InMemoryIngestRateLimitService();
    private MutableClock clock;

    @BeforeEach
    void setUp() {
        clock = new MutableClock(Instant.parse("2026-03-13T02:31:00Z"), ZoneId.of("UTC"));
        ReflectionTestUtils.setField(service, "clock", clock);
    }

    @Test
    void shouldRejectWhenSourceQpsExceededWithinSameSecond() {
        IngestRuntimeConfig runtimeConfig = IngestRuntimeConfig.builder()
                .sourceCode("trade_sdk_demo")
                .source(IngestSourceConfig.builder().sourceCode("trade_sdk_demo").rateLimitQps(1).build())
                .build();

        assertThatCode(() -> service.checkAllowed(runtimeConfig)).doesNotThrowAnyException();
        assertThatThrownBy(() -> service.checkAllowed(runtimeConfig))
                .isInstanceOf(IngestRateLimitException.class)
                .hasMessage("接入源请求频率超出限制: sourceCode=trade_sdk_demo, qps=1");

        clock.plusSeconds(1);
        assertThatCode(() -> service.checkAllowed(runtimeConfig)).doesNotThrowAnyException();
    }

    private static final class MutableClock extends Clock {

        private Instant instant;
        private final ZoneId zoneId;

        private MutableClock(Instant instant, ZoneId zoneId) {
            this.instant = instant;
            this.zoneId = zoneId;
        }

        private void plusSeconds(long seconds) {
            this.instant = this.instant.plusSeconds(seconds);
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

    }

}
