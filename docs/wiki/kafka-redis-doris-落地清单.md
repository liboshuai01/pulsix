# Kafka / Redis / Doris 落地清单（不含 MySQL）

## 1. 说明

- 本文基于以下文档整理：
  - `docs/wiki/项目架构及技术栈.md`
  - `docs/wiki/接入层设计.md`
  - `docs/参考资料/实时风控系统第21章：数据库、Redis、Kafka 的落地模型设计.md`
  - `docs/参考资料/实时风控系统第24章：部署、监控、性能优化与开源包装.md`
  - `docs/参考资料/实时风控系统附录D：第一版 DDL 与 Redis Key 设计.md`
  - `docs/参考资料/实时风控系统附录E：三大风控场景的样例规则与样例事件.md`
  - `docs/参考资料/实时风控系统附录A：完整快照 JSON 示例.md`
  - `docs/参考资料/实时风控系统附录C：Flink 核心伪代码.md`
  - `docs/sql/pulsix-risk.sql`
- 文档中已经明确给出的内容：`topic` 名称、流向职责、Redis Key 模式、Doris 承载的查询模型范围、示例事件、日志表字段。
- 文档中没有写死、本文补充的建议值：Kafka 分区数、副本数、Redis TTL 的具体时间、Doris 的表名与建表 SQL。
- 当前建议默认面向“一期 MVP / Docker 演示环境”：
  - Kafka 单 broker：副本数统一按 `1`
  - 如果是 3 broker 生产集群，可把所有 topic 的副本数改为 `3`
- 当前系统**不建议创建** `pulsix.config.snapshot` topic。配置同步链路按文档约定走：`scene_release -> MySQL CDC -> Flink`。

## 2. Kafka Topic 清单

## 2.1 建议创建的 Topic

| Topic | 是否必建 | 推荐分区数 | 推荐副本数 | 推荐消息 Key | 生产者 | 主要消费者/下游 | 说明 |
| --- | --- | ---: | ---: | --- | --- | --- | --- |
| `pulsix.event.standard` | 是 | 6 | 1 | `sceneCode` | `pulsix-ingest` | `pulsix-engine` | 标准事件主流，一期统一 Topic |
| `pulsix.decision.result` | 是 | 3 | 1 | `traceId` | `pulsix-engine` | 业务订阅方、告警消费者、Doris sink | 轻量最终决策结果流 |
| `pulsix.decision.log` | 是 | 3 | 1 | `traceId` | `pulsix-engine` | Doris sink、问题排查消费者 | 详细决策追溯流 |
| `pulsix.engine.error` | 是 | 1 | 1 | `traceId` | `pulsix-engine` | Doris sink、告警消费者 | 引擎异常流 |
| `pulsix.event.dlq` | 是 | 1 | 1 | `traceId` 或 `rawEventId` | `pulsix-ingest` | Doris sink、接入治理页 | 非法事件 / 死信流 |
| `pulsix.ingest.error` | 建议预留 | 1 | 1 | `traceId` 或 `rawEventId` | `pulsix-ingest` | Doris sink、接入治理页 | 接入错误增强流；预留可选，一期默认不强依赖 |

### 分区数说明

- 文档没有给出固定分区数，只有两条原则：
  - `topic` 分区数要和 Flink `job` 并行度协调。
  - 本地 Docker 演示环境目标吞吐约 `1000 ~ 3000 EPS`。
- 因此本文给出的分区数是**一期可直接用**的建议值：
  - `pulsix.event.standard = 6`：输入主流，预留并行度空间。
  - `pulsix.decision.result / pulsix.decision.log = 3`：结果和日志分流，吞吐压力明显小于主输入流。
  - `pulsix.engine.error / pulsix.event.dlq / pulsix.ingest.error = 1`：异常流默认低吞吐；其中 `pulsix.ingest.error` 仅作为预留增强流，可先预建但一期默认不强依赖。
- 如果你当前只是单机联调，也可以先下调为：`3 / 2 / 2 / 1 / 1 / 1`。

### 不建议当前创建的 Topic

