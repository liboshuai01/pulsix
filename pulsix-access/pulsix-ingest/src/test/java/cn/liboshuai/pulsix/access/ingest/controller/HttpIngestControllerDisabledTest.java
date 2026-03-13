package cn.liboshuai.pulsix.access.ingest.controller;

import cn.liboshuai.pulsix.access.ingest.service.IngestPipelineService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(value = HttpIngestController.class, properties = "pulsix.access.ingest.http.enabled=false")
class HttpIngestControllerDisabledTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private IngestPipelineService ingestPipelineService;

    @Test
    void shouldRejectWhenHttpIngressDisabled() throws Exception {
        mockMvc.perform(post("/api/access/ingest/events")
                        .queryParam("sourceCode", "http_none_demo")
                        .queryParam("sceneCode", "TRADE_RISK")
                        .queryParam("eventCode", "TRADE_EVENT")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"event_id\":\"E_HTTP_1\"}"))
                .andExpect(status().isNotFound());

        verifyNoInteractions(ingestPipelineService);
    }

}
