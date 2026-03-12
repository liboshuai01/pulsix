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

}
