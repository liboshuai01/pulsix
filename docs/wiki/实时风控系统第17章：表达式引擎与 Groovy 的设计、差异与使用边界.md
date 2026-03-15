## 17.1 这一章解决什么问题

前面几章我们已经把下面这些关键问题拆开讲过了：

- 控制平台为什么要生成运行时快照
- Flink 如何接收、缓存和切换快照
- 实时特征哪些适合动态化，哪些不适合
- 流式特征的状态如何设计
- 规则与策略在 Flink 中如何执行

到这里，系统主骨架已经比较清楚了。但还差一个你最关心、也是最容易在实现阶段出问题的核心点：

> 控制平台里配置出来的“表达式”和“Groovy 脚本”，在 Flink 里到底应该怎么使用？

更具体一点，这一章就是要彻底讲透下面这些问题：

1. 表达式引擎是什么，它适合解决什么问题
2. Groovy 是什么，它适合解决什么问题
3. 两者在能力、性能、安全性、可解释性上有什么本质差异
4. 风控平台里哪些逻辑应该交给表达式，哪些才应该交给 Groovy
5. Flink 在运行时到底是如何处理这两类逻辑的
6. 为什么不能把所有规则都做成 Groovy
7. 为什么更不能让 Groovy 直接定义 Flink 的状态模型、窗口语义和定时器语义
8. 控制平台发布时，应该如何校验和编译这两类逻辑
9. Flink 运行时，应该如何做预编译、本地缓存、异常处理与安全隔离

如果你把这一章吃透了，那么你后面做下面这些设计时会非常顺：

- RuleSpec 应该长什么样
- DerivedFeatureSpec 应该长什么样
- engineType 如何设计
- Flink 本地运行时缓存如何设计
- 为什么 Broadcast State 里只存快照，不存编译结果
- 为什么仿真测试与线上执行必须共用同一套执行器抽象

所以这一章本质上是在回答：

> 平台动态逻辑，到底应该如何“既灵活，又可控”地落在 Flink 决策引擎里。

---

## 17.2 先给出全章最核心的结论

我先把结论放在前面，便于你带着判断去看后面所有内容。

### 结论 1：表达式引擎应该是平台默认主力，Groovy 应该是高级扩展能力

也就是说，你的平台不应该一上来就：

- 所有 derived feature 都支持 Groovy
- 所有 rule 都支持 Groovy
- 所有人都能随便写脚本

更合理的方式是：

- **90% 常规规则与派生特征，用表达式引擎解决**
- **少量复杂逻辑，再由 Groovy 补位**

---

### 结论 2：表达式/Groovy 适合定义“上下文就绪后的判断逻辑”，不适合定义 Flink 的底层状态语义

这句话非常关键。

适合脚本化的，是：

- 规则条件判断
- 派生特征计算
- 命中原因模板生成中的部分逻辑
- 少量复杂条件组合

不适合脚本化的，是：

- keyBy 逻辑任意定义
- 状态结构任意定义
- timer 语义任意定义
- watermark/window 行为任意定义
- 事件缓存与状态清理任意定义

因为这些不是“业务判断层”，而是“流计算引擎层”。

---

### 结论 3：控制平台负责校验和生成运行时计划，Flink 负责执行，不负责理解一堆任意脚本的世界

控制平台负责：

- 语法检查
- 变量检查
- 依赖分析
- 安全校验
- 编译前验证
- 生成 engineType + exprContent + dependsOn 的运行时描述

Flink 负责：

- 接收快照
- 预编译表达式/Groovy
- 构建 EvalContext
- 调用统一执行器
- 处理命中与异常

Flink 不应该在运行时临时决定：

- 这段脚本是不是安全
- 这个变量是不是存在
- 这个特征依赖图是不是有环

这些事都应该在发布前做掉。

---

### 结论 4：平台必须有统一执行器抽象，不能让表达式和 Groovy 各玩各的

你最终一定要抽象出类似这样的能力：

