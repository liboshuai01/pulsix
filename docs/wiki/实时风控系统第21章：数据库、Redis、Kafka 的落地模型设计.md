## 21.1 这一章解决什么问题

前面第 20 章我们已经把 Spring Boot 控制平台的模块边界、后端分层、发布中心、仿真测试、控制面工程结构这些事情讲清楚了。接下来就要进入一个非常务实、也非常容易决定系统成败的问题：

> 这套实时风控平台里的数据库、Redis、Kafka，到底应该怎么落地设计？

这个问题看起来像“存储细节”，但实际上它并不是后期优化问题，而是系统骨架的一部分。因为你这类系统和普通 CRUD 最大的不同在于：

- 它同时有**设计态数据**和**运行态数据**
- 它同时有**热路径数据**和**分析路径数据**
- 它同时有**强结构化关系数据**和**高并发在线访问数据**
- 它同时有**流式消息**和**查询型状态**

所以如果数据库、Redis、Kafka 的职责边界没划清楚，后面很容易出现这些问题：

1. MySQL 承担了热路径实时查询，延迟和连接数都顶不住
2. Redis 被当成万能数据库，结果结构混乱、维护困难
3. Kafka 里什么都放，Topic 没有语义边界
4. Flink State、Redis、MySQL 三者之间的数据职责重叠
5. 日志、决策、配置流混在一起，后面扩展困难
6. 外部系统根本不知道该消费哪个 Topic，或者该查哪个存储

所以这一章的目标，就是把下面这些事情一次性讲清楚：

- MySQL 到底该存什么，不该存什么
- Redis 到底该存什么，不该存什么
- Kafka 的 Topic 应该怎么规划
- 设计态数据、运行态数据、日志数据、名单画像数据分别怎么落地
- 各个存储之间的边界是什么
- 你的一期项目应该选择怎样的落地方案，既专业又不会过重

一句话概括：

> 这一章解决的是“这套平台的数据到底落在哪、怎么流、为什么这样分工”。

---

## 21.2 先建立一个总原则：不要按技术习惯分存储，要按数据职责分存储

很多人做系统时，会下意识按技术习惯来分配存储：

- 配置存 MySQL
- 缓存存 Redis
- 消息走 Kafka

这当然没错，但还不够。对于实时风控平台来说，更准确的分法应该是：

> **按数据职责来分，而不是按中间件标签来分。**

你需要先问清楚一条数据属于哪一种：

- 它是设计态配置，还是运行态上下文？
- 它是高频读写的热路径状态，还是低频查询的管理数据？
- 它是事件流的一部分，还是结构化主数据？
- 它需要强一致版本追溯，还是只需要低延迟查询？
- 它是短期中间状态，还是长期存档？

回答完这些问题以后，存储归属通常就很清楚了。

所以你可以先记住这条分工铁律：

### MySQL

负责：

- 设计态配置
- 管理态数据
- 版本与发布记录
- 审计记录
- 中低频查询日志

### Redis

负责：

- 在线名单查询
- 在线画像查询
- 热点特征缓存
- 低延迟 lookup 数据

### Kafka

负责：

- 事件流转
- 配置快照广播
- 决策结果流
- 日志异步流

### Flink State

负责：

- 流式特征的中间状态
- 定时器相关状态
- 广播快照
- 短时运行态上下文

这四者不能相互替代。它们协作，但边界必须清楚。

---

## 21.3 MySQL 的落地模型：它是“设计态与管理态主存储”

先讲 MySQL。对于你的项目来说，MySQL 的角色非常明确：

> **MySQL 主要承担设计态配置、发布版本、管理数据和中低频查询数据。**

它不是热路径实时计算存储，不是特征计算引擎，也不是毫秒级 lookup 的主战场。

### 21.3.1 MySQL 最适合存哪些数据

我建议你把 MySQL 中的数据分成 5 类：

1. 设计态元数据
2. 发布态版本数据
3. 平台管理数据
4. 仿真测试数据
5. 中低频查询日志

下面分别展开。

---

### 21.3.2 设计态元数据

这部分就是控制平台最核心的元数据，上一章我们已经讲过大致表结构。这里再从“落地职责”的角度归纳一次。

