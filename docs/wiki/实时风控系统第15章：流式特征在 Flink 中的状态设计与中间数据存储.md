第 15 章：流式特征在 Flink 中的状态设计与中间数据存储

## 15.1 这一章解决什么问题

前面几章我们已经逐步走到了 Flink 引擎内部：

- 第 11 章讲了 Flink 在实时风控系统中的角色与边界
- 第 12 章讲了 Flink 引擎整体执行链路
- 第 13 章讲了快照如何进入 Flink、如何被存储与切换
- 第 14 章讲了特征系统的分类，以及哪些特征适合动态化、哪些不适合

接下来就进入一个真正偏“引擎实现内核”的问题：

> 流式特征在 Flink 里到底怎么存？怎么计算？中间状态放哪？怎么清理？怎么恢复？

这是实时风控系统里最容易“听起来会、真正做起来容易乱”的部分。

因为很多人都知道要做下面这些特征：

- 用户 5 分钟交易次数
- 用户 30 分钟交易金额和
- 设备 1 小时关联账号数
- IP 10 分钟注册次数
- 最近一次登录时间
- 最近一次失败登录原因

但真到实现时就会开始遇到一大堆具体问题：

- 这些特征到底按什么 key 存 state？
- 一个 feature 用一个算子，还是做通用执行器？
- 5 分钟窗口的 count 用 Window API 还是自己维护 bucket？
- 当前事件要不要算进当前结果里？
- 特征的中间数据应该存在 Flink State 还是 Redis？
- distinct\_count 应该怎么做？
- state TTL 怎么配？
- timer 什么时候注册，什么时候清？
- 特征定义变了以后旧 state 怎么办？
- checkpoint 恢复时特征状态会恢复到什么程度？
- 热路径里哪些数据必须在 Flink State 中，哪些应该查 Redis？

所以这一章的目标，就是把“流式特征系统的内部实现模型”讲透。

这一章会重点回答：

1. 流式特征在 Flink 里的状态主键该怎么设计
2. 常见聚合类型分别应该怎么存 state
3. bucket 化状态为什么很适合实时风控
4. timer 和 TTL 怎么配合使用
5. 中间数据哪些该放 Flink State，哪些该放 Redis
6. RocksDB / HashMapStateBackend 分别适合什么
7. 特征变更、版本升级、恢复、回滚时要注意什么
8. 你的一期项目最推荐采用什么状态实现方案

简单说：

> 这一章要解决的是“实时特征在 Flink 内部到底如何存在与运转”。

---

## 15.2 先建立一个核心认知：流式特征本质上是“状态演化结果”

很多人在平台层面看特征时，会把特征理解成一个变量，比如：

- `user_trade_cnt_5m`
- `device_bind_user_cnt_1h`
- `ip_register_cnt_10m`

从控制平台、规则平台视角看，这样理解没有问题。 但从 Flink 引擎实现视角看，这种理解还不够。

在 Flink 内部，你必须把流式特征理解成：

> **某个实体在某种聚合语义下，随着事件不断到来而持续演化的一份状态结果。**

比如：

- `user_trade_cnt_5m`
  - 不是一条静态数据
  - 而是“某个用户最近 5 分钟交易事件累积后的 count 结果”

- `user_trade_amt_sum_30m`
  - 不是一条简单字段
  - 而是“某个用户最近 30 分钟满足条件交易金额的滚动 sum”

- `device_bind_user_cnt_1h`
  - 不是一条数据库字段
  - 而是“某个设备在最近 1 小时内出现过多少不同 userId”的近实时聚合结果

也就是说：

> **流式特征不是普通变量，而是“状态 + 时间语义 + 聚合逻辑”的结果。**

这句话非常重要，因为它直接决定：

- 这类特征不能只靠表达式引擎算
- 这类特征必须由 Flink State 来支撑
- 这类特征的定义修改，不等于简单字符串修改
- 这类特征的运行态实现，必须考虑时间、空间、恢复、一致性

---

## 15.3 流式特征的状态模型到底围绕什么组织

一个流式特征在 Flink 中，至少要回答下面几个问题：

1. 按谁聚合？
2. 聚合什么？
3. 聚合多长时间？
4. 如何清理过期数据？
5. 当前结果如何计算？
6. 当前结果是否包含当前事件？

如果把这几件事统一起来，一个流式特征的运行时身份，其实可以抽象成下面这个元组：

