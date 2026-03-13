package cn.liboshuai.pulsix.access.sdk.callback;

import cn.liboshuai.pulsix.framework.common.biz.risk.access.dto.AccessIngestResponseDTO;

public interface PulsixSdkAckCallback {

    void onAck(AccessIngestResponseDTO response);

    default void onError(Throwable throwable) {
    }

}