- `FeatureInvoker`
- `RuleInvoker`
- `ConditionEvaluator`
- `ScriptInvoker`

这样你才能做到：

- 控制平台仿真与 Flink 运行共用执行内核
- 不同 engineType 可以统一接入
- 更容易做缓存、监控、异常统计

---

### 结论 5：Groovy 必须沙箱化、缓存化、限边界化

Groovy 不是不能用，而是必须“谨慎地用”。

如果不用边界控制，你很快会遇到：

- 每条事件都动态编译，性能直接崩
- 脚本访问危险 API，安全性出问题
- 类加载器泄漏，Metaspace 持续上涨
- 脚本副作用污染运行线程
- 脚本逻辑难解释、难回溯、难验证

所以 Groovy 只能作为**高级能力**，而不是平台默认基础能力。

---

## 17.3 表达式引擎到底是什么

### 17.3.1 表达式引擎的本质

表达式引擎本质上是：

> 在一个给定的变量上下文中，执行一段受限的条件或计算表达式，并返回结果。

例如：

```latex
amount >= 5000 && user_trade_cnt_5m >= 3
```

或者：

```latex
device_in_blacklist == true
```

或者：

```latex
user_trade_amt_sum_30m / max(user_trade_cnt_30m, 1)
```

它的特点是：

- 语法相对有限
- 能力聚焦于计算和判断
- 很适合做无副作用逻辑
- 很适合在高频运行中重复执行

---

### 17.3.2 在风控平台里，表达式引擎最适合做什么

在你的平台里，表达式引擎最适合做两类事：

#### 一类：规则条件判断

例如：

```latex
amount >= 5000 && user_trade_cnt_5m >= 3 && device_in_blacklist == true
```

#### 二类：派生特征计算

例如：

```latex
amount >= 5000
```

得到：

- `high_amt_flag = true`

再比如：

```latex
user_trade_cnt_5m >= 3 && amount >= 5000
```

得到：

- `trade_burst_flag = true`

---

### 17.3.3 为什么表达式引擎很适合作为平台默认能力

因为它具备几个非常适合平台化的特征：

#### 1）语法简单，易理解

策略人员、测试、开发都更容易理解。

#### 2）容易做静态校验

例如：

- 变量是否存在
- 函数是否允许
- 类型是否大致兼容

#### 3）可解释性强

一条规则长什么样，一眼就能看懂。

#### 4）更容易限制边界

不容易随便做文件 IO、网络请求、线程阻塞等危险操作。

#### 5）运行时开销相对更低

预编译后执行通常比较稳定。

所以从平台设计角度看：

> 表达式引擎是“平台可控性”和“业务灵活性”之间最好的平衡点。

---

## 17.4 Groovy 到底是什么

### 17.4.1 Groovy 的本质

Groovy 是一门运行在 JVM 上的动态脚本语言。它能做的事情远比表达式引擎多。

它不仅能写：

```groovy
amount >= 5000 && user_trade_cnt_5m >= 3
```

还可以写：

```groovy
def burst = amount >= 5000 && user_trade_cnt_5m >= 3
if (burst && ['M', 'H'].contains(user_risk_level)) {
    return true
}
return false
```

它支持：

- if/else
- 方法调用
- 集合处理
- 局部变量
- 更灵活的对象操作
- 更复杂的计算逻辑

所以 Groovy 本质上更像：

> 一个“轻量脚本编程语言”，而不只是一个“表达式求值器”。

---

### 17.4.2 在风控平台里，Groovy 最适合做什么

Groovy 不适合做平台默认能力，但它很适合用来解决一些表达式引擎不够优雅的问题。

比如下面这些场景：

#### 场景 1：逻辑分支比较复杂

```groovy
def suspicious = false
if (amount >= 5000 && user_trade_cnt_5m >= 3) {
    suspicious = true
}
if (device_bind_user_cnt_1h >= 4 && ['M','H'].contains(user_risk_level)) {
    suspicious = true
}
return suspicious
```

