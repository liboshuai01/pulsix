## 13.1 这一章解决什么问题

前面几章我们已经把下面几件事讲清楚了：

- 控制平台为什么要生成运行时快照
- 快照里应该包含哪些内容
- Flink 为什么要执行“运行时计划”，而不是直接理解后台多表
- 事件流和配置流会在 Flink 端汇合

但是到了真正落地时，还会出现一个非常具体、也非常关键的问题：

> **快照到底怎么进入 Flink？进入以后存在哪里？版本怎么切？编译失败怎么办？checkpoint 恢复以后怎么重建运行态？**

这件事如果设计不好，后面整个动态化能力都会很脆弱。常见问题包括：

- 配置消息已经到了，但引擎没有安全切换
- Broadcast State 和本地编译缓存不一致
- 把 Groovy 编译结果直接塞进状态，恢复时崩掉
- checkpoint 恢复后，本地缓存丢失但状态还在
- 新版本切到一半，旧版本和新版本混用
- 多次发布后，Groovy 类加载器泄漏，Metaspace 持续增长

所以这一章的目标非常明确，就是要讲透：

1. 配置广播流如何进入 Flink
2. Broadcast State 应该怎么设计
3. 为什么 Broadcast State 不应该直接存编译后的 Groovy 对象
4. 本地编译缓存是什么，为什么必须有
5. 版本切换怎么做才安全
6. checkpoint 恢复后如何重建本地运行态
7. 多版本切换时如何处理类加载器和旧缓存淘汰

你可以把这一章理解为：

> **它回答的是“快照从控制平台到 Flink 运行态”的完整工程机制。**

---

## 13.2 为什么“快照接收与切换”是 Flink 引擎的核心能力

很多人做动态规则系统时，一开始会把注意力放在：

- 表达式怎么写
- 规则怎么命中
- 特征怎么计算

但真正到了工程落地，你会发现一个更底层的问题：

> **不是规则能不能跑，而是“规则版本能不能稳定、可控地切进去”。**

这在实时风控里尤其重要。因为风控系统有两个非常特殊的特点：

### 1）运行时逻辑会频繁变化

- 阈值会改
- 规则会加
- 策略顺序会变
- 名单命中逻辑会调整
- derived feature 表达式会优化

也就是说，线上逻辑不是一成不变的。

### 2）它又必须保持运行稳定

- 不能因为发一条新规则就停机
- 不能因为切版本就丢状态
- 不能因为某条 Groovy 脚本编译失败就把所有流处理搞挂
- 不能因为 checkpoint 恢复就回到一个逻辑不完整的状态

所以对于 Flink 引擎来说， **“接收配置并稳定切换”不是附加能力，而是核心能力**。

如果这部分设计得好：

- 你的平台才真正有“热更新”能力
- 发布和回滚才会可靠
- 仿真和线上版本才容易对齐

如果这部分设计得差：

- 平台就会退化成“后台改表 + 作业读表 + 线上碰运气”

---

## 13.3 快照从控制平台进入 Flink 的完整路径

先建立全局图景。一个运行时快照从控制平台进入 Flink，大致会经历下面这条链路：

1. 控制平台读取设计态多表
2. 发布服务做校验、依赖分析、快照组装
3. 快照写入 `scene_release`
4. Flink 直接通过 MySQL CDC 订阅 `scene_release` / 发布表，或接收主动推送配置流
5. 配置流进入 Flink
6. 配置流作为 Broadcast Stream 进入引擎
7. `processBroadcastElement` 解析快照、校验版本、构建本地编译缓存
8. 可序列化快照写入 Broadcast State
9. 后续 `processElement` 处理业务事件时，读取当前场景快照与本地运行态对象
10. 规则、特征、策略执行都基于当前活跃版本进行

这条链路看起来简单，但里面其实包含了几个关键分层：

### 控制平台负责

- 生成快照
- 赋予版本号
- 决定什么是“当前待上线逻辑”

### MySQL CDC / 主动推送负责

- 把配置传播给所有 Flink Subtask

### Broadcast State 负责

- 持久化可恢复的运行时配置

### 本地 transient cache 负责

- 保存不可序列化、为了性能而存在的编译结果

这四层分工必须清楚。否则你后面很容易把“可恢复配置”和“不可序列化运行时对象”混在一起。

---

