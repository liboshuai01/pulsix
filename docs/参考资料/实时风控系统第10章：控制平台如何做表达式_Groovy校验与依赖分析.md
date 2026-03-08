这一章进入控制平台在“发布前编译阶段”的关键能力： **表达式/Groovy 校验与依赖分析**。这一部分做得是否扎实，直接决定你后面 Flink 运行态是否稳定、可追溯、可回滚、可解释。

## 10.1 这一章解决什么问题

到第 9 章为止，你已经知道：

- 控制平台管理的是设计态配置
- 发布本质上是把设计态配置编译为运行时快照
- Flink 只应该消费运行时快照，而不是直接读取后台多张配置表

但现在会遇到一个更细、更关键的问题：

> 控制平台到底怎么保证“发布出去的快照是可执行、可解释、可恢复、不会把 Flink 弄挂”的？

这件事的核心不在页面，不在 CRUD，而在**发布前校验**。

你后面支持的这些能力：

- 规则表达式
- 派生特征表达式
- Groovy 高级规则
- 规则依赖分析
- 特征依赖分析
- 变量检查
- 循环依赖检测
- 风险函数限制

本质上都属于这一章要解决的问题。

如果这部分不做，后果会非常明显：

### 问题 1：坏配置直接进入运行时

例如：

- 规则写错变量名
- 派生特征依赖不存在的 feature
- Groovy 语法错误
- 规则引用了 String，但拿去做数值比较
- 策略引用了已停用规则

这些错误如果不在控制平台拦住，就会把问题推迟到 Flink 运行时暴露。

### 问题 2：线上才报错，代价极高

一旦问题在 Flink 才出现，会带来：

- 配置已进入广播链路
- 版本切换失败
- 某些并行实例本地编译异常
- 日志追踪困难
- 运维和策略人员无法快速判断问题在哪

### 问题 3：规则可以写，但平台不能解释

如果没有依赖分析，你就很难回答：

- 这条规则到底依赖哪些变量？
- 这次发布需要哪些 stream feature？
- 哪些 derived feature 应该先计算？
- 哪个变量来自事件字段，哪个来自 Redis，哪个来自 Flink State？

所以这一章的目标就是建立一条完整的**发布前编译校验链路**。

---

## 10.2 核心结论：发布前必须做“编译期校验”

这一章最核心的一句话是：

> **表达式和 Groovy 不应该等到 Flink 收到快照后再第一次发现问题，而应该在控制平台发布时就完成尽可能充分的编译期校验。**

这里的“编译期校验”不是说一定要生成 JVM 字节码，而是说：

- 在进入运行态之前
- 先做静态检查
- 先做依赖解析
- 先做类型检查
- 先做安全检查
- 先做可执行性验证

你可以把发布前校验理解为一个“轻编译器前端”。

它要完成的事情，类似传统编译器中的：

- 词法/语法检查
- 符号表建立
- 名称解析
- 依赖图构建
- 类型兼容检查
- 安全限制检查
- 输出中间表示

也就是说，控制平台并不只是一个配置中心，它还是一个：

> **规则与特征逻辑的静态分析器。**

---

## 10.3 为什么表达式和 Groovy 不能“先发布再说”

很多早期 Demo 系统会用一种非常危险的思路：

- 用户在页面里填表达式
- 后台只是简单保存
- 发布时不做深度检查
- Flink 收到快照后才编译/执行
- 出错了再看日志

这种方案一开始开发看起来很快，但实际上问题很大。

### 10.3.1 错误暴露时机太晚

错误越晚暴露，代价越高。

- 页面保存时报错：成本最低
- 发布时报错：还能接受
- Flink 收到快照时报错：已经影响运行态
- 事件执行时报错：最糟糕

所以正确策略是：

> 能在发布前发现的问题，绝不要拖到运行时。

### 10.3.2 运行态错误更难定位

例如一个 Groovy 脚本线上报错：

- 到底是语法错？
- 是类没开放？
- 是变量不存在？
- 是类型不兼容？
- 是某个函数调用被禁用？

