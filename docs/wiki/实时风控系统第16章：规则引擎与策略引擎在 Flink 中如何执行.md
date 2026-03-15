## 16.1 这一章解决什么问题

前面几章，我们已经把很多关键基础讲清楚了：

- 控制平台如何管理设计态配置
- 为什么要发布成运行时快照
- Flink 如何接收、持有并切换快照
- 实时特征系统如何分类
- 流式特征的状态如何设计

到这里，系统已经具备了一个很重要的前提：

> 一条事件进入 Flink 后，平台已经能够拿到当前场景的运行时快照，也能够拿到规则执行所需的基础字段、流式特征、名单命中结果、画像值以及派生特征。

接下来就进入真正的“决策时刻”了：

> **规则到底怎么在 Flink 里执行？策略又是怎么把多条规则的结果收敛成一个最终决策的？**

这就是本章要解决的问题。

很多人一开始会把“规则执行”和“策略执行”混成一件事，但实际上它们在职责上是不同层次的两件事：

- **规则引擎**负责：在当前上下文上判断某一条规则是否命中
- **策略引擎**负责：把多条规则的命中结果组织、排序、加权、收敛成最终动作

如果这两个概念混在一起，系统很容易出现以下问题：

- 单条规则承担了过多职责，既判断条件，又决定最终结果，又控制执行流程
- 规则之间无法复用
- 无法支持不同策略模式，例如 FIRST\_HIT 和 SCORE\_CARD
- 命中原因、总分、最终动作难以解释
- 后续要扩展动作优先级、灰度、评分卡时结构会非常别扭

所以这一章的目标，就是把下面这些问题讲透：

1. 规则引擎与策略引擎分别负责什么
2. 一条事件进入 Flink 后，规则执行所需的输入上下文到底是什么
3. 规则执行结果应该长什么样
4. 多条规则执行后，策略如何收敛成最终结果
5. FIRST\_HIT、SCORE\_CARD 这些模式在工程上怎么落地
6. 命中原因、特征快照、日志追溯如何保留
7. 在 Flink 热路径里，规则执行怎样做到性能可控、结果可解释、版本可追溯

简单说：

> **这一章要回答的是：Flink 里“最后那一下决策”到底是怎么完成的。**

---

## 16.2 先建立一个最重要的认知：规则不等于最终决策

这是整章最核心的前提。

### 16.2.1 规则是什么

规则本质上是：

> **针对当前上下文的一次条件判断。**

例如：

- `device_in_blacklist == true`
- `amount >= 5000 && user_trade_cnt_5m >= 3`
- `device_bind_user_cnt_1h >= 4 && user_risk_level in ['M','H']`

规则的核心输出通常只有：

- 命中 / 不命中
- 命中后的建议动作
- 命中分数
- 命中原因

它表达的是：

> “这条规则怎么看当前这条事件？”

而不是：

> “系统最终应该怎么处理这条事件？”

---

### 16.2.2 策略是什么

策略本质上是：

> **对多条规则执行结果的组织、排序、过滤与收敛。**

比如系统里有 3 条规则：

- R001：设备命中黑名单 -> REJECT
- R002：高频高额交易 -> REVIEW
- R003：高风险用户且多账号设备 -> REJECT

当一条事件来时，可能出现：

- 只命中 R002
- 同时命中 R002 和 R003
- 三条都没命中

那么最终返回什么结果，不是规则本身能单独决定的，而是由策略决定：

- 是否按优先级第一条命中就返回
- 是否要累计总分
- 是否要按动作优先级收敛
- 是否允许同时命中多条规则
- 是否需要输出所有命中规则，还是只输出首条

所以：

> **规则解决“单点判断”，策略解决“整体收敛”。**

---

### 16.2.3 为什么必须把两者分开

分开以后，系统会获得几个很大的好处：

#### 1）规则更容易复用

同一条规则可以被多个策略复用。

#### 2）规则职责更单一

规则只关心条件是否成立，不关心整套决策流如何组织。

#### 3）策略模式更容易扩展

