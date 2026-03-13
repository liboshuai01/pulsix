package cn.liboshuai.pulsix.access.ingest.service.auth;

import cn.hutool.core.util.StrUtil;
import cn.liboshuai.pulsix.access.ingest.domain.config.IngestRuntimeConfig;
import cn.liboshuai.pulsix.framework.common.biz.risk.access.dto.AccessIngestRequestDTO;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.Base64;
import java.util.Collection;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

import static cn.liboshuai.pulsix.access.ingest.enums.ErrorCodeConstants.INGEST_AUTH_FAILED;
import static cn.liboshuai.pulsix.access.ingest.enums.ErrorCodeConstants.INGEST_AUTH_UNSUPPORTED;

@Service
public class ConfigDrivenIngestAuthService implements IngestAuthService {

    private static final String AUTH_NONE = "NONE";
    private static final String AUTH_TOKEN = "TOKEN";
    private static final String AUTH_HMAC = "HMAC";
    private static final String AUTH_AKSK = "AKSK";
    private static final String AUTH_JWT = "JWT";
    private static final String DEFAULT_TOKEN_HEADER = "Authorization";
    private static final String DEFAULT_JWT_PREFIX = "Bearer ";
    private static final String DEFAULT_HMAC_HEADER = "X-Pulsix-Signature";
    private static final String DEFAULT_HMAC_TIMESTAMP_HEADER = "X-Pulsix-Timestamp";
    private static final String DEFAULT_HMAC_ALGORITHM = "HmacSHA256";
    private static final String DEFAULT_AKSK_ACCESS_KEY_HEADER = "X-Pulsix-Access-Key";
    private static final String DEFAULT_AKSK_NONCE_HEADER = "X-Pulsix-Nonce";
    private static final String DEFAULT_JWT_ALGORITHM = "HS256";
    private static final long DEFAULT_HMAC_TOLERANCE_SECONDS = 300L;
    private static final TypeReference<LinkedHashMap<String, Object>> MAP_TYPE = new TypeReference<>() {
    };
    private static final HexFormat HEX_FORMAT = HexFormat.of();

    @Resource
    private Clock clock;

    @Resource
    private ObjectMapper objectMapper;

    @Override
    public void authenticate(AccessIngestRequestDTO request, IngestRuntimeConfig runtimeConfig) {
        String authType = normalize(runtimeConfig == null || runtimeConfig.getSource() == null
                ? null : runtimeConfig.getSource().getAuthType());
        if (authType == null || AUTH_NONE.equals(authType)) {
            return;
        }
        if (AUTH_TOKEN.equals(authType)) {
            authenticateToken(request, runtimeConfig);
            return;
        }
        if (AUTH_HMAC.equals(authType)) {
            authenticateHmac(request, runtimeConfig);
            return;
        }
        if (AUTH_AKSK.equals(authType)) {
            authenticateAksk(request, runtimeConfig);
            return;
        }
        if (AUTH_JWT.equals(authType)) {
            authenticateJwt(request, runtimeConfig);
            return;
        }
        throw new IngestAuthException(INGEST_AUTH_UNSUPPORTED.getCode(), "AUTH_TYPE_UNSUPPORTED",
                "暂不支持的鉴权方式: " + runtimeConfig.getSource().getAuthType());
    }

