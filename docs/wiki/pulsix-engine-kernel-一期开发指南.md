## 1. 文档用途

这份文档现在作为 `pulsix-engine / pulsix-kernel` 第一期开发的统一主文档。

用途只有三个：

- 帮你快速确认当前阶段到底做什么、不做什么。
- 帮后续 AI 助手仅凭这一份文档理解第一期开发边界。
- 作为 `kernel + engine` 一期开发的默认设计依据。

如果只读一份文档，优先读这份。

---

## 2. 一期结论

当前阶段只做两条主线：

- `pulsix-kernel`
- `pulsix-engine`

当前不作为开发重点：

- `pulsix-module-risk`
- `pulsix-access`

当前替代方案：

- 发布快照：直接用 SQL 写 `scene_release`
- mock 事件：直接用 Kafka 工具、IDEA Kafka 插件或本地 producer 发送

一期目标不是先把平台做完整，而是先验证：

- `kernel` 的执行语义能否稳定落地
- `engine` 的 Flink 主链路能否跑通
- 本地执行、仿真、Flink 执行能否尽量一致

一期明确不优先做：

- 页面、CRUD、控制台包装
- 正式发布后台
- 正式接入层
- 多租户、灰度发布、多环境推广
- 复杂策略编排
- 重型 `Groovy` 能力
- 完整 Dashboard / 分析面

一句话：

> 先把执行内核打透，再补控制面和接入面。

---

## 3. 一期最小系统边界

当前最小系统只需要下面这些东西：

```text
1. 稳定的 SceneSnapshot 结构
2. 一张 scene_release 表
3. 稳定的 RiskEvent 结构
4. 一个 Kafka 标准事件 topic
5. pulsix-kernel 本地执行 / 仿真 / 轻量回放能力
6. pulsix-engine Flink 执行能力
7. 一组固定样例数据与回归用例
```

只要满足以下四点，就足够支撑一期：

- 能构造并存储快照
- 能输入标准事件
- 能稳定执行并输出结果
- 能重复验证和回归验证

---

## 4. 引擎主链路

```text
Kafka 标准事件流
    -> 读取 RiskEvent
    -> keyBy(sceneCode)
    -> connect(快照广播流)
    -> 找到当前 CompiledSceneRuntime
    -> 构建 EvalContext
    -> 计算 Stream Feature
    -> 计算 Lookup Feature
    -> 计算 Derived Feature
    -> 执行 Rule
    -> 执行 Policy
    -> 输出 DecisionResult
    -> Side Output: DecisionLogRecord / EngineErrorRecord

scene_release 快照流
    -> 读取 SceneSnapshotEnvelope
    -> Broadcast State
    -> RuntimeCompiler 编译并激活本地运行时缓存
```

理解方式：

- 事件流提供待决策数据
- 快照流提供当前执行逻辑
- `engine` 负责在 Flink 中把两者组合起来

---

## 5. 最重要的工程原则

### 5.1 Flink 只认运行时快照

- `pulsix-engine` 消费的是 `SceneSnapshot` / `SceneSnapshotEnvelope`
- 不是设计态几十张表
- `scene_release.snapshot_json` 才是引擎真正消费的配置对象

### 5.2 编译和执行必须分开

- 快照变更时编译
- 事件到来时只执行
- 不在每条事件里临时编译表达式或脚本

也就是：`compile once, execute many`

### 5.3 发布态和运行态必须分开

- 发布态：`SceneSnapshot`
- 运行态：`CompiledSceneRuntime`

运行态对象包含：

- 已编译表达式
- 已编译脚本
- 派生特征执行顺序
- 规则执行顺序

### 5.4 上下文准备和规则执行必须分开

正确顺序固定为：

1. 事件进入
2. 找到当前快照
3. 构建上下文
4. 计算 stream feature
5. 计算 lookup feature
6. 计算 derived feature
7. 执行 rule
8. 执行 policy
9. 输出 result / log / error

### 5.5 只有一套执行语义

后续无论是：

- 本地仿真
- 轻量回放
- Flink 实时执行
- 控制面接口包装

都必须复用同一套 `kernel` 执行语义。

---

## 6. 模块边界

### 6.1 `pulsix-kernel`

负责纯执行语义，建议放：

