package cn.liboshuai.pulsix.access.ingest.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import java.time.Clock;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;

class PulsixIngestPropertiesTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(PulsixIngestConfiguration.class)
            .withPropertyValues(
                    "pulsix.access.ingest.zone-id=UTC",
                    "pulsix.access.ingest.http.path=/custom/access/events",
                    "pulsix.access.ingest.http.max-payload-bytes=4096",
                    "pulsix.access.ingest.netty.port=29091",
                    "pulsix.access.ingest.kafka.standard-topic-name=pulsix.event.standard.test",
                    "pulsix.access.ingest.kafka.send-max-attempts=5",
                    "pulsix.access.ingest.kafka.send-backoff-millis=50",
                    "pulsix.access.ingest.config-cache.refresh-interval-seconds=45"
            );

    @Test
    void shouldBindPropertiesAndRegisterCoreBeans() {
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(PulsixIngestProperties.class);
            assertThat(context).hasSingleBean(ObjectMapper.class);
            assertThat(context).hasSingleBean(Clock.class);

            PulsixIngestProperties properties = context.getBean(PulsixIngestProperties.class);
            Clock clock = context.getBean(Clock.class);

            assertThat(properties.getZoneId()).isEqualTo("UTC");
            assertThat(properties.getHttp().getPath()).isEqualTo("/custom/access/events");
            assertThat(properties.getHttp().getMaxPayloadBytes()).isEqualTo(4096);
            assertThat(properties.getNetty().getPort()).isEqualTo(29091);
            assertThat(properties.getKafka().getStandardTopicName()).isEqualTo("pulsix.event.standard.test");
            assertThat(properties.getKafka().getSendMaxAttempts()).isEqualTo(5);
            assertThat(properties.getKafka().getSendBackoffMillis()).isEqualTo(50L);
            assertThat(properties.getConfigCache().getRefreshIntervalSeconds()).isEqualTo(45);
            assertThat(clock.getZone()).isEqualTo(ZoneId.of("UTC"));
        });
    }

}
