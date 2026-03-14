-- A14 smoke setup for pulsix-access
-- 目的：为 trade_beacon_demo / trade_sdk_demo 补齐最小 smoke 数据，
--       让 HTTP / Beacon / SDK 复用同一份 TRADE_EVENT 原始样例，
--       联调时只聚焦传输协议与鉴权差异，避免把 smoke 成败绑定到两套业务口径。

DELETE FROM `ingest_source`
WHERE `source_code` = 'trade_beacon_demo';

INSERT INTO `ingest_source`
(`id`, `source_code`, `source_name`, `source_type`, `auth_type`, `auth_config_json`, `scene_scope_json`, `standard_topic_name`, `error_topic_name`, `rate_limit_qps`, `status`, `description`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
VALUES
(2143, 'trade_beacon_demo', '交易 Beacon Demo', 'BEACON', 'NONE', NULL, '["TRADE_RISK"]', 'pulsix.event.standard', 'pulsix.event.dlq', 300, 0, 'A14 Beacon 接入源样例，演示 sendBeacon 兼容入口与 NONE 鉴权。', 'admin', '2026-03-14 00:00:00', 'admin', '2026-03-14 00:00:00', b'0');

DELETE FROM `ingest_mapping_def`
WHERE `source_code` IN ('trade_beacon_demo', 'trade_sdk_demo')
  AND `scene_code` = 'TRADE_RISK'
  AND `event_code` = 'TRADE_EVENT';

INSERT INTO `ingest_mapping_def`
(`id`, `source_code`, `scene_code`, `event_code`, `source_field_path`, `target_field_code`, `target_field_name`,
 `transform_type`, `transform_expr`, `default_value`, `required_flag`, `clean_rule_json`, `sort_no`, `status`,
 `creator`, `create_time`, `updater`, `update_time`, `deleted`)
VALUES
(2161, 'trade_sdk_demo', 'TRADE_RISK', 'TRADE_EVENT', '$.event_id', 'eventId', '事件编号', 'DIRECT', NULL, NULL, 1, '{"trim":true,"blankToNull":true}', 10, 0, 'admin', '2026-03-13 00:00:00', 'admin', '2026-03-13 00:00:00', b'0'),
(2162, 'trade_sdk_demo', 'TRADE_RISK', 'TRADE_EVENT', '$.occur_time_ms', 'eventTime', '事件时间', 'TIME_MILLIS_TO_DATETIME', NULL, NULL, 1, NULL, 20, 0, 'admin', '2026-03-13 00:00:00', 'admin', '2026-03-13 00:00:00', b'0'),
(2163, 'trade_sdk_demo', 'TRADE_RISK', 'TRADE_EVENT', '$.req.traceId', 'traceId', '链路编号', 'DIRECT', NULL, NULL, 0, '{"trim":true,"blankToNull":true}', 30, 0, 'admin', '2026-03-13 00:00:00', 'admin', '2026-03-13 00:00:00', b'0'),
(2164, 'trade_sdk_demo', 'TRADE_RISK', 'TRADE_EVENT', '$.uid', 'userId', '用户编号', 'DIRECT', NULL, NULL, 1, '{"trim":true,"blankToNull":true}', 40, 0, 'admin', '2026-03-13 00:00:00', 'admin', '2026-03-13 00:00:00', b'0'),
(2165, 'trade_sdk_demo', 'TRADE_RISK', 'TRADE_EVENT', '$.dev_id', 'deviceId', '设备编号', 'DIRECT', NULL, NULL, 1, '{"trim":true,"blankToNull":true}', 50, 0, 'admin', '2026-03-13 00:00:00', 'admin', '2026-03-13 00:00:00', b'0'),
(2166, 'trade_sdk_demo', 'TRADE_RISK', 'TRADE_EVENT', '$.client_ip', 'ip', 'IP 地址', 'DIRECT', NULL, NULL, 1, '{"trim":true,"blankToNull":true}', 60, 0, 'admin', '2026-03-13 00:00:00', 'admin', '2026-03-13 00:00:00', b'0'),
(2167, 'trade_sdk_demo', 'TRADE_RISK', 'TRADE_EVENT', '$.pay_amt', 'amount', '交易金额', 'DIVIDE_100', NULL, NULL, 1, NULL, 70, 0, 'admin', '2026-03-13 00:00:00', 'admin', '2026-03-13 00:00:00', b'0'),
(2168, 'trade_sdk_demo', 'TRADE_RISK', 'TRADE_EVENT', '$.trade_result', 'result', '交易结果', 'ENUM_MAP', '{"ok":"SUCCESS","fail":"FAIL"}', NULL, 1, '{"trim":true,"lowerCase":true}', 80, 0, 'admin', '2026-03-13 00:00:00', 'admin', '2026-03-13 00:00:00', b'0'),
(2169, 'trade_beacon_demo', 'TRADE_RISK', 'TRADE_EVENT', '$.event_id', 'eventId', '事件编号', 'DIRECT', NULL, NULL, 1, '{"trim":true,"blankToNull":true}', 10, 0, 'admin', '2026-03-14 00:00:00', 'admin', '2026-03-14 00:00:00', b'0'),
(2170, 'trade_beacon_demo', 'TRADE_RISK', 'TRADE_EVENT', '$.occur_time_ms', 'eventTime', '事件时间', 'TIME_MILLIS_TO_DATETIME', NULL, NULL, 1, NULL, 20, 0, 'admin', '2026-03-14 00:00:00', 'admin', '2026-03-14 00:00:00', b'0'),
(2171, 'trade_beacon_demo', 'TRADE_RISK', 'TRADE_EVENT', '$.req.traceId', 'traceId', '链路编号', 'DIRECT', NULL, NULL, 0, '{"trim":true,"blankToNull":true}', 30, 0, 'admin', '2026-03-14 00:00:00', 'admin', '2026-03-14 00:00:00', b'0'),
(2172, 'trade_beacon_demo', 'TRADE_RISK', 'TRADE_EVENT', '$.uid', 'userId', '用户编号', 'DIRECT', NULL, NULL, 1, '{"trim":true,"blankToNull":true}', 40, 0, 'admin', '2026-03-14 00:00:00', 'admin', '2026-03-14 00:00:00', b'0'),
(2173, 'trade_beacon_demo', 'TRADE_RISK', 'TRADE_EVENT', '$.dev_id', 'deviceId', '设备编号', 'DIRECT', NULL, NULL, 1, '{"trim":true,"blankToNull":true}', 50, 0, 'admin', '2026-03-14 00:00:00', 'admin', '2026-03-14 00:00:00', b'0'),
(2174, 'trade_beacon_demo', 'TRADE_RISK', 'TRADE_EVENT', '$.client_ip', 'ip', 'IP 地址', 'DIRECT', NULL, NULL, 1, '{"trim":true,"blankToNull":true}', 60, 0, 'admin', '2026-03-14 00:00:00', 'admin', '2026-03-14 00:00:00', b'0'),
(2175, 'trade_beacon_demo', 'TRADE_RISK', 'TRADE_EVENT', '$.pay_amt', 'amount', '交易金额', 'DIVIDE_100', NULL, NULL, 1, NULL, 70, 0, 'admin', '2026-03-14 00:00:00', 'admin', '2026-03-14 00:00:00', b'0'),
(2176, 'trade_beacon_demo', 'TRADE_RISK', 'TRADE_EVENT', '$.trade_result', 'result', '交易结果', 'ENUM_MAP', '{"ok":"SUCCESS","fail":"FAIL"}', NULL, 1, '{"trim":true,"lowerCase":true}', 80, 0, 'admin', '2026-03-14 00:00:00', 'admin', '2026-03-14 00:00:00', b'0');
