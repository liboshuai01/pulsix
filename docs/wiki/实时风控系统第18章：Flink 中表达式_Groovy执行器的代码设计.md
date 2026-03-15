## 18.1 本章解决什么问题

前面几章我们已经把这些关键问题讲清楚了：

- 控制平台为什么要发布运行时快照
- Flink 如何接收、存储和切换快照
- stream feature、lookup feature、derived feature 如何分层
- 规则与策略在 Flink 中怎么执行
- 表达式引擎和 Groovy 的边界应该怎么划

但如果要真正把系统写成代码，你很快会碰到一个最实际的问题：

> **这些表达式和 Groovy，在代码层到底应该怎么抽象、怎么编译、怎么缓存、怎么执行？**

也就是：

- 执行器接口怎么设计
- 编译和执行怎么分离
- `EvalContext` 应该长什么样
- Flink 在收到快照后到底怎么把表达式/Groovy 编译成可执行对象
- 运行时如何高效复用这些编译结果
- 如何做异常兜底、脚本隔离、类加载器管理
- 为什么不能每条事件都现编译脚本
- 如何让控制平台仿真和 Flink 线上执行共用一套内核

所以这一章的重点，不再是概念，而是：

> **把“表达式 / Groovy 执行”真正落到代码结构、接口设计和运行时实现上。**

这一章讲完之后，你应该能够对下面这些问题有非常清晰的实现感：

- `pulsix-kernel` 这个模块该怎么拆
- Flink 算子里要缓存什么对象
- 表达式执行器和 Groovy 执行器如何统一抽象
- 如何做到“版本切换时编译一次，事件执行时反复复用”
- 如何把风险控制在一个合理边界内

---

## 18.2 先说一个总原则：编译和执行必须分离

这是这一章最核心的工程原则。

> **快照加载时负责编译，事件处理时只负责执行。**

为什么必须这样做？

因为在实时风控系统里，事件量可能非常大。即使你个人项目规模不算企业级，也会希望系统能够稳定支撑：

- 每秒几百到几千事件
- 决策延迟在几十到几百毫秒内
- 规则和派生特征可热更新
- 规则命中结果可追溯

如果你在每条事件进入时都做：

- 解析表达式
- 编译 Aviator / DSL
- 编译 Groovy
- 分析依赖
- 动态生成类

那性能几乎一定不可接受。

所以正确做法一定是：

### 第一步：快照到达时

- 解析快照
- 构建执行计划
- 预编译表达式
- 预编译 Groovy
- 把这些编译结果缓存起来

### 第二步：事件执行时

- 构建 `EvalContext`
- 直接调用已编译好的执行器
- 拿到结果

这本质上就是：

> **compile once, execute many**

你后面所有代码设计，都必须围绕这个原则展开。

---

## 18.3 执行器代码设计的目标

如果从工程实现角度来看，表达式/Groovy 执行器设计应该满足下面几个目标。

### 18.3.1 统一抽象

无论底层是：

- Aviator
- 自定义 DSL
- Groovy

对上层规则引擎来说，最好都能统一成类似接口：

- 输入： `EvalContext`
- 输出：执行结果
- 失败：统一异常

这样规则层和策略层就不会被具体脚本引擎绑死。

---

### 18.3.2 可预编译

执行器必须支持“先编译，再执行”。

也就是说：

- 表达式不是字符串直接到处传
- Groovy 也不是运行时随手 `eval`
- 而是变成“已编译的可执行对象”

---

### 18.3.3 可缓存

不同 scene、不同 version 的运行态对象要能缓存。

例如：

- `TRADE_RISK:v12`
- `LOGIN_RISK:v5`

每个版本对应一套独立执行器集合。

---

### 18.3.4 可隔离

不同版本之间不能互相污染，尤其是 Groovy。

因为：

- 旧版本脚本可能仍在运行
- 新版本脚本已经编译完成
- 两个版本不能共享随意的全局状态

---

### 18.3.5 可追溯

规则执行失败、脚本异常、变量缺失，都应该能定位到：

- sceneCode
- version
- ruleCode / featureCode
- engineType
- 原始表达式或脚本摘要

---

### 18.3.6 可控、安全

特别是 Groovy，必须避免成为“任意代码执行入口”。

至少要做到：

- 限制 import
- 限制反射
- 禁止文件/网络/进程操作
- 不允许任意拿 Spring Bean
- 控制执行超时与异常策略

---

## 18.4 推荐的模块划分

为了让控制平台仿真和 Flink 线上执行共用同一套逻辑，我建议你把执行器相关代码放到统一执行内核模块中，并且让目录结构贴合当前 `pulsix` 仓库。

推荐模块结构如下：

```latex
pulsix/
├── pulsix-dependencies/              # BOM / 版本对齐
├── pulsix-framework/
│   ├── pulsix-common/                # 通用 DTO / 枚举 / 工具 / CommonApi
│   ├── pulsix-kernel/                # 统一执行内核（重点）
│   └── pulsix-spring-boot-starter-*  # 各类基础组件
├── pulsix-server/                    # Spring Boot 启动器
├── pulsix-module-system/             # 用户、权限、租户、菜单、审计
├── pulsix-module-infra/              # 配置、文件、任务、监控、基础日志
├── pulsix-module-risk/               # 控制平台风控主业务
├── pulsix-engine/                    # Flink 引擎
├── pulsix-ui/                        # 前端
└── docs/
```

