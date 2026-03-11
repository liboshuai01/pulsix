package cn.liboshuai.pulsix.engine.feature;

import cn.liboshuai.pulsix.engine.context.EvalContext;
import cn.liboshuai.pulsix.engine.model.AggType;
import cn.liboshuai.pulsix.engine.runtime.CompiledSceneRuntime;
import cn.liboshuai.pulsix.engine.support.ValueConverter;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;

public abstract class AbstractStreamFeatureStateStore implements StreamFeatureStateStore {

    private StreamFeatureExecutionContext executionContext;

    @Override
    public void bindExecutionContext(StreamFeatureExecutionContext executionContext) {
        this.executionContext = executionContext;
    }

    @Override
    public void clearExecutionContext() {
        this.executionContext = null;
    }

    @Override
    public Object evaluate(CompiledSceneRuntime.CompiledStreamFeature feature, EvalContext context) {
        if (feature.getSpec().getAggType() == null) {
            return null;
        }
        Object entityKeyValue = feature.getEntityKeyScript().execute(context);
        String entityKey = ValueConverter.asString(entityKeyValue);
        if (entityKey == null || entityKey.isBlank()) {
            return defaultFeatureValue(feature.getSpec().getAggType());
        }
        return switch (feature.getSpec().getAggType()) {
            case COUNT, SUM, MAX -> evaluateNumericFeature(feature, context, entityKey);
            case LATEST -> evaluateLatestFeature(feature, context, entityKey);
            case DISTINCT_COUNT -> evaluateDistinctFeature(feature, context, entityKey);
        };
    }

    protected abstract NumericWindowState getNumericState(String featureInstanceKey);

    protected abstract void putNumericState(String featureInstanceKey, NumericWindowState state);

    protected abstract void removeNumericState(String featureInstanceKey);

    protected abstract LatestValueState getLatestState(String featureInstanceKey);

    protected abstract void putLatestState(String featureInstanceKey, LatestValueState state);

    protected abstract void removeLatestState(String featureInstanceKey);

    protected abstract DistinctWindowState getDistinctState(String featureInstanceKey);

    protected abstract void putDistinctState(String featureInstanceKey, DistinctWindowState state);

    protected abstract void removeDistinctState(String featureInstanceKey);

    private Object evaluateNumericFeature(CompiledSceneRuntime.CompiledStreamFeature feature,
                                          EvalContext context,
                                          String entityKey) {
        String featureInstanceKey = featureInstanceKey(feature, entityKey);
        NumericWindowState state = getNumericState(featureInstanceKey);
        if (state == null) {
            state = new NumericWindowState();
        }
        state.setRetentionMs(feature.getRetentionMs());
        state.setBucketSizeMs(bucketSizeMs(feature));
        long eventTimeMs = eventTimeMs(context);
        cleanupExpiredBuckets(state.getBuckets(), eventTimeMs - feature.getRetentionMs());
        boolean accepted = accepted(feature, context);
        boolean includeCurrent = includeCurrent(feature);
        if (accepted && includeCurrent) {
            updateNumericBucket(feature, context, state, eventTimeMs);
            registerNumericCleanupTimer(feature, eventTimeMs);
        }
        Object featureValue = aggregateNumeric(feature, state.getBuckets(), eventTimeMs);
        if (accepted && !includeCurrent) {
            updateNumericBucket(feature, context, state, eventTimeMs);
            registerNumericCleanupTimer(feature, eventTimeMs);
        }
        if (state.getBuckets().isEmpty()) {
            removeNumericState(featureInstanceKey);
        } else {
            putNumericState(featureInstanceKey, state);
        }
        return featureValue;
    }

    private Object evaluateLatestFeature(CompiledSceneRuntime.CompiledStreamFeature feature,
                                         EvalContext context,
                                         String entityKey) {
        String featureInstanceKey = featureInstanceKey(feature, entityKey);
        LatestValueState state = getLatestState(featureInstanceKey);
        if (state == null) {
            state = new LatestValueState();
        }
        state.setRetentionMs(feature.getRetentionMs());
        long eventTimeMs = eventTimeMs(context);
        boolean accepted = accepted(feature, context);
        boolean includeCurrent = includeCurrent(feature);
        if (accepted && includeCurrent) {
            updateLatestState(feature, context, state, eventTimeMs);
            registerTimer(eventTimeMs + Math.max(feature.getRetentionMs(), 1L));
        }
        Object featureValue = currentLatestValue(feature, state, eventTimeMs);
        if (accepted && !includeCurrent) {
            updateLatestState(feature, context, state, eventTimeMs);
            registerTimer(eventTimeMs + Math.max(feature.getRetentionMs(), 1L));
        }
        if (state.getLatestEventTimeMs() == null) {
            removeLatestState(featureInstanceKey);
        } else {
            putLatestState(featureInstanceKey, state);
        }
        return featureValue;
    }

