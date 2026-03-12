package cn.liboshuai.pulsix.module.risk.enums;

/**
 * 风控管理端权限前缀约定。
 *
 * <p>当前阶段（S00）只落统一前缀，后续控制器可基于对应资源前缀继续拼接 query/create/update 等动作后缀。</p>
 */
public interface RiskPermissionConstants {

    String PREFIX = "risk";

    String SCENE = PREFIX + ":scene";
    String EVENT_SCHEMA = PREFIX + ":event-schema";
    String EVENT_FIELD = PREFIX + ":event-field";
    String EVENT_SAMPLE = PREFIX + ":event-sample";

    String INGEST_SOURCE = PREFIX + ":ingest-source";
    String INGEST_MAPPING = PREFIX + ":ingest-mapping";

    String LIST = PREFIX + ":list";
    String FEATURE_STREAM = PREFIX + ":feature-stream";
    String FEATURE_LOOKUP = PREFIX + ":feature-lookup";
    String FEATURE_DERIVED = PREFIX + ":feature-derived";
    String RULE = PREFIX + ":rule";
    String POLICY = PREFIX + ":policy";

    String RELEASE = PREFIX + ":release";
    String SIMULATION = PREFIX + ":simulation";

    String DECISION_LOG = PREFIX + ":decision-log";
    String INGEST_ERROR = PREFIX + ":ingest-error";
    String DASHBOARD = PREFIX + ":dashboard";
    String AUDIT_LOG = PREFIX + ":audit-log";
    String REPLAY = PREFIX + ":replay";

}

