# `pulsix-engine-kernel` 一期数据结构与示例数据冻结清单

## 1. 文档目的

这份文档用于把 `pulsix-engine / pulsix-kernel` 一期开发前必须先拍板的数据结构、存储对象和示例数据范围收敛下来，避免边开发边改样例口径。

本文定位不是替代详细 DDL、详细快照 JSON 或详细示例 SQL，而是给出一份“一期先冻结什么、哪些必须先有、每类至少准备几条样例”的执行清单。

## 2. 参考范围

主参考：

- `docs/wiki/pulsix-engine-kernel-一期开发指南.md`
- `docs/sql/pulsix-risk.sql`
- `docs/wiki/kafka-redis-doris-落地清单.md`
- `docs/wiki/风控功能清单.md`
- `docs/wiki/项目架构及技术栈.md`

辅助参考：

- `docs/参考资料` 下相关章节，仅在主文档没有写死时用于补齐建议值或解释边界。

## 3. 一期冻结原则

一期先坚持下面几条：

1. `pulsix-engine` 只认运行时快照，真正消费的是 `scene_release.snapshot_json`，不是设计态几十张表拼装出来的临时对象。
2. 编译和执行必须分开，`compile once, execute many`。
3. 本地执行、仿真、轻量回放、Flink 实时执行必须复用同一套执行语义。
4. 一期优先打通 `kernel + engine` 主链路，不优先做正式控制台、正式接入层、完整分析面。
5. 一期策略主链路优先做稳 `FIRST_HIT`；`SCORE_CARD` 可以保留结构和样例，但不应反向拖慢一期交付。

## 4. 一期先拍板的口径统一项

当前主文档、代码 Demo、`pulsix-risk.sql` 之间存在少量样例口径差异。正式开始一期开发前，建议先统一下面这些命名和样例基线。

### 4.1 建议统一项

| 主题 | 当前存在的口径 | 一期冻结建议 |
| --- | --- | --- |
| 快照版本字段 | `version` / `versionNo` 并存 | 运行时 JSON 统一用 `version`；SQL 列名可继续保留 `version_no` |
| 事件结果字段 | `result` / `loginResult` / `tradeResult` 并存 | 一期标准 `RiskEvent` 统一对内收敛为 `result`；接入层可做别名映射 |
| 交易主策略 | 主文档强调 `FIRST_HIT`，SQL 样例主打 `SCORE_CARD` | 一期主链路以 `FIRST_HIT` 为必跑口径；`SCORE_CARD` 保留为扩展示例 |
| 交易派生特征 | `trade_burst_flag` / `high_risk_trade_flag` 并存 | 一期主样例优先统一为 `trade_burst_flag`；`high_risk_trade_flag` 可保留给评分卡样例 |
| 交易 lookup 集 | 主文档 Demo 包含 `device_in_blacklist + user_risk_level`，SQL 交易样例主要只有 `user_risk_level` | 一期主样例建议统一保留 `device_in_blacklist + user_risk_level`，更完整覆盖 lookup 场景 |

### 4.2 推荐场景基线

- 主演示场景：`TRADE_RISK`
- 辅助场景：`LOGIN_RISK`
- 一期必跑链路：`event -> stream -> lookup -> derived -> rule -> FIRST_HIT policy -> result/log/error`
- 一期可保留但不强依赖的扩展示例：`SCORE_CARD`

## 5. 运行时对象冻结清单

这些对象是一期最先要稳定下来的“跨模块公共契约”。