你可以先做 FIRST\_HIT，后面再扩 SCORE\_CARD，而不需要重写所有规则。

#### 4）结果更容易解释

日志里可以清楚区分：

- 哪些规则命中
- 为什么命中
- 最终策略是如何得到当前动作的

#### 5）代码结构更稳定

规则执行内核和策略收敛内核可以独立抽象，后续扩展也更自然。

所以后面你在代码里最好也坚持这个边界：

- `RuleEngine`：执行单条规则
- `PolicyEngine`：执行整套策略

---

## 16.3 Flink 决策执行在整个链路中的位置

在进入具体实现前，我们先明确规则/策略执行在 Flink 链路中的位置。

一条事件在引擎里通常经历下面几个阶段：

1. 读取事件
2. 找到当前场景的运行时快照
3. 构建基础上下文
4. 计算流式特征
5. 查询名单 / 画像 / 外部 lookup 特征
6. 计算派生特征
7. **执行规则**
8. **执行策略收敛**
9. 生成最终决策结果
10. 输出日志与指标

所以规则和策略执行不是孤立的一步，它是站在“上下文已经准备好”的基础之上的。

这意味着你在系统设计上要坚持一个原则：

> **规则引擎不负责构建上下文，只负责消费上下文。**

这条原则非常重要。

否则你会很容易把：

- Redis 查询
- 特征计算
- 变量默认值补齐
- 派生特征构建

这些本属于上下文准备的责任，错误地塞进规则执行器里。

正确边界应该是：

- `ContextBuilder` / `FeatureAssembler`：准备上下文
- `RuleEngine`：读取上下文执行规则
- `PolicyEngine`：对规则结果做收敛

---

## 16.4 规则执行前必须具备的输入：EvalContext

如果你想把规则执行做成平台化、可解释、可热更新，最关键的数据结构之一就是：

> **EvalContext（评估上下文）**

它代表：

> **当前这条事件在本次决策时刻，所有可供规则访问的变量集合。**

你可以把它理解为规则引擎看到的“世界快照”。

---

### 16.4.1 EvalContext 里通常应该包含什么

建议至少包含下面几类数据：

#### 一、事件基础字段

例如：

- `eventId`
- `traceId`
- `sceneCode`
- `eventType`
- `eventTime`
- `userId`
- `deviceId`
- `ip`
- `amount`
- `channel`

#### 二、流式特征值

例如：

- `user_trade_cnt_5m`
- `user_trade_amt_sum_30m`
- `device_bind_user_cnt_1h`

#### 三、lookup 特征

例如：

- `device_in_blacklist`
- `user_risk_level`
- `ip_proxy_flag`

#### 四、派生特征

例如：

- `high_amt_flag`
- `trade_burst_flag`

#### 五、运行元信息

例如：

- `snapshotVersion`
- `policyCode`
- `processTime`
- `eventLagMs`

这些元信息不一定都暴露给规则，但通常对日志和调试很有帮助。

---

### 16.4.2 EvalContext 的工程设计建议

你不要把 EvalContext 简单设计成一个裸 `Map<String, Object>` 然后哪里都乱塞。 更推荐的做法是：

- 外层是一个领域对象 `EvalContext`
- 内部保留一份变量 Map 供表达式 / Groovy 使用
- 同时保留结构化元信息字段，方便日志、监控、调试

例如可以是下面这种思路：

```java
public class EvalContext {
    private String traceId;
    private String eventId;
    private String sceneCode;
    private String eventType;
    private Long eventTime;
    private Long processTime;
    private Long snapshotVersion;

    private Map<String, Object> vars;
    private Map<String, Object> debugMeta;
}
```

这样做好处是：

- 表达式引擎拿 `vars`
- 日志与链路追踪拿结构化字段
- 调试信息放 `debugMeta`
- 后续可以继续扩展而不破坏执行器接口

---

### 16.4.3 EvalContext 的构建原则

#### 原则 1：上下文一旦进入规则执行阶段，应尽量视为只读

避免脚本执行过程中到处修改上下文，破坏结果可预测性。

