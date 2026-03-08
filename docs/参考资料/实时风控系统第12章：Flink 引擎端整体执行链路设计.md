## 12.1 这一章解决什么问题

前面几章我们已经把几个关键前提铺好了：

- 第 8 章讲清楚了：为什么控制平台必须发布运行时快照
- 第 9 章讲清楚了：运行时快照应该长什么样
- 第 10 章讲清楚了：表达式 / Groovy 为什么要在发布前做校验与依赖分析
- 第 11 章讲清楚了：Flink 在这套系统里的角色，不是简单窗口工具，而是状态型实时决策执行引擎

到了这一章，问题终于进入到你现在最关心、也是整套系统真正落地的核心：

> Flink 到底是怎么把控制平台发布出来的快照“消费掉”，再对真实业务事件执行出决策结果的？

也就是说，这一章不再停留在“概念层”，而是开始回答运行时的真实过程：

- Flink Job 的整体拓扑应该长什么样
- 事件流和配置广播流怎么 connect
- 一条事件进入 Flink 以后会经历哪些步骤
- 当前场景快照如何定位
- EvalContext 是怎么构建的
- stream feature、lookup feature、derived feature 在执行链路里分别怎么参与
- 规则执行和策略收敛具体在什么位置发生
- 决策日志和指标该怎么输出
- Flink 端哪些模块应该拆开，哪些不要揉在一起

这一章你可以把它理解成：

> **Flink 引擎运行时的一张总时序图 + 总执行蓝图**

后面第 13 章会讲快照如何接收与切换，
第 14\~18 章会把特征、状态、规则、表达式、Groovy 再分别拆细。

但这一章最重要，因为它回答的是：

> **整条实时决策链在 Flink 里到底是如何串起来的。**

---

## 12.2 先给出一个总答案：Flink 引擎执行链路的本质是什么

如果把这一章先压缩成一句话，那么答案是：

> **Flink 引擎的本质，是在事件流上不断构建“运行时决策上下文”，并按照当前场景快照驱动特征计算、规则执行和策略收敛。**

这句话里有三个关键词：

### 1）事件流

也就是原始业务事件：

- login
- register
- trade
- withdraw

### 2）运行时决策上下文

也就是某条事件在当前时刻真正被拿来做判断时，需要具备的一整套变量集合，包括：

- 原始字段
- 流式特征
- lookup 特征
- 派生特征
- 命中名单结果
- 元信息（版本、traceId、sceneCode）

### 3）当前场景快照

也就是：

- 当前有哪些 feature 要算
- 哪些 lookup 要查
- 哪些 derived feature 要执行
- 哪些 rule 要执行
- policy 采用什么模式

所以你会发现，Flink 引擎不是在“直接执行几条规则”，而是在做这件事：

> **基于快照，把一条原始事件逐步加工成一个完整上下文，然后在这个上下文上执行决策。**

这就是整条链路的本质。

---

## 12.3 Flink Job 的整体拓扑应该怎么理解

先不要一上来钻进 `processElement`。 你先要在脑子里建立起 Flink Job 的总拓扑。

我建议你把实时决策 Flink Job 先抽象成下面这几个阶段：

```latex
Kafka Event Stream
      │
      ▼
事件反序列化 / 标准化 / 预处理
      │
      ▼
Connect 配置广播流
      │
      ▼
场景快照定位 + 运行时上下文初始化
      │
      ▼
流式特征执行 / 外部 lookup / 派生特征执行
      │
      ▼
规则执行
      │
      ▼
策略收敛
      │
      ▼
决策结果输出 + 日志输出 + 指标输出
```

同时还有另一条配置流：

```latex
MySQL CDC / 可选 Kafka Config Topic
      │
      ▼
快照反序列化
      │
      ▼
Broadcast Stream
      │
      ▼
Broadcast State + 本地运行时编译缓存
```

两条流在 Flink 里通过：

- `BroadcastProcessFunction`
- 或 `KeyedBroadcastProcessFunction`

进行结合。

