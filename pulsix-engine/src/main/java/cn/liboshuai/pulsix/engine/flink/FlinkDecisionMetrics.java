package cn.liboshuai.pulsix.engine.flink;

import cn.liboshuai.pulsix.engine.model.DecisionResult;
import cn.liboshuai.pulsix.engine.model.EngineErrorCodes;
import cn.liboshuai.pulsix.engine.model.EngineErrorRecord;
import cn.liboshuai.pulsix.engine.model.EngineErrorTypes;
import cn.liboshuai.pulsix.engine.model.RuleHit;
import org.apache.flink.metrics.Counter;
import org.apache.flink.metrics.MetricGroup;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

final class FlinkDecisionMetrics {

    private final Counter inputEventCounter;

    private final Counter decisionOutputCounter;

    private final Counter decisionLogCounter;

    private final Counter engineErrorCounter;

    private final Counter noSnapshotCounter;

    private final Counter snapshotCompileSuccessCounter;

    private final Counter snapshotCompileFailureCounter;

    private final Counter snapshotConflictCounter;

    private final Counter lookupErrorCounter;

    private final Counter lookupTimeoutCounter;

    private final Counter lookupFallbackCounter;

    private final Counter stateErrorCounter;

    private final Counter executionErrorCounter;

    private final Counter expressionErrorCounter;

    private final Counter groovyErrorCounter;

    private final Counter ruleHitCounter;

    private final Counter pendingDroppedCounter;

    private final Counter preparedRouteCounter;

    private final Counter preparedBypassCounter;

    private final Counter preparedChunkCounter;

    private final Counter preparedAggregateCompleteCounter;

    private final Counter preparedAggregateTimeoutCounter;

    private final AtomicLong inputEventCount = new AtomicLong();

    private final AtomicLong decisionOutputCount = new AtomicLong();

    private final AtomicLong decisionLogCount = new AtomicLong();

    private final AtomicLong engineErrorCount = new AtomicLong();

    private final AtomicLong noSnapshotCount = new AtomicLong();

    private final AtomicLong snapshotCompileSuccessCount = new AtomicLong();

    private final AtomicLong snapshotCompileFailureCount = new AtomicLong();

    private final AtomicLong snapshotConflictCount = new AtomicLong();

    private final AtomicLong lookupErrorCount = new AtomicLong();

    private final AtomicLong lookupTimeoutCount = new AtomicLong();

    private final AtomicLong lookupFallbackCount = new AtomicLong();

    private final AtomicLong stateErrorCount = new AtomicLong();

    private final AtomicLong executionErrorCount = new AtomicLong();

    private final AtomicLong expressionErrorCount = new AtomicLong();

    private final AtomicLong groovyErrorCount = new AtomicLong();

    private final AtomicLong ruleHitCount = new AtomicLong();

    private final AtomicLong pendingDroppedCount = new AtomicLong();

    private final AtomicLong preparedRouteCount = new AtomicLong();

    private final AtomicLong preparedBypassCount = new AtomicLong();

    private final AtomicLong preparedChunkCount = new AtomicLong();

    private final AtomicLong preparedAggregateCompleteCount = new AtomicLong();

    private final AtomicLong preparedAggregateTimeoutCount = new AtomicLong();

    private final AtomicLong lastDecisionLatencyMs = new AtomicLong();

    private final AtomicInteger pendingEventBufferSize = new AtomicInteger();

    private final AtomicLong pendingOldestAgeMs = new AtomicLong();

    private final AtomicInteger preparedAggregatePendingGroups = new AtomicInteger();

    private FlinkDecisionMetrics(Counter inputEventCounter,
                                 Counter decisionOutputCounter,
                                 Counter decisionLogCounter,
                                 Counter engineErrorCounter,
                                 Counter noSnapshotCounter,
                                 Counter snapshotCompileSuccessCounter,
                                 Counter snapshotCompileFailureCounter,
                                 Counter snapshotConflictCounter,
                                 Counter lookupErrorCounter,
                                 Counter lookupTimeoutCounter,
                                 Counter lookupFallbackCounter,
                                 Counter stateErrorCounter,
                                 Counter executionErrorCounter,
                                 Counter expressionErrorCounter,
                                 Counter groovyErrorCounter,
                                 Counter ruleHitCounter,
                                 Counter pendingDroppedCounter,
                                 Counter preparedRouteCounter,
                                 Counter preparedBypassCounter,
                                 Counter preparedChunkCounter,
                                 Counter preparedAggregateCompleteCounter,
                                 Counter preparedAggregateTimeoutCounter) {
        this.inputEventCounter = inputEventCounter;
        this.decisionOutputCounter = decisionOutputCounter;
        this.decisionLogCounter = decisionLogCounter;
        this.engineErrorCounter = engineErrorCounter;
        this.noSnapshotCounter = noSnapshotCounter;
        this.snapshotCompileSuccessCounter = snapshotCompileSuccessCounter;
        this.snapshotCompileFailureCounter = snapshotCompileFailureCounter;
        this.snapshotConflictCounter = snapshotConflictCounter;
        this.lookupErrorCounter = lookupErrorCounter;
        this.lookupTimeoutCounter = lookupTimeoutCounter;
        this.lookupFallbackCounter = lookupFallbackCounter;
        this.stateErrorCounter = stateErrorCounter;
        this.executionErrorCounter = executionErrorCounter;
        this.expressionErrorCounter = expressionErrorCounter;
        this.groovyErrorCounter = groovyErrorCounter;
        this.ruleHitCounter = ruleHitCounter;
        this.pendingDroppedCounter = pendingDroppedCounter;
        this.preparedRouteCounter = preparedRouteCounter;
        this.preparedBypassCounter = preparedBypassCounter;
        this.preparedChunkCounter = preparedChunkCounter;
        this.preparedAggregateCompleteCounter = preparedAggregateCompleteCounter;
        this.preparedAggregateTimeoutCounter = preparedAggregateTimeoutCounter;
    }