    private void authenticateToken(AccessIngestRequestDTO request, IngestRuntimeConfig runtimeConfig) {
        Map<String, Object> authConfig = resolveAuthConfig(runtimeConfig);
        String headerName = defaultIfBlank(toStringValue(authConfig.get("tokenHeader")), DEFAULT_TOKEN_HEADER);
        String expectedPrefix = defaultIfBlank(toStringValue(authConfig.get("tokenPrefix")), "");
        String expectedToken = firstNonBlank(
                toStringValue(authConfig.get("tokenValue")),
                toStringValue(authConfig.get("expectedToken")),
                toStringValue(authConfig.get("token"))
        );

        String headerValue = getMetadataValue(request, headerName);
        if (StrUtil.isBlank(headerValue)) {
            throw new IngestAuthException(INGEST_AUTH_FAILED.getCode(), "AUTH_TOKEN_MISSING",
                    "缺少 Token 请求头: " + headerName);
        }
        if (StrUtil.isNotBlank(expectedPrefix) && !StrUtil.startWith(headerValue, expectedPrefix)) {
            throw new IngestAuthException(INGEST_AUTH_FAILED.getCode(), "AUTH_TOKEN_PREFIX_INVALID",
                    "Token 前缀不合法");
        }
        String actualToken = StrUtil.trim(StrUtil.removePrefix(headerValue, expectedPrefix));
        if (StrUtil.isBlank(actualToken)) {
            throw new IngestAuthException(INGEST_AUTH_FAILED.getCode(), "AUTH_TOKEN_INVALID",
                    "Token 不能为空");
        }
        if (StrUtil.isNotBlank(expectedToken) && !StrUtil.equals(actualToken, expectedToken)) {
            throw new IngestAuthException(INGEST_AUTH_FAILED.getCode(), "AUTH_TOKEN_INVALID",
                    "Token 校验失败");
        }
    }

    private void authenticateHmac(AccessIngestRequestDTO request, IngestRuntimeConfig runtimeConfig) {
        Map<String, Object> authConfig = resolveAuthConfig(runtimeConfig);
        String signatureHeader = defaultIfBlank(toStringValue(authConfig.get("headerName")), DEFAULT_HMAC_HEADER);
        String timestampHeader = defaultIfBlank(toStringValue(authConfig.get("timestampHeader")),
                DEFAULT_HMAC_TIMESTAMP_HEADER);
        String algorithm = defaultIfBlank(toStringValue(authConfig.get("algorithm")), DEFAULT_HMAC_ALGORITHM);
        String appKey = firstNonBlank(
                toStringValue(authConfig.get("appKey")),
                toStringValue(authConfig.get("accessKey")),
                toStringValue(authConfig.get("clientKey"))
        );
        String secret = firstNonBlank(
                toStringValue(authConfig.get("appSecret")),
                toStringValue(authConfig.get("secretKey")),
                toStringValue(authConfig.get("secret"))
        );

        if (StrUtil.isBlank(appKey) || StrUtil.isBlank(secret)) {
            throw new IngestAuthException(INGEST_AUTH_FAILED.getCode(), "AUTH_SIGN_CONFIG_INVALID",
                    "HMAC 鉴权配置缺少 appKey 或密钥");
        }

        String actualSignature = requireHeader(request, signatureHeader, "AUTH_SIGN_MISSING", "缺少签名请求头: ");
        String timestampValue = requireHeader(request, timestampHeader, "AUTH_TIMESTAMP_MISSING", "缺少时间戳请求头: ");
        validateTimestamp(timestampValue, authConfig, "AUTH_TIMESTAMP_INVALID", "AUTH_TIMESTAMP_EXPIRED");

        byte[] signatureBytes = sign(algorithm, secret, buildStringToSign(appKey, timestampValue, request));
        if (!signatureMatches(actualSignature, signatureBytes)) {
            throw new IngestAuthException(INGEST_AUTH_FAILED.getCode(), "AUTH_SIGN_INVALID",
                    "签名校验失败，拒绝写入标准事件 Topic");
        }
    }

