package cn.liboshuai.pulsix.access.sdk.client;

import cn.liboshuai.pulsix.access.sdk.callback.PulsixSdkAckCallback;
import cn.liboshuai.pulsix.access.sdk.model.PulsixSdkHealthSnapshot;
import cn.liboshuai.pulsix.access.sdk.model.PulsixSdkMetricsSnapshot;
import cn.liboshuai.pulsix.access.sdk.model.PulsixSdkSendRequest;
import cn.liboshuai.pulsix.framework.common.biz.risk.access.dto.AccessIngestRequestDTO;
import cn.liboshuai.pulsix.framework.common.biz.risk.access.dto.AccessIngestResponseDTO;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public interface PulsixSdkClient extends AutoCloseable {

    void start();

    boolean isStarted();

    PulsixSdkMetricsSnapshot getMetricsSnapshot();

    PulsixSdkHealthSnapshot healthCheck();

    CompletableFuture<AccessIngestResponseDTO> sendAsync(AccessIngestRequestDTO request);

    CompletableFuture<AccessIngestResponseDTO> sendAsync(PulsixSdkSendRequest request);

    default List<CompletableFuture<AccessIngestResponseDTO>> sendBatchAsync(List<PulsixSdkSendRequest> requests) {
        if (requests == null || requests.isEmpty()) {
            return Collections.emptyList();
        }
        List<CompletableFuture<AccessIngestResponseDTO>> futures = new ArrayList<>(requests.size());
        for (PulsixSdkSendRequest request : requests) {
            futures.add(sendAsync(request));
        }
        return futures;
    }

    default List<CompletableFuture<AccessIngestResponseDTO>> sendBatchAsync(List<PulsixSdkSendRequest> requests,
                                                                             PulsixSdkAckCallback callback) {
        if (requests == null || requests.isEmpty()) {
            return Collections.emptyList();
        }
        List<CompletableFuture<AccessIngestResponseDTO>> futures = new ArrayList<>(requests.size());
        for (PulsixSdkSendRequest request : requests) {
            futures.add(sendAsync(request, callback));
        }
        return futures;
    }

    default CompletableFuture<AccessIngestResponseDTO> sendAsync(AccessIngestRequestDTO request,
                                                                 PulsixSdkAckCallback callback) {
        CompletableFuture<AccessIngestResponseDTO> future = sendAsync(request);
        bindCallback(future, callback);
        return future;
    }

    default CompletableFuture<AccessIngestResponseDTO> sendAsync(PulsixSdkSendRequest request,
                                                                 PulsixSdkAckCallback callback) {
        CompletableFuture<AccessIngestResponseDTO> future = sendAsync(request);
        bindCallback(future, callback);
        return future;
    }

    @Override
    void close();

    private static void bindCallback(CompletableFuture<AccessIngestResponseDTO> future,
                                     PulsixSdkAckCallback callback) {
        if (callback == null) {
            return;
        }
        future.whenComplete((response, throwable) -> {
            if (throwable != null) {
                callback.onError(throwable);
                return;
            }
            callback.onAck(response);
        });
    }

}
