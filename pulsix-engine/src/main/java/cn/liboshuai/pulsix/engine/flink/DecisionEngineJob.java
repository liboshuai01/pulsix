package cn.liboshuai.pulsix.engine.flink;

import cn.liboshuai.pulsix.engine.demo.DemoFixtures;
import cn.liboshuai.pulsix.engine.model.DecisionResult;
import cn.liboshuai.pulsix.engine.model.RiskEvent;
import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.api.common.state.MapStateDescriptor;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.streaming.api.datastream.BroadcastStream;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.datastream.KeyedStream;
import org.apache.flink.streaming.api.datastream.SingleOutputStreamOperator;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.functions.source.SourceFunction;

import java.util.List;

public class DecisionEngineJob {

    public static void main(String[] args) throws Exception {
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        env.setParallelism(1);
        env.getConfig().enableObjectReuse();

        DataStream<Tuple2<String, String>> eventStream = buildDemoEventStream(env);
        DataStream<String> configStream = buildDemoConfigStream(env);

        MapStateDescriptor<String, String> snapshotStateDescriptor = new MapStateDescriptor<>(
                "scene-snapshot-broadcast-state",
                TypeInformation.of(String.class),
                TypeInformation.of(String.class)
        );

        KeyedStream<Tuple2<String, String>, String> keyedEventStream = eventStream.keyBy(value -> value.f0);
        BroadcastStream<String> broadcastStream = configStream.broadcast(snapshotStateDescriptor);
        SingleOutputStreamOperator<DecisionResult> resultStream = keyedEventStream
                .connect(broadcastStream)
                .process(new DecisionBroadcastProcessFunction(snapshotStateDescriptor));

        resultStream.print("decision-result");
        resultStream.getSideOutput(EngineOutputTags.DECISION_LOG).print("decision-log");
        resultStream.getSideOutput(EngineOutputTags.ENGINE_ERROR).print("engine-error");

        env.execute("pulsix-engine-demo-job");
    }

    private static DataStream<Tuple2<String, String>> buildDemoEventStream(StreamExecutionEnvironment env) {
        return env.addSource(new DemoRiskEventSource())
                .assignTimestampsAndWatermarks(WatermarkStrategy.noWatermarks());
    }

    private static DataStream<String> buildDemoConfigStream(StreamExecutionEnvironment env) {
        return env.addSource(new DemoSnapshotSource());
    }

    private static class DemoRiskEventSource implements SourceFunction<Tuple2<String, String>> {

        private volatile boolean running = true;

        @Override
        public void run(SourceContext<Tuple2<String, String>> context) {
            List<RiskEvent> events = DemoFixtures.demoEvents();
            for (RiskEvent event : events) {
                if (!running) {
                    return;
                }
                synchronized (context.getCheckpointLock()) {
                    context.collect(Tuple2.of(event.getSceneCode(), DemoFixtures.toJson(event)));
                }
            }
        }

        @Override
        public void cancel() {
            this.running = false;
        }
    }

    private static class DemoSnapshotSource implements SourceFunction<String> {

        private volatile boolean running = true;

        @Override
        public void run(SourceContext<String> context) {
            if (!running) {
                return;
            }
            synchronized (context.getCheckpointLock()) {
                context.collect(DemoFixtures.demoEnvelopeJson());
            }
        }

        @Override
        public void cancel() {
            this.running = false;
        }
    }

}