所以从拓扑上看，Flink Job 不是单输入，而是一个**事件主流 + 配置广播流**的双流拓扑。

---

## 12.4 为什么配置流必须是广播流

这一点一定要从运行语义上理解。

你发布了一个新的 `TRADE_RISK v12` 快照后，
希望的结果是什么？

答案一定是：

> **所有并行子任务，都拿到同一份场景运行时配置，并在之后按同一逻辑执行。**

这就决定了配置流必须满足：

- 每个并行实例都能收到
- 每个并行实例都能持有
- 配置切换时能同时更新本地执行逻辑

这正是 Broadcast State 的适用场景。

所以配置流采用广播，不是因为“Flink 提供了这个 API 所以拿来用”，而是因为：

> **配置本质上是全局控制信息，而不是按 key 分区的数据。**

---

## 12.5 事件流和配置流在 Flink 中怎么 connect

通常推荐的处理方式是：

### 事件主流

- 从 Kafka `pulsix.event.raw` 读取
- 解析成 `RiskEvent`
- 按需要做 `keyBy`

### 配置流

- 从 MySQL CDC 的发布表读取，或按需从 Kafka `pulsix.config.snapshot` 读取
- 解析成 `SceneSnapshotEnvelope`
- `broadcast()` 成广播流

### 然后 connect

如果你的事件流后面需要按实体 key 做状态计算，比较常见的是：

```java
KeyedStream<RiskEvent, String> keyedEventStream = eventStream
    .keyBy(event -> routeKey(event));

BroadcastStream<SceneSnapshotEnvelope> configBroadcastStream = configStream
    .broadcast(SNAPSHOT_STATE_DESC);

DataStream<DecisionResult> resultStream = keyedEventStream
    .connect(configBroadcastStream)
    .process(new DecisionEngineProcessFunction(...));
```

这里的关键点是：

- 事件流负责提供“每条待处理事件”
- 配置流负责提供“当前执行逻辑”
- connect 后的 process function，负责把二者组合起来

你可以把这个算子理解成整个引擎的“总调度器”。

---

## 12.6 Flink 引擎端推荐的模块划分

如果你把所有逻辑都写在一个 `processElement` 里，代码会非常快地失控。 所以我建议你从一开始就按职责拆分引擎内部模块。

一个比较合理的 Flink 引擎内部模块划分可以是这样：

### 1）Event Parser / Standardizer

负责：

- 事件反序列化
- 基础字段标准化
- sceneCode / eventType 校验
- 字段映射与别名归一
- 时间 / 金额 / 枚举格式转换
- 公共字段补齐与默认值填充
- 不合法事件打入错误流 / DLQ

### 2）Snapshot Runtime Manager

负责：

- 处理广播快照
- 维护 Broadcast State
- 维护本地编译缓存
- 提供当前场景运行时对象查询

### 3）EvalContext Builder

负责：

- 把原始事件、特征值、lookup 值、派生值组装成上下文

### 4）Stream Feature Executor

负责：

- 按快照执行 stream feature
- 维护 keyed state / timer
- 返回当前事件所需特征值

### 5）Lookup Executor

负责：

- 查 Redis 名单 / 画像 / 热点 KV
- 提供默认值 / 超时兜底

### 6）Derived Feature Executor

负责：

- 执行表达式 / Groovy
- 生成派生特征

### 7）Rule Executor

负责：

- 顺序执行规则
- 收集命中结果
- 生成 hit reason

### 8）Policy Executor

负责：

- FIRST\_HIT / SCORE\_CARD 收敛
- 输出最终 action / score / reasons

### 9）Decision Sink / Log Sink / Metrics Reporter

负责：

- 输出结果
- 输出日志
- 打业务指标和异常指标

你会发现，这样拆完以后， `DecisionEngineProcessFunction` 自己只负责总流程协调，而不是把所有细节都塞进去。

这对你后面做：

- 单元测试
- 仿真复用
- 规则引擎复用
- 代码维护

都非常有帮助。

---