建议 MySQL 存放：

- `scene_def`
- `event_schema`
- `event_field_def`
- `access_source_def`
- `event_access_binding`
- `event_access_raw_field_def`
- `event_access_mapping_rule`
- `feature_def`
- `feature_stream_conf`
- `feature_lookup_conf`
- `feature_derived_conf`
- `list_set`
- `list_item`
- `rule_def`
- `policy_def`
- `policy_rule_ref`

这些表的共同特点是：

- 结构化
- 可编辑
- 需要页面管理
- 需要做校验
- 需要做版本编译
- 需要做差异比对

这类数据非常适合 MySQL。

---

### 21.3.3 发布态版本数据

你一定要把“设计态”和“发布态”分开，这一点在数据库层也要体现出来。

建议 MySQL 存放：

- `scene_release`
- 可选 `release_task`

其中 `scene_release` 是最关键的：

- `scene_code`
- `version_no`
- `snapshot_json`
- `checksum`
- `publish_status`
- `published_by`
- `published_at`
- `effective_from`
- `rollback_from_version`
- `remark`

这张表的本质不是“普通配置表”，而是：

> **运行态版本产物仓库**

它是：

- Flink 配置广播的来源
- 历史版本追溯的依据
- 回滚的依据
- 仿真指定版本执行的依据

所以从系统重要性上说，这张表是 MySQL 里最关键的表之一。

---

### 21.3.4 平台管理数据

这部分比较常规，但平台一定要有。

建议 MySQL 存放：

- `sys_user`
- `sys_role`
- `sys_user_role`
- `sys_menu`
- `sys_audit_log`

如果后面你还想加：

- 数据权限
- 发布审批
- 操作留痕

这些也都适合在 MySQL 中做结构化管理。

---

### 21.3.5 仿真测试数据

仿真和回归测试是平台能力的一部分，建议同样放在 MySQL。

例如：

- `simulation_case`
- `simulation_report`

这些数据量通常不大，但非常重要。因为它们能支持：

- 回归测试
- 规则调整效果验证
- 面试演示样例
- 历史版本行为比对

---

### 21.3.6 中低频查询日志

对于你的项目一期，MySQL 也可以承担一部分日志查询职责，尤其是在数据量还不大时。

建议先支持：

- `decision_log`
- `rule_hit_log`

但这里要注意边界：

> **MySQL 可以作为日志查询落地库，但不应该成为热路径同步写入的重型依赖。**

正确做法通常是：

- Flink 将决策结果 / 日志先发 Kafka
- 异步 consumer 或 sink 再写 MySQL

这样 MySQL 是日志查询终点，而不是热路径的中间阻塞点。

---

## 21.4 MySQL 不该承担什么职责

明白 MySQL 能干什么之后，更重要的是知道它不能干什么。

### 21.4.1 不该承担毫秒级热路径 lookup

比如这些查询，不适合每条事件都走 MySQL：

- 设备是否命中黑名单
- 用户当前风险等级
- 当前用户某个热标签值
- 短时热点特征值

因为这类查询：

- 频率高
- 并发高
- 对延迟敏感

应该交给 Redis 或 Flink State。

---

### 21.4.2 不该承担实时流式特征中间状态

例如：

- 用户 5 分钟交易次数桶
- 设备 1 小时关联账号集合
- 定时器状态

这些应该存在 Flink State，而不是 MySQL。

---

### 21.4.3 不该承担配置流热切换

虽然配置最终落在 MySQL `scene_release` 中，但 Flink 不应该每条事件实时去查 MySQL 当前版本。

正确方式是：

- MySQL 存版本产物
- 配置仅通过 MySQL CDC 进入 Flink，这是当前系统唯一的配置同步链路
- Flink 存 Broadcast State

---

### 21.4.4 不该承担高吞吐海量日志的长期分析主库

如果后期量很大，MySQL 做全量日志分析会比较吃力。你的项目一期可以用 MySQL 先扛住，但二期以后更适合考虑：

- Doris
- ClickHouse

不过你一期先不必太重，先把链路打通更重要。

---

## 21.5 Redis 的落地模型：它是“在线查询和辅助上下文层”