    private void authenticateAksk(AccessIngestRequestDTO request, IngestRuntimeConfig runtimeConfig) {
        Map<String, Object> authConfig = resolveAuthConfig(runtimeConfig);
        String accessKeyHeader = defaultIfBlank(toStringValue(authConfig.get("accessKeyHeader")),
                DEFAULT_AKSK_ACCESS_KEY_HEADER);
        String signatureHeader = defaultIfBlank(firstNonBlank(toStringValue(authConfig.get("signatureHeader")),
                toStringValue(authConfig.get("headerName"))), DEFAULT_HMAC_HEADER);
        String timestampHeader = defaultIfBlank(toStringValue(authConfig.get("timestampHeader")),
                DEFAULT_HMAC_TIMESTAMP_HEADER);
        String nonceHeader = defaultIfBlank(toStringValue(authConfig.get("nonceHeader")), DEFAULT_AKSK_NONCE_HEADER);
        String algorithm = defaultIfBlank(toStringValue(authConfig.get("algorithm")), DEFAULT_HMAC_ALGORITHM);
        String expectedAccessKey = firstNonBlank(
                toStringValue(authConfig.get("accessKey")),
                toStringValue(authConfig.get("appKey")),
                toStringValue(authConfig.get("clientKey"))
        );
        String secret = firstNonBlank(
                toStringValue(authConfig.get("secretKey")),
                toStringValue(authConfig.get("appSecret")),
                toStringValue(authConfig.get("secret"))
        );
        if (StrUtil.isBlank(expectedAccessKey) || StrUtil.isBlank(secret)) {
            throw new IngestAuthException(INGEST_AUTH_FAILED.getCode(), "AUTH_AKSK_CONFIG_INVALID",
                    "AKSK 鉴权配置缺少 accessKey 或密钥");
        }

        String actualAccessKey = requireHeader(request, accessKeyHeader,
                "AUTH_AKSK_ACCESS_KEY_MISSING", "缺少 AccessKey 请求头: ");
        if (!StrUtil.equals(actualAccessKey, expectedAccessKey)) {
            throw new IngestAuthException(INGEST_AUTH_FAILED.getCode(), "AUTH_AKSK_ACCESS_KEY_INVALID",
                    "AccessKey 校验失败");
        }

        String actualSignature = requireHeader(request, signatureHeader,
                "AUTH_AKSK_SIGN_MISSING", "缺少签名请求头: ");
        String timestampValue = requireHeader(request, timestampHeader,
                "AUTH_AKSK_TIMESTAMP_MISSING", "缺少时间戳请求头: ");
        validateTimestamp(timestampValue, authConfig, "AUTH_AKSK_TIMESTAMP_INVALID", "AUTH_AKSK_TIMESTAMP_EXPIRED");
        String nonceValue = StrUtil.trimToEmpty(getMetadataValue(request, nonceHeader));

        String stringToSign = expectedAccessKey + '\n' + timestampValue + '\n' + nonceValue + '\n'
                + StrUtil.nullToEmpty(request == null ? null : request.getPayload());
        byte[] signatureBytes = sign(algorithm, secret, stringToSign);
        if (!signatureMatches(actualSignature, signatureBytes)) {
            throw new IngestAuthException(INGEST_AUTH_FAILED.getCode(), "AUTH_AKSK_SIGN_INVALID",
                    "AKSK 签名校验失败");
        }
    }

