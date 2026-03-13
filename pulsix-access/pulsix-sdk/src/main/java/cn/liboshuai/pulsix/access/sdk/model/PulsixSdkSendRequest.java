package cn.liboshuai.pulsix.access.sdk.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import static cn.liboshuai.pulsix.access.sdk.enums.ErrorCodeConstants.SDK_REQUEST_INVALID;
import static cn.liboshuai.pulsix.framework.common.exception.util.ServiceExceptionUtil.exception;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PulsixSdkSendRequest implements Serializable {

    private String requestId;

    private String sceneCode;

    private String eventCode;

    private String payload;

    @Builder.Default
    private Map<String, String> metadata = new LinkedHashMap<>();

    public PulsixSdkSendRequest validate() {
        if (StringUtils.isBlank(sceneCode)) {
            throw exception(SDK_REQUEST_INVALID, "sceneCode 不能为空");
        }
        if (StringUtils.isBlank(eventCode)) {
            throw exception(SDK_REQUEST_INVALID, "eventCode 不能为空");
        }
        if (StringUtils.isBlank(payload)) {
            throw exception(SDK_REQUEST_INVALID, "payload 不能为空");
        }
        if (metadata == null) {
            metadata = new LinkedHashMap<>();
        }
        return this;
    }

}