如果这些都在线上 Flink 才报，你的排查链路会非常长。

### 10.3.3 运行态编译失败会影响版本切换

如果 Flink 端收到新快照后本地编译失败，那你必须处理：

- 是否拒绝切换版本
- 是否继续使用旧版本
- 是否记录失败原因
- 是否部分 task 已切换、部分未切换

虽然这些运行态兜底机制仍然需要，但它们应该是**最后一道防线**，不是主校验手段。

---

## 10.4 你到底要校验什么：校验目标的完整清单

控制平台在发布时，至少要做下面五类校验：

1. **语法校验**
2. **名称解析与变量存在性校验**
3. **依赖分析与循环依赖校验**
4. **类型兼容校验**
5. **安全边界校验**

下面逐一展开。

---

## 10.5 语法校验：先确保“写得对”

语法校验是最基础的一层。

### 10.5.1 表达式语法校验

无论你用：

- 自定义 DSL
- Aviator
- MVEL
- SpEL

都应该在发布前至少完成：

- 语法可解析
- 括号匹配
- 运算符合法
- 函数名合法
- 字面量格式合法

例如：

合法：

```latex
amount >= 5000 && user_trade_cnt_5m >= 3
```

非法：

```latex
amount >= && user_trade_cnt_5m
```

或者：

```latex
(user_trade_cnt_5m >= 3
```

这种错误属于“根本没法执行”的错误，必须第一时间拦住。

### 10.5.2 Groovy 语法校验

Groovy 比表达式更复杂，因为它支持：

- if/else
- 集合
- 方法调用
- 闭包
- import
- 类定义（原则上不建议开放）

所以你至少要做：

- 脚本能否被 Groovy 解析
- 是否存在明显语法错误
- 是否出现不允许的语法结构

例如：

```groovy
if (amount > 5000) {
  return true
}
return false
```

这是语法层面合法的。

但像这样：

```groovy
if (amount > 5000 {
  return true
}
```

就必须在发布前拦截。

---

## 10.6 名称解析与变量存在性校验：确保“引用得对”

仅仅语法正确，远远不够。

因为很多表达式虽然语法合法，但引用了系统里并不存在的变量。

例如：

```latex
user_trade_cnt_5mm >= 3
```

这个表达式语法完全没问题，但变量拼错了。

所以你必须建立一个**符号表（symbol table）** ，去检查变量是否存在。

### 10.6.1 变量可能来自哪里

在你的系统里，规则和派生特征里可引用的变量，一般来自五类来源：

#### 1）事件字段

例如：

- amount
- ip
- userId
- deviceId
- channel
- result

#### 2）stream feature

例如：

- user\_trade\_cnt\_5m
- user\_trade\_amt\_sum\_30m

#### 3）lookup feature

例如：

- device\_in\_blacklist
- user\_risk\_level

#### 4）derived feature

例如：

- high\_amt\_flag
- trade\_burst\_flag

#### 5）系统内置函数 / 常量

例如：

- now()
- isBlank(x)
- riskLevelEnum

### 10.6.2 建立统一符号表

发布时你应该先为某个 scene 构建统一变量视图，例如：

```latex
事件字段: amount, ip, userId, deviceId, result
stream feature: user_trade_cnt_5m, user_trade_amt_sum_30m
lookup feature: device_in_blacklist, user_risk_level
derived feature: high_amt_flag, trade_burst_flag
内置函数: isBlank(), containsAny(), now()
```

然后所有：

- derived feature 表达式
- rule 表达式
- Groovy 脚本

都在这个变量集合上做解析。

### 10.6.3 变量不存在时如何报错

错误信息一定要做得可读，不要只是抛一个 Exception。

建议输出：

- 规则/特征编号
- 错误类型
- 不存在的变量名
- 可选的相似变量建议

例如：

```latex
规则 R002 校验失败：变量 user_trade_cnt_5mm 不存在，是否想使用 user_trade_cnt_5m？
```

这会极大提升平台可用性。

---

## 10.7 依赖分析：确保“顺序对、图结构对”

这是整个发布编译的核心。

### 10.7.1 为什么要做依赖分析