    private void authenticateJwt(AccessIngestRequestDTO request, IngestRuntimeConfig runtimeConfig) {
        Map<String, Object> authConfig = resolveAuthConfig(runtimeConfig);
        String headerName = defaultIfBlank(toStringValue(authConfig.get("tokenHeader")), DEFAULT_TOKEN_HEADER);
        String tokenPrefix = defaultIfBlank(toStringValue(authConfig.get("tokenPrefix")), DEFAULT_JWT_PREFIX);
        String headerValue = requireHeader(request, headerName, "AUTH_JWT_MISSING", "缺少 JWT 请求头: ");
        if (StrUtil.isNotBlank(tokenPrefix) && !StrUtil.startWith(headerValue, tokenPrefix)) {
            throw new IngestAuthException(INGEST_AUTH_FAILED.getCode(), "AUTH_JWT_PREFIX_INVALID",
                    "JWT 前缀不合法");
        }
        String token = StrUtil.trim(StrUtil.removePrefix(headerValue, tokenPrefix));
        if (StrUtil.isBlank(token)) {
            throw new IngestAuthException(INGEST_AUTH_FAILED.getCode(), "AUTH_JWT_INVALID", "JWT 不能为空");
        }

        String[] parts = StrUtil.splitToArray(token, '.');
        if (parts.length != 3) {
            throw new IngestAuthException(INGEST_AUTH_FAILED.getCode(), "AUTH_JWT_INVALID", "JWT 格式不合法");
        }

        Map<String, Object> headerJson = parseJwtPart(parts[0], "AUTH_JWT_INVALID", "JWT Header 不是合法 JSON");
        Map<String, Object> payloadJson = parseJwtPart(parts[1], "AUTH_JWT_INVALID", "JWT Payload 不是合法 JSON");
        String actualAlgorithm = normalizeJwtAlgorithm(toStringValue(headerJson.get("alg")));
        String expectedAlgorithm = normalizeJwtAlgorithm(defaultIfBlank(toStringValue(authConfig.get("algorithm")),
                DEFAULT_JWT_ALGORITHM));
        if (StrUtil.isBlank(actualAlgorithm)) {
            throw new IngestAuthException(INGEST_AUTH_FAILED.getCode(), "AUTH_JWT_ALGORITHM_INVALID",
                    "JWT 缺少签名算法");
        }
        if (StrUtil.isNotBlank(expectedAlgorithm) && !StrUtil.equals(actualAlgorithm, expectedAlgorithm)) {
            throw new IngestAuthException(INGEST_AUTH_FAILED.getCode(), "AUTH_JWT_ALGORITHM_INVALID",
                    "JWT 签名算法不匹配");
        }
        String hmacAlgorithm = resolveJwtHmacAlgorithm(actualAlgorithm);
        String secret = firstNonBlank(
                toStringValue(authConfig.get("jwtSecret")),
                toStringValue(authConfig.get("secretKey")),
                toStringValue(authConfig.get("signingKey")),
                toStringValue(authConfig.get("secret"))
        );
        if (StrUtil.isBlank(secret)) {
            throw new IngestAuthException(INGEST_AUTH_FAILED.getCode(), "AUTH_JWT_CONFIG_INVALID",
                    "JWT 鉴权配置缺少签名密钥");
        }
        byte[] signatureBytes = sign(hmacAlgorithm, secret, parts[0] + '.' + parts[1]);
        String expectedSignature = Base64.getUrlEncoder().withoutPadding().encodeToString(signatureBytes);
        if (!StrUtil.equals(parts[2], expectedSignature)) {
            throw new IngestAuthException(INGEST_AUTH_FAILED.getCode(), "AUTH_JWT_SIGN_INVALID",
                    "JWT 签名校验失败");
        }
        validateJwtClaims(payloadJson, authConfig);
    }

    private void validateJwtClaims(Map<String, Object> claims, Map<String, Object> authConfig) {
        long nowEpochSecond = resolveClock().instant().getEpochSecond();
        long toleranceSeconds = resolveToleranceSeconds(authConfig);
        Long exp = toEpochSecond(claims.get("exp"));
        if (exp != null && nowEpochSecond > exp + toleranceSeconds) {
            throw new IngestAuthException(INGEST_AUTH_FAILED.getCode(), "AUTH_JWT_EXPIRED", "JWT 已过期");
        }
        Long nbf = toEpochSecond(claims.get("nbf"));
        if (nbf != null && nowEpochSecond + toleranceSeconds < nbf) {
            throw new IngestAuthException(INGEST_AUTH_FAILED.getCode(), "AUTH_JWT_NOT_BEFORE", "JWT 尚未生效");
        }
        String expectedIssuer = firstNonBlank(toStringValue(authConfig.get("issuer")),
                toStringValue(authConfig.get("expectedIssuer")));
        if (StrUtil.isNotBlank(expectedIssuer) && !StrUtil.equals(expectedIssuer, toStringValue(claims.get("iss")))) {
            throw new IngestAuthException(INGEST_AUTH_FAILED.getCode(), "AUTH_JWT_ISSUER_INVALID", "JWT issuer 校验失败");
        }
        String expectedAudience = firstNonBlank(toStringValue(authConfig.get("audience")),
                toStringValue(authConfig.get("expectedAudience")));
        if (StrUtil.isNotBlank(expectedAudience) && !containsAudience(claims.get("aud"), expectedAudience)) {
            throw new IngestAuthException(INGEST_AUTH_FAILED.getCode(), "AUTH_JWT_AUDIENCE_INVALID", "JWT audience 校验失败");
        }
        String expectedSubject = firstNonBlank(toStringValue(authConfig.get("requiredSubject")),
                toStringValue(authConfig.get("subject")));
        if (StrUtil.isNotBlank(expectedSubject) && !StrUtil.equals(expectedSubject, toStringValue(claims.get("sub")))) {
            throw new IngestAuthException(INGEST_AUTH_FAILED.getCode(), "AUTH_JWT_SUBJECT_INVALID", "JWT subject 校验失败");
        }
    }

