package cn.liboshuai.pulsix.access.ingest.service.config;

import cn.hutool.core.util.StrUtil;
import cn.liboshuai.pulsix.access.ingest.config.PulsixIngestProperties;
import cn.liboshuai.pulsix.access.ingest.domain.config.EventFieldConfig;
import cn.liboshuai.pulsix.access.ingest.domain.config.IngestMappingConfig;
import cn.liboshuai.pulsix.access.ingest.domain.config.IngestRuntimeConfig;
import cn.liboshuai.pulsix.access.ingest.domain.config.IngestSourceConfig;
import cn.liboshuai.pulsix.framework.common.enums.CommonStatusEnum;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static cn.liboshuai.pulsix.access.ingest.enums.ErrorCodeConstants.INGEST_EVENT_FIELDS_NOT_CONFIGURED;
import static cn.liboshuai.pulsix.access.ingest.enums.ErrorCodeConstants.INGEST_MAPPING_NOT_CONFIGURED;
import static cn.liboshuai.pulsix.access.ingest.enums.ErrorCodeConstants.INGEST_SOURCE_DISABLED;
import static cn.liboshuai.pulsix.access.ingest.enums.ErrorCodeConstants.INGEST_SOURCE_NOT_EXISTS;
import static cn.liboshuai.pulsix.access.ingest.enums.ErrorCodeConstants.INGEST_SOURCE_SCENE_NOT_ALLOWED;
import static cn.liboshuai.pulsix.framework.common.exception.util.ServiceExceptionUtil.exception;

@Service
public class CachingIngestDesignConfigService implements IngestDesignConfigService {

    @Resource
    private IngestDesignConfigRepository repository;

    @Resource
    private Clock clock;

    @Resource
    private PulsixIngestProperties properties;

    private final ConcurrentMap<String, CachedSource> sourceCache = new ConcurrentHashMap<>();
    private final ConcurrentMap<ConfigCacheKey, CachedConfig> configCache = new ConcurrentHashMap<>();

    @Override
    public IngestRuntimeConfig getConfig(String sourceCode, String sceneCode, String eventCode) {
        ConfigCacheKey cacheKey = new ConfigCacheKey(normalize(sourceCode), normalize(sceneCode), normalize(eventCode));
        long now = clock.millis();
        CachedConfig cachedConfig = configCache.compute(cacheKey, (ignored, existing) -> {
            if (existing != null && !existing.expired(now, ttlMillis())) {
                return existing;
            }
            return new CachedConfig(loadConfig(cacheKey, now), now);
        });
        return cachedConfig.value();
    }

    @Override
    public void invalidate(String sourceCode, String sceneCode, String eventCode) {
        configCache.remove(new ConfigCacheKey(normalize(sourceCode), normalize(sceneCode), normalize(eventCode)));
        sourceCache.remove(normalize(sourceCode));
    }

    @Override
    public void clear() {
        configCache.clear();
        sourceCache.clear();
    }

    private IngestRuntimeConfig loadConfig(ConfigCacheKey cacheKey, long now) {
        IngestSourceConfig sourceConfig = loadSource(cacheKey.sourceCode(), now);
        validateSource(sourceConfig, cacheKey.sceneCode());

        List<IngestMappingConfig> mappings = repository.findEnabledMappings(cacheKey.sourceCode(), cacheKey.sceneCode(), cacheKey.eventCode());
        if (mappings.isEmpty()) {
            throw exception(INGEST_MAPPING_NOT_CONFIGURED, cacheKey.sourceCode(), cacheKey.sceneCode(), cacheKey.eventCode());
        }

        List<EventFieldConfig> eventFields = repository.findEventFields(cacheKey.sceneCode(), cacheKey.eventCode());
        if (eventFields.isEmpty()) {
            throw exception(INGEST_EVENT_FIELDS_NOT_CONFIGURED, cacheKey.sceneCode(), cacheKey.eventCode());
        }

        return IngestRuntimeConfig.builder()
                .sourceCode(cacheKey.sourceCode())
                .sceneCode(cacheKey.sceneCode())
                .eventCode(cacheKey.eventCode())
                .source(sourceConfig)
                .mappings(List.copyOf(mappings))
                .eventFields(List.copyOf(eventFields))
                .loadedAt(Instant.ofEpochMilli(now))
                .build();
    }

    private IngestSourceConfig loadSource(String sourceCode, long now) {
        CachedSource cachedSource = sourceCache.compute(sourceCode, (ignored, existing) -> {
            if (existing != null && !existing.expired(now, ttlMillis())) {
                return existing;
            }
            IngestSourceConfig source = repository.findSource(sourceCode)
                    .orElseThrow(() -> exception(INGEST_SOURCE_NOT_EXISTS, sourceCode));
            return new CachedSource(source, now);
        });
        return cachedSource.value();
    }

    private void validateSource(IngestSourceConfig sourceConfig, String sceneCode) {
        if (!CommonStatusEnum.isEnable(sourceConfig.getStatus())) {
            throw exception(INGEST_SOURCE_DISABLED, sourceConfig.getSourceCode());
        }
        if (!sourceConfig.supportsScene(sceneCode)) {
            throw exception(INGEST_SOURCE_SCENE_NOT_ALLOWED, sourceConfig.getSourceCode(), sceneCode);
        }
    }

    private long ttlMillis() {
        return Math.max(1, properties.getConfigCache().getRefreshIntervalSeconds()) * 1000L;
    }

    private String normalize(String value) {
        return StrUtil.blankToDefault(StrUtil.trim(value), "");
    }

    private record ConfigCacheKey(String sourceCode, String sceneCode, String eventCode) {
    }

    private record CachedSource(IngestSourceConfig value, long loadedAtMillis) {

        private boolean expired(long now, long ttlMillis) {
            return now - loadedAtMillis >= ttlMillis;
        }

    }

    private record CachedConfig(IngestRuntimeConfig value, long loadedAtMillis) {

        private boolean expired(long now, long ttlMillis) {
            return now - loadedAtMillis >= ttlMillis;
        }

    }

}