| Topic | 结论 | 原因 |
| --- | --- | --- |
| `pulsix.config.snapshot` | 不创建 | 文档明确要求配置同步只走 MySQL CDC |
| `pulsix.event.login` / `pulsix.event.register` / `pulsix.event.trade` | 暂不创建 | 一期先统一用 `pulsix.event.standard`，后期量大再拆 |

## 2.2 Topic 数据结构建议

### 1）`pulsix.event.standard`

推荐采用**扁平标准事件 JSON**，与 `docs/sql/pulsix-risk.sql` 中 `event_sample`、以及附录 E 的样例保持一致。

基础字段：

- `eventId`
- `traceId`
- `sceneCode`
- `eventType`
- `eventTime`
- `userId`
- `deviceId`
- `ip`
- `channel`
- `ext`

场景扩展字段示例：

- 登录：`loginResult`、`failReason`、`city`、`province`
- 注册：`mobile`、`inviteCode`、`registerResult`
- 交易：`amount`、`currency`、`tradeResult`、`merchantId`、`payMethod`

样例数据（交易事件）：

```json
{
  "eventId": "E_TRADE_0001",
  "traceId": "T_TRADE_0001",
  "sceneCode": "TRADE_RISK",
  "eventType": "trade",
  "eventTime": "2026-03-07T11:00:00",
  "userId": "U5001",
  "deviceId": "D5001",
  "ip": "66.77.88.99",
  "channel": "APP",
  "amount": 6800,
  "currency": "CNY",
  "tradeResult": "SUCCESS",
  "merchantId": "M1001",
  "payMethod": "CARD",
  "ext": {
    "city": "Shanghai"
  }
}
```

### 2）`pulsix.decision.result`

建议字段：

- `traceId`
- `eventId`
- `sceneCode`
- `version`
- `finalAction`
- `finalScore`
- `hitRuleCodes`
- `decisionTime`
- `latencyMs`

样例数据：

```json
{
  "traceId": "T_TRADE_9001",
  "eventId": "E_TRADE_9001",
  "sceneCode": "TRADE_RISK",
  "version": 1,
  "finalAction": "REJECT",
  "finalScore": 100,
  "hitRuleCodes": ["TRADE_R001", "TRADE_R002", "TRADE_R003"],
  "decisionTime": "2026-03-08T09:32:00",
  "latencyMs": 26
}
```

### 3）`pulsix.decision.log`

建议字段：

- `traceId`
- `eventId`
- `sceneCode`
- `sourceCode`
- `eventCode`
- `entityId`
- `policyCode`
- `finalAction`
- `finalScore`
- `versionNo`
- `latencyMs`
- `eventTime`
- `input`
- `featureSnapshot`
- `hitRulesDetail`
- `decisionDetail`

样例数据：

```json
{
  "traceId": "T_TRADE_9001",
  "eventId": "E_TRADE_9001",
  "sceneCode": "TRADE_RISK",
  "sourceCode": "PAY_CORE_SDK",
  "eventCode": "trade",
  "entityId": "U5001",
  "policyCode": "TRADE_RISK_SCORECARD",
  "finalAction": "REJECT",
  "finalScore": 100,
  "versionNo": 1,
  "latencyMs": 26,
  "eventTime": "2026-03-07T11:00:00",
  "input": {
    "eventId": "E_TRADE_9001",
    "traceId": "T_TRADE_9001",
    "sceneCode": "TRADE_RISK",
    "eventType": "trade",
    "eventTime": "2026-03-07T11:00:00",
    "userId": "U5001",
    "deviceId": "D5001",
    "ip": "66.77.88.99",
    "amount": 6800,
    "merchantId": "M1001",
    "payMethod": "CARD"
  },
  "featureSnapshot": {
    "user_trade_cnt_5m": 4,
    "user_trade_amt_sum_30m": 12800,
    "device_bind_user_cnt_1h": 5,
    "user_risk_level": "H",
    "high_risk_trade_flag": true
  },
  "hitRulesDetail": [
    {
      "ruleCode": "TRADE_R001",
      "score": 40,
      "hit": true
    },
    {
      "ruleCode": "TRADE_R002",
      "score": 35,
      "hit": true
    },
    {
      "ruleCode": "TRADE_R003",
      "score": 25,
      "hit": true
    }
  ],
  "decisionDetail": {
    "decisionMode": "SCORE_CARD",
    "totalScore": 100,
    "matchedBand": {
      "min": 80,
      "max": 999999,
      "action": "REJECT"
    }
  }
}
```

