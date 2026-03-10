## 1. 这份文档解决什么问题

这份文档的目标不是只告诉你 `pulsix-engine` 里“有哪些类”，而是让你真正搞清楚下面几件事：

- `pulsix-engine` 在整套 `pulsix` 风控平台里处在什么位置。
- 一条标准事件进入引擎以后，完整执行链路是什么。
- 运行时快照、流式特征、lookup 特征、派生特征、规则、策略，分别该由谁负责。
- 代码应该先写哪些，再写哪些，哪些地方可以先做骨架，哪些地方必须一次设计对。
- 数据输入有哪些、结构是什么、各自起什么作用。
- 数据输出有哪些、结构是什么、应该发往哪里。

你可以把它理解成：这是 `pulsix-engine` 的“第一版落地设计说明 + 编码指南”。

---

## 2. 本次已经生成的初始代码

这次我已经在 `pulsix-engine` 中放入了一套“可编译、可跑本地单测、可继续演进”的初始骨架，核心思想是：

- 先把**运行时模型**稳定下来。
- 再把**编译一次、多次执行**的内核搭起来。
- 再补一层 **Flink 适配器**，对接广播流和事件流。
- 再用 **Demo 数据 + 单测**证明主链路是通的。

当前代码大致分成 6 层：

### 2.1 模型层

- `SceneSnapshot`：运行时快照。
- `RiskEvent`：标准事件。
- `RuleSpec` / `PolicySpec` / `FeatureSpec`：规则、策略、特征定义。
- `DecisionResult` / `DecisionLogRecord` / `EngineErrorRecord`：三类输出模型。

### 2.2 上下文层

- `EvalContext`：规则执行时看到的统一变量世界。

### 2.3 脚本执行层

- `CompiledScript`
- `DefaultScriptCompiler`
- `AviatorCompiledScript`
- `GroovyCompiledScript`

这一层负责把“发布态字符串表达式”编译成“运行时可反复调用对象”。

### 2.4 运行时编译层

- `RuntimeCompiler`
- `CompiledSceneRuntime`
- `SceneRuntimeManager`

这一层负责把 `SceneSnapshot` 转成引擎真正消费的 `CompiledSceneRuntime`。

### 2.5 决策执行层

- `DecisionExecutor`
- `LocalDecisionEngine`
- `InMemoryStreamFeatureStateStore`
- `InMemoryLookupService`

这一层负责把一条事件从输入处理到输出结果。

### 2.6 Flink 适配层

- `DecisionBroadcastProcessFunction`
- `DecisionEngineJob`
- `EngineOutputTags`

这一层负责把“本地可执行内核”挂到 Flink 的 `KeyedBroadcastProcessFunction` 上。

---

## 3. 为什么这版代码要这样拆

核心原因只有一句话：

> 实时风控引擎不是“写几个 if/else”，而是“围绕运行时快照，不断构建上下文并执行决策”。

所以模块边界必须清楚：

### 3.1 发布态对象和运行态对象必须分开

- 发布态：`SceneSnapshot`
- 运行态：`CompiledSceneRuntime`

发布态可以进 `Broadcast State`，可以持久化为 JSON。

运行态包含：

- 已编译的 Aviator 表达式
- 已编译的 Groovy 脚本
- 已排序的派生特征执行顺序
- 已排序的规则执行顺序

这类对象不适合直接放进 `Broadcast State`，适合放在 Task 本地 transient cache 中。

### 3.2 上下文准备和规则执行必须分开

规则引擎只做一件事：

- 在一个已经准备好的 `EvalContext` 上判断规则是否命中。

它不应该负责：

- 去查 Redis
- 去做流式聚合
- 去做字段标准化
- 去补默认值

正确顺序应该是：

1. 事件进入
2. 找到场景当前快照
3. 构建上下文
4. 计算 stream feature
5. 计算 lookup feature
6. 计算 derived feature
7. 执行 rule
8. 执行 policy
9. 输出 result / log / error

