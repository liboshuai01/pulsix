package cn.liboshuai.pulsix.engine.feature;

import java.io.Serializable;
import java.util.Objects;

public record RedisLookupConfig(String host,
                                int port,
                                int database,
                                String user,
                                String password,
                                boolean ssl,
                                int connectTimeoutMs,
                                int defaultTimeoutMs) implements Serializable {

    public RedisLookupConfig {
        host = Objects.requireNonNullElse(host, "127.0.0.1").trim();
        if (host.isBlank()) {
            host = "127.0.0.1";
        }
        port = port <= 0 ? 6379 : port;
        database = Math.max(database, 0);
        connectTimeoutMs = connectTimeoutMs <= 0 ? 50 : connectTimeoutMs;
        defaultTimeoutMs = defaultTimeoutMs <= 0 ? 20 : defaultTimeoutMs;
    }

}
