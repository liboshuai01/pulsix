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

}

