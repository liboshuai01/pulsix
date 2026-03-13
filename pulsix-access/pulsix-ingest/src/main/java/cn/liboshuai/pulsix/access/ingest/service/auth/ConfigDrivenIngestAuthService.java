package cn.liboshuai.pulsix.access.ingest.service.auth;

import cn.hutool.core.util.StrUtil;
import cn.liboshuai.pulsix.access.ingest.domain.config.IngestRuntimeConfig;
import cn.liboshuai.pulsix.framework.common.biz.risk.access.dto.AccessIngestRequestDTO;
import org.springframework.stereotype.Service;

import java.util.Locale;
import java.util.Map;

import static cn.liboshuai.pulsix.access.ingest.enums.ErrorCodeConstants.INGEST_AUTH_FAILED;
import static cn.liboshuai.pulsix.access.ingest.enums.ErrorCodeConstants.INGEST_AUTH_UNSUPPORTED;

@Service
public class ConfigDrivenIngestAuthService implements IngestAuthService {

    private static final String AUTH_NONE = "NONE";
    private static final String AUTH_TOKEN = "TOKEN";

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
        throw new IngestAuthException(INGEST_AUTH_UNSUPPORTED.getCode(), "AUTH_TYPE_UNSUPPORTED",
                "暂不支持的鉴权方式: " + runtimeConfig.getSource().getAuthType());
    }

    private void authenticateToken(AccessIngestRequestDTO request, IngestRuntimeConfig runtimeConfig) {
        Map<String, Object> authConfig = runtimeConfig.getSource().getAuthConfigJson();
        String headerName = defaultIfBlank(toStringValue(authConfig.get("tokenHeader")), "Authorization");
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
