package cn.liboshuai.pulsix.access.ingest.controller;

import cn.hutool.core.util.StrUtil;
import cn.liboshuai.pulsix.access.ingest.service.IngestPipelineService;
import cn.liboshuai.pulsix.framework.common.biz.risk.access.dto.AccessIngestRequestDTO;
import cn.liboshuai.pulsix.framework.common.biz.risk.access.dto.AccessIngestResponseDTO;
import cn.liboshuai.pulsix.framework.common.biz.risk.access.enums.AccessTransportTypeEnum;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@RestController
public class HttpIngestController {

    @Resource
    private IngestPipelineService ingestPipelineService;

    @PostMapping("${pulsix.access.ingest.http.path:/api/access/ingest/events}")
    public AccessIngestResponseDTO ingest(@RequestParam String sourceCode,
                                          @RequestParam String sceneCode,
                                          @RequestParam String eventCode,
                                          @RequestBody String payload,
                                          HttpServletRequest request) {
        Map<String, String> metadata = new LinkedHashMap<>();
        metadata.put("sceneCode", sceneCode);
        metadata.put("eventCode", eventCode);
        Enumeration<String> headerNames = request.getHeaderNames();
        while (headerNames != null && headerNames.hasMoreElements()) {
            String headerName = headerNames.nextElement();
            metadata.put(headerName.toLowerCase(Locale.ROOT), request.getHeader(headerName));
        }

        return ingestPipelineService.ingest(AccessIngestRequestDTO.builder()
                .requestId(resolveRequestId(request))
                .sourceCode(sourceCode)
                .transportType(AccessTransportTypeEnum.HTTP.getType())
                .payload(payload)
                .metadata(metadata)
                .sendTimeMillis(System.currentTimeMillis())
                .remoteAddress(request.getRemoteAddr())
                .build());
    }

    private String resolveRequestId(HttpServletRequest request) {
        String requestId = request.getHeader("X-Request-Id");
        if (StrUtil.isBlank(requestId)) {
            return UUID.randomUUID().toString();
        }
        return StrUtil.trim(requestId);
    }

}
