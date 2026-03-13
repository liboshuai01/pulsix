package cn.liboshuai.pulsix.access.ingest.controller;

import cn.liboshuai.pulsix.access.ingest.service.IngestPipelineService;
import cn.liboshuai.pulsix.framework.common.biz.risk.access.dto.AccessIngestRequestDTO;
import cn.liboshuai.pulsix.framework.common.biz.risk.access.dto.AccessIngestResponseDTO;
import cn.liboshuai.pulsix.framework.common.biz.risk.access.enums.AccessAckStatusEnum;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(HttpIngestController.class)
class HttpIngestControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private IngestPipelineService ingestPipelineService;

    @Test
    void shouldBindHttpRequestIntoAccessIngestRequest() throws Exception {
        when(ingestPipelineService.ingest(any())).thenReturn(AccessIngestResponseDTO.builder()
                .requestId("REQ_HTTP_1")
                .traceId("TRACE_HTTP_1")
                .eventId("E_HTTP_1")
                .status(AccessAckStatusEnum.ACCEPTED.getStatus())
                .code(0)
                .message("accepted")
                .standardTopicName("pulsix.event.standard")
                .processTimeMillis(12L)
                .build());

        mockMvc.perform(post("/api/access/ingest/events")
                        .queryParam("sourceCode", "http_none_demo")
                        .queryParam("sceneCode", "TRADE_RISK")
                        .queryParam("eventCode", "TRADE_EVENT")
                        .header("X-Request-Id", "REQ_HTTP_1")
                        .header("Authorization", "Bearer token-demo-001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"event_id\":\"E_HTTP_1\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.requestId").value("REQ_HTTP_1"))
                .andExpect(jsonPath("$.status").value("ACCEPTED"))
                .andExpect(jsonPath("$.standardTopicName").value("pulsix.event.standard"));

        ArgumentCaptor<AccessIngestRequestDTO> captor = ArgumentCaptor.forClass(AccessIngestRequestDTO.class);
        verify(ingestPipelineService).ingest(captor.capture());
        AccessIngestRequestDTO request = captor.getValue();
        assertThat(request.getSourceCode()).isEqualTo("http_none_demo");
        assertThat(request.getTransportType()).isEqualTo("HTTP");
        assertThat(request.getMetadata()).containsEntry("sceneCode", "TRADE_RISK");
        assertThat(request.getMetadata()).containsEntry("eventCode", "TRADE_EVENT");
        assertThat(request.getMetadata()).containsEntry("authorization", "Bearer token-demo-001");
    }

    @Test
    void shouldBindBeaconPlainTextRequestIntoAccessIngestRequest() throws Exception {
        when(ingestPipelineService.ingest(any())).thenReturn(AccessIngestResponseDTO.builder()
                .requestId("REQ_BEACON_1")
                .traceId("TRACE_BEACON_1")
                .eventId("E_BEACON_1")
                .status(AccessAckStatusEnum.ACCEPTED.getStatus())
                .code(0)
                .message("accepted")
                .standardTopicName("pulsix.event.standard")
                .processTimeMillis(8L)
                .build());

        mockMvc.perform(post("/api/access/ingest/beacon")
                        .queryParam("sourceCode", "beacon_none_demo")
                        .queryParam("sceneCode", "TRADE_RISK")
                        .queryParam("eventCode", "TRADE_EVENT")
                        .queryParam("requestId", "REQ_BEACON_1")
                        .contentType(MediaType.TEXT_PLAIN)
                        .content("{\"event_id\":\"E_BEACON_1\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.requestId").value("REQ_BEACON_1"))
                .andExpect(jsonPath("$.status").value("ACCEPTED"));

        ArgumentCaptor<AccessIngestRequestDTO> captor = ArgumentCaptor.forClass(AccessIngestRequestDTO.class);
        verify(ingestPipelineService).ingest(captor.capture());
        AccessIngestRequestDTO request = captor.getValue();
        assertThat(request.getRequestId()).isEqualTo("REQ_BEACON_1");
        assertThat(request.getSourceCode()).isEqualTo("beacon_none_demo");
        assertThat(request.getTransportType()).isEqualTo("BEACON");
        assertThat(request.getPayload()).isEqualTo("{\"event_id\":\"E_BEACON_1\"}");
        assertThat(request.getMetadata()).containsEntry("sceneCode", "TRADE_RISK");
        assertThat(request.getMetadata()).containsEntry("eventCode", "TRADE_EVENT");
    }

    @Test
    void shouldBindBeaconFormPayloadIntoAccessIngestRequest() throws Exception {
        when(ingestPipelineService.ingest(any())).thenReturn(AccessIngestResponseDTO.builder()
                .requestId("REQ_BEACON_2")
                .traceId("TRACE_BEACON_2")
                .eventId("E_BEACON_2")
                .status(AccessAckStatusEnum.ACCEPTED.getStatus())
                .code(0)
                .message("accepted")
                .standardTopicName("pulsix.event.standard")
                .processTimeMillis(9L)
                .build());

        mockMvc.perform(post("/api/access/ingest/beacon")
                        .queryParam("sourceCode", "beacon_none_demo")
                        .queryParam("sceneCode", "TRADE_RISK")
                        .queryParam("eventCode", "TRADE_EVENT")
                        .queryParam("requestId", "REQ_BEACON_2")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("payload", "{\"event_id\":\"E_BEACON_2\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.requestId").value("REQ_BEACON_2"))
                .andExpect(jsonPath("$.status").value("ACCEPTED"));

        ArgumentCaptor<AccessIngestRequestDTO> captor = ArgumentCaptor.forClass(AccessIngestRequestDTO.class);
        verify(ingestPipelineService).ingest(captor.capture());
        AccessIngestRequestDTO request = captor.getValue();
        assertThat(request.getRequestId()).isEqualTo("REQ_BEACON_2");
        assertThat(request.getTransportType()).isEqualTo("BEACON");
        assertThat(request.getPayload()).isEqualTo("{\"event_id\":\"E_BEACON_2\"}");
    }

}
