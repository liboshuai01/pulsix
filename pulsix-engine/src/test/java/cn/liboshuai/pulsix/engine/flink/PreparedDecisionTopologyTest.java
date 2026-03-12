package cn.liboshuai.pulsix.engine.flink;

import cn.liboshuai.pulsix.engine.demo.DemoFixtures;
import cn.liboshuai.pulsix.engine.feature.InMemoryLookupService;
import cn.liboshuai.pulsix.engine.json.EngineJson;
import cn.liboshuai.pulsix.engine.flink.runtime.SceneReleaseTimeline;
import cn.liboshuai.pulsix.engine.flink.typeinfo.EngineTypeInfos;
import cn.liboshuai.pulsix.engine.model.ActionType;
import cn.liboshuai.pulsix.engine.model.DecisionResult;
import cn.liboshuai.pulsix.engine.model.RiskEvent;
import cn.liboshuai.pulsix.engine.model.SceneSnapshotEnvelope;
import org.apache.flink.api.common.state.MapStateDescriptor;
import org.apache.flink.api.common.typeinfo.Types;
import org.apache.flink.streaming.api.datastream.BroadcastStream;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.datastream.KeyedStream;
import org.apache.flink.streaming.api.datastream.SingleOutputStreamOperator;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PreparedDecisionTopologyTest {

    @Test
    void shouldProduceSameDecisionThroughPreparedTopology() throws Exception {
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        env.setParallelism(1);

        List<RiskEvent> events = new ArrayList<>();
        for (RiskEvent event : DemoFixtures.demoEvents()) {
            events.add(copy(event, RiskEvent.class));
        }
        SceneSnapshotEnvelope envelope = copy(DemoFixtures.demoEnvelope(), SceneSnapshotEnvelope.class);
        DataStream<RiskEvent> eventStream = env.fromCollection(events, EngineTypeInfos.riskEvent());
        DataStream<SceneSnapshotEnvelope> configStream = env.fromCollection(new ArrayList<>(java.util.Collections.singletonList(envelope)), EngineTypeInfos.sceneSnapshotEnvelope());

        MapStateDescriptor<String, SceneReleaseTimeline> snapshotStateDescriptor = new MapStateDescriptor<>(
                "scene-snapshot-broadcast-state",
                Types.STRING,
                EngineTypeInfos.sceneReleaseTimeline()
        );

        KeyedStream<RiskEvent, String> keyedEventStream = eventStream.keyBy(RiskEvent::processingRouteKey);
        BroadcastStream<SceneSnapshotEnvelope> broadcastStream = configStream.broadcast(snapshotStateDescriptor);

        SingleOutputStreamOperator<StreamFeatureRouteEvent> routedFeatureStream = keyedEventStream
                .connect(broadcastStream)
                .process(new StreamFeatureRoutingProcessFunction(snapshotStateDescriptor))
                .returns(EngineTypeInfos.streamFeatureRouteEvent());

        SingleOutputStreamOperator<PreparedStreamFeatureChunk> preparedStreamFeatureStream = routedFeatureStream
                .keyBy(StreamFeatureRouteEvent::getRouteExecutionKey)
                .process(new StreamFeaturePrepareProcessFunction())
                .returns(EngineTypeInfos.preparedStreamFeatureChunk());

        SingleOutputStreamOperator<PreparedDecisionInput> aggregatedPreparedDecisionStream = preparedStreamFeatureStream
                .keyBy(PreparedStreamFeatureChunk::getEventJoinKey)
                .process(new PreparedDecisionAggregateProcessFunction())
                .returns(EngineTypeInfos.preparedDecisionInput());

        DataStream<PreparedDecisionInput> preparedDecisionInputStream = aggregatedPreparedDecisionStream
                .union(routedFeatureStream.getSideOutput(StreamFeatureRoutingProcessFunction.PREPARED_DECISION_BYPASS));

        DataStream<DecisionResult> resultStream = preparedDecisionInputStream
                .process(new PreparedDecisionProcessFunction(InMemoryLookupService::demo))
                .returns(EngineTypeInfos.decisionResult());

        List<DecisionResult> results = new ArrayList<>(resultStream.executeAndCollect(events.size()));

        assertEquals(events.size(), results.size());
        DecisionResult finalResult = results.get(results.size() - 1);
        assertEquals(ActionType.REJECT, finalResult.getFinalAction());
        assertEquals(80, finalResult.getFinalScore());
        assertEquals("3", finalResult.getFeatureSnapshot().get("user_trade_cnt_5m"));
        assertEquals("7180", finalResult.getFeatureSnapshot().get("user_trade_amt_sum_30m"));
        assertEquals("4", finalResult.getFeatureSnapshot().get("device_bind_user_cnt_1h"));
    }

    private <T> T copy(Object value, Class<T> type) {
        return EngineJson.read(EngineJson.write(value), type);
    }


}