    static FlinkDecisionMetrics create(MetricGroup metricGroup) {
        if (metricGroup == null) {
            return new FlinkDecisionMetrics(
                    noOpCounter(), noOpCounter(), noOpCounter(), noOpCounter(), noOpCounter(),
                    noOpCounter(), noOpCounter(), noOpCounter(), noOpCounter(), noOpCounter(), noOpCounter(),
                    noOpCounter(), noOpCounter(), noOpCounter(), noOpCounter(), noOpCounter(), noOpCounter(),
                    noOpCounter(), noOpCounter(), noOpCounter(), noOpCounter(), noOpCounter()
            );
        }
        MetricGroup group = metricGroup.addGroup("pulsix").addGroup("engine");
        FlinkDecisionMetrics metrics = new FlinkDecisionMetrics(
                group.counter("inputEventCount"),
                group.counter("decisionOutputCount"),
                group.counter("decisionLogCount"),
                group.counter("engineErrorCount"),
                group.counter("noSnapshotCount"),
                group.counter("snapshotCompileSuccessCount"),
                group.counter("snapshotCompileFailureCount"),
                group.counter("snapshotConflictCount"),
                group.counter("lookupErrorCount"),
                group.counter("lookupTimeoutCount"),
                group.counter("lookupFallbackCount"),
                group.counter("stateErrorCount"),
                group.counter("executionErrorCount"),
                group.counter("expressionErrorCount"),
                group.counter("groovyErrorCount"),
                group.counter("ruleHitCount"),
                group.counter("pendingDroppedCount"),
                group.counter("preparedRouteCount"),
                group.counter("preparedBypassCount"),
                group.counter("preparedChunkCount"),
                group.counter("preparedAggregateCompleteCount"),
                group.counter("preparedAggregateTimeoutCount")
        );
        group.gauge("lastDecisionLatencyMs", metrics.lastDecisionLatencyMs::get);
        group.gauge("pendingEventBufferSize", metrics.pendingEventBufferSize::get);
        group.gauge("pendingOldestAgeMs", metrics.pendingOldestAgeMs::get);
        group.gauge("preparedAggregatePendingGroups", metrics.preparedAggregatePendingGroups::get);
        return metrics;
    }

    void onInputEvent() {
        increment(inputEventCounter, inputEventCount, 1L);
    }

    void onPendingBuffered() {
        pendingEventBufferSize.incrementAndGet();
    }

    void onPendingFlushed(int count) {
        if (count <= 0) {
            return;
        }
        pendingEventBufferSize.updateAndGet(current -> Math.max(current - count, 0));
    }

    void onPendingDropped(int count) {
        if (count <= 0) {
            return;
        }
        increment(pendingDroppedCounter, pendingDroppedCount, count);
        pendingEventBufferSize.updateAndGet(current -> Math.max(current - count, 0));
    }

    void onPendingOldestAgeObserved(long oldestAgeMs) {
        pendingOldestAgeMs.set(Math.max(oldestAgeMs, 0L));
    }

    void onNoSnapshot() {
        increment(noSnapshotCounter, noSnapshotCount, 1L);
    }

    void onSnapshotCompiled() {
        increment(snapshotCompileSuccessCounter, snapshotCompileSuccessCount, 1L);
    }

    void onPreparedRoute(int routeCount) {
        increment(preparedRouteCounter, preparedRouteCount, routeCount);
    }

    void onPreparedBypass() {
        increment(preparedBypassCounter, preparedBypassCount, 1L);
    }

    void onPreparedChunk() {
        increment(preparedChunkCounter, preparedChunkCount, 1L);
    }

    void onPreparedAggregateCompleted() {
        increment(preparedAggregateCompleteCounter, preparedAggregateCompleteCount, 1L);
    }

    void onPreparedAggregateTimeout() {
        increment(preparedAggregateTimeoutCounter, preparedAggregateTimeoutCount, 1L);
    }

    void onPreparedAggregatePendingGroupsDelta(int delta) {
        if (delta == 0) {
            return;
        }
        preparedAggregatePendingGroups.updateAndGet(current -> Math.max(current + delta, 0));
    }