- `RiskEvent`
- `SceneSnapshot`
- `EvalContext`
- `DecisionResult`
- `DecisionLogRecord`
- `EngineErrorRecord`
- `RuntimeCompiler`
- `SceneRuntimeManager`
- `DecisionExecutor`
- 规则/策略执行逻辑
- 表达式编译抽象
- 本地 runner / 仿真 / 轻量回放 / 测试支撑能力

要求：

- 不依赖 Spring MVC
- 不依赖 MyBatis
- 不依赖 Flink API
- 不依赖控制台页面

### 6.2 `pulsix-engine`

负责 Flink 运行适配，建议放：

- Flink Job
- 事件流消费
- 快照广播流消费
- Broadcast State 适配
- Keyed State 适配
- lookup 适配
- side output 输出
- Kafka Source / Sink

要求：

- 只做运行时适配
- 只认 `SceneSnapshot` / runtime
- 不直接读取设计态表拼执行语义
- 不承载控制面逻辑

### 6.3 `pulsix-module-risk`

当前阶段：

- 先不开发
- 先不做页面、接口、CRUD
- 只保留 `scene_release` 这类最小发布结果

后续阶段：

- 作为控制面接口层
- 依赖 `pulsix-kernel`
- 提供发布、仿真、回放、查询能力

### 6.4 `pulsix-access`

当前阶段：

- 先不开发
- 直接用 Kafka 工具 / mock producer 替代

后续阶段：

- 恢复为正式接入层
- 承载标准化、鉴权、补齐、错误分流能力

---

## 7. 一期必须先稳定的核心契约

### 7.1 `RiskEvent`

至少固定：

- `eventId`
- `sceneCode`
- `eventType`
- `eventTime`
- `traceId`
- `entity fields`
- `ext`

### 7.2 `SceneSnapshot`

至少固定：

- `scene`
- `features`
- `rules`
- `policy`
- `runtimeHints`
- `version`
- `checksum`

### 7.3 `EvalContext`

至少固定：

- `base fields`
- `feature values`
- `lookup values`
- `derived values`
- `metadata`

### 7.4 `DecisionResult`

至少固定：

- `sceneCode`
- `version`
- `finalAction`
- `score`
- `hitRules`
- `hitReasons`
- `latencyMs`
- `traceId`

### 7.5 `DecisionLogRecord`

至少要支持：

- 结果追溯
- 命中链路展示
- 仿真对比
- 版本号、traceId、耗时等基础信息查看

### 7.6 `EngineErrorRecord`

至少要区分：

- 未找到快照
- 表达式执行失败
- 脚本执行失败
- lookup 失败
- 状态处理失败

### 7.7 固定样例

一期必须补齐：

- 样例快照 JSON
- 样例事件 JSON
- 样例 SQL
- 样例 Kafka 消息

---

## 8. 当前代码结构怎么理解

当前代码可以按 6 层理解：

### 8.1 模型层
- `SceneSnapshot`
- `RiskEvent`
- `RuleSpec` / `PolicySpec` / `FeatureSpec`
- `DecisionResult` / `DecisionLogRecord` / `EngineErrorRecord`

### 8.2 上下文层
- `EvalContext`

### 8.3 脚本执行层
- `CompiledScript`
- `DefaultScriptCompiler`
- `AviatorCompiledScript`
- `GroovyCompiledScript`

### 8.4 运行时编译层
- `RuntimeCompiler`
- `CompiledSceneRuntime`
- `SceneRuntimeManager`

### 8.5 决策执行层
- `DecisionExecutor`
- `LocalDecisionEngine`
- `InMemoryStreamFeatureStateStore`
- `InMemoryLookupService`

### 8.6 Flink 适配层
- `DecisionBroadcastProcessFunction`
- `DecisionEngineJob`
- `EngineOutputTags`

---

## 9. 最关键的几个类

### 9.1 `RuntimeCompiler`

负责把快照编译成运行态对象，重点包括：

- 编译 stream feature 相关表达式
- 编译 lookup feature 相关表达式
- 编译 derived feature 表达式
- 编译 rule 表达式
- 派生特征依赖排序
- 规则执行顺序排序

输出产物：`CompiledSceneRuntime`

### 9.2 `SceneRuntimeManager`

负责维护当前生效的本地运行时缓存：

- 收到快照后编译并激活
- 按 `sceneCode` 查询当前 active runtime

后续要增强：

- 多版本并存
- 延迟生效
- 回滚
- 编译失败保留旧版本

### 9.3 `DecisionExecutor`

