package cn.liboshuai.pulsix.access.ingest.controller;

import cn.hutool.core.util.StrUtil;
import cn.liboshuai.pulsix.access.ingest.service.IngestPipelineService;
import cn.liboshuai.pulsix.framework.common.biz.risk.access.dto.AccessIngestRequestDTO;
import cn.liboshuai.pulsix.framework.common.biz.risk.access.dto.AccessIngestResponseDTO;
import cn.liboshuai.pulsix.framework.common.biz.risk.access.enums.AccessTransportTypeEnum;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.util.StreamUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@RestController
public class HttpIngestController {

    @Resource
    private IngestPipelineService ingestPipelineService;

    @Value("${pulsix.access.ingest.http.enabled:true}")
    private boolean httpEnabled;

    @Value("${pulsix.access.ingest.http.max-payload-bytes:262144}")
    private int maxPayloadBytes;

    @PostMapping("${pulsix.access.ingest.http.path:/api/access/ingest/events}")
    public AccessIngestResponseDTO ingest(@RequestParam String sourceCode,
                                          @RequestParam String sceneCode,
                                          @RequestParam String eventCode,
                                          @RequestBody String payload,
                                          HttpServletRequest request) {
        return dispatch(sourceCode, sceneCode, eventCode, payload, AccessTransportTypeEnum.HTTP, request);
    }

    @PostMapping("${pulsix.access.ingest.http.beacon-path:/api/access/ingest/beacon}")
    public AccessIngestResponseDTO ingestBeacon(@RequestParam String sourceCode,
                                                @RequestParam String sceneCode,
                                                @RequestParam String eventCode,
                                                HttpServletRequest request) {
        return dispatch(sourceCode, sceneCode, eventCode, resolveBeaconPayload(request),
                AccessTransportTypeEnum.BEACON, request);
    }

    private AccessIngestResponseDTO dispatch(String sourceCode,
                                             String sceneCode,
                                             String eventCode,
                                             String payload,
                                             AccessTransportTypeEnum transportType,
                                             HttpServletRequest request) {
        ensureHttpEnabled();
        validatePayloadSize(payload, transportType);
        return ingestPipelineService.ingest(AccessIngestRequestDTO.builder()
                .requestId(resolveRequestId(request))
                .sourceCode(sourceCode)
                .transportType(transportType.getType())
                .payload(payload)
                .metadata(resolveMetadata(sceneCode, eventCode, request))
                .sendTimeMillis(System.currentTimeMillis())
                .remoteAddress(request.getRemoteAddr())
                .build());
    }

    private void ensureHttpEnabled() {
        if (!httpEnabled) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "HTTP 接入已关闭");
        }
    }

    private void validatePayloadSize(String payload, AccessTransportTypeEnum transportType) {
        int payloadBytes = StrUtil.nullToEmpty(payload).getBytes(StandardCharsets.UTF_8).length;
        if (payloadBytes > maxPayloadBytes) {
            throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE,
                    transportType.getType() + " 请求报文超过最大限制: " + payloadBytes + " > " + maxPayloadBytes);
        }
    }

    private Map<String, String> resolveMetadata(String sceneCode, String eventCode, HttpServletRequest request) {
        Map<String, String> metadata = new LinkedHashMap<>();
        metadata.put("sceneCode", sceneCode);
        metadata.put("eventCode", eventCode);
        Enumeration<String> headerNames = request.getHeaderNames();
        while (headerNames != null && headerNames.hasMoreElements()) {
            String headerName = headerNames.nextElement();
            metadata.put(headerName.toLowerCase(Locale.ROOT), request.getHeader(headerName));
        }
        return metadata;
    }

    private String resolveBeaconPayload(HttpServletRequest request) {
        String payload = firstNonBlank(
                request.getParameter("payload"),
                request.getParameter("data"),
                request.getParameter("event")
        );
        if (StrUtil.isNotBlank(payload)) {
            return payload;
        }
        try {
            return StreamUtils.copyToString(request.getInputStream(), StandardCharsets.UTF_8);
        } catch (IOException ex) {
            throw new IllegalStateException("读取 Beacon 请求体失败", ex);
        }
    }

    private String resolveRequestId(HttpServletRequest request) {
        String requestId = firstNonBlank(
                request.getHeader("X-Request-Id"),
                request.getParameter("requestId"),
                request.getParameter("request_id")
        );
        if (StrUtil.isBlank(requestId)) {
            return UUID.randomUUID().toString();
        }
        return StrUtil.trim(requestId);
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (StrUtil.isNotBlank(value)) {
                return StrUtil.trim(value);
            }
        }
        return null;
    }

}
