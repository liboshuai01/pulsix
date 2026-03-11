package cn.liboshuai.pulsix.engine.feature;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
public class LookupResult implements Serializable {

    public static final String ERROR_TIMEOUT = "LOOKUP_TIMEOUT";

    public static final String ERROR_CONNECTION_FAILED = "LOOKUP_CONNECTION_FAILED";

    public static final String ERROR_VALUE_MISSING = "LOOKUP_VALUE_MISSING";

    public static final String ERROR_KEY_MISSING = "LOOKUP_KEY_MISSING";

    public static final String FALLBACK_NONE = "NONE";

    public static final String FALLBACK_DEFAULT_VALUE = "DEFAULT_VALUE";

    public static final String FALLBACK_CACHE_VALUE = "CACHE_VALUE";

    private Object value;

    private String errorCode;

    private String errorMessage;

    private String lookupKey;

    private String fallbackMode = FALLBACK_NONE;

    public static LookupResult success(Object value, String lookupKey) {
        LookupResult result = new LookupResult();
        result.setValue(value);
        result.setLookupKey(lookupKey);
        result.setFallbackMode(FALLBACK_NONE);
        return result;
    }

    public static LookupResult fallback(Object value,
                                        String lookupKey,
                                        String errorCode,
                                        String errorMessage,
                                        String fallbackMode) {
        LookupResult result = new LookupResult();
        result.setValue(value);
        result.setLookupKey(lookupKey);
        result.setErrorCode(errorCode);
        result.setErrorMessage(errorMessage);
        result.setFallbackMode(fallbackMode == null || fallbackMode.isBlank() ? FALLBACK_NONE : fallbackMode);
        return result;
    }

    public boolean hasError() {
        return errorCode != null && !errorCode.isBlank();
    }

}