因为你的规则和派生特征不是孤立的。

例如：

- `trade_burst_flag = user_trade_cnt_5m >= 3 && amount >= 5000`
- `R002 = trade_burst_flag == true && user_risk_level == 'H'`

这里显然有依赖链：

```latex
R002
 ├── trade_burst_flag
 │    ├── user_trade_cnt_5m
 │    └── amount
 └── user_risk_level
```

如果你不提前分析，后面就会出现很多问题：

- derived feature 先算还是后算？
- 哪些 stream feature 真正需要？
- 哪些 lookup 需要准备？
- 哪些 feature 根本没被规则用到？

### 10.7.2 依赖分析至少要覆盖两类对象

#### A. 派生特征依赖分析

用于回答：

- 这个 derived feature 依赖哪些字段/feature？
- 是否形成循环依赖？
- 它应该在什么顺序执行？

#### B. 规则依赖分析

用于回答：

- 这条 rule 依赖哪些变量？
- 它最终依赖哪些 stream feature / lookup feature？
- 它是否引用了尚未可用的 derived feature？

### 10.7.3 依赖分析的最终产物是什么

建议你在发布编译阶段得到下面这些中间结果：

- 每个 derived feature 的直接依赖集合
- 每条 rule 的直接依赖集合
- 每条 rule 的传递依赖集合
- derived feature 的拓扑顺序
- 当前 scene 最终需要的 stream feature 集合
- 当前 scene 最终需要的 lookup feature 集合

这对后面 Snapshot 生成和 Flink 执行计划都非常有用。

---

## 10.8 循环依赖校验：这是必须拦住的严重问题

派生特征一旦支持表达式，就天然可能出现循环依赖。

例如：

```latex
f1 = f2 && amount > 100
f2 = f1 || device_in_blacklist
```

或者更隐蔽一点：

```latex
f1 -> f2 -> f3 -> f1
```

如果不做循环依赖检测，运行时就会出现：

- 无法确定计算顺序
- 递归求值
- 栈溢出
- 值不稳定

### 10.8.1 如何检测循环依赖

本质上这是一个图问题。

- 节点：derived feature
- 边：A 依赖 B，则 A -> B

然后做：

- DFS 检环
- 或拓扑排序

如果无法完成拓扑排序，说明存在环。

### 10.8.2 错误信息应该怎么给

建议把环路径直接展示出来：

```latex
派生特征依赖校验失败：检测到循环依赖 high_amt_flag -> suspicious_flag -> risk_mix_flag -> high_amt_flag
```

这样用户才知道到底该改哪几个点。

---

## 10.9 类型兼容校验：确保“算得通”

这是很多平台容易忽略，但后期非常重要的能力。

表达式能解析、变量也都存在，不代表它一定能正确执行。

例如：

```latex
user_risk_level > 3
```

语法合法，变量也存在，但如果 `user_risk_level` 是 String，这就不合理。

再比如：

```latex
device_in_blacklist + 1
```

如果 `device_in_blacklist` 是 Boolean，这也不合理。

### 10.9.1 为什么做类型校验

因为它能提前发现大量低级但危险的问题：

- String 和 Number 比较
- Boolean 参与数值运算
- List 当 String 用
- null 默认值与类型不匹配
- 函数参数类型错误

### 10.9.2 类型系统不需要做得太重，但要有基本能力

对你的项目来说，建议至少支持下面这些类型：

- STRING
- LONG
- DECIMAL
- BOOLEAN
- DATETIME
- LIST\_STRING
- LIST\_LONG

然后为常见运算符定义兼容规则：

- `>` `<` `>=` `<=`：要求数值或时间可比较类型
- `==` `!=`：要求可比较
- `&&` `||`：要求两边都为 BOOLEAN
- `in`：右侧应为集合，左侧类型应与集合元素一致
- `+ - * /`：要求数值

### 10.9.3 类型校验的现实边界

要说明一点：

- 表达式引擎通常能做比较明确的类型分析
- Groovy 是动态语言，完全静态类型校验很难做到 100% 完整

