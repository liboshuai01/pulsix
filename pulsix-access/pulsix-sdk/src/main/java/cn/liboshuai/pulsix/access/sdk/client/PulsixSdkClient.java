package cn.liboshuai.pulsix.access.sdk.client;

import cn.liboshuai.pulsix.framework.common.biz.risk.access.dto.AccessIngestRequestDTO;
import cn.liboshuai.pulsix.framework.common.biz.risk.access.dto.AccessIngestResponseDTO;

import java.util.concurrent.CompletableFuture;

public interface PulsixSdkClient extends AutoCloseable {

    void start();

    boolean isStarted();

    CompletableFuture<AccessIngestResponseDTO> sendAsync(AccessIngestRequestDTO request);

    @Override
    void close();

}
