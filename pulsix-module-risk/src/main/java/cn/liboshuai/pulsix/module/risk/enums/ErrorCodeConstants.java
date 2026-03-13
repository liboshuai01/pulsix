package cn.liboshuai.pulsix.module.risk.enums;

import cn.liboshuai.pulsix.framework.common.exception.ErrorCode;

/**
 * Risk 错误码枚举类。
 *
 * risk 模块，使用 1-005-000-000 段
 */
public interface ErrorCodeConstants {

    // ========== 场景管理 1-005-001-000 ==========
    ErrorCode SCENE_NOT_EXISTS = new ErrorCode(1_005_001_000, "场景不存在");
    ErrorCode SCENE_CODE_DUPLICATE = new ErrorCode(1_005_001_001, "场景编码已存在");

    // ========== 事件 Schema 管理 1-005-002-000 ==========
    ErrorCode EVENT_SCHEMA_NOT_EXISTS = new ErrorCode(1_005_002_000, "事件 Schema 不存在");
    ErrorCode EVENT_SCHEMA_CODE_DUPLICATE = new ErrorCode(1_005_002_001, "当前场景下事件编码已存在");

    // ========== 事件字段管理 1-005-003-000 ==========
    ErrorCode EVENT_FIELD_NOT_EXISTS = new ErrorCode(1_005_003_000, "事件字段不存在");
    ErrorCode EVENT_FIELD_CODE_DUPLICATE = new ErrorCode(1_005_003_001, "当前事件下字段编码已存在");

    // ========== 事件样例管理 1-005-004-000 ==========
    ErrorCode EVENT_SAMPLE_NOT_EXISTS = new ErrorCode(1_005_004_000, "事件样例不存在");
    ErrorCode EVENT_SAMPLE_CODE_DUPLICATE = new ErrorCode(1_005_004_001, "当前事件下样例编码已存在");

    // ========== 接入源管理 1-005-005-000 ==========
    ErrorCode INGEST_SOURCE_NOT_EXISTS = new ErrorCode(1_005_005_000, "接入源不存在");
    ErrorCode INGEST_SOURCE_CODE_DUPLICATE = new ErrorCode(1_005_005_001, "接入源编码已存在");

    // ========== 接入字段映射管理 1-005-006-000 ==========
    ErrorCode INGEST_MAPPING_NOT_EXISTS = new ErrorCode(1_005_006_000, "接入字段映射不存在");
    ErrorCode INGEST_MAPPING_KEY_DUPLICATE = new ErrorCode(1_005_006_001, "当前接入源与事件下目标字段映射已存在");

    // ========== 名单中心 1-005-007-000 ==========
    ErrorCode LIST_SET_NOT_EXISTS = new ErrorCode(1_005_007_000, "名单集合不存在");
    ErrorCode LIST_SET_CODE_DUPLICATE = new ErrorCode(1_005_007_001, "当前场景下名单编码已存在");
    ErrorCode LIST_ITEM_NOT_EXISTS = new ErrorCode(1_005_007_002, "名单条目不存在");
    ErrorCode LIST_ITEM_VALUE_DUPLICATE = new ErrorCode(1_005_007_003, "当前名单下匹配值已存在");
    ErrorCode LIST_SYNC_FAILED = new ErrorCode(1_005_007_004, "同步 Redis 失败");

    // ========== 流式特征 1-005-008-000 ==========
    ErrorCode ENTITY_TYPE_NOT_EXISTS = new ErrorCode(1_005_008_000, "实体类型不存在");
    ErrorCode FEATURE_STREAM_NOT_EXISTS = new ErrorCode(1_005_008_001, "流式特征不存在");
    ErrorCode FEATURE_STREAM_CODE_DUPLICATE = new ErrorCode(1_005_008_002, "当前场景下特征编码已存在");

    // ========== 查询特征 1-005-009-000 ==========
    ErrorCode FEATURE_LOOKUP_NOT_EXISTS = new ErrorCode(1_005_009_000, "查询特征不存在");
    ErrorCode FEATURE_LOOKUP_CODE_DUPLICATE = new ErrorCode(1_005_009_001, "当前场景下特征编码已存在");

