package cn.liboshuai.pulsix.access.ingest.enums;

import cn.liboshuai.pulsix.framework.common.exception.ErrorCode;

public interface ErrorCodeConstants {

    ErrorCode INGEST_CONFIG_NOT_READY = new ErrorCode(1_006_001_000, "接入运行时配置未就绪");
    ErrorCode INGEST_SOURCE_DISABLED = new ErrorCode(1_006_001_001, "接入源未启用或不存在");
    ErrorCode INGEST_TRANSPORT_UNSUPPORTED = new ErrorCode(1_006_001_002, "接入协议暂不支持：{}");
    ErrorCode INGEST_PAYLOAD_INVALID = new ErrorCode(1_006_001_003, "接入报文不合法：{}");
    ErrorCode INGEST_KAFKA_SEND_FAILED = new ErrorCode(1_006_001_004, "标准事件投递 Kafka 失败");

}
