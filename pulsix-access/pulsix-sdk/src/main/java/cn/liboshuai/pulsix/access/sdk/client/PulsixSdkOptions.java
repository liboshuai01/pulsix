package cn.liboshuai.pulsix.access.sdk.client;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;

import java.io.Serializable;

import static cn.liboshuai.pulsix.access.sdk.enums.ErrorCodeConstants.SDK_OPTIONS_INVALID;
import static cn.liboshuai.pulsix.framework.common.exception.util.ServiceExceptionUtil.exception;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PulsixSdkOptions implements Serializable {

    @Builder.Default
    private String host = "127.0.0.1";

    @Builder.Default
    private Integer port = 19100;

    private String sourceCode;

    @Builder.Default
    private Integer connectTimeoutMillis = 3000;

    @Builder.Default
    private Integer requestTimeoutMillis = 5000;

    @Builder.Default
    private Integer maxBatchSize = 100;

    @Builder.Default
    private Integer maxBufferSize = 1000;

    @Builder.Default
    private Integer reconnectIntervalMillis = 3000;

    @Builder.Default
    private Boolean autoReconnect = true;

    public PulsixSdkOptions validate() {
        if (StringUtils.isBlank(host)) {
            throw exception(SDK_OPTIONS_INVALID, "host 不能为空");
        }
        if (port == null || port < 1 || port > 65535) {
            throw exception(SDK_OPTIONS_INVALID, "port 必须在 1 到 65535 之间");
        }
        if (StringUtils.isBlank(sourceCode)) {
            throw exception(SDK_OPTIONS_INVALID, "sourceCode 不能为空");
        }
        if (connectTimeoutMillis == null || connectTimeoutMillis <= 0) {
            throw exception(SDK_OPTIONS_INVALID, "connectTimeoutMillis 必须大于 0");
        }
        if (requestTimeoutMillis == null || requestTimeoutMillis <= 0) {
            throw exception(SDK_OPTIONS_INVALID, "requestTimeoutMillis 必须大于 0");
        }
        if (maxBatchSize == null || maxBatchSize <= 0) {
            throw exception(SDK_OPTIONS_INVALID, "maxBatchSize 必须大于 0");
        }
        if (maxBufferSize == null || maxBufferSize < maxBatchSize) {
            throw exception(SDK_OPTIONS_INVALID, "maxBufferSize 不能小于 maxBatchSize");
        }
        if (reconnectIntervalMillis == null || reconnectIntervalMillis <= 0) {
            throw exception(SDK_OPTIONS_INVALID, "reconnectIntervalMillis 必须大于 0");
        }
        return this;
    }

}
