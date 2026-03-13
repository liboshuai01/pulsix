package cn.liboshuai.pulsix.framework.common.biz.risk.access.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccessIngestRequestDTO implements Serializable {

    private String requestId;

    private String sourceCode;

    private String transportType;

    private String payload;

    @Builder.Default
    private Map<String, String> metadata = new LinkedHashMap<>();

    private Long sendTimeMillis;

    private String remoteAddress;

}
