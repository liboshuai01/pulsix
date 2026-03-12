# `pulsix-module-risk` 管理端页面开发阶段划分

> 基于 `2026-03-12` 仓库现状整理。主要参考：`docs/wiki/风控功能清单.md`、`docs/wiki/项目架构及技术栈.md`；辅助参考：`docs/wiki/风控功能模块与表映射.md`、`docs/wiki/pulsix-engine-kernel-一期开发指南.md`、`docs/sql/pulsix-risk.sql`、`docs/参考资料/实时风控系统第20章：Spring Boot 控制平台的模块设计与实现.md`、`docs/参考资料/实时风控系统第22章：项目代码结构设计与从0到1的落地顺序.md`、`docs/参考资料/实时风控系统第23章：测试体系——单元测试、仿真测试、回放测试、联调测试.md`。
>
> 当前交接状态：`S00 ~ S04` 已完成，下一阶段从 `S05` 开始。
> 风控管理端当前范围**不做多租户**；后续新增 risk 表 DO 默认加 `@TenantIgnore`，除非用户明确要求租户化。

## 1. 文档目标

- 给你和后续 AI 一个统一的开发顺序，避免单阶段做太多导致出错。
- 每个阶段都能通过“页面操作 + MySQL 落库 + 必要时内核结果”直接人工验证。
- 功能范围按 `风控功能清单.md` 的【必须做】+【推荐做】推进，但优先保证主链路闭环。
- 尽量对齐当前 `pulsix-engine / pulsix-kernel` 已有能力，尤其复用现有 `TRADE_RISK` 样例链路。

## 2. 固定约束

- `docs/sql/pulsix-system-infra.sql` 只可复用，**不能改表结构**。
- `docs/sql/pulsix-risk.sql` 可以调整；后续每做完一个功能，都要补该功能的**示例数据**。
- `pulsix-module-risk` 当前几乎是空模块，开发时应直接参考 `pulsix-module-system` / `pulsix-module-infra` 的后端分层与 `pulsix-ui` 的页面模式。
- 单阶段建议控制在：**1 个主功能、最多 2 个页面、最多 3 张核心表、1 条清晰验收链路**。
- 发布、仿真、回放类能力必须复用 `pulsix-kernel`，不要在管理端单独写一套判断逻辑。
- 日志、监控、错误查询页面的**第一版**优先基于 MySQL 表和示例数据打通，不把 Doris / Flink / Kafka / CDC 联调绑进同一阶段。

## 3. 跨阶段统一规则

- 统一用 `TRADE_RISK` 作为演示场景主样例，减少多场景切换带来的复杂度。
- 每个阶段都应同时交付：后端 API、前端页面、菜单/按钮权限、`pulsix-risk.sql` 示例数据。
- 风控管理端当前阶段（`S00 ~ S21`）默认**不做多租户**；`pulsix-module-risk` 新增 DO 如对应表无 `tenant_id`，必须加 `@TenantIgnore`，避免 SQL 被自动拼接 `tenant_id` 条件。
- 从有写操作的阶段开始，建议同步写 `risk_audit_log`；查询页可后置，但记录动作不要后置。
- `pulsix-risk.sql` 的示例数据要尽量**可重复执行**：优先固定 `code`，必要时按 `scene_code/code` 先删后插。
- 风控菜单、按钮权限可以写入 `pulsix-risk.sql` 对 `system_menu` 的初始化数据中；这不算修改 `pulsix-system-infra.sql`。

## 4. 三个里程碑

- **M1：设计态主链路可配** —— 做完 `S00 ~ S12`。
- **M2：平台核心闭环可演示** —— 做完 `S13 ~ S16`。
- **M3：推荐能力补齐** —— 做完 `S17 ~ S21`。

## 5. 阶段划分

### 5.0 当前已完成阶段（交接用）

- `S00`：已完成；风控菜单骨架、占位页、权限前缀、`pulsix-risk.sql` 菜单初始化已落地。
- `S01`：已完成；场景管理已落地，支持列表、创建、编辑、启停、详情，主样例为 `TRADE_RISK`。
- `S02`：已完成；事件 Schema 管理已落地，支持列表、创建、编辑、详情，主样例为 `TRADE_EVENT`。
- `S03`：已完成；事件字段管理已落地，支持列表、创建、编辑、删除、排序，主样例覆盖 `eventId/userId/deviceId/ip/amount/result`。
- `S04`：已完成；事件样例管理与标准事件预览已落地，支持列表、创建、编辑、删除、预览，预览当前仅按同名字段/`fieldPath`/默认值生成标准事件，原始字段映射留到 `S06`。
- **下一阶段**：从 `S05` 继续，不要回头重复做 `S00 ~ S04`。

