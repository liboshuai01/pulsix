数据库、Redis、Kafka 的第一版落地设计，重点给出核心表结构、Redis Key 命名规范以及 Kafka Topic 规划，便于项目从 0 到 1 快速落地。

## D.1 附录定位

这个附录的目标不是直接给出一套企业级最终版 schema，而是给你一版：

- **能支撑 MVP 落地**
- **概念边界清晰**
- **后续可扩展**
- **和前面 24 章保持一致**

的第一版设计。

也就是说，这一版 DDL 和 Key 设计要满足下面几件事：

1. 控制平台能完成场景、特征、规则、策略、发布、仿真、日志管理
2. Flink 能消费发布后的快照并执行
3. Redis 能承担名单、画像、热点 lookup 的职责
4. Kafka 能清楚地区分事件流、配置流、决策流、日志流

---

## D.2 第一版核心表结构设计思路

表结构我建议按下面几类来组织：

1. **基础元数据表**
2. **特征与名单表**
3. **规则与策略表**
4. **发布与版本表**
5. **仿真与测试表**
6. **决策日志表**
7. **系统管理与审计表**

这里不会把所有索引、字符集、引擎参数全部写满，而是先给你一版足够清晰的 MVP DDL 草稿。

---

## D.3 基础元数据表

### D.3.1 场景表 `scene_def`

```sql
CREATE TABLE scene_def (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  scene_code VARCHAR(64) NOT NULL UNIQUE,
  scene_name VARCHAR(128) NOT NULL,
  scene_type VARCHAR(64) DEFAULT NULL,
  status TINYINT NOT NULL DEFAULT 1,
  default_policy_code VARCHAR(64) DEFAULT NULL,
  description VARCHAR(512) DEFAULT NULL,
  created_by VARCHAR(64) DEFAULT NULL,
  updated_by VARCHAR(64) DEFAULT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);
```

#### 说明

- `scene_code` 建议全局唯一，例如： `LOGIN_RISK`、 `REGISTER_ANTI_FRAUD`、 `TRADE_RISK`
- `status` 可约定： `1=启用`， `0=停用`
- `default_policy_code` 可以为空，一期允许后面再绑定

---

### D.3.2 事件模型表 `event_schema`

```sql
CREATE TABLE event_schema (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  scene_code VARCHAR(64) NOT NULL,
  event_code VARCHAR(64) NOT NULL,
  event_name VARCHAR(128) NOT NULL,
  event_type VARCHAR(64) NOT NULL,
  source_type VARCHAR(32) DEFAULT NULL,
  topic_name VARCHAR(128) DEFAULT NULL,
  version INT NOT NULL DEFAULT 1,
  status TINYINT NOT NULL DEFAULT 1,
  description VARCHAR(512) DEFAULT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_scene_event (scene_code, event_code)
);
```

---

### D.3.3 事件字段定义表 `event_field_def`

```sql
CREATE TABLE event_field_def (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  event_code VARCHAR(64) NOT NULL,
  field_name VARCHAR(64) NOT NULL,
  field_label VARCHAR(128) DEFAULT NULL,
  field_type VARCHAR(32) NOT NULL,
  required_flag TINYINT NOT NULL DEFAULT 0,
  default_value VARCHAR(256) DEFAULT NULL,
  sample_value VARCHAR(512) DEFAULT NULL,
  description VARCHAR(512) DEFAULT NULL,
  sort_no INT NOT NULL DEFAULT 0,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_event_field (event_code, field_name)
);
```

---

## D.4 特征与名单表

### D.4.1 特征定义主表 `feature_def`

```sql
CREATE TABLE feature_def (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  scene_code VARCHAR(64) NOT NULL,
  feature_code VARCHAR(64) NOT NULL,
  feature_name VARCHAR(128) NOT NULL,
  feature_type VARCHAR(32) NOT NULL,
  entity_type VARCHAR(32) DEFAULT NULL,
  event_code VARCHAR(64) DEFAULT NULL,
  value_type VARCHAR(32) NOT NULL,
  status TINYINT NOT NULL DEFAULT 1,
  version INT NOT NULL DEFAULT 1,
  description VARCHAR(512) DEFAULT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_scene_feature (scene_code, feature_code)
);
```

#### `feature_type` 建议值

- `STREAM`
- `LOOKUP`
- `DERIVED`

---

### D.4.2 流式特征配置表 `feature_stream_conf`