当前执行主入口，负责：

- 校验事件
- 构建上下文
- 计算 stream / lookup / derived features
- 执行规则
- 执行策略
- 组装结果

### 9.4 `LookupService`

lookup 能力抽象。当前可先用内存实现，后续再替换为 Redis。

真实版重点：

- timeout
- 本地短 TTL cache
- 默认值兜底
- 降级策略
- 指标统计

### 9.5 `StreamFeatureStateStore`

流式特征状态抽象。当前可先用内存实现，后续再替换为 Flink Keyed State。

真实版重点：

- bucket 结构
- TTL
- timer 清理
- state schema 迁移

### 9.6 `DecisionBroadcastProcessFunction`

Flink 侧核心适配器，负责：

- 接快照广播流
- 接标准事件流
- 根据当前快照执行决策
- 将结果、日志、错误分流输出

---

## 10. 一期能力边界

### 10.1 特征能力

一期建议至少支持：

- `Stream Feature`：`COUNT`、`SUM`、`MAX`、`LATEST`、`DISTINCT_COUNT`（基础版）
- `Lookup Feature`：先支持最小 lookup 抽象
- `Derived Feature`：先支持表达式型派生

### 10.2 规则能力

规则最小职责：

- 读取上下文
- 判断是否命中
- 生成命中原因
- 记录建议动作与分数

建议用 `RuleHit` 这类中间结果解耦规则与策略。

### 10.3 策略能力

当前代码已留出：

- `FIRST_HIT`
- `SCORE_CARD`

但一期主链路只重点做：

- `FIRST_HIT`

原因：

- 最常见
- 最稳定
- 上线路径最短

### 10.4 表达式与脚本能力

一期建议：

- 表达式主力用 Aviator/DSL
- `Groovy` 先保留抽象，不做重投入

`Groovy` 后续再补：

- 沙箱
- 类加载隔离
- import 限制
- 反射禁用
- 资源访问禁用

---

## 11. 仿真、轻量回放、重型流式回放的边界

### 11.1 仿真

当前优先放在 `pulsix-kernel` 内部，形式可以是：

- 单元测试
- 集成测试
- 本地 runner
- 命令行工具

本质是在验证“给定快照 + 给定事件 -> 最终结果”的执行语义。

### 11.2 轻量回放

当前也优先放在 `pulsix-kernel` 内部。

典型输入：

- 固定事件样例
- JSON 文件
- 数据库导出的样例数据

主要用途：

- 版本回归
- 结果比对
- 问题复现

### 11.3 重型流式回放

后续可以放在 `pulsix-engine` 或专门的 replay runner 中，更适合验证：

- 历史 Kafka 数据回放
- Flink State 语义
- 版本切换过程
- Timer / TTL / 状态清理

但必须坚持：重型回放的核心执行语义仍然复用 `pulsix-kernel`。

### 11.4 后续接口化

后续如果要做正式页面或接口：

- 由 `pulsix-module-risk` 依赖 `pulsix-kernel`
- 提供 `/simulation/*`、`/replay/*`、`/release/*`

原则：`module-risk` 只能做接口包装，不能实现第二套执行逻辑。

---

## 12. 输入、输出与外部依赖

### 12.1 输入

至少有 4 类：

- `RiskEvent`：标准事件主输入
- `SceneSnapshotEnvelope`：快照广播输入
- Lookup 数据：通常来自 Redis
- 流式状态：通常来自 Flink State

### 12.2 输出

至少有 3 类：

- `DecisionResult`：主结果流
- `DecisionLogRecord`：决策追溯流
- `EngineErrorRecord`：引擎错误流

### 12.3 推荐 topic

- 输入：`pulsix.event.standard`
- 输入：`scene_release` 的 CDC 广播流
- 输出：`pulsix.decision.result`
- 输出：`pulsix.decision.log`
- 输出：`pulsix.engine.error`

### 12.4 外部存储分工

- MySQL：设计态、发布态、仿真、审计
- Redis：名单、画像、在线 lookup
- Doris：明细查询、Dashboard、错误追踪
- Flink State：流式特征、广播快照、短时运行态

---

## 13. 当前代码做到什么程度

### 13.1 已经做到的

- 有稳定的运行时模型
- 有 Demo 快照和 Demo 事件
- 有本地可执行主链路
- 有 Flink `Broadcast + Event` 骨架
- 有基础单测验证结果
- 已支持 `FIRST_HIT` 主链路
- 已留出 `SCORE_CARD` 入口
- 已有决策结果 / 日志 / 错误流模型

