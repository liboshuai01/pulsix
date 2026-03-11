# `pulsix-engine / pulsix-kernel` 一期阶段 1~6 review 问题清单

> 评估日期：`2026-03-12`
>
> 评估范围：`docs/wiki/pulsix-engine-kernel-一期开发指南.md` 中阶段 1 ~ 6 的“已完成”声明，以及 `pulsix-framework/pulsix-kernel` / `pulsix-engine` 当前实现。
>
> 已复核基线：`mvn -q -pl pulsix-framework/pulsix-kernel,pulsix-engine test`、`mvn -q -pl pulsix-engine test` 均通过。

---

## 1. 结论摘要

- **阶段 1 ~ 4：可认定已完成。** 当前没有发现明显阻塞性偏差，模块归位、本地仿真、轻量回放、`scene_release` 快照接入主链路均已落地。
- **阶段 5 ~ 6：主干能力已落地，但不建议表述为“圆满完成”。** 目前仍存在若干会影响“原本意图”达成度的问题，主要集中在 Kafka 输出协议、Demo 默认行为、Redis 降级兜底边界，以及 CDC 实现收口度。
- **建议对外表述：** “阶段 1 ~ 4 已完成；阶段 5 ~ 6 已打通主链路并完成基础联调，但仍有少量收口项待修复。”

---

## 2. 修复优先级总览

| 优先级 | 问题 | 影响阶段 | 当前判断 |
| --- | --- | --- | --- |
| P1 | Redis lookup 存在未完全兜底的异常路径 | 阶段 6 | 偏离“失败时不把整个引擎拖死” |
| P1 | Kafka 输出未写入消息 key | 阶段 5 | 偏离 Kafka topic 规划与下游消费预期 |
| P1 | Demo 默认配置仍依赖真实 Redis | 阶段 5 / 6 | 偏离“保留最小样例回归基线”的原意 |
| P2 | CDC 链路存在 bootstrap + initial snapshot 双重首发 | 阶段 4 | 功能可用，但实现不够收口 |

> 说明：这里没有列出阶段 7 及之后的问题，例如版本治理、运行时约束、Groovy 安全等，因为它们已被开发指南明确归入后续阶段，不属于本次阶段 1 ~ 6 的验收缺口。

---

## 3. 问题清单

### 3.1 P1：Redis lookup 未完全满足“降级兜底、不拖死主链路”

**问题描述**

- 当前 `RedisLookupService` 只对 `JedisConnectionException` 做 fallback 处理。
- 如果 Redis 查询过程中抛出的是其他运行时异常，异常会直接向上抛出。
- `DecisionExecutor` 在 lookup 调用外层没有再做 feature 级兜底，最终会进入 `decision-execute` 异常路径，而不是“输出明确 lookup 错误 + 返回默认值/缓存值继续执行”。

**为什么这算偏离原意**

- 阶段 6 的目标是：lookup 失败时“输出明确错误记录，但不把整个引擎拖死”。
- 当前实现对“超时 / 连接失败”覆盖较好，但对“其他 Redis 访问异常”仍可能中断整条决策。

**对应代码点**

- `pulsix-framework/pulsix-kernel/src/main/java/cn/liboshuai/pulsix/engine/feature/RedisLookupService.java`
  - `lookup(...)`
  - `fetchFromRedis(...)`
- `pulsix-framework/pulsix-kernel/src/main/java/cn/liboshuai/pulsix/engine/core/DecisionExecutor.java`
  - `executeLookupFeature(...)`

**修复建议**

- 方案 A：在 `RedisLookupService.lookup(...)` 内统一兜底所有可预期的 Redis 访问异常，并一律转换为 `LookupResult.fallback(...)`。
- 方案 B：在 `DecisionExecutor.executeLookupFeature(...)` 外再包一层保护，将 lookup provider 抛出的异常转为 `EngineErrorRecord + defaultValue fallback`。
- 推荐优先采用 **方案 A + 方案 B 双保险**：provider 负责错误语义，executor 负责执行安全边界。

**验收标准**

- 人为制造非 `JedisConnectionException` 的 lookup 异常时，仍能产出 `DecisionResult`。
- 错误流可看到明确的 lookup 错误记录。
- `defaultValue` 或缓存值生效，主链路不因为 lookup 异常中断。

---

### 3.2 P1：Kafka 输出未设置消息 key，偏离 topic 规划

**问题描述**

- 当前 Kafka sink 只设置了 `topic` 和 `value serialization schema`，没有设置 record key。
- 结果是 `pulsix.decision.result`、`pulsix.decision.log`、`pulsix.engine.error` 发送到 Kafka 时不会按 `traceId` 写 key。

**为什么这算偏离原意**

- 辅助文档 `docs/wiki/kafka-redis-doris-落地清单.md` 中已经明确：
  - `pulsix.decision.result` 推荐 key 为 `traceId`
  - `pulsix.decision.log` 推荐 key 为 `traceId`
  - `pulsix.engine.error` 推荐 key 为 `traceId`
- 不写 key 不会阻断功能，但会影响：
  - 同一 `traceId` 的局部有序性
  - 下游基于 key 的分区消费习惯
  - 与文档规划的一致性

**对应代码点**

- `pulsix-engine/src/main/java/cn/liboshuai/pulsix/engine/flink/DecisionEngineJob.java`
  - `buildKafkaSink(...)`
- 相关规划文档：
  - `docs/wiki/kafka-redis-doris-落地清单.md`

**修复建议**