其中最关键的是 `pulsix-kernel`。

它应该同时被：

- `pulsix-module-risk` 的仿真服务使用
- `pulsix-engine` 的 Flink 决策链路使用

这样才能保证：

> **同一份快照、同一份上下文、同一套执行器，在仿真和线上得到一致结果。**

这是系统可信度的关键。

---

## 18.5 核心对象设计总览

我建议你先把执行器相关的核心对象分成下面这几层：

1. **可序列化模型层**
   - `SceneSnapshot`
   - `RuleSpec`
   - `DerivedFeatureSpec`
   - `PolicySpec`

2. **运行时上下文层**
   - `EvalContext`
   - `EvalMeta`

3. **执行器抽象层**
   - `CompiledExecutable`
   - `RuleInvoker`
   - `DerivedFeatureInvoker`
   - `ScriptEngineAdapter`

4. **运行时编译产物层**
   - `CompiledSceneRuntime`
   - `CompiledRule`
   - `CompiledDerivedFeature`

5. **策略执行层**
   - `PolicyExecutor`
   - `FirstHitPolicyExecutor`
   - `ScoreCardPolicyExecutor`

6. **辅助层**
   - `HitReasonRenderer`
   - `DependencyResolver`
   - `ExecutionExceptionTranslator`

这样分层以后，你的代码就会很清楚：

- 快照是快照
- 上下文是上下文
- 编译产物是编译产物
- 执行器是执行器
- 策略是策略

而不是所有逻辑都塞在一个 `RuleEngineService` 里。

---

## 18.6 EvalContext 应该怎么设计

不管底层你用什么表达式引擎，最终都离不开一个问题：

> **执行器接收的上下文对象长什么样？**

我建议你设计一个统一的 `EvalContext`，作为表达式和 Groovy 的统一输入。

### 18.6.1 设计目标

`EvalContext` 应该满足：

- 能容纳基础事件字段
- 能容纳流式特征
- 能容纳 lookup 特征
- 能容纳派生特征
- 能容纳一些系统元信息
- 既方便表达式执行，也方便命中原因渲染

---

### 18.6.2 推荐结构

```java
public final class EvalContext {

    private final String sceneCode;
    private final long version;
    private final String eventId;
    private final String traceId;
    private final long eventTime;

    /**
     * 统一变量视图：
     * 基础字段 + stream feature + lookup feature + derived feature
     */
    private final Map<String, Object> variables;

    /**
     * 调试与追溯用的扩展元信息
     */
    private final Map<String, Object> meta;

    public EvalContext(String sceneCode,
                       long version,
                       String eventId,
                       String traceId,
                       long eventTime,
                       Map<String, Object> variables,
                       Map<String, Object> meta) {
        this.sceneCode = sceneCode;
        this.version = version;
        this.eventId = eventId;
        this.traceId = traceId;
        this.eventTime = eventTime;
        this.variables = variables;
        this.meta = meta;
    }

    public Object get(String name) {
        return variables.get(name);
    }

    public <T> T getAs(String name, Class<T> clazz) {
        Object value = variables.get(name);
        if (value == null) {
            return null;
        }
        return clazz.cast(value);
    }

    public boolean contains(String name) {
        return variables.containsKey(name);
    }

    public Map<String, Object> asMap() {
        return variables;
    }

    public Map<String, Object> meta() {
        return meta;
    }

    public String getSceneCode() {
        return sceneCode;
    }

    public long getVersion() {
        return version;
    }

    public String getEventId() {
        return eventId;
    }

    public String getTraceId() {
        return traceId;
    }

    public long getEventTime() {
        return eventTime;
    }
}
```

---

### 18.6.3 为什么推荐统一变量 Map

因为从执行器角度看，不论是：

- Aviator
- DSL
- Groovy

它们最容易适配的输入形式，通常都是：

```java
Map<String, Object>
```

这样你可以统一变量命名，例如：

- `amount`
- `user_trade_cnt_5m`
- `device_in_blacklist`
- `user_risk_level`
- `high_amt_flag`

规则表达式和 Groovy 都围绕这些变量运行。

---

### 18.6.4 为什么不要把上下文做得过于复杂

有些人会想做很多层级对象：

- `ctx.event.user.id`
- `ctx.features.stream.userTradeCnt5m`
- `ctx.lookup.device.blackList`

这当然也能做，但它会明显增加：

- 表达式复杂度
- Groovy 脚本复杂度
- 模板渲染复杂度
- 变量依赖分析复杂度

对于你的项目，我建议先做成：

> **统一拍平成变量表**

后期如果要增强类型系统，再做扩展。

---

## 18.7 执行器的统一接口设计

接下来是最关键的代码抽象。

### 18.7.1 最底层：已编译可执行对象接口