#### 原则 2：上下文里的变量命名必须稳定

否则控制平台和 Flink 引擎会很难对齐。

#### 原则 3：上下文中变量应尽量是“已经准备好的最终值”

不要让规则里再触发额外外部查询。

#### 原则 4：上下文最好保留一份特征快照副本

方便后续日志落地与问题追溯。

---

## 16.5 规则引擎的输入、输出与职责边界

规则引擎在工程上应该被设计成一个“纯评估组件”。

它的输入是：

- `RuleSpec`
- `EvalContext`

它的输出是：

- `RuleEvalResult`

它不应该负责：

- 去 Redis 查名单
- 去算 Flink state
- 去决定最终动作
- 去修改状态
- 去发 Kafka

它应该尽量做到：

> **纯函数化、可重复、可解释、可测试。**

---

### 16.5.1 RuleSpec 应该包含什么

运行时的 `RuleSpec` 建议至少有这些字段：

```java
public class RuleSpec {
    private String ruleCode;
    private String ruleName;
    private String engineType;      // DSL / AVIATOR / GROOVY
    private int priority;
    private String whenExpr;
    private String hitAction;       // PASS / REVIEW / REJECT / TAG
    private Integer score;
    private List<String> dependsOn;
    private String hitReasonTemplate;
    private boolean enabled;
}
```

### 字段解释

#### `whenExpr`

真正的条件判断表达式。

#### `hitAction`

规则命中后的建议动作，不一定就是最终动作。

#### `score`

评分卡模式下的分值，也可在 FIRST\_HIT 中用于日志展示。

#### `dependsOn`

建议在发布时就分析好，这对运行时优化非常有帮助。

#### `hitReasonTemplate`

用于生成日志和界面展示中的命中原因。

---

### 16.5.2 RuleEvalResult 应该包含什么

规则执行结果不能只返回一个 boolean。 否则日志、分析、策略收敛都会很难做。

建议结构类似：

```java
public class RuleEvalResult {
    private String ruleCode;
    private String ruleName;
    private boolean hit;
    private String suggestedAction;
    private Integer score;
    private String reason;
    private Map<String, Object> matchedValues;
    private Long costNanos;
    private String errorCode;
    private String errorMessage;
}
```

### 为什么需要这些字段

#### `hit`

最基本结果。

#### `suggestedAction`

让策略层有选择空间。

#### `score`

支持评分卡模式。

#### `reason`

方便解释。

#### `matchedValues`

例如：

- `user_trade_cnt_5m = 4`
- `amount = 6800`

这些对调试特别有帮助。

#### `costNanos`

可以用于做规则耗时分析。

#### `errorCode / errorMessage`

让运行时异常变得可观察，而不是悄悄吞掉。

---

## 16.6 策略引擎的输入、输出与职责边界

策略引擎消费的是“一组规则执行结果”，而不是原始事件本身。

它通常的输入包括：

- `PolicySpec`
- `List<RuleEvalResult>`
- 可选的 `EvalContext`

它的输出是：

- `DecisionResult`

策略引擎的责任是：

- 定义规则执行顺序
- 决定是否短路
- 决定如何累计分数
- 决定命中结果如何收敛为最终动作
- 决定最终要输出哪些命中规则

---

### 16.6.1 PolicySpec 建议结构

```java
public class PolicySpec {
    private String policyCode;
    private String policyName;
    private String decisionMode;      // FIRST_HIT / SCORE_CARD / ACTION_PRIORITY
    private String defaultAction;
    private List<String> ruleOrder;
    private Map<String, Integer> actionPriority;
    private List<ScoreThreshold> scoreThresholds;
}
```

### 关键字段说明

#### `decisionMode`

决定当前策略按什么方式收敛。

#### `defaultAction`

没有规则命中时返回什么。

#### `ruleOrder`

规则执行次序。通常发布时就应该排好。

#### `actionPriority`

支持一种常见模式：多条命中时按动作优先级收敛，例如 `REJECT > REVIEW > PASS`。

#### `scoreThresholds`