所以你的目标不是把 Groovy 做成 Java 编译器，而是：

> **尽可能做基本类型约束和危险模式识别，剩余部分再交给运行时编译验证。**

---

## 10.10 冲突引用和命名问题：确保“不会互相覆盖”

除了依赖和类型，命名冲突也很重要。

### 10.10.1 常见冲突类型

#### 1）feature code 与事件字段重名

例如你有个事件字段叫 `amount`，又定义一个 derived feature 也叫 `amount`。

这会导致上下文注入时语义混乱。

#### 2）derived feature 与 lookup feature 重名

例如：

- lookup feature: `user_risk_level`
- derived feature: `user_risk_level`

这会直接造成覆盖。

#### 3）内置函数名被 feature 占用

例如你开放了 `isBlank()`，结果有人定义了 feature code 也叫 `isBlank`。

### 10.10.2 规则建议

建议控制平台明确规定：

- feature code 在 scene 内全局唯一
- rule code 在 scene 内全局唯一
- feature code 不得与基础字段重名
- feature code 不得与内置函数名重名
- derived feature 不能覆盖 lookup/stream feature

这是非常基础但非常必要的约束。

---

## 10.11 表达式校验与 Groovy 校验：为什么要区别对待

这是本章非常重要的一个边界。

> **表达式和 Groovy 都是“可配置逻辑”，但它们不是同一种东西，所以校验方式不能一样。**

### 10.11.1 表达式的特点

表达式通常具有：

- 语法简单
- 无副作用
- AST 相对容易分析
- 变量依赖容易提取
- 类型分析更容易做
- 安全性更容易控制

所以表达式非常适合做：

- 规则条件
- 派生特征
- 命中原因模板中的条件片段

### 10.11.2 Groovy 的特点

Groovy 具有：

- 语法灵活
- 能力强
- 支持复杂控制流
- AST 分析更复杂
- 安全风险更高
- 运行时开销更大

所以 Groovy 更适合做：

- 少量复杂规则
- 表达式难以描述的复杂派生逻辑

但它不能作为平台的一般能力泛滥开放。

### 10.11.3 校验策略差异

因此建议你采用不同校验策略：

#### 对表达式：尽量做强静态校验

- 语法
- 变量存在
- 依赖提取
- 类型兼容
- 函数名校验

#### 对 Groovy：做“安全可控的静态校验 + 编译试运行”

- 语法
- AST 白名单/黑名单检查
- import 检查
- 方法调用限制
- 类访问限制
- 变量存在性基本检查
- 运行前编译校验

这两者是不同层次的校验强度。

---

## 10.12 表达式依赖分析怎么做

现在更具体一点讲表达式校验链路。

### 10.12.1 建议流程

对于一条表达式（无论是 derived feature 还是 rule），建议至少走下面几步：

1. 解析表达式
2. 构建 AST
3. 提取变量引用
4. 提取函数调用
5. 校验变量是否存在
6. 校验函数是否允许
7. 校验类型兼容性
8. 输出依赖集合
9. 编译为可执行对象（可选在发布时预编译一次）

### 10.12.2 依赖提取结果示例

例如表达式：

```latex
user_trade_cnt_5m >= 3 && amount >= 5000 && device_in_blacklist == false
```

你应该能提取出：

- 直接依赖：
  - `user_trade_cnt_5m`
  - `amount`
  - `device_in_blacklist`

- 运算符：
  - `>=`
  - `&&`
  - `==`

- 变量类型需求：
  - `user_trade_cnt_5m` 应为 Number
  - `amount` 应为 Number
  - `device_in_blacklist` 应为 Boolean

### 10.12.3 依赖提取的价值

做完这一步后，你可以：

- 快速知道 rule 依赖什么
- 做发布时影响分析
- 生成 snapshot 的 dependsOn 字段
- 决定 Flink 运行时应先准备哪些数据

---

## 10.13 Groovy 校验怎么做：不能只看语法

Groovy 校验一定比表达式更谨慎。

### 10.13.1 Groovy 校验至少应分三层

#### 第一层：语法层

检查 Groovy 脚本能否正常 parse/compile。

