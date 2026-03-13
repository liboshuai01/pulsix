package cn.liboshuai.pulsix.access.ingest.service.ratelimit;

import lombok.Getter;

@Getter
public class IngestRateLimitException extends RuntimeException {

    private final Integer code;

    private final String errorCode;

    public IngestRateLimitException(Integer code, String errorCode, String message) {
        super(message);
        this.code = code;
        this.errorCode = errorCode;
    }

}
