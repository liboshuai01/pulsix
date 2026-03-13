package cn.liboshuai.pulsix.access.ingest.service.metrics;

import cn.hutool.core.util.StrUtil;
import cn.liboshuai.pulsix.access.ingest.enums.IngestStageEnum;
import cn.liboshuai.pulsix.access.ingest.service.metrics.model.IngestMetricsSnapshot;
import cn.liboshuai.pulsix.access.ingest.service.metrics.model.IngestSourceMetricsSnapshot;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;

@Service
public class InMemoryIngestMetricsService implements IngestMetricsService {

    private static final String UNKNOWN_SOURCE = "_UNKNOWN";

    private final ConcurrentMap<String, SourceMetricsBucket> sourceMetrics = new ConcurrentHashMap<>();
    private final LongAdder totalCount = new LongAdder();
    private final LongAdder acceptedCount = new LongAdder();
    private final LongAdder rejectedCount = new LongAdder();
    private final LongAdder errorCount = new LongAdder();
    private final LongAdder totalProcessTimeMillis = new LongAdder();
    private final LongAdder totalProduceLatencyMillis = new LongAdder();
    private final AtomicInteger nettyActiveConnectionCount = new AtomicInteger(0);

    @Override
    public void recordAccepted(String sourceCode, long processTimeMillis, long produceLatencyMillis) {
        String actualSourceCode = normalizeSourceCode(sourceCode);
        totalCount.increment();
        acceptedCount.increment();
        totalProcessTimeMillis.add(Math.max(processTimeMillis, 0L));
        totalProduceLatencyMillis.add(Math.max(produceLatencyMillis, 0L));
        sourceMetrics.computeIfAbsent(actualSourceCode, ignored -> new SourceMetricsBucket(actualSourceCode))
                .recordAccepted(processTimeMillis, produceLatencyMillis);
    }

    @Override
    public void recordRejected(String sourceCode, IngestStageEnum stage, long processTimeMillis) {
        String actualSourceCode = normalizeSourceCode(sourceCode);
        totalCount.increment();
        rejectedCount.increment();
        errorCount.increment();
        totalProcessTimeMillis.add(Math.max(processTimeMillis, 0L));
        sourceMetrics.computeIfAbsent(actualSourceCode, ignored -> new SourceMetricsBucket(actualSourceCode))
                .recordRejected(processTimeMillis, stage);
    }

    @Override
    public void recordNettyConnectionOpened() {
        nettyActiveConnectionCount.incrementAndGet();
    }

    @Override
    public void recordNettyConnectionClosed() {
        nettyActiveConnectionCount.updateAndGet(current -> Math.max(current - 1, 0));
    }

    @Override
    public IngestMetricsSnapshot snapshot() {
        Map<String, IngestSourceMetricsSnapshot> sourceSnapshots = new LinkedHashMap<>();
        sourceMetrics.entrySet().stream()
                .sorted(Map.Entry.comparingByKey(Comparator.naturalOrder()))
                .forEach(entry -> sourceSnapshots.put(entry.getKey(), entry.getValue().snapshot()));
        return IngestMetricsSnapshot.builder()
                .timestamp(System.currentTimeMillis())
                .totalCount(totalCount.sum())
                .acceptedCount(acceptedCount.sum())
                .rejectedCount(rejectedCount.sum())
                .errorCount(errorCount.sum())
                .avgProcessTimeMillis(average(totalProcessTimeMillis.sum(), totalCount.sum()))
                .avgProduceLatencyMillis(average(totalProduceLatencyMillis.sum(), acceptedCount.sum()))
                .nettyActiveConnectionCount(nettyActiveConnectionCount.get())
                .sourceMetrics(sourceSnapshots)
                .build();
    }

    private String normalizeSourceCode(String sourceCode) {
        String normalized = StrUtil.trim(sourceCode);
        return StrUtil.isBlank(normalized) ? UNKNOWN_SOURCE : normalized;
    }

    private long average(long total, long count) {
        if (count <= 0L) {
            return 0L;
        }
        return total / count;
    }

    private static final class SourceMetricsBucket {

        private final String sourceCode;
        private final LongAdder totalCount = new LongAdder();
        private final LongAdder acceptedCount = new LongAdder();
        private final LongAdder rejectedCount = new LongAdder();
        private final LongAdder errorCount = new LongAdder();
        private final LongAdder totalProcessTimeMillis = new LongAdder();
        private final LongAdder totalProduceLatencyMillis = new LongAdder();
        private final AtomicLong lastRequestAtMillis = new AtomicLong(0L);

        private SourceMetricsBucket(String sourceCode) {
            this.sourceCode = sourceCode;
        }

        private void recordAccepted(long processTimeMillis, long produceLatencyMillis) {
            totalCount.increment();
            acceptedCount.increment();
            totalProcessTimeMillis.add(Math.max(processTimeMillis, 0L));
            totalProduceLatencyMillis.add(Math.max(produceLatencyMillis, 0L));
            lastRequestAtMillis.set(System.currentTimeMillis());
        }

        private void recordRejected(long processTimeMillis, IngestStageEnum stage) {
            totalCount.increment();
            rejectedCount.increment();
            errorCount.increment();
            totalProcessTimeMillis.add(Math.max(processTimeMillis, 0L));
            lastRequestAtMillis.set(System.currentTimeMillis());
        }

        private IngestSourceMetricsSnapshot snapshot() {
            long accepted = acceptedCount.sum();
            long total = totalCount.sum();
            return IngestSourceMetricsSnapshot.builder()
                    .sourceCode(sourceCode)
                    .totalCount(total)
                    .acceptedCount(accepted)
                    .rejectedCount(rejectedCount.sum())
                    .errorCount(errorCount.sum())
                    .avgProcessTimeMillis(total <= 0L ? 0L : totalProcessTimeMillis.sum() / total)
                    .avgProduceLatencyMillis(accepted <= 0L ? 0L : totalProduceLatencyMillis.sum() / accepted)
                    .lastRequestAtMillis(lastRequestAtMillis.get())
                    .build();
        }

    }

}