    private Map<String, Object> parseJwtPart(String part, String errorCode, String message) {
        try {
            byte[] bytes = Base64.getUrlDecoder().decode(part);
            return resolveObjectMapper().readValue(bytes, MAP_TYPE);
        } catch (Exception ex) {
            throw new IngestAuthException(INGEST_AUTH_FAILED.getCode(), errorCode, message);
        }
    }

    private String resolveJwtHmacAlgorithm(String jwtAlgorithm) {
        return switch (normalizeJwtAlgorithm(jwtAlgorithm)) {
            case "HS256" -> "HmacSHA256";
            case "HS384" -> "HmacSHA384";
            case "HS512" -> "HmacSHA512";
            default -> throw new IngestAuthException(INGEST_AUTH_FAILED.getCode(),
                    "AUTH_JWT_ALGORITHM_UNSUPPORTED", "JWT 签名算法不支持: " + jwtAlgorithm);
        };
    }

    private String requireHeader(AccessIngestRequestDTO request, String headerName, String errorCode, String messagePrefix) {
        String value = StrUtil.trim(getMetadataValue(request, headerName));
        if (StrUtil.isBlank(value)) {
            throw new IngestAuthException(INGEST_AUTH_FAILED.getCode(), errorCode, messagePrefix + headerName);
        }
        return value;
    }

    private void validateTimestamp(String timestampValue,
                                   Map<String, Object> authConfig,
                                   String invalidErrorCode,
                                   String expiredErrorCode) {
        Instant timestamp = parseTimestamp(timestampValue, invalidErrorCode);
        long toleranceSeconds = resolveToleranceSeconds(authConfig);
        long diffSeconds = Math.abs(Duration.between(timestamp, resolveClock().instant()).getSeconds());
        if (diffSeconds > toleranceSeconds) {
            throw new IngestAuthException(INGEST_AUTH_FAILED.getCode(), expiredErrorCode,
                    "请求时间戳超出允许范围");
        }
    }

    private Map<String, Object> resolveAuthConfig(IngestRuntimeConfig runtimeConfig) {
        if (runtimeConfig == null || runtimeConfig.getSource() == null || runtimeConfig.getSource().getAuthConfigJson() == null) {
            return Map.of();
        }
        return runtimeConfig.getSource().getAuthConfigJson();
    }

    private ObjectMapper resolveObjectMapper() {
        return objectMapper == null ? new ObjectMapper() : objectMapper;
    }

    private Clock resolveClock() {
        return clock == null ? Clock.systemUTC() : clock;
    }

    private Instant parseTimestamp(String timestampValue, String invalidErrorCode) {
        try {
            long epochValue = Long.parseLong(timestampValue);
            if (epochValue > -100_000_000_000L && epochValue < 100_000_000_000L) {
                return Instant.ofEpochSecond(epochValue);
            }
            return Instant.ofEpochMilli(epochValue);
        } catch (NumberFormatException ignored) {
        }
        try {
            return Instant.parse(timestampValue);
        } catch (DateTimeParseException ex) {
            throw new IngestAuthException(INGEST_AUTH_FAILED.getCode(), invalidErrorCode,
                    "时间戳格式不合法: " + timestampValue);
        }
    }