### 5.1 主链路建模阶段（先做）

| 阶段 | 本阶段只做什么 | 关键表 | 页面/操作验收 | `pulsix-risk.sql` 本阶段至少补什么 |
| --- | --- | --- | --- | --- |
| `S00` | 风控菜单骨架 + 空页面入口 + 权限前缀约定 | `system_menu` | 登录后能看到“风控平台”一级菜单和子菜单占位页 | 风控目录、子菜单、按钮权限样例 |
| `S01` | 场景管理：列表、创建、编辑、启停、详情 | `scene_def` | 页面新增 `TRADE_RISK`，修改名称/状态后刷新可见 | `TRADE_RISK` 场景样例 |
| `S02` | 事件 Schema 管理：事件编码、事件类型、基础校验 | `event_schema` | 在 `TRADE_RISK` 下新增 `TRADE_EVENT`，列表与详情可查 | `TRADE_EVENT` Schema 样例 |
| `S03` | 事件字段管理：字段名、类型、必填、顺序、说明 | `event_field_def` | 可维护 `eventId/userId/deviceId/ip/amount/result` 等字段并落库 | `TRADE_EVENT` 字段样例 |
| `S04` | 事件样例管理 + 标准事件预览 | `event_sample` | 录入一条交易样例 JSON，点击预览可看到标准事件内容 | 成功样例、异常样例各 1 条 |
| `S05` | 接入源管理：来源编码、接入方式、鉴权配置、启停 | `ingest_source` | 新增 HTTP/SDK 来源，页面可启停、可查看鉴权配置 | `trade_http_demo`、`trade_sdk_demo` |
| `S06` | 接入字段映射：原始字段到标准字段映射 + 预览 | `ingest_mapping_def` | 输入原始报文后，页面可预览映射结果 | `uid->userId`、`dev_id->deviceId`、`pay_amt->amount` 等映射 |
| `S07` | 名单中心：名单集合、名单项、启停、手动同步 Redis | `list_set`、`list_item` | 新增设备黑名单，录入名单项，点击同步后 DB 数据可查，Redis 中有对应 key | `DEVICE_BLACKLIST`、若干设备名单项 |
| `S08` | Stream 特征：仅做 `COUNT/SUM/MAX/LATEST/DISTINCT_COUNT` | `entity_type_def`、`feature_def`、`feature_stream_conf` | 页面可创建并查看流式特征；不做复杂窗口/任意脚本 | `user_trade_cnt_5m`、`user_trade_amt_sum_30m`、`device_bind_user_cnt_1h` |
| `S09` | Lookup 特征：Redis set/hash 查询配置 | `feature_def`、`feature_lookup_conf` | 页面可维护 key 表达式、sourceRef、默认值、超时 | `device_in_blacklist`、`user_risk_level` |
| `S10` | Derived 特征：依赖选择、表达式校验、结果类型 | `feature_def`、`feature_derived_conf` | 页面可配置依赖并校验 Aviator/Groovy 表达式 | `high_amt_flag`、`trade_burst_flag` |
| `S11` | 规则中心：条件表达式、优先级、动作、命中原因、启停 | `rule_def` | 页面可新增/编辑规则，并可做语法校验 | `R001`、`R002`、`R003` 规则样例 |
| `S12` | 策略中心：**只做 `FIRST_HIT`**，规则排序、默认动作 | `policy_def`、`policy_rule_ref` | 页面可拖序或排序规则，保存后详情正确展示 | `TRADE_RISK_POLICY` + 规则顺序样例 |

### 5.2 发布与验证阶段（主链路闭环）