- 为三条输出流补充 key 提取逻辑：优先取 `traceId`，缺失时再退化到 `eventId` 或空 key。
- 可新增按类型提取 key 的轻量接口，避免 `DecisionResult / DecisionLogRecord / EngineErrorRecord` 分别写三套 sink 代码。

**验收标准**

- Kafka 中同一 `traceId` 的输出记录具有稳定 key。
- 配置不变时，仍保持现有 JSON value 协议不变。
- `DecisionEngineJobOptionsTest` 或新增测试能覆盖输出 key 策略。

---

### 3.3 P1：Demo 默认配置仍依赖真实 Redis，弱化“最小样例基线”

**问题描述**

- 当前默认配置中：
  - `snapshot-source=demo`
  - `event-source=demo`
  - 但 `lookup-source=redis`
- 这意味着默认直接运行 `DecisionEngineJob` 时，即便事件与快照都走 Demo，lookup 仍默认依赖外部 Redis。

**为什么这算偏离原意**

- 阶段 5 明确要求“保留 Demo 模式，避免回归测试失去最小样例”。
- 现在的默认行为更像“半 Demo、半正式模式”，不再是零外部依赖的最小样例。
- 虽然测试能通过，但运行默认 Demo 时会把环境依赖抬高，不利于快速回归与新同学上手。

**对应代码点**

- `pulsix-engine/src/main/resources/pulsix-engine.properties`
  - `pulsix.engine.lookup-source=redis`
- `pulsix-engine/src/main/java/cn/liboshuai/pulsix/engine/flink/DecisionEngineJobOptions.java`
  - `parse(...)` 中 `lookup source` 默认值
- `pulsix-engine/src/test/java/cn/liboshuai/pulsix/engine/flink/DecisionEngineJobOptionsTest.java`
  - `shouldDefaultToDemoSourceAndPrintSinks()`

**修复建议**

- 把默认 `lookup-source` 改回 `demo`，让默认 Demo 路径保持“快照 / 事件 / lookup 全部最小样例化”。
- 如果希望本地联调继续方便，可新增单独的 `pulsix-engine-local.properties` 或 README 中的“联调推荐配置”，而不是改默认基线。

**验收标准**

- 零外部依赖下直接运行 `DecisionEngineJob`，即可跑通 Demo 主链路。
- 切到联调配置后，仍可用真实 Redis 跑 smoke。
- 默认配置与“最小回归基线”表述重新一致。

---

### 3.4 P2：CDC 首次启动存在 bootstrap 与 initial snapshot 双发

**问题描述**

- `MySqlCdcSceneSnapshotSourceFactory` 先做一次 JDBC bootstrap。
- 随后 MySQL CDC 又使用 `StartupOptions.initial()` 启动，CDC 本身也会做一轮初始快照。
- 两路 union 之后，首次启动时同一快照大概率会被送两次。

**为什么这不是阻塞问题**

- 当前 `DecisionBroadcastProcessFunction` 对“同版本 + 同 checksum”场景会直接忽略，因此功能上基本无害。
- 但这个实现说明快照接入虽然“能用”，还没有完全收口成最简洁、最可解释的形态。

**对应代码点**

- `pulsix-engine/src/main/java/cn/liboshuai/pulsix/engine/flink/snapshot/MySqlCdcSceneSnapshotSourceFactory.java`
  - JDBC bootstrap
  - `StartupOptions.initial()`
- `pulsix-engine/src/main/java/cn/liboshuai/pulsix/engine/flink/DecisionBroadcastProcessFunction.java`
  - `shouldIgnoreEnvelope(...)`

**修复建议**

- 二选一：
  - 保留 JDBC bootstrap，则 CDC 改为只接增量；
  - 保留 CDC initial snapshot，则移除额外 JDBC bootstrap。
- 如果短期不改实现，至少要在代码注释或文档里明确“双发由下游幂等吸收”的设计理由。

**验收标准**

- 首次启动时同一版本快照只进入一次，或双发行为被明确设计化、文档化。
- 对“初始快照 + 增量更新”链路有清晰测试覆盖。

---

## 4. 建议修复顺序

### 第一批：先补主链路一致性

1. 修 `Redis lookup` 的兜底边界。
2. 给 Kafka 输出补 `traceId` key。

**原因**

- 这两项最直接影响“阶段 5 / 6 是否符合原始目标”的判断。
- 修完后，Kafka 正式链路和 Redis 正式链路都更接近“可对外说明完成”的状态。

### 第二批：恢复最小样例基线

3. 把默认 `lookup-source` 调回 `demo`，把真实 Redis 放到联调配置里。

**原因**

- 这项不一定影响生产联调，但会影响回归基线的纯度和可复用性。

### 第三批：做实现收口

4. 统一 CDC 首次加载策略，避免 bootstrap / initial snapshot 双发。

**原因**

- 这是“工程整洁度”问题，优先级低于主链路语义正确性。

---

## 5. 当前阶段建议表述

可采用下面这段作为后续文档或汇报口径：

> `pulsix-kernel` 的模块归位、本地仿真、轻量回放、golden case 回归，以及 `scene_release` 的 `demo/file/jdbc/cdc` 快照接入已经完成；Kafka 与 Redis 的正式主链路也已打通并具备基础回归能力。当前剩余问题主要是阶段 5 ~ 6 的少量收口项，包括 Kafka 输出 key、Demo 默认基线、Redis 降级边界与 CDC 首发实现收口。修复这些问题后，再对阶段 5 ~ 6 使用“圆满完成”的表述更稳妥。

