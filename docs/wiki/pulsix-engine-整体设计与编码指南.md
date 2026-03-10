## 1. 核心结论

这篇文档只保留 `pulsix-engine` 最关键的信息，帮助你快速抓住引擎设计主线。

只记住这 6 句话即可：

- `pulsix-engine` 不是直接查设计态表执行规则，而是**消费运行时快照执行规则**。
- 引擎主链路是：`事件 + 快照 -> 上下文 -> 特征 -> 规则 -> 策略 -> 结果/日志/错误`。
- 最重要的工程原则是：**compile once, execute many**。
- `Broadcast State` 放原始 `SceneSnapshot`，Task 本地缓存放 `CompiledSceneRuntime`。
- 当前阶段应先把本地执行内核打稳，再补 Flink 状态、Redis、Kafka、CDC。
- 长期建议把纯执行语义沉到 `pulsix-kernel`，`pulsix-engine` 只做 Flink 运行适配。

---

## 2. `pulsix-engine` 到底负责什么

一句话：

> `pulsix-engine` 负责把“事件流”和“快照流”组合起来，在 Flink 中完成实时决策。

主链路如下：

```text
Kafka 标准事件流
    -> 读取 RiskEvent
    -> 连接快照广播流
    -> 找到当前 CompiledSceneRuntime
    -> 构建 EvalContext
    -> 计算 Stream Feature
    -> 计算 Lookup Feature
    -> 计算 Derived Feature
    -> 执行 Rule
    -> 执行 Policy
    -> 输出 DecisionResult
    -> Side Output: DecisionLogRecord / EngineErrorRecord

MySQL scene_release（CDC）
    -> 读取 SceneSnapshotEnvelope
    -> Broadcast State
    -> RuntimeCompiler 编译本地运行时缓存
```

---

## 3. 引擎最重要的 4 条原则

### 3.1 只认运行时快照

- 输入给 Flink 的是 `SceneSnapshot` / `SceneSnapshotEnvelope`
- 不是设计态几十张表
- `scene_release.snapshot_json` 才是引擎真正消费的配置对象

### 3.2 编译和执行必须分开

- 快照变更时编译
- 事件到来时只执行
- 不能在每条事件里临时编译表达式或脚本

### 3.3 上下文准备和规则执行必须分开

正确顺序是：

1. 事件进入
2. 找到当前快照
3. 构建上下文
4. 计算 stream feature
5. 计算 lookup feature
6. 计算 derived feature
7. 执行 rule
8. 执行 policy
9. 输出 result / log / error

### 3.4 发布态和运行态必须分开

- 发布态：`SceneSnapshot`
- 运行态：`CompiledSceneRuntime`

运行态对象里会包含：

- 已编译表达式
- 已编译脚本
- 派生特征执行顺序
- 规则执行顺序

---

## 4. 当前推荐的模块边界

### 4.1 `pulsix-kernel`

建议放纯执行语义：

- `RiskEvent`
- `SceneSnapshot`
- `EvalContext`
- `DecisionResult`
- `RuntimeCompiler`
- `SceneRuntimeManager`
- `DecisionExecutor`
- 规则/策略执行逻辑
- 本地 runner / 仿真 / 回放验证能力

### 4.2 `pulsix-engine`

建议只放 Flink 运行适配：

- Flink Job
- Broadcast State 适配
- Keyed State 适配
- Kafka Source / Sink
- Redis lookup 适配
- side output 输出

一句话：

- `kernel` 负责“怎么算”
- `engine` 负责“怎么在 Flink 里跑”

---

## 5. 当前代码可以怎么理解

当前代码大体可以按 6 层来理解：

### 5.1 模型层

- `SceneSnapshot`
- `RiskEvent`
- `RuleSpec` / `PolicySpec` / `FeatureSpec`
- `DecisionResult` / `DecisionLogRecord` / `EngineErrorRecord`

### 5.2 上下文层

- `EvalContext`

### 5.3 脚本执行层

- `CompiledScript`
- `DefaultScriptCompiler`
- `AviatorCompiledScript`
- `GroovyCompiledScript`

### 5.4 运行时编译层

- `RuntimeCompiler`
- `CompiledSceneRuntime`
- `SceneRuntimeManager`

### 5.5 决策执行层

- `DecisionExecutor`
- `LocalDecisionEngine`
- `InMemoryStreamFeatureStateStore`
- `InMemoryLookupService`

### 5.6 Flink 适配层

- `DecisionBroadcastProcessFunction`
- `DecisionEngineJob`
- `EngineOutputTags`

---

## 6. 最关键的几个类分别干什么

### 6.1 `RuntimeCompiler`

负责把快照编译成运行态对象，重点包括：