| 阶段 | 本阶段只做什么 | 关键表 | 页面/操作验收 | `pulsix-risk.sql` 本阶段至少补什么 |
| --- | --- | --- | --- | --- |
| `S13` | 发布预检：依赖分析、表达式校验、快照预览、候选版本生成 | `scene_release` | 点击“预检/编译”后，页面能看到依赖摘要、校验结果、快照 JSON 预览 | `scene_release` 候选版本样例（可用 `DRAFT`/`VALIDATED` 状态） |
| `S14` | 正式发布与回滚：版本号、生效时间、发布记录、基础回滚 | `scene_release` | 发布后出现新版本；回滚后可看到回滚来源版本 | `TRADE_RISK v12` 及一个可回滚样例版本 |
| `S15` | 仿真测试：输入事件、指定场景/版本、展示特征/命中链路/最终动作 | `simulation_case`、`simulation_report` | 页面执行仿真后，可看到 `REJECT/REVIEW/PASS`、命中规则、特征快照 | 仿真用例、仿真报告样例 |
| `S16` | 决策日志与命中明细：按 `traceId/eventId` 查询，查看规则命中明细 | `decision_log`、`rule_hit_log` | 页面能搜到一条样例日志，并下钻到命中规则/命中原因/版本号 | 决策日志、规则命中日志样例 |

### 5.3 推荐增强阶段（主链路稳定后做）

| 阶段 | 本阶段只做什么 | 关键表 | 页面/操作验收 | `pulsix-risk.sql` 本阶段至少补什么 |
| --- | --- | --- | --- | --- |
| `S17` | `SCORE_CARD` 策略补齐：分值汇总、分段动作映射 | `policy_score_band` | 页面可维护分数区间；给定总分可预览动作结果 | 一套评分段样例 |
| `S18` | 接入治理：接入异常 / DLQ 查询、来源维度筛选 | `ingest_error_log` | 页面按来源、错误码、时间范围能查到异常记录 | 接入异常、坏报文样例 |
| `S19` | Dashboard / 基础监控：事件量、决策量、动作占比、延迟指标 | `risk_metric_snapshot` | 页面能看到趋势图与统计卡片；数据随筛选条件变化 | 分钟级指标快照样例 |
| `S20` | 审计日志页：查看谁改了什么、谁发布了什么 | `risk_audit_log` | 页面能按对象类型、操作人、时间过滤审计日志 | 场景修改、规则修改、发布、回滚审计样例 |
| `S21` | 回放对比：基线版本 vs 目标版本差异分析 | `replay_job` | 页面能发起一条回放任务并查看差异摘要 | 一条回放任务样例及 diff 摘要 |

## 6. 每阶段的最小验收模板

后续每一阶段开发完成后，建议都按下面顺序人工验收：

1. 页面菜单是否可见，权限按钮是否显示正常。
2. 列表页能否查到 `pulsix-risk.sql` 中初始化的样例数据。
3. 新增 / 编辑 / 启停 / 删除（若本阶段支持）是否能通过页面完成。
4. MySQL 对应表是否正确落库，关键关联字段是否正确。
5. 若阶段涉及内核能力（发布 / 仿真 / 回放），结果是否与 `TRADE_RISK` 预期一致。
6. 再次导入或重新执行 `pulsix-risk.sql` 后，样例数据是否仍可用于快速验证。

## 7. 对后续 AI 的直接约束

- 一次只做**当前阶段**，不要跨阶段顺手补很多“看起来相关”的功能。
- 当前交接点在 `S04`；后续 AI 直接从 `S05` 开始，不要重复改造 `S00 ~ S04`，除非用户明确要求返工。
- 风控管理端当前范围明确**不做多租户**；不要再让 `scene_def`、`event_schema`、`event_field_def` 等无 `tenant_id` 字段的表被注入租户条件。
- 当前阶段没要求的高级能力，不要提前做，例如：复杂序列规则、拖拽编排、灰度发布、多租户风控隔离、同步在线决策 API。
- `FIRST_HIT` 先做稳，再补 `SCORE_CARD`；不要反过来。
- 发布、仿真、回放必须复用已有 `pulsix-kernel` 能力，不能自己在页面层拼装另一套规则执行逻辑。
- 管理端第一版优先解决“**可配置、可发布、可验证、可追溯**”，不是一开始就把所有接入链路和分析链路做到生产级。

## 8. 推荐执行顺序总结

最推荐的实际开发顺序是：

`S00 -> S01 -> S02 -> S03 -> S04 -> S05 -> S06 -> S07 -> S08 -> S09 -> S10 -> S11 -> S12 -> S13 -> S14 -> S15 -> S16 -> S17 -> S18 -> S19 -> S20 -> S21`

如果中途要设“可停点”，建议停在下面三个位置：

- **停点 A：`S12` 后** —— 设计态页面已基本齐全。
- **停点 B：`S16` 后** —— 已具备完整演示闭环。
- **停点 C：`S21` 后** —— 必做 + 推荐做功能全部覆盖完成。
