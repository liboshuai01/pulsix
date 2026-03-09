package cn.liboshuai.pulsix.engine.flink;

import cn.liboshuai.pulsix.engine.model.DecisionLogRecord;
import cn.liboshuai.pulsix.engine.model.EngineErrorRecord;
import org.apache.flink.util.OutputTag;

public final class EngineOutputTags {

    public static final OutputTag<DecisionLogRecord> DECISION_LOG = new OutputTag<>("decision-log") {
    };

    public static final OutputTag<EngineErrorRecord> ENGINE_ERROR = new OutputTag<>("engine-error") {
    };

    private EngineOutputTags() {
    }

}
