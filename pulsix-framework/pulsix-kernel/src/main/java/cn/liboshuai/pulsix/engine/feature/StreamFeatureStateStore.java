package cn.liboshuai.pulsix.engine.feature;

import cn.liboshuai.pulsix.engine.context.EvalContext;
import cn.liboshuai.pulsix.engine.runtime.CompiledSceneRuntime;

public interface StreamFeatureStateStore {

    Object evaluate(CompiledSceneRuntime.CompiledStreamFeature feature, EvalContext context);

    default void bindExecutionContext(StreamFeatureExecutionContext executionContext) {
    }

    default void clearExecutionContext() {
    }

    default void onTimer(long timestamp) {
    }

    interface StreamFeatureExecutionContext {

        void registerEventTimeTimer(long timestamp);

        long currentWatermark();

    }

}
