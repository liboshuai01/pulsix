package cn.liboshuai.pulsix.engine.model;

public final class EngineErrorCodes {

    public static final String EVENT_DESERIALIZE_FAILED = "EVENT_DESERIALIZE_FAILED";

    public static final String EVENT_VALIDATION_FAILED = "EVENT_VALIDATION_FAILED";

    public static final String SNAPSHOT_VERSION_CONFLICT = "SNAPSHOT_VERSION_CONFLICT";

    public static final String SNAPSHOT_COMPILE_FAILED = "SNAPSHOT_COMPILE_FAILED";

    public static final String SNAPSHOT_ACTIVATE_FAILED = "SNAPSHOT_ACTIVATE_FAILED";

    public static final String GROOVY_DISABLED = "GROOVY_DISABLED";

    public static final String GROOVY_SANDBOX_REJECTED = "GROOVY_SANDBOX_REJECTED";

    public static final String STATE_ACCESS_FAILED = "STATE_ACCESS_FAILED";

    public static final String STATE_TIMER_CLEANUP_FAILED = "STATE_TIMER_CLEANUP_FAILED";

    public static final String DECISION_EXECUTION_FAILED = "DECISION_EXECUTION_FAILED";

    public static final String DERIVED_EXECUTION_FAILED = "DERIVED_EXECUTION_FAILED";

    public static final String RULE_EXECUTION_FAILED = "RULE_EXECUTION_FAILED";

    public static final String DECISION_RESULT_EMIT_FAILED = "DECISION_RESULT_EMIT_FAILED";

    public static final String DECISION_LOG_EMIT_FAILED = "DECISION_LOG_EMIT_FAILED";

    private EngineErrorCodes() {
    }

}