```java
public interface CompiledExecutable {

    /**
     * 执行返回原始结果，可以是 Boolean / Number / String / 任意对象
     */
    Object execute(EvalContext context) throws ScriptExecutionException;

    /**
     * 当前编译对象对应的引擎类型
     */
    String engineType();

    /**
     * 原始脚本/表达式摘要，用于日志追溯
     */
    String sourceSummary();
}
```

这个接口非常基础。

它表达的思想是：

> 不管底层是 Aviator 还是 Groovy，编译完以后，对上层而言都只是一个 `execute(context)` 的对象。

---

### 18.7.2 规则执行器接口

规则执行和普通表达式不同，它需要返回更丰富的结构。

```java
public interface RuleInvoker {

    RuleHitResult evaluate(EvalContext context);

    String ruleCode();

    int priority();
}
```

`RuleHitResult` 建议这样设计：

```java
public final class RuleHitResult {

    private final String ruleCode;
    private final boolean hit;
    private final String hitAction;
    private final Integer score;
    private final String hitReason;
    private final long costNanos;
    private final Map<String, Object> debugInfo;

    // constructor / getters
}
```

---

### 18.7.3 派生特征执行器接口

```java
public interface DerivedFeatureInvoker {

    DerivedFeatureResult evaluate(EvalContext context);

    String featureCode();
}
```

返回值：

```java
public final class DerivedFeatureResult {

    private final String featureCode;
    private final Object value;
    private final long costNanos;

    // constructor / getters
}
```

---

### 18.7.4 为什么规则和派生特征要分接口

虽然二者底层都可能由表达式/Groovy 支撑，但它们的语义不同：

- **派生特征**：输出一个变量值
- **规则**：输出命中结果和动作建议

如果强行做成同一个接口，最后上层代码会很别扭。

所以推荐：

- 底层 `CompiledExecutable` 统一
- 上层 `RuleInvoker` / `DerivedFeatureInvoker` 分开

---

## 18.8 ScriptEngineAdapter：屏蔽具体引擎差异

这是整个执行器体系的关键抽象。

### 18.8.1 接口设计

```java
public interface ScriptEngineAdapter {

    String engineType();

    CompiledExecutable compile(ExecutableSpec spec) throws ScriptCompileException;
}
```

`ExecutableSpec` 可以定义为：

```java
public final class ExecutableSpec {

    private final String bizCode;      // ruleCode / featureCode
    private final String sceneCode;
    private final long version;
    private final String engineType;
    private final String source;
    private final Set<String> dependsOn;
    private final String returnType;

    // constructor / getters
}
```

---

### 18.8.2 这个接口的意义

它把上层逻辑和底层脚本引擎解耦了。

上层编译器只需要知道：

- 这个 spec 是 AVIATOR 还是 GROOVY
- 找到对应 adapter
- 调用 `compile(spec)`

至于底层怎么编译、怎么执行，由 adapter 自己负责。

这样后续你想扩展：

- Aviator
- MVEL
- 自定义 DSL
- Janino

都比较容易。

---

## 18.9 Aviator 执行器的实现思路

Aviator 这类表达式引擎通常很适合：

- 布尔规则判断
- 派生特征表达式
- 简单数值运算
- 集合包含判断

### 18.9.1 推荐实现骨架

```java
public final class AviatorEngineAdapter implements ScriptEngineAdapter {

    @Override
    public String engineType() {
        return "AVIATOR";
    }

    @Override
    public CompiledExecutable compile(ExecutableSpec spec) throws ScriptCompileException {
        try {
            Expression expression = AviatorEvaluator.compile(spec.getSource(), true);
            return new AviatorCompiledExecutable(spec, expression);
        } catch (Exception e) {
            throw new ScriptCompileException(
                    spec.getSceneCode(),
                    spec.getVersion(),
                    spec.getBizCode(),
                    spec.getEngineType(),
                    "Aviator compile failed", e);
        }
    }
}
```

已编译对象：

```java
public final class AviatorCompiledExecutable implements CompiledExecutable {

    private final ExecutableSpec spec;
    private final Expression expression;

    public AviatorCompiledExecutable(ExecutableSpec spec, Expression expression) {
        this.spec = spec;
        this.expression = expression;
    }

    @Override
    public Object execute(EvalContext context) throws ScriptExecutionException {
        try {
            return expression.execute(context.asMap());
        } catch (Exception e) {
            throw new ScriptExecutionException(
                    context.getSceneCode(),
                    context.getVersion(),
                    spec.getBizCode(),
                    spec.getEngineType(),
                    "Aviator execute failed", e);
        }
    }

    @Override
    public String engineType() {
        return spec.getEngineType();
    }

    @Override
    public String sourceSummary() {
        return spec.getSource();
    }
}
```

---

### 18.9.2 Aviator 适合做什么

推荐让 Aviator 负责：

- rule.whenExpr
- derivedFeature.expr
- hitReason 中某些简单变量模板预解析（可选）

不推荐用它做：

- 复杂状态机
- 动态外部资源访问
- 任意副作用逻辑

它的定位应该始终是：

