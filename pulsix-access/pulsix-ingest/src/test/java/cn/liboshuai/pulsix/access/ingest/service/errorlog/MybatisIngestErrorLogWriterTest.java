package cn.liboshuai.pulsix.access.ingest.service.errorlog;

import cn.liboshuai.pulsix.access.ingest.dal.dataobject.ingesterror.IngestErrorLogDO;
import cn.liboshuai.pulsix.access.ingest.dal.mysql.ingesterror.IngestErrorLogMapper;
import cn.liboshuai.pulsix.access.ingest.domain.error.IngestErrorEvent;
import cn.liboshuai.pulsix.access.ingest.enums.IngestStageEnum;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class MybatisIngestErrorLogWriterTest {

    private IngestErrorLogMapper ingestErrorLogMapper;
    private MybatisIngestErrorLogWriter writer;

    @BeforeEach
    void setUp() {
        ingestErrorLogMapper = mock(IngestErrorLogMapper.class);
        writer = new MybatisIngestErrorLogWriter();
        ReflectionTestUtils.setField(writer, "ingestErrorLogMapper", ingestErrorLogMapper);
        ReflectionTestUtils.setField(writer, "clock",
                Clock.fixed(Instant.parse("2026-03-13T02:31:00Z"), ZoneId.of("Asia/Shanghai")));
    }

    @Test
    void shouldInsertErrorLogForValidatedEvent() {
        IngestErrorEvent errorEvent = IngestErrorEvent.builder()
                .traceId("TRACE-S18-8101")
                .sourceCode("trade_http_demo")
                .sceneCode("TRADE_RISK")
                .eventCode("TRADE_EVENT")
                .rawEventId("raw_trade_bad_8101")
                .ingestStage(IngestStageEnum.VALIDATE)
                .errorCode("REQUIRED_FIELD_MISSING")
                .errorMessage("标准事件必填字段缺失: eventTime,userId")
                .rawPayloadJson(Map.of("event_id", "raw_trade_bad_8101"))
                .standardPayloadJson(Map.of("eventId", "raw_trade_bad_8101"))
                .errorTopicName("pulsix.event.dlq")
                .occurTime("2026-03-13T10:31:00")
                .build();

        writer.write(errorEvent, IngestErrorLogWriter.REPROCESS_STATUS_PENDING);

        ArgumentCaptor<IngestErrorLogDO> captor = ArgumentCaptor.forClass(IngestErrorLogDO.class);
        verify(ingestErrorLogMapper).insert(captor.capture());
        IngestErrorLogDO logDO = captor.getValue();
        assertThat(logDO.getTraceId()).isEqualTo("TRACE-S18-8101");
        assertThat(logDO.getSourceCode()).isEqualTo("trade_http_demo");
        assertThat(logDO.getSceneCode()).isEqualTo("TRADE_RISK");
        assertThat(logDO.getEventCode()).isEqualTo("TRADE_EVENT");
        assertThat(logDO.getRawEventId()).isEqualTo("raw_trade_bad_8101");
        assertThat(logDO.getIngestStage()).isEqualTo("VALIDATE");
        assertThat(logDO.getErrorCode()).isEqualTo("REQUIRED_FIELD_MISSING");
        assertThat(logDO.getErrorTopicName()).isEqualTo("pulsix.event.dlq");
        assertThat(logDO.getReprocessStatus()).isEqualTo("PENDING");
        assertThat(logDO.getOccurTime()).isEqualTo(LocalDateTime.of(2026, 3, 13, 10, 31, 0));
        assertThat(logDO.getStatus()).isEqualTo(1);
        assertThat(logDO.getRawPayloadJson()).isEqualTo(Map.of("event_id", "raw_trade_bad_8101"));
        assertThat(logDO.getStandardPayloadJson()).isEqualTo(Map.of("eventId", "raw_trade_bad_8101"));
    }

    @Test
    void shouldFallbackWhenDlqSendFailedOrOccurTimeMissing() {
        IngestErrorEvent errorEvent = IngestErrorEvent.builder()
                .sourceCode("trade_http_demo")
                .ingestStage(IngestStageEnum.AUTH)
                .errorCode("AUTH_SIGN_INVALID")
                .errorMessage("签名校验失败，拒绝写入标准事件 Topic")
                .rawPayloadJson("{\"event_id\":\"raw_trade_bad_8103\"}")
                .errorTopicName("pulsix.event.dlq")
                .build();

        writer.write(errorEvent, IngestErrorLogWriter.REPROCESS_STATUS_RETRY_FAILED);

        ArgumentCaptor<IngestErrorLogDO> captor = ArgumentCaptor.forClass(IngestErrorLogDO.class);
        verify(ingestErrorLogMapper).insert(captor.capture());
        IngestErrorLogDO logDO = captor.getValue();
        assertThat(logDO.getTraceId()).isEqualTo("");
        assertThat(logDO.getSourceCode()).isEqualTo("trade_http_demo");
        assertThat(logDO.getSceneCode()).isNull();
        assertThat(logDO.getIngestStage()).isEqualTo("AUTH");
        assertThat(logDO.getReprocessStatus()).isEqualTo("RETRY_FAILED");
        assertThat(logDO.getOccurTime()).isEqualTo(LocalDateTime.of(2026, 3, 13, 10, 31, 0));
        assertThat(logDO.getRawPayloadJson()).isEqualTo("{\"event_id\":\"raw_trade_bad_8103\"}");
    }

}