```sql
CREATE TABLE feature_stream_conf (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  feature_code VARCHAR(64) NOT NULL,
  source_event_types VARCHAR(256) NOT NULL,
  entity_key_expr VARCHAR(256) NOT NULL,
  agg_type VARCHAR(32) NOT NULL,
  value_expr VARCHAR(512) DEFAULT NULL,
  filter_expr VARCHAR(1024) DEFAULT NULL,
  window_type VARCHAR(32) NOT NULL,
  window_size VARCHAR(32) NOT NULL,
  window_slide VARCHAR(32) DEFAULT NULL,
  include_current_event TINYINT NOT NULL DEFAULT 1,
  ttl_seconds BIGINT DEFAULT NULL,
  state_hint_json JSON DEFAULT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_feature_stream (feature_code)
);
```

#### 说明

- `source_event_types` MVP 阶段可先用逗号分隔字符串，后期可再拆表
- `agg_type` 例如： `COUNT` / `SUM` / `MAX` / `LATEST` / `DISTINCT_COUNT`
- `window_size` 建议采用字符串表达： `5m`、 `30m`、 `1h`

---

### D.4.3 查询特征配置表 `feature_lookup_conf`

```sql
CREATE TABLE feature_lookup_conf (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  feature_code VARCHAR(64) NOT NULL,
  lookup_type VARCHAR(32) NOT NULL,
  key_expr VARCHAR(256) NOT NULL,
  source_ref VARCHAR(256) NOT NULL,
  default_value VARCHAR(256) DEFAULT NULL,
  cache_ttl_seconds BIGINT DEFAULT NULL,
  timeout_ms INT DEFAULT NULL,
  extra_json JSON DEFAULT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_feature_lookup (feature_code)
);
```

#### `lookup_type` 建议值

- `REDIS_SET`
- `REDIS_HASH`
- `REDIS_STRING`
- `DICT`

---

### D.4.4 派生特征配置表 `feature_derived_conf`

```sql
CREATE TABLE feature_derived_conf (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  feature_code VARCHAR(64) NOT NULL,
  engine_type VARCHAR(32) NOT NULL,
  expr_content TEXT NOT NULL,
  depends_on_json JSON DEFAULT NULL,
  value_type VARCHAR(32) NOT NULL,
  extra_json JSON DEFAULT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_feature_derived (feature_code)
);
```

---

### D.4.5 名单主表 `list_set`

```sql
CREATE TABLE list_set (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  scene_code VARCHAR(64) NOT NULL,
  list_code VARCHAR(64) NOT NULL,
  list_name VARCHAR(128) NOT NULL,
  match_type VARCHAR(32) NOT NULL,
  list_type VARCHAR(32) NOT NULL,
  status TINYINT NOT NULL DEFAULT 1,
  description VARCHAR(512) DEFAULT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_scene_list (scene_code, list_code)
);
```

#### `match_type` 示例

- `USER`
- `DEVICE`
- `IP`
- `MOBILE`
- `CARD`

#### `list_type` 示例

- `BLACK`
- `WHITE`
- `WATCH`

---

### D.4.6 名单项表 `list_item`

```sql
CREATE TABLE list_item (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  list_code VARCHAR(64) NOT NULL,
  match_key VARCHAR(128) DEFAULT NULL,
  match_value VARCHAR(512) NOT NULL,
  expire_at DATETIME DEFAULT NULL,
  status TINYINT NOT NULL DEFAULT 1,
  remark VARCHAR(512) DEFAULT NULL,
  ext_json JSON DEFAULT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  KEY idx_list_code (list_code),
  KEY idx_match_value (match_value)
);
```

---

## D.5 规则与策略表

### D.5.1 规则定义表 `rule_def`

```sql
CREATE TABLE rule_def (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  scene_code VARCHAR(64) NOT NULL,
  rule_code VARCHAR(64) NOT NULL,
  rule_name VARCHAR(128) NOT NULL,
  rule_type VARCHAR(32) DEFAULT 'NORMAL',
  engine_type VARCHAR(32) NOT NULL,
  expr_content TEXT NOT NULL,
  priority INT NOT NULL DEFAULT 0,
  hit_action VARCHAR(32) NOT NULL,
  risk_score INT DEFAULT 0,
  status TINYINT NOT NULL DEFAULT 1,
  hit_reason_template VARCHAR(1024) DEFAULT NULL,
  version INT NOT NULL DEFAULT 1,
  description VARCHAR(512) DEFAULT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_scene_rule (scene_code, rule_code)
);
```

#### `engine_type` 建议值

- `DSL`
- `AVIATOR`
- `GROOVY`