| 对象 | 一期结论 | 必须字段 / 关键属性 | 最少样例数 | 说明 |
| --- | --- | --- | ---: | --- |
| `RiskEvent` | 必须冻结 | `eventId`、`traceId`、`sceneCode`、`eventType`、`eventTime`、实体字段、`result`、`ext` | 6 条连续交易事件 + 1 条黑名单事件 + 1 条非法事件 | 主文档明确要求先固定事件契约 |
| `SceneSnapshot` | 必须冻结 | `scene`、`eventSchema`、`features`、`rules`、`policy`、`runtimeHints`、`version`、`checksum` | 1 个 active 版本 + 1 个 candidate 版本 | 引擎运行时只认快照 |
| `SceneSnapshotEnvelope` | 必须冻结 | `sceneCode`、`version`、`checksum`、`publishType`、`publishedAt`、`effectiveFrom`、`snapshot` | 2 条 | 对接 `scene_release` / CDC |
| `EvalContext` | 必须冻结 | `base fields`、`feature values`、`lookup values`、`derived values`、`metadata`、trace 信息 | 1 套 | 本地仿真与 Flink 执行共用 |
| `DecisionResult` | 必须冻结 | `traceId`、`eventId`、`sceneCode`、`version`、`finalAction`、`finalScore`、`hitRules`、`latencyMs`、`featureSnapshot` | 3 条 | 至少覆盖 `PASS / REVIEW / REJECT` |
| `DecisionLogRecord` | 必须冻结 | `input`、`featureSnapshot`、`hitRulesDetail`、`decisionDetail`、`traceId`、`version`、`latencyMs` | 3 条 | 用于追溯和演示 |
| `EngineErrorRecord` | 必须冻结 | `traceId`、`eventId`、`sceneCode`、`version`、`errorStage`、`errorType`、`errorCode`、`errorMessage`、`sourceRef` | 2 条 | 至少覆盖 `lookup 失败`、`表达式/脚本失败` |

### 5.1 一期建议先固定的公共编码

- `sceneCode`
- `eventCode`
- `featureCode`
- `ruleCode`
- `policyCode`
- `traceId`
- `eventId`

这几个编码需要在 MySQL、Kafka、Redis、Doris、仿真用例、决策日志之间保持同一套命名，不要每层都重新发明一套别名。

## 6. MySQL 冻结清单

## 6.1 一期必须先定的最小表

| 表 | 一期结论 | 必须列 | 最少样例数 | 建议说明 |
| --- | --- | --- | ---: | --- |
| `scene_release` | 必须 | `scene_code`、`version_no`、`snapshot_json`、`checksum`、`publish_status`、`validation_status`、`published_at`、`effective_from` | 2 | 至少准备 1 个 active 版本、1 个 candidate/回滚候选版本 |
| `simulation_case` | 必须 | `scene_code`、`case_code`、`version_select_mode`、`input_event_json`、`mock_feature_json`、`mock_lookup_json`、`expected_action`、`expected_hit_rules` | 3 | 最少覆盖 `PASS / REVIEW / REJECT` |
| `simulation_report` | 必须 | `case_id`、`scene_code`、`version_no`、`trace_id`、`result_json`、`pass_flag`、`duration_ms` | 3 | 每个仿真用例至少有 1 份报告 |
| `decision_log` | 必须 | `trace_id`、`event_id`、`scene_code`、`policy_code`、`final_action`、`final_score`、`version_no`、`latency_ms`、`input_json`、`feature_snapshot_json`、`hit_rules_json`、`decision_detail_json` | 3 | 至少沉淀 3 条可追溯结果 |
| `rule_hit_log` | 必须 | `decision_id`、`rule_code`、`rule_name`、`rule_order_no`、`hit_flag`、`hit_reason`、`score`、`hit_value_json` | 6 | 至少能还原 1 次完整规则执行链 |

## 6.2 一期建议同步冻结的设计态表

