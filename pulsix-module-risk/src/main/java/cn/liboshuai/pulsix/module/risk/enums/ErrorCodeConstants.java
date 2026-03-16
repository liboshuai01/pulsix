package cn.liboshuai.pulsix.module.risk.enums;

import cn.liboshuai.pulsix.framework.common.exception.ErrorCode;

/**
 * Risk 错误码枚举类
 *
 * risk 系统，使用 1-003-000-000 段
 */
public interface ErrorCodeConstants {

    // ========== 场景管理 1-003-000-000 ==========
    ErrorCode SCENE_NOT_EXISTS = new ErrorCode(1_003_000_000, "风控场景不存在");
    ErrorCode SCENE_CODE_DUPLICATE = new ErrorCode(1_003_000_001, "场景编码已存在");
    ErrorCode SCENE_CODE_IMMUTABLE = new ErrorCode(1_003_000_002, "场景编码创建后不允许修改");
    ErrorCode SCENE_DELETE_DENIED = new ErrorCode(1_003_000_003, "场景【{}】存在关联{}，无法删除");

}