#### `hit_action` 建议值

- `PASS`
- `REVIEW`
- `REJECT`
- `TAG_ONLY`
- `LIMIT`

---

### D.5.2 策略定义表 `policy_def`

```sql
CREATE TABLE policy_def (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  scene_code VARCHAR(64) NOT NULL,
  policy_code VARCHAR(64) NOT NULL,
  policy_name VARCHAR(128) NOT NULL,
  decision_mode VARCHAR(32) NOT NULL,
  default_action VARCHAR(32) NOT NULL,
  status TINYINT NOT NULL DEFAULT 1,
  description VARCHAR(512) DEFAULT NULL,
  version INT NOT NULL DEFAULT 1,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_scene_policy (scene_code, policy_code)
);
```

#### `decision_mode` 建议值

- `FIRST_HIT`
- `SCORE_CARD`

---

### D.5.3 策略规则关联表 `policy_rule_ref`

```sql
CREATE TABLE policy_rule_ref (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  policy_code VARCHAR(64) NOT NULL,
  rule_code VARCHAR(64) NOT NULL,
  order_no INT NOT NULL DEFAULT 0,
  enabled_flag TINYINT NOT NULL DEFAULT 1,
  branch_expr VARCHAR(1024) DEFAULT NULL,
  score_weight INT DEFAULT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_policy_rule (policy_code, rule_code)
);
```

---

## D.6 发布与版本表

### D.6.1 发布版本表 `scene_release`

```sql
CREATE TABLE scene_release (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  scene_code VARCHAR(64) NOT NULL,
  version_no INT NOT NULL,
  snapshot_json JSON NOT NULL,
  checksum VARCHAR(128) NOT NULL,
  publish_status VARCHAR(32) NOT NULL,
  published_by VARCHAR(64) DEFAULT NULL,
  published_at DATETIME DEFAULT NULL,
  effective_from DATETIME DEFAULT NULL,
  rollback_from_version INT DEFAULT NULL,
  remark VARCHAR(512) DEFAULT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_scene_version (scene_code, version_no),
  KEY idx_scene_publish_time (scene_code, published_at)
);
```

#### `publish_status` 建议值

- `DRAFT`
- `PUBLISHED`
- `ROLLED_BACK`
- `FAILED`

---

## D.7 仿真与测试表

### D.7.1 仿真用例表 `simulation_case`

```sql
CREATE TABLE simulation_case (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  scene_code VARCHAR(64) NOT NULL,
  case_name VARCHAR(128) NOT NULL,
  input_event_json JSON NOT NULL,
  expected_action VARCHAR(32) DEFAULT NULL,
  expected_hit_rules JSON DEFAULT NULL,
  status TINYINT NOT NULL DEFAULT 1,
  created_by VARCHAR(64) DEFAULT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);
```

---

### D.7.2 仿真报告表 `simulation_report`

```sql
CREATE TABLE simulation_report (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  case_id BIGINT NOT NULL,
  scene_code VARCHAR(64) NOT NULL,
  version_no INT NOT NULL,
  result_json JSON NOT NULL,
  pass_flag TINYINT NOT NULL,
  duration_ms BIGINT DEFAULT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  KEY idx_case_version (case_id, version_no)
);
```

---

## D.8 决策日志表

### D.8.1 决策日志主表 `decision_log`

```sql
CREATE TABLE decision_log (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  trace_id VARCHAR(128) NOT NULL,
  event_id VARCHAR(128) NOT NULL,
  scene_code VARCHAR(64) NOT NULL,
  entity_id VARCHAR(128) DEFAULT NULL,
  final_action VARCHAR(32) NOT NULL,
  final_score INT DEFAULT NULL,
  version_no INT NOT NULL,
  latency_ms BIGINT DEFAULT NULL,
  event_time DATETIME NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  input_json JSON DEFAULT NULL,
  feature_snapshot_json JSON DEFAULT NULL,
  hit_rules_json JSON DEFAULT NULL,
  KEY idx_trace_id (trace_id),
  KEY idx_event_id (event_id),
  KEY idx_scene_time (scene_code, event_time)
);
```

---

### D.8.2 规则命中日志表 `rule_hit_log`

```sql
CREATE TABLE rule_hit_log (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  decision_id BIGINT NOT NULL,
  rule_code VARCHAR(64) NOT NULL,
  hit_flag TINYINT NOT NULL,
  hit_value_json JSON DEFAULT NULL,
  score INT DEFAULT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  KEY idx_decision_rule (decision_id, rule_code)
);
```

---

