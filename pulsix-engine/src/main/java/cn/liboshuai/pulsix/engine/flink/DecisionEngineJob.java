package cn.liboshuai.pulsix.engine.flink;

import cn.liboshuai.pulsix.engine.demo.DemoFixtures;
import cn.liboshuai.pulsix.engine.model.DecisionResult;
import cn.liboshuai.pulsix.engine.model.RiskEvent;
import cn.liboshuai.pulsix.engine.model.SceneSnapshot;
import cn.liboshuai.pulsix.engine.model.SceneSnapshotEnvelope;
import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.api.common.state.MapStateDescriptor;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.streaming.api.datastream.BroadcastStream;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.datastream.KeyedStream;
import org.apache.flink.streaming.api.datastream.SingleOutputStreamOperator;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;

import java.util.ArrayList;
import java.util.List;

public class DecisionEngineJob {

    public static void main(String[] args) throws Exception {
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        env.setParallelism(1);

        DataStream<RiskEvent> eventStream = buildDemoEventStream(env);
        DataStream<SceneSnapshotEnvelope> configStream = buildDemoConfigStream(env);

        MapStateDescriptor<String, SceneSnapshot> snapshotStateDescriptor = new MapStateDescriptor<>(
                "scene-snapshot-broadcast-state",
                TypeInformation.of(String.class),
                TypeInformation.of(SceneSnapshot.class)
        );

        KeyedStream<RiskEvent, String> keyedEventStream = eventStream.keyBy(RiskEvent::routeKey);
        BroadcastStream<SceneSnapshotEnvelope> broadcastStream = configStream.broadcast(snapshotStateDescriptor);
        SingleOutputStreamOperator<DecisionResult> resultStream = keyedEventStream
                .connect(broadcastStream)
                .process(new DecisionBroadcastProcessFunction(snapshotStateDescriptor));

        resultStream.print("decision-result");
        resultStream.getSideOutput(EngineOutputTags.DECISION_LOG).print("decision-log");
        resultStream.getSideOutput(EngineOutputTags.ENGINE_ERROR).print("engine-error");

        env.execute("pulsix-engine-demo-job");
    }

    private static DataStream<RiskEvent> buildDemoEventStream(StreamExecutionEnvironment env) {
        List<RiskEvent> events = new ArrayList<>(DemoFixtures.demoEvents());
        return env.fromCollection(events)
                .assignTimestampsAndWatermarks(WatermarkStrategy.noWatermarks());
    }

    private static DataStream<SceneSnapshotEnvelope> buildDemoConfigStream(StreamExecutionEnvironment env) {
        List<SceneSnapshotEnvelope> snapshots = new ArrayList<>();
        snapshots.add(DemoFixtures.demoEnvelope());
        return env.fromCollection(snapshots);
    }

}
