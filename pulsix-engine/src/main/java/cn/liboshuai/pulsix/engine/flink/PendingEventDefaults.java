package cn.liboshuai.pulsix.engine.flink;

final class PendingEventDefaults {

    static final long DEFAULT_PENDING_RETRY_DELAY_MS = 1_000L;

    static final int DEFAULT_MAX_PENDING_EVENTS_PER_KEY = 1_024;

    static final long DEFAULT_MAX_PENDING_EVENT_AGE_MS = 300_000L;

    private PendingEventDefaults() {
    }

}