### 3.3 编译和执行必须分开

这是 `pulsix-engine` 最关键的工程原则：

- 快照变更时编译
- 事件到来时只执行

也就是经典的：

> compile once, execute many

如果把表达式/Groovy 放到每条事件里临时编译，性能和稳定性都会出问题。

---

## 4. `pulsix-engine` 的整体链路是什么

可以先把主链路抽象成这张图：

```text
Kafka 标准事件流
    -> Flink 读取 RiskEvent
    -> keyBy(sceneCode)
    -> connect(配置广播流)
    -> 找到当前 CompiledSceneRuntime
    -> 构建 EvalContext
    -> 执行 Stream Feature
    -> 执行 Lookup Feature
    -> 执行 Derived Feature
    -> 执行 Rule
    -> 执行 Policy
    -> 输出 DecisionResult
    -> Side Output: DecisionLogRecord / EngineErrorRecord

MySQL scene_release（Flink CDC）
    -> Flink 读取 SceneSnapshotEnvelope
    -> Broadcast State
    -> RuntimeCompiler 编译本地运行时缓存
```

一句话总结：

> 事件流提供待决策数据，配置广播流提供当前执行逻辑，`DecisionBroadcastProcessFunction` 负责把二者组合起来。

---

## 5. 当前代码中的关键职责划分

### 5.1 `RuntimeCompiler`

负责把快照编译成运行态对象，主要做 4 件事：

- 编译 stream feature 的 `entityKeyExpr / valueExpr / filterExpr`
- 编译 lookup feature 的 `keyExpr`
- 编译 derived feature 的 `expr`
- 编译 rule 的 `whenExpr`

除此之外还负责：

- 对派生特征做依赖排序
- 对规则按 `policy.ruleOrder` 或 `priority` 排序

这一步的产物就是 `CompiledSceneRuntime`。

### 5.2 `SceneRuntimeManager`

负责维护“当前生效的本地运行时缓存”。

当前初始版做的是：

- 收到 `SceneSnapshotEnvelope` 后编译并激活
- 按 `sceneCode` 查询当前活跃 runtime

真实生产版后续要继续增强：

- 支持多版本并存
- 支持 `effectiveFrom` 延迟生效
- 支持回滚
- 支持编译失败保留旧版本

### 5.3 `DecisionExecutor`

这是当前代码最核心的执行入口。

它内部实现顺序就是：

1. 校验事件是否符合 `eventSchema`
2. 构建 `EvalContext`
3. 计算 stream features
4. 计算 lookup features
5. 计算 derived features
6. 执行 ordered rules
7. 执行 policy 收敛
8. 组装 `DecisionResult`

### 5.4 `DecisionBroadcastProcessFunction`

这是 Flink 适配层的核心：

- `processBroadcastElement`：接收快照广播并更新 runtime
- `processElement`：接收事件并执行决策

其中还负责两件关键事：

- 如果本地 runtime cache 丢失，就从 `Broadcast State` 重新恢复并编译
- 把正常结果、决策日志、错误记录分流到主输出和 side output

---

## 6. 数据输入有哪些

`pulsix-engine` 的输入，不只是“事件”。至少有 4 类：

### 6.1 输入一：标准事件 `RiskEvent`

这是主输入，也是热路径输入。

**作用**

- 触发一次实时决策
- 提供基础字段给特征、规则、策略使用

**结构**

```json
{
  "eventId": "E202603070006",
  "traceId": "T202603070006",
  "sceneCode": "TRADE_RISK",
  "eventType": "trade",
  "eventTime": "2026-03-07T10:00:00Z",
  "userId": "U1001",
  "deviceId": "D9001",
  "ip": "1.2.3.4",
  "amount": 6800,
  "currency": "CNY",
  "result": "SUCCESS",
  "channel": "APP"
}
```

**字段作用**