#### 场景 2：需要对集合做更灵活处理

```groovy
return risk_tags != null && risk_tags.any { it in ['BLACK_DEVICE','HIGH_PROXY_IP'] }
```

#### 场景 3：需要更复杂的派生逻辑封装

```groovy
def score = 0
if (amount >= 5000) score += 30
if (user_trade_cnt_5m >= 3) score += 40
if (device_in_blacklist) score += 100
return score >= 100
```

这些逻辑用纯表达式也不是完全不能写，但可读性会明显下降。

所以 Groovy 的适用位置是：

> **复杂但仍然属于“上下文就绪后的纯逻辑判断/计算”的场景。**

---

## 17.5 表达式引擎和 Groovy 的本质区别

这一节非常关键，因为它决定你后面平台能力的边界。

| 维度       | 表达式引擎      | Groovy    |
| :------- | :--------- | :-------- |
| 本质       | 受限计算/判断表达式 | 完整脚本语言    |
| 能力强度     | 中等         | 很强        |
| 可解释性     | 很强         | 相对较弱      |
| 运行性能     | 通常更高更稳定    | 通常略低，波动更大 |
| 安全控制     | 更容易        | 更难        |
| 校验难度     | 较低         | 较高        |
| 平台化难度    | 较低         | 较高        |
| 调试难度     | 较低         | 较高        |
| 适合做默认能力吗 | 非常适合       | 不适合       |
| 适合做高级扩展吗 | 一般         | 非常适合      |

---

### 17.5.1 能力差异

表达式引擎能做的，通常是：

- 比较运算
- 布尔组合
- 数值计算
- 三元表达式
- 简单函数调用

Groovy 能做的更多：

- if/else
- 局部变量
- 集合循环
- 更复杂对象处理
- 自定义函数封装（若开放）

所以能力上，Groovy 明显更强。

但平台设计从来不是“能力越强越好”，而是：

> **能力是否可控、可解释、可运营。**

---

### 17.5.2 性能差异

从运行角度看：

- 表达式引擎更轻量
- Groovy 更重
- Groovy 对类加载、对象创建、Binding 组织更敏感

如果你有 10,000+ TPS 的决策流，哪怕每条事件只是多做一点对象构造和脚本调用，也会带来明显差异。

所以性能上：

> **表达式应当承担高频主路径，Groovy 只做少量高级补充。**

---

### 17.5.3 安全差异

表达式引擎通常语法受限，平台比较容易控制它“只能做计算，不能乱干别的”。

Groovy 则不同。理论上它可以：

- import 类
- 创建对象
- 访问系统 API
- 做文件/网络操作
- 调线程
- 调反射

如果不做沙箱约束，就会很危险。

所以安全性上：

> 表达式天生更适合平台开放；Groovy 必须在沙箱中谨慎开放。

---

### 17.5.4 可解释性差异

风控平台不是单纯“跑出结果”就行，它还要向策略人员、测试、运营甚至审计解释：

- 为什么命中
- 哪些条件导致命中
- 变量值是什么
- 规则含义是什么

表达式一般天然更适合解释。

例如：

```latex
amount >= 5000 && user_trade_cnt_5m >= 3
```

这条规则非常容易被人理解。

而 Groovy 脚本一旦开始引入：

- 局部变量
- 分支逻辑
- 集合处理
- 自定义辅助函数

其可解释性就会下降。

所以从平台运营角度看：

> **表达式更适合大量常规规则，Groovy 更适合少量专家级规则。**

---

## 17.6 风控平台里，哪些逻辑应该用表达式，哪些应该用 Groovy

这一节是全章最重要的实践判断标准。

---

### 17.6.1 最适合表达式的逻辑

#### 1）普通阈值判断

例如：

```latex
user_login_fail_cnt_10m >= 5
```

#### 2）多条件组合判断

例如：

