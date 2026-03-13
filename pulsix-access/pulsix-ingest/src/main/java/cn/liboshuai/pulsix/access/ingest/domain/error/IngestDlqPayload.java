package cn.liboshuai.pulsix.access.ingest.domain.error;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IngestDlqPayload {

    private String traceId;

    private String rawEventId;

    private String sourceCode;

    private String sceneCode;

    private String eventCode;

    private String ingestStage;

    private String errorCode;

    private String errorMessage;

    private Object rawPayload;

    private Object standardPayload;

    private String occurTime;

}