```latex
(sceneCode, featureCode, entityKey)
```

其中：

- `sceneCode`：属于哪个风控场景
- `featureCode`：是哪一个流式特征
- `entityKey`：这个特征当前作用于哪个实体

例如：

```latex
(TRADE_RISK, user_trade_cnt_5m, U1001)
(TRADE_RISK, user_trade_amt_sum_30m, U1001)
(LOGIN_RISK, ip_login_fail_cnt_10m, 1.2.3.4)
(REGISTER_ANTI_FRAUD, device_register_user_cnt_1h, D9001)
```

这就是状态组织的核心主键。

你后面实现通用特征执行器时，脑子里一定要明确：

> **Flink State 中真正维护的是“某个 feature 在某个 entity 上的运行状态”。**

而不是笼统地“存一份 scene 状态”。

---

## 15.4 状态主键设计：feature-instance-key 应该怎么理解

在实际代码里，很多人会问：

- 我要不要把 featureCode 拼进 keyBy？
- 一个 feature 一个算子行不行？
- 通用执行器怎么组织状态？

这里我给你一个工程上非常实用的理解。

### 15.4.1 逻辑主键

逻辑上，一个流式特征实例的唯一标识就是：

```latex
featureInstanceKey = sceneCode + "|" + featureCode + "|" + entityKey
```

但注意，这只是逻辑概念，不一定真的作为字符串去 keyBy。

---

### 15.4.2 物理实现方式有两种

#### 方式 A：一个 feature 一个专用算子/子流

这种方式下：

- 你按实体 keyBy
- 每个 feature 独立维护 state

优点：

- 实现直观
- 单 feature 调试简单

缺点：

- feature 一多算子链很臃肿
- 动态 feature 平台化困难
- 发布时扩缩容和维护成本高

---

#### 方式 B：通用特征执行器 + 多 feature 复用一套算子

这种方式下：

- 按 entityKey 或组合 keyBy
- 在 state 内部再按 featureCode 分桶/分 namespace

优点：

- 更适合平台化
- 更容易让控制平台动态发布 feature
- 引擎结构更统一

缺点：

- 实现复杂度更高
- 状态结构设计要更慎重

---

### 15.4.3 你项目的一期建议

我建议你采用一种折中方案：

> **同一类 stream feature 走统一执行框架，但不要一上来追求极致通用。**

更具体一点：

- count/sum/max/latest 走统一模板化执行器
- distinct\_count 可以先单独实现
- 不要一开始就搞完全抽象到任意 state schema 的执行引擎

原因很简单：

- 这样足够平台化
- 同时不会把自己拖入过重的抽象泥潭

---

## 15.5 为什么我强烈建议使用 bucket 化状态，而不是过度依赖 Window API

这一节很关键。

很多人做流式特征时，第一反应是：

- 5 分钟 count -> Sliding Window
- 30 分钟 sum -> Sliding Window
- 1 小时 max -> Sliding Window

这当然能做，Flink Window API 也很强。 但对“可动态配置的实时风控特征平台”来说，我更建议你优先采用：

> **bucket 化状态 + 自己控制窗口求值**

而不是把所有流式特征都直接建在 Window API 上。

---

### 15.5.1 为什么 Window API 不一定最适合平台化

Window API 的问题不是不能用，而是它更适合：

- 代码静态定义
- 算子拓扑相对固定
- 少量固定窗口逻辑

而你现在做的是：

- feature 想动态配置
- feature 可能会不断增加
- 多个 feature 想复用统一执行框架
- 希望引擎能根据快照动态切换执行计划

这时如果每个 feature 都映射成一个独立 Window 算子，平台会很难做。

---

### 15.5.2 bucket 化状态的核心思想

bucket 化本质上就是：

> **把时间窗口切成小桶，状态里维护这些桶的聚合值，计算结果时再按窗口范围把桶加起来。**

例如：

- 5 分钟窗口，按 1 分钟切桶
- 30 分钟窗口，按 1 分钟切桶
- 1 小时窗口，按 5 分钟切桶

然后状态里存：

```latex
bucketStartTs -> 聚合值
```

举例：

```latex
2026-03-07 10:00 -> 1
2026-03-07 10:01 -> 2
2026-03-07 10:02 -> 0
2026-03-07 10:03 -> 1
2026-03-07 10:04 -> 3
```

当前想算最近 5 分钟 count，只要把窗口范围内这些桶累加起来即可。