> **上下文就绪后的纯函数判断或派生。**

---

## 18.10 Groovy 执行器的实现思路

Groovy 的能力更强，但风险也更高。

所以我建议你：

> **Groovy 在你的系统里应该是“高级规则扩展能力”，而不是默认路径。**

### 18.10.1 推荐的接口方式

不要让用户直接写任意脚本文件、任意类结构。更稳妥的方式是：

- 平台规定一个固定的脚本入口方法签名
- 用户只写脚本主体或固定返回逻辑
- 编译时由平台包裹成受控类

例如你可以定义：

```java
public interface GroovyRuleScript {
    Object execute(Map<String, Object> ctx);
}
```

然后把用户脚本包装为：

```groovy
class UserScript_xxx implements GroovyRuleScript {
    Object execute(Map<String, Object> ctx) {
        def amount = ctx.get("amount")
        def user_trade_cnt_5m = ctx.get("user_trade_cnt_5m")
        def user_risk_level = ctx.get("user_risk_level")
        return amount >= 5000 && user_trade_cnt_5m >= 3 && ['M','H'].contains(user_risk_level)
    }
}
```

这样你就能控制：

- 方法入口
- 变量来源
- 返回类型预期
- 可注入内容

---

### 18.10.2 推荐实现骨架

```java
public final class GroovyEngineAdapter implements ScriptEngineAdapter {

    private final GroovySandboxFactory sandboxFactory;

    public GroovyEngineAdapter(GroovySandboxFactory sandboxFactory) {
        this.sandboxFactory = sandboxFactory;
    }

    @Override
    public String engineType() {
        return "GROOVY";
    }

    @Override
    public CompiledExecutable compile(ExecutableSpec spec) throws ScriptCompileException {
        GroovySandbox sandbox = sandboxFactory.create(spec);
        try {
            Class<?> scriptClass = sandbox.compile(spec.getSource());
            return new GroovyCompiledExecutable(spec, sandbox, scriptClass);
        } catch (Exception e) {
            sandbox.closeQuietly();
            throw new ScriptCompileException(
                    spec.getSceneCode(),
                    spec.getVersion(),
                    spec.getBizCode(),
                    spec.getEngineType(),
                    "Groovy compile failed", e);
        }
    }
}
```

已编译对象：

```java
public final class GroovyCompiledExecutable implements CompiledExecutable, AutoCloseable {

    private final ExecutableSpec spec;
    private final GroovySandbox sandbox;
    private final Class<?> scriptClass;

    public GroovyCompiledExecutable(ExecutableSpec spec,
                                    GroovySandbox sandbox,
                                    Class<?> scriptClass) {
        this.spec = spec;
        this.sandbox = sandbox;
        this.scriptClass = scriptClass;
    }

    @Override
    public Object execute(EvalContext context) throws ScriptExecutionException {
        try {
            GroovyRuleScript script = (GroovyRuleScript) scriptClass.getDeclaredConstructor().newInstance();
            return script.execute(context.asMap());
        } catch (Exception e) {
            throw new ScriptExecutionException(
                    context.getSceneCode(),
                    context.getVersion(),
                    spec.getBizCode(),
                    spec.getEngineType(),
                    "Groovy execute failed", e);
        }
    }

    @Override
    public String engineType() {
        return spec.getEngineType();
    }

    @Override
    public String sourceSummary() {
        return spec.getSource();
    }

    @Override
    public void close() {
        sandbox.closeQuietly();
    }
}
```

---

### 18.10.3 Groovy 的几个实现重点

#### 重点 1：不要每条事件 new ClassLoader

编译阶段创建版本级 ClassLoader 即可，执行阶段只 new 脚本实例或使用固定对象。

#### 重点 2：不要把 `Class<?>` 放进 Broadcast State

Broadcast State 应该只放可恢复快照。

#### 重点 3：旧版本淘汰时要释放 ClassLoader 引用

否则容易 metaspace 增长。

#### 重点 4：脚本必须是纯函数风格

输入 `ctx`，输出结果，不允许副作用。

---

## 18.11 RuleInvoker 的推荐实现方式

上面有了通用 `CompiledExecutable`，现在来看规则执行器。

规则执行器除了调用表达式/Groovy，还要负责：

- 结果转 boolean
- 生成 hit reason
- 组装 `RuleHitResult`
- 记录耗时

推荐实现：

