package cn.liboshuai.pulsix.access.sdk.buffer;

import cn.liboshuai.pulsix.framework.common.biz.risk.access.dto.AccessIngestRequestDTO;
import cn.liboshuai.pulsix.framework.common.biz.risk.access.dto.AccessIngestResponseDTO;
import lombok.Getter;

import java.util.concurrent.CompletableFuture;

@Getter
public class BufferedSdkRequest {

    private final AccessIngestRequestDTO request;
    private final CompletableFuture<AccessIngestResponseDTO> future = new CompletableFuture<>();
    private volatile int attemptCount;
    private volatile long lastSendTimeMillis;

    public BufferedSdkRequest(AccessIngestRequestDTO request) {
        this.request = request;
    }

    public String getRequestId() {
        return request.getRequestId();
    }

    public void markSent(long now) {
        this.attemptCount++;
        this.lastSendTimeMillis = now;
    }

    public boolean canRetry(int maxRetryCount) {
        return attemptCount <= maxRetryCount;
    }

}