---

### 15.5.3 bucket 化状态的优势

#### 优势 1：非常适合动态特征平台

因为：

- 窗口大小、slide、aggType 可以由快照描述
- 执行器逻辑相对统一
- 不用为每个 feature 生成独立算子拓扑

#### 优势 2：状态可控、逻辑清晰

你知道每个 bucket 存了什么。 调试、解释、清理都更可控。

#### 优势 3：支持当前事件参与计算

对风控来说很常见：

- 当前这次交易也算进最近 5 分钟交易次数

bucket 方式实现这种“先更新再求值”的语义很直观。

#### 优势 4：更容易和统一 state 结构结合

你后面做通用模板执行器时，bucket 化会比 Window API 更容易统一抽象。

---

## 15.6 常见聚合类型分别怎么存状态

现在进入最核心的实现部分：

> 不同类型的流式特征，中间状态到底应该怎么存？

我按最常见的几类来讲：

- COUNT
- SUM
- MAX/MIN
- LATEST
- DISTINCT\_COUNT

---

## 15.6.1 COUNT

### 场景例子

- 用户 5 分钟交易次数
- IP 10 分钟注册次数
- 用户 10 分钟登录失败次数

### 推荐状态结构

```java
MapState<Long, Long> bucketCountState;
```

其中：

- key = bucketStartTs
- value = 该时间桶内的 count

### 更新方式

当一条事件到来时：

1. 判断是否满足 filterExpr
2. 算出它落在哪个 bucket
3. bucketCountState\[bucket] += 1

### 计算方式

取出窗口范围内所有 bucket，累加 count。

### 清理方式

- 注册清理 timer
- 超过窗口 + TTL 的桶删掉

### 特点

- 最简单
- 性能很好
- 非常适合一期先支持

---

## 15.6.2 SUM

### 场景例子

- 用户 30 分钟交易金额和
- 商户 1 小时支付金额和

### 推荐状态结构

```java
MapState<Long, BigDecimal> bucketSumState;
```

或者如果是 long 金额分单位：

```java
MapState<Long, Long> bucketSumState;
```

### 更新方式

1. 判断 filterExpr
2. 计算 valueExpr，例如 `amount`
3. 定位 bucket
4. bucketSumState\[bucket] += amount

### 计算方式

扫描窗口范围内所有 bucket，累加。

### 注意点

- 金额建议统一为分，用 long 存更稳
- 避免热路径频繁用 BigDecimal 做重计算

---

## 15.6.3 MAX / MIN

### 场景例子

- 用户 1 小时最大交易金额
- IP 最近 10 分钟最大注册频次（更少见）

### 推荐状态结构

```java
MapState<Long, Long> bucketMaxState;
```

或：

```java
MapState<Long, BigDecimal> bucketMaxState;
```

### 更新方式

当前事件进入后：

- 找当前 bucket
- 比较并更新该 bucket 的最大值

### 计算方式

扫描窗口范围内 bucket 取最大值。

### 注意点

MAX/MIN 这类特征不太适合用单 ValueState 直接存全窗口结果， 因为窗口滑出时旧最大值可能失效，需要重算。 用 bucket 化更稳妥。

---

## 15.6.4 LATEST

### 场景例子

- 最近一次登录时间
- 最近一次登录设备
- 最近一次失败原因

### 推荐状态结构

```java
ValueState<Object> latestValueState;
ValueState<Long> latestEventTimeState;
```

### 更新方式

如果当前事件的 eventTime 更新，则覆盖最新值。

### 清理方式

- 可以配 TTL
- 或当 entity 长时间不活跃时自然过期

### 特点

- 非常简单
- 很适合做最近一次状态类特征
- 一期强烈建议支持

---

## 15.6.5 DISTINCT\_COUNT

### 场景例子

- 设备 1 小时关联用户数
- IP 30 分钟关联手机号数
- 用户 1 天绑定设备数

这是风控非常常见、但实现相对复杂的一类。

### 一期建议：先做“小规模精确版”

推荐状态结构：

```java
MapState<String, Long> memberLastSeenState;
```

例如：

- key = userId
- value = lastSeenTs

### 更新方式

当前事件到来：

- 取 distinct member，例如 `userId`
- memberLastSeenState\[userId] = eventTs

### 计算方式

统计窗口内 lastSeenTs 仍未过期的 member 数量。