## 12.7 一条事件进入 Flink 以后，整体执行步骤是什么

下面给你一版推荐的主执行步骤。 这基本就是你后面实现 `processElement` 时要遵循的主流程。

```latex
1. 读取原始事件
2. 找到该 scene 当前生效快照
3. 初始化 EvalContext
4. 计算流式特征
5. 查询 lookup 特征（名单/画像/Redis）
6. 计算派生特征
7. 执行规则
8. 执行策略收敛
9. 生成 DecisionResult
10. 输出决策结果 / 日志 / 指标
```

这个顺序不是随便排的，而是有依赖关系的。

### 为什么先特征，再规则？

因为规则依赖特征值。

### 为什么 lookup 在规则前？

因为名单 / 画像也是规则变量的一部分。

### 为什么 derived feature 在规则前？

因为派生特征也是规则的输入。

### 为什么 policy 在规则后？

因为 policy 的职责是对多个 rule 的结果做收敛，而不是自己做底层判断。

所以你可以记住：

> **Flink 决策引擎的核心执行顺序是：上下文构建优先，规则执行其次，策略收敛最后。**

---

## 12.8 当前场景快照如何定位

这是运行时的第一个关键动作。

一条事件进入后，Flink 要先回答：

> **这条事件现在到底应该按哪份逻辑执行？**

通常定位方式是：

- 事件中带 `sceneCode`
- 用 `sceneCode` 去 Broadcast State 里找当前快照
- 再从本地编译缓存里找对应的 `CompiledSceneRuntime`

大致逻辑如下：

```java
String sceneCode = event.getSceneCode();
SceneSnapshot snapshot = broadcastState.get(sceneCode);
CompiledSceneRuntime runtime = runtimeManager.getCompiledRuntime(sceneCode, snapshot.getVersion());
```

这里有几个重要点。

### 1）sceneCode 是运行时入口主键

所以它必须在事件模型里稳定存在。

### 2）Broadcast State 持有的是“可恢复快照”

而不是全部运行时对象。

### 3）真正执行时最好用本地编译缓存

这样避免每条事件都去解析 JSON、编译表达式、加载脚本。

也就是说：

- **Broadcast State 用于恢复和一致性**
- **本地 compiled runtime 用于高效执行**

这两个层次必须分开。

---

## 12.9 EvalContext 到底是什么，为什么它是执行链路的核心

你后面会频繁看到 `EvalContext` 这个概念。 它可以理解为：

> **某条事件在某个版本快照下，被拿来做决策时的完整变量上下文。**

它通常会包含下面几类数据。

### 1）基础字段

来自事件本身：

- eventId
- userId
- deviceId
- ip
- amount
- result
- eventTime

### 2）流式特征

来自 Flink state 计算：

- user\_trade\_cnt\_5m
- user\_trade\_amt\_sum\_30m
- device\_bind\_user\_cnt\_1h

### 3）lookup 特征

来自 Redis / 外部 KV：

- device\_in\_blacklist
- ip\_risk\_level
- user\_risk\_level

### 4）派生特征

来自表达式 / Groovy：

- high\_amt\_flag
- trade\_burst\_flag

### 5）元信息

例如：

- sceneCode
- snapshotVersion
- traceId
- eventTime
- processingStartNs

你可以把它简单理解成一个大 Map，但在工程实现里，建议做成更清晰的对象，例如：

```java
class EvalContext {
    RiskEvent event;
    Map<String, Object> baseFields;
    Map<String, Object> streamFeatures;
    Map<String, Object> lookupFeatures;
    Map<String, Object> derivedFeatures;
    RuntimeMeta runtimeMeta;
}
```

为什么它重要？

因为无论：

- 表达式执行
- Groovy 执行
- 规则判断
- hit reason 渲染
- 决策日志输出

最终都依赖这个上下文对象。

所以：

> **EvalContext 是 Flink 决策链路里最核心的“统一数据平面”。**

---

## 12.10 流式特征在引擎中的执行位置和角色

这部分你最关心，我这里先从整体链路角度讲。