| 表 | 一期建议 | 必须列 | 最少样例数 | 备注 |
| --- | --- | --- | ---: | --- |
| `scene_def` | 建议同步定 | `scene_code`、`scene_name`、`default_event_code`、`default_policy_code`、`standard_topic_name`、`decision_topic_name` | 2 | 建议保留 `TRADE_RISK + LOGIN_RISK` |
| `event_schema` | 建议同步定 | `scene_code`、`event_code`、`event_name`、`standard_topic_name`、`status` | 2 | 标准事件模型根对象 |
| `event_field_def` | 建议同步定 | `scene_code`、`event_code`、`field_code`、`field_type`、`required_flag`、`sample_value` | 10+ | `TRADE_RISK` 至少 10 个标准字段 |
| `event_sample` | 建议同步定 | `scene_code`、`event_code`、`sample_code`、`sample_type`、`sample_json` | 4 | 推荐 `LOGIN_RAW/LOGIN_STD/TRADE_RAW/TRADE_STD` |
| `feature_def` | 建议同步定 | `scene_code`、`feature_code`、`feature_type`、`entity_type`、`event_code`、`value_type` | 6 | `TRADE_RISK` 至少 3 stream + 2 lookup + 1 derived |
| `feature_stream_conf` | 建议同步定 | `scene_code`、`feature_code`、`entity_key_expr`、`agg_type`、`window_type`、`window_size`、`ttl_seconds` | 3 | 对应 3 个交易流式特征 |
| `feature_lookup_conf` | 建议同步定 | `scene_code`、`feature_code`、`lookup_type`、`key_expr`、`source_ref`、`default_value`、`cache_ttl_seconds`、`timeout_ms` | 2 | 对应 `device_in_blacklist`、`user_risk_level` |
| `feature_derived_conf` | 建议同步定 | `scene_code`、`feature_code`、`engine_type`、`expr_content`、`depends_on_json`、`value_type` | 2 | 对应 `high_amt_flag`、`trade_burst_flag` |
| `rule_def` | 建议同步定 | `scene_code`、`rule_code`、`engine_type`、`expr_content`、`priority`、`hit_action`、`risk_score`、`hit_reason_template` | 3 | 一期主链路建议 3 条规则 |
| `policy_def` | 建议同步定 | `scene_code`、`policy_code`、`decision_mode`、`default_action` | 1 | 一期主链路建议 1 条 `FIRST_HIT` 主策略 |
| `policy_rule_ref` | 建议同步定 | `scene_code`、`policy_code`、`rule_code`、`order_no`、`stop_on_hit` | 3 | 保证规则执行顺序可控 |

## 6.3 一期可以后置但建议保留结构的表

| 表 | 一期结论 | 原因 |
| --- | --- | --- |
| `list_set` | 后置 | 一期不强依赖正式名单中心，但建议保留结构和少量 seed |
| `list_item` | 后置 | 可用 SQL seed + Redis seed 替代完整名单后台 |
| `ingest_source` | 后置 | 如果一期仍用 mock producer，可不阻塞主链路 |
| `ingest_mapping_def` | 后置 | 仅在要演示原始报文标准化时需要 |
| `policy_score_band` | 后置 | `SCORE_CARD` 结构可预留，但一期不作为主阻塞项 |
| `risk_audit_log` | 后置 | 审计属于 P1 推荐能力 |
| `replay_job` | 后置 | 重型回放不是一期最小闭环必需 |
| `risk_metric_snapshot` | 后置 | Dashboard / 基础监控属于 P1 推荐能力 |

## 7. Redis 冻结清单

## 7.1 一期主链路必须先定的 key 模式

| Key 模式 | 值类型 | 一期结论 | 最少 seed 数 | 推荐样例值 |
| --- | --- | --- | ---: | --- |
| `pulsix:list:black:device:{deviceId}` | String / 单值存在 | 必须 | 1 | `pulsix:list:black:device:D0009 -> 1` |
| `pulsix:profile:user:risk:{userId}` | String | 必须 | 3 | `U1001 -> H`、`U2002 -> L`、`U4004 -> M` |

## 7.2 一期建议同步准备的 key 模式

| Key 模式 | 值类型 | 一期建议 | 最少 seed 数 | 用途 |
| --- | --- | --- | ---: | --- |
| `pulsix:list:black:ip:{ip}` | String | 建议 | 1 | 补辅助样例 |
| `pulsix:list:white:user:{userId}` | String | 建议 | 1 | 支撑 `LOGIN_RISK` 白名单保护示例 |
| `pulsix:profile:device:score:{deviceId}` | String | 建议 | 1 | 设备画像扩展示例 |
| `pulsix:profile:user:{userId}` | Hash | 建议 | 1 | 多属性画像扩展示例 |
| `pulsix:cache:scene:active_version:{sceneCode}` | String | 建议 | 2 | 记录当前生效版本 |

## 7.3 一期不必强依赖的 Redis key