评分卡模式下，用于把总分映射到最终动作。

---

### 16.6.2 DecisionResult 建议结构

```java
public class DecisionResult {
    private String traceId;
    private String eventId;
    private String sceneCode;
    private Long snapshotVersion;
    private String policyCode;

    private String finalAction;
    private Integer totalScore;
    private List<RuleEvalResult> hitRules;
    private List<RuleEvalResult> allRuleResults;
    private Map<String, Object> featureSnapshot;
    private Long latencyMs;
    private String decisionReason;
}
```

### 为什么要同时保留 `hitRules` 和 `allRuleResults`

- `hitRules`：用于业务展示和主日志
- `allRuleResults`：用于深度排障、仿真对比、策略分析

一期如果想简化，也可以先只保留：

- `hitRules`
- `featureSnapshot`
- `finalAction`
- `latencyMs`

但从长期看， `allRuleResults` 很有价值。

---

## 16.7 规则执行的标准生命周期

一条规则在 Flink 中的执行，不应该是“拿一段表达式现算现跑”这么随意。 更推荐你把它看成一个标准生命周期。

---

### 16.7.1 第一阶段：发布时校验

控制平台在发布时，已经对规则做过：

- 语法校验
- 变量依赖校验
- 场景归属校验
- 表达式安全校验

这一阶段保证规则至少是“理论上可执行”的。

---

### 16.7.2 第二阶段：快照加载时编译

Flink 收到快照后，会对规则做进一步的运行时准备，例如：

- 解析规则顺序
- 预编译表达式
- 预编译 Groovy
- 构建 `CompiledRuleInvoker`

注意：

> **规则不应该在每条事件到来时再去 parse / compile。**

必须做到：

- 版本切换时编译一次
- 事件处理时重复使用

---

### 16.7.3 第三阶段：事件处理时执行

真正执行时的步骤通常是：

1. 从快照运行时中取到 `CompiledRuleInvoker`
2. 读取 `EvalContext`
3. 调用 `invoke(ctx)`
4. 得到 `RuleEvalResult`
5. 交给策略层

这一阶段必须尽量轻量，避免重复解析、重复装配。

---

### 16.7.4 第四阶段：日志与可解释信息输出

规则执行结果还要被用于：

- 决策日志
- 命中原因
- 规则效果统计
- 仿真对比

所以规则执行器不能只做“算一下布尔值”，它还得保留必要的解释信息。

---

## 16.8 推荐的规则引擎代码抽象

我建议你把规则执行器设计成统一接口，不要让 Flink 代码里到处 `if DSL else if GROOVY`。

建议抽象类似这样：

```java
public interface RuleInvoker {
    RuleEvalResult evaluate(EvalContext context);
}
```

然后按引擎类型做实现：

```java
public class AviatorRuleInvoker implements RuleInvoker { ... }
public class GroovyRuleInvoker implements RuleInvoker { ... }
```

对应地，再设计一个工厂：

```java
public interface RuleInvokerFactory {
    RuleInvoker build(RuleSpec spec);
}
```

这样好处是：

- 快照加载时统一编译
- 运行时统一调用
- Flink 主链路不需要关心具体表达式实现细节
- 后面支持更多引擎也更容易

---

## 16.9 FIRST\_HIT 模式怎么执行

这是最适合你一期项目先落地的策略模式。

它的基本思想是：

> **按规则顺序依次执行，命中第一条有效规则后立即停止，返回该规则对应动作。**

---

### 16.9.1 FIRST\_HIT 的优点

#### 1）简单直接

很好理解，也很好做。

#### 2）延迟低

因为命中后可以短路，不一定要执行所有规则。

#### 3）符合很多风控场景

例如：

- 黑名单命中直接拒绝
- 白名单命中直接放行
- 某条强规则命中即审核

#### 4）日志清晰

容易解释“为什么是这个结果”。

---

### 16.9.2 FIRST\_HIT 的执行步骤

假设规则顺序是：

1. R001 黑名单设备 -> REJECT
2. R002 高频高额 -> REVIEW
3. R003 高风险用户多账号设备 -> REJECT