    private long resolveToleranceSeconds(Map<String, Object> authConfig) {
        String toleranceValue = firstNonBlank(
                toStringValue(authConfig.get("expireToleranceSeconds")),
                toStringValue(authConfig.get("timestampToleranceSeconds")),
                toStringValue(authConfig.get("toleranceSeconds"))
        );
        if (StrUtil.isBlank(toleranceValue)) {
            return DEFAULT_HMAC_TOLERANCE_SECONDS;
        }
        try {
            return Math.max(Long.parseLong(StrUtil.trim(toleranceValue)), 0L);
        } catch (NumberFormatException ex) {
            return DEFAULT_HMAC_TOLERANCE_SECONDS;
        }
    }

    private byte[] sign(String algorithm, String secret, String text) {
        try {
            Mac mac = Mac.getInstance(algorithm);
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), algorithm));
            return mac.doFinal(text.getBytes(StandardCharsets.UTF_8));
        } catch (GeneralSecurityException ex) {
            throw new IngestAuthException(INGEST_AUTH_FAILED.getCode(), "AUTH_SIGN_ALGORITHM_UNSUPPORTED",
                    "HMAC 签名算法不支持: " + algorithm);
        }
    }

    private String buildStringToSign(String appKey, String timestampValue, AccessIngestRequestDTO request) {
        String payload = request == null ? null : request.getPayload();
        return appKey + '\n' + timestampValue + '\n' + StrUtil.nullToEmpty(payload);
    }

    private boolean signatureMatches(String actualSignature, byte[] expectedSignatureBytes) {
        String expectedHex = HEX_FORMAT.formatHex(expectedSignatureBytes);
        String expectedBase64 = Base64.getEncoder().encodeToString(expectedSignatureBytes);
        return StrUtil.equalsIgnoreCase(actualSignature, expectedHex) || StrUtil.equals(actualSignature, expectedBase64);
    }

    private String getMetadataValue(AccessIngestRequestDTO request, String key) {
        if (request == null || request.getMetadata() == null) {
            return null;
        }
        String exactValue = request.getMetadata().get(key);
        if (StrUtil.isNotBlank(exactValue)) {
            return exactValue;
        }
        return request.getMetadata().get(key.toLowerCase(Locale.ROOT));
    }

    private boolean containsAudience(Object audienceClaim, String expectedAudience) {
        if (audienceClaim == null || StrUtil.isBlank(expectedAudience)) {
            return false;
        }
        if (audienceClaim instanceof Collection<?> collection) {
            return collection.stream().map(this::toStringValue).anyMatch(expectedAudience::equals);
        }
        String audienceText = toStringValue(audienceClaim);
        if (StrUtil.isBlank(audienceText)) {
            return false;
        }
        for (String value : StrUtil.split(audienceText, ',')) {
            if (StrUtil.equals(StrUtil.trim(value), expectedAudience)) {
                return true;
            }
        }
        return false;
    }

    private Long toEpochSecond(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        String text = StrUtil.trim(toStringValue(value));
        if (StrUtil.isBlank(text)) {
            return null;
        }
        try {
            return Long.parseLong(text);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private String normalizeJwtAlgorithm(String value) {
        return normalize(value);
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (StrUtil.isNotBlank(value)) {
                return value;
            }
        }
        return null;
    }

    private String defaultIfBlank(String value, String defaultValue) {
        return StrUtil.isBlank(value) ? defaultValue : value;
    }

    private String toStringValue(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private String normalize(String value) {
        if (StrUtil.isBlank(value)) {
            return null;
        }
        return StrUtil.trim(value).toUpperCase(Locale.ROOT);
    }

}