## 13.4 配置广播消息建议长什么样

Flink 端接收到的，不应该只是裸的 `snapshot_json`，而应该是一个带元信息的“配置包络对象”。

推荐设计成类似下面的结构：

```json
{
  "sceneCode": "TRADE_RISK",
  "versionNo": 12,
  "opType": "PUBLISH",
  "checksum": "9d8c1a...",
  "publishedAt": "2026-03-07T20:00:00Z",
  "effectiveFrom": "2026-03-07T20:00:10Z",
  "snapshot": {
    "sceneCode": "TRADE_RISK",
    "version": 12,
    "streamFeatures": [],
    "lookupFeatures": [],
    "derivedFeatures": [],
    "rules": [],
    "policy": {}
  }
}
```

建议至少带上下面这些字段：

### `sceneCode`

用于标识这条配置属于哪个场景。

### `versionNo`

用于判断版本新旧、去重和回滚。

### `opType`

建议支持：

- `PUBLISH`
- `ROLLBACK`
- `DISABLE`

对于个人项目，一期只做 `PUBLISH` 和 `ROLLBACK` 就够了。

### `checksum`

用于识别内容是否变化、避免重复应用相同快照。

### `publishedAt`

用于审计和观测。

### `effectiveFrom`

用于控制生效时机。这个字段很有价值，但实现方式要分层：

- MVP 阶段可以做“发布即生效”
- 增强阶段可以支持“稍后生效”

### `snapshot`

真正的运行时快照内容。

这个“包络对象”非常重要，因为：

- Broadcast State 需要的不只是业务配置，还要有版本元信息
- 日志中需要记录当前使用的是哪个版本
- 配置乱序、重复投递、回滚场景都需要这些元信息来判断

---

## 13.5 配置流进入 Flink 的方式：为什么推荐 Broadcast Stream

Flink 端处理配置流，最常用也最适合实时风控的方式，就是：

> **事件主流 + 配置广播流，然后通过** `KeyedBroadcastProcessFunction`\*\* /\*\* `BroadcastProcessFunction`\*\* 把两者连接起来。\*\*

典型拓扑如下：

```latex
 Kafka `pulsix.event.standard`             --->  eventStream  ---\\
                                                           connect ---> KeyedBroadcastProcessFunction ---> decisionStream
MySQL CDC / 可选 Kafka `pulsix.config.snapshot` --->  configStream ---/
```

为什么是广播流？

因为每个并行子任务都要知道当前场景的最新快照。否则：

- task 1 跑旧规则
- task 2 跑新规则
- 同一个场景在不同并行度下执行结果不一致

广播流的价值就是：

- 所有 subtask 都能收到同样的配置消息
- 每个 subtask 都维护一份本地一致的配置视图
- checkpoint 会把 Broadcast State 一并持久化

这非常适合“低频配置变更 + 高频事件执行”的场景。

---

## 13.6 配置流在工程上要注意的两个前提

虽然 Broadcast Stream 很适合，但要注意两个现实问题。

### 13.6.1 配置流的顺序问题

配置流虽然低频，但顺序很重要。你至少要保证同一 scene 的版本更新顺序可判定。

建议按接入方式区分：

#### 方案 A：直接走 MySQL CDC，并用 `versionNo` 做版本判定

这是你当前项目更自然的方案。

优点：

- 不需要 Kafka
- 直接读取发布表快照和 binlog
- 架构更简单

注意点：

- 要开启 MySQL binlog
- 要给 CDC 账号开通复制读取权限
- 仍然要用 `versionNo` / `checksum` 做幂等和乱序保护

#### 方案 B：配置流走 Kafka 时，配置 topic 单分区

最简单、最稳。
对于个人项目非常推荐。

优点：

- 全局顺序天然成立
- 实现简单

缺点：

- 吞吐有限

但配置流吞吐本来就很低，所以这通常不是问题。

#### 方案 C：Kafka 多分区，但按 `sceneCode` 做 key，并用 `versionNo` 做防乱序判断

适合未来扩展，但复杂度更高。

对于你当前项目，我建议：

> **优先直接用 MySQL CDC；如果配置流未来必须走 Kafka，再先用单分区保证顺序简单可靠。**

### 13.6.2 配置消息要支持幂等处理

MySQL CDC、Kafka、下游重试都可能导致同一个版本消息重复到达。Flink 端要能正确处理：