```java
public final class DefaultRuleInvoker implements RuleInvoker {

    private final RuleSpec ruleSpec;
    private final CompiledExecutable executable;
    private final HitReasonRenderer hitReasonRenderer;

    public DefaultRuleInvoker(RuleSpec ruleSpec,
                              CompiledExecutable executable,
                              HitReasonRenderer hitReasonRenderer) {
        this.ruleSpec = ruleSpec;
        this.executable = executable;
        this.hitReasonRenderer = hitReasonRenderer;
    }

    @Override
    public RuleHitResult evaluate(EvalContext context) {
        long start = System.nanoTime();
        try {
            Object raw = executable.execute(context);
            boolean hit = toBoolean(raw);
            String reason = hit
                    ? hitReasonRenderer.render(ruleSpec.getHitReasonTemplate(), context)
                    : null;
            return new RuleHitResult(
                    ruleSpec.getRuleCode(),
                    hit,
                    hit ? ruleSpec.getHitAction() : null,
                    hit ? ruleSpec.getRiskScore() : null,
                    reason,
                    System.nanoTime() - start,
                    Map.of("engineType", executable.engineType())
            );
        } catch (Exception e) {
            throw RuleExecutionRuntimeException.from(ruleSpec, context, e);
        }
    }

    @Override
    public String ruleCode() {
        return ruleSpec.getRuleCode();
    }

    @Override
    public int priority() {
        return ruleSpec.getPriority();
    }

    private boolean toBoolean(Object raw) {
        if (raw instanceof Boolean b) {
            return b;
        }
        if (raw == null) {
            return false;
        }
        if (raw instanceof Number n) {
            return n.intValue() != 0;
        }
        return Boolean.parseBoolean(String.valueOf(raw));
    }
}
```

---

## 18.12 DerivedFeatureInvoker 的推荐实现方式

派生特征和规则的差异在于：

- 规则关心 hit / not hit
- 派生特征关心“产出什么值”

实现会更简单：

```java
public final class DefaultDerivedFeatureInvoker implements DerivedFeatureInvoker {

    private final DerivedFeatureSpec spec;
    private final CompiledExecutable executable;

    public DefaultDerivedFeatureInvoker(DerivedFeatureSpec spec,
                                        CompiledExecutable executable) {
        this.spec = spec;
        this.executable = executable;
    }

    @Override
    public DerivedFeatureResult evaluate(EvalContext context) {
        long start = System.nanoTime();
        try {
            Object value = executable.execute(context);
            return new DerivedFeatureResult(
                    spec.getFeatureCode(),
                    value,
                    System.nanoTime() - start
            );
        } catch (Exception e) {
            throw DerivedFeatureExecutionException.from(spec, context, e);
        }
    }

    @Override
    public String featureCode() {
        return spec.getFeatureCode();
    }
}
```

---

## 18.13 HitReasonRenderer 的设计

规则命中以后，通常还要输出命中原因，例如：

- `设备命中黑名单`
- `用户5分钟交易次数={user_trade_cnt_5m}, 当前金额={amount}`

推荐抽一个专门渲染器，不要把模板替换逻辑写进规则执行器里。

### 18.13.1 接口

```java
public interface HitReasonRenderer {
    String render(String template, EvalContext context);
}
```

### 18.13.2 简单实现

可以先做 `${var}` 或 `{var}` 替换。

```java
public final class SimplePlaceholderHitReasonRenderer implements HitReasonRenderer {

    private static final Pattern P = Pattern.compile("\\{([a-zA-Z0-9_]+)\\}");

    @Override
    public String render(String template, EvalContext context) {
        if (template == null || template.isBlank()) {
            return null;
        }
        Matcher matcher = P.matcher(template);
        StringBuffer sb = new StringBuffer();
        while (matcher.find()) {
            String key = matcher.group(1);
            Object value = context.get(key);
            matcher.appendReplacement(sb, Matcher.quoteReplacement(String.valueOf(value)));
        }
        matcher.appendTail(sb);
        return sb.toString();
    }
}
```

这个功能虽然小，但对：

- 日志展示
- 仿真结果展示
- 业务解释性

都非常重要。

---

## 18.14 CompiledSceneRuntime 的设计

这是连接“快照”和“事件执行”的核心对象。

### 18.14.1 它是什么

你可以把它理解为：

> **某个 scene 某个 version 对应的一整套已编译运行时执行计划。**

它不是可序列化快照，而是本地运行时对象。

### 18.14.2 推荐结构

```java
public final class CompiledSceneRuntime implements AutoCloseable {

    private final String sceneCode;
    private final long version;

    private final SceneSnapshot snapshot;

    private final List<StreamFeaturePlan> streamFeaturePlans;
    private final List<LookupFeaturePlan> lookupFeaturePlans;

    private final List<DerivedFeatureInvoker> derivedFeatureInvokersInOrder;
    private final List<RuleInvoker> orderedRuleInvokers;

    private final PolicyExecutor policyExecutor;

    private final Set<String> requiredVariables;
    private final RuntimeResourceHolder resourceHolder;

    public CompiledSceneRuntime(String sceneCode,
                                long version,
                                SceneSnapshot snapshot,
                                List<StreamFeaturePlan> streamFeaturePlans,
                                List<LookupFeaturePlan> lookupFeaturePlans,
                                List<DerivedFeatureInvoker> derivedFeatureInvokersInOrder,
                                List<RuleInvoker> orderedRuleInvokers,
                                PolicyExecutor policyExecutor,
                                Set<String> requiredVariables,
                                RuntimeResourceHolder resourceHolder) {
        this.sceneCode = sceneCode;
        this.version = version;
        this.snapshot = snapshot;
        this.streamFeaturePlans = streamFeaturePlans;
        this.lookupFeaturePlans = lookupFeaturePlans;
        this.derivedFeatureInvokersInOrder = derivedFeatureInvokersInOrder;
        this.orderedRuleInvokers = orderedRuleInvokers;
        this.policyExecutor = policyExecutor;
        this.requiredVariables = requiredVariables;
        this.resourceHolder = resourceHolder;
    }

    // getters

    @Override
    public void close() {
        resourceHolder.closeQuietly();
    }
}
```