    private Object evaluateDistinctFeature(CompiledSceneRuntime.CompiledStreamFeature feature,
                                           EvalContext context,
                                           String entityKey) {
        String featureInstanceKey = featureInstanceKey(feature, entityKey);
        DistinctWindowState state = getDistinctState(featureInstanceKey);
        if (state == null) {
            state = new DistinctWindowState();
        }
        state.setRetentionMs(feature.getRetentionMs());
        long eventTimeMs = eventTimeMs(context);
        cleanupExpiredMembers(state.getMemberLastSeenMs(), eventTimeMs - feature.getRetentionMs());
        boolean accepted = accepted(feature, context);
        boolean includeCurrent = includeCurrent(feature);
        if (accepted && includeCurrent) {
            updateDistinctState(feature, context, state, eventTimeMs);
            registerTimer(eventTimeMs + Math.max(feature.getRetentionMs(), 1L));
        }
        long featureValue = countDistinctWithinWindow(feature, state, eventTimeMs);
        if (accepted && !includeCurrent) {
            updateDistinctState(feature, context, state, eventTimeMs);
            registerTimer(eventTimeMs + Math.max(feature.getRetentionMs(), 1L));
        }
        if (state.getMemberLastSeenMs().isEmpty()) {
            removeDistinctState(featureInstanceKey);
        } else {
            putDistinctState(featureInstanceKey, state);
        }
        return featureValue;
    }

    private void updateNumericBucket(CompiledSceneRuntime.CompiledStreamFeature feature,
                                     EvalContext context,
                                     NumericWindowState state,
                                     long eventTimeMs) {
        long bucketTs = toBucket(eventTimeMs, bucketSizeMs(feature));
        BigDecimal increment = switch (feature.getSpec().getAggType()) {
            case COUNT -> BigDecimal.ONE;
            case SUM, MAX -> ValueConverter.asDecimal(feature.getValueScript().execute(context));
            default -> BigDecimal.ZERO;
        };
        BigDecimal current = state.getBuckets().get(bucketTs);
        BigDecimal updated = switch (feature.getSpec().getAggType()) {
            case COUNT, SUM -> (current == null ? BigDecimal.ZERO : current).add(increment);
            case MAX -> current == null ? increment : current.max(increment);
            default -> current;
        };
        state.getBuckets().put(bucketTs, updated);
    }

    private void updateLatestState(CompiledSceneRuntime.CompiledStreamFeature feature,
                                   EvalContext context,
                                   LatestValueState state,
                                   long eventTimeMs) {
        if (state.getLatestEventTimeMs() == null || eventTimeMs >= state.getLatestEventTimeMs()) {
            state.setLatestEventTimeMs(eventTimeMs);
            state.setLatestValue(feature.getValueScript().execute(context));
        }
    }

    private void updateDistinctState(CompiledSceneRuntime.CompiledStreamFeature feature,
                                     EvalContext context,
                                     DistinctWindowState state,
                                     long eventTimeMs) {
        String member = ValueConverter.asString(feature.getValueScript().execute(context));
        if (member == null || member.isBlank()) {
            return;
        }
        state.getMemberLastSeenMs().put(member, eventTimeMs);
    }