### 清理方式

- 定时器清理过期 member
- 或查询时惰性清理

### 问题

如果 distinct member 很大，这种精确方案状态会膨胀。

---

### 二期可考虑近似结构

后续可以扩展：

- Bloom Filter
- HyperLogLog
- RoaringBitmap

但对你一期项目来说，我建议：

> **先做精确但受控的 distinct\_count，配合合理场景和 TTL。**

足够展示能力，也更好解释。

---

## 15.7 Timer 和过期清理到底怎么设计

实时特征系统如果只会“加状态”，不会“清状态”，迟早会出问题。 所以你必须认真对待状态清理问题。

---

### 15.7.1 为什么必须清理

因为风控特征有很强的时间语义：

- 最近 5 分钟
- 最近 30 分钟
- 最近 1 小时

这意味着过期数据继续留在 state 里，不仅浪费空间，还会污染结果。

---

### 15.7.2 清理的两种常见方式

#### 方式 A：查询时惰性清理

即：

- 每次计算特征时顺带检查过期 bucket / member
- 发现过期就删除

优点：

- 实现简单

缺点：

- 长时间不活跃的 key 不会被及时清掉
- state 容量可能累积

---

#### 方式 B：Timer 主动清理

即：

- 每次更新状态时，注册一个未来清理时间点的 timer
- timer 触发时删除过期 bucket / member

优点：

- 清理更主动
- state 更可控

缺点：

- timer 管理复杂度更高

---

### 15.7.3 推荐实践：惰性清理 + 定时器清理结合

这是我更推荐的方案：

- 查询时做局部惰性清理
- 同时注册较粗粒度 timer 做兜底清理

这样可以兼顾：

- 实现复杂度
- 状态可控性
- 性能

---

### 15.7.4 timer 何时注册

一般可以在状态首次写入或更新时注册：

```latex
cleanupTs = currentBucketEnd + ttl
```

比如：

- 5 分钟窗口
- 额外保留 5 分钟 buffer

则某桶在窗口外后再过一段时间才清。

这样做更稳妥，避免过早删除导致边界问题。

---

## 15.8 State TTL 要不要开，怎么理解

Flink 提供了 State TTL 功能。 但很多人会误以为：

> 只要开 TTL，窗口问题就解决了。

不是这样的。

---

### 15.8.1 TTL 的本质

TTL 更像是：

> **一个状态生命周期兜底清理机制**

它适合防止：

- 长期无人访问的脏状态一直存在
- 某些 ValueState / MapState 永远不释放

---

### 15.8.2 TTL 不能替代窗口语义

例如：

- 最近 5 分钟 count

这不是“5 分钟后整块 state 过期”这么简单。 而是窗口内的不同 bucket / member 要按时间滚动滑出。

所以：

- 窗口边界清理要靠你自己的 bucket/timer 逻辑
- TTL 更适合做整体 state 的兜底回收

---

### 15.8.3 推荐用法

对于你的项目：

- bucket / member 的窗口清理由业务逻辑自己控制
- ValueState / MapState 可配一个相对宽松的 TTL 兜底

例如：

- 窗口 5 分钟
- ttl 可设 30 分钟或 1 小时

避免冷 key 永远残留。

---

## 15.9 哪些中间数据应该存在 Flink State，哪些应该存在 Redis

这是你后面实现中特别关键的边界判断。

一句话先说结论：

> **只要这个数据的存在是为了完成当前流式计算语义，而且强依赖时间/状态演化，就优先放 Flink State；**
> **只要这个数据更多用于在线查询共享、跨链路复用、热点快速 lookup，就更适合放 Redis。**

下面分开说。

---

## 15.9.1 适合放 Flink State 的数据

### 1）窗口桶状态

例如：

- 最近 5 分钟每分钟交易 count
- 最近 30 分钟每分钟交易 sum

这是典型的 Flink State。

---

### 2）distinct member 状态

例如：

- device -> userId 最近出现时间集合

这是完成 distinct\_count 计算的内部状态。

---

### 3）latest value 状态

例如：

- 最近一次登录设备
- 最近一次失败原因

---

### 4）event-time 相关定时器辅助状态

---

### 5）等待组装/等待超时的小型运行时上下文

如果你后面做复杂多步特征汇总，这些也应放在 Flink State。

---

## 15.9.2 适合放 Redis 的数据

### 1）名单

例如：

