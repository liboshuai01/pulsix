package cn.liboshuai.pulsix.engine.model;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.Instant;

@Data
@NoArgsConstructor
public class EngineErrorRecord implements Serializable {

    private String stage;

    private String errorType;

    private String sceneCode;

    private Integer version;

    private String snapshotId;

    private String snapshotChecksum;

    private String eventId;

    private String traceId;

    private String errorCode;

    private String errorMessage;

    private String exceptionClass;

    private String featureCode;

    private String ruleCode;

    private String engineType;

    private String lookupType;

    private String sourceRef;

    private String lookupKey;

    private String fallbackMode;

    private Instant occurredAt;

    public static EngineErrorRecord of(String stage, RiskEvent event, Integer version, Throwable throwable) {
        return of(stage, null, null, event, version, throwable);
    }

    public static EngineErrorRecord of(String stage,
                                       String errorType,
                                       String errorCode,
                                       RiskEvent event,
                                       Integer version,
                                       Throwable throwable) {
        EngineErrorRecord record = new EngineErrorRecord();
        record.setStage(stage);
        record.setErrorType(errorType);
        record.setSceneCode(event != null ? event.getSceneCode() : null);
        record.setVersion(version);
        record.setEventId(event != null ? event.getEventId() : null);
        record.setTraceId(event != null ? event.getTraceId() : null);
        record.setErrorCode(errorCode);
        record.setErrorMessage(throwable != null ? throwable.getMessage() : null);
        record.setExceptionClass(throwable != null ? throwable.getClass().getName() : null);
        record.setOccurredAt(Instant.now());
        return record;
    }

    public boolean hasFallback() {
        return fallbackMode != null && !fallbackMode.isBlank() && !"NONE".equals(fallbackMode);
    }

}
