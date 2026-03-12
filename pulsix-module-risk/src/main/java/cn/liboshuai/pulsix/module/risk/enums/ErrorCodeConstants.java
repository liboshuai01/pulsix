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

}