## D.9 系统管理与审计表

### D.9.1 用户表 `sys_user`

```sql
CREATE TABLE sys_user (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  username VARCHAR(64) NOT NULL UNIQUE,
  password_hash VARCHAR(256) NOT NULL,
  real_name VARCHAR(64) DEFAULT NULL,
  status TINYINT NOT NULL DEFAULT 1,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);
```

---

### D.9.2 角色表 `sys_role`

```sql
CREATE TABLE sys_role (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  role_code VARCHAR(64) NOT NULL UNIQUE,
  role_name VARCHAR(64) NOT NULL,
  status TINYINT NOT NULL DEFAULT 1,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);
```

---

### D.9.3 用户角色关联表 `sys_user_role`

```sql
CREATE TABLE sys_user_role (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  role_id BIGINT NOT NULL,
  UNIQUE KEY uk_user_role (user_id, role_id)
);
```

---

### D.9.4 审计日志表 `sys_audit_log`

```sql
CREATE TABLE sys_audit_log (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  operator_id BIGINT DEFAULT NULL,
  biz_type VARCHAR(64) NOT NULL,
  biz_code VARCHAR(128) NOT NULL,
  action_type VARCHAR(64) NOT NULL,
  before_json JSON DEFAULT NULL,
  after_json JSON DEFAULT NULL,
  remark VARCHAR(512) DEFAULT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  KEY idx_biz (biz_type, biz_code),
  KEY idx_operator_time (operator_id, created_at)
);
```

---

## D.10 Redis Key 命名规范设计

Redis 在这套系统里主要承担：

- 名单
- 在线画像
- 热点特征
- 少量辅助缓存

命名建议遵循：

```latex
rdp:{domain}:{type}:{scene}:{bizKey}
```

这样做的好处是：

- 前缀统一，方便排查和清理
- 域清晰，避免不同用途混用
- scene 显式可见，方便隔离

---

## D.10.1 名单 Key 设计

### 设备黑名单

```latex
rdp:list:black:device:{deviceId}
```

值可以是：

- String： `1`
- 或 Hash，包含更多元信息

#### 示例

```latex
rdp:list:black:device:D9001 -> 1
```

---

### IP 黑名单

```latex
rdp:list:black:ip:{ip}
```

#### 示例

```latex
rdp:list:black:ip:1.2.3.4 -> 1
```

---

### 用户白名单

```latex
rdp:list:white:user:{userId}
```

---

### 如果需要保留名单元信息

也可以用 Hash：

```latex
rdp:list:black:device:{deviceId}
```

字段例如：

- `status=1`
- `expireAt=2026-03-31 23:59:59`
- `source=manual_import`

---

## D.10.2 在线画像 Key 设计

### 用户风险等级

```latex
rdp:profile:user:risk:{userId}
```

#### 示例

```latex
rdp:profile:user:risk:U1001 -> H
```

---

### 设备风险分

```latex
rdp:profile:device:score:{deviceId}
```

#### 示例

```latex
rdp:profile:device:score:D9001 -> 87
```

---

### 用户画像 Hash

如果你想一次性存多项用户画像：

```latex
rdp:profile:user:{userId}
```

字段例如：

- `riskLevel=H`
- `userType=VIP`
- `registerDays=180`
- `refundRate=0.13`

---

## D.10.3 热点特征 Key 设计

如果某些特征要 materialize 到 Redis，可采用：

### 用户 5 分钟交易次数

```latex
rdp:feature:user_trade_cnt_5m:{userId}
```

#### 示例

```latex
rdp:feature:user_trade_cnt_5m:U1001 -> 3
```

---

### 设备 1 小时关联用户数

```latex
rdp:feature:device_bind_user_cnt_1h:{deviceId}
```

---

### 如果你希望带场景维度

```latex
rdp:feature:{sceneCode}:{featureCode}:{entityKey}
```

例如：

```latex
rdp:feature:TRADE_RISK:user_trade_cnt_5m:U1001
```

这个更通用，我更推荐这一种。

---

## D.10.4 辅助缓存 Key 设计

### 小型配置缓存

```latex
rdp:cache:scene:active_version:{sceneCode}
```

### 仿真结果缓存

```latex
rdp:cache:simulation:{sceneCode}:{caseId}:{versionNo}
```

### 本地热点 lookup 预热标记

```latex
rdp:cache:warmup:{sceneCode}:{featureCode}
```

---

## D.10.5 Redis 设计注意事项

### 1）避免无边界 key

不要出现这种：

```latex
risk_data:{id}
```