执行过程：

1. 执行 R001
2. 如果命中，直接返回 `REJECT`
3. 如果未命中，执行 R002
4. 如果命中，直接返回 `REVIEW`
5. 如果未命中，再执行 R003
6. 如果都未命中，返回 `defaultAction`

---

### 16.9.3 FIRST\_HIT 适合什么场景

它特别适合：

- 有明显优先级的规则体系
- “命中即处理”的风控策略
- 拒绝优先、审核其次的保守型策略
- 想先快速做出稳定 MVP 的系统

对于你的项目，我建议：

> **一期必须把 FIRST\_HIT 做扎实。**

因为它最容易形成完整闭环，也最容易解释。

---

### 16.9.4 FIRST\_HIT 的一个重要细节：顺序必须稳定

FIRST\_HIT 非常依赖顺序。 所以顺序不能由运行时临时随机决定，而应该在发布时明确下来。

优先级来源可以是：

- rule priority
- policy\_rule\_ref.order\_no

最终建议在发布时统一排成一个有序列表，放入快照。

这样 Flink 运行时只需要按顺序遍历即可。

---

## 16.10 SCORE\_CARD 模式怎么执行

评分卡模式的思路是：

> **执行多条规则，把命中的规则分数累计起来，再根据总分映射最终动作。**

这适合一些“风险不是由单条规则决定，而是由多种信号共同累积”的场景。

---

### 16.10.1 SCORE\_CARD 的执行步骤

例如：

- R001 命中黑名单设备：+100
- R002 高频高额：+60
- R003 多账号设备：+40

阈值定义：

- score < 40 -> PASS
- 40 <= score < 80 -> REVIEW
- score >= 80 -> REJECT

执行时：

1. 依次执行所有规则
2. 收集命中结果
3. 累加 score
4. 根据 scoreThresholds 算最终动作
5. 生成最终原因

---

### 16.10.2 SCORE\_CARD 的优点

#### 1）适合复杂风险信号组合

单条规则不够强，但多个弱信号合在一起风险很高。

#### 2）扩展性好

后续很容易接入更多风险维度。

#### 3）更接近很多企业风控策略思路

特别是“规则 + 打分”的系统。

---

### 16.10.3 SCORE\_CARD 的代价

#### 1）通常不能轻易短路

因为你需要执行更多规则才能知道总分。

#### 2）解释难度比 FIRST\_HIT 高

要解释为什么最终是 REJECT，需要把总分来源解释清楚。

#### 3）性能上比 FIRST\_HIT 更重

因为更多规则要执行。

所以我建议你：

- 一期先做 FIRST\_HIT
- 二期再做 SCORE\_CARD

或者一期先把接口设计支持好，功能先做简单版。

---

## 16.11 ACTION\_PRIORITY 模式：一个实用的中间形态

除了 FIRST\_HIT 和 SCORE\_CARD，很多实际系统还会有一种折中模式：

> **执行所有规则，收集命中动作，再按动作优先级收敛最终结果。**

例如：

- REJECT 优先级 3
- REVIEW 优先级 2
- PASS 优先级 1

如果命中了：

- R002 -> REVIEW
- R003 -> REJECT

最终返回 `REJECT`。

这种模式的特点是：

- 不像 FIRST\_HIT 那么依赖顺序
- 不需要打分卡
- 比较适合“强动作优先”的风控场景

你一期不一定要做，但设计 PolicySpec 时可以预留这个模式。

---

## 16.12 建议的策略执行流程

无论最终是 FIRST\_HIT 还是 SCORE\_CARD，策略执行流程都建议统一成几个固定阶段。

---

### 16.12.1 阶段 1：获取可执行规则列表

从当前 `PolicySpec` 获取：

- 已启用规则
- 正确顺序
- 对应的 `RuleInvoker`

这一步最好在快照编译时就准备好，而不是每条事件现拼。

---

### 16.12.2 阶段 2：执行规则

按模式执行：