- `eventId`：事件唯一标识
- `traceId`：链路追踪标识
- `sceneCode`：决定走哪个场景配置
- `eventType`：决定是否匹配当前场景事件模型
- `eventTime`：流式特征计算窗口时间
- `userId / deviceId / ip`：实体维度主键
- `amount / result / channel`：规则和派生特征常用字段

### 6.2 输入二：快照发布流 `SceneSnapshotEnvelope`

这是配置输入，不是业务输入。

**作用**

- 告诉 Flink 当前场景应该按什么逻辑执行
- 驱动运行时热更新

**结构**

```json
{
  "sceneCode": "TRADE_RISK",
  "version": 12,
  "checksum": "8d2041a7cf8f47b4b6b0f91d2ab8d9d0",
  "publishType": "PUBLISH",
  "publishedAt": "2026-03-07T20:00:00Z",
  "effectiveFrom": "2026-03-07T20:00:10Z",
  "snapshot": {
    "snapshotId": "TRADE_RISK_v12",
    "sceneCode": "TRADE_RISK",
    "streamFeatures": ["..."],
    "lookupFeatures": ["..."],
    "derivedFeatures": ["..."],
    "rules": ["..."],
    "policy": {"..."}
  }
}
```

**字段作用**

- `sceneCode`：场景主键
- `version`：版本号
- `checksum`：幂等与一致性校验
- `publishType`：发布 / 回滚
- `effectiveFrom`：理论生效时间
- `snapshot`：真正的运行时执行计划

### 6.3 输入三：Lookup 数据

这是外部上下文输入，真实生产版通常来自 `Redis`。

当前初始代码里，我用 `InMemoryLookupService` 做了替身，后续要切到 Redis。

**作用**

- 名单判断
- 用户画像
- 热点在线查询变量

**典型结构**

#### Redis Set：设备黑名单

```text
key = pulsix:list:black:device
members = [D0009, D0010, D0011]
```

#### Redis Hash：用户风险等级

```text
key = pulsix:profile:user:risk
field/value = {
  U1001: H,
  U2002: L,
  U4004: M
}
```

**当前代码里的 lookup spec 示例**

```json
{
  "code": "device_in_blacklist",
  "lookupType": "REDIS_SET",
  "keyExpr": "deviceId",
  "sourceRef": "pulsix:list:black:device",
  "defaultValue": false
}
```

### 6.4 输入四：流式状态

这不是外部直接送进来的 JSON，但它是引擎决策必不可少的第四类输入。

**作用**

- 支撑近 5 分钟次数
- 支撑近 30 分钟金额和
- 支撑近 1 小时设备关联用户数

当前初始版代码里，状态由 `InMemoryStreamFeatureStateStore` 承担。

生产版应该替换成：

- `Keyed State`
- `MapState / ListState / ValueState`
- 必要时配合 RocksDB State Backend

---

## 7. 数据输出有哪些

至少有 3 类输出：

### 7.1 输出一：决策结果 `DecisionResult`

这是给业务系统消费的主结果流。

**作用**

- 通知业务系统这条事件的最终动作
- 提供规则命中明细和特征快照

**结构示例**

```json
{
  "eventId": "E202603070006",
  "traceId": "T202603070006",
  "sceneCode": "TRADE_RISK",
  "version": 12,
  "decisionMode": "FIRST_HIT",
  "finalAction": "REJECT",
  "finalScore": 80,
  "latencyMs": 3,
  "ruleHits": [
    {
      "ruleCode": "R001",
      "hit": false,
      "action": "REJECT",
      "score": 100
    },
    {
      "ruleCode": "R003",
      "hit": true,
      "action": "REJECT",
      "score": 80,
      "reason": "设备1小时关联用户数=4, 用户风险等级=H"
    },
    {
      "ruleCode": "R002",
      "hit": true,
      "action": "REVIEW",
      "score": 60,
      "reason": "用户5分钟交易次数=3, 当前金额=6800"
    }
  ],
  "featureSnapshot": {
    "user_trade_cnt_5m": 3,
    "user_trade_amt_sum_30m": 7180,
    "device_bind_user_cnt_1h": 4,
    "device_in_blacklist": false,
    "user_risk_level": "H",
    "high_amt_flag": true,
    "trade_burst_flag": true
  }
}
```

