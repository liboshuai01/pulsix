package cn.liboshuai.pulsix.engine.feature;

import cn.liboshuai.pulsix.engine.context.EvalContext;
import cn.liboshuai.pulsix.engine.model.LookupFeatureSpec;
import cn.liboshuai.pulsix.engine.model.LookupType;
import cn.liboshuai.pulsix.engine.runtime.CompiledSceneRuntime;
import cn.liboshuai.pulsix.engine.support.ValueConverter;
import redis.clients.jedis.DefaultJedisClientConfig;
import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.exceptions.JedisConnectionException;

import java.net.SocketTimeoutException;
import java.time.Clock;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

public class RedisLookupService implements LookupService {

    private final RedisLookupConfig config;

    private final Clock clock;

    private final Map<CacheKey, CacheEntry> cache = new ConcurrentHashMap<>();

    public RedisLookupService(RedisLookupConfig config) {
        this(config, Clock.systemUTC());
    }

    RedisLookupService(RedisLookupConfig config, Clock clock) {
        this.config = Objects.requireNonNull(config, "config");
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    @Override
    public Object lookup(LookupType lookupType, String sourceRef, String key) {
        if (lookupType == null || sourceRef == null || sourceRef.isBlank() || key == null || key.isBlank()) {
            return null;
        }
        return fetchFromRedis(lookupType, sourceRef, key, config.defaultTimeoutMs());
    }

    @Override
    public LookupResult lookup(CompiledSceneRuntime.CompiledLookupFeature feature, EvalContext context) {
        if (feature == null || feature.getSpec() == null) {
            return LookupResult.success(null, null);
        }
        LookupFeatureSpec spec = feature.getSpec();
        String lookupKey = feature.getKeyScript() == null ? null : ValueConverter.asString(feature.getKeyScript().execute(context));
        Object defaultValue = ValueConverter.coerce(spec.getDefaultValue(), spec.getValueType());
        if (lookupKey == null || lookupKey.isBlank()) {
            return LookupResult.fallback(defaultValue,
                    lookupKey,
                    LookupResult.ERROR_KEY_MISSING,
                    "lookup key is blank for feature: " + spec.getCode(),
                    LookupResult.FALLBACK_DEFAULT_VALUE);
        }

        CacheKey cacheKey = new CacheKey(spec.getLookupType(), spec.getSourceRef(), lookupKey, spec.getValueType());
        CacheEntry cacheEntry = cache.get(cacheKey);
        long now = clock.millis();
        if (isFresh(cacheEntry, now)) {
            return LookupResult.success(cacheEntry.value(), lookupKey);
        }

        try {
            Object rawValue = fetchFromRedis(spec.getLookupType(), spec.getSourceRef(), lookupKey, resolveTimeoutMs(spec));
            if (rawValue != null) {
                Object resolvedValue = ValueConverter.coerce(rawValue, spec.getValueType());
                storeCache(cacheKey, resolvedValue, spec.getCacheTtlSeconds(), now);
                return LookupResult.success(resolvedValue, lookupKey);
            }
            return LookupResult.fallback(defaultValue,
                    lookupKey,
                    LookupResult.ERROR_VALUE_MISSING,
                    "lookup value not found for sourceRef=" + spec.getSourceRef(),
                    LookupResult.FALLBACK_DEFAULT_VALUE);
        } catch (JedisConnectionException exception) {
            return failureFallback(cacheEntry,
                    defaultValue,
                    lookupKey,
                    isTimeout(exception) ? LookupResult.ERROR_TIMEOUT : LookupResult.ERROR_CONNECTION_FAILED,
                    exception);
        } catch (RuntimeException exception) {
            return failureFallback(cacheEntry,
                    defaultValue,
                    lookupKey,
                    isTimeout(exception) ? LookupResult.ERROR_TIMEOUT : LookupResult.ERROR_CONNECTION_FAILED,
                    exception);
        }
    }

    private LookupResult failureFallback(CacheEntry cacheEntry,
                                         Object defaultValue,
                                         String lookupKey,
                                         String errorCode,
                                         Throwable throwable) {
        if (cacheEntry != null) {
            return LookupResult.fallback(cacheEntry.value(),
                    lookupKey,
                    errorCode,
                    errorMessageOf(throwable),
                    LookupResult.FALLBACK_CACHE_VALUE);
        }
        return LookupResult.fallback(defaultValue,
                lookupKey,
                errorCode,
                errorMessageOf(throwable),
                LookupResult.FALLBACK_DEFAULT_VALUE);
    }

    private Object fetchFromRedis(LookupType lookupType,
                                  String sourceRef,
                                  String key,
                                  int timeoutMs) {
        if (lookupType == null || sourceRef == null || sourceRef.isBlank() || key == null || key.isBlank()) {
            return null;
        }
        try (Jedis jedis = new Jedis(new HostAndPort(config.host(), config.port()), clientConfig(timeoutMs))) {
            return switch (lookupType) {
                case REDIS_SET -> lookupRedisSet(jedis, sourceRef, key);
                case REDIS_HASH, DICT -> jedis.hget(sourceRef, key);
                case REDIS_STRING -> jedis.get(composeKey(sourceRef, key));
            };
        }
    }

    private DefaultJedisClientConfig clientConfig(int timeoutMs) {
        DefaultJedisClientConfig.Builder builder = DefaultJedisClientConfig.builder()
                .connectionTimeoutMillis(config.connectTimeoutMs())
                .socketTimeoutMillis(timeoutMs)
                .database(config.database())
                .ssl(config.ssl());
        if (config.user() != null && !config.user().isBlank()) {
            builder.user(config.user());
        }
        if (config.password() != null && !config.password().isBlank()) {
            builder.password(config.password());
        }
        return builder.build();
    }

    private Object lookupRedisSet(Jedis jedis, String sourceRef, String key) {
        if (jedis.exists(composeKey(sourceRef, key))) {
            return Boolean.TRUE;
        }
        return jedis.sismember(sourceRef, key);
    }

    private boolean isFresh(CacheEntry cacheEntry, long now) {
        return cacheEntry != null && now <= cacheEntry.expiresAtMs();
    }

    private void storeCache(CacheKey cacheKey, Object value, Integer cacheTtlSeconds, long now) {
        long ttlMs = cacheTtlSeconds == null || cacheTtlSeconds <= 0 ? 0L : cacheTtlSeconds.longValue() * 1_000L;
        if (ttlMs <= 0L) {
            cache.remove(cacheKey);
            return;
        }
        cache.put(cacheKey, new CacheEntry(value, now + ttlMs));
    }

    private int resolveTimeoutMs(LookupFeatureSpec spec) {
        if (spec == null || spec.getTimeoutMs() == null || spec.getTimeoutMs() <= 0) {
            return config.defaultTimeoutMs();
        }
        return spec.getTimeoutMs();
    }

    private String composeKey(String sourceRef, String key) {
        return sourceRef.endsWith(":") ? sourceRef + key : sourceRef + ':' + key;
    }

    private boolean isTimeout(Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            if (current instanceof SocketTimeoutException) {
                return true;
            }
            current = current.getCause();
        }
        String message = throwable.getMessage();
        return message != null && message.toLowerCase(Locale.ROOT).contains("timed out");
    }

    private String errorMessageOf(Throwable throwable) {
        if (throwable == null) {
            return null;
        }
        if (throwable.getMessage() != null && !throwable.getMessage().isBlank()) {
            return throwable.getMessage();
        }
        return throwable.getClass().getSimpleName();
    }

    private record CacheKey(LookupType lookupType, String sourceRef, String key, String valueType) {
    }

    private record CacheEntry(Object value, long expiresAtMs) {
    }

}