接下来讲 Redis。

很多项目把 Redis 当万能工具，用来：

- 存 session
- 存缓存
- 存计数器
- 存锁

但在你的实时风控平台里，Redis 的角色应该更加明确：

> **Redis 是在线名单、画像和热点辅助数据的低延迟访问层。**

它的核心价值不在“缓存一切”，而在于：

- 低延迟
- 高并发
- 适合 key-value / set / hash 查询
- 非常适合做 rule/feature 的 lookup 支撑

---

### 21.5.1 Redis 最适合存哪些数据

我建议 Redis 承载下面 4 类内容：

1. 名单数据
2. 在线画像数据
3. 热点特征缓存
4. 轻量辅助运行态数据

---

### 21.5.2 名单数据

这是 Redis 最天然的使用场景之一。

典型有：

- 设备黑名单
- IP 黑名单
- 用户白名单
- 代理 IP 名单
- 风险手机号名单

#### 推荐 key 设计方式 1：精确单值命中

适合单项判断。

例如：

- `list:black:device:D9001 -> 1`
- `list:black:ip:1.2.3.4 -> 1`
- `list:white:user:U1001 -> 1`

优点：

- 查询简单
- 语义清晰
- TTL 好控制

#### 推荐 key 设计方式 2：Set 结构

例如：

- `list:black:device` 是一个 Set
- `SISMEMBER list:black:device D9001`

优点：

- 批量管理方便

缺点：

- 大 key 风险要注意
- TTL 粒度较粗

对于你项目，一期建议优先使用**单值 key 模式**，因为：

- 语义清晰
- 更容易做过期时间
- 便于单条管理

---

### 21.5.3 在线画像数据

画像是风控上下文的重要组成部分。很多画像不是 Flink 当场算出来的，而是：

- 离线沉淀
- 外部同步
- 预先入库

典型例如：

- 用户风险等级
- 用户历史标签
- 设备风险评分
- 商户风险等级
- 手机号归属地风险

#### 推荐 key 设计

例如：

- `profile:user:risk:U1001 -> H`
- `profile:device:score:D9001 -> 87`
- `profile:user:tags:U1001 -> hash`

如果画像字段较少，建议每个 key 单值保存；
如果画像字段较多，可以用 Hash：

- `profile:user:U1001 -> {riskLevel:H, vipFlag:0, refundRate:0.13}`

一期建议：

- 简单画像用 String
- 多属性画像用 Hash

---

### 21.5.4 热点特征缓存

有些特征虽然本质上来自 Flink 计算，但为了：

- 给外部同步 API 复用
- 提供更快 lookup
- 跨链路共享

可以 materialize 到 Redis。

例如：

- `feature:user_trade_cnt_5m:U1001 -> 3`
- `feature:user_trade_amt_sum_30m:U1001 -> 18800`
- `feature:device_bind_user_cnt_1h:D9001 -> 4`

但这里一定要注意：

> **Redis 中的特征缓存不是 Flink State 的替代品，而是外部化副本或辅助访问层。**

也就是说：

- 真正实时一致的中间态还在 Flink State
- Redis 是为了共享和快速查询而存在

---

### 21.5.5 轻量辅助运行态数据

例如：

- 小型字典
- 轻量级映射
- 稳定维表缓存
- 本地无法长期保存但需要热访问的小对象

这类数据可以适当放 Redis，但不要把 Redis 变成“杂物间”。

---

## 21.6 Redis 不该承担什么职责

### 21.6.1 不该承担设计态主数据

例如：

- rule\_def
- feature\_def
- policy\_def
- scene\_release

这些都不应该把 Redis 当主存储。因为：

- 缺少关系建模能力
- 不利于审计
- 不利于版本管理
- 不利于页面管理

---

### 21.6.2 不该承担复杂审计和查询职责

Redis 不适合用来做：

- 规则历史对比
- 发布记录查询
- 审计日志查询
- 配置 diff

这些更适合 MySQL。

---

### 21.6.3 不该承担长期日志主存储

决策日志、命中日志可以短期缓存，但长期主存储还是应落在 MySQL / Doris 等。

---