#### 第二层：安全层

检查是否出现平台不允许的语法和 API：

- `System.exit`
- `new File(...)`
- 网络访问
- 线程操作
- 反射
- 任意 import
- ClassLoader 操作

#### 第三层：语义层

检查：

- 变量是否存在
- 返回值是否符合预期
- 是否包含不受支持的运行模式

### 10.13.2 建议采用“白名单优先”策略

Groovy 的安全设计，建议优先用白名单思路，而不是纯黑名单。

也就是：

- 只允许访问有限包和类型
- 只允许调用有限函数
- 只允许脚本访问上下文变量和少量 helper 方法

而不是先默认全开，再慢慢封禁危险 API。

### 10.13.3 建议限制的内容

至少建议限制：

- import 语句
- new 任意类
- 文件 IO
- 网络 IO
- 线程操作
- 睡眠阻塞
- System/Runtime 访问
- 反射调用
- 类加载器访问
- 静态全局状态修改

### 10.13.4 建议开放的能力

建议只开放：

- 读取 Binding/context 中的变量
- 使用少量 helper 工具类
- 基本 if/else
- 基本集合判断
- 少量内置函数

这就够做复杂规则了。

---

## 10.14 一个推荐的校验流水线设计

这里给你一版控制平台发布时的推荐流水线。

### 10.14.1 总体流程

```latex
加载设计态对象
   ↓
构建符号表（字段/feature/函数）
   ↓
校验 feature 定义
   ↓
校验 derived feature 表达式/Groovy
   ↓
构建 derived feature 依赖图并检测环
   ↓
校验 rule 表达式/Groovy
   ↓
校验 policy 引用和顺序
   ↓
汇总 scene 级依赖集合
   ↓
生成运行时快照
```

### 10.14.2 为什么要先校验 derived feature，再校验 rule

因为 rule 往往依赖 derived feature。

如果 derived feature 自身都没校验通过，rule 的校验基础就不成立。

所以推荐顺序是：

1. 先构建“基础可用变量集”
2. 校验 derived feature
3. 把 derived feature 合法结果加入变量集
4. 再校验 rule

这是一种逐层扩展符号表的过程。

---

## 10.15 控制平台中建议设计哪些类来承载校验能力

这一部分直接关系到 Spring Boot 代码怎么写。

建议至少抽出下面这些核心对象：

### 10.15.1 `ValidationResult`

用于统一承载校验结果。

建议字段：

- `success`
- `errors`
- `warnings`
- `dependencySummary`

### 10.15.2 `ValidationMessage`

建议字段：

- `level`（ERROR/WARN）
- `code`
- `targetType`（FEATURE/RULE/POLICY）
- `targetCode`
- `message`
- `detail`

### 10.15.3 `SymbolTable`

用于维护 scene 维度可引用变量和函数。

建议内容：

- 基础字段 -> 类型
- stream feature -> 类型
- lookup feature -> 类型
- derived feature -> 类型
- 内置函数 -> 签名

### 10.15.4 `DependencyGraph`

用于维护 derived feature / rule 的依赖关系。

### 10.15.5 `ExpressionAnalyzer`

用于：

- parse 表达式
- 提取变量
- 提取函数
- 做类型校验

### 10.15.6 `GroovyStaticChecker`

用于：

- 语法检查
- 安全检查
- 受限 AST 检查

### 10.15.7 `ScenePublishValidator`

作为发布总入口，串起整个 scene 的校验流程。

---

## 10.16 一套推荐的发布前校验伪代码

下面给你一版非常接近实际工程的伪代码。