```latex
amount >= 5000 && user_trade_cnt_5m >= 3 && device_in_blacklist == true
```

#### 3）简单派生特征

例如：

```latex
amount >= 5000
```

#### 4）简单评分逻辑

例如：

```latex
(amount >= 5000 ? 30 : 0) + (user_trade_cnt_5m >= 3 ? 40 : 0)
```

#### 5）白名单豁免、黑名单命中之类简单布尔逻辑

这些都应该优先用表达式。

---

### 17.6.2 适合 Groovy 的逻辑

#### 1）逻辑分支明显比表达式更易读

```groovy
def score = 0
if (amount >= 5000) score += 30
if (user_trade_cnt_5m >= 3) score += 40
if (device_in_blacklist) score += 100
return score >= 100
```

#### 2）复杂集合判断

```groovy
return riskTags != null && riskTags.any { it in ['DEVICE_BLACK','IP_PROXY','CARD_STOLEN'] }
```

#### 3）复杂派生逻辑的封装

#### 4）需要少量局部变量提高可读性的专家规则

但注意：即使是这些，也必须满足一个前提：

> **Groovy 只是在完整 EvalContext 上做“纯逻辑计算”，不能去触碰引擎底层能力。**

---

### 17.6.3 不应该交给表达式/Groovy 的逻辑

这一条尤其重要。

下面这些逻辑，都不应该开放成表达式或 Groovy：

#### 1）Flink 状态结构定义

例如：

- 用什么 State 类型
- 如何 bucket 化
- 如何清理

#### 2）keyBy 语义

例如：

- 按 userId keyBy
- 按 deviceId keyBy
- 按组合 keyBy

这些应该通过特征模板配置，而不是脚本任意定义。

#### 3）timer / watermark / window 行为

例如：

- 乱序容忍
- 事件时间推进
- 过期清理时机

这些属于流引擎核心能力，不适合开放给规则脚本。

#### 4）任意外部副作用

例如：

- 查数据库
- 发 HTTP
- 读文件
- 写文件
- sleep
- 打开线程

这些行为会直接破坏 Flink 热路径的稳定性。

所以非常明确地说：

> **表达式和 Groovy 的边界，是“上下文就绪后的纯逻辑计算”；不是“任意运行时脚本能力”。**

---

## 17.7 平台层面的统一抽象应该怎么设计

为了让表达式和 Groovy 能同时被控制平台和 Flink 共用，你必须做统一抽象。

如果不抽象，后面会出现：

- 控制台仿真一套逻辑
- Flink 线上一套逻辑
- 表达式一套调用方式
- Groovy 一套调用方式
- 日志解释又是一套方式

最终平台会非常混乱。

我建议你至少抽象出下面这些核心接口。

---

### 17.7.1 EvalContext

先统一“执行上下文”。

```java
public class EvalContext {
    private String sceneCode;
    private String eventId;
    private String traceId;
    private Long version;
    private Map<String, Object> vars;

    public Object get(String name) {
        return vars.get(name);
    }

    public Map<String, Object> asMap() {
        return vars;
    }
}
```

这里的 `vars` 里统一放：

- base fields
- stream features
- lookup features
- derived features

这样无论表达式还是 Groovy，最终都面对同一种上下文模型。

---

### 17.7.2 ConditionEvaluator / ScriptInvoker

建议统一抽象成：

```java
public interface ConditionEvaluator {
    boolean eval(EvalContext context);
}
```

对于派生特征，还可以有：

```java
public interface ValueEvaluator {
    Object eval(EvalContext context);
}
```

---

### 17.7.3 ExpressionEvaluator 实现

```java
public class AviatorConditionEvaluator implements ConditionEvaluator {
    private final Expression compiled;

    @Override
    public boolean eval(EvalContext context) {
        Object result = compiled.execute(context.asMap());
        return Boolean.TRUE.equals(result);
    }
}
```

---

### 17.7.4 GroovyEvaluator 实现

