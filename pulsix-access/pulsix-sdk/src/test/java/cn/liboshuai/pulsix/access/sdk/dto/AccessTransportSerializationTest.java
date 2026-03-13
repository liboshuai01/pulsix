package cn.liboshuai.pulsix.access.sdk.dto;

import cn.liboshuai.pulsix.framework.common.biz.risk.access.dto.AccessIngestRequestDTO;
import cn.liboshuai.pulsix.framework.common.biz.risk.access.dto.AccessIngestResponseDTO;
import cn.liboshuai.pulsix.framework.common.biz.risk.access.enums.AccessAckStatusEnum;
import cn.liboshuai.pulsix.framework.common.biz.risk.access.enums.AccessTransportTypeEnum;
import cn.liboshuai.pulsix.framework.common.util.json.JsonUtils;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class AccessTransportSerializationTest {

    @Test
    void shouldSerializeAndDeserializeSharedAccessDtos() {
        AccessIngestRequestDTO request = AccessIngestRequestDTO.builder()
                .requestId("req-1001")
                .sourceCode("trade_sdk_demo")
                .transportType(AccessTransportTypeEnum.SDK.getType())
                .payload("{\"eventId\":\"E1001\"}")
                .metadata(Map.of("traceId", "T1001", "header:authorization", "Bearer test"))
                .sendTimeMillis(1_741_849_600_000L)
                .remoteAddress("127.0.0.1")
                .build();
        AccessIngestResponseDTO response = AccessIngestResponseDTO.builder()
                .requestId("req-1001")
                .traceId("T1001")
                .eventId("E1001")
                .status(AccessAckStatusEnum.ACCEPTED.getStatus())
                .code(0)
                .message("ok")
                .standardTopicName("pulsix.event.standard")
                .processTimeMillis(12L)
                .build();

        String requestJson = JsonUtils.toJsonString(request);
        String responseJson = JsonUtils.toJsonString(response);

        AccessIngestRequestDTO requestAfter = JsonUtils.parseObject(requestJson, AccessIngestRequestDTO.class);
        AccessIngestResponseDTO responseAfter = JsonUtils.parseObject(responseJson, AccessIngestResponseDTO.class);

        assertThat(requestAfter.getTransportType()).isEqualTo(AccessTransportTypeEnum.SDK.getType());
        assertThat(requestAfter.getMetadata()).containsEntry("traceId", "T1001");
        assertThat(responseAfter.getStatus()).isEqualTo(AccessAckStatusEnum.ACCEPTED.getStatus());
        assertThat(responseAfter.getStandardTopicName()).isEqualTo("pulsix.event.standard");
    }

}
