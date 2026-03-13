package cn.liboshuai.pulsix.access.ingest.enums;

import cn.liboshuai.pulsix.framework.common.exception.ErrorCode;

public interface ErrorCodeConstants {

    ErrorCode INGEST_CONFIG_NOT_READY = new ErrorCode(1_006_001_000, "接入运行时配置未就绪");
    ErrorCode INGEST_SOURCE_NOT_EXISTS = new ErrorCode(1_006_001_001, "接入源不存在：{}");
    ErrorCode INGEST_SOURCE_DISABLED = new ErrorCode(1_006_001_008, "接入源未启用：{}");
    ErrorCode INGEST_SOURCE_SCENE_NOT_ALLOWED = new ErrorCode(1_006_001_002, "接入源 {} 未开放场景 {}");
    ErrorCode INGEST_MAPPING_NOT_CONFIGURED = new ErrorCode(1_006_001_003, "接入映射未配置：sourceCode={}, sceneCode={}, eventCode={}");
    ErrorCode INGEST_EVENT_FIELDS_NOT_CONFIGURED = new ErrorCode(1_006_001_004, "事件字段未配置：sceneCode={}, eventCode={}");
    ErrorCode INGEST_TRANSPORT_UNSUPPORTED = new ErrorCode(1_006_001_005, "接入协议暂不支持：{}");
    ErrorCode INGEST_PAYLOAD_INVALID = new ErrorCode(1_006_001_006, "接入报文不合法：{}");
    ErrorCode INGEST_KAFKA_SEND_FAILED = new ErrorCode(1_006_001_007, "标准事件投递 Kafka 失败");

}