### 21.6.4 不该替代 Flink State

这是特别关键的一条。

你不能把 Flink 中本应由状态管理的中间数据，全都搬去 Redis，再靠读写 Redis 来模拟流式状态。这会带来：

- 网络 IO 增加
- 一致性复杂
- 热点问题
- 更难保障低延迟

所以：

- 流式中间态 -> Flink State
- 外部 lookup 数据 -> Redis

这个边界一定要稳住。

---

## 21.7 Kafka 的落地模型：它是“流转总线”

下面讲 Kafka。

在实时风控系统里，Kafka 绝不只是“消息队列”。它承担的是系统各层之间的**流式总线**角色。

你可以把 Kafka 理解为：

> **所有需要异步传播、解耦处理、顺序消费、扩展下游的流动数据，都优先通过 Kafka 来承载。**

对于你的项目，一期建议 Kafka 至少承载下面 3 类主流：

1. 原始事件流
2. 决策结果流
3. 日志流

配置快照不纳入 Kafka Topic 规划。当前系统让 Flink 直接通过 MySQL CDC 读取发布表，这是唯一的配置同步链路。

---

### 21.7.1 原始事件流 Topic

结合当前接入层方案，一期更建议统一一个主标准事件 Topic 即可，减少复杂度。

例如：

- `pulsix.event.standard`

事件体中包含：

- `sceneCode`
- `eventCode`
- `eventId`
- `eventTime`
- 业务字段

补充一个一期约束：

- 上游请求仍显式携带标准 `eventCode`
- `pulsix-ingest` 按 `sourceCode + eventCode` 装载唯一接入映射，再完成标准化

#### 为什么一期建议统一 Topic

因为你的项目阶段更重要的是：

- 降低 Topic 管理复杂度
- 降低消费者管理复杂度
- 强化统一事件模型

后面如果量大，再考虑按场景拆：

- `pulsix.event.login`
- `pulsix.event.register`
- `pulsix.event.trade`

---

### 21.7.2 配置快照下发链路（仅 MySQL CDC）

先说明当前系统边界：**MySQL 配置同步到 Flink 的唯一方式，就是 Flink CDC 直接读取 `scene_release` 等发布表。**

配置快照从 MySQL 快照 + binlog 直接进入 Flink，然后广播到各并行子任务。

因此当前系统：

- 不保留 `pulsix.config.snapshot`
- 不通过 Kafka 传配置
- 不通过主动推送传配置

控制面和计算面之间关于配置同步的关键桥梁只有一条：

- `scene_release -> MySQL CDC -> Flink`

#### 消息体建议包含

- sceneCode
- version
- checksum
- effectiveFrom
- operationType（PUBLISH / ROLLBACK）
- snapshotJson

---

### 21.7.3 决策结果流 Topic

建议：

- `pulsix.decision.result`

承载较轻的最终决策结果，供下游消费。

建议包含：

- traceId
- eventId
- sceneCode
- finalAction
- finalScore
- version
- hitRuleCodes
- eventTime
- decisionTime

这条 Topic 更多是“业务下游可消费的结果流”。

---

### 21.7.4 决策日志流 Topic

建议：

- `pulsix.decision.log`

这条 Topic 可以更详细，承载：

- 输入事件快照
- featureSnapshot
- hitRulesDetail
- latencyMs
- errorInfo
- engineNodeInfo

它的主要用途是：

- 异步落日志库
- 做问题排查
- 做分析与回放基础

建议把 `decision.result` 和 `decision.log` 分开，原因是：

- 结果流更轻，适合下游业务消费
- 日志流更重，适合分析侧消费

---

### 21.7.5 异常 / 死信 Topic

建议你一定预留：

- `pulsix.event.dlq`
- `pulsix.engine.error`

分别接住：

- 反序列化失败的原始事件
- 不合法事件
- 字段映射 / 标准化失败事件
- 配置异常
- 运行时不可恢复错误

这样做的价值很大：

- 便于排障
- 便于追查脏数据
- 便于做测试和回放

---

## 21.8 Kafka Topic 的规划原则

Topic 规划千万不要随意。建议遵循下面几个原则。

### 原则 1：按语义分 Topic，不按“顺手”分 Topic

