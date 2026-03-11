package cn.liboshuai.pulsix.engine.flink;

import cn.liboshuai.pulsix.engine.feature.AbstractStreamFeatureStateStore;
import cn.liboshuai.pulsix.engine.flink.typeinfo.EngineTypeInfos;
import cn.liboshuai.pulsix.engine.model.DecisionLogRecord;
import cn.liboshuai.pulsix.engine.model.DecisionResult;
import cn.liboshuai.pulsix.engine.model.RiskEvent;
import cn.liboshuai.pulsix.engine.model.SceneSnapshotEnvelope;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.api.common.typeutils.CompositeType;
import org.apache.flink.api.java.typeutils.GenericTypeInfo;
import org.apache.flink.api.java.typeutils.ListTypeInfo;
import org.apache.flink.api.java.typeutils.MapTypeInfo;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;

class FlinkTypeInfoRegressionTest {

    @Test
    void shouldNotFallbackToGenericTypeForCoreFlinkModels() {
        assertNoGenericFallback(EngineTypeInfos.riskEvent());
        assertNoGenericFallback(EngineTypeInfos.sceneSnapshotEnvelope());
        assertNoGenericFallback(EngineTypeInfos.decisionResult());
        assertNoGenericFallback(EngineTypeInfos.decisionLogRecord());
        assertNoGenericFallback(TypeInformation.of(AbstractStreamFeatureStateStore.NumericWindowState.class));
        assertNoGenericFallback(TypeInformation.of(AbstractStreamFeatureStateStore.LatestValueState.class));
        assertNoGenericFallback(TypeInformation.of(AbstractStreamFeatureStateStore.DistinctWindowState.class));
    }

    private void assertNoGenericFallback(TypeInformation<?> typeInformation) {
        assertFalse(typeInformation instanceof GenericTypeInfo<?>, () -> "unexpected generic type: " + typeInformation);
        if (typeInformation instanceof CompositeType<?> compositeType) {
            for (int index = 0; index < compositeType.getArity(); index++) {
                assertNoGenericFallback(compositeType.getTypeAt(index));
            }
            return;
        }
        if (typeInformation instanceof ListTypeInfo<?> listTypeInfo) {
            assertNoGenericFallback(listTypeInfo.getElementTypeInfo());
            return;
        }
        if (typeInformation instanceof MapTypeInfo<?, ?> mapTypeInfo) {
            assertNoGenericFallback(mapTypeInfo.getKeyTypeInfo());
            assertNoGenericFallback(mapTypeInfo.getValueTypeInfo());
        }
    }

}