### 7.2 输出二：决策日志 `DecisionLogRecord`

这是给日志查询、仿真对比、页面追溯使用的详细输出。

**作用**

- 支撑 traceId / eventId 查询
- 支撑“为什么命中”解释
- 支撑回放和差异分析

它和 `DecisionResult` 很像，但更偏“追溯和日志查询”而不是“业务联动消费”。

### 7.3 输出三：引擎异常 `EngineErrorRecord`

这是错误流，不进入正常结果主链路。

**作用**

- 记录 runtime 缺失
- 记录表达式执行异常
- 记录 Groovy 脚本错误
- 记录配置版本切换问题

**结构示例**

```json
{
  "stage": "decision-execute",
  "sceneCode": "TRADE_RISK",
  "version": 12,
  "eventId": "E202603070006",
  "traceId": "T202603070006",
  "errorMessage": "rule expression execute failed",
  "occurredAt": "2026-03-07T10:00:00Z"
}
```

---

## 8. 各个技术功能的实现要点

### 8.1 运行时快照 + 热更新

要点是：

- MySQL 的 `scene_release.snapshot_json` 才是 Flink 要消费的对象
- Flink 不应直接查询设计态几十张表
- `Broadcast State` 中保存的是原始 `SceneSnapshot`
- Task 本地缓存中保存的是 `CompiledSceneRuntime`

生产版还要补：

- compile-before-activate
- 编译失败不切版本
- 历史版本缓存
- 回滚版本快速恢复

### 8.2 Stream Feature

当前初始代码实现的是“概念正确、易读易改”的第一版：

- 用 `InMemoryStreamFeatureStateStore` 存窗口内观测值
- 支持 `COUNT / SUM / MAX / LATEST / DISTINCT_COUNT`
- 按 `sceneCode + featureCode + entityKey` 组织状态

这版代码适合你理解模型，但不等于最终生产版。

生产版建议升级为：

- `Keyed State`
- 按 bucket 存储
- timer + TTL 清理
- 对 `DISTINCT_COUNT` 做更稳妥的集合状态或近似计数方案

### 8.3 Lookup Feature

当前初始版是 `LookupService` 抽象 + `InMemoryLookupService` 演示实现。

真实版要补的关键点：

- Redis 超时控制
- 本地短 TTL cache
- 默认值兜底
- 降级策略
- 指标统计（RT、错误率、超时率）

### 8.4 Derived Feature

派生特征本质是：

- 在当前上下文里生成一个新变量

要点：

- 大部分用 Aviator
- 少量复杂逻辑用 Groovy
- 必须先做依赖排序，再执行

所以当前代码在 `RuntimeCompiler` 中做了派生特征拓扑排序。

### 8.5 Rule

规则做的事情很单纯：

- 读取上下文
- 判断是否命中
- 生成命中原因
- 记录命中建议动作和分数

当前代码里，规则执行结果被封装为 `RuleHit`。

这样做的好处是：

- 规则和策略解耦
- 结果可追溯
- 容易支持 `FIRST_HIT` 和 `SCORE_CARD`

### 8.6 Policy

当前代码已经支持两种策略模式入口：

- `FIRST_HIT`
- `SCORE_CARD`

这次主链路实际验证的是 `FIRST_HIT`。

为什么一期优先做 `FIRST_HIT`：

- 业务最常见
- 结构最稳定
- 上线路径最短

### 8.7 表达式与 Groovy

当前代码采用统一抽象：

- `CompiledScript`
- `DefaultScriptCompiler`

要点是：

- 上层不用关心底层是 Aviator 还是 Groovy
- 快照加载时编译
- 事件处理时复用

生产版 Groovy 还要继续补：