- 设备黑名单
- IP 黑名单
- 用户白名单

这类数据是：

- 外部可维护
- 引擎外也可能使用
- 热路径只需要 lookup

---

### 2）在线画像

例如：

- 用户风险等级
- 设备画像标签
- 商户评分等级

---

### 3）materialized feature

例如某些特征结果你想暴露给：

- 同步决策 API
- 其他系统复用
- 管理后台查看

这时可以把部分特征结果同步写 Redis。

但注意：

- Redis 中这份结果更像缓存或对外可见视图
- 真正的计算语义仍然以 Flink State 为准

---

### 4）热点辅助数据

例如：

- 常用字典
- 命中阈值配置副本（少量）

---

## 15.9.3 不建议放 Redis 的东西

### 1）正在演化的窗口中间态

例如：

- 每个用户的每分钟 bucket 状态

这些如果全放 Redis，会让：

- 网络开销变大
- 一致性复杂
- 性能抖动明显

---

### 2）引擎内部 timer / cleanup 语义相关状态

这本来就是 Flink 的强项，不该外移。

---

### 3）大规模临时中间上下文

这类状态不适合频繁外部 IO。

---

## 15.10 当前事件是否应该包含在当前特征结果里

这是实时风控里一个非常细、但非常关键的问题。

例如一条交易事件到来时：

- “最近 5 分钟交易次数”
- 到底要返回事件到来前的 count，还是包含当前这次后的 count？

这两个语义会直接影响规则结果。

---

### 15.10.1 两种常见语义

#### 语义 A：pre-update

先读取旧状态，再执行判断。 当前事件不计入当前结果。

#### 语义 B：post-update

先把当前事件写入状态，再计算当前结果。 当前事件算进当前结果。

---

### 15.10.2 风控中更常见的语义

对于很多风控场景，通常更合理的是：

> **post-update，即包含当前事件。**

例如：

- 用户当前正在发起第 3 次交易
- 规则是“5 分钟内交易次数 >= 3”

如果你不把当前这次算进去，那就会变成第 4 次才触发，语义往往不对。

---

### 15.10.3 平台层面的建议

在 feature 快照定义里，建议显式带上：

```latex
includeCurrentEvent = true / false
```

这样：

- 语义清晰
- 仿真和线上可保持一致
- 规则解释更明确

一期你完全可以先统一默认 `true`，后续再开放配置。

---

## 15.11 特征定义变更以后，旧 state 怎么办

这是流式特征平台真正有难度的一点。

很多人以为 feature 改了配置，发个新快照就行。 对 rule 或 policy 来说可能差不多； 但对 stream feature 来说，事情没有那么简单。

---

### 15.11.1 为什么 stream feature 变更更敏感

因为 stream feature 不只是表达式，它还对应：

- 状态结构
- 时间语义
- 清理逻辑
- 聚合方式
- 结果解释

如果你把：

- `user_trade_cnt_5m`

改成：

- `user_trade_cnt_10m`

看似只是窗口参数变了，实际上这已经是：

> **特征语义变了。**

旧 state 并不能天然无损转成新 state。

---

### 15.11.2 推荐原则：不要原地修改已上线 stream feature 的核心语义

特别是这些字段变动时要谨慎：

- aggType
- windowSize
- windowSlide
- entityKeyExpr
- valueExpr
- distinct member 维度

更推荐的做法是：

> **新增一个新 feature code 或新 version，让它从新版本开始 warm-up。**

例如：

- `user_trade_cnt_5m`
- `user_trade_cnt_10m_v2`

新规则引用新特征，旧特征逐步下线。

---

### 15.11.3 什么变更相对可接受

例如：

- description 改了
- 展示名称改了
- 非核心 hint 改了

这些不影响 state 语义。

---

## 15.12 Checkpoint 恢复时，流式特征状态会恢复成什么样

Flink 的一个巨大优势就是状态容错。 这一节你必须理解清楚。

---

### 15.12.1 Checkpoint 会保存什么

对于流式特征系统，checkpoint 通常会保存：

- bucketState
- latestValueState
- distinct member state
- timer 注册信息
- Broadcast State 中的 snapshot

所以当任务失败重启后：

- 特征中间状态会恢复
- 定时器会恢复
- 配置快照会恢复

这意味着：

> **引擎会从最近一次 checkpoint 的状态和配置继续运行。**

---

### 15.12.2 本地编译缓存怎么办

