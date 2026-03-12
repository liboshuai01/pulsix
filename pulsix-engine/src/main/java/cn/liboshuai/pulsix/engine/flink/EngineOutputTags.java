package cn.liboshuai.pulsix.engine.flink;

import cn.liboshuai.pulsix.engine.flink.typeinfo.EngineTypeInfos;
import cn.liboshuai.pulsix.engine.model.DecisionLogRecord;
import cn.liboshuai.pulsix.engine.model.EngineErrorRecord;
import org.apache.flink.util.OutputTag;

public final class EngineOutputTags {

    public static final OutputTag<DecisionLogRecord> DECISION_LOG = new OutputTag<>(
            "decision-log",
            EngineTypeInfos.decisionLogRecord()
    );

    public static final OutputTag<EngineErrorRecord> ENGINE_ERROR = new OutputTag<>(
            "engine-error",
            EngineTypeInfos.engineErrorRecord()
    );

    private EngineOutputTags() {
    }

}