### 4）`pulsix.engine.error`

文档只明确了错误来源类型，没有给出固定消息结构。建议你统一成下面这种结构，便于 Doris 下沉和页面查询：

- `traceId`
- `eventId`
- `sceneCode`
- `eventCode`
- `versionNo`
- `errorStage`
- `errorType`
- `errorCode`
- `errorMessage`
- `ruleCode`
- `scriptType`
- `sourceRef`
- `event`
- `featureSnapshot`
- `occurTime`

样例数据（本文推导建议）：

```json
{
  "traceId": "T_TRADE_9002",
  "eventId": "E_TRADE_9002",
  "sceneCode": "TRADE_RISK",
  "eventCode": "trade",
  "versionNo": 1,
  "errorStage": "RULE_EVAL",
  "errorType": "AVIATOR",
  "errorCode": "EXPRESSION_TYPE_MISMATCH",
  "errorMessage": "字段 amount 类型不符合表达式预期，当前规则按失败处理",
  "ruleCode": "TRADE_R004",
  "scriptType": "AVIATOR",
  "sourceRef": "rule_def.TRADE_R004",
  "event": {
    "eventId": "E_TRADE_9002",
    "traceId": "T_TRADE_9002",
    "sceneCode": "TRADE_RISK",
    "eventType": "trade",
    "amount": "6800"
  },
  "featureSnapshot": {
    "device_bind_user_cnt_1h": 5,
    "user_risk_level": "H"
  },
  "occurTime": "2026-03-08T09:40:00"
}
```

### 5）`pulsix.event.dlq`

建议承载：

- `traceId`
- `rawEventId`
- `sourceCode`
- `sceneCode`
- `eventCode`
- `ingestStage`
- `errorCode`
- `errorMessage`
- `rawPayload`
- `standardPayload`
- `occurTime`

样例数据：

```json
{
  "traceId": "T_LOGIN_ERR_0001",
  "rawEventId": "raw_login_bad_0001",
  "sourceCode": "APP_BEACON_WEB",
  "sceneCode": "LOGIN_RISK",
  "eventCode": "login",
  "ingestStage": "VALIDATE",
  "errorCode": "REQUIRED_FIELD_MISSING",
  "errorMessage": "eventTime 缺失，且接入层无法从 ts_ms 中解析出合法时间",
  "rawPayload": {
    "event_id": "raw_login_bad_0001",
    "trace_id": "T_LOGIN_ERR_0001",
    "uid": "U1002",
    "did": "D1002",
    "clientIp": "10.10.10.10",
    "result": "FAIL"
  },
  "standardPayload": {
    "eventId": "raw_login_bad_0001",
    "traceId": "T_LOGIN_ERR_0001",
    "sceneCode": "LOGIN_RISK",
    "eventType": "login",
    "userId": "U1002",
    "deviceId": "D1002",
    "ip": "10.10.10.10",
    "loginResult": "FAIL"
  },
  "occurTime": "2026-03-08T09:35:00"
}
```

### 6）`pulsix.ingest.error`

一期默认仍以 `pulsix.event.dlq` 作为唯一必达错误流；如果你决定单独保留一条“接入错误增强流”，建议字段与 `ingest_error_log` 对齐，并额外携带：

- `topicName`
- `reprocessStatus`
- `status`

样例数据可直接复用 `dwd_ingest_error_log` 的样例。

## 3. Redis Key 清单

## 3.1 命名规范

实际落地建议优先采用附录 D 的统一前缀：

```text
pulsix:{domain}:{type}:{scene?}:{bizKey}
```

其中：

- `domain`：`list` / `profile` / `feature` / `cache` / `dict`
- `scene`：只有在特征、缓存这类容易重名的数据上建议显式带上
- 名单和画像类通常可不带 `scene`