stream feature 的作用，不是“给平台凑个功能”，而是：

> **把原始事件扩展成带有实时历史感知能力的上下文。**

例如：

- 一条当前交易事件本身只知道 `amount=6800`
- 但加上 stream feature 以后，你才知道：
  - `user_trade_cnt_5m=3`
  - `user_trade_amt_sum_30m=18800`
  - `device_bind_user_cnt_1h=4`

这些值正是实时风控判断的核心依据。

在执行链路里，stream feature 一般位于：

- 快照定位之后
- 规则执行之前
- 作为上下文构建的一部分

也就是说，它不是和规则并列的，而是规则的前置输入层。

### 推荐理解方式

规则擅长表达“判断”，
而 stream feature 负责提供“判断所需的实时变量”。

这也是为什么：

- stream feature 不应完全交给任意 Groovy
- 它更适合由 Flink 模板化执行器负责

后面第 14、15 章我们会讲得更细，这里先建立链路位置感。

---

## 12.11 名单、画像、lookup 特征在链路里的位置

很多风控逻辑不仅依赖 state 计算，还依赖在线 lookup。

例如：

- 设备是否在黑名单
- 当前 IP 是否高风险代理
- 用户风险等级是否为 H
- 设备标签是否异常

这些值一般不适合放在 Flink State 主计算链里长期维护，
更适合由 Redis / KV 提供低延迟查询。

所以在总执行链路中，它们通常位于：

- stream feature 之后（或并行）
- derived feature 和 rule 之前

一个简单的执行顺序可以是：

```latex
基础字段 -> stream feature -> lookup feature -> derived feature -> rules -> policy
```

当然从性能角度，一些 stream feature 和 lookup 也可以并行获取，
但从逻辑依赖上，你可以先按这个顺序理解。

### 为什么名单不是 rule？

因为名单本身只是一个“数据源”，
真正的规则是：

- `device_in_blacklist == true -> REJECT`

所以名单在运行时应被转化为 lookup feature，
再参与规则执行。

---

## 12.12 派生特征在执行链路中的位置

派生特征你可以理解成：

> **对已有上下文再进行一层业务语义提炼。**

例如：

- `high_amt_flag = amount >= 5000`
- `trade_burst_flag = user_trade_cnt_5m >= 3 && amount >= 5000`

这类逻辑的特点是：

- 不直接维护 Flink state
- 依赖已有变量
- 通常比较适合表达式或 Groovy
- 计算成本低于复杂流式特征

所以它在执行链路里的典型位置是：

```latex
基础字段 + stream feature + lookup feature
                ↓
         derived feature 执行
                ↓
             rule 执行
```

### 为什么要有派生特征这一层？

因为它能把复杂规则拆解成更清晰的中间语义。

例如不要一条规则全写成：

```latex
amount >= 5000 && user_trade_cnt_5m >= 3 && device_in_blacklist == false && user_risk_level == 'H'
```

而是可以先派生：

- `high_amt_flag`
- `trade_burst_flag`
- `high_risk_user_flag`

然后规则只需要组合这些语义变量。

这会显著提升：

- 规则可读性
- 平台复用性
- 仿真解释能力

---

## 12.13 规则在 Flink 里的执行模型是什么

现在上下文准备好了，就进入规则执行阶段。

规则执行本质上就是：

> **在 EvalContext 上顺序执行若干布尔判断逻辑，并收集命中结果。**

每条规则通常需要输出：

- 是否命中
- 风险分 / score
- hitAction
- hitReason
- ruleCode

在运行时，不建议让规则层自己去再拉外部数据，也不建议再动态改上下文结构。 规则层应该尽量做到：

- 输入：完整上下文
- 处理：只做判断
- 输出：命中结果

也就是说，规则执行器更像一个“纯函数阶段”。

这非常重要，因为这样：

- 执行更稳定
- 更容易回放
- 更容易仿真
- 更容易解释

---

## 12.14 策略在 Flink 里的执行模型是什么

