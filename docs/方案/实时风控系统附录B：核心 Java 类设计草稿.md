## 附录 B：核心 Java 类设计草稿

本附录给出实时风控系统中最核心的一组 Java 领域类与运行时类设计草稿，目标不是一开始就把所有实现细节写死，而是帮助你把：

- 控制平台发布出来的快照对象
- Flink 引擎运行时的编译对象
- 规则执行上下文
- 特征执行器接口
- 规则执行器接口

这些关键对象的边界先稳定下来。

---

### B.1 设计原则

在开始定义类之前，先明确几个原则：

#### 原则 1：发布态对象和运行态对象分开

- 发布态对象：可序列化、可持久化、可进入 Broadcast State
- 运行态对象：包含编译结果、本地缓存、不可直接序列化

也就是说：

- `SceneSnapshot` 是发布态对象
- `CompiledSceneRuntime` 是运行态对象

#### 原则 2：规则与特征执行都围绕统一上下文进行

无论是：

- stream feature
- lookup feature
- derived feature
- rule

最终都需要围绕统一的 `EvalContext` 来读写变量。

#### 原则 3：接口先稳定，再逐步补实现

一开始不要急着把实现写得很花。先把接口边界稳定下来，后面扩 DSL、Groovy、评分卡都会顺很多。

---

### B.2 SceneSnapshot

这是发布态最核心的对象，对应一份运行时快照。

```java
public class SceneSnapshot {

    private String snapshotId;
    private String sceneCode;
    private Integer version;
    private String status;
    private String checksum;
    private Instant publishedAt;
    private Instant effectiveFrom;

    private EventSchemaSpec eventSchema;

    private List<StreamFeatureSpec> streamFeatures;
    private List<LookupFeatureSpec> lookupFeatures;
    private List<DerivedFeatureSpec> derivedFeatures;
    private List<RuleSpec> rules;
    private PolicySpec policy;
    private RuntimeHints runtimeHints;

    // getter/setter
}
```

#### 说明

- 它要能被序列化为 JSON
- 它要能进入 Broadcast State
- 它不应该包含编译后的 Aviator 对象、Groovy Class 等不可序列化内容

---

### B.3 EventSchemaSpec

```java
public class EventSchemaSpec {

    private String eventType;
    private List<String> requiredFields;
    private Map<String, String> fieldTypes;
}
```

#### 作用

- 描述当前场景事件结构的最小运行时摘要
- 用于运行时做基础字段校验与上下文约束

---

### B.4 FeatureSpec 体系

建议把 Feature 做成抽象父类 + 三种子类。

```java
public abstract class FeatureSpec {

    private String code;
    private String name;
    private FeatureType type;
    private String valueType;
    private String description;
}
```

```java
public enum FeatureType {
    STREAM,
    LOOKUP,
    DERIVED
}
```

---

### B.5 StreamFeatureSpec

```java
public class StreamFeatureSpec extends FeatureSpec {

    private List<String> sourceEventTypes;
    private String entityType;
    private String entityKeyExpr;
    private AggType aggType;
    private String valueExpr;
    private String filterExpr;
    private WindowType windowType;
    private String windowSize;
    private String windowSlide;
    private Boolean includeCurrentEvent;
    private String ttl;
}
```

```java
public enum AggType {
    COUNT,
    SUM,
    MAX,
    LATEST,
    DISTINCT_COUNT
}
```

```java
public enum WindowType {
    TUMBLING,
    SLIDING,
    NONE
}
```

#### 说明

- 这是控制平台发布给 Flink 的流式特征定义
- 真正的聚合逻辑仍由 Flink 内置模板执行器实现
- 这里表达的是“声明式运行计划”，不是“任意脚本”

---

### B.6 LookupFeatureSpec

```java
public class LookupFeatureSpec extends FeatureSpec {

    private LookupType lookupType;
    private String keyExpr;
    private String sourceRef;
    private Object defaultValue;
    private Integer timeoutMs;
    private Integer cacheTtlSeconds;
}
```

```java
public enum LookupType {
    REDIS_SET,
    REDIS_HASH,
    REDIS_STRING,
    DICT
}
```

#### 说明

- 运行时根据 keyExpr 从上下文中取 key
- 再根据 lookupType 决定走哪个查询器

---

### B.7 DerivedFeatureSpec

```java
public class DerivedFeatureSpec extends FeatureSpec {

    private EngineType engine;
    private String expr;
    private List<String> dependsOn;
}
```

```java
public enum EngineType {
    AVIATOR,
    GROOVY,
    DSL
}
```

#### 说明

- 派生特征本质是“生成新变量”
- 它在规则执行之前计算