- 重复消息
- 老版本消息晚到
- 相同 checksum 的重复发布

所以在 `processBroadcastElement` 中必须做：

- 版本比较
- checksum 比较
- opType 判断

不能“来一条配一条”。

---

## 13.7 Broadcast State 应该怎么设计

这部分是本章的核心之一。

你要先建立一个非常重要的认知：

> **Broadcast State 存的是“可恢复配置”，不是“所有运行时对象”。**

它的目标是：

- 被 checkpoint 持久化
- 恢复时可直接拿回来
- 所有 subtask 都有一份
- 能支撑运行时版本定位

### 13.7.1 推荐的状态结构

如果你的一个 Flink 作业支持多个场景，推荐这样设计：

```java
MapStateDescriptor<String, SceneSnapshotEnvelope> SNAPSHOT_STATE_DESC
```

其中：

- key = `sceneCode`
- value = `SceneSnapshotEnvelope`

也就是说，Broadcast State 里按场景保存“当前运行中的快照包络对象”。

对应 Java 概念可以是：

```java
class SceneSnapshotEnvelope implements Serializable {
    private String sceneCode;
    private long versionNo;
    private String opType;
    private String checksum;
    private long publishedAt;
    private Long effectiveFrom;
    private SceneSnapshot snapshot;
}
```

### 13.7.2 为什么建议只保留“当前稳定版本”

很多人会问：

- 要不要把历史版本也放进 Broadcast State？

通常不建议。

因为：

- 历史版本已经在 MySQL `scene_release` 里有完整记录
- Broadcast State 的主要目的是支撑运行态执行和恢复
- 保留过多历史版本会让状态膨胀
- 回滚完全可以通过“重新广播旧版本 snapshot”来完成

所以推荐策略是：

> **Broadcast State 只保留每个 scene 当前活跃的稳定版本。**

必要时最多可以在增强版里保留一个 `pending` 版本，但不要无限积累历史。

### 13.7.3 如果要支持 delayed effectiveFrom，状态结构怎么升级

如果你后面要支持“新版本先下发、稍后生效”，那么只存一个活跃版本可能不够。

这时可升级为：

```java
class SceneVersionBundle implements Serializable {
    private SceneSnapshotEnvelope active;
    private SceneSnapshotEnvelope pending;
}
```

不过对于一期项目，我建议先做：

- 发布即生效
- Broadcast State 只存当前活跃快照

这样复杂度最低。

---

## 13.8 为什么 Broadcast State 不应该直接存 Groovy 编译结果

这也是本章的关键点之一，必须讲透。

有些人会想：

- 反正新快照来了就要编译 Groovy
- 那我直接把编译后的 Groovy Class、Script、Invoker 放进 Broadcast State，不就恢复时更快？

这个想法在工程上通常是错的。

### 13.8.1 原因 1：编译结果通常不可安全序列化

Groovy 编译出来的对象往往包含：

- ClassLoader 引用
- 动态字节码
- 方法句柄
- 运行时上下文

这些对象并不适合作为 Flink 状态做序列化、checkpoint 和恢复。

### 13.8.2 原因 2：恢复时容易出现 ClassLoader 问题

即使你勉强把某些对象序列化进去，也很容易在恢复时遇到：

- 反序列化失败
- 类定义丢失
- 旧版本类无法正确加载
- metaspace 持续增长

### 13.8.3 原因 3：表达式/Groovy 编译结果本质上是“运行时加速对象”

它们存在的意义是：

- 提升执行效率
- 避免每条事件重复编译

它们不是“系统真实状态”的一部分。

系统真正需要恢复的是：

- 这时应该使用哪一份快照
- 这份快照长什么样

至于编译结果，可以在本地重新构建。

### 13.8.4 正确分层

所以正确做法应该是：

#### Broadcast State 存

- 原始快照 JSON 或可序列化 POJO
- 版本、checksum、effectiveFrom 等元信息

#### 本地 transient cache 存

- 编译后的表达式对象
- 编译后的 Groovy Invoker / Class
- 依赖图
- 规则执行计划
- feature 执行计划

这是非常关键的边界。

你可以记一句非常重要的话：

> **状态中存“可恢复的真相”，本地缓存中存“为了性能存在的衍生对象”。**

---