## 3.2 关键说明

- 文档一期更推荐**单值 key 模式**，而不是大 `Set`。
- 虽然运行态里有 `lookupType = REDIS_SET` 的命名，但附录 C 的伪代码实际是：
  - 拼出 `redisKeyPrefix + entityKey`
  - 用 `exists(redisKey)` 判定是否命中
- 也就是说，**一期名单查询本质上是“判断某个 key 是否存在”**，不是 Redis 大 Set 成员判断。

## 3.3 建议创建的 Redis Key

| Key 模式 | 数据结构 | 推荐 TTL | 用途 | 示例数据 |
| --- | --- | --- | --- | --- |
| `pulsix:list:black:device:{deviceId}` | String / Hash | 不过期；或按 `expireAt` 清理 | 设备黑名单 | `pulsix:list:black:device:D9001 -> 1` |
| `pulsix:list:black:ip:{ip}` | String | 不过期；或按 `expireAt` 清理 | IP 黑名单 | `pulsix:list:black:ip:1.2.3.4 -> 1` |
| `pulsix:list:white:user:{userId}` | String | 不过期；或按 `expireAt` 清理 | 用户白名单 | `pulsix:list:white:user:U1001 -> 1` |
| `pulsix:profile:user:risk:{userId}` | String | `24h` | 用户风险等级 | `pulsix:profile:user:risk:U1001 -> H` |
| `pulsix:profile:device:score:{deviceId}` | String | `24h` | 设备风险分 | `pulsix:profile:device:score:D9001 -> 87` |
| `pulsix:profile:user:{userId}` | Hash | `24h` | 多属性用户画像 | `riskLevel=H,userType=VIP,registerDays=180,refundRate=0.13` |
| `pulsix:feature:TRADE_RISK:user_trade_cnt_5m:{userId}` | String | `10m` | 用户 5 分钟交易次数副本 | `pulsix:feature:TRADE_RISK:user_trade_cnt_5m:U1001 -> 3` |
| `pulsix:feature:TRADE_RISK:user_trade_amt_sum_30m:{userId}` | String | `40m` | 用户 30 分钟交易金额和副本 | `pulsix:feature:TRADE_RISK:user_trade_amt_sum_30m:U1001 -> 18800` |
| `pulsix:feature:TRADE_RISK:device_bind_user_cnt_1h:{deviceId}` | String | `2h` | 设备 1 小时关联用户数副本 | `pulsix:feature:TRADE_RISK:device_bind_user_cnt_1h:D9001 -> 4` |
| `pulsix:cache:scene:active_version:{sceneCode}` | String | 不过期 | 当前激活版本缓存 | `pulsix:cache:scene:active_version:TRADE_RISK -> 12` |
| `pulsix:cache:simulation:{sceneCode}:{caseId}:{versionNo}` | String | `24h` | 仿真结果缓存 | `{"finalAction":"REVIEW","score":60}` |
| `pulsix:cache:warmup:{sceneCode}:{featureCode}` | String | `30m` | 热点 lookup 预热标记 | `DONE` |
| `pulsix:dict:geo:ip:{ip}` | String / Hash | `30d` | IP 地理字典 | `{"country":"CN","province":"Shanghai","city":"Shanghai"}` |
| `pulsix:dict:merchant:risk:{merchantId}` | String | `24h` | 商户风险等级字典 | `M` |

### TTL 说明

- 文档只明确了 TTL 的**方向性要求**：
  - 名单：可永久，或按 `expireAt` 清理
  - 热点特征：通常短 TTL
  - 仿真缓存：短 TTL
- 因此上表 TTL 是本文补充的**建议值**，规则是：
  - 名单：不随便过期，避免误放行
  - 画像：按上游刷新周期给 `24h`
  - 特征副本：一般设置为窗口长度的 `1.5 ~ 2` 倍
  - 缓存类：尽量短 TTL

### 本地缓存 TTL 说明

运行态快照示例里，lookup 特征还配置了引擎侧本地缓存 TTL，例如：

