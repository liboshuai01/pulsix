package cn.liboshuai.pulsix.framework.common.biz.risk.access.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccessIngestResponseDTO implements Serializable {

    private String requestId;

    private String traceId;

    private String eventId;

    private String status;

    private Integer code;

    private String message;

    private String standardTopicName;

    private Long processTimeMillis;

}
