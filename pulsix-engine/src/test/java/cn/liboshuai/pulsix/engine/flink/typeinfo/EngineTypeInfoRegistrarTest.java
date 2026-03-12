package cn.liboshuai.pulsix.engine.flink.typeinfo;

import cn.liboshuai.pulsix.engine.feature.AbstractStreamFeatureStateStore;
import cn.liboshuai.pulsix.engine.flink.PreparedDecisionInput;
import cn.liboshuai.pulsix.engine.flink.StreamFeatureRouteEvent;
import cn.liboshuai.pulsix.engine.model.DecisionResult;
import cn.liboshuai.pulsix.engine.model.RiskEvent;
import cn.liboshuai.pulsix.engine.model.SceneSnapshot;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.api.common.typeinfo.Types;
import org.apache.flink.api.common.typeutils.CompositeType;
import org.apache.flink.api.java.typeutils.TypeExtractor;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

class EngineTypeInfoRegistrarTest {

    @Test
    void shouldRegisterKernelFactoriesBeforeTypeExtraction() {
        EngineTypeInfoRegistrar.registerAll();

        CompositeType<RiskEvent> riskEventType = composite(TypeExtractor.createTypeInfo(RiskEvent.class));
        assertEquals(Types.MAP(Types.STRING, Types.STRING), riskEventType.getTypeAt("ext"));

        CompositeType<SceneSnapshot> sceneSnapshotType = composite(TypeExtractor.createTypeInfo(SceneSnapshot.class));
        assertEquals(Types.MAP(Types.STRING, Types.LIST(Types.STRING)), sceneSnapshotType.getTypeAt("variables"));

        CompositeType<DecisionResult> decisionResultType = composite(TypeExtractor.createTypeInfo(DecisionResult.class));
        assertEquals(Types.LIST(Types.STRING), decisionResultType.getTypeAt("traceLogs"));
    }

    @Test
    void shouldRegisterEngineFactoriesBeforeTypeExtraction() {
        EngineTypeInfoRegistrar.registerAll();

        CompositeType<StreamFeatureRouteEvent> routeEventType = composite(TypeExtractor.createTypeInfo(StreamFeatureRouteEvent.class));
        assertEquals(Types.LIST(Types.STRING), routeEventType.getTypeAt("featureCodes"));

        CompositeType<PreparedDecisionInput> preparedDecisionType = composite(TypeExtractor.createTypeInfo(PreparedDecisionInput.class));
        assertEquals(Types.MAP(Types.STRING, Types.STRING), preparedDecisionType.getTypeAt("featureSnapshot"));

        CompositeType<AbstractStreamFeatureStateStore.NumericWindowState> numericWindowStateType =
                composite(TypeExtractor.createTypeInfo(AbstractStreamFeatureStateStore.NumericWindowState.class));
        assertEquals(Types.MAP(Types.LONG, Types.BIG_DEC), numericWindowStateType.getTypeAt("buckets"));
    }

    @SuppressWarnings("unchecked")
    private <T> CompositeType<T> composite(TypeInformation<T> typeInformation) {
        return assertInstanceOf(CompositeType.class, typeInformation);
    }

}
