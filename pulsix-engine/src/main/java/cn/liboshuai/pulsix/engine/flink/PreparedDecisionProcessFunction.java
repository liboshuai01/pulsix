package cn.liboshuai.pulsix.engine.flink;

import cn.liboshuai.pulsix.engine.core.DecisionExecutionException;
import cn.liboshuai.pulsix.engine.core.DecisionExecutor;
import cn.liboshuai.pulsix.engine.feature.LookupService;
import cn.liboshuai.pulsix.engine.flink.runtime.CompiledSceneRuntimeResolver;
import cn.liboshuai.pulsix.engine.model.DecisionLogRecord;
import cn.liboshuai.pulsix.engine.model.DecisionResult;
import cn.liboshuai.pulsix.engine.model.EngineErrorCodes;
import cn.liboshuai.pulsix.engine.model.EngineErrorRecord;
import cn.liboshuai.pulsix.engine.model.EngineErrorTypes;
import cn.liboshuai.pulsix.engine.model.SceneSnapshot;
import cn.liboshuai.pulsix.engine.runtime.CompiledSceneRuntime;
import cn.liboshuai.pulsix.engine.runtime.RuntimeCompileException;
import cn.liboshuai.pulsix.engine.runtime.RuntimeCompiler;
import cn.liboshuai.pulsix.engine.script.DefaultScriptCompiler;
import org.apache.flink.streaming.api.functions.ProcessFunction;
import org.apache.flink.util.Collector;

public class PreparedDecisionProcessFunction
        extends ProcessFunction<PreparedDecisionInput, DecisionResult> {

    private final EngineLookupServiceFactory lookupServiceFactory;

    private transient CompiledSceneRuntimeResolver runtimeResolver;

    private transient DecisionExecutor decisionExecutor;

    private transient LookupService lookupService;

    private transient FlinkDecisionMetrics metrics;

    public PreparedDecisionProcessFunction(EngineLookupServiceFactory lookupServiceFactory) {
        this.lookupServiceFactory = lookupServiceFactory;
    }

    @Override
    public void open(org.apache.flink.configuration.Configuration parameters) {
        this.runtimeResolver = new CompiledSceneRuntimeResolver(new RuntimeCompiler(new DefaultScriptCompiler()));
        this.decisionExecutor = new DecisionExecutor();
        this.lookupService = lookupServiceFactory.create();
        this.metrics = FlinkDecisionMetrics.create(getRuntimeContext().getMetricGroup());
    }

    @Override
    public void processElement(PreparedDecisionInput value,
                               Context context,
                               Collector<DecisionResult> collector) {
        if (value == null || value.getEvent() == null || value.getSnapshot() == null) {
            return;
        }
        CompiledSceneRuntime runtime = null;
        try {
            runtime = resolveRuntime(value.getSnapshot());
            DecisionExecutor.PreparedDecisionContext preparedContext = decisionExecutor.restorePreparedContext(runtime,
                    value.getEvent(),
                    value.getFeatureSnapshot(),
                    System.nanoTime());
            DecisionResult result = decisionExecutor.executePrepared(runtime,
                    preparedContext,
                    lookupService,
                    record -> emitEngineError(context, record));
            if (value.getPreparedAtEpochMs() != null) {
                result.setLatencyMs(Math.max(System.currentTimeMillis() - value.getPreparedAtEpochMs(), 0L));
            }
            collector.collect(result);
            metrics.onDecisionResult(result);
            context.output(EngineOutputTags.DECISION_LOG, DecisionLogRecord.from(result, runtime.needFullDecisionLog()));
            metrics.onDecisionLogEmitted();
        } catch (Exception exception) {
            emitEngineError(context, errorRecord(value, runtime, exception));
        }
    }

    private CompiledSceneRuntime resolveRuntime(SceneSnapshot snapshot) {
        return runtimeResolver.resolve(snapshot);
    }

    private EngineErrorRecord errorRecord(PreparedDecisionInput input,
                                          CompiledSceneRuntime runtime,
                                          Throwable throwable) {
        if (throwable instanceof RuntimeCompileException compileException) {
            EngineErrorRecord record = EngineErrorRecord.of("prepared-decision-compile",
                    EngineErrorTypes.SNAPSHOT,
                    compileException.getErrorCode(),
                    input == null ? null : input.getEvent(),
                    input != null && input.getSnapshot() != null ? input.getSnapshot().getVersion() : null,
                    throwable);
            if (input != null && input.getSnapshot() != null) {
                record.setSceneCode(input.getSnapshot().getSceneCode());
                record.setSnapshotId(input.getSnapshot().getSnapshotId());
                record.setSnapshotChecksum(input.getSnapshot().getChecksum());
            }
            record.setFeatureCode(compileException.getFeatureCode());
            record.setRuleCode(compileException.getRuleCode());
            record.setEngineType(compileException.getEngineType() == null ? null : compileException.getEngineType().name());
            return record;
        }
        if (throwable instanceof DecisionExecutionException executionException) {
            EngineErrorRecord record = EngineErrorRecord.of(executionException.getStage(),
                    executionException.getErrorType(),
                    executionException.getErrorCode(),
                    input == null ? null : input.getEvent(),
                    runtime == null ? null : runtime.version(),
                    throwable);
            populateRuntimeDetails(record, runtime);
            record.setFeatureCode(executionException.getFeatureCode());
            record.setRuleCode(executionException.getRuleCode());
            record.setEngineType(executionException.getEngineType() == null ? null : executionException.getEngineType().name());
            return record;
        }
        EngineErrorRecord record = EngineErrorRecord.of("prepared-decision-execute",
                EngineErrorTypes.EXECUTION,
                EngineErrorCodes.DECISION_EXECUTION_FAILED,
                input == null ? null : input.getEvent(),
                runtime == null ? null : runtime.version(),
                throwable);
        populateRuntimeDetails(record, runtime);
        return record;
    }

    private void populateRuntimeDetails(EngineErrorRecord record, CompiledSceneRuntime runtime) {
        if (record == null || runtime == null || runtime.getSnapshot() == null) {
            return;
        }
        record.setSceneCode(runtime.sceneCode());
        record.setSnapshotId(runtime.getSnapshot().getSnapshotId());
        record.setSnapshotChecksum(runtime.getSnapshot().getChecksum());
    }

    private void emitEngineError(ProcessFunction<PreparedDecisionInput, DecisionResult>.Context context,
                                 EngineErrorRecord record) {
        context.output(EngineOutputTags.ENGINE_ERROR, record);
        metrics.onEngineError(record);
    }

}