---

### 18.14.3 为什么它一定要和 SceneSnapshot 分开

因为：

- `SceneSnapshot` 适合放 Broadcast State
- `CompiledSceneRuntime` 里可能有不可序列化对象：
  - Aviator Expression
  - Groovy Class
  - GroovyClassLoader
  - 模板编译缓存

所以二者职责完全不同：

- 快照：可恢复配置
- 运行态对象：本地执行缓存

---

## 18.15 RuntimeCompiler：快照到运行时对象的编译器

这是 Flink `processBroadcastElement()` 里的关键组件。

### 18.15.1 接口建议

```java
public interface RuntimeCompiler {
    CompiledSceneRuntime compile(SceneSnapshot snapshot) throws RuntimeCompileException;
}
```

### 18.15.2 它负责什么

- 解析快照
- 为 derived feature 选择引擎 adapter
- 为 rule 选择引擎 adapter
- 编译表达式 / Groovy
- 构建 `RuleInvoker`
- 构建 `DerivedFeatureInvoker`
- 构建 `PolicyExecutor`
- 组装 `CompiledSceneRuntime`

---

### 18.15.3 推荐实现伪代码

```java
public final class DefaultRuntimeCompiler implements RuntimeCompiler {

    private final Map<String, ScriptEngineAdapter> engineAdapters;
    private final HitReasonRenderer hitReasonRenderer;
    private final PolicyExecutorFactory policyExecutorFactory;

    @Override
    public CompiledSceneRuntime compile(SceneSnapshot snapshot) {
        RuntimeResourceHolder resourceHolder = new RuntimeResourceHolder();
        try {
            List<DerivedFeatureInvoker> derivedInvokers = new ArrayList<>();
            for (DerivedFeatureSpec spec : snapshot.getDerivedFeaturesInOrder()) {
                ScriptEngineAdapter adapter = engineAdapters.get(spec.getEngineType());
                CompiledExecutable executable = adapter.compile(toExecutableSpec(snapshot, spec));
                resourceHolder.register(executable);
                derivedInvokers.add(new DefaultDerivedFeatureInvoker(spec, executable));
            }

            List<RuleInvoker> ruleInvokers = new ArrayList<>();
            for (RuleSpec spec : snapshot.getRulesInPriorityOrder()) {
                ScriptEngineAdapter adapter = engineAdapters.get(spec.getEngineType());
                CompiledExecutable executable = adapter.compile(toExecutableSpec(snapshot, spec));
                resourceHolder.register(executable);
                ruleInvokers.add(new DefaultRuleInvoker(spec, executable, hitReasonRenderer));
            }

            PolicyExecutor policyExecutor = policyExecutorFactory.create(snapshot.getPolicy());

            return new CompiledSceneRuntime(
                    snapshot.getSceneCode(),
                    snapshot.getVersion(),
                    snapshot,
                    snapshot.getStreamFeaturePlans(),
                    snapshot.getLookupFeaturePlans(),
                    derivedInvokers,
                    ruleInvokers,
                    policyExecutor,
                    snapshot.getRuntimeHints().getRequiredVariables(),
                    resourceHolder
            );
        } catch (Exception e) {
            resourceHolder.closeQuietly();
            throw RuntimeCompileException.from(snapshot, e);
        }
    }
}
```

---

## 18.16 Flink 中如何使用这些执行器

这里我们把执行器代码设计和 Flink 真正连起来。

### 18.16.1 在 Broadcast State 中存什么

只存：

- `SceneSnapshot`
- version 元信息

不要存：

- `CompiledSceneRuntime`
- Aviator compiled object
- Groovy class
- Groovy classloader

---

### 18.16.2 在本地缓存中存什么

存：

- `Map<String, CompiledSceneRuntime>`

key 可以是：

- `sceneCode`
- 或 `sceneCode:version`

推荐支持版本级 key，便于调试和切换：

```java
private transient Map<String, CompiledSceneRuntime> runtimeCache;
```

---

### 18.16.3 `processBroadcastElement` 伪代码

```java
@Override
public void processBroadcastElement(SceneSnapshot snapshot,
                                    Context ctx,
                                    Collector<DecisionResult> out) throws Exception {
    BroadcastState<String, SceneSnapshot> state = ctx.getBroadcastState(SNAPSHOT_STATE_DESC);

    String sceneKey = snapshot.getSceneCode();
    CompiledSceneRuntime oldRuntime = runtimeCache.get(sceneKey);

    CompiledSceneRuntime newRuntime;
    try {
        newRuntime = runtimeCompiler.compile(snapshot);
    } catch (Exception e) {
        // 编译失败，不切换线上版本，保留旧版本
        log.error("compile snapshot failed, scene={}, version={}",
                snapshot.getSceneCode(), snapshot.getVersion(), e);
        metrics.incConfigCompileFail(snapshot.getSceneCode());
        return;
    }

    // 先更新可恢复状态
    state.put(sceneKey, snapshot);

    // 再替换本地运行时缓存
    runtimeCache.put(sceneKey, newRuntime);

    // 最后释放旧资源
    if (oldRuntime != null) {
        oldRuntime.close();
    }

    metrics.incConfigSwitchSuccess(snapshot.getSceneCode());
}
```