- 编译 stream feature 表达式
- 编译 lookup feature 表达式
- 编译 derived feature 表达式
- 编译 rule 表达式
- 派生特征依赖排序
- 规则执行顺序排序

输出产物：`CompiledSceneRuntime`

### 6.2 `SceneRuntimeManager`

负责维护当前生效的本地运行时缓存。

当前最重要的职责：

- 收到快照后编译并激活
- 按 `sceneCode` 查询当前 active runtime

后续还要增强：

- 多版本并存
- 延迟生效
- 回滚
- 编译失败保留旧版本

### 6.3 `DecisionExecutor`

这是当前执行主入口。

它负责：

- 校验事件
- 构建上下文
- 计算 stream / lookup / derived features
- 执行规则
- 执行策略
- 组装结果

### 6.4 `DecisionBroadcastProcessFunction`

这是 Flink 侧核心适配器。

它负责：

- 接快照广播流
- 接标准事件流
- 根据当前快照执行决策
- 将结果、日志、错误分流输出

---

## 7. 引擎的输入和输出

### 7.1 输入

至少有 4 类：

- `RiskEvent`：标准事件主输入
- `SceneSnapshotEnvelope`：快照广播输入
- Lookup 数据：通常来自 Redis
- 流式状态：通常来自 Flink State

### 7.2 输出

至少有 3 类：

- `DecisionResult`：主结果流
- `DecisionLogRecord`：决策追溯流
- `EngineErrorRecord`：引擎错误流

建议的 topic：

- 输入：`pulsix.event.standard`
- 输入：`scene_release` 的 CDC 广播流
- 输出：`pulsix.decision.result`
- 输出：`pulsix.decision.log`
- 输出：`pulsix.engine.error`

---

## 8. 当前代码做到什么程度

### 8.1 已经做到的

- 有稳定的运行时模型
- 有 Demo 快照和 Demo 事件
- 有本地可执行主链路
- 有 Flink `Broadcast + Event` 骨架
- 有基础单测验证结果
- 已支持 `FIRST_HIT` 主链路
- 已留出 `SCORE_CARD` 入口

### 8.2 还没完全生产化的部分

- Kafka Source / Sink 真实接入
- Redis Lookup 真实实现
- Flink Keyed State 真实实现
- Checkpoint / Recovery / Exactly-Once
- 指标体系
- 版本回滚 / 延迟生效
- Groovy 沙箱
- 仿真与线上统一下沉到 `pulsix-kernel`

一句话：

> 当前代码是“开工版骨架”，不是最终生产版引擎。

---

## 9. 一期该优先做什么

### 9.1 先做本地执行内核

继续打磨：

- `RuntimeCompiler`
- `DecisionExecutor`
- `LookupService`
- `StreamFeatureStateStore`

目标：

- 给定快照
- 给定一组事件
- 本地一定能稳定跑出结果

### 9.2 再把 in-memory 状态换成 Flink State

重点：

- bucket 结构
- TTL
- timer 清理
- state schema 迁移

### 9.3 再把 Demo 快照流换成真实快照流

- 从 `DemoFixtures` 过渡到 `scene_release` CDC

### 9.4 再把 in-memory lookup 换成 Redis

重点：

- timeout
- cache
- 降级
- metrics

### 9.5 再接真实输出链路

- 主流：`pulsix.decision.result`
- 侧输出：`pulsix.decision.log`、`pulsix.engine.error`

### 9.6 最后再补监控、回放、回滚

这一步才是平台化收口。

---

## 10. 一期应怎么控制复杂度

### 10.1 策略先以 `FIRST_HIT` 为主

原因：

- 最常见
- 最稳定
- 上线路径最短

### 10.2 表达式先以 Aviator 为主

`Groovy` 可以保留抽象，但一期不应重投入。

后续才补：

- 沙箱
- 类加载隔离
- import 限制
- 反射禁用
- 资源访问禁用

### 10.3 错误必须标准化输出

至少要区分：

- 未找到快照
- 表达式执行失败
- 脚本执行失败
- lookup 失败

---

## 11. 只记住这些就够了

如果你只想快速抓重点，记住下面这些即可：

- `engine` 消费的是快照，不是设计态表。
- 引擎核心不是规则本身，而是“上下文构建 + 规则执行 + 策略收敛”。
- Broadcast State 放快照，Task 本地缓存放编译结果。
- 流式特征、lookup 特征、派生特征、规则、策略必须严格分层。
- 先把本地执行内核打稳，再替换成真实 Flink State / Kafka / Redis。
- 不要把状态、规则、脚本、策略、接入全部揉进一个 `processElement`。

一句话总结：

> `pulsix-engine` 的正确方向不是“把所有逻辑堆进 Flink”，而是“围绕运行时快照构建可编译、可执行、可替换的决策内核，再由 Flink 负责运行时承载”。
