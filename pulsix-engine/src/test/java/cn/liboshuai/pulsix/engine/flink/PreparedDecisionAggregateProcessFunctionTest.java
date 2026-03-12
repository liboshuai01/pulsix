package cn.liboshuai.pulsix.engine.flink;

import cn.liboshuai.pulsix.engine.model.EngineErrorCodes;
import cn.liboshuai.pulsix.engine.model.EngineErrorRecord;
import cn.liboshuai.pulsix.engine.model.EngineErrorTypes;
import cn.liboshuai.pulsix.engine.model.RiskEvent;
import cn.liboshuai.pulsix.engine.model.SceneSnapshot;
import org.apache.flink.api.common.typeinfo.Types;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.runtime.streamrecord.StreamRecord;
import org.apache.flink.streaming.util.KeyedOneInputStreamOperatorTestHarness;
import org.apache.flink.streaming.util.ProcessFunctionTestHarnesses;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PreparedDecisionAggregateProcessFunctionTest {

    @Test
    void shouldAggregatePreparedChunksIntoSingleDecisionInput() throws Exception {
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        env.setParallelism(1);

        List<PreparedStreamFeatureChunk> chunks = new ArrayList<>();
        chunks.add(chunk("JOIN-001", 2, mapOf("user_trade_cnt_5m", "3")));
        chunks.add(chunk("JOIN-001", 2, mapOf("device_bind_user_cnt_1h", "4")));

        DataStream<PreparedDecisionInput> outputStream = env
                .fromCollection(chunks, cn.liboshuai.pulsix.engine.flink.typeinfo.EngineTypeInfos.preparedStreamFeatureChunk())
                .keyBy(PreparedStreamFeatureChunk::getEventJoinKey)
                .process(new PreparedDecisionAggregateProcessFunction())
                .returns(cn.liboshuai.pulsix.engine.flink.typeinfo.EngineTypeInfos.preparedDecisionInput());

        List<PreparedDecisionInput> outputs = new ArrayList<>(outputStream.executeAndCollect(chunks.size()));
        assertEquals(1, outputs.size());
        PreparedDecisionInput output = outputs.get(0);
        assertEquals("TRADE_RISK", output.getSceneCode());
        assertEquals("JOIN-001", output.getEventJoinKey());
        assertEquals("3", output.getFeatureSnapshot().get("user_trade_cnt_5m"));
        assertEquals("4", output.getFeatureSnapshot().get("device_bind_user_cnt_1h"));
    }

    @Test
    void shouldEmitTimeoutErrorWhenPreparedChunksStayIncomplete() throws Exception {
        try (KeyedOneInputStreamOperatorTestHarness<String, PreparedStreamFeatureChunk, PreparedDecisionInput> harness = newHarness(1_000L)) {
            PreparedStreamFeatureChunk first = chunk("JOIN-TIMEOUT", 2, mapOf("user_trade_cnt_5m", "1"));

            harness.setProcessingTime(0L);
            harness.processElement(first, 0L);
            assertTrue(harness.extractOutputValues().isEmpty());

            harness.setProcessingTime(1_001L);
            Queue<?> errors = sideOutput(harness, EngineOutputTags.ENGINE_ERROR);
            assertEquals(1, errors.size());
            @SuppressWarnings("unchecked")
            EngineErrorRecord record = ((StreamRecord<EngineErrorRecord>) errors.peek()).getValue();
            assertEquals("prepared-decision-aggregate-timeout", record.getStage());
            assertEquals(EngineErrorTypes.STATE, record.getErrorType());
            assertEquals(EngineErrorCodes.PREPARED_DECISION_AGGREGATE_TIMEOUT, record.getErrorCode());
            assertEquals("TRADE_RISK", record.getSceneCode());
            assertTrue(record.getErrorMessage().contains("JOIN-TIMEOUT"));
            assertTrue(harness.extractOutputValues().isEmpty());
        }
    }

    private KeyedOneInputStreamOperatorTestHarness<String, PreparedStreamFeatureChunk, PreparedDecisionInput> newHarness(
            long aggregationTimeoutMs) throws Exception {
        return ProcessFunctionTestHarnesses.forKeyedProcessFunction(
                new PreparedDecisionAggregateProcessFunction(aggregationTimeoutMs),
                PreparedStreamFeatureChunk::getEventJoinKey,
                Types.STRING
        );
    }

    private PreparedStreamFeatureChunk chunk(String eventJoinKey,
                                             int expectedGroupCount,
                                             Map<String, String> featureSnapshot) {
        PreparedStreamFeatureChunk chunk = new PreparedStreamFeatureChunk();
        chunk.setSceneCode("TRADE_RISK");
        chunk.setEventJoinKey(eventJoinKey);
        chunk.setExpectedGroupCount(expectedGroupCount);
        chunk.setPreparedAtEpochMs(10L);
        chunk.setEvent(baseEvent(eventJoinKey));
        chunk.setSnapshot(baseSnapshot());
        chunk.setFeatureSnapshot(featureSnapshot);
        return chunk;
    }

    private RiskEvent baseEvent(String eventJoinKey) {
        RiskEvent event = new RiskEvent();
        event.setSceneCode("TRADE_RISK");
        event.setEventId(eventJoinKey + "-E");
        event.setTraceId(eventJoinKey + "-T");
        event.setEventTime(Instant.parse("2026-03-07T10:00:00Z"));
        event.setEventType("trade");
        event.setUserId("U1001");
        event.setDeviceId("D9101");
        return event;
    }

    private SceneSnapshot baseSnapshot() {
        SceneSnapshot snapshot = new SceneSnapshot();
        snapshot.setSceneCode("TRADE_RISK");
        snapshot.setSnapshotId("TRADE_RISK_v12");
        snapshot.setVersion(12);
        snapshot.setChecksum("checksum-v12");
        snapshot.setPublishedAt(Instant.parse("2026-03-07T09:55:00Z"));
        snapshot.setEffectiveFrom(snapshot.getPublishedAt());
        return snapshot;
    }

    private Map<String, String> mapOf(String key, String value) {
        Map<String, String> map = new LinkedHashMap<>();
        map.put(key, value);
        return map;
    }

    @SuppressWarnings("unchecked")
    private <T> Queue<?> sideOutput(KeyedOneInputStreamOperatorTestHarness<String, PreparedStreamFeatureChunk, PreparedDecisionInput> harness,
                                    org.apache.flink.util.OutputTag<T> outputTag) {
        Queue<?> queue = harness.getSideOutput(outputTag);
        return queue == null ? new java.util.ArrayDeque<>() : queue;
    }

}
