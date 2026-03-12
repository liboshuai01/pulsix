package cn.liboshuai.pulsix.engine.flink.typeinfo;

import cn.liboshuai.pulsix.engine.feature.AbstractStreamFeatureStateStore;
import cn.liboshuai.pulsix.engine.flink.PreparedDecisionInput;
import cn.liboshuai.pulsix.engine.flink.PreparedStreamFeatureChunk;
import cn.liboshuai.pulsix.engine.flink.StreamFeatureRouteEvent;
import cn.liboshuai.pulsix.engine.flink.runtime.SceneReleaseTimeline;
import org.apache.flink.api.common.typeinfo.TypeInfoFactory;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.api.common.typeinfo.Types;

import java.lang.reflect.Type;
import java.util.LinkedHashMap;
import java.util.Map;

public final class EngineTypeInfoFactories {

    private EngineTypeInfoFactories() {
    }

    public static final class SceneReleaseTimelineTypeInfoFactory extends TypeInfoFactory<SceneReleaseTimeline> {

        @Override
        public TypeInformation<SceneReleaseTimeline> createTypeInfo(Type type, Map<String, TypeInformation<?>> genericParameters) {
            Map<String, TypeInformation<?>> fields = new LinkedHashMap<>();
            fields.put("sceneCode", Types.STRING);
            fields.put("releases", Types.LIST(EngineTypeInfos.sceneSnapshotEnvelope()));
            return Types.POJO(SceneReleaseTimeline.class, fields);
        }

    }

    public static final class StreamFeatureRouteEventTypeInfoFactory extends TypeInfoFactory<StreamFeatureRouteEvent> {

        @Override
        public TypeInformation<StreamFeatureRouteEvent> createTypeInfo(Type type,
                                                                       Map<String, TypeInformation<?>> genericParameters) {
            Map<String, TypeInformation<?>> fields = new LinkedHashMap<>();
            fields.put("sceneCode", Types.STRING);
            fields.put("groupKey", Types.STRING);
            fields.put("routeExecutionKey", Types.STRING);
            fields.put("eventJoinKey", Types.STRING);
            fields.put("expectedGroupCount", Types.INT);
            fields.put("preparedAtEpochMs", Types.LONG);
            fields.put("event", EngineTypeInfos.riskEvent());
            fields.put("snapshot", EngineTypeInfos.sceneSnapshot());
            fields.put("featureCodes", Types.LIST(Types.STRING));
            return Types.POJO(StreamFeatureRouteEvent.class, fields);
        }

    }

    public static final class PreparedStreamFeatureChunkTypeInfoFactory extends TypeInfoFactory<PreparedStreamFeatureChunk> {

        @Override
        public TypeInformation<PreparedStreamFeatureChunk> createTypeInfo(Type type,
                                                                          Map<String, TypeInformation<?>> genericParameters) {
            Map<String, TypeInformation<?>> fields = new LinkedHashMap<>();
            fields.put("sceneCode", Types.STRING);
            fields.put("eventJoinKey", Types.STRING);
            fields.put("expectedGroupCount", Types.INT);
            fields.put("preparedAtEpochMs", Types.LONG);
            fields.put("event", EngineTypeInfos.riskEvent());
            fields.put("snapshot", EngineTypeInfos.sceneSnapshot());
            fields.put("featureSnapshot", Types.MAP(Types.STRING, Types.STRING));
            return Types.POJO(PreparedStreamFeatureChunk.class, fields);
        }

    }

    public static final class PreparedDecisionInputTypeInfoFactory extends TypeInfoFactory<PreparedDecisionInput> {

        @Override
        public TypeInformation<PreparedDecisionInput> createTypeInfo(Type type,
                                                                     Map<String, TypeInformation<?>> genericParameters) {
            Map<String, TypeInformation<?>> fields = new LinkedHashMap<>();
            fields.put("sceneCode", Types.STRING);
            fields.put("eventJoinKey", Types.STRING);
            fields.put("preparedAtEpochMs", Types.LONG);
            fields.put("event", EngineTypeInfos.riskEvent());
            fields.put("snapshot", EngineTypeInfos.sceneSnapshot());
            fields.put("featureSnapshot", Types.MAP(Types.STRING, Types.STRING));
            return Types.POJO(PreparedDecisionInput.class, fields);
        }

    }

    public static final class NumericWindowStateTypeInfoFactory extends TypeInfoFactory<AbstractStreamFeatureStateStore.NumericWindowState> {

        @Override
        public TypeInformation<AbstractStreamFeatureStateStore.NumericWindowState> createTypeInfo(Type type,
                                                                                                   Map<String, TypeInformation<?>> genericParameters) {
            Map<String, TypeInformation<?>> fields = new LinkedHashMap<>();
            fields.put("retentionMs", Types.LONG);
            fields.put("bucketSizeMs", Types.LONG);
            fields.put("buckets", Types.MAP(Types.LONG, Types.BIG_DEC));
            return Types.POJO(AbstractStreamFeatureStateStore.NumericWindowState.class, fields);
        }

    }

    public static final class LatestValueStateTypeInfoFactory extends TypeInfoFactory<AbstractStreamFeatureStateStore.LatestValueState> {

        @Override
        public TypeInformation<AbstractStreamFeatureStateStore.LatestValueState> createTypeInfo(Type type,
                                                                                                 Map<String, TypeInformation<?>> genericParameters) {
            Map<String, TypeInformation<?>> fields = new LinkedHashMap<>();
            fields.put("retentionMs", Types.LONG);
            fields.put("latestEventTimeMs", Types.LONG);
            fields.put("latestValueRaw", Types.STRING);
            fields.put("latestValueType", Types.STRING);
            return Types.POJO(AbstractStreamFeatureStateStore.LatestValueState.class, fields);
        }

    }

    public static final class DistinctWindowStateTypeInfoFactory extends TypeInfoFactory<AbstractStreamFeatureStateStore.DistinctWindowState> {

        @Override
        public TypeInformation<AbstractStreamFeatureStateStore.DistinctWindowState> createTypeInfo(Type type,
                                                                                                    Map<String, TypeInformation<?>> genericParameters) {
            Map<String, TypeInformation<?>> fields = new LinkedHashMap<>();
            fields.put("retentionMs", Types.LONG);
            fields.put("memberLastSeenMs", Types.MAP(Types.STRING, Types.LONG));
            return Types.POJO(AbstractStreamFeatureStateStore.DistinctWindowState.class, fields);
        }

    }

}