```java
public class GroovyConditionEvaluator implements ConditionEvaluator {
    private final GroovyInvoker invoker;

    @Override
    public boolean eval(EvalContext context) {
        Object result = invoker.invoke(context.asMap());
        return Boolean.TRUE.equals(result);
    }
}
```

---

### 17.7.5 统一 RuleInvoker

```java
public interface RuleInvoker {
    RuleHitResult execute(EvalContext context);
}
```

内部再持有：

- RuleSpec
- ConditionEvaluator
- 命中原因模板处理器

这样 RuleInvoker 就不关心底层到底是表达式还是 Groovy。

这才是平台化的正确方式。

---

## 17.8 控制平台在发布前，应该如何处理表达式和 Groovy

这里进入真正工程落地点。

控制平台面对表达式/Groovy，绝不能只是“保存文本”。

它至少要做三类事：

1. 语法校验
2. 依赖分析
3. 安全校验

---

### 17.8.1 对表达式的校验

控制平台发布前至少要检查：

#### 1）语法是否合法

例如：

- 括号是否匹配
- 运算符是否合法
- 函数是否合法

#### 2）变量是否存在

例如：

- `user_trade_cnt_5m` 是否定义过
- `amount` 是否属于 event schema

#### 3）类型是否大体合理

例如：

- 把字符串拿去做大于比较
- 布尔和数字混用

#### 4）函数白名单

比如允许：

- `max`
- `min`
- `contains`
- `size`

不允许未知函数随便出现。

---

### 17.8.2 对 Groovy 的校验

Groovy 的校验要更严格。

至少要做：

#### 1）语法解析是否通过

确保脚本本身可编译。

#### 2）引用变量是否存在

哪怕无法完全静态分析，也应尽可能做基础变量检查。

#### 3）危险 API 检查

例如禁止：

- `System.exit`
- `Runtime.getRuntime()`
- `new File(...)`
- `URL(...)`
- `Class.forName(...)`
- 反射相关调用

#### 4）import 白名单

限制能 import 的包。

#### 5）执行入口规范

比如要求返回：

- `boolean`
- `Object`
- 或实现固定接口

不要允许脚本结构完全自由散漫。

---

### 17.8.3 依赖分析

无论表达式还是 Groovy，都建议在发布时生成：

- `dependsOn`
- `engineType`
- `returnType`
- `normalizedExpr`

例如：

```json
{
  "code": "R002",
  "engineType": "AVIATOR",
  "expr": "user_trade_cnt_5m >= 3 && amount >= 5000",
  "dependsOn": ["user_trade_cnt_5m", "amount"],
  "returnType": "BOOLEAN"
}
```

这会让后续：

- 快照更清晰
- Flink 更容易构建执行计划
- 仿真结果更可解释

---

## 17.9 Flink 在运行时，如何处理表达式与 Groovy

现在进入你最关心的运行时问题。

---

### 17.9.1 Flink 不应该每条事件现编译脚本

这一点必须绝对明确：

> **所有表达式和 Groovy，都应该在快照切换时预编译，而不是在每条事件执行时临时编译。**

否则后果非常明显：

- CPU 开销暴涨
- GC 压力增大
- 延迟波动非常大
- Groovy 的类加载器问题会放大

所以运行时正确方式是：

1. 收到新快照
2. 解析所有 rule / derived feature
3. 按 engineType 分别编译
4. 放入本地编译缓存
5. 事件来了直接执行

---

### 17.9.2 Broadcast State 存什么，本地缓存存什么

这一条很关键。

#### Broadcast State 存：

- 原始 SceneSnapshot
- 可序列化配置对象
- version / checksum / effectiveFrom

#### 本地 transient cache 存：

- 编译后的表达式对象
- 编译后的 Groovy 脚本类 / invoker
- 已整理好的 RuleInvoker 列表
- DerivedFeatureInvoker 列表

原因很简单：

