package cn.liboshuai.pulsix.engine.model;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.Instant;

@Data
@NoArgsConstructor
public class EngineErrorRecord implements Serializable {

    private String stage;

    private String sceneCode;

    private Integer version;

    private String eventId;

    private String traceId;

    private String errorMessage;

    private Instant occurredAt;

    public static EngineErrorRecord of(String stage, RiskEvent event, Integer version, Throwable throwable) {
        EngineErrorRecord record = new EngineErrorRecord();
        record.setStage(stage);
        record.setSceneCode(event != null ? event.getSceneCode() : null);
        record.setVersion(version);
        record.setEventId(event != null ? event.getEventId() : null);
        record.setTraceId(event != null ? event.getTraceId() : null);
        record.setErrorMessage(throwable != null ? throwable.getMessage() : null);
        record.setOccurredAt(Instant.now());
        return record;
    }

}
