package cn.liboshuai.pulsix.access.ingest.service.auth;

import cn.hutool.core.util.StrUtil;
import cn.liboshuai.pulsix.access.ingest.domain.config.IngestRuntimeConfig;
import cn.liboshuai.pulsix.framework.common.biz.risk.access.dto.AccessIngestRequestDTO;
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
import java.util.HexFormat;
import java.util.Locale;
import java.util.Map;

import static cn.liboshuai.pulsix.access.ingest.enums.ErrorCodeConstants.INGEST_AUTH_FAILED;
import static cn.liboshuai.pulsix.access.ingest.enums.ErrorCodeConstants.INGEST_AUTH_UNSUPPORTED;

@Service
public class ConfigDrivenIngestAuthService implements IngestAuthService {

    private static final String AUTH_NONE = "NONE";
    private static final String AUTH_TOKEN = "TOKEN";
    private static final String AUTH_HMAC = "HMAC";
    private static final String DEFAULT_TOKEN_HEADER = "Authorization";
    private static final String DEFAULT_HMAC_HEADER = "X-Pulsix-Signature";
    private static final String DEFAULT_HMAC_TIMESTAMP_HEADER = "X-Pulsix-Timestamp";
    private static final String DEFAULT_HMAC_ALGORITHM = "HmacSHA256";
    private static final long DEFAULT_HMAC_TOLERANCE_SECONDS = 300L;
    private static final HexFormat HEX_FORMAT = HexFormat.of();

    @Resource
    private Clock clock;

    @Override
    public void authenticate(AccessIngestRequestDTO request, IngestRuntimeConfig runtimeConfig) {
        String authType = normalize(runtimeConfig.getSource().getAuthType());
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
                toStringValue(authConfig.get("secret")),
                appKey
        );

        if (StrUtil.isBlank(appKey) || StrUtil.isBlank(secret)) {
            throw new IngestAuthException(INGEST_AUTH_FAILED.getCode(), "AUTH_SIGN_CONFIG_INVALID",
                    "HMAC 鉴权配置缺少 appKey 或密钥");
        }

        String actualSignature = StrUtil.trim(getMetadataValue(request, signatureHeader));
        if (StrUtil.isBlank(actualSignature)) {
            throw new IngestAuthException(INGEST_AUTH_FAILED.getCode(), "AUTH_SIGN_MISSING",
                    "缺少签名请求头: " + signatureHeader);
        }

        String timestampValue = StrUtil.trim(getMetadataValue(request, timestampHeader));
        if (StrUtil.isBlank(timestampValue)) {
            throw new IngestAuthException(INGEST_AUTH_FAILED.getCode(), "AUTH_TIMESTAMP_MISSING",
                    "缺少时间戳请求头: " + timestampHeader);
        }

        Instant timestamp = parseTimestamp(timestampValue);
        long toleranceSeconds = resolveToleranceSeconds(authConfig);
        long diffSeconds = Math.abs(Duration.between(timestamp, resolveClock().instant()).getSeconds());
        if (diffSeconds > toleranceSeconds) {
            throw new IngestAuthException(INGEST_AUTH_FAILED.getCode(), "AUTH_TIMESTAMP_EXPIRED",
                    "请求时间戳超出允许范围");
        }

        byte[] signatureBytes = sign(algorithm, secret, buildStringToSign(appKey, timestampValue, request));
        String expectedHex = HEX_FORMAT.formatHex(signatureBytes);
        String expectedBase64 = Base64.getEncoder().encodeToString(signatureBytes);
        if (!signatureMatches(actualSignature, expectedHex, expectedBase64)) {
            throw new IngestAuthException(INGEST_AUTH_FAILED.getCode(), "AUTH_SIGN_INVALID",
                    "签名校验失败，拒绝写入标准事件 Topic");
        }
    }

    private Map<String, Object> resolveAuthConfig(IngestRuntimeConfig runtimeConfig) {
        if (runtimeConfig == null || runtimeConfig.getSource() == null || runtimeConfig.getSource().getAuthConfigJson() == null) {
            return Map.of();
        }
        return runtimeConfig.getSource().getAuthConfigJson();
    }

    private Clock resolveClock() {
        return clock == null ? Clock.systemUTC() : clock;
    }

    private Instant parseTimestamp(String timestampValue) {
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
            throw new IngestAuthException(INGEST_AUTH_FAILED.getCode(), "AUTH_TIMESTAMP_INVALID",
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

    private boolean signatureMatches(String actualSignature, String expectedHex, String expectedBase64) {
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