- FIRST\_HIT：顺序执行，可短路
- SCORE\_CARD：执行所有有效规则
- ACTION\_PRIORITY：执行所有规则后按动作收敛

---

### 16.12.3 阶段 3：收集命中信息

至少收集：

- 哪些规则命中
- 每条规则的 suggestedAction
- 每条规则的 score
- 命中原因

---

### 16.12.4 阶段 4：计算最终动作

根据模式输出：

- FIRST\_HIT -> 第一条命中动作
- SCORE\_CARD -> 总分阈值映射
- ACTION\_PRIORITY -> 命中动作最高优先级

---

### 16.12.5 阶段 5：构造 DecisionResult

把：

- finalAction
- totalScore
- hitRules
- featureSnapshot
- latency
- version

统一装进最终结果对象。

---

## 16.13 命中原因（Hit Reason）应该怎么设计

命中原因是一个非常容易被低估、但对平台价值极高的能力。

很多系统最后之所以不好用，不是因为规则算不出来，而是因为算出来以后没人看得懂。

所以你必须从一开始就考虑：

> **命中后要如何告诉业务和开发“为什么命中”。**

---

### 16.13.1 命中原因最简单的形式

最简单是写死一句：

- “设备命中黑名单”
- “高频高额交易”

这种方式够用，但解释力度不够。

---

### 16.13.2 更推荐的方式：模板化命中原因

例如：

```latex
用户5分钟交易次数={user_trade_cnt_5m}, 当前金额={amount}
```

命中后渲染成：

```latex
用户5分钟交易次数=4, 当前金额=6800
```

这样业务方、开发、面试官一看就知道为什么命中。

---

### 16.13.3 命中原因生成时的建议

#### 1）原因模板在规则层定义

因为命中原因往往最贴近规则语义。

#### 2）模板渲染在规则命中后执行

只对命中规则做渲染，节省开销。

#### 3）尽量引用关键变量，不要过长

保留最关键的判断依据即可。

#### 4）注意空值处理

避免渲染出 `null` 导致体验差。

---

## 16.14 决策结果为什么必须可解释、可追溯

在实时风控里，可解释性不是锦上添花，而是核心能力之一。

你必须能回答：

- 为什么这笔交易被拒绝？
- 命中了哪条规则？
- 当时特征值是多少？
- 用的是哪个版本？
- 如果规则顺序变化，结果会不会不同？

所以建议你从一开始就在 `DecisionResult` 中保留这些信息：

- `snapshotVersion`
- `policyCode`
- `hitRules`
- `featureSnapshot`
- `finalAction`
- `decisionReason`
- `latencyMs`

这是后面日志系统、仿真测试、版本对比的基础。

---

## 16.15 规则执行中的异常与空值处理

这是工程上非常关键的一节。

如果你不提前想好异常和空值怎么处理，线上规则执行会非常脆弱。

---

### 16.15.1 空值处理

规则表达式经常会依赖一些可能缺失的变量，例如：

- `user_risk_level`
- `ip_risk_score`
- `device_bind_user_cnt_1h`

如果这些值没查到，怎么办？

建议原则：

#### 原则 1：尽量在上下文构建阶段就补默认值

不要把空值问题留到规则表达式才爆。

#### 原则 2：发布时校验字段类型与默认值策略

确保上下文语义稳定。

#### 原则 3：表达式执行要支持 null-safe

尤其是 Groovy/表达式引擎中要有统一的空值约定。

---

### 16.15.2 执行异常处理

即使发布前做了校验，运行时仍可能出现异常，例如：

- lookup 返回类型不符合预期
- 某个值转换失败
- Groovy 运行时抛异常
- 模板渲染失败

建议策略：

#### 1）单条规则异常不应轻易拖垮整个作业

规则异常最好被封装成 `RuleEvalResult` 中的 error 信息。

#### 2）可配置 fail-open / fail-close

例如某些场景下：

- 规则异常视为不命中（fail-open）
- 或直接返回 REVIEW（fail-safe）

一期你可以先简单做成：

- 记录异常
- 该条规则视为未命中
- 最终在 DecisionResult 中保留 error 标记