比如不要把：

- 原始事件
- 配置快照
- 决策日志

都塞进一个 Topic 再靠 type 字段区分。

这样虽然省 Topic，但会让消费语义很乱。

---

### 原则 2：轻结果流和重日志流分开

业务消费方通常不需要拿到巨大的 featureSnapshot 和详细 hit detail。

所以：

- `decision.result` 轻量
- `decision.log` 详细

这是非常好的分法。

---

### 原则 3：给错误留出口

任何成熟系统都应该有异常流和死信流，不要只在日志里打印错误然后就没了。

---

### 原则 4：命名统一、前缀统一

建议统一前缀：

- `pulsix.event.*`
- `pulsix.config.*`
- `pulsix.decision.*`
- `pulsix.engine.*`

这样后面维护和监控都会舒服很多。

---

## 21.9 决策日志应该怎么落地：MySQL 先行，后续可扩 Doris/ClickHouse

这部分很多人容易纠结：

- 日志要不要直接上 Doris？
- 要不要一开始就上 ClickHouse？

我的建议很明确：

> **一期先让日志链路成立，再追求分析型数据库。**

对于你当前项目目标，一期可采用：

- Flink -> Kafka `pulsix.decision.log`
- 独立消费者/简化 sink -> MySQL `decision_log` / `rule_hit_log`

这样你能快速做出：

- 日志查询页面
- traceId/eventId 排查
- 命中规则展示
- 版本追溯

如果后面日志量上来，再增加：

- Kafka -> Doris
- Kafka -> ClickHouse

做报表分析即可。

这叫：

> **先跑通闭环，再做分析增强。**

非常适合个人项目。

---

## 21.10 MySQL、Redis、Kafka 与 Flink State 的协作边界

这一节非常关键，我建议你记成一张“职责图”。

### MySQL

存：

- 设计态配置
- 发布版本
- 仿真数据
- 管理与审计
- 中低频查询日志

### Redis

存：

- 名单
- 画像
- 热点特征副本
- lookup 数据

### Kafka

传：

- 原始事件
- 配置快照
- 决策结果
- 日志与错误

### Flink State

存：

- 流式特征中间状态
- timer 状态
- broadcast snapshot
- 短期运行态上下文

你后面任何设计，如果发现某条数据不知道该放哪，就先用这张图判断。

---

## 21.11 一期项目的推荐最小落地方案

如果结合你当前目标，我建议你采用下面这个最务实的落地方式。

### 21.11.1 MySQL

先用一个 MySQL 实例，包含：

- 设计态配置表
- 发布表
- 用户权限表
- 仿真表
- 决策日志表

### 21.11.2 Redis

先用一个 Redis，包含：

- 名单数据
- 画像数据
- 少量热点特征数据

### 21.11.3 Kafka

先规划 4 类必需 Topic，外加 1 类可选 Topic：

- `pulsix.event.standard`
- `pulsix.decision.result`
- `pulsix.decision.log`
- `pulsix.event.dlq`
- `pulsix.ingest.error`（可选）

### 21.11.4 Flink

- 事件消费主链路
- 快照广播流
- State 计算流式特征
- Redis lookup
- 结果输出

### 21.11.5 日志落地

- 一期先落 MySQL
- 二期再扩 Doris / ClickHouse

这套方案既不会太轻，也不会把你拖进过度复杂的基础设施里。

---

## 21.12 Redis Key 设计建议

为了让你后面更容易落地，我再给你一版建议命名规范。

### 名单类

- `list:black:device:{deviceId}`
- `list:black:ip:{ip}`
- `list:white:user:{userId}`

### 画像类

- `profile:user:risk:{userId}`
- `profile:user:{userId}`
- `profile:device:{deviceId}`

### 特征类

- `feature:{featureCode}:{entityKey}`
- 例如： `feature:user_trade_cnt_5m:U1001`

### 字典/辅助类

- `dict:geo:ip:{ip}`
- `dict:merchant:risk:{merchantId}`

命名原则：

- 前缀分层
- 语义清晰
- 保持 scene 无关或低耦合，尽量让 featureCode 自带业务含义

---