- `device_in_blacklist.cacheTtlSeconds = 30`
- `user_risk_level.cacheTtlSeconds = 30`

这和 Redis Key 本身的 TTL 不是一回事：

- Redis TTL：控制 key 生命周期
- 本地缓存 TTL：控制 Flink/引擎进程内的短缓存刷新频率

## 4. Doris 表设计建议

## 4.1 设计原则

- 文档已经明确：Doris 负责“结果 / 日志 / 错误 / 接入异常”的统一查询模型。
- 文档没有给出具体 Doris 表名，因此下面是一版与当前 `Kafka topic`、`decision_log`、`rule_hit_log`、`ingest_error_log` 对齐的最小可用建模。
- Doris 建议只做**查询读库**，不参与热路径同步写。
- 以下示例按 Doris `3.1.4` 的常规 `DUPLICATE KEY` 模型给出。

## 4.2 建库语句

```sql
CREATE DATABASE IF NOT EXISTS pulsix_olap;
USE pulsix_olap;
```

## 4.3 建表 DDL

### 1）最终结果表 `dwd_decision_result`

```sql
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
```

### 2）详细决策日志表 `dwd_decision_log`

```sql
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
```

### 3）规则命中明细表 `dwd_rule_hit_log`

这里建议不要照搬 MySQL 的 `decision_id` 关联方式，而是直接做查询友好的宽表。

```sql
CREATE TABLE IF NOT EXISTS dwd_rule_hit_log (
  dt DATE NOT NULL COMMENT '业务日期',
  trace_id VARCHAR(64) NOT NULL COMMENT '链路号',
  event_id VARCHAR(128) NOT NULL COMMENT '事件号',
  scene_code VARCHAR(64) NOT NULL COMMENT '场景编码',
  version_no INT NOT NULL COMMENT '版本号',
  rule_code VARCHAR(64) NOT NULL COMMENT '规则编码',
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
```

### 4）接入异常表 `dwd_ingest_error_log`

```sql
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
```

### 5）引擎异常表 `dwd_engine_error_log`

这张表是按文档中的错误流职责推导出来的建议模型。

```sql
CREATE TABLE IF NOT EXISTS dwd_engine_error_log (
  dt DATE NOT NULL COMMENT '业务日期',
  trace_id VARCHAR(64) NOT NULL COMMENT '链路号',
  event_id VARCHAR(128) NULL COMMENT '事件号',
  scene_code VARCHAR(64) NULL COMMENT '场景编码',
  event_code VARCHAR(64) NULL COMMENT '事件编码',
  version_no INT NULL COMMENT '运行版本号',
  error_stage VARCHAR(32) NOT NULL COMMENT '错误阶段',
  error_type VARCHAR(32) NOT NULL COMMENT '错误类型',
  error_code VARCHAR(64) NOT NULL COMMENT '错误编码',
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
```

## 4.4 Doris 示例数据

### 1）`dwd_decision_result`

```sql
INSERT INTO dwd_decision_result VALUES
('2026-03-08', 'T_LOGIN_9001', 'E_LOGIN_9001', 'LOGIN_RISK', 1, 'REVIEW', 60, '["LOGIN_R002"]', '2026-03-07 09:20:00', '2026-03-08 09:32:00', 18,
'{"traceId":"T_LOGIN_9001","eventId":"E_LOGIN_9001","sceneCode":"LOGIN_RISK","version":1,"finalAction":"REVIEW","finalScore":60,"hitRuleCodes":["LOGIN_R002"],"decisionTime":"2026-03-08T09:32:00","latencyMs":18}'),
('2026-03-08', 'T_TRADE_9001', 'E_TRADE_9001', 'TRADE_RISK', 1, 'REJECT', 100, '["TRADE_R001","TRADE_R002","TRADE_R003"]', '2026-03-07 11:00:00', '2026-03-08 09:32:00', 26,
'{"traceId":"T_TRADE_9001","eventId":"E_TRADE_9001","sceneCode":"TRADE_RISK","version":1,"finalAction":"REJECT","finalScore":100,"hitRuleCodes":["TRADE_R001","TRADE_R002","TRADE_R003"],"decisionTime":"2026-03-08T09:32:00","latencyMs":26}');
```

