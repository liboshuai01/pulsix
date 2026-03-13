package cn.liboshuai.pulsix.access.ingest.service.auth;

import cn.liboshuai.pulsix.access.ingest.domain.config.IngestRuntimeConfig;
import cn.liboshuai.pulsix.access.ingest.domain.config.IngestSourceConfig;
import cn.liboshuai.pulsix.framework.common.biz.risk.access.dto.AccessIngestRequestDTO;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Base64;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ConfigDrivenIngestAuthServiceTest {

    private static final HexFormat HEX_FORMAT = HexFormat.of();

    private final ConfigDrivenIngestAuthService service = new ConfigDrivenIngestAuthService();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "clock",
                Clock.fixed(Instant.parse("2026-03-13T02:31:00Z"), ZoneId.of("UTC")));
        ReflectionTestUtils.setField(service, "objectMapper", objectMapper);
    }

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

    @Test
    void shouldPassWhenHmacSignatureMatchesTradeHttpDemoSample() {
        String timestamp = String.valueOf(Instant.parse("2026-03-13T02:31:00Z").toEpochMilli());
        String payload = "{\"event_id\":\"E_RAW_9103\",\"occur_time_ms\":1773287100000,\"req\":{\"traceId\":\"T_RAW_9103\"},\"uid\":\" U9003 \",\"dev_id\":\"D9003\",\"client_ip\":\"88.66.55.44\",\"pay_amt\":256800,\"trade_result\":\"ok\"}";
        String signature = signHex("HmacSHA256", "trade-http-demo", "trade-http-demo", timestamp, payload);

        assertThatCode(() -> service.authenticate(AccessIngestRequestDTO.builder()
                        .sourceCode("trade_http_demo")
                        .payload(payload)
                        .metadata(Map.of(
                                "x-pulsix-signature", signature,
                                "x-pulsix-timestamp", timestamp
                        ))
                        .build(), IngestRuntimeConfig.builder()
                        .source(IngestSourceConfig.builder()
                                .authType("HMAC")
                                .authConfigJson(Map.of(
                                        "headerName", "X-Pulsix-Signature",
                                        "timestampHeader", "X-Pulsix-Timestamp",
                                        "algorithm", "HmacSHA256",
                                        "appKey", "trade-http-demo",
                                        "appSecret", "trade-http-demo"
                                ))
                                .build())
                        .build()))
                .doesNotThrowAnyException();
    }

    @Test
    void shouldRejectWhenHmacSecretMissing() {
        String timestamp = String.valueOf(Instant.parse("2026-03-13T02:31:00Z").toEpochMilli());
        String payload = "{\"event_id\":\"E_RAW_9103\"}";

        assertThatThrownBy(() -> service.authenticate(AccessIngestRequestDTO.builder()
                        .sourceCode("trade_http_demo")
                        .payload(payload)
                        .metadata(Map.of(
                                "x-pulsix-signature", "bad-signature",
                                "x-pulsix-timestamp", timestamp
                        ))
                        .build(), IngestRuntimeConfig.builder()
                        .source(IngestSourceConfig.builder()
                                .authType("HMAC")
                                .authConfigJson(Map.of(
                                        "headerName", "X-Pulsix-Signature",
                                        "timestampHeader", "X-Pulsix-Timestamp",
                                        "algorithm", "HmacSHA256",
                                        "appKey", "trade-http-demo"
                                ))
                                .build())
                        .build()))
                .isInstanceOfSatisfying(IngestAuthException.class, ex -> {
                    assertThat(ex.getErrorCode()).isEqualTo("AUTH_SIGN_CONFIG_INVALID");
                    assertThat(ex.getMessage()).isEqualTo("HMAC 鉴权配置缺少 appKey 或密钥");
                });
    }

    @Test
    void shouldRejectWhenHmacSignatureInvalid() {
        String timestamp = String.valueOf(Instant.parse("2026-03-13T02:31:00Z").toEpochMilli());
        String payload = "{\"event_id\":\"raw_trade_bad_8103\",\"uid\":\"U8103\",\"dev_id\":\"D8103\",\"client_ip\":\"88.10.20.30\",\"pay_amt\":128000,\"trade_result\":\"ok\"}";

        assertThatThrownBy(() -> service.authenticate(AccessIngestRequestDTO.builder()
                        .sourceCode("trade_http_demo")
                        .payload(payload)
                        .metadata(Map.of(
                                "x-pulsix-signature", "bad-signature",
                                "x-pulsix-timestamp", timestamp
                        ))
                        .build(), IngestRuntimeConfig.builder()
                        .source(IngestSourceConfig.builder()
                                .authType("HMAC")
                                .authConfigJson(Map.of(
                                        "headerName", "X-Pulsix-Signature",
                                        "timestampHeader", "X-Pulsix-Timestamp",
                                        "algorithm", "HmacSHA256",
                                        "appKey", "trade-http-demo",
                                        "appSecret", "trade-http-demo"
                                ))
                                .build())
                        .build()))
                .isInstanceOfSatisfying(IngestAuthException.class, ex -> {
                    assertThat(ex.getErrorCode()).isEqualTo("AUTH_SIGN_INVALID");
                    assertThat(ex.getMessage()).isEqualTo("签名校验失败，拒绝写入标准事件 Topic");
                });
    }

    @Test
    void shouldRejectWhenHmacTimestampExpired() {
        String timestamp = String.valueOf(Instant.parse("2026-03-13T02:25:59Z").toEpochMilli());
        String payload = "{\"event_id\":\"E_RAW_9103\"}";
        String signature = signHex("HmacSHA256", "trade-http-demo", "trade-http-demo", timestamp, payload);

        assertThatThrownBy(() -> service.authenticate(AccessIngestRequestDTO.builder()
                        .sourceCode("trade_http_demo")
                        .payload(payload)
                        .metadata(Map.of(
                                "x-pulsix-signature", signature,
                                "x-pulsix-timestamp", timestamp
                        ))
                        .build(), IngestRuntimeConfig.builder()
                        .source(IngestSourceConfig.builder()
                                .authType("HMAC")
                                .authConfigJson(Map.of(
                                        "headerName", "X-Pulsix-Signature",
                                        "timestampHeader", "X-Pulsix-Timestamp",
                                        "algorithm", "HmacSHA256",
                                        "appKey", "trade-http-demo",
                                        "appSecret", "trade-http-demo"
                                ))
                                .build())
                        .build()))
                .isInstanceOfSatisfying(IngestAuthException.class, ex -> {
                    assertThat(ex.getErrorCode()).isEqualTo("AUTH_TIMESTAMP_EXPIRED");
                    assertThat(ex.getMessage()).isEqualTo("请求时间戳超出允许范围");
                });
    }

    @Test
    void shouldPassWhenAkskSignatureMatches() {
        String timestamp = String.valueOf(Instant.parse("2026-03-13T02:31:00Z").toEpochMilli());
        String nonce = "NONCE-1001";
        String payload = "{\"event_id\":\"E_AKSK_1\"}";
        String signature = signAkskHex("HmacSHA256", "aksk-secret-001", "aksk-demo-001", timestamp, nonce, payload);

        assertThatCode(() -> service.authenticate(AccessIngestRequestDTO.builder()
                        .sourceCode("trade_aksk_demo")
                        .payload(payload)
                        .metadata(Map.of(
                                "x-pulsix-access-key", "aksk-demo-001",
                                "x-pulsix-signature", signature,
                                "x-pulsix-timestamp", timestamp,
                                "x-pulsix-nonce", nonce
                        ))
                        .build(), IngestRuntimeConfig.builder()
                        .source(IngestSourceConfig.builder()
                                .authType("AKSK")
                                .authConfigJson(Map.of(
                                        "accessKey", "aksk-demo-001",
                                        "secretKey", "aksk-secret-001",
                                        "algorithm", "HmacSHA256"
                                ))
                                .build())
                        .build()))
                .doesNotThrowAnyException();
    }

    @Test
    void shouldRejectWhenAkskAccessKeyMismatch() {
        String timestamp = String.valueOf(Instant.parse("2026-03-13T02:31:00Z").toEpochMilli());
        String payload = "{\"event_id\":\"E_AKSK_2\"}";

        assertThatThrownBy(() -> service.authenticate(AccessIngestRequestDTO.builder()
                        .sourceCode("trade_aksk_demo")
                        .payload(payload)
                        .metadata(Map.of(
                                "x-pulsix-access-key", "bad-key",
                                "x-pulsix-signature", "bad-signature",
                                "x-pulsix-timestamp", timestamp,
                                "x-pulsix-nonce", "NONCE-1002"
                        ))
                        .build(), IngestRuntimeConfig.builder()
                        .source(IngestSourceConfig.builder()
                                .authType("AKSK")
                                .authConfigJson(Map.of(
                                        "accessKey", "aksk-demo-001",
                                        "secretKey", "aksk-secret-001"
                                ))
                                .build())
                        .build()))
                .isInstanceOfSatisfying(IngestAuthException.class, ex -> {
                    assertThat(ex.getErrorCode()).isEqualTo("AUTH_AKSK_ACCESS_KEY_INVALID");
                    assertThat(ex.getMessage()).isEqualTo("AccessKey 校验失败");
                });
    }

    @Test
    void shouldPassWhenJwtTokenValid() {
        String token = createJwt("jwt-secret-001", Map.of("alg", "HS256", "typ", "JWT"), new LinkedHashMap<>(Map.of(
                "iss", "pulsix-access",
                "aud", List.of("trade-ingest"),
                "sub", "trade-client",
                "exp", Instant.parse("2026-03-13T02:33:00Z").getEpochSecond(),
                "nbf", Instant.parse("2026-03-13T02:30:00Z").getEpochSecond()
        )));

        assertThatCode(() -> service.authenticate(AccessIngestRequestDTO.builder()
                        .sourceCode("trade_jwt_demo")
                        .metadata(Map.of("authorization", "Bearer " + token))
                        .build(), IngestRuntimeConfig.builder()
                        .source(IngestSourceConfig.builder()
                                .authType("JWT")
                                .authConfigJson(Map.of(
                                        "tokenHeader", "Authorization",
                                        "tokenPrefix", "Bearer ",
                                        "algorithm", "HS256",
                                        "jwtSecret", "jwt-secret-001",
                                        "issuer", "pulsix-access",
                                        "audience", "trade-ingest",
                                        "requiredSubject", "trade-client"
                                ))
                                .build())
                        .build()))
                .doesNotThrowAnyException();
    }

    @Test
    void shouldRejectWhenJwtExpired() {
        String token = createJwt("jwt-secret-001", Map.of("alg", "HS256", "typ", "JWT"), new LinkedHashMap<>(Map.of(
                "iss", "pulsix-access",
                "aud", "trade-ingest",
                "sub", "trade-client",
                "exp", Instant.parse("2026-03-13T02:20:00Z").getEpochSecond()
        )));

        assertThatThrownBy(() -> service.authenticate(AccessIngestRequestDTO.builder()
                        .sourceCode("trade_jwt_demo")
                        .metadata(Map.of("authorization", "Bearer " + token))
                        .build(), IngestRuntimeConfig.builder()
                        .source(IngestSourceConfig.builder()
                                .authType("JWT")
                                .authConfigJson(Map.of(
                                        "algorithm", "HS256",
                                        "jwtSecret", "jwt-secret-001",
                                        "issuer", "pulsix-access",
                                        "audience", "trade-ingest"
                                ))
                                .build())
                        .build()))
                .isInstanceOfSatisfying(IngestAuthException.class, ex -> {
                    assertThat(ex.getErrorCode()).isEqualTo("AUTH_JWT_EXPIRED");
                    assertThat(ex.getMessage()).isEqualTo("JWT 已过期");
                });
    }

    private String signHex(String algorithm, String secret, String appKey, String timestamp, String payload) {
        return hexSign(algorithm, secret, appKey + '\n' + timestamp + '\n' + payload);
    }

    private String signAkskHex(String algorithm, String secret, String accessKey, String timestamp, String nonce, String payload) {
        return hexSign(algorithm, secret, accessKey + '\n' + timestamp + '\n' + nonce + '\n' + payload);
    }

    private String hexSign(String algorithm, String secret, String content) {
        try {
            Mac mac = Mac.getInstance(algorithm);
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), algorithm));
            byte[] digest = mac.doFinal(content.getBytes(StandardCharsets.UTF_8));
            return HEX_FORMAT.formatHex(digest);
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
    }

    private String createJwt(String secret, Map<String, Object> header, Map<String, Object> claims) {
        try {
            String headerPart = Base64.getUrlEncoder().withoutPadding()
                    .encodeToString(objectMapper.writeValueAsBytes(header));
            String payloadPart = Base64.getUrlEncoder().withoutPadding()
                    .encodeToString(objectMapper.writeValueAsBytes(claims));
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] signature = mac.doFinal((headerPart + '.' + payloadPart).getBytes(StandardCharsets.UTF_8));
            String signaturePart = Base64.getUrlEncoder().withoutPadding().encodeToString(signature);
            return headerPart + '.' + payloadPart + '.' + signaturePart;
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
    }

}