本地编译缓存一般是 transient 的，不直接进入 checkpoint。 恢复后需要：

- 从 Broadcast State 重新加载快照
- 重新编译表达式/Groovy
- 重建运行时缓存

这一点和流式特征状态是两回事。

---

### 15.12.3 恢复的一致性边界

如果 checkpoint 时：

- 状态和配置版本是一致的

恢复后：

- 逻辑就会恢复到那个版本点

这也是为什么“快照进入 Broadcast State”是正确做法。 因为状态恢复和配置恢复可以一起完成。

---

## 15.13 状态后端怎么选：HashMapStateBackend 还是 RocksDB

这是非常实际的问题。

---

### 15.13.1 HashMapStateBackend

优点：

- 本地开发方便
- 小状态性能好
- 调试体验好

缺点：

- 状态规模大时容易吃内存
- 对大 key / 大 MapState 不够稳

适合：

- 本地开发
- demo 环境
- 状态量较小场景

---

### 15.13.2 EmbeddedRocksDBStateBackend

优点：

- 更适合大状态
- MapState / distinct / 大量 bucket 更稳
- 更接近真实生产环境

缺点：

- 开发调试不如内存后端直观
- 序列化、磁盘 IO 成本更高

适合：

- 较真实环境
- 设备/用户规模更大
- distinct/count 状态较多

---

### 15.13.3 对你项目的建议

建议这样分阶段：

#### 本地开发阶段

- 先用 `HashMapStateBackend`
- 快速验证逻辑、调试方便

#### 集成验证 / 演示 / 压测阶段

- 切到 `EmbeddedRocksDBStateBackend`
- 更接近真实状态管理形态

这样最好。

---

## 15.14 状态大小控制与性能风险点

你做这个项目时，状态设计里最容易踩坑的地方，我给你提前指出来。

---

### 15.14.1 风险 1：distinct 状态膨胀

如果：

- 设备 -> userId 关联量很大
- TTL 又很长

MapState 很容易膨胀。

应对方式：

- 控制场景规模
- 限定窗口长度
- 加强清理
- 二期再考虑近似结构

---

### 15.14.2 风险 2：bucket 粒度过细

例如：

- 24 小时窗口还按秒切桶

这会导致 bucket 数量非常多。

建议：

- 根据窗口长度合理选 bucket 粒度
- 5m -> 1m 桶
- 1h -> 1m 或 5m 桶
- 24h -> 5m 或 10m 桶

---

### 15.14.3 风险 3：过度把状态外移到 Redis

热路径如果每来一条事件都大量 Redis IO：

- 延迟会明显上升
- 稳定性依赖网络和 Redis
- 语义一致性更难保证

所以要坚持边界：

- 强语义中间态放 Flink State
- 外部共享 lookup 放 Redis

---

### 15.14.4 风险 4：状态不清理

如果只增不删，状态一定会越来越大。 特别是 demo 项目一开始量小看不出问题，后面一压测就会暴露。

---

### 15.14.5 风险 5：BigDecimal 频繁重运算

金额类聚合建议：

- 用 long 存分
- 展示层再转 decimal

热路径里这样会更稳。

---

## 15.15 你一期项目最推荐的状态实现方案

结合你的项目目标、时间周期和求职展示需求，我建议你一期流式特征系统采用下面这个方案。

---

### 15.15.1 支持的特征类型

一期建议先支持：

- COUNT
- SUM
- MAX
- LATEST
- DISTINCT\_COUNT（精确小规模版）

这已经足够覆盖非常多的风控场景。

---

### 15.15.2 状态结构建议

#### COUNT / SUM / MAX

统一采用：

```java
MapState<Long, Long> / MapState<Long, DecimalLike>
```

按 bucket 维护。

#### LATEST

采用：

```java
ValueState<T>
ValueState<Long>
```

#### DISTINCT\_COUNT

采用：

```java
MapState<String, Long>
```

member -> lastSeenTs

---

### 15.15.3 清理策略建议

- 查询时惰性清理
- 定时器兜底清理
- 额外配置宽松 State TTL

---

### 15.15.4 状态后端建议

- 开发期：HashMapStateBackend
- 演示/压测期：EmbeddedRocksDBStateBackend

---

### 15.15.5 当前事件语义建议

一期统一采用：

```latex
includeCurrentEvent = true
```

这样更符合大多数风控规则的直觉。

---

