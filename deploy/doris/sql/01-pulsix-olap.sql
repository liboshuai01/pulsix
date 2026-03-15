CREATE TABLE IF NOT EXISTS dwd_decision_result (
  dt DATE NOT NULL COMMENT '业务日期',
  trace_id VARCHAR(64) NOT NULL COMMENT '链路号',
  event_id VARCHAR(128) NOT NULL COMMENT '事件号',
  scene_code VARCHAR(64) NOT NULL COMMENT '场景编码',
  version_no INT NOT NULL COMMENT '运行版本号',
  final_action VARCHAR(32) NOT NULL COMMENT '最终动作',
  final_score INT NULL COMMENT '最终分数',
  hit_rule_codes STRING NULL COMMENT '命中规则编码 JSON 数组',
  event_time DATETIME NULL COMMENT '事件时间',
  decision_time DATETIME NOT NULL COMMENT '决策时间',
  latency_ms BIGINT NULL COMMENT '决策耗时',
  raw_json STRING NULL COMMENT '原始消息 JSON'
)
DUPLICATE KEY(dt, trace_id, event_id)
DISTRIBUTED BY HASH(trace_id) BUCKETS 8
PROPERTIES (
  "replication_num" = "1"
);

CREATE TABLE IF NOT EXISTS dwd_decision_log (
  dt DATE NOT NULL COMMENT '业务日期',
  trace_id VARCHAR(64) NOT NULL COMMENT '链路号',
  event_id VARCHAR(128) NOT NULL COMMENT '事件号',
  scene_code VARCHAR(64) NOT NULL COMMENT '场景编码',
  source_code VARCHAR(64) NULL COMMENT '接入源编码',
  event_code VARCHAR(64) NOT NULL COMMENT '事件编码',
  entity_id VARCHAR(128) NULL COMMENT '主实体编号',
  policy_code VARCHAR(64) NOT NULL COMMENT '策略编码',
  final_action VARCHAR(32) NOT NULL COMMENT '最终动作',
  final_score INT NULL COMMENT '最终分值',
  version_no INT NOT NULL COMMENT '版本号',
  latency_ms BIGINT NULL COMMENT '决策耗时',
  event_time DATETIME NOT NULL COMMENT '事件时间',
  decision_time DATETIME NOT NULL COMMENT '决策落库时间',
  input_json STRING NULL COMMENT '标准事件 JSON',
  feature_snapshot_json STRING NULL COMMENT '特征快照 JSON',
  hit_rules_json STRING NULL COMMENT '命中规则摘要 JSON',
  decision_detail_json STRING NULL COMMENT '决策明细 JSON'
)
DUPLICATE KEY(dt, trace_id, event_id)
DISTRIBUTED BY HASH(trace_id) BUCKETS 8
PROPERTIES (
  "replication_num" = "1"
);

CREATE TABLE IF NOT EXISTS dwd_rule_hit_log (
  dt DATE NOT NULL COMMENT '业务日期',
  trace_id VARCHAR(64) NOT NULL COMMENT '链路号',
  event_id VARCHAR(128) NOT NULL COMMENT '事件号',
  rule_code VARCHAR(64) NOT NULL COMMENT '规则编码',
  scene_code VARCHAR(64) NOT NULL COMMENT '场景编码',
  version_no INT NOT NULL COMMENT '版本号',
  rule_name VARCHAR(128) NOT NULL COMMENT '规则名称',
  rule_order_no INT NOT NULL COMMENT '执行顺序',
  hit_flag TINYINT NOT NULL COMMENT '是否命中',
  hit_reason VARCHAR(1024) NULL COMMENT '命中原因',
  score INT NULL COMMENT '规则贡献分',
  event_time DATETIME NULL COMMENT '事件时间',
  decision_time DATETIME NULL COMMENT '决策时间',
  hit_value_json STRING NULL COMMENT '命中值快照 JSON'
)
DUPLICATE KEY(dt, trace_id, event_id, rule_code)
DISTRIBUTED BY HASH(trace_id) BUCKETS 8
PROPERTIES (
  "replication_num" = "1"
);

CREATE TABLE IF NOT EXISTS dwd_ingest_error_log (
  dt DATE NOT NULL COMMENT '业务日期',
  trace_id VARCHAR(64) NOT NULL COMMENT '链路号',
  raw_event_id VARCHAR(128) NULL COMMENT '原始事件号',
  source_code VARCHAR(64) NOT NULL COMMENT '接入源编码',
  scene_code VARCHAR(64) NULL COMMENT '场景编码',
  event_code VARCHAR(64) NULL COMMENT '事件编码',
  ingest_stage VARCHAR(32) NOT NULL COMMENT '异常阶段',
  error_code VARCHAR(64) NOT NULL COMMENT '错误编码',
  error_message VARCHAR(1024) NOT NULL COMMENT '错误信息',
  error_topic_name VARCHAR(128) NULL COMMENT '错误 topic',
  reprocess_status VARCHAR(32) NOT NULL COMMENT '重处理状态',
  status TINYINT NOT NULL COMMENT '记录状态',
  occur_time DATETIME NOT NULL COMMENT '发生时间',
  raw_payload_json STRING NULL COMMENT '原始报文 JSON',
  standard_payload_json STRING NULL COMMENT '标准化后报文 JSON'
)
DUPLICATE KEY(dt, trace_id, raw_event_id)
DISTRIBUTED BY HASH(trace_id) BUCKETS 4
PROPERTIES (
  "replication_num" = "1"
);

CREATE TABLE IF NOT EXISTS dwd_engine_error_log (
  dt DATE NOT NULL COMMENT '业务日期',
  trace_id VARCHAR(64) NOT NULL COMMENT '链路号',
  event_id VARCHAR(128) NULL COMMENT '事件号',
  error_code VARCHAR(64) NOT NULL COMMENT '错误编码',
  scene_code VARCHAR(64) NULL COMMENT '场景编码',
  event_code VARCHAR(64) NULL COMMENT '事件编码',
  version_no INT NULL COMMENT '运行版本号',
  error_stage VARCHAR(32) NOT NULL COMMENT '错误阶段',
  error_type VARCHAR(32) NOT NULL COMMENT '错误类型',
  error_message VARCHAR(1024) NOT NULL COMMENT '错误信息',
  rule_code VARCHAR(64) NULL COMMENT '规则编码',
  script_type VARCHAR(32) NULL COMMENT '脚本类型',
  source_ref VARCHAR(128) NULL COMMENT '来源引用',
  occur_time DATETIME NOT NULL COMMENT '发生时间',
  event_json STRING NULL COMMENT '事件快照 JSON',
  feature_snapshot_json STRING NULL COMMENT '特征快照 JSON'
)
DUPLICATE KEY(dt, trace_id, event_id, error_code)
DISTRIBUTED BY HASH(trace_id) BUCKETS 4
PROPERTIES (
  "replication_num" = "1"
);
