package cn.liboshuai.pulsix.access.ingest.service.ratelimit;

import cn.hutool.core.util.StrUtil;
import cn.liboshuai.pulsix.access.ingest.domain.config.IngestRuntimeConfig;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static cn.liboshuai.pulsix.access.ingest.enums.ErrorCodeConstants.INGEST_RATE_LIMIT_EXCEEDED;

@Service
public class InMemoryIngestRateLimitService implements IngestRateLimitService {

    private final ConcurrentMap<String, SourceWindowCounter> counters = new ConcurrentHashMap<>();

    @Resource
    private Clock clock;

    @Override
    public void checkAllowed(IngestRuntimeConfig runtimeConfig) {
        if (runtimeConfig == null || runtimeConfig.getSource() == null) {
            return;
        }
        Integer rateLimitQps = runtimeConfig.getSource().getRateLimitQps();
        if (rateLimitQps == null || rateLimitQps <= 0) {
            return;
        }
        String sourceCode = normalizeSourceCode(runtimeConfig.getSourceCode(), runtimeConfig.getSource().getSourceCode());
        long currentEpochSecond = resolveClock().instant().getEpochSecond();
        SourceWindowCounter counter = counters.computeIfAbsent(sourceCode, ignored -> new SourceWindowCounter());
        if (!counter.tryAcquire(currentEpochSecond, rateLimitQps)) {
            throw new IngestRateLimitException(INGEST_RATE_LIMIT_EXCEEDED.getCode(),
                    "SOURCE_RATE_LIMIT_EXCEEDED",
                    "接入源请求频率超出限制: sourceCode=" + sourceCode + ", qps=" + rateLimitQps);
        }
    }

    private Clock resolveClock() {
        return clock == null ? Clock.systemUTC() : clock;
    }

    private String normalizeSourceCode(String... values) {
        for (String value : values) {
            String normalized = StrUtil.trim(value);
            if (StrUtil.isNotBlank(normalized)) {
                return normalized;
            }
        }
        return "_UNKNOWN";
    }

    private static final class SourceWindowCounter {

        private long windowEpochSecond = Long.MIN_VALUE;
        private int currentCount = 0;

        private synchronized boolean tryAcquire(long epochSecond, int limit) {
            if (windowEpochSecond != epochSecond) {
                windowEpochSecond = epochSecond;
                currentCount = 0;
            }
            if (currentCount >= limit) {
                return false;
            }
            currentCount++;
            return true;
        }

    }

}
