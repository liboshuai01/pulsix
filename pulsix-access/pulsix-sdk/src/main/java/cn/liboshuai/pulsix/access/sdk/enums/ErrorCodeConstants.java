package cn.liboshuai.pulsix.access.sdk.enums;

import cn.liboshuai.pulsix.framework.common.exception.ErrorCode;

public interface ErrorCodeConstants {

    ErrorCode SDK_OPTIONS_INVALID = new ErrorCode(1_006_002_000, "SDK 配置不合法：{}");
    ErrorCode SDK_CLIENT_NOT_STARTED = new ErrorCode(1_006_002_001, "SDK 客户端尚未启动");
    ErrorCode SDK_REQUEST_INVALID = new ErrorCode(1_006_002_002, "SDK 请求不合法：{}");
    ErrorCode SDK_TRANSPORT_NOT_READY = new ErrorCode(1_006_002_003, "SDK 连接尚未就绪");
    ErrorCode SDK_SEND_FAILED = new ErrorCode(1_006_002_004, "SDK 发送失败");
    ErrorCode SDK_BUFFER_FULL = new ErrorCode(1_006_002_005, "SDK 内存缓冲已满");
    ErrorCode SDK_RETRY_EXHAUSTED = new ErrorCode(1_006_002_006, "SDK 重试次数已耗尽");

}
