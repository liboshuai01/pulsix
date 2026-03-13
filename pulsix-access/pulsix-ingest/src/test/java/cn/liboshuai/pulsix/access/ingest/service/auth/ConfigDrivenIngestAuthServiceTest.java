package cn.liboshuai.pulsix.access.ingest.service.auth;

import cn.liboshuai.pulsix.access.ingest.domain.config.IngestRuntimeConfig;
import cn.liboshuai.pulsix.access.ingest.domain.config.IngestSourceConfig;
import cn.liboshuai.pulsix.framework.common.biz.risk.access.dto.AccessIngestRequestDTO;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ConfigDrivenIngestAuthServiceTest {

    private final ConfigDrivenIngestAuthService service = new ConfigDrivenIngestAuthService();

    @Test
    void shouldPassWhenAuthTypeIsNone() {
        assertThatCode(() -> service.authenticate(AccessIngestRequestDTO.builder()
                        .sourceCode("http_none_demo")
                        .metadata(Map.of())
                        .build(), IngestRuntimeConfig.builder()
                        .source(IngestSourceConfig.builder().authType("NONE").build())
                        .build()))
                .doesNotThrowAnyException();
    }

    @Test
    void shouldPassWhenTokenHeaderMatchesExpectedToken() {
        assertThatCode(() -> service.authenticate(AccessIngestRequestDTO.builder()
                        .sourceCode("http_token_demo")
                        .metadata(Map.of("authorization", "Bearer token-demo-001"))
                        .build(), IngestRuntimeConfig.builder()
                        .source(IngestSourceConfig.builder()
                                .authType("TOKEN")
                                .authConfigJson(Map.of(
                                        "tokenHeader", "Authorization",
                                        "tokenPrefix", "Bearer ",
                                        "tokenValue", "token-demo-001"
                                ))
                                .build())
                        .build()))
                .doesNotThrowAnyException();
    }

    @Test
    void shouldRejectWhenTokenHeaderMissing() {
        assertThatThrownBy(() -> service.authenticate(AccessIngestRequestDTO.builder()
                        .sourceCode("http_token_demo")
                        .metadata(Map.of())
                        .build(), IngestRuntimeConfig.builder()
                        .source(IngestSourceConfig.builder()
                                .authType("TOKEN")
                                .authConfigJson(Map.of("tokenHeader", "Authorization", "tokenPrefix", "Bearer "))
                                .build())
                        .build()))
                .isInstanceOf(IngestAuthException.class)
                .hasMessage("缺少 Token 请求头: Authorization");
    }

}
