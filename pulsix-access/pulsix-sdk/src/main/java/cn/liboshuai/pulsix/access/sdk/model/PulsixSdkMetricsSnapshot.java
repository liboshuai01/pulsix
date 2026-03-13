package cn.liboshuai.pulsix.access.sdk.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PulsixSdkMetricsSnapshot implements Serializable {

    private String sourceCode;

    private String connectionStatus;

    private Boolean started;

    private Boolean connected;

    private Boolean autoReconnect;

    private Long submittedCount;

    private Long sentCount;

    private Long ackCount;

    private Long failedCount;

    private Long retryCount;

    private Long connectSuccessCount;

    private Long connectFailureCount;

    private Integer bufferSize;

    private Integer inflightSize;

    private Long lastConnectedAtMillis;

    private Long lastDisconnectedAtMillis;

    private Long lastAckAtMillis;

    private Long lastErrorAtMillis;

}