规则执行完以后，并不一定直接得到最终结果。 你还需要 policy 做最后的结果收敛。

### 常见两种模式

#### 1）FIRST\_HIT

- 按 priority / order 顺序执行规则
- 第一条命中后直接返回
- 适合很多简单风控场景

#### 2）SCORE\_CARD

- 所有规则都执行
- 汇总命中分数
- 再根据分数区间决定 PASS / REVIEW / REJECT

所以在链路位置上：

```latex
rules -> hit results -> policy executor -> final decision
```

这里一定要坚持一个设计边界：

> **Rule 负责单点判断，Policy 负责多规则收敛。**

不要把策略逻辑回写到规则层里，不然整套模型会很乱。

---

## 12.15 决策日志在引擎里应该怎么产出

很多人容易把日志当成执行后“顺手写一下”。 其实决策日志本身应该是执行链路的正式产物之一。

推荐的做法是：

### 决策结果对象和决策日志对象分离

#### `DecisionResult`

更偏下游消费，内容相对精简，例如：

- traceId
- eventId
- sceneCode
- finalAction
- finalScore
- version

#### `DecisionLog`

更偏分析和追溯，内容更完整，例如：

- 输入事件摘要
- 所用 snapshot version
- stream feature snapshot
- lookup values
- derived values
- hit rules
- final action
- latency
- error info

这两个对象可以由同一次执行同时产出，
然后分别写不同 topic 或不同 sink。

### 为什么建议异步输出日志？

因为如果热路径同步写 MySQL：

- 延迟会明显增加
- 容易被日志库抖动拖垮

所以更推荐：

```latex
Flink -> decision topic / decision log topic -> 异步 sink 到存储
```

这样热路径和分析链路就分开了。

---

## 12.16 指标在引擎里应该怎么打

除了决策日志，你还应该在 Flink 引擎中打指标。 因为平台不仅要能“出结果”，还要能“被观测”。

建议至少有三类指标：

### 1）吞吐类

- input event count
- decision output count
- scene-level TPS

### 2）延迟类

- 单事件总决策耗时
- stream feature 计算耗时
- lookup 耗时
- rule execution 耗时

### 3）质量/异常类

- rule hit count
- lookup timeout count
- no snapshot count
- expression eval error count
- groovy eval error count
- fallback count

这样后续你才能做：

- Dashboard
- 告警
- 性能分析
- 错误定位

---

## 12.17 推荐的主流程伪代码

下面我给你一版总流程伪代码。 这段代码不是最终实现，但非常适合你建立整体执行模型。

```java
public class DecisionEngineProcessFunction
        extends KeyedBroadcastProcessFunction<String, RiskEvent, SceneSnapshotEnvelope, DecisionResult> {

    private final RuntimeManager runtimeManager;
    private final StreamFeatureExecutor streamFeatureExecutor;
    private final LookupExecutor lookupExecutor;
    private final DerivedFeatureExecutor derivedFeatureExecutor;
    private final RuleExecutor ruleExecutor;
    private final PolicyExecutor policyExecutor;
    private final LogEmitter logEmitter;
    private final MetricsReporter metricsReporter;

    @Override
    public void processBroadcastElement(SceneSnapshotEnvelope envelope,
                                        Context ctx,
                                        Collector<DecisionResult> out) throws Exception {
        runtimeManager.onSnapshot(envelope, ctx.getBroadcastState(SNAPSHOT_STATE_DESC));
    }

    @Override
    public void processElement(RiskEvent event,
                               ReadOnlyContext ctx,
                               Collector<DecisionResult> out) throws Exception {

        long start = System.nanoTime();

        CompiledSceneRuntime runtime = runtimeManager.getRuntime(
                event.getSceneCode(),
                ctx.getBroadcastState(SNAPSHOT_STATE_DESC)
        );

        if (runtime == null) {
            metricsReporter.incNoSnapshot(event.getSceneCode());
            return;
        }

        EvalContext evalContext = EvalContext.fromEvent(event, runtime.getSnapshotMeta());

        // 1. stream features
        Map<String, Object> streamValues = streamFeatureExecutor.execute(runtime, event, ctx);
        evalContext.putAll(streamValues);

        // 2. lookup features
        Map<String, Object> lookupValues = lookupExecutor.execute(runtime, evalContext);
        evalContext.putAll(lookupValues);

        // 3. derived features
        Map<String, Object> derivedValues = derivedFeatureExecutor.execute(runtime, evalContext);
        evalContext.putAll(derivedValues);

        // 4. rules
        List<RuleHit> hits = ruleExecutor.execute(runtime, evalContext);

        // 5. policy
        DecisionResult result = policyExecutor.decide(runtime, evalContext, hits);

        long latencyNs = System.nanoTime() - start;
        result.setLatencyNs(latencyNs);

        // 6. emit result / log / metrics
        out.collect(result);
        logEmitter.emit(runtime, evalContext, hits, result);
        metricsReporter.report(runtime, result, hits, latencyNs);
    }
}
```