- 编译结果往往不可序列化
- 不适合直接 checkpoint
- 容易引发 ClassLoader 和恢复问题

所以应该坚持：

> **可恢复配置进 Broadcast State；不可序列化运行时对象进本地缓存。**

---

### 17.9.3 事件执行时的典型顺序

当一条事件进入时，Flink 的流程可以简化为：

1. 根据 sceneCode 找当前 `CompiledSceneRuntime`
2. 构建基础 `EvalContext`
3. 填充 stream features
4. 填充 lookup features
5. 执行 derived feature evaluator
6. 执行 rule invokers
7. 执行 policy aggregator
8. 输出 DecisionResult

在这个过程中：

- 表达式与 Groovy 只是某些步骤中的“执行器”
- 它们不是整条链路的主宰

这点一定要分清。

---

## 17.10 表达式引擎在 Flink 中的推荐运行方式

你项目里，一期我非常建议以表达式引擎为主。

---

### 17.10.1 推荐用法

#### derived feature

```json
{
  "code": "high_amt_flag",
  "engineType": "AVIATOR",
  "expr": "amount >= 5000"
}
```

#### rule

```json
{
  "code": "R002",
  "engineType": "AVIATOR",
  "expr": "high_amt_flag == true && user_trade_cnt_5m >= 3"
}
```

---

### 17.10.2 快照加载时

- parse expr
- compile expr
- 包装为 `ConditionEvaluator` / `ValueEvaluator`
- 放进 `CompiledSceneRuntime`

---

### 17.10.3 事件执行时

- 从 `EvalContext.asMap()` 读取变量
- 执行 compiled expression
- 拿结果返回

这样整体非常轻量，而且更容易稳定。

---

## 17.11 Groovy 在 Flink 中的推荐运行方式

Groovy 可以支持，但一定要“受控设计”。

---

### 17.11.1 推荐的脚本规范

不要让 Groovy 脚本结构过于自由。

建议统一规范，例如：

#### 规范 A：表达式式脚本

```groovy
return amount >= 5000 && user_trade_cnt_5m >= 3
```

#### 规范 B：有限脚本块

```groovy
def suspicious = false
if (amount >= 5000 && user_trade_cnt_5m >= 3) {
    suspicious = true
}
return suspicious
```

不要开放：

- 自定义类定义
- 任意 import
- 多文件依赖
- 任意 new 对象

---

### 17.11.2 快照加载时

推荐流程：

1. 使用受控 `GroovyClassLoader`
2. 对脚本做 compile
3. 包装成 `GroovyInvoker`
4. 放进 `CompiledSceneRuntime`

可以考虑每个 snapshot version 对应一个独立 classloader 范围，便于后续淘汰旧版本。

---

### 17.11.3 事件执行时

- 构造受控 Binding / Map 上下文
- 调用脚本 invoker
- 获取返回值
- 做布尔或对象类型转换

这一步必须保持：

- 无副作用
- 无外部资源访问
- 无阻塞

---

## 17.12 为什么不能把所有规则都做成 Groovy

这个问题非常现实，我直接给你几个最关键原因。

---

### 17.12.1 原因 1：平台可解释性会下降

大多数策略人员、测试、运营更容易读表达式，不容易读复杂脚本。

风控平台不是开发者私有系统，它是要给业务人员运营的。

---

### 17.12.2 原因 2：发布校验难度会大幅上升

表达式比较容易做：

- 变量存在性检查
- 运算符校验
- 函数白名单

Groovy 则困难得多，尤其一旦脚本复杂起来：

- 静态分析难度高
- 安全审计难度高
- 类型推断也更复杂

---

### 17.12.3 原因 3：运行风险明显更高

Groovy 可能带来：

- 类加载问题
- 内存泄漏问题
- 脚本副作用问题
- 非预期 API 调用
- 性能抖动

如果全站所有规则都 Groovy 化，平台稳定性会很难保证。

---

### 17.12.4 原因 4：仿真、解释、回放的复杂度更高