| Key 模式 | 一期结论 | 原因 |
| --- | --- | --- |
| `pulsix:feature:{scene}:...` | 后置 | 一期主流式特征应优先落在 Flink Keyed State，而不是先做 Redis 物化副本 |
| `pulsix:cache:simulation:{sceneCode}:{caseId}:{versionNo}` | 后置 | 仿真缓存可后补，不阻塞执行语义 |
| `pulsix:cache:warmup:{sceneCode}:{featureCode}` | 后置 | 预热优化不阻塞一期闭环 |

## 7.4 Redis 还要同时冻结的非功能项

- key 命名规范：统一采用 `pulsix:{domain}:{type}:{scene?}:{bizKey}`
- 过期策略：名单尽量不过期；画像按上游刷新周期；不要把本地缓存 TTL 和 Redis TTL 混为一谈
- lookup 默认值：例如 `device_in_blacklist=false`、`user_risk_level=L`
- lookup 超时：建议一期样例统一先用 `20ms`
- 引擎本地缓存 TTL：建议一期样例统一先用 `30s`

## 8. Kafka 冻结清单

## 8.1 一期必建 topic

| Topic | 一期结论 | Key 建议 | 最少样例条数 | 用途 |
| --- | --- | --- | ---: | --- |
| `pulsix.event.standard` | 必建 | `sceneCode` | 8 | 标准事件输入主流 |
| `pulsix.decision.result` | 必建 | `traceId` | 3 | 最终结果流 |
| `pulsix.decision.log` | 必建 | `traceId` | 3 | 详细追溯流 |
| `pulsix.engine.error` | 必建 | `traceId` | 2 | 引擎错误流 |
| `pulsix.event.dlq` | 必建 | `traceId` 或 `rawEventId` | 1 | 非法事件 / 死信流 |

## 8.2 建议预留 topic

| Topic | 一期结论 | 说明 |
| --- | --- | --- |
| `pulsix.ingest.error` | 预留 | 如果要把接入错误和 DLQ 分开治理，可提前预留 |

## 8.3 一期必须先冻结的消息结构

| Topic | 对应对象 | 必须字段 |
| --- | --- | --- |
| `pulsix.event.standard` | `RiskEvent` | `eventId`、`traceId`、`sceneCode`、`eventType`、`eventTime`、主实体字段、`result`、`ext` |
| `pulsix.decision.result` | `DecisionResult` | `traceId`、`eventId`、`sceneCode`、`version`、`finalAction`、`finalScore`、`hitRuleCodes`、`latencyMs` |
| `pulsix.decision.log` | `DecisionLogRecord` | `traceId`、`eventId`、`sceneCode`、`policyCode`、`featureSnapshot`、`hitRulesDetail`、`decisionDetail` |
| `pulsix.engine.error` | `EngineErrorRecord` | `traceId`、`eventId`、`sceneCode`、`version`、`errorStage`、`errorType`、`errorCode`、`errorMessage` |
| `pulsix.event.dlq` | `IngestError` 结构 | `traceId`、`rawEventId`、`sourceCode`、`ingestStage`、`errorCode`、`rawPayload`、`standardPayload` |

## 8.4 一期输入样例建议

为了真正验证 `Stream Feature`，`pulsix.event.standard` 不能只准备 1 条交易消息。建议至少准备：

- 6 条连续 `TRADE_RISK` 标准交易事件，用于形成 `5m / 30m / 1h` 窗口结果
- 1 条黑名单设备交易事件，用于验证立即拒绝
- 1 条正常交易事件，用于验证放行

## 9. Doris 冻结清单

如果一期只验证本地执行和 Flink 主链路，Doris 不是阻塞项；但如果要把“可查、可演示、可追溯”一并落地，建议直接冻结最小读模型。

| 表 | 一期建议 | 必须字段 | 最少样例条数 |
| --- | --- | --- | ---: |
| `dwd_decision_result` | 建议同步定 | `trace_id`、`event_id`、`scene_code`、`version_no`、`final_action`、`final_score`、`latency_ms`、`raw_json` | 3 |
| `dwd_decision_log` | 建议同步定 | `trace_id`、`event_id`、`scene_code`、`policy_code`、`feature_snapshot_json`、`hit_rules_json`、`decision_detail_json` | 3 |
| `dwd_rule_hit_log` | 建议同步定 | `trace_id`、`event_id`、`scene_code`、`rule_code`、`hit_flag`、`hit_reason`、`score` | 6 |
| `dwd_ingest_error_log` | 建议同步定 | `trace_id`、`raw_event_id`、`source_code`、`ingest_stage`、`error_code`、`error_message` | 1 |
| `dwd_engine_error_log` | 建议同步定 | `trace_id`、`event_id`、`scene_code`、`version_no`、`error_stage`、`error_code`、`error_message` | 2 |