这段伪代码有几个重点：

### 1）快照处理和事件处理分开

- `processBroadcastElement` 只处理配置
- `processElement` 只处理事件

### 2）执行链路按层推进

- stream
- lookup
- derived
- rule
- policy

### 3）日志和指标是正式输出，不是临时打印

### 4）运行时对象从 `runtimeManager` 获取

这能让你的主流程非常清晰。

---

## 12.18 Flink 引擎端为什么要强调“执行计划”而不是“动态任意逻辑”

这是一个特别关键的思想。

你后面会越来越清楚，Flink 引擎最适合执行的是：

> **控制平台提前编译出来的一份“声明式运行计划”**

而不是：

> **任意用户脚本在运行时自由操控状态、窗口、定时器、类加载等底层语义**

为什么？

### 因为决策引擎要的是稳定性

- 可恢复
- 可审计
- 可解释
- 可回放

### 而不是无限自由度

如果你让运行态什么都能脚本化，会带来：

- 安全风险
- 性能风险
- 可维护性崩溃
- checkpoint 恢复困难

所以这条执行链路设计的关键不是“越灵活越好”，而是：

> **在可控的执行模型里，尽量支持业务动态化。**

这句话你后面在：

- stream feature 模板
- lookup 抽象
- derived feature
- 表达式 / Groovy 边界

这些地方都会反复用到。

---

## 12.19 引擎设计中几个非常重要的边界

这一节我专门总结一些你在实现时一定要守住的边界。

### 边界 1：快照只决定“执行什么”，不应该决定“Flink 底层怎么跑”

也就是说：

- 快照可以定义 feature / rule / policy
- 但不要让快照任意定义 watermark / state backend / 任意线程行为

### 边界 2：规则层不要再做外部 IO

规则执行应该建立在完整上下文之上。
不要一边跑规则一边再查 Redis / 调 HTTP。

### 边界 3：日志输出不要阻塞热路径

日志应该异步处理。

### 边界 4：Groovy / 表达式要在快照加载时编译，不要在每条事件上编译

这是性能和稳定性的底线。

### 边界 5：当前事件的执行必须绑定当前快照版本

日志、hit reason、result 都必须带版本号，保证可追溯。

### 边界 6：一条事件的上下文必须在同一个明确版本下完成

不要出现：

- stream feature 按新版本算
- rule 按旧版本执行

这也是为什么版本切换要小心处理。

---

## 12.20 一个完整的示例链路

下面用一条交易事件，把整条执行链路再串一次。

### 已知当前场景快照

`TRADE_RISK v12`

### 原始事件

```json
{
  "eventId": "E10001",
  "sceneCode": "TRADE_RISK",
  "eventType": "trade",
  "userId": "U1001",
  "deviceId": "D9001",
  "ip": "1.2.3.4",
  "amount": 6800,
  "result": "SUCCESS"
}
```

### 快照中要求的输入

- streamFeatures:
  - `user_trade_cnt_5m`
  - `user_trade_amt_sum_30m`
  - `device_bind_user_cnt_1h`