### 15.15.6 不建议一期就做的东西

- 任意脚本定义状态结构
- 自定义窗口 DSL
- CEP 通用状态机平台化
- 大规模近似 distinct 组件化抽象
- 跨作业统一 Feature Store 级别能力

这些都可以后置。

---

## 15.16 一段通用流式特征执行器的伪代码

我给你一段比较贴近实现的伪代码，帮助你把前面这些概念串起来。

```java
public FeatureValue evaluateStreamFeature(StreamFeatureSpec spec, RiskEvent event) {
    if (!matchSourceEvent(spec, event)) {
        return FeatureValue.notApplicable(spec.getCode());
    }

    if (!evalFilter(spec.getFilterExpr(), event)) {
        return readCurrentValue(spec, event);
    }

    String entityKey = evalEntityKey(spec.getEntityKeyExpr(), event);
    long eventTs = event.getEventTime();
    long bucketTs = bucketStart(eventTs, spec.getBucketSize());

    switch (spec.getAggType()) {
        case COUNT:
            updateCountBucket(spec, entityKey, bucketTs, 1L);
            break;
        case SUM:
            long amount = evalLongValue(spec.getValueExpr(), event);
            updateSumBucket(spec, entityKey, bucketTs, amount);
            break;
        case MAX:
            long current = evalLongValue(spec.getValueExpr(), event);
            updateMaxBucket(spec, entityKey, bucketTs, current);
            break;
        case LATEST:
            updateLatestValue(spec, entityKey, eventTs, extractLatestValue(spec, event));
            break;
        case DISTINCT_COUNT:
            String member = evalMember(spec.getValueExpr(), event);
            updateDistinctMember(spec, entityKey, member, eventTs);
            break;
        default:
            throw new IllegalStateException("unsupported aggType");
    }

    registerCleanupTimerIfNeeded(spec, entityKey, bucketTs, eventTs);
    cleanupExpiredIfNeeded(spec, entityKey, eventTs);

    return calculateCurrentFeatureValue(spec, entityKey, eventTs);
}
```

这段伪代码虽然简化了很多细节，但已经表达出几个关键点：

- feature 执行是模板化的
- 不同 aggType 共用一套执行框架
- state 是内部实现，不暴露给平台用户
- filter、entityKey、valueExpr 可以动态配置
- timer 和 cleanup 是执行器的一部分
- 最终输出的是当前特征值

---

## 15.17 本章小结

这一章我们真正进入了 Flink 流式特征实现的核心内部。

把重点收一下：

### 1）流式特征本质上是状态演化结果

不是普通字段，也不是单纯表达式结果。

### 2）状态组织的核心单位是 feature 在某个 entity 上的实例

逻辑主键可以理解为：

```latex
(sceneCode, featureCode, entityKey)
```

### 3）bucket 化状态非常适合动态化风控特征平台

它比大量独立 Window API 更适合平台化统一执行。

### 4）不同聚合类型有不同推荐状态结构

- COUNT / SUM / MAX：MapState(bucket -> value)
- LATEST：ValueState
- DISTINCT\_COUNT：MapState(member -> lastSeenTs)

### 5）窗口清理不能只靠 TTL

TTL 是兜底机制，窗口语义清理仍要靠 bucket/timer 逻辑。

### 6）Flink State 和 Redis 的边界要清晰

- Flink State：强语义中间状态
- Redis：名单、画像、热点 lookup 与外部共享结果

### 7）stream feature 的语义变更要非常谨慎

核心参数变化时，不建议原地改，建议新 feature code/version + warm-up。

### 8）一期项目建议采用“模板化执行器 + bucket 状态 + 有限聚合类型支持”

这是最稳、最像平台、也最适合你当前节奏的方案。

---

## 15.18 下一章会讲什么

下一章我们进入：

第 16 章：规则引擎与策略引擎在 Flink 中如何执行

这一章会重点回答：

- Rule 和 Policy 在 Flink 中的执行边界是什么
- FIRST\_HIT 和 SCORE\_CARD 怎么实现
- 规则执行顺序如何控制
- 命中结果如何汇总
- hit reason 如何生成
- 为什么规则执行必须建立在完整 EvalContext 上
- 如何保证规则执行结果可解释、可追溯

也就是说，下一章会从“特征怎么来”进一步走到：

> **特征准备好之后，规则和策略到底怎么在 Flink 里真正跑起来。**