表达式天然更容易：

- 展示原文
- 抽取变量
- 生成命中原因
- 做 explain

Groovy 的解释能力则相对弱很多。

所以平台建设角度一定要坚持：

> **Groovy 是高级逃生舱，不是默认主干路。**

---

## 17.13 Groovy 必须做哪些安全与工程约束

这里我给你一版非常务实的建议清单。

---

### 17.13.1 约束 1：限制 import

只允许极少量白名单包。

例如只允许：

- `java.util.*` 中的有限集合类
- 平台定义的辅助类（若你需要）

禁止：

- `java.io.*`
- `java.net.*`
- `java.lang.reflect.*`
- `java.nio.file.*`
- `groovy.sql.*`

---

### 17.13.2 约束 2：限制危险方法

至少禁止：

- `System.exit`
- `Runtime.getRuntime`
- `Thread.sleep`
- `new File`
- `ProcessBuilder`
- 反射入口

---

### 17.13.3 约束 3：限制脚本执行时间与复杂度

虽然在 JVM 内部脚本很难像外部容器那样硬隔离，但你至少应做到：

- 运行前白名单校验
- 限制脚本大小
- 限制脚本编译数量
- 限制单场景 Groovy 规则数量

例如一期甚至可以规定：

- 每个 scene 最多 10 条 Groovy 规则
- 一个 snapshot 中脚本总长度不超过某阈值

---

### 17.13.4 约束 4：脚本必须无副作用

也就是：

- 不改全局状态
- 不写外部资源
- 不访问网络
- 不发消息
- 不做阻塞 IO

平台必须把 Groovy 定位成：

> 纯函数式逻辑计算脚本

而不是“万能脚本执行器”。

---

### 17.13.5 约束 5：旧版本脚本要可清理

Groovy 很容易出现类加载器残留问题。你必须考虑：

- 新版本切换后，旧版本脚本缓存何时清理
- classloader 何时释放引用
- 本地 runtime cache 如何淘汰

否则长期运行后很可能出现 Metaspace 持续膨胀。

---

## 17.14 在你的项目里，表达式与 Groovy 的推荐落地策略

这是我给你的非常明确的实施建议。

---

### 17.14.1 第一阶段：表达式优先，Groovy 暂缓

#### 支持内容：

- Derived feature：表达式
- Rule：表达式
- Policy：结构化配置

#### 目的：

- 先把主链路做稳
- 先把快照、Flink 执行、仿真、日志打通
- 先把控制平台的变量校验和依赖分析做好

这是最适合你项目第一个可用版本的路线。

---

### 17.14.2 第二阶段：引入受控 Groovy 作为高级规则能力

#### 支持范围：

- 少量复杂 derived feature
- 少量高级 rule

#### 必须同时补齐：

- 脚本白名单
- 安全校验
- 本地缓存
- 脚本数量限制
- 更明确的仿真验证

---

### 17.14.3 第三阶段：再考虑更复杂脚本能力

例如：

- 更复杂评分脚本
- 更复杂 explain 逻辑
- 少量高级模板函数

但我仍然不建议把它扩展到“脚本定义状态机”。

---

## 17.15 一套推荐的 engineType 设计

你的平台里，建议先统一使用 `engineType` 字段，贯穿：

- derived feature
- rule

推荐值：

```latex
AVIATOR
GROOVY
```

如果你想更抽象，也可以：

```latex
EXPR
GROOVY
```

然后由系统内部决定 `EXPR` 对应 Aviator 或其他表达式实现。

这样好处是：

- 快照结构稳定
- 控制平台更容易扩展
- Flink 运行时通过工厂模式统一创建 evaluator

例如：

```java
public interface EvaluatorFactory {
    ValueEvaluator createValueEvaluator(String engineType, String expr);
    ConditionEvaluator createConditionEvaluator(String engineType, String expr);
}
```

---

## 17.16 表达式和 Groovy 在快照中应该如何表示