## 13.9 本地编译缓存是什么，为什么必须有

如果 Broadcast State 只存快照，那运行时高性能从哪里来？

答案就是：

> **本地编译缓存。**

它通常是算子实例内的 `transient` 内存对象，不会被 checkpoint 序列化。

### 13.9.1 为什么必须有本地缓存

因为你不能在每条事件上做这些事情：

- 反序列化 snapshot
- 重新解析表达式
- 重新编译 Groovy
- 重新做依赖拓扑排序

这会直接把性能打崩。

所以合理方式是：

- 配置变更时，编译一次
- 事件执行时，复用很多次

### 13.9.2 推荐的数据结构

```java
transient Map<String, CompiledSceneRuntime> localRuntimeCache;
```

其中 key 可以用：

```latex
sceneCode#versionNo
```

也可以再维护一个场景到当前活跃 runtime 的索引：

```java
transient Map<String, CompiledSceneRuntime> activeRuntimeByScene;
```

### 13.9.3 `CompiledSceneRuntime` 建议长什么样

```java
class CompiledSceneRuntime {
    private SceneSnapshotEnvelope envelope;
    private SceneSnapshot snapshot;
    private Map<String, DerivedFeatureInvoker> derivedInvokers;
    private Map<String, RuleInvoker> ruleInvokers;
    private PolicyRuntime policyRuntime;
    private FeatureExecutionPlan featurePlan;
    private ClassLoader groovyClassLoader;
    private long compiledAt;
}
```

这里面最重要的几个对象：

- `derivedInvokers`：派生特征执行器
- `ruleInvokers`：规则执行器
- `policyRuntime`：策略执行对象
- `featurePlan`：特征执行计划
- `groovyClassLoader`：如果用了 Groovy，则单版本单 loader

### 13.9.4 为什么 key 要带 versionNo

n
因为同一个场景未来可能：

- 发布新版本
- 回滚旧版本
- 在某个时刻存在新旧版本切换过程

如果你只用 `sceneCode` 做 key，很容易覆盖旧对象，不利于安全切换和问题排查。

所以最好：

- `activeRuntimeByScene` 用 `sceneCode`
- `localRuntimeCache` 用 `sceneCode#version`

---

## 13.10 `processBroadcastElement` 应该做什么

这是 Flink 端配置切换的入口。它不是简单 `put state`，而应该是一套完整处理流程。

推荐步骤如下：

### 第一步：反序列化配置消息

得到 `SceneSnapshotEnvelope`。

### 第二步：基础校验

检查：

- `sceneCode` 是否为空
- `versionNo` 是否合法
- `snapshot` 是否为空
- `checksum` 是否存在

### 第三步：幂等和版本判断

读取当前 Broadcast State 中的已有版本，如果发现：

- 新消息版本号更小，忽略
- 版本号相同且 checksum 相同，忽略
- 版本号相同但 checksum 不同，记录异常

### 第四步：本地尝试编译运行时对象

这个顺序很重要：

> **先在内存里尝试编译，成功后再真正切换版本。**

也就是说：

- 编译表达式
- 编译 Groovy
- 构建 rule invoker
- 构建 policy runtime
- 构建 feature 执行计划

### 第五步：编译成功后更新本地缓存

把 `CompiledSceneRuntime` 放入：

- `localRuntimeCache`
- `activeRuntimeByScene`

### 第六步：写入 Broadcast State

把 envelope 写入：

- `broadcastState.put(sceneCode, envelope)`

### 第七步：清理旧版本本地缓存（可延后）

如果只保留最近 1\~2 个版本，可以在此处做清理。

### 第八步：输出版本切换日志和指标

例如：

- version switch success count
- compile duration
- current active version gauge

这个过程有一个非常重要的设计原则：

> **先编译，后激活；编译成功再写状态；状态和本地缓存要尽量同步推进。**

---

## 13.11 `processElement` 如何读取当前运行版本

业务事件进入时，处理逻辑大致如下：

### 1）从事件中取出 `sceneCode`

### 2）优先从本地活动缓存中找

例如：

```java
CompiledSceneRuntime runtime = activeRuntimeByScene.get(sceneCode);
```

### 3）如果本地没有，但 Broadcast State 中有

这通常发生在：

- job 刚恢复
- 本地缓存是 transient，已经丢失
- 但 Broadcast State 已经恢复成功