## 21.13 Kafka 消息体设计建议

你后面在实现时，建议统一消息 Envelope 结构。

### 原始事件消息建议

```json
{
  "traceId": "T10001",
  "eventId": "E10001",
  "sceneCode": "TRADE_RISK",
  "eventCode": "TRADE_EVENT",
  "eventTime": "2026-03-07T10:00:00",
  "payload": {
    "userId": "U1001",
    "deviceId": "D9001",
    "ip": "1.2.3.4",
    "amount": 6800,
    "result": "SUCCESS"
  }
}
```

### 快照消息建议

```json
{
  "sceneCode": "TRADE_RISK",
  "version": 12,
  "operationType": "PUBLISH",
  "effectiveFrom": "2026-03-07T20:00:10",
  "checksum": "9d8c1a...",
  "snapshot": { ... }
}
```

### 决策结果消息建议

```json
{
  "traceId": "T10001",
  "eventId": "E10001",
  "sceneCode": "TRADE_RISK",
  "version": 12,
  "finalAction": "REJECT",
  "finalScore": 100,
  "hitRuleCodes": ["R001", "R003"],
  "decisionTime": "2026-03-07T10:00:01",
  "latencyMs": 37
}
```

这样后面各个消费者和排查工具都会更容易统一。

---

## 21.14 一个非常重要的判断标准：热路径尽量少碰 MySQL，多用 Flink State + Redis + Kafka

如果你后面在开发中拿不准某条数据到底该怎么处理，我建议你先问自己一个问题：

> **它是不是热路径上每条事件都要访问的？**

如果是，那么优先顺序通常应该是：

1. 先看能不能在 Flink State 内解决
2. 不能的话看能不能 Redis lookup
3. 结果输出走 Kafka 异步化
4. 尽量不要同步查/写 MySQL

而如果它属于：

- 页面配置
- 审计记录
- 版本查询
- 仿真结果
- 日志回看

那就更适合落在 MySQL。

这条判断规则非常实用。

---

## 21.15 本章小结

这一章本质上是在回答：

> **实时风控平台里的数据库、Redis、Kafka，到底该怎么各司其职。**

我们把重点收一下。

### 1）MySQL 是设计态与管理态主存储

适合存：

- scene / feature / rule / policy 等元数据
- 发布版本与快照
- 仿真数据
- 用户权限与审计
- 中低频日志查询数据

### 2）Redis 是在线 lookup 支撑层

适合存：

- 黑白名单
- 在线画像
- 热点特征副本
- 轻量辅助数据

### 3）Kafka 是系统流转总线

建议至少有：

- `pulsix.event.standard`
- `pulsix.decision.result`
- `pulsix.decision.log`
- `pulsix.event.dlq`
- `pulsix.ingest.error`（可选）

### 4）Flink State 是流式中间态主场

适合存：

- 窗口/桶状态
- 定时器状态
- 广播快照
- 短期上下文

### 5）热路径与查询路径要坚决分离

- 热路径少碰 MySQL
- 结果和日志尽量走 Kafka 异步落地

### 6）一期项目推荐“够用且专业”的方案

- MySQL 扛设计态和日志查询
- Redis 扛名单与画像
- Kafka 扛事件、结果、日志流；配置快照仅通过 MySQL CDC 直连 Flink
- Flink 扛状态计算和执行

---

## 21.16 下一章会讲什么

下一章我们进入：

## 第 22 章：项目代码结构设计与从 0 到 1 的落地顺序

这一章会重点回答：

- 仓库应该怎么拆
- `pulsix-server / pulsix-module-system / pulsix-module-infra / pulsix-module-risk / pulsix-engine / pulsix-ui / pulsix-framework/pulsix-common / pulsix-framework/pulsix-kernel / pulsix-spring-boot-starter-*` 等模块应该如何组织
- 代码目录应该长什么样
- 一个阶段一个阶段应该先写什么
- 哪些接口和领域对象应该先稳定下来
- 一个人做这个项目，最合理的编码推进顺序是什么

也就是说，接下来我们会从“存储和消息的落地模型”，进一步进入：

> **这套系统到底应该怎么开始写代码，先写哪块，后写哪块。**