这段伪代码体现了几个很重要的原则：

1. 先编译成功，再切换
2. 编译失败不影响旧版本运行
3. 广播状态和本地缓存分离
4. 旧版本资源需要显式释放

---

### 18.16.4 `processElement` 伪代码

```java
@Override
public void processElement(DecisionEvent event,
                           ReadOnlyContext ctx,
                           Collector<DecisionResult> out) throws Exception {
    CompiledSceneRuntime runtime = runtimeCache.get(event.getSceneCode());
    if (runtime == null) {
        // 没有运行时版本，可按 fail-open / fail-close 处理
        out.collect(DecisionResult.noConfig(event));
        return;
    }

    EvalContext evalContext = evalContextBuilder.buildBase(runtime, event);

    // 1. stream feature
    streamFeatureExecutor.fill(runtime.getStreamFeaturePlans(), event, evalContext);

    // 2. lookup feature
    lookupFeatureExecutor.fill(runtime.getLookupFeaturePlans(), event, evalContext);

    // 3. derived feature
    for (DerivedFeatureInvoker invoker : runtime.getDerivedFeatureInvokersInOrder()) {
        DerivedFeatureResult result = invoker.evaluate(evalContext);
        evalContext.asMap().put(result.getFeatureCode(), result.getValue());
    }

    // 4. rules
    List<RuleHitResult> hits = new ArrayList<>();
    for (RuleInvoker invoker : runtime.getOrderedRuleInvokers()) {
        RuleHitResult hit = invoker.evaluate(evalContext);
        if (hit.isHit()) {
            hits.add(hit);
        }
    }

    // 5. policy
    DecisionResult decision = runtime.getPolicyExecutor().decide(event, evalContext, hits, runtime);

    out.collect(decision);
}
```

你可以看到，执行器在 Flink 中的职责非常清晰：

- 不负责取快照
- 不负责算 stream feature
- 不负责查 Redis
- 它只负责：
  - 在上下文准备好之后，执行派生特征和规则

这正是它最合理的边界。

---

## 18.17 异常设计：编译异常和执行异常要分开

这点非常重要。

### 18.17.1 编译异常

发生在：

- 快照加载时
- 规则/派生特征预编译时

例如：

- Aviator compile fail
- Groovy syntax error
- classloader 构建失败

推荐异常：

```java
public class ScriptCompileException extends RuntimeException {
    private final String sceneCode;
    private final long version;
    private final String bizCode;
    private final String engineType;
    // ...
}
```

这类异常的处理原则是：

> **不切版本，保留旧版本运行。**

---

### 18.17.2 执行异常

发生在：

- 事件运行时
- 某条规则执行时
- 某个派生特征脚本运行时报错

例如：

- 空指针
- 类型转换异常
- Groovy 运行时异常

推荐异常：

```java
public class ScriptExecutionException extends RuntimeException {
    private final String sceneCode;
    private final long version;
    private final String bizCode;
    private final String engineType;
    // ...
}
```

这类异常的处理要按场景决定：

- fail-open：脚本出错则跳过该规则/默认放行
- fail-close：脚本出错则给默认拒绝/审核
- fail-current-rule：只视作该规则未命中

对于个人项目，我建议默认采用：

> **规则执行异常默认按“该规则未命中 + 记录错误日志”处理**

同时保留 scene 级别的错误策略配置能力。

---

## 18.18 Groovy 沙箱建议

这一节必须讲，因为 Groovy 一旦放开，就是系统最大风险点之一。

### 18.18.1 不建议开放的能力

至少不要允许：

- `System.exit`
- `new File(...)`
- `new URL(...)`
- `Runtime.getRuntime()`
- 反射 `Class.forName`
- 任意 import
- 任意线程操作
- 任意网络访问
- 任意 Bean 获取

---

### 18.18.2 推荐限制方式

可以从几个层面入手：

#### 1）AST 限制

编译前扫描语法树，禁止危险节点。

#### 2）ClassLoader 白名单

只允许加载指定包和接口。

#### 3）基类约束

要求脚本必须实现指定接口或继承指定基类。

#### 4）Binding 受控

只把 `ctx` 提供给脚本，不注入其他对象。

#### 5）版本级隔离

每个快照版本独立类加载器。

---

### 18.18.3 如果你短期不想把沙箱做太重怎么办

这是很现实的问题。

对个人项目，一期可以这样做：

- Groovy 仅对自己开放，不对外部任意用户开放
- 明确写在 README：当前为受限实验能力
- 只允许固定模板，不允许任意 import
- 只支持返回布尔/数值表达式

也就是说：