### 2）`dwd_decision_log`

```sql
INSERT INTO dwd_decision_log VALUES
('2026-03-08', 'T_LOGIN_9001', 'E_LOGIN_9001', 'LOGIN_RISK', 'APP_BEACON_WEB', 'login', 'U1001', 'LOGIN_RISK_POLICY', 'REVIEW', 60, 1, 18,
'2026-03-07 09:20:00', '2026-03-08 09:32:00',
'{"eventId":"E_LOGIN_9001","traceId":"T_LOGIN_9001","sceneCode":"LOGIN_RISK","eventType":"login","eventTime":"2026-03-07T09:20:00","userId":"U1001","deviceId":"D9002","ip":"10.20.30.40","channel":"APP","loginResult":"FAIL"}',
'{"user_login_fail_cnt_10m":6,"ip_login_fail_cnt_10m":8,"device_login_user_cnt_1h":1,"device_in_blacklist":false,"ip_risk_level":"HIGH","user_in_white_list":false}',
'["LOGIN_R002"]',
'{"decisionMode":"FIRST_HIT","versionNo":1,"stopRule":"LOGIN_R002"}'),
('2026-03-08', 'T_TRADE_9001', 'E_TRADE_9001', 'TRADE_RISK', 'PAY_CORE_SDK', 'trade', 'U5001', 'TRADE_RISK_SCORECARD', 'REJECT', 100, 1, 26,
'2026-03-07 11:00:00', '2026-03-08 09:32:00',
'{"eventId":"E_TRADE_9001","traceId":"T_TRADE_9001","sceneCode":"TRADE_RISK","eventType":"trade","eventTime":"2026-03-07T11:00:00","userId":"U5001","deviceId":"D5001","ip":"66.77.88.99","amount":6800,"merchantId":"M1001","payMethod":"CARD"}',
'{"user_trade_cnt_5m":4,"user_trade_amt_sum_30m":12800,"device_bind_user_cnt_1h":5,"user_risk_level":"H","high_risk_trade_flag":true}',
'["TRADE_R001","TRADE_R002","TRADE_R003"]',
'{"decisionMode":"SCORE_CARD","versionNo":1,"totalScore":100,"matchedBand":{"min":80,"max":999999,"action":"REJECT"}}');
```

### 3）`dwd_rule_hit_log`

```sql
INSERT INTO dwd_rule_hit_log VALUES
('2026-03-08', 'T_LOGIN_9001', 'E_LOGIN_9001', 'LOGIN_RISK', 1, 'LOGIN_R002', '短时密码爆破复核', 3, 1,
'用户 10 分钟失败次数过高且 IP 风险等级=HIGH', 60, '2026-03-07 09:20:00', '2026-03-08 09:32:00', '{"user_login_fail_cnt_10m":6,"ip_risk_level":"HIGH"}'),
('2026-03-08', 'T_TRADE_9001', 'E_TRADE_9001', 'TRADE_RISK', 1, 'TRADE_R001', '高风险交易候选命中', 1, 1,
'高风险交易标记命中，金额=6800，5 分钟交易次数=4', 40, '2026-03-07 11:00:00', '2026-03-08 09:32:00', '{"amount":6800,"user_trade_cnt_5m":4,"user_risk_level":"H","high_risk_trade_flag":true}'),
('2026-03-08', 'T_TRADE_9001', 'E_TRADE_9001', 'TRADE_RISK', 1, 'TRADE_R002', '设备多用户交易风险', 2, 1,
'设备 1 小时关联用户数=5', 35, '2026-03-07 11:00:00', '2026-03-08 09:32:00', '{"device_bind_user_cnt_1h":5}'),
('2026-03-08', 'T_TRADE_9001', 'E_TRADE_9001', 'TRADE_RISK', 1, 'TRADE_R003', '高风险用户画像命中', 3, 1,
'用户风险等级=H', 25, '2026-03-07 11:00:00', '2026-03-08 09:32:00', '{"user_risk_level":"H"}');
```

### 4）`dwd_ingest_error_log`

