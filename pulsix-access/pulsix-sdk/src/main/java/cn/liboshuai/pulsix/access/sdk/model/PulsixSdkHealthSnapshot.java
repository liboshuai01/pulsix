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
public class PulsixSdkHealthSnapshot implements Serializable {

    private String status;

    private String sourceCode;

    private String connectionStatus;

    private Boolean started;

    private Boolean connected;

    private Boolean autoReconnect;

    private Integer bufferSize;

    private Integer inflightSize;

    private Long lastConnectedAtMillis;

    private Long lastAckAtMillis;

}