    // ========== 派生特征 1-005-010-000 ==========
    ErrorCode FEATURE_DERIVED_NOT_EXISTS = new ErrorCode(1_005_010_000, "派生特征不存在");
    ErrorCode FEATURE_DERIVED_CODE_DUPLICATE = new ErrorCode(1_005_010_001, "当前场景下特征编码已存在");
    ErrorCode FEATURE_DERIVED_DEPENDENCY_INVALID = new ErrorCode(1_005_010_002, "派生特征依赖无效：{}");
    ErrorCode FEATURE_DERIVED_DEPENDENCY_CYCLE = new ErrorCode(1_005_010_003, "派生特征依赖存在循环：{}");
    ErrorCode FEATURE_DERIVED_EXPR_INVALID = new ErrorCode(1_005_010_004, "派生特征表达式校验失败：{}");

    // ========== 规则中心 1-005-011-000 ==========
    ErrorCode RULE_NOT_EXISTS = new ErrorCode(1_005_011_000, "规则不存在");
    ErrorCode RULE_CODE_DUPLICATE = new ErrorCode(1_005_011_001, "当前场景下规则编码已存在");
    ErrorCode RULE_EXPR_INVALID = new ErrorCode(1_005_011_002, "规则表达式校验失败：{}");
    ErrorCode RULE_HIT_REASON_INVALID = new ErrorCode(1_005_011_003, "命中原因模板包含未知占位符：{}");

    // ========== 策略中心 1-005-012-000 ==========
    ErrorCode POLICY_NOT_EXISTS = new ErrorCode(1_005_012_000, "策略不存在");
    ErrorCode POLICY_CODE_DUPLICATE = new ErrorCode(1_005_012_001, "当前场景下策略编码已存在");
    ErrorCode POLICY_RULE_INVALID = new ErrorCode(1_005_012_002, "策略引用的规则不存在：{}");
    ErrorCode POLICY_SCORE_BAND_INVALID = new ErrorCode(1_005_012_003, "策略评分段配置不合法：{}");

    // ========== 发布预检 1-005-013-000 ==========
    ErrorCode SCENE_RELEASE_NOT_EXISTS = new ErrorCode(1_005_013_000, "发布记录不存在");
    ErrorCode SCENE_RELEASE_VALIDATION_FAILED = new ErrorCode(1_005_013_001, "发布记录未通过预检，不能执行发布操作");
    ErrorCode SCENE_RELEASE_STATUS_INVALID = new ErrorCode(1_005_013_002, "发布记录当前状态不允许执行{}");
    ErrorCode SCENE_RELEASE_ROLLBACK_SOURCE_INVALID = new ErrorCode(1_005_013_003, "仅正式发布历史版本支持作为回滚来源");
    ErrorCode SCENE_RELEASE_ALREADY_ACTIVE = new ErrorCode(1_005_013_004, "所选版本已是当前生效版本，无需回滚");

    // ========== 仿真测试 1-005-015-000 ==========
    ErrorCode SIMULATION_CASE_NOT_EXISTS = new ErrorCode(1_005_015_000, "仿真用例不存在");
    ErrorCode SIMULATION_CASE_CODE_DUPLICATE = new ErrorCode(1_005_015_001, "当前场景下仿真用例编码已存在");
    ErrorCode SIMULATION_CASE_CONFIG_INVALID = new ErrorCode(1_005_015_002, "仿真用例配置不合法：{}");
    ErrorCode SIMULATION_RELEASE_NOT_AVAILABLE = new ErrorCode(1_005_015_003, "未找到可用于仿真的场景版本");
    ErrorCode SIMULATION_REPORT_NOT_EXISTS = new ErrorCode(1_005_015_004, "仿真报告不存在");

    // ========== 决策日志 1-005-016-000 ==========
    ErrorCode DECISION_LOG_NOT_EXISTS = new ErrorCode(1_005_016_000, "决策日志不存在");

    // ========== 接入异常 1-005-018-000 ==========
    ErrorCode INGEST_ERROR_LOG_NOT_EXISTS = new ErrorCode(1_005_018_000, "接入异常记录不存在");

}
