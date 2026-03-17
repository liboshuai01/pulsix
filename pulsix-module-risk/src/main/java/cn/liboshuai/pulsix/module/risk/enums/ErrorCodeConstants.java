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
    ErrorCode EVENT_MODEL_IDENTITY_IMMUTABLE = new ErrorCode(1_003_000_102, "事件模型创建后不允许修改场景编码、事件编码或事件类型");
    ErrorCode EVENT_MODEL_DELETE_DENIED = new ErrorCode(1_003_000_103, "事件模型【{}】存在关联特征，无法删除");
    ErrorCode EVENT_MODEL_FIELD_DUPLICATE = new ErrorCode(1_003_000_104, "字段【{}】重复定义");
    ErrorCode EVENT_MODEL_FIELD_INVALID = new ErrorCode(1_003_000_105, "事件模型字段配置非法：{}");
    ErrorCode EVENT_MODEL_BINDING_REQUIRED = new ErrorCode(1_003_000_106, "事件模型至少需要绑定一个接入源");
    ErrorCode EVENT_MODEL_BINDING_DUPLICATE = new ErrorCode(1_003_000_107, "接入源【{}】重复绑定");
    ErrorCode EVENT_MODEL_BINDING_SOURCE_NOT_EXISTS = new ErrorCode(1_003_000_108, "接入源【{}】不存在");
    ErrorCode EVENT_MODEL_BINDING_SCENE_MISMATCH = new ErrorCode(1_003_000_109, "接入源【{}】未开放当前场景【{}】");

    // ========== 接入源管理 1-003-000-200 ==========
    ErrorCode ACCESS_SOURCE_NOT_EXISTS = new ErrorCode(1_003_000_200, "接入源不存在");
    ErrorCode ACCESS_SOURCE_CODE_DUPLICATE = new ErrorCode(1_003_000_201, "接入源编码【{}】已存在");
    ErrorCode ACCESS_SOURCE_CODE_IMMUTABLE = new ErrorCode(1_003_000_202, "接入源创建后不允许修改接入源编码");
    ErrorCode ACCESS_SOURCE_DELETE_DENIED = new ErrorCode(1_003_000_203, "接入源【{}】存在关联事件模型，无法删除");
    ErrorCode ACCESS_SOURCE_ALLOWED_SCENE_INVALID = new ErrorCode(1_003_000_204, "接入源允许场景【{}】不存在");
    ErrorCode ACCESS_SOURCE_ALLOWED_SCENE_REQUIRED = new ErrorCode(1_003_000_205, "接入源至少需要配置一个允许场景");
    ErrorCode ACCESS_SOURCE_ALLOWED_SCENE_CONFLICT = new ErrorCode(1_003_000_206, "接入源【{}】已绑定场景【{}】的事件，不能移除该允许场景");

}