### 13.2 还没完全生产化的部分

- Kafka Source / Sink 真实接入
- Redis Lookup 真实实现
- Flink Keyed State 真实实现
- Checkpoint / Recovery / Exactly-Once
- 指标体系
- 版本回滚 / 延迟生效
- Groovy 沙箱
- 仿真与线上统一进一步收口到 `pulsix-kernel`

当前代码是开工版骨架，不是最终生产版引擎。

---

## 14. 一期推荐开发顺序

### 第 1 步：先打稳 `pulsix-kernel`

重点：

- 冻结 `RiskEvent`、`SceneSnapshot`、`EvalContext`、`DecisionResult`
- 打磨 `RuntimeCompiler`
- 打磨 `DecisionExecutor`
- 固化最小样例与 golden case
- 先用内存状态与内存 lookup 跑稳最小场景

### 第 2 步：补齐 `kernel` 内的仿真 / 轻量回放能力

重点：

- 本地 runner 或命令行工具
- 支持读取快照 JSON 与事件 JSON
- 输出命中规则、原因、特征快照、最终动作
- 增加固定回归样例集

### 第 3 步：打通 `pulsix-engine` 最小主链路

重点：

- 打磨 `DecisionEngineJob`
- 接入最小快照流
- 接入标准事件流
- 跑通一个 `FIRST_HIT` 场景
- 输出 `DecisionResult`、`DecisionLogRecord`、`EngineErrorRecord`

### 第 4 步：增强状态、版本切换与误差收敛

重点：

- 向 Flink Keyed State 过渡
- 增加状态清理、TTL、版本切换校验
- 增强错误分类
- 固化结果一致性检查
- 补充关键集成测试

### 第 5 步：再补真实 Redis、CDC、输出链路

重点：

- 用真实快照流替换 DemoFixtures
- 用 Redis 替换 in-memory lookup
- 接真实 Kafka 输出 / Doris / MySQL 落地

### 第 6 步：最后再补监控、重型回放、回滚

这一步才是平台化收口。

---

## 15. 一期最值钱的测试

第一优先级：

- 表达式执行器测试
- 规则执行器测试
- 策略执行器测试
- Snapshot 编译器测试
- 本地 runner / 仿真样例测试
- 一条 Kafka -> Flink -> 输出 的主链路测试

第二优先级：

- Flink 状态逻辑测试
- Broadcast State 切换测试
- lookup 组件测试
- 命中原因渲染测试

第三优先级：

- 回放测试
- 压测脚本
- 恢复测试
- 长稳测试

测试目标不是接口覆盖率，而是：

- `kernel` 执行正确
- `engine` 运行一致
- 版本切换可解释

---

## 16. 供后续 AI 助手使用的默认假设

如果后续 AI 助手没有收到新的明确指令，默认应遵循：

1. 当前开发中心只放在 `pulsix-kernel + pulsix-engine`
2. 不主动扩展 `pulsix-module-risk` 和 `pulsix-access`
3. 快照优先通过样例 JSON 或 SQL 注入 `scene_release`
4. 事件优先通过 Kafka 工具或本地样例输入
5. 仿真和轻量回放优先实现于 `pulsix-kernel`
6. 任何执行语义都不要分叉到第二套实现
7. 一期策略优先做 `FIRST_HIT`
8. 一期表达式优先做 Aviator/DSL，不重投入 `Groovy`
9. Flink 只认快照，不反查设计态表拼执行逻辑
10. 优先交付可验证、可回归、可解释的小步增量

---

## 17. 快速记忆版

如果只记住 8 句话，就记这些：

- `engine` 消费的是快照，不是设计态表。
- 引擎核心不是规则本身，而是“上下文构建 + 规则执行 + 策略收敛”。
- Broadcast State 放快照，Task 本地缓存放编译结果。
- `kernel` 负责“怎么算”，`engine` 负责“怎么在 Flink 里跑”。
- 仿真和轻量回放当前放在 `kernel` 内部。
- 重型流式回放后续可放在 `engine`，但核心逻辑仍复用 `kernel`。
- 一期先把本地执行内核打稳，再替换成真实 Flink State / Kafka / Redis。
- 当前不是先做完整平台，而是先做出可编译、可执行、可验证、可回归的决策内核。