---

### B.8 RuleSpec

```java
public class RuleSpec {

    private String code;
    private String name;
    private EngineType engine;
    private Integer priority;
    private String whenExpr;
    private List<String> dependsOn;
    private ActionType hitAction;
    private Integer score;
    private String hitReasonTemplate;
    private Boolean enabled;
}
```

```java
public enum ActionType {
    PASS,
    REVIEW,
    REJECT,
    LIMIT,
    TAG_ONLY
}
```

#### 说明

- `whenExpr` 是规则判断逻辑
- `hitAction` 是该规则命中后的建议动作
- 最终动作不一定由单条规则直接决定，还要看 Policy

---

### B.9 PolicySpec

```java
public class PolicySpec {

    private String policyCode;
    private String policyName;
    private DecisionMode decisionMode;
    private ActionType defaultAction;
    private List<String> ruleOrder;
    private ScoreCardConfig scoreCardConfig;
}
```

```java
public enum DecisionMode {
    FIRST_HIT,
    SCORE_CARD
}
```

```java
public class ScoreCardConfig {

    private List<ScoreRangeAction> ranges;
}
```

```java
public class ScoreRangeAction {

    private Integer minScore;
    private Integer maxScore;
    private ActionType action;
}
```

#### 说明

- FIRST\_HIT：按顺序命中即返回
- SCORE\_CARD：累计分数后映射动作

---

### B.10 RuntimeHints

```java
public class RuntimeHints {

    private List<String> requiredStreamFeatures;
    private List<String> requiredLookupFeatures;
    private List<String> requiredDerivedFeatures;
}
```

#### 作用

- 让引擎快速知道当前场景实际要准备哪些能力
- 有助于减少运行时重复扫描

---

### B.11 EvalContext

这是规则执行最核心的上下文对象。

```java
public class EvalContext {

    private String sceneCode;
    private Integer version;
    private String eventId;
    private String traceId;
    private Instant eventTime;

    private Map<String, Object> baseFields = new HashMap<>();
    private Map<String, Object> featureValues = new HashMap<>();
    private Map<String, Object> ext = new HashMap<>();

    public Object get(String name) {
        if (featureValues.containsKey(name)) {
            return featureValues.get(name);
        }
        return baseFields.get(name);
    }

    public void putBaseField(String key, Object value) {
        baseFields.put(key, value);
    }

    public void putFeature(String key, Object value) {
        featureValues.put(key, value);
    }

    public Map<String, Object> toFlatMap() {
        Map<String, Object> all = new HashMap<>(baseFields);
        all.putAll(featureValues);
        all.putAll(ext);
        return all;
    }
}
```

#### 说明

- 表达式执行器 / Groovy 执行器都围绕它工作
- 规则命中原因模板也可以从这里取值
- 仿真与线上都应该使用同一套上下文结构

---

### B.12 CompiledSceneRuntime

这是 Flink 本地 transient cache 中最核心的对象。

```java
public class CompiledSceneRuntime {

    private SceneSnapshot snapshot;

    private Map<String, DerivedFeatureInvoker> derivedFeatureInvokers;
    private Map<String, RuleInvoker> ruleInvokers;
    private PolicyRuntime policyRuntime;
    private FeatureExecutionPlan featureExecutionPlan;

    public SceneSnapshot getSnapshot() {
        return snapshot;
    }
}
```

#### 说明

- 这个对象不建议直接进 Broadcast State
- 它应该在 Flink task 本地由 Snapshot 编译而来

---

### B.13 FeatureExecutionPlan

```java
public class FeatureExecutionPlan {

    private List<StreamFeatureSpec> streamFeatures;
    private List<LookupFeatureSpec> lookupFeatures;
    private List<DerivedFeatureSpec> derivedFeaturesInOrder;
}
```

#### 作用

- 告诉引擎当前一条事件进入后，先算哪些，再查哪些，再派生哪些
- `derivedFeaturesInOrder` 需要经过依赖拓扑排序

---

### B.14 PolicyRuntime

```java
public class PolicyRuntime {

    private PolicySpec spec;
    private List<RuleInvoker> orderedRuleInvokers;
}
```

#### 说明

- 这里可以预先把 ruleOrder 展开成实际执行列表
- 避免每条事件动态排序

---

### B.15 RuleInvoker

规则执行器建议抽象成统一接口。

```java
public interface RuleInvoker {

    String getRuleCode();

    RuleEvalResult eval(EvalContext context);
}
```

```java
public class RuleEvalResult {

    private String ruleCode;
    private boolean hit;
    private Integer score;
    private ActionType suggestedAction;
    private String hitReason;
    private Map<String, Object> hitSnapshot;
}
```