```java
public ValidationResult validateForPublish(String sceneCode) {
    SceneDefinition scene = sceneRepo.load(sceneCode);
    List<EventFieldDef> fields = eventRepo.loadFields(sceneCode);
    List<FeatureDef> features = featureRepo.loadByScene(sceneCode);
    List<RuleDef> rules = ruleRepo.loadByScene(sceneCode);
    PolicyDef policy = policyRepo.loadActivePolicy(sceneCode);
    List<PolicyRuleRef> refs = policyRepo.loadRuleRefs(policy.getPolicyCode());

    SymbolTable symbolTable = SymbolTable.builder()
        .addBaseFields(fields)
        .addBuiltinFunctions(builtinRegistry)
        .build();

    ValidationCollector collector = new ValidationCollector();

    // 1. 先注册 stream / lookup feature 到符号表
    for (FeatureDef feature : features) {
        if (feature.isStream() || feature.isLookup()) {
            validateFeatureBasic(feature, collector);
            symbolTable.addFeature(feature.getFeatureCode(), feature.getValueType());
        }
    }

    // 2. 校验 derived feature，但先不立即写入运行态
    DependencyGraph graph = new DependencyGraph();
    List<FeatureDef> derivedFeatures = features.stream()
        .filter(FeatureDef::isDerived)
        .toList();

    for (FeatureDef feature : derivedFeatures) {
        AnalysisResult ar = analyzeFeatureExpr(feature, symbolTable);
        collector.addAll(ar.messages());
        graph.addNode(feature.getFeatureCode());
        for (String dep : ar.dependencies()) {
            if (isDerivedFeature(dep, derivedFeatures)) {
                graph.addEdge(feature.getFeatureCode(), dep);
            }
        }
    }

    // 3. 检测 derived feature 是否存在环
    collector.addAll(graph.checkCycleMessages());

    // 4. 只有 derived feature 全部通过后，按拓扑顺序加入符号表
    List<String> topo = graph.topologicalOrder();
    for (String featureCode : topo) {
        FeatureDef f = findFeature(featureCode, derivedFeatures);
        symbolTable.addFeature(f.getFeatureCode(), f.getValueType());
    }

    // 5. 校验 rules
    for (RuleDef rule : rules) {
        AnalysisResult ar = analyzeRule(rule, symbolTable);
        collector.addAll(ar.messages());
    }

    // 6. 校验 policy
    validatePolicy(policy, refs, rules, collector);

    return collector.toResult();
}
```

这段代码体现的核心思想是：

- 先建符号表
- 先校验 feature
- 再建依赖图
- 再校验 rule
- 最后校验 policy

这个顺序是非常合理的。

---

## 10.17 需要不要把依赖分析结果落库

这个问题没有绝对标准，但我建议你这样做：

### 10.17.1 一期建议：不强制落设计态表

即：

- 发布时实时分析
- 把依赖结果直接放入 snapshot
- 同时返回到发布结果页面

这就够用了。

### 10.17.2 二期如果想增强，可考虑落库

适合增加：

- `feature_dependency`
- `rule_dependency`

场景：

- 做可视化依赖图
- 做影响分析
- 做增量发布评估

但一期没有必要为了“显得更全”就先把它做重。

---

## 10.18 页面交互层面应该怎样反馈校验结果

一个好平台，不只是后台做了校验，还要把错误清楚地反馈给用户。

### 10.18.1 建议区分 ERROR 与 WARN

#### ERROR

阻止发布，例如：

- 语法错误
- 变量不存在
- 循环依赖
- 策略引用缺失 rule
- 类型严重不兼容

#### WARN

允许发布但建议用户注意，例如：

- 某个 feature 定义了但未被任何 rule 使用
- 某个 rule 优先级重复
- 某个 derived feature 过于复杂
- 某个 Groovy 脚本行数过长

### 10.18.2 建议支持“定位到对象”

例如发布失败后，前端能直接定位到：

- 规则 R003
- 派生特征 F\_D\_001
- 策略 POLICY\_TRADE\_MAIN

而不是只返回一串通用报错。

### 10.18.3 建议支持“影响范围展示”

例如：

- 本次发布最终涉及 3 个 stream feature
- 2 个 lookup feature
- 5 条 rule
- 1 个 policy

这对于策略人员理解“我到底发布了什么”很有帮助。

---

## 10.19 为什么编译期校验对 Flink 端意义巨大

这章虽然讲的是控制平台，但它其实直接决定 Flink 端的复杂度和稳定性。

### 10.19.1 能显著减少运行时异常

提前拦住：