建议快照里明确写出：

```json
{
  "code": "R002",
  "engineType": "AVIATOR",
  "expr": "high_amt_flag == true && user_trade_cnt_5m >= 3",
  "dependsOn": ["high_amt_flag", "user_trade_cnt_5m"],
  "returnType": "BOOLEAN"
}
```

如果是 Groovy：

```json
{
  "code": "R003",
  "engineType": "GROOVY",
  "expr": "def score = 0; if (amount >= 5000) score += 30; if (device_in_blacklist) score += 100; return score >= 100",
  "dependsOn": ["amount", "device_in_blacklist"],
  "returnType": "BOOLEAN"
}
```

这样快照既清晰，又便于 Flink 统一处理。

---

## 17.17 一条完整规则在 Flink 中的执行过程示例

我们用一个例子把所有内容串一下。

### 场景

`TRADE_RISK`

### 规则 R002

```latex
high_amt_flag == true && user_trade_cnt_5m >= 3
```

### 规则 R003

```groovy
def suspicious = false
if (device_bind_user_cnt_1h >= 4 && ['M','H'].contains(user_risk_level)) {
    suspicious = true
}
return suspicious
```

---

### 快照加载时

Flink 收到 snapshot 后：

1. 解析 `R002`，发现 `engineType=AVIATOR`
2. 编译表达式，生成 `AviatorConditionEvaluator`
3. 解析 `R003`，发现 `engineType=GROOVY`
4. 通过受控 GroovyClassLoader 编译脚本
5. 包装成 `GroovyConditionEvaluator`
6. 放入 `CompiledSceneRuntime.ruleInvokers`

---

### 事件到来时

一条交易事件进入，Flink 完成：

1. 计算 `user_trade_cnt_5m`
2. 查询 `user_risk_level`
3. 计算 `high_amt_flag`
4. 构建 `EvalContext`
5. 先执行 `R002`
6. 再执行 `R003`
7. 策略层汇总结果
8. 输出最终决策

你会发现：

- 表达式和 Groovy 都只是 `RuleInvoker` 内部的 evaluator
- Flink 主链路并没有被脚本绑架
- 规则执行是可控、可缓存、可解释的

这就是正确的引擎设计方式。

---

## 17.18 本章小结

这一章最核心的目标，是帮你建立“表达式与 Groovy 的正确边界”。

把最重要的内容收一下：

### 1）表达式引擎和 Groovy 本质不同

- 表达式是受限计算工具
- Groovy 是完整脚本语言

### 2）平台默认能力应以表达式为主

因为它：

- 更轻量
- 更可控
- 更可解释
- 更适合高频执行

### 3）Groovy 应作为高级扩展能力存在

适合：

- 少量复杂规则
- 少量复杂派生特征

但必须：

- 沙箱化
- 预编译
- 缓存化
- 限边界

### 4）表达式/Groovy 的边界是“上下文就绪后的纯逻辑计算”

它们不适合定义：

- Flink 状态模型
- keyBy
- timer
- window
- 外部副作用逻辑

### 5）控制平台负责校验与依赖分析，Flink 负责预编译与执行

两边职责必须清晰。

### 6）平台必须做统一执行器抽象

这样才能实现：

- 控制台仿真与线上一致
- 不同 engineType 统一接入
- 更好的缓存、监控和异常处理

---

## 17.19 下一章会讲什么

下一章进入：

## 第 18 章：Flink 中表达式/Groovy执行器的代码设计

下一章会进一步落到代码层，重点讲：

- 统一执行器接口怎么设计
- ExpressionInvoker / GroovyInvoker 怎么实现
- 快照加载时如何预编译
- processElement 中如何高效复用
- Binding / Context 注入怎么做
- 异常兜底和监控怎么设计
- 脚本沙箱和白名单怎么落代码

也就是说，下一章会把本章的理论边界，真正推进到：

> 代码层面到底应该怎么写。