#### 说明

- 表达式规则和 Groovy 规则都实现这个接口
- 上层策略引擎只关心统一结果，不关心底层执行方式

---

### B.16 DerivedFeatureInvoker

```java
public interface DerivedFeatureInvoker {

    String getFeatureCode();

    Object eval(EvalContext context);
}
```

#### 说明

- 派生特征执行器和规则执行器分开，边界更清晰
- 派生特征先执行，把结果写回 EvalContext；规则再执行

---

### B.17 FeatureExecutor

流式特征和 lookup 特征建议分别抽象。

```java
public interface FeatureExecutor<T extends FeatureSpec> {

    String getFeatureCode();

    Object execute(T spec, EvalContext context, FeatureExecutionEnv env) throws Exception;
}
```

```java
public class FeatureExecutionEnv {

    private RedisLookupService redisLookupService;
    private RuntimeStateAccessor stateAccessor;
    private MetricRecorder metricRecorder;
}
```

#### 说明

- 对于 lookup feature，这个接口很自然
- 对于 stream feature，更常见的是由 Flink 状态模板统一处理，但也可以保留统一抽象思路

---

### B.18 RuntimeStateAccessor

```java
public interface RuntimeStateAccessor {

    Object getFeatureState(String featureCode, String entityKey);

    void updateFeatureState(String featureCode, String entityKey, Object state);
}
```

#### 说明

- 这是一个抽象层，不一定一开始就真的完整实现
- 主要帮助你在设计上把“执行逻辑”和“状态访问”解耦

---

### B.19 表达式/Groovy执行器接口建议

为了统一 DerivedFeatureInvoker 和 RuleInvoker 的底层能力，可以再抽一层。

```java
public interface ScriptInvoker {

    Object execute(Map<String, Object> context);
}
```

```java
public interface ScriptCompiler {

    ScriptInvoker compile(String exprOrScript, EngineType engineType);
}
```

#### 说明

- Aviator / DSL / Groovy 都可以通过不同 compiler 实现
- 运行时只持有编译后的 invoker

---

### B.20 决策结果对象

```java
public class DecisionResult {

    private String sceneCode;
    private Integer version;
    private String eventId;
    private String traceId;
    private ActionType finalAction;
    private Integer totalScore;
    private List<RuleEvalResult> hitRules;
    private Map<String, Object> featureSnapshot;
    private Long latencyMs;
    private Instant eventTime;
}
```

#### 说明

- 这个对象既可以输出到 Kafka
- 也可以用于日志落库
- 也可以用于仿真测试返回

---

### B.21 仿真与线上如何共用核心类

建议你的项目结构上做到：

- 控制平台仿真调用：
  - `SceneSnapshot`
  - `CompiledSceneRuntime`
  - `EvalContext`
  - `RuleInvoker`
  - `DerivedFeatureInvoker`
  - `PolicyRuntime`

- Flink 线上执行也调用同样这套核心类

这样才能保证：

- 仿真和线上执行逻辑一致
- 规则行为可解释
- 避免“仿真命中、线上不命中”

---

### B.22 一套推荐的包结构

```java
com.rdp.core.snapshot
  ├── SceneSnapshot
  ├── EventSchemaSpec
  ├── FeatureSpec
  ├── StreamFeatureSpec
  ├── LookupFeatureSpec
  ├── DerivedFeatureSpec
  ├── RuleSpec
  ├── PolicySpec
  └── RuntimeHints

com.rdp.core.runtime
  ├── CompiledSceneRuntime
  ├── FeatureExecutionPlan
  ├── PolicyRuntime
  ├── EvalContext
  ├── DecisionResult
  └── RuleEvalResult

com.rdp.core.engine
  ├── RuleInvoker
  ├── DerivedFeatureInvoker
  ├── FeatureExecutor
  ├── ScriptInvoker
  ├── ScriptCompiler
  └── RuntimeStateAccessor
```

---

### B.23 本附录小结

这一组核心 Java 类草稿的目标，是帮你先稳定三件事：

1. **发布态对象和运行态对象分离**
   - `SceneSnapshot` 用于发布与恢复
   - `CompiledSceneRuntime` 用于本地执行

2. **统一上下文模型**
   - `EvalContext` 是表达式、Groovy、规则、派生特征共同依赖的核心对象

3. **统一执行接口**
   - `RuleInvoker`
   - `DerivedFeatureInvoker`
   - `FeatureExecutor`

这样后面你无论是继续扩展：

- Aviator
- Groovy
- ScoreCard
- 仿真
- 回放

都会有比较稳的骨架。