- 变量不存在
- 语法错误
- 循环依赖
- 安全问题

Flink 端就只需要处理：

- 本地编译失败的极端兜底
- Redis 超时
- 运行时数据异常

而不是大量配置错误。

### 10.19.2 能让快照更“接近执行计划”

因为依赖分析已经做好，所以 snapshot 里可以直接带：

- `dependsOn`
- `requiredStreamFeatures`
- `requiredLookupFeatures`
- derived feature 执行顺序

这样 Flink 会更轻。

### 10.19.3 能让仿真与线上一致性更容易保证

因为：

- 控制平台的仿真用的是同一套校验和依赖分析结果
- Flink 的运行时则用编译后的 snapshot

这两边的语义会更一致。

---

## 10.20 Groovy 安全边界：这一章必须先定原则

虽然真正 Groovy 执行器和沙箱会在后面章节展开，但在控制平台校验阶段，你必须先定几个原则。

### 原则 1：Groovy 是高级能力，不是默认能力

一期建议：

- 规则默认用表达式
- 派生特征默认用表达式
- 只有少数复杂规则开放 Groovy

### 原则 2：Groovy 只允许“上下文计算”，不允许“系统操作”

允许：

- 读取变量
- 调 helper
- 做逻辑判断

不允许：

- IO
- 线程
- 反射
- 网络
- 文件
- 系统调用

### 原则 3：Groovy 校验必须分为“控制平台预检查 + Flink 本地编译兜底”

也就是说：

- 控制平台负责绝大部分静态问题
- Flink 收到快照后仍要做一次本地编译验证
- 如果 Flink 编译失败，应拒绝切换版本并保留旧版本

这是双保险。

---

## 10.21 一期项目中我建议你做到什么程度

结合你目前的目标和 6 个月周期，我建议第 10 章对应的实现深度如下：

### 一期必须做到

- 表达式语法校验
- 变量存在性校验
- feature/rule/policy 基本完整性检查
- derived feature 依赖分析
- 循环依赖检测
- 规则依赖提取
- 基础类型兼容检查
- Groovy 基本语法检查
- Groovy 安全黑/白名单检查
- 发布失败时给出明确错误信息

### 一期可以适度简化

- Groovy 完全静态类型推导
- 复杂函数重载解析
- 超复杂依赖图可视化
- 代码级别的 Groovy AST 深度优化

### 二期可增强

- 依赖可视化
- 影响范围分析
- 规则复杂度评分
- Groovy 更细粒度 AST 安全策略
- 增量编译优化

这个范围对个人项目来说是很合理的。

---

## 10.22 本章小结

这一章的核心是在回答：

> **控制平台为什么必须在发布前做表达式/Groovy 校验与依赖分析，以及应该做到什么程度。**

把重点收一下：

### 1）发布前必须做编译期校验

不能把错误推迟到 Flink 运行时。

### 2）校验至少包含五类

- 语法
- 变量存在性
- 依赖分析
- 类型兼容
- 安全边界

### 3）derived feature 和 rule 都要做依赖分析

尤其要做循环依赖检测和拓扑排序。

### 4）表达式和 Groovy 不能同等对待

- 表达式适合强静态校验
- Groovy 适合受控静态校验 + 编译兜底

### 5）控制平台的校验结果会直接影响快照质量

校验越充分，Flink 运行态越轻、越稳、越可解释。

### 6）一期项目的目标不是把脚本系统做成语言编译器

而是做出一条清晰、可控、足够实用的发布前校验链路。

---

## 10.23 下一章会讲什么

下一章进入第四部分的开篇：

**第 11 章：Flink 在实时风控系统中的角色与适用边界**

会重点回答：

- 为什么实时风控适合用 Flink
- Flink 能做什么、不能做什么
- 哪些能力适合动态化到 Flink
- 为什么 Flink 更适合执行“运行时计划”，而不是执行“无限制任意脚本”
- 你应该如何把 Flink 看成“状态计算引擎”，而不是“窗口统计工具”

这一章讲完以后，你会从控制平台正式切到运行时引擎视角。