- lookupFeatures:
  - `device_in_blacklist`
  - `user_risk_level`

- derivedFeatures:
  - `high_amt_flag`
  - `trade_burst_flag`

- rules:
  - `R001`
  - `R002`
  - `R003`

- policy:
  - `FIRST_HIT`

### 运行过程

#### 1）定位快照

根据 `sceneCode=TRADE_RISK` 找到 `v12`

#### 2）初始化上下文

放入：

- amount=6800
- userId=U1001
- deviceId=D9001
- result=SUCCESS

#### 3）执行 stream feature

得到：

- user\_trade\_cnt\_5m = 3
- user\_trade\_amt\_sum\_30m = 18800
- device\_bind\_user\_cnt\_1h = 4

#### 4）执行 lookup

得到：

- device\_in\_blacklist = false
- user\_risk\_level = H

#### 5）执行 derived feature

得到：

- high\_amt\_flag = true
- trade\_burst\_flag = true

#### 6）执行规则

- R001: `device_in_blacklist == true` -> false
- R002: `trade_burst_flag == true` -> true
- R003: `device_bind_user_cnt_1h >= 4 && user_risk_level == 'H'` -> true

#### 7）策略收敛

如果 priority 是 R003 > R002，则最终：

- `REJECT`

#### 8）输出结果

`DecisionResult(scene=TRADE_RISK, version=12, action=REJECT)`

#### 9）输出日志

写出：

- 输入事件摘要
- 特征快照
- 命中规则 `[R002, R003]`
- 最终 action = REJECT
- latency = 37ms

这就是一条完整执行链。

---

## 12.21 这一章对后面章节的作用

这一章其实是 Flink 部分的“总索引”。

### 对第 13 章的作用

你已经知道快照在执行链里出现在哪，下一章会进一步讲：

- 快照如何进入 Flink
- Broadcast State 怎么设计
- 本地缓存怎么做
- 版本如何切换

### 对第 14 章的作用

你已经知道特征在链路中的位置，下一章会进一步讲：

- 哪些特征能动态化
- 哪些不能
- 为什么 stream feature 不能完全交给 Groovy

### 对第 15 章的作用

你已经知道 stream feature 是链路的重要部分，下一章会进一步讲：

- 它们的 state 怎么组织
- 中间数据怎么存

### 对第 16\~18 章的作用

你已经知道 rule / policy / expression / Groovy 在总链路中的位置，后面会进一步拆解它们的实现细节。

所以这一章的价值在于：

> **你先看清总链路，再去看各局部实现，不会迷路。**

---

## 12.22 本章小结

这一章最核心的内容，可以收敛为下面几句话。

### 1）Flink 引擎执行链路的本质

不是“直接跑规则”，而是：

- 消费事件
- 找当前快照
- 构建上下文
- 执行特征、规则、策略
- 输出结果

### 2）事件流和配置流是双流拓扑

- 事件流提供待处理事件
- 配置广播流提供当前执行逻辑
- 两者在引擎核心算子中汇合

### 3）EvalContext 是统一上下文数据平面

规则、派生特征、日志、解释都依赖它。

### 4）执行顺序必须清晰

基础字段 -> stream feature -> lookup -> derived -> rule -> policy -> result/log

### 5）日志和指标是正式产物

不是调试附属物，而是平台闭环的一部分。

### 6）Flink 更适合执行“快照驱动的运行计划”

而不是任意脚本自由操控底层状态语义。

---

## 12.23 下一章会讲什么

下一章进入：

## 第 13 章：Flink 如何接收、存储和切换运行时快照

这一章会专门解决：

- Broadcast State 应该怎么设计
- 为什么 Broadcast State 不直接存 Groovy 编译结果
- 本地编译缓存应该怎么组织
- 配置广播流到达后具体怎么处理
- 版本切换怎么保证安全
- 编译失败如何回退
- checkpoint 恢复后怎么重建本地缓存

也就是说，下一章会把这一章里“快照进入引擎”的部分再单独彻底讲透。