## 10. 示例数据包冻结清单

## 10.1 一期主样例包：`TRADE_RISK`

建议把一期主链路样例全部围绕一套交易风控数据包来组织。

### A. 设计态样例

- `scene_def`：1 条 `TRADE_RISK`
- `event_schema`：1 条 `trade`
- `event_field_def`：至少 10 个标准字段
- `feature_def`：至少 6 个特征
- `feature_stream_conf`：3 条
- `feature_lookup_conf`：2 条
- `feature_derived_conf`：2 条
- `rule_def`：3 条
- `policy_def`：1 条 `FIRST_HIT`
- `policy_rule_ref`：3 条规则顺序

### B. 运行态样例

- `scene_release` active 版本：1 条
- `scene_release` candidate 版本：1 条
- Redis seed：设备黑名单 1 条、用户风险等级 3 条
- Kafka 标准事件：至少 8 条

### C. 推荐规则链路样例

建议至少固定下面 4 类样例：

1. 正常交易放行
2. 高频高风险交易复核
3. 黑名单设备直接拒绝
4. 多账号设备 + 高风险画像拒绝

### D. 推荐仿真样例

| 用例编码 | 场景 | 目标 | 期望动作 |
| --- | --- | --- | --- |
| `SIM_TRADE_PASS` | `TRADE_RISK` | 正常交易不误杀 | `PASS` |
| `SIM_TRADE_REVIEW` | `TRADE_RISK` | 高频高风险交易进入复核 | `REVIEW` |
| `SIM_TRADE_REJECT` | `TRADE_RISK` | 黑名单或多账号高风险设备直接拒绝 | `REJECT` |

## 10.2 辅助样例包：`LOGIN_RISK`

`LOGIN_RISK` 不一定要作为一期主链路，但很适合补足“原始报文 -> 标准化 -> DLQ / review”的场景。

建议固定：

- 1 条原始 Beacon 登录报文样例
- 1 条标准登录事件样例
- 1 条登录 review 仿真样例
- 1 条非法登录报文 DLQ 样例

## 10.3 样例最少条数汇总

| 类别 | 最少条数 |
| --- | ---: |
| `scene_release` | 2 |
| `simulation_case` | 3 |
| `simulation_report` | 3 |
| `decision_log` | 3 |
| `rule_hit_log` | 6 |
| Redis seed | 4 |
| 标准交易事件消息 | 8 |
| 引擎错误消息 | 2 |
| DLQ 消息 | 1 |

## 11. 一期推荐冻结结果

如果要尽快进入开发，建议你现在就按下面这个结果执行：

### 11.1 必须立即冻结

- 运行时 6 大对象契约
- `scene_release`
- `simulation_case / simulation_report`
- `decision_log / rule_hit_log`
- Kafka 5 个核心 topic 及消息结构
- Redis 2 个主 lookup key 模式
- `TRADE_RISK` 一套主样例包

### 11.2 建议同步冻结

- `scene_def / event_schema / event_field_def / event_sample`
- `feature_def + feature_stream_conf + feature_lookup_conf + feature_derived_conf`
- `rule_def / policy_def / policy_rule_ref`
- Doris 5 张最小查询读表

### 11.3 可以后置

- `list_set / list_item`
- `ingest_source / ingest_mapping_def`
- `policy_score_band`
- `risk_audit_log / replay_job / risk_metric_snapshot`

## 12. 下一步建议

这份清单确认后，下一步就可以继续产出两类落地物：

1. 一份“按本清单收敛后的一期标准命名与字段字典”
2. 一份“按 `TRADE_RISK` 主样例包展开的具体数据结构和示例数据”

前者解决命名漂移，后者直接服务于 SQL、Redis seed、Kafka 消息、仿真用例和 README 演示。
