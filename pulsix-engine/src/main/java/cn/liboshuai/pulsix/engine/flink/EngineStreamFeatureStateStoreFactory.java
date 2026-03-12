package cn.liboshuai.pulsix.engine.flink;

import cn.liboshuai.pulsix.engine.feature.StreamFeatureStateStore;
import org.apache.flink.api.common.functions.RuntimeContext;

import java.io.Serializable;

@FunctionalInterface
public interface EngineStreamFeatureStateStoreFactory extends Serializable {

    StreamFeatureStateStore create(RuntimeContext runtimeContext);

}
