package cn.liboshuai.pulsix.engine.flink;

import cn.liboshuai.pulsix.engine.model.RiskEvent;
import org.apache.flink.streaming.api.functions.ProcessFunction;
import org.apache.flink.util.Collector;

final class RiskEventJsonProcessFunction extends ProcessFunction<String, RiskEvent> {

    @Override
    public void processElement(String value, Context context, Collector<RiskEvent> collector) {
        RiskEvent event;
        try {
            event = RiskEventJsonCodec.read(value);
        } catch (Exception exception) {
            context.output(EngineOutputTags.ENGINE_ERROR, RiskEventJsonCodec.deserializeError(value, exception));
            return;
        }
        try {
            RiskEventJsonCodec.validate(event);
        } catch (Exception exception) {
            context.output(EngineOutputTags.ENGINE_ERROR, RiskEventJsonCodec.validationError(event, exception));
            return;
        }
        collector.collect(event);
    }

}