    void onDecisionResult(DecisionResult result) {
        increment(decisionOutputCounter, decisionOutputCount, 1L);
        if (result != null && result.getLatencyMs() != null) {
            lastDecisionLatencyMs.set(result.getLatencyMs());
        }
        long hitCount = result == null || result.getRuleHits() == null ? 0L : result.getRuleHits().stream()
                .filter(ruleHit -> ruleHit != null && Boolean.TRUE.equals(ruleHit.getHit()))
                .map(RuleHit::getRuleCode)
                .count();
        if (hitCount > 0L) {
            increment(ruleHitCounter, ruleHitCount, hitCount);
        }
    }

    void onDecisionLogEmitted() {
        increment(decisionLogCounter, decisionLogCount, 1L);
    }

    void onEngineError(EngineErrorRecord record) {
        if (record == null) {
            return;
        }
        increment(engineErrorCounter, engineErrorCount, 1L);
        if (EngineErrorTypes.LOOKUP.equals(record.getErrorType())) {
            increment(lookupErrorCounter, lookupErrorCount, 1L);
            if ("LOOKUP_TIMEOUT".equals(record.getErrorCode())) {
                increment(lookupTimeoutCounter, lookupTimeoutCount, 1L);
            }
            if (record.hasFallback()) {
                increment(lookupFallbackCounter, lookupFallbackCount, 1L);
            }
            return;
        }
        if (EngineErrorTypes.STATE.equals(record.getErrorType())) {
            increment(stateErrorCounter, stateErrorCount, 1L);
        }
        if (EngineErrorTypes.EXECUTION.equals(record.getErrorType()) || EngineErrorTypes.INPUT.equals(record.getErrorType())) {
            increment(executionErrorCounter, executionErrorCount, 1L);
        }
        if (EngineErrorCodes.SNAPSHOT_VERSION_CONFLICT.equals(record.getErrorCode())) {
            increment(snapshotConflictCounter, snapshotConflictCount, 1L);
        }
        if (EngineErrorCodes.SNAPSHOT_COMPILE_FAILED.equals(record.getErrorCode())
                || EngineErrorCodes.GROOVY_DISABLED.equals(record.getErrorCode())
                || EngineErrorCodes.GROOVY_SANDBOX_REJECTED.equals(record.getErrorCode())) {
            increment(snapshotCompileFailureCounter, snapshotCompileFailureCount, 1L);
        }
        if ("GROOVY".equals(record.getEngineType())) {
            increment(groovyErrorCounter, groovyErrorCount, 1L);
            return;
        }
        if (EngineErrorTypes.EXECUTION.equals(record.getErrorType())) {
            increment(expressionErrorCounter, expressionErrorCount, 1L);
        }
    }

    long inputEventCount() {
        return inputEventCount.get();
    }

    long decisionOutputCount() {
        return decisionOutputCount.get();
    }

    long decisionLogCount() {
        return decisionLogCount.get();
    }

    long engineErrorCount() {
        return engineErrorCount.get();
    }

    long noSnapshotCount() {
        return noSnapshotCount.get();
    }

    long snapshotCompileSuccessCount() {
        return snapshotCompileSuccessCount.get();
    }

    long snapshotCompileFailureCount() {
        return snapshotCompileFailureCount.get();
    }

    long snapshotConflictCount() {
        return snapshotConflictCount.get();
    }

    long lookupErrorCount() {
        return lookupErrorCount.get();
    }

    long lookupTimeoutCount() {
        return lookupTimeoutCount.get();
    }

    long lookupFallbackCount() {
        return lookupFallbackCount.get();
    }

    long stateErrorCount() {
        return stateErrorCount.get();
    }

    long executionErrorCount() {
        return executionErrorCount.get();
    }

    long expressionErrorCount() {
        return expressionErrorCount.get();
    }

    long groovyErrorCount() {
        return groovyErrorCount.get();
    }

    long ruleHitCount() {
        return ruleHitCount.get();
    }

    long lastDecisionLatencyMs() {
        return lastDecisionLatencyMs.get();
    }

    int pendingEventBufferSize() {
        return pendingEventBufferSize.get();
    }

    long pendingDroppedCount() {
        return pendingDroppedCount.get();
    }

    long pendingOldestAgeMs() {
        return pendingOldestAgeMs.get();
    }

    long preparedRouteCount() {
        return preparedRouteCount.get();
    }

    long preparedBypassCount() {
        return preparedBypassCount.get();
    }

    long preparedChunkCount() {
        return preparedChunkCount.get();
    }

    long preparedAggregateCompleteCount() {
        return preparedAggregateCompleteCount.get();
    }

    long preparedAggregateTimeoutCount() {
        return preparedAggregateTimeoutCount.get();
    }

    int preparedAggregatePendingGroups() {
        return preparedAggregatePendingGroups.get();
    }

    private static void increment(Counter counter, AtomicLong value, long delta) {
        if (delta <= 0L) {
            return;
        }
        counter.inc(delta);
        value.addAndGet(delta);
    }

    private static Counter noOpCounter() {
        return new Counter() {
            @Override
            public void inc() {
            }

            @Override
            public void inc(long n) {
            }

            @Override
            public void dec() {
            }

            @Override
            public void dec(long n) {
            }

            @Override
            public long getCount() {
                return 0L;
            }
        };
    }

}