- 沙箱
- 类加载隔离
- import 限制
- 反射禁用
- 资源访问禁用

### 8.8 错误流和追溯

错误不能直接吞掉。

至少要分清：

- 运行时没找到快照
- 表达式执行失败
- Groovy 执行失败
- 外部 lookup 失败

当前初始代码已经用 `EngineErrorRecord` 把错误标准化了。

---

## 9. 现在这份初始代码“做到什么程度”了

这版代码已经做到：

- 有稳定的运行时数据模型
- 有 Demo 快照和 Demo 事件
- 有本地可执行的决策主链路
- 有 Flink `Broadcast + Event` 骨架
- 有单测验证规则与策略结果

但它还**没有完全生产化**，这很正常。当前仍然是“开工版骨架”，不是最终版引擎。

### 9.1 已完成的部分

- 快照建模
- 表达式/Groovy 编译抽象
- 派生特征依赖排序
- FIRST_HIT 主链路
- 基础 SCORE_CARD 入口
- 决策结果 / 决策日志 / 错误流模型

### 9.2 还要继续补的生产能力

- Kafka Source / Sink 真正接入
- Redis Lookup 真实现
- Flink Keyed State 真实现
- Checkpoint / Recovery / Exactly-Once
- 指标体系
- 配置延迟生效 / 回滚
- Groovy 沙箱
- 规则仿真与线上统一下沉到 `pulsix-kernel`

---

## 10. 你接下来该如何继续写 `pulsix-engine`

我建议按下面顺序推进，而不是一上来全铺开：

### 第一步：先把本地执行内核写稳

继续完善：

- `RuntimeCompiler`
- `DecisionExecutor`
- `LookupService`
- `StreamFeatureStateStore`

目标是：

- 给定一个快照
- 给定一组事件
- 本地一定能稳定跑出结果

### 第二步：把 in-memory 状态替换成 Flink Keyed State

重点做：

- bucket 结构
- TTL
- timer 清理
- state schema 版本迁移

### 第三步：把 Demo 配置流替换成真实快照流

从：

- `env.fromElements(DemoFixtures.demoEnvelope())`

替换成：

- MySQL CDC -> Flink

### 第四步：把 in-memory lookup 替换成 Redis

重点补：

- timeout
- 降级
- cache
- metrics

### 第五步：把 side output 接到 Kafka / Doris / MySQL

主流输出：

- `pulsix.decision.result`

侧输出：

- `pulsix.decision.log`
- `pulsix.engine.error`

### 第六步：再补监控、回放、回滚

这一步才是平台化收口。

---

## 11. 推荐的 Topic / 存储落地关系

建议你后续把 `pulsix-engine` 的输入输出按下面方式落地：

### 输入

- `pulsix.event.standard`：标准事件主流
- `MySQL CDC`（读取 `scene_release`）：快照广播流来源

### 输出

- `pulsix.decision.result`：业务主结果流
- `pulsix.decision.log`：决策追溯流
- `pulsix.engine.error`：引擎异常流

### 外部存储

- MySQL：设计态、发布态、仿真、审计
- Redis：名单、画像、在线 lookup
- Doris：明细查询、Dashboard、错误追踪
- Flink State：流式特征、广播快照、短时运行态

---

## 12. 这版代码最重要的认知结论

如果你只记住 5 句话，就记下面这些：

1. `pulsix-engine` 不是直接查数据库执行规则，而是消费快照执行规则。
2. 引擎核心不是“规则”，而是“上下文构建 + 规则执行 + 策略收敛”。
3. Broadcast State 放的是快照，Task 本地缓存放的是编译结果。
4. 流式特征、lookup 特征、派生特征、规则、策略必须严格分层。
5. 先把本地执行内核打磨稳定，再替换成真实 Flink State / Kafka / Redis。

如果按这个思路继续写，`pulsix-engine` 的代码会越来越稳；如果一开始就把状态、规则、脚本、策略、接入全部揉在一个 `processElement` 里，后面基本一定会失控。