#### 3）异常必须进入日志和指标

否则你很难排查线上问题。

---

## 16.16 在 Flink 热路径里如何保证规则执行性能

规则和策略执行虽然不像流式特征那么吃状态，但它也在热路径上。 如果处理不好，延迟会明显上升。

---

### 16.16.1 原则一：编译只做一次，执行重复使用

这是最重要的原则。

- 表达式/Groovy 在版本加载时预编译
- 每条事件只调用执行器

不要每条事件都：

- parse 规则
- compile 脚本
- 重新分析依赖

---

### 16.16.2 原则二：上下文尽量提前构建完成

规则执行阶段不要再去：

- 查 Redis
- 查数据库
- 做复杂 JSON 解析

规则引擎只消费已准备好的上下文。

---

### 16.16.3 原则三：按依赖裁剪上下文和规则

如果发布时已经分析出：

- 某条规则只依赖 `amount` 和 `user_trade_cnt_5m`

那运行时就没必要为它准备一堆无关变量。

进一步地，你还可以做：

- 按 eventType 过滤不相关规则
- 按 scene 只加载当前场景规则
- 按 rule dependsOn 做上下文最小化

这对性能和可解释性都有帮助。

---

### 16.16.4 原则四：合理使用短路

FIRST\_HIT 模式最大的性能优势就在于可以短路。

例如：

- 黑名单直接 REJECT
- 后面的复杂规则就没必要再跑

这在高 TPS 场景下会非常有价值。

---

### 16.16.5 原则五：日志构建不要过重

例如：

- 不要在热路径里对每条规则都构建超大 JSON
- 不要对未命中规则做复杂 reason 渲染
- 可把详细日志异步下沉

---

## 16.17 如何保证规则结果与仿真结果一致

这件事我建议你从一开始就当成核心要求，而不是后面补救。

很多平台最怕的问题就是：

- 仿真页命中了
- 线上却没命中

造成这种问题最常见的原因是：

- 后台仿真和 Flink 线上使用了两套规则执行逻辑
- 上下文构建方式不同
- 默认值处理不同
- 模板渲染逻辑不同

所以建议坚持：

> **仿真和线上要共用同一套 RuleEngine / PolicyEngine 核心库。**

也就是说：

- Spring Boot 仿真页依赖 `pulsix-kernel`
- Flink 引擎也依赖 `pulsix-kernel`

两边的区别应该只在于：

- 上下文来源不同
- 运行环境不同

而不是执行逻辑不同。

---

## 16.18 规则执行与版本切换的一致性要求

在 Flink 中，运行时快照可能会更新。 那么规则执行必须满足一个非常重要的一致性要求：

> **同一条事件的整个决策过程，必须使用同一个版本的快照。**

不能出现：

- 前半段按 v11 构建上下文
- 后半段按 v12 执行策略

所以推荐原则是：

1. 事件开始处理时先确定当前场景版本
2. 在这条事件处理完成前，始终使用这个版本对应的运行时对象
3. 新版本只影响后续进入的事件

这会让日志、回放、问题定位都清晰很多。

---

## 16.19 建议的代码骨架

下面给你一版比较适合后续落地的代码抽象思路。

---

### 16.19.1 规则执行接口

```java
public interface RuleEngine {
    RuleEvalResult evaluate(RuleInvoker invoker, EvalContext context);
}
```

更简单一点也可以直接把逻辑放到 `RuleInvoker` 里。

---

### 16.19.2 策略执行接口

```java
public interface PolicyEngine {
    DecisionResult decide(PolicyRuntime policyRuntime, EvalContext context);
}
```

---

### 16.19.3 策略运行时对象

```java
public class PolicyRuntime {
    private PolicySpec policySpec;
    private List<RuleRuntime> orderedRules;
}

public class RuleRuntime {
    private RuleSpec ruleSpec;
    private RuleInvoker invoker;
}
```

---

### 16.19.4 FIRST\_HIT 伪代码