这时应该：

- 从 `ReadOnlyBroadcastState` 读取 envelope
- 重新本地编译出 `CompiledSceneRuntime`
- 放入本地缓存

### 4）如果两边都没有

说明该 scene 还没有发布版本，应该：

- 记录异常
- 走默认兜底策略（如 PASS / DROP / ERROR）

### 5）拿到运行时对象后，继续处理事件

后续：

- 计算 stream feature
- lookup Redis
- 计算 derived feature
- 执行 rules/policy

这里你要注意一个关键点：

> **事件执行时不应该再去理解设计态，而只依赖当前** `CompiledSceneRuntime`**。**

---

## 13.12 版本切换怎么保证“尽量安全”

这是本章最关键的工程问题之一。

所谓“安全切换”，不是说绝对没有风险，而是说切换过程尽量满足：

- 不中断事件处理
- 不使用半编译状态
- 不让旧版本对象被无序覆盖
- 出错时能回到上一个稳定版本

### 13.12.1 最重要原则：先编译成功，再切换引用

错误做法是：

- 消息一到，先更新 active 版本
- 然后再开始编译表达式/Groovy

这样如果中途失败，你就会出现：

- active 指向新版本
- 但运行时对象不完整

正确做法是：

1. 先拿当前 active runtime 作为旧版本
2. 在内存中新建 candidate runtime
3. candidate 完整编译成功
4. 再原子地把 `activeRuntimeByScene(sceneCode)` 指向新版本
5. 再写 Broadcast State

### 13.12.2 最简单的切换模型：发布即生效

对个人项目，一期推荐：

- 快照到达即生效
- 不搞复杂的 future effectiveFrom
- 当前 scene 始终只有一个 active runtime

这个方案简单且稳。

### 13.12.3 如果要支持 `effectiveFrom`

增强版可以这样做：

- 收到新版本后先编译成 `pendingRuntime`
- 到达 `effectiveFrom` 时再切换为 active

但这意味着你至少需要维护：

- active
- pending

同时 checkpoint 也最好能反映这个状态。

对于当前项目，建议先把字段保留，但实现可以简化为“收到即生效”。

### 13.12.4 处理乱序与重复发布

切换时要遵守下面规则：

- `newVersion < currentVersion`：忽略
- `newVersion == currentVersion && sameChecksum`：幂等忽略
- `newVersion == currentVersion && diffChecksum`：记录异常并告警
- `newVersion > currentVersion`：正常尝试切换

### 13.12.5 回滚本质也是一次版本切换

回滚不是特殊魔法，本质上就是：

- 把历史某个 snapshot 重新广播给 Flink
- Flink 把它视为新的活跃版本切换

因此从引擎视角， `ROLLBACK` 和 `PUBLISH` 的处理流程非常相似。

---

## 13.13 编译失败时应该怎么办

这个问题不能回避。

即使控制平台已经做了校验，Flink 本地编译仍可能失败，例如：

- 表达式引擎版本不一致
- Groovy 脚本用了非法写法
- 本地资源异常
- 类加载器问题

这时有两种处理策略。

### 13.13.1 策略 A：严格一致型（生产更推荐）

做法：

- 新版本本地编译失败，直接抛异常
- 让 task fail/restart
- 由 checkpoint 恢复到上一个稳定版本
- 同时控制平台或运维介入回滚

优点：

- 不容易出现不同 subtask 使用不同版本

缺点：

- 发布异常会引起 job 重启

### 13.13.2 策略 B：保守回退型（个人项目更好演示）

做法：

- 新版本编译失败，不更新 active runtime
- 不写入 Broadcast State 或保留旧稳定值
- 发错误日志/错误 topic
- 继续使用旧版本处理事件

优点：

- 引擎可持续运行

缺点：

- 严格一致性不如 fail-fast 强

### 13.13.3 对你项目的建议

我建议你采用“两层防线”：

#### 第一层：控制平台发布前预编译校验

尽量把绝大多数错误挡在发布前。

#### 第二层：Flink 本地编译失败时默认保留旧版本，并输出强告警

这样更利于个人项目演示，也更符合你当前的实现复杂度。

但你要在文档中明确说明：

> **如果未来追求最严格的一致性，生产方案应进一步引入 fail-fast 与集中回滚机制。**