> **可以先把接口预留和结构搭好，Groovy 能力逐步加强，不必一开始就做成“任意脚本平台”。**

---

## 18.19 性能设计建议

执行器设计如果不注意性能，后面很容易拖慢热路径。这里给你几个关键建议。

### 18.19.1 不要每条事件都重新构造巨大上下文

最好只构造当前规则真正需要的变量，或者至少控制 `EvalContext` 的大小。

一期为了简单可以先全量 map，但后面可优化为：

- runtimeHints 提前给出 required variables
- context builder 按需装配

---

### 18.19.2 不要每次执行都反复模板解析

`HitReasonRenderer` 可以做简单缓存，或者模板结构预解析。

---

### 18.19.3 规则顺序要预排序

不要每次事件来都再 sort。

---

### 18.19.4 派生特征执行顺序要预拓扑排序

如果有依赖链：

- A 依赖 B
- B 依赖 C

那发布时就应确定好顺序，运行时直接按顺序执行。

---

### 18.19.5 Groovy 版本切换后及时释放旧资源

否则内存和 metaspace 会慢慢涨。

---

## 18.20 线程安全与 Flink 场景下的注意点

### 18.20.1 单个 Flink subtask 通常是单线程处理元素

这意味着很多执行器如果是无状态对象，天然就比较安全。

但你仍然要注意：

- 本地缓存替换时的原子性
- Groovy classloader 生命周期
- Async IO 场景下共享对象的线程安全

---

### 18.20.2 推荐做法

- 本地运行时缓存用 `ConcurrentHashMap`
- `CompiledSceneRuntime` 尽量设计为不可变对象
- 版本切换采用“新对象替换旧对象”而不是原地修改

这类不可变设计在运行态特别稳。

---

## 18.21 测试设计建议

执行器这块很适合做较完整的单元测试。

### 18.21.1 表达式编译测试

测试：

- 正常表达式能否 compile
- 错误表达式是否抛预期异常
- 变量缺失时行为是否符合预期

---

### 18.21.2 Groovy 编译测试

测试：

- 合法脚本是否成功编译
- 非法语法是否被拦截
- 危险操作是否被拒绝

---

### 18.21.3 规则执行测试

输入 `EvalContext`，断言：

- 命中结果
- score
- hitReason
- engineType

---

### 18.21.4 派生特征测试

断言：

- 派生结果值
- 依赖顺序
- 类型一致性

---

### 18.21.5 版本切换测试

模拟：

- v1 编译成功
- v2 编译失败
- 系统应继续使用 v1

这个测试非常有价值。

---

## 18.22 你这个项目的一套推荐最小实现方案

如果考虑“6个月能落地”，我建议你分阶段做。

### 第一步：先把统一抽象做好

实现：

- `EvalContext`
- `CompiledExecutable`
- `RuleInvoker`
- `DerivedFeatureInvoker`
- `ScriptEngineAdapter`
- `CompiledSceneRuntime`

---

### 第二步：先只支持 Aviator

先跑通：

- 派生特征
- 规则执行
- 命中原因渲染
- 策略执行

这一步已经足够把主链路打通。

---

### 第三步：把编译缓存和 Flink 快照切换打通

实现：

- `RuntimeCompiler`
- 本地缓存
- `processBroadcastElement` 切版本

---

### 第四步：再补 Groovy

先只开放给少量“高级规则”使用。

---

### 第五步：最后再增强沙箱和调优

包括：

- AST 限制
- 类加载器回收
- 更细的异常策略
- 模板缓存

这样项目推进会稳很多。

---

## 18.23 本章小结

这一章本质上是在回答：

> **表达式引擎和 Groovy 在代码层到底该怎么落地。**

我们把重点收一下。

### 1）编译和执行必须分离

- 快照加载时编译
- 事件处理时执行

### 2）要有统一抽象

建议至少抽出：

- `EvalContext`
- `CompiledExecutable`
- `RuleInvoker`
- `DerivedFeatureInvoker`
- `ScriptEngineAdapter`
- `CompiledSceneRuntime`

### 3）Aviator 和 Groovy 应通过适配器统一接入

上层不应该直接依赖某个具体脚本引擎实现。

### 4）Broadcast State 存快照，本地缓存存编译结果

这是 Flink 执行器设计的关键边界。

### 5）Groovy 必须谨慎使用

它适合做高级规则扩展，但不能成为任意脚本执行平台。

### 6）仿真与线上必须共用执行核心

这会极大提升系统可信度和可维护性。

---

## 18.24 下一章会讲什么

下一章进入：

**第 19 章：完整事件处理时序——一条交易事件在引擎里到底经历了什么**

这一章会把前面所有章节串起来，用一条真实交易事件的完整时序，把下面这些内容连成一条线：

- 配置版本定位
- stream feature 计算
- lookup 查询
- 派生特征执行
- 规则执行
- 策略收敛
- 决策结果生成
- 日志与指标输出

也就是说，下一章会把你前面学到的“概念、快照、执行器、策略”，真正还原成：

> **一条事件进入 Flink 后，系统到底是怎么一步一步跑起来的。**