    private Object aggregateNumeric(CompiledSceneRuntime.CompiledStreamFeature feature,
                                    Map<Long, BigDecimal> buckets,
                                    long eventTimeMs) {
        if (buckets.isEmpty()) {
            return defaultFeatureValue(feature.getSpec().getAggType());
        }
        long windowStartBucket = toBucket(windowStartMs(feature, eventTimeMs), bucketSizeMs(feature));
        return switch (feature.getSpec().getAggType()) {
            case COUNT -> buckets.entrySet().stream()
                    .filter(entry -> entry.getKey() >= windowStartBucket)
                    .map(Map.Entry::getValue)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add)
                    .longValue();
            case SUM -> buckets.entrySet().stream()
                    .filter(entry -> entry.getKey() >= windowStartBucket)
                    .map(Map.Entry::getValue)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            case MAX -> buckets.entrySet().stream()
                    .filter(entry -> entry.getKey() >= windowStartBucket)
                    .map(Map.Entry::getValue)
                    .filter(Objects::nonNull)
                    .max(BigDecimal::compareTo)
                    .orElse(BigDecimal.ZERO);
            default -> defaultFeatureValue(feature.getSpec().getAggType());
        };
    }

    private Object currentLatestValue(CompiledSceneRuntime.CompiledStreamFeature feature,
                                      LatestValueState state,
                                      long eventTimeMs) {
        Long latestEventTimeMs = state.getLatestEventTimeMs();
        if (latestEventTimeMs == null) {
            return defaultFeatureValue(feature.getSpec().getAggType());
        }
        if (feature.getWindowSizeMs() > 0L && latestEventTimeMs < eventTimeMs - feature.getWindowSizeMs()) {
            state.setLatestEventTimeMs(null);
            state.setLatestValue(null);
            return defaultFeatureValue(feature.getSpec().getAggType());
        }
        return state.getLatestValue();
    }

    private long countDistinctWithinWindow(CompiledSceneRuntime.CompiledStreamFeature feature,
                                           DistinctWindowState state,
                                           long eventTimeMs) {
        long boundary = feature.getWindowSizeMs() > 0L ? eventTimeMs - feature.getWindowSizeMs() : Long.MIN_VALUE;
        return state.getMemberLastSeenMs().values().stream()
                .filter(Objects::nonNull)
                .filter(value -> value >= boundary)
                .count();
    }

    protected long bucketSizeMs(CompiledSceneRuntime.CompiledStreamFeature feature) {
        return feature.getWindowSlideMs() > 0L ? feature.getWindowSlideMs() : Math.max(feature.getWindowSizeMs(), 1L);
    }

    protected long eventTimeMs(EvalContext context) {
        Instant eventTime = context.getEvent() != null ? context.getEvent().getEventTime() : null;
        return eventTime != null ? eventTime.toEpochMilli() : System.currentTimeMillis();
    }

    protected long windowStartMs(CompiledSceneRuntime.CompiledStreamFeature feature, long eventTimeMs) {
        long bucketSizeMs = bucketSizeMs(feature);
        long windowSizeMs = feature.getWindowSizeMs() > 0L ? feature.getWindowSizeMs() : bucketSizeMs;
        long inclusiveStart = Math.max(0L, eventTimeMs - windowSizeMs + bucketSizeMs);
        return toBucket(inclusiveStart, bucketSizeMs);
    }

    protected long toBucket(long timestamp, long bucketSizeMs) {
        long safeBucketSizeMs = Math.max(bucketSizeMs, 1L);
        return Math.floorDiv(timestamp, safeBucketSizeMs) * safeBucketSizeMs;
    }

    protected void cleanupExpiredBuckets(Map<Long, BigDecimal> buckets, long boundaryMs) {
        if (boundaryMs <= 0L || buckets.isEmpty()) {
            return;
        }
        Iterator<Map.Entry<Long, BigDecimal>> iterator = buckets.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<Long, BigDecimal> entry = iterator.next();
            if (entry.getKey() < boundaryMs) {
                iterator.remove();
            }
        }
    }

    protected void cleanupExpiredMembers(Map<String, Long> members, long boundaryMs) {
        if (boundaryMs <= 0L || members.isEmpty()) {
            return;
        }
        Iterator<Map.Entry<String, Long>> iterator = members.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, Long> entry = iterator.next();
            if (entry.getValue() == null || entry.getValue() < boundaryMs) {
                iterator.remove();
            }
        }
    }

    protected void registerNumericCleanupTimer(CompiledSceneRuntime.CompiledStreamFeature feature, long eventTimeMs) {
        long bucketTs = toBucket(eventTimeMs, bucketSizeMs(feature));
        long cleanupTs = bucketTs + bucketSizeMs(feature) + Math.max(feature.getRetentionMs(), 1L);
        registerTimer(cleanupTs);
    }

    protected void registerTimer(long timestamp) {
        if (executionContext != null && timestamp > 0L) {
            executionContext.registerEventTimeTimer(timestamp);
        }
    }

    protected boolean includeCurrent(CompiledSceneRuntime.CompiledStreamFeature feature) {
        return !Boolean.FALSE.equals(feature.getSpec().getIncludeCurrentEvent());
    }

    protected boolean accepted(CompiledSceneRuntime.CompiledStreamFeature feature, EvalContext context) {
        boolean acceptedEventType = feature.getSpec().getSourceEventTypes() == null
                || feature.getSpec().getSourceEventTypes().isEmpty()
                || feature.getSpec().getSourceEventTypes().contains(context.getEvent().getEventType());
        boolean acceptedFilter = ValueConverter.asBoolean(feature.getFilterScript().execute(context));
        return acceptedEventType && acceptedFilter;
    }

    protected String featureInstanceKey(CompiledSceneRuntime.CompiledStreamFeature feature, String entityKey) {
        return feature.getSpec().getCode() + '|' + entityKey;
    }

    protected Object defaultFeatureValue(AggType aggType) {
        if (aggType == null) {
            return null;
        }
        return switch (aggType) {
            case COUNT, DISTINCT_COUNT -> 0L;
            case SUM, MAX -> BigDecimal.ZERO;
            case LATEST -> null;
        };
    }

    public static class NumericWindowState implements Serializable {

        private long retentionMs;

        private long bucketSizeMs;

        private Map<Long, BigDecimal> buckets = new HashMap<>();

        public long getRetentionMs() {
            return retentionMs;
        }

        public void setRetentionMs(long retentionMs) {
            this.retentionMs = retentionMs;
        }

        public long getBucketSizeMs() {
            return bucketSizeMs;
        }

        public void setBucketSizeMs(long bucketSizeMs) {
            this.bucketSizeMs = bucketSizeMs;
        }

        public Map<Long, BigDecimal> getBuckets() {
            return buckets;
        }

        public void setBuckets(Map<Long, BigDecimal> buckets) {
            this.buckets = buckets;
        }

    }

    public static class LatestValueState implements Serializable {

        private long retentionMs;

        private Long latestEventTimeMs;

        private String latestValueRaw;

        private String latestValueType;

        public long getRetentionMs() {
            return retentionMs;
        }

        public void setRetentionMs(long retentionMs) {
            this.retentionMs = retentionMs;
        }

        public Long getLatestEventTimeMs() {
            return latestEventTimeMs;
        }

        public void setLatestEventTimeMs(Long latestEventTimeMs) {
            this.latestEventTimeMs = latestEventTimeMs;
        }

        public Object getLatestValue() {
            return ValueConverter.coerce(latestValueRaw, latestValueType);
        }

        public void setLatestValue(Object latestValue) {
            if (latestValue == null) {
                this.latestValueRaw = null;
                this.latestValueType = null;
                return;
            }
            if (latestValue instanceof Boolean) {
                this.latestValueType = "BOOLEAN";
                this.latestValueRaw = String.valueOf(latestValue);
                return;
            }
            if (latestValue instanceof Byte || latestValue instanceof Short
                    || latestValue instanceof Integer || latestValue instanceof Long) {
                this.latestValueType = "LONG";
                this.latestValueRaw = String.valueOf(((Number) latestValue).longValue());
                return;
            }
            if (latestValue instanceof Number) {
                this.latestValueType = "DECIMAL";
                this.latestValueRaw = ValueConverter.asDecimal(latestValue).toPlainString();
                return;
            }
            this.latestValueType = "STRING";
            this.latestValueRaw = String.valueOf(latestValue);
        }

        public String getLatestValueRaw() {
            return latestValueRaw;
        }

        public void setLatestValueRaw(String latestValueRaw) {
            this.latestValueRaw = latestValueRaw;
        }

        public String getLatestValueType() {
            return latestValueType;
        }

        public void setLatestValueType(String latestValueType) {
            this.latestValueType = latestValueType;
        }

    }

    public static class DistinctWindowState implements Serializable {

        private long retentionMs;

        private Map<String, Long> memberLastSeenMs = new HashMap<>();

        public long getRetentionMs() {
            return retentionMs;
        }

        public void setRetentionMs(long retentionMs) {
            this.retentionMs = retentionMs;
        }

        public Map<String, Long> getMemberLastSeenMs() {
            return memberLastSeenMs;
        }

        public void setMemberLastSeenMs(Map<String, Long> memberLastSeenMs) {
            this.memberLastSeenMs = memberLastSeenMs;
        }

    }

}