这样你的设计边界会显得很清楚。

---

## 13.14 checkpoint 恢复后如何重建本地缓存

这是很多人第一次做 Broadcast State 时最容易忽略的问题。

### 13.14.1 恢复后什么还在，什么没了？

恢复后：

- Keyed State 还在
- Broadcast State 还在
- Timer 还在

但是：

- `transient localRuntimeCache` 没了
- `transient activeRuntimeByScene` 没了
- Groovy ClassLoader 没了
- 编译后的表达式对象没了

这就意味着：

> **checkpoint 恢复以后，Broadcast State 恢复的是“配置真相”，但本地运行态加速对象必须自己重建。**

### 13.14.2 为什么不能在 `open()` 里直接重建所有本地缓存

因为 Broadcast State 通常只能在：

- `processBroadcastElement`
- `processElement`
- `onTimer`

这些上下文里读取。

在 `open()` 里你没有 `Context`，拿不到 Broadcast State 的完整视图。

### 13.14.3 最实用的重建策略：惰性重建

推荐这样做：

#### 场景 A：配置流再次到达

- 重新触发 `processBroadcastElement`
- 本地缓存自然被重建

#### 场景 B：事件先到达

- 在 `processElement` 里发现 `activeRuntimeByScene` 没有该 scene
- 从 `ReadOnlyBroadcastState` 读取 snapshot
- 现场重新编译构建 `CompiledSceneRuntime`
- 放回本地缓存

这种方式实现简单，也足够稳。

### 13.14.4 惰性重建的注意事项

- 第一次重建可能比平时慢一些
- 需要做好 metrics 记录
- 要确保编译过程幂等
- 要避免多个事件同时重复重建同一个 scene runtime

不过在 Flink 普通算子线程模型下，同一个 subtask 的 `processElement` / `processBroadcastElement` 是串行执行的，所以并发竞争通常没有那么大。

---

## 13.15 多版本切换时如何避免类加载器问题

只要你用了 Groovy，这一节就必须严肃对待。

### 13.15.1 为什么会有类加载器问题

Groovy 动态编译通常会：

- 生成新的 class
- 通过某个 `GroovyClassLoader` 加载

如果你每次发版都：

- 编译一批新 class
- 旧 class loader 又一直被引用

那很容易出现：

- Metaspace 持续增长
- 旧版本类无法卸载
- 长时间运行后内存异常

### 13.15.2 推荐做法：一版本一 loader

也就是：

- 每个 `sceneCode#version` 对应一个独立 GroovyClassLoader
- 这个 loader 只服务于该版本的 `CompiledSceneRuntime`

优点是：

- 生命周期清晰
- 旧版本淘汰时更容易整体释放

### 13.15.3 旧版本淘汰策略

推荐只保留：

- 当前 active version
- 可选保留上一个 recent version

当旧版本不再被任何 runtime holder 引用时：

- 从本地缓存移除
- 清理对 classloader 的引用
- 如可能则调用 close

### 13.15.4 不要使用全局静态脚本缓存

很多人为了“复用”，会把 Groovy 缓存做成全局静态单例。

这在普通 Web 应用里有时问题不大，
但在 Flink 长时间运行的任务里非常容易变成：

- 版本不清
- 难以淘汰
- 内存泄漏

所以建议：

> **缓存生命周期跟随 subtask 和 sceneVersion，而不是全局静态单例。**

---

## 13.16 Broadcast State、本地缓存、事件处理三者的关系

到这里你可以把三者关系总结成下面这个模型：

### Broadcast State

负责保存：

- 当前场景的稳定快照
- 能被 checkpoint 恢复

### 本地编译缓存

负责保存：

- 表达式编译结果
- Groovy Invoker
- feature/rule 执行计划
- 只为了性能存在

### 事件处理逻辑

负责：

- 读取当前场景 active runtime
- 真正执行特征 / 规则 / 策略

三者关系可以用一句话概括：

> **Broadcast State 保证“我知道该跑什么版本”，本地缓存保证“我能高效地跑这个版本”，事件处理保证“每条事件按当前版本执行”。**

---

## 13.17 推荐的核心 Java 结构草图

下面给一版比较适合你项目的类结构草图。