因为你根本不知道它是什么域的数据。

### 2）要考虑 TTL

名单、画像、热点特征是否设置 TTL，要按业务来定：

- 名单：可永久或按 expireAt 清理
- 热点特征：通常设置较短 TTL
- 仿真缓存：设置较短 TTL

### 3）避免把设计态元数据放进 Redis 当主存储

Redis 可以加速读取，但不是设计态配置主库。

---

## D.11 Kafka Topic 规划

Kafka Topic 命名建议统一前缀，例如：

```latex
rdp.{domain}.{name}
```

或：

```latex
rdp-{domain}-{name}
```

我这里用点号风格举例。

---

## D.11.1 原始事件 Topic

### 统一原始事件流

```latex
rdp.event.raw
```

事件体中带：

- `sceneCode`
- `eventType`

这是 MVP 阶段最推荐的方式。

---

### 如果后期按场景拆分

```latex
rdp.event.login
rdp.event.register
rdp.event.trade
```

一期不一定需要，统一 raw topic 更简单。

---

## D.11.2 配置快照 Topic

```latex
rdp.config.snapshot
```

用于承载：

- 新发布快照
- 回滚后的快照
- 生效版本通知

消息体建议包含：

- `sceneCode`
- `versionNo`
- `effectiveFrom`
- `snapshotJson`
- `checksum`
- `opType`（PUBLISH / ROLLBACK）

---

## D.11.3 决策结果 Topic

```latex
rdp.decision.result
```

承载：

- 最终动作
- score
- traceId
- versionNo
- sceneCode
- eventId

适合作为：

- 下游审核系统输入
- 下游标签系统输入
- 业务订阅结果流

---

## D.11.4 决策日志 Topic

```latex
rdp.decision.log
```

承载更完整的日志内容：

- input
- feature snapshot
- hit rules
- final action
- latency
- error info

然后再异步 sink 到 MySQL / Doris。

---

## D.11.5 引擎错误 Topic

```latex
rdp.engine.error
```

用于承载：

- 配置解析失败
- 表达式执行失败
- Groovy 执行异常
- Redis 查询异常

---

## D.11.6 死信 Topic

```latex
rdp.event.dlq
```

用于承载：

- 非法事件
- 反序列化失败
- 缺失关键字段
- sceneCode 不存在

---

## D.12 第一版推荐索引与唯一约束原则

DDL 不只是“字段写出来”，还要考虑查询和唯一性边界。

---

## D.12.1 唯一性建议

### 按 scene 维度唯一

- `feature_code`
- `rule_code`
- `policy_code`
- `list_code`

推荐使用 `(scene_code, code)` 联合唯一。

### 全局唯一建议

- `scene_code`
- `username`

---

## D.12.2 常见查询索引建议

### 日志表

- `trace_id`
- `event_id`
- `(scene_code, event_time)`

### 发布表

- `(scene_code, version_no)`
- `(scene_code, published_at)`

### 审计表

- `(biz_type, biz_code)`
- `(operator_id, created_at)`

---

## D.13 MVP 阶段可以进一步简化的点

如果你要快速落地，可以做以下简化：

### 简化 1

先不建 `rule_dependency` 表，依赖发布时临时分析。

### 简化 2

先不建 `release_task` 表，发布状态先只靠 `scene_release` 管。

### 简化 3

先不把日志拆得太细，先保留 `decision_log` 主表即可。

### 简化 4

Redis 先只做：

- 黑名单
- 用户风险等级
- 少量热特征

### 简化 5

Kafka 先只保留：

- `rdp.event.raw`
- `rdp.config.snapshot`
- `rdp.decision.result`
- `rdp.decision.log`

这样足够跑通 MVP。

---

## D.14 本附录小结

这一版第一版落地设计，核心目标是：

- **控制平台能管得住设计态**
- **发布中心能产出运行态快照**
- **Flink 能拿到快照并执行**
- **Redis 能承担在线 lookup**
- **Kafka 能承载配置流、事件流、决策流、日志流**

如果你后面进入真正编码阶段，我建议你最先落地的表是：

1. `scene_def`
2. `event_schema`
3. `event_field_def`
4. `feature_def`
5. `feature_stream_conf`
6. `feature_lookup_conf`
7. `feature_derived_conf`
8. `rule_def`
9. `policy_def`
10. `policy_rule_ref`
11. `scene_release`
12. `decision_log`
13. `sys_user`
14. `sys_role`
15. `sys_audit_log`

这样就已经足够支撑项目第一版主链路。