```java
public DecisionResult decideFirstHit(PolicyRuntime runtime, EvalContext ctx) {
    List<RuleEvalResult> allResults = new ArrayList<>();

    for (RuleRuntime ruleRuntime : runtime.getOrderedRules()) {
        RuleEvalResult result = ruleRuntime.getInvoker().evaluate(ctx);
        allResults.add(result);

        if (result.isHit()) {
            return DecisionResult.firstHit(ctx, runtime, result, allResults);
        }
    }

    return DecisionResult.defaultAction(ctx, runtime, allResults);
}
```

---

### 16.19.5 SCORE\_CARD 伪代码

```java
public DecisionResult decideScoreCard(PolicyRuntime runtime, EvalContext ctx) {
    List<RuleEvalResult> allResults = new ArrayList<>();
    int totalScore = 0;

    for (RuleRuntime ruleRuntime : runtime.getOrderedRules()) {
        RuleEvalResult result = ruleRuntime.getInvoker().evaluate(ctx);
        allResults.add(result);

        if (result.isHit() && result.getScore() != null) {
            totalScore += result.getScore();
        }
    }

    String finalAction = runtime.getPolicySpec().resolveActionByScore(totalScore);
    return DecisionResult.scoreCard(ctx, runtime, totalScore, finalAction, allResults);
}
```

---

## 16.20 你这个项目一期最推荐的落地方式

结合你的项目目标和 6 个月节奏，我建议你在第 16 章落地时，优先采用下面这套方案：

### 一期一定做好的

#### 1）RuleSpec + PolicySpec + EvalContext + DecisionResult 四个核心对象

这是整套决策执行的数据骨架。

#### 2）统一 RuleInvoker 抽象

无论 DSL 还是 Groovy，都通过统一接口执行。

#### 3）先把 FIRST\_HIT 做扎实

支持：

- 规则顺序
- 命中短路
- 命中原因
- 最终动作输出
- 日志留痕

#### 4）保留 score 字段和 scoreThresholds 结构

即使一期暂时不 fully support，也为后续 SCORE\_CARD 预留好结构。

#### 5）日志里必须落：版本、命中规则、特征快照、最终动作

这是你平台价值的重要体现。

---

### 二期再增强的

- SCORE\_CARD 完整实现
- ACTION\_PRIORITY 模式
- 更复杂的命中原因模板
- 规则组 / 预筛选优化
- 多动作组合输出
- 命中路径可视化

---

## 16.21 本章小结

这一章核心解决的是：

> **规则引擎和策略引擎在 Flink 中到底如何执行。**

把重点收一下：

### 1）规则和策略必须分开

- 规则负责单条条件判断
- 策略负责多条结果收敛

### 2）规则执行依赖完整上下文

上下文应由前置链路准备好，规则引擎只消费 `EvalContext`。

### 3）规则输出不应只是 boolean

而应是结构化的 `RuleEvalResult`，包含：

- hit
- score
- suggestedAction
- reason
- matchedValues

### 4）策略输出应是统一的 `DecisionResult`

至少包含：

- finalAction
- hitRules
- featureSnapshot
- version
- latency

### 5）一期最适合先做好 FIRST\_HIT

它简单、稳定、低延迟、易解释，非常适合你的 MVP。

### 6）规则执行必须做到

- 预编译
- 可解释
- 可追溯
- 与仿真一致
- 与版本切换一致

---

## 16.22 下一章会讲什么

下一章我们进入：

## 第 17 章：表达式引擎与 Groovy 的设计、差异与使用边界

这一章会重点回答：

- 表达式引擎是什么，适合做什么
- Groovy 是什么，适合做什么
- 两者在功能、性能、安全、可解释性上的差异
- 为什么不要一上来就全部用 Groovy
- 哪些规则适合表达式，哪些场景才真正需要 Groovy
- 它们在 Flink 运行时是如何被编译和执行的

也就是说，下一章我们会把本章中的“RuleInvoker”进一步拆开，真正讲清楚：

> **规则表达式和 Groovy 脚本在这个平台里该怎么分工、怎么落地。**