```java
class SceneSnapshotEnvelope implements Serializable {
    String sceneCode;
    long versionNo;
    String opType;
    String checksum;
    long publishedAt;
    Long effectiveFrom;
    SceneSnapshot snapshot;
}

class SceneSnapshot implements Serializable {
    String sceneCode;
    long version;
    List<StreamFeatureSpec> streamFeatures;
    List<LookupFeatureSpec> lookupFeatures;
    List<DerivedFeatureSpec> derivedFeatures;
    List<RuleSpec> rules;
    PolicySpec policy;
    RuntimeHints runtimeHints;
}

class CompiledSceneRuntime {
    SceneSnapshotEnvelope envelope;
    SceneSnapshot snapshot;
    Map<String, DerivedFeatureInvoker> derivedInvokers;
    Map<String, RuleInvoker> ruleInvokers;
    PolicyRuntime policyRuntime;
    FeatureExecutionPlan featurePlan;
    ClassLoader groovyClassLoader;
    long compiledAt;
}
```

算子里的核心成员可以是：

```java
private transient Map<String, CompiledSceneRuntime> localRuntimeCache;
private transient Map<String, CompiledSceneRuntime> activeRuntimeByScene;
private final MapStateDescriptor<String, SceneSnapshotEnvelope> SNAPSHOT_STATE_DESC = ...;
```

---

## 13.18 推荐的伪代码： `processBroadcastElement`

```java
@Override
public void processBroadcastElement(
        SceneSnapshotEnvelope incoming,
        Context ctx,
        Collector<DecisionResult> out) throws Exception {

    BroadcastState<String, SceneSnapshotEnvelope> state = ctx.getBroadcastState(SNAPSHOT_STATE_DESC);

    String sceneCode = incoming.getSceneCode();
    SceneSnapshotEnvelope current = state.get(sceneCode);

    // 1. 基础校验
    validateEnvelope(incoming);

    // 2. 幂等 / 乱序判断
    if (current != null) {
        if (incoming.getVersionNo() < current.getVersionNo()) {
            return;
        }
        if (incoming.getVersionNo() == current.getVersionNo()
                && Objects.equals(incoming.getChecksum(), current.getChecksum())) {
            return;
        }
    }

    // 3. 先本地编译 candidate runtime
    CompiledSceneRuntime candidate = compileRuntime(incoming);

    // 4. 编译成功后再激活
    localRuntimeCache.put(sceneCode + "#" + incoming.getVersionNo(), candidate);
    activeRuntimeByScene.put(sceneCode, candidate);

    // 5. 更新 broadcast state
    state.put(sceneCode, incoming);

    // 6. 清理旧版本（可选）
    cleanupOldVersions(sceneCode, incoming.getVersionNo());

    // 7. 指标与日志
    metrics.versionSwitchSuccess(sceneCode, incoming.getVersionNo());
}
```

如果你采用“编译失败保留旧版本”的策略，那么 `compileRuntime(incoming)` 异常时：

- 捕获异常
- 记录 error topic / metrics
- 不更新 `activeRuntimeByScene`
- 不覆盖 Broadcast State 中当前稳定版本

---

## 13.19 推荐的伪代码： `processElement`

```java
@Override
public void processElement(
        StandardEvent event,
        ReadOnlyContext ctx,
        Collector<DecisionResult> out) throws Exception {

    String sceneCode = event.getSceneCode();
    CompiledSceneRuntime runtime = activeRuntimeByScene.get(sceneCode);

    // 1. 本地缓存缺失，尝试从 broadcast state 惰性重建
    if (runtime == null) {
        ReadOnlyBroadcastState<String, SceneSnapshotEnvelope> state = ctx.getBroadcastState(SNAPSHOT_STATE_DESC);
        SceneSnapshotEnvelope envelope = state.get(sceneCode);
        if (envelope != null) {
            runtime = compileRuntime(envelope);
            localRuntimeCache.put(sceneCode + "#" + envelope.getVersionNo(), runtime);
            activeRuntimeByScene.put(sceneCode, runtime);
        }
    }

    if (runtime == null) {
        out.collect(defaultFallbackDecision(event, "NO_ACTIVE_SNAPSHOT"));
        return;
    }

    // 2. 构建上下文
    EvalContext evalContext = buildBaseContext(event);

    // 3. 计算 stream feature
    fillStreamFeatures(evalContext, event, runtime.getFeaturePlan());

    // 4. lookup Redis / profile
    fillLookupFeatures(evalContext, event, runtime.getSnapshot());

    // 5. 派生特征
    fillDerivedFeatures(evalContext, runtime.getDerivedInvokers());

    // 6. 执行规则与策略
    DecisionResult result = evaluateRulesAndPolicy(event, evalContext, runtime);

    out.collect(result);
}
```

