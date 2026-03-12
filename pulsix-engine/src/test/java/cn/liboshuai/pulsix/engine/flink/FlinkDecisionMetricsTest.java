package cn.liboshuai.pulsix.engine.flink;

import cn.liboshuai.pulsix.engine.model.ActionType;
import cn.liboshuai.pulsix.engine.model.DecisionResult;
import cn.liboshuai.pulsix.engine.model.EngineErrorCodes;
import cn.liboshuai.pulsix.engine.model.EngineErrorRecord;
import cn.liboshuai.pulsix.engine.model.EngineErrorTypes;
import cn.liboshuai.pulsix.engine.model.RuleHit;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FlinkDecisionMetricsTest {

    @Test
    void shouldTrackDecisionAndLookupMetrics() {
        FlinkDecisionMetrics metrics = FlinkDecisionMetrics.create(null);
        DecisionResult result = new DecisionResult();
        result.setLatencyMs(18L);
        result.setFinalAction(ActionType.REJECT);
        RuleHit ruleHit = new RuleHit();
        ruleHit.setRuleCode("R001");
        ruleHit.setHit(true);
        result.setRuleHits(List.of(ruleHit));

        EngineErrorRecord lookupError = new EngineErrorRecord();
        lookupError.setErrorType(EngineErrorTypes.LOOKUP);
        lookupError.setErrorCode("LOOKUP_TIMEOUT");
        lookupError.setFallbackMode("DEFAULT_VALUE");

        metrics.onInputEvent();
        metrics.onDecisionResult(result);
        metrics.onDecisionLogEmitted();
        metrics.onEngineError(lookupError);

        assertEquals(1L, metrics.inputEventCount());
        assertEquals(1L, metrics.decisionOutputCount());
        assertEquals(1L, metrics.decisionLogCount());
        assertEquals(1L, metrics.lookupErrorCount());
        assertEquals(1L, metrics.lookupTimeoutCount());
        assertEquals(1L, metrics.lookupFallbackCount());
        assertEquals(1L, metrics.ruleHitCount());
        assertEquals(18L, metrics.lastDecisionLatencyMs());
    }

    @Test
    void shouldTrackSnapshotAndExecutionFailures() {
        FlinkDecisionMetrics metrics = FlinkDecisionMetrics.create(null);
        EngineErrorRecord compileError = new EngineErrorRecord();
        compileError.setErrorType(EngineErrorTypes.SNAPSHOT);
        compileError.setErrorCode(EngineErrorCodes.GROOVY_SANDBOX_REJECTED);
        compileError.setEngineType("GROOVY");

        EngineErrorRecord executionError = new EngineErrorRecord();
        executionError.setErrorType(EngineErrorTypes.EXECUTION);
        executionError.setErrorCode(EngineErrorCodes.RULE_EXECUTION_FAILED);
        executionError.setEngineType("GROOVY");

        EngineErrorRecord stateError = new EngineErrorRecord();
        stateError.setErrorType(EngineErrorTypes.STATE);
        stateError.setErrorCode(EngineErrorCodes.STATE_ACCESS_FAILED);

        metrics.onEngineError(compileError);
        metrics.onEngineError(executionError);
        metrics.onEngineError(stateError);

        assertEquals(3L, metrics.engineErrorCount());
        assertEquals(1L, metrics.snapshotCompileFailureCount());
        assertEquals(1L, metrics.executionErrorCount());
        assertEquals(1L, metrics.stateErrorCount());
        assertEquals(2L, metrics.groovyErrorCount());
    }

    @Test
    void shouldTrackPreparedTopologyMetrics() {
        FlinkDecisionMetrics metrics = FlinkDecisionMetrics.create(null);

        EngineErrorRecord aggregateTimeoutError = new EngineErrorRecord();
        aggregateTimeoutError.setErrorType(EngineErrorTypes.STATE);
        aggregateTimeoutError.setErrorCode(EngineErrorCodes.PREPARED_DECISION_AGGREGATE_TIMEOUT);

        metrics.onPreparedRoute(2);
        metrics.onPreparedBypass();
        metrics.onPreparedChunk();
        metrics.onPreparedChunk();
        metrics.onPreparedAggregatePendingGroupsDelta(2);
        metrics.onPreparedAggregatePendingGroupsDelta(-1);
        metrics.onPreparedAggregateCompleted();
        metrics.onPreparedAggregateTimeout();
        metrics.onEngineError(aggregateTimeoutError);
        metrics.onPreparedAggregatePendingGroupsDelta(-1);

        assertEquals(2L, metrics.preparedRouteCount());
        assertEquals(1L, metrics.preparedBypassCount());
        assertEquals(2L, metrics.preparedChunkCount());
        assertEquals(1L, metrics.preparedAggregateCompleteCount());
        assertEquals(1L, metrics.preparedAggregateTimeoutCount());
        assertEquals(0, metrics.preparedAggregatePendingGroups());
        assertEquals(1L, metrics.engineErrorCount());
        assertEquals(1L, metrics.stateErrorCount());
    }

}