```sql
INSERT INTO dwd_ingest_error_log VALUES
('2026-03-08', 'T_LOGIN_ERR_0001', 'raw_login_bad_0001', 'APP_BEACON_WEB', 'LOGIN_RISK', 'login', 'VALIDATE', 'REQUIRED_FIELD_MISSING',
'eventTime 缺失，且接入层无法从 ts_ms 中解析出合法时间', 'pulsix.event.dlq', 'PENDING', 1, '2026-03-08 09:35:00',
'{"event_id":"raw_login_bad_0001","trace_id":"T_LOGIN_ERR_0001","uid":"U1002","did":"D1002","clientIp":"10.10.10.10","result":"FAIL"}',
'{"eventId":"raw_login_bad_0001","traceId":"T_LOGIN_ERR_0001","sceneCode":"LOGIN_RISK","eventType":"login","userId":"U1002","deviceId":"D1002","ip":"10.10.10.10","loginResult":"FAIL"}'),
('2026-03-08', 'T_TRADE_ERR_0001', 'raw_trade_bad_0001', 'PAY_CORE_SDK', 'TRADE_RISK', 'trade', 'NORMALIZE', 'AMOUNT_NEGATIVE',
'amountFen 转换后得到负数金额，疑似上游报文异常', 'pulsix.event.dlq', 'IGNORED', 1, '2026-03-08 09:36:00',
'{"req":{"traceId":"T_TRADE_ERR_0001","occurTime":1772842800000},"biz":{"tradeId":"raw_trade_bad_0001","uid":"U5002","deviceNo":"D5002","ip":"66.77.88.10","amountFen":-1000,"merchantNo":"M2001","payMethod":"CARD"}}',
'{"eventId":"raw_trade_bad_0001","traceId":"T_TRADE_ERR_0001","sceneCode":"TRADE_RISK","eventType":"trade","eventTime":"2026-03-07T11:00:00","userId":"U5002","deviceId":"D5002","ip":"66.77.88.10","amount":-10.00,"merchantId":"M2001","payMethod":"CARD"}');
```

### 5）`dwd_engine_error_log`

```sql
INSERT INTO dwd_engine_error_log VALUES
('2026-03-08', 'T_TRADE_9002', 'E_TRADE_9002', 'TRADE_RISK', 'trade', 1, 'RULE_EVAL', 'AVIATOR', 'EXPRESSION_TYPE_MISMATCH',
'字段 amount 类型不符合表达式预期，当前规则按失败处理', 'TRADE_R004', 'AVIATOR', 'rule_def.TRADE_R004', '2026-03-08 09:40:00',
'{"eventId":"E_TRADE_9002","traceId":"T_TRADE_9002","sceneCode":"TRADE_RISK","eventType":"trade","amount":"6800"}',
'{"device_bind_user_cnt_1h":5,"user_risk_level":"H"}');
```

## 5. 结论：你现在最适合直接落地的最小集合

### Kafka

- 必建：
  - `pulsix.event.standard`
  - `pulsix.decision.result`
  - `pulsix.decision.log`
  - `pulsix.engine.error`
  - `pulsix.event.dlq`
- 建议预留：
  - `pulsix.ingest.error`

### Redis

- 名单：
  - `pulsix:list:black:device:{deviceId}`
  - `pulsix:list:black:ip:{ip}`
  - `pulsix:list:white:user:{userId}`
- 画像：
  - `pulsix:profile:user:risk:{userId}`
  - `pulsix:profile:device:score:{deviceId}`
  - `pulsix:profile:user:{userId}`
- 热点特征副本：
  - `pulsix:feature:TRADE_RISK:user_trade_cnt_5m:{userId}`
  - `pulsix:feature:TRADE_RISK:user_trade_amt_sum_30m:{userId}`
  - `pulsix:feature:TRADE_RISK:device_bind_user_cnt_1h:{deviceId}`

### Doris

- 建议最小查询读模型：
  - `dwd_decision_result`
  - `dwd_decision_log`
  - `dwd_rule_hit_log`
  - `dwd_ingest_error_log`
  - `dwd_engine_error_log`