---

## 13.20 常见坑与设计边界

这一节非常重要，我把这一章最容易踩的坑集中列一下。

### 坑 1：Broadcast State 存太多历史版本

会导致：

- 状态膨胀
- checkpoint 变重
- 恢复变慢

建议只存当前稳定版本，必要时最多保留一个 pending。

### 坑 2：把大名单也广播进来

Broadcast State 适合小而关键的配置，不适合：

- 百万级黑名单
- 高频名单项更新

大名单应放 Redis / 外部 KV。

### 坑 3：每条事件动态编译 Groovy

这几乎是灾难性设计。必须做到：

- 版本切换时编译一次
- 事件执行时只复用

### 坑 4：本地缓存没有淘汰机制

连续多次发版后，很容易：

- 堆积很多旧 runtime
- 累积很多 ClassLoader

至少要有最近版本保留策略。

### 坑 5：checkpoint 恢复后忘记重建本地缓存

结果就是：

- 状态恢复了
- 但事件一来找不到 runtime

一定要做惰性重建。

### 坑 6：版本切换使用事件时间强一致

这是个很诱人的方向，但复杂度很高。

比如你会问：

- 一条晚到事件是不是应该按旧版本跑？

对于个人项目，建议明确边界：

> **版本切换按处理时间生效，而不是按事件时间回溯生效。**

也就是说，晚到事件默认按当前活跃版本执行。这样系统可控性更高、实现也更清晰。

### 坑 7：配置编译失败后处理策略不清

必须提前设计好是：

- fail-fast 重启
- 还是保留旧版本继续运行

不要到了线上临时拍脑袋。

---

## 13.21 这一章最终产出什么

如果你把这一章落到工程上，最后应该至少产出下面这些东西：

### 1）配置包络对象

- `SceneSnapshotEnvelope`

### 2）Broadcast State 结构

- `MapStateDescriptor<String, SceneSnapshotEnvelope>`

### 3）本地运行态缓存结构

- `Map<String, CompiledSceneRuntime>`
- `Map<String, CompiledSceneRuntime> activeRuntimeByScene`

### 4）版本切换策略

- 幂等判断
- 乱序判断
- compile-before-activate
- old version cleanup

### 5）恢复策略

- checkpoint 恢复后惰性重建本地缓存

### 6）Groovy classloader 生命周期策略

- 一版本一 loader
- 旧版本淘汰时释放引用

这些东西一旦设计清楚，后面你写 Flink 代码时会非常顺。

---

## 13.22 本章小结

这一章的核心可以压缩成下面几句话。

### 1）快照进入 Flink 的最佳方式是“配置流 + Broadcast State”

这是实时风控里最自然、最稳定的做法。

### 2）Broadcast State 存的是可恢复配置，不是运行时编译对象

可恢复真相和性能缓存必须分层。

### 3）本地编译缓存是必须的

没有它，表达式和 Groovy 根本跑不出合理性能。

### 4）版本切换的关键原则是“先编译成功，再激活新版本”

也就是 compile-before-activate。

### 5）checkpoint 恢复后，Broadcast State 会回来，但本地缓存不会

必须设计惰性重建机制。

### 6）Groovy 的真正难点不只是执行，而是类加载器生命周期

必须做版本级管理和旧版本淘汰。

---

## 13.23 下一章会讲什么

下一章我们进入：

**第 14 章：实时特征系统设计——哪些特征能动态化，哪些不能**

这一章会重点回答：

- 什么是 stream feature / lookup feature / derived feature
- 三类特征在引擎中的执行边界分别是什么
- 为什么流式特征不能完全交给 Groovy 自由发挥
- 高并发下如何设计一个“可动态配置”的特征系统
- 特征的版本、warm-up、兼容性怎么处理
- 为什么修改窗口定义不能简单热替换

也就是说，下一章会继续顺着这一章往下走，进一步回答：

> **快照已经切进来了，那么特征系统到底该如何在 Flink 中被实现。**
