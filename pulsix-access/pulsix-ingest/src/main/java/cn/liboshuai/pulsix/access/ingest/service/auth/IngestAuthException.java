package cn.liboshuai.pulsix.access.ingest.service.auth;

import lombok.Getter;

@Getter
public class IngestAuthException extends RuntimeException {

    private final Integer code;

    private final String errorCode;

    public IngestAuthException(Integer code, String errorCode, String message) {
        super(message);
        this.code = code;
        this.errorCode = errorCode;
    }

}
