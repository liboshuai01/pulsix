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

    // ========== 事件模型 1-003-000-100 ==========
    ErrorCode EVENT_MODEL_NOT_EXISTS = new ErrorCode(1_003_000_100, "事件模型不存在");
    ErrorCode EVENT_MODEL_CODE_DUPLICATE = new ErrorCode(1_003_000_101, "事件编码【{}】已存在");
    ErrorCode EVENT_MODEL_IDENTITY_IMMUTABLE = new ErrorCode(1_003_000_102, "事件模型创建后不允许修改场景编码或事件编码");
    ErrorCode EVENT_MODEL_DELETE_DENIED = new ErrorCode(1_003_000_103, "事件模型【{}】存在关联特征，无法删除");
    ErrorCode EVENT_MODEL_FIELD_DUPLICATE = new ErrorCode(1_003_000_104, "字段【{}】重复定义");
    ErrorCode EVENT_MODEL_FIELD_INVALID = new ErrorCode(1_003_000_105, "事件模型字段配置非法：{}");

}
