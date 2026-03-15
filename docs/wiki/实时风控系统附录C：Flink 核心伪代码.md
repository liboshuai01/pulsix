这个附录用于补充一版**可以直接指导开工的 Flink 引擎核心伪代码**。内容目标不是追求完全可编译，而是把实时风控系统在 Flink 侧最关键的执行骨架讲清楚，包括：

- 配置快照如何进入 Flink
- `processBroadcastElement` 如何处理发布配置
- `processElement` 如何处理实时事件
- 流式特征执行器如何抽象
- 表达式 / Groovy 执行器如何抽象
- 决策结果如何输出
- 异常与版本切换如何处理

你可以把这一份内容，直接当成后续 `pulsix-engine` 的第一版代码骨架参考。

---

## 一、整体伪代码结构总览

先看一版总骨架：

```java
public class DecisionEngineJob {

    public static void main(String[] args) throws Exception {
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();

        env.enableCheckpointing(30000);
        env.setParallelism(4);

        DataStream<DecisionEvent> eventStream = buildEventStream(env);
        DataStream<SceneSnapshotEnvelope> configStream = buildConfigStream(env);

        MapStateDescriptor<String, SceneSnapshot> snapshotStateDesc =
                new MapStateDescriptor<>(
                        "scene-snapshot-broadcast-state",
                        Types.STRING,
                        TypeInformation.of(SceneSnapshot.class)
                );

        BroadcastStream<SceneSnapshotEnvelope> broadcastConfigStream =
                configStream.broadcast(snapshotStateDesc);

        SingleOutputStreamOperator<DecisionResult> resultStream =
                eventStream
                        .keyBy(DecisionEvent::routeKey)
                        .connect(broadcastConfigStream)
                        .process(new DecisionBroadcastProcessFunction(snapshotStateDesc));

        resultStream.addSink(buildDecisionSink());
        resultStream.getSideOutput(OutputTags.DECISION_LOG_TAG).addSink(buildDecisionLogSink());
        resultStream.getSideOutput(OutputTags.ERROR_TAG).addSink(buildErrorSink());

        env.execute("pulsix-risk-decision-engine");
    }
}
```

这段骨架背后有几个关键点：

1. **事件流是主流**
2. **快照流是广播流**
3. `DecisionBroadcastProcessFunction` 是核心执行入口
4. 结果流和日志流要分开输出
5. 配置快照只进 Broadcast State，不直接查控制平台多表

---

## 二、核心数据结构伪代码

在写主流程之前，先定义几个运行时核心对象。

---

### 2.1 事件对象

```java
class DecisionEvent {
    String eventId;
    String traceId;
    String sceneCode;
    String eventType;
    long eventTime;

    String userId;
    String deviceId;
    String ip;
    BigDecimal amount;

    Map<String, Object> ext;

    public String routeKey() {
        // 一期可简单按 sceneCode 路由；
        // 如果后面需要更细粒度状态，可改成 sceneCode + 主实体
        return sceneCode;
    }
}
```

---

### 2.2 快照发布包装对象

```java
class SceneSnapshotEnvelope {
    String sceneCode;
    long version;
    String checksum;
    String publishType; // PUBLISH / ROLLBACK
    long publishedAt;
    long effectiveFrom;
    SceneSnapshot snapshot;
}
```

---

### 2.3 运行时上下文对象

```java
class EvalContext {
    String sceneCode;
    long version;
    DecisionEvent event;

    Map<String, Object> values = new HashMap<>();
    List<RuleHit> ruleHits = new ArrayList<>();
    List<String> traceLogs = new ArrayList<>();

    void put(String key, Object value) {
        values.put(key, value);
    }

    Object get(String key) {
        return values.get(key);
    }

    <T> T getAs(String key, Class<T> type) {
        return type.cast(values.get(key));
    }
}
```

---

### 2.4 规则命中结果

```java
class RuleHit {
    String ruleCode;
    boolean hit;
    String action;
    int score;
    String reason;
    Map<String, Object> detail;
}
```

---

### 2.5 最终决策结果

```java
class DecisionResult {
    String eventId;
    String traceId;
    String sceneCode;
    long version;

    String finalAction;   // PASS / REVIEW / REJECT
    int finalScore;
    long latencyMs;

    List<RuleHit> hitRules;
    Map<String, Object> featureSnapshot;
    String errorMsg;
}
```

---

### 2.6 编译后的运行时对象

注意：这个对象**不直接进 Broadcast State**，而是保存在 Task 本地 transient cache 中。

```java
class CompiledSceneRuntime {
    SceneSnapshot snapshot;

    Map<String, FeatureExecutor> streamFeatureExecutors;
    Map<String, LookupFeatureExecutor> lookupFeatureExecutors;
    Map<String, DerivedFeatureInvoker> derivedFeatureInvokers;
    Map<String, RuleInvoker> ruleInvokers;

    PolicyExecutor policyExecutor;

    List<String> orderedDerivedFeatures;
    List<String> orderedRules;
}
```

---

## 三、DecisionBroadcastProcessFunction 核心伪代码

这是整个 Flink 引擎最重要的类。

```java
public class DecisionBroadcastProcessFunction
        extends KeyedBroadcastProcessFunction<String, DecisionEvent, SceneSnapshotEnvelope, DecisionResult> {

    private final MapStateDescriptor<String, SceneSnapshot> snapshotStateDesc;

    // 本地运行时缓存：不放入 checkpoint
    private transient Map<String, CompiledSceneRuntime> localRuntimeCache;

    // Redis / 表达式引擎 / Groovy 编译器 / 指标组件
    private transient RedisLookupService redisLookupService;
    private transient RuntimeCompiler runtimeCompiler;
    private transient MetricsCollector metricsCollector;

    public DecisionBroadcastProcessFunction(MapStateDescriptor<String, SceneSnapshot> snapshotStateDesc) {
        this.snapshotStateDesc = snapshotStateDesc;
    }

    @Override
    public void open(Configuration parameters) {
        this.localRuntimeCache = new ConcurrentHashMap<>();
        this.redisLookupService = new RedisLookupService();
        this.runtimeCompiler = new RuntimeCompiler();
        this.metricsCollector = new MetricsCollector();
    }
}
```

后面最关键的就是两个方法：

- `processBroadcastElement`
- `processElement`

---

## 四、processBroadcastElement 伪代码

这个方法负责：

- 接收控制平台发布的快照
- 更新 Broadcast State
- 编译运行时缓存
- 切换版本
- 在编译失败时保留旧版本

---

### 4.1 核心伪代码

```java
@Override
public void processBroadcastElement(
        SceneSnapshotEnvelope envelope,
        Context ctx,
        Collector<DecisionResult> out) throws Exception {

    BroadcastState<String, SceneSnapshot> state = ctx.getBroadcastState(snapshotStateDesc);

    String sceneCode = envelope.getSceneCode();
    SceneSnapshot incomingSnapshot = envelope.getSnapshot();

    if (incomingSnapshot == null) {
        log.warn("snapshot is null, sceneCode={}", sceneCode);
        return;
    }

    // 1. 基础校验
    validateEnvelope(envelope);

    SceneSnapshot currentSnapshot = state.get(sceneCode);

    // 2. 版本判断：旧版本直接丢弃
    if (currentSnapshot != null && incomingSnapshot.getVersion() <= currentSnapshot.getVersion()) {
        log.warn("ignore old snapshot, sceneCode={}, incomingVersion={}, currentVersion={}",
                sceneCode,
                incomingSnapshot.getVersion(),
                currentSnapshot.getVersion());
        return;
    }

    try {
        // 3. 编译运行时对象（表达式、Groovy、执行计划）
        CompiledSceneRuntime compiledRuntime = runtimeCompiler.compile(incomingSnapshot);

        // 4. 写广播状态（可恢复）
        state.put(sceneCode, incomingSnapshot);

        // 5. 切换本地缓存（高性能）
        localRuntimeCache.put(sceneCode, compiledRuntime);

        // 6. 清理旧版本 classloader / groovy cache（如有）
        cleanupOldRuntimeIfNecessary(sceneCode, currentSnapshot, compiledRuntime);

        metricsCollector.markSnapshotSwitch(sceneCode, incomingSnapshot.getVersion());
        log.info("snapshot switched successfully, sceneCode={}, version={}",
                sceneCode,
                incomingSnapshot.getVersion());

    } catch (Exception e) {
        // 7. 编译失败：不能覆盖旧版本
        metricsCollector.markSnapshotCompileError(sceneCode);
        log.error("snapshot compile failed, keep old runtime, sceneCode={}, version={}",
                sceneCode,
                incomingSnapshot.getVersion(),
                e);
    }
}
```

---

### 4.2 这里最关键的设计点

#### 1）先编译，后切换

不能先把快照写进状态，再发现编译失败。正确顺序是：

- 先编译运行时对象
- 编译成功再写 Broadcast State
- 再切换本地缓存

#### 2）Broadcast State 放可恢复对象

放的是：

- `SceneSnapshot`

不放的是：

- 已编译 Groovy Class
- 表达式对象
- 本地 ClassLoader

#### 3）旧版本失败保护

新版本编译失败时：

- 保持旧版本继续服务
- 记录错误
- 不让引擎整体崩掉

---

## 五、processElement 伪代码

这个方法负责真正处理每一条实时事件。

执行步骤通常是：

1. 找快照
2. 找本地编译缓存
3. 构建上下文
4. 写入基础字段
5. 计算流式特征
6. lookup 查询
7. 派生特征计算
8. 规则执行
9. 策略收敛
10. 输出结果与日志

---

### 5.1 核心伪代码

```java
@Override
public void processElement(
        DecisionEvent event,
        ReadOnlyContext ctx,
        Collector<DecisionResult> out) throws Exception {

    long start = System.currentTimeMillis();

    ReadOnlyBroadcastState<String, SceneSnapshot> state = ctx.getBroadcastState(snapshotStateDesc);
    String sceneCode = event.getSceneCode();

    SceneSnapshot snapshot = state.get(sceneCode);
    if (snapshot == null) {
        emitNoSnapshotError(event, out, start);
        return;
    }

    CompiledSceneRuntime runtime = localRuntimeCache.get(sceneCode);
    if (runtime == null || runtime.snapshot.getVersion() != snapshot.getVersion()) {
        // checkpoint恢复后，可能只恢复了快照，本地缓存还没重建
        try {
            runtime = runtimeCompiler.compile(snapshot);
            localRuntimeCache.put(sceneCode, runtime);
        } catch (Exception e) {
            emitRuntimeCompileError(event, snapshot, e, out, start);
            return;
        }
    }

    try {
        // 1. 构建上下文
        EvalContext evalContext = new EvalContext();
        evalContext.sceneCode = sceneCode;
        evalContext.version = snapshot.getVersion();
        evalContext.event = event;

        // 2. 放入基础字段
        putBaseFields(evalContext, event);

        // 3. 计算 stream feature
        executeStreamFeatures(runtime, evalContext, ctx);

        // 4. 执行 lookup feature
        executeLookupFeatures(runtime, evalContext);

        // 5. 执行 derived feature
        executeDerivedFeatures(runtime, evalContext);

        // 6. 执行规则
        List<RuleHit> hits = executeRules(runtime, evalContext);

        // 7. 策略收敛
        DecisionResult result = runtime.policyExecutor.decide(evalContext, hits);

        // 8. 补齐公共字段
        result.setEventId(event.getEventId());
        result.setTraceId(event.getTraceId());
        result.setSceneCode(sceneCode);
        result.setVersion(snapshot.getVersion());
        result.setLatencyMs(System.currentTimeMillis() - start);
        result.setFeatureSnapshot(new HashMap<>(evalContext.values));
        result.setHitRules(hits);

        // 9. 输出主结果
        out.collect(result);

        // 10. 输出日志 side output
        ctx.output(OutputTags.DECISION_LOG_TAG, buildDecisionLog(result, evalContext));

        metricsCollector.markDecisionSuccess(sceneCode, result.getFinalAction(), result.getLatencyMs());

    } catch (Exception e) {
        metricsCollector.markDecisionError(sceneCode);
        ctx.output(OutputTags.ERROR_TAG, buildErrorLog(event, snapshot, e));
        emitExecutionError(event, snapshot, e, out, start);
    }
}
```

---

### 5.2 这里的关键点

#### 1）优先从 Broadcast State 读取快照

这是系统的真实恢复来源。

#### 2）本地缓存缺失时允许懒编译重建

这在 task 重启或 checkpoint 恢复后非常重要。

#### 3）上下文构建必须分阶段

不要一上来就直接执行规则。

#### 4）日志与主结果输出分离

主结果走主流，详细日志走 side output。

---

## 六、基础字段注入伪代码

```java
private void putBaseFields(EvalContext ctx, DecisionEvent event) {
    ctx.put("eventId", event.getEventId());
    ctx.put("traceId", event.getTraceId());
    ctx.put("sceneCode", event.getSceneCode());
    ctx.put("eventType", event.getEventType());
    ctx.put("eventTime", event.getEventTime());

    ctx.put("userId", event.getUserId());
    ctx.put("deviceId", event.getDeviceId());
    ctx.put("ip", event.getIp());
    ctx.put("amount", event.getAmount());

    if (event.getExt() != null) {
        event.getExt().forEach(ctx::put);
    }
}
```

这个阶段只负责原始字段，不做任何复杂判断。

---

## 七、流式特征执行器伪代码

这里先定义抽象接口。

---

### 7.1 FeatureExecutor 接口

```java
interface FeatureExecutor {
    Object execute(EvalContext ctx, KeyedBroadcastProcessFunction.ReadOnlyContext flinkCtx) throws Exception;
}
```

如果某些执行器需要使用 KeyedState，可以再做更具体的接口：

```java
interface StatefulFeatureExecutor extends FeatureExecutor {
    void open(RuntimeContext runtimeContext);
}
```

---

### 7.2 执行所有 stream feature

```java
private void executeStreamFeatures(
        CompiledSceneRuntime runtime,
        EvalContext evalContext,
        ReadOnlyContext flinkCtx) throws Exception {

    for (Map.Entry<String, FeatureExecutor> entry : runtime.streamFeatureExecutors.entrySet()) {
        String featureCode = entry.getKey();
        FeatureExecutor executor = entry.getValue();

        Object value = executor.execute(evalContext, flinkCtx);
        evalContext.put(featureCode, value);
    }
}
```

---

### 7.3 一个 Count 滑动窗口执行器示意

下面这个伪代码主要讲“思路”，不完全追求 Flink API 细节正确。

```java
class CountSlidingWindowFeatureExecutor implements FeatureExecutor {

    private final StreamFeatureSpec spec;
    private transient MapState<Long, Long> bucketCountState;

    public CountSlidingWindowFeatureExecutor(StreamFeatureSpec spec) {
        this.spec = spec;
    }

    @Override
    public Object execute(EvalContext ctx, ReadOnlyContext flinkCtx) throws Exception {
        DecisionEvent event = ctx.event;

        // 1. 过滤判断
        if (!matchFilter(spec.getFilterExpr(), ctx)) {
            return readCurrentWindowValue(event.getEventTime());
        }

        long bucketTs = toBucket(event.getEventTime(), spec.getWindowSlideMillis());

        // 2. 更新当前桶计数
        Long current = bucketCountState.get(bucketTs);
        bucketCountState.put(bucketTs, current == null ? 1L : current + 1L);

        // 3. 注册清理 timer
        long cleanupTs = bucketTs + spec.getTtlMillis();
        registerCleanupTimer(cleanupTs);

        // 4. 汇总窗口范围内 bucket
        return readCurrentWindowValue(event.getEventTime());
    }

    private long readCurrentWindowValue(long now) throws Exception {
        long start = now - spec.getWindowSizeMillis();
        long sum = 0L;
        for (Map.Entry<Long, Long> entry : bucketCountState.entries()) {
            if (entry.getKey() >= start) {
                sum += entry.getValue();
            }
        }
        return sum;
    }
}
```

---

### 7.4 一个 Sum 执行器示意

```java
class SumSlidingWindowFeatureExecutor implements FeatureExecutor {

    private final StreamFeatureSpec spec;
    private transient MapState<Long, BigDecimal> bucketSumState;

    @Override
    public Object execute(EvalContext ctx, ReadOnlyContext flinkCtx) throws Exception {
        if (!matchFilter(spec.getFilterExpr(), ctx)) {
            return readCurrentWindowSum(ctx.event.getEventTime());
        }

        BigDecimal value = readNumberFromExpr(spec.getValueExpr(), ctx);
        long bucketTs = toBucket(ctx.event.getEventTime(), spec.getWindowSlideMillis());

        BigDecimal current = bucketSumState.get(bucketTs);
        bucketSumState.put(bucketTs, current == null ? value : current.add(value));

        registerCleanupTimer(bucketTs + spec.getTtlMillis());

        return readCurrentWindowSum(ctx.event.getEventTime());
    }
}
```

---

### 7.5 一个 Latest 执行器示意

```java
class LatestValueFeatureExecutor implements FeatureExecutor {

    private transient ValueState<Object> latestValueState;
    private transient ValueState<Long> latestTimeState;
    private final StreamFeatureSpec spec;

    @Override
    public Object execute(EvalContext ctx, ReadOnlyContext flinkCtx) throws Exception {
        long eventTime = ctx.event.getEventTime();
        Object candidate = readValueByExpr(spec.getValueExpr(), ctx);

        Long latestTs = latestTimeState.value();
        if (latestTs == null || eventTime >= latestTs) {
            latestTimeState.update(eventTime);
            latestValueState.update(candidate);
        }
        return latestValueState.value();
    }
}
```

---

## 八、Lookup 特征执行器伪代码

这类特征一般来自 Redis / KV。

---

### 8.1 接口定义

```java
interface LookupFeatureExecutor {
    Object lookup(EvalContext ctx) throws Exception;
}
```

---

### 8.2 执行所有 lookup 特征

```java
private void executeLookupFeatures(CompiledSceneRuntime runtime, EvalContext ctx) throws Exception {
    for (Map.Entry<String, LookupFeatureExecutor> entry : runtime.lookupFeatureExecutors.entrySet()) {
        String featureCode = entry.getKey();
        Object value = entry.getValue().lookup(ctx);
        ctx.put(featureCode, value);
    }
}
```

---

### 8.3 Redis Set 命中示意

```java
class RedisSetMembershipExecutor implements LookupFeatureExecutor {

    private final LookupFeatureSpec spec;
    private final RedisLookupService redisLookupService;

    @Override
    public Object lookup(EvalContext ctx) {
        String entityKey = String.valueOf(resolveExpr(spec.getKeyExpr(), ctx));
        String redisKey = spec.getRedisKeyPrefix() + entityKey;

        Boolean hit = redisLookupService.exists(redisKey);
        return hit != null ? hit : spec.getDefaultValue();
    }
}
```

---

### 8.4 Redis Hash 查询示意

```java
class RedisHashValueExecutor implements LookupFeatureExecutor {

    private final LookupFeatureSpec spec;
    private final RedisLookupService redisLookupService;

    @Override
    public Object lookup(EvalContext ctx) {
        String entityKey = String.valueOf(resolveExpr(spec.getKeyExpr(), ctx));
        String redisKey = spec.getRedisKeyPrefix() + entityKey;

        Object value = redisLookupService.get(redisKey);
        return value != null ? value : spec.getDefaultValue();
    }
}
```

---

## 九、派生特征执行器伪代码

派生特征本质上是“生成变量”，不是直接做最终决策。

---

### 9.1 接口定义

```java
interface DerivedFeatureInvoker {
    Object eval(EvalContext ctx) throws Exception;
}
```

---

### 9.2 执行顺序

派生特征可能依赖别的派生特征，所以必须先在发布时做好拓扑排序。

```java
private void executeDerivedFeatures(CompiledSceneRuntime runtime, EvalContext ctx) throws Exception {
    for (String featureCode : runtime.orderedDerivedFeatures) {
        DerivedFeatureInvoker invoker = runtime.derivedFeatureInvokers.get(featureCode);
        Object value = invoker.eval(ctx);
        ctx.put(featureCode, value);
    }
}
```

---

## 十、规则执行器伪代码

---

### 10.1 RuleInvoker 接口

```java
interface RuleInvoker {
    RuleHit eval(EvalContext ctx) throws Exception;
}
```

---

### 10.2 执行所有规则

```java
private List<RuleHit> executeRules(CompiledSceneRuntime runtime, EvalContext ctx) throws Exception {
    List<RuleHit> hits = new ArrayList<>();

    for (String ruleCode : runtime.orderedRules) {
        RuleInvoker invoker = runtime.ruleInvokers.get(ruleCode);
        RuleHit hit = invoker.eval(ctx);
        hits.add(hit);
        ctx.ruleHits.add(hit);
    }

    return hits;
}
```

---

### 10.3 一个表达式规则执行器示意

```java
class ExpressionRuleInvoker implements RuleInvoker {

    private final RuleSpec spec;
    private final CompiledExpression compiledExpression;

    @Override
    public RuleHit eval(EvalContext ctx) throws Exception {
        boolean matched = compiledExpression.execute(ctx.values);

        RuleHit hit = new RuleHit();
        hit.ruleCode = spec.getRuleCode();
        hit.hit = matched;
        hit.action = matched ? spec.getHitAction() : null;
        hit.score = matched ? spec.getRiskScore() : 0;
        hit.reason = matched ? renderReason(spec.getHitReasonTemplate(), ctx) : null;
        hit.detail = matched ? extractRuleDetail(spec, ctx) : Collections.emptyMap();

        return hit;
    }
}
```

---

### 10.4 一个 Groovy 规则执行器示意

```java
class GroovyRuleInvoker implements RuleInvoker {

    private final RuleSpec spec;
    private final GroovyScriptExecutor scriptExecutor;

    @Override
    public RuleHit eval(EvalContext ctx) throws Exception {
        boolean matched = scriptExecutor.executeBoolean(ctx.values);

        RuleHit hit = new RuleHit();
        hit.ruleCode = spec.getRuleCode();
        hit.hit = matched;
        hit.action = matched ? spec.getHitAction() : null;
        hit.score = matched ? spec.getRiskScore() : 0;
        hit.reason = matched ? renderReason(spec.getHitReasonTemplate(), ctx) : null;
        hit.detail = matched ? extractRuleDetail(spec, ctx) : Collections.emptyMap();

        return hit;
    }
}
```

---

## 十一、策略执行器伪代码

规则执行只是“逐条判断”，最终还需要策略层做收敛。

---

### 11.1 策略接口

```java
interface PolicyExecutor {
    DecisionResult decide(EvalContext ctx, List<RuleHit> hits);
}
```

---

### 11.2 FIRST\_HIT 策略

```java
class FirstHitPolicyExecutor implements PolicyExecutor {

    private final PolicySpec spec;

    @Override
    public DecisionResult decide(EvalContext ctx, List<RuleHit> hits) {
        for (RuleHit hit : hits) {
            if (hit.hit) {
                DecisionResult result = new DecisionResult();
                result.setFinalAction(hit.action);
                result.setFinalScore(hit.score);
                return result;
            }
        }

        DecisionResult result = new DecisionResult();
        result.setFinalAction(spec.getDefaultAction());
        result.setFinalScore(0);
        return result;
    }
}
```

---

### 11.3 SCORE\_CARD 策略

```java
class ScoreCardPolicyExecutor implements PolicyExecutor {

    private final PolicySpec spec;

    @Override
    public DecisionResult decide(EvalContext ctx, List<RuleHit> hits) {
        int totalScore = 0;
        for (RuleHit hit : hits) {
            if (hit.hit) {
                totalScore += hit.score;
            }
        }

        String finalAction = resolveActionByScore(totalScore, spec.getScoreThresholds());

        DecisionResult result = new DecisionResult();
        result.setFinalAction(finalAction);
        result.setFinalScore(totalScore);
        return result;
    }
}
```

---

## 十二、表达式执行器伪代码

这一部分对应附录里要求的“表达式/Groovy 执行器伪代码”。

---

### 12.1 表达式执行器统一接口

```java
interface CompiledExpression {
    boolean execute(Map<String, Object> context) throws Exception;
}
```

---

### 12.2 Aviator 执行器示意

```java
class AviatorCompiledExpression implements CompiledExpression {

    private final Expression expression;

    @Override
    public boolean execute(Map<String, Object> context) {
        Object result = expression.execute(context);
        return Boolean.TRUE.equals(result);
    }
}
```

---

### 12.3 表达式编译器示意

```java
class ExpressionCompiler {

    CompiledExpression compile(String expr) {
        Expression aviatorExpr = AviatorEvaluator.compile(expr, true);
        return new AviatorCompiledExpression(aviatorExpr);
    }
}
```

这里一定要注意：

- 编译发生在**快照切换时**
- 不是每条事件都编译

---

## 十三、Groovy 执行器伪代码

---

### 13.1 Groovy 执行接口

```java
interface GroovyScriptExecutor {
    boolean executeBoolean(Map<String, Object> context) throws Exception;
}
```

---

### 13.2 一个简单执行器示意

```java
class DefaultGroovyScriptExecutor implements GroovyScriptExecutor {

    private final Class<?> scriptClass;

    @Override
    public boolean executeBoolean(Map<String, Object> context) throws Exception {
        Binding binding = new Binding();
        for (Map.Entry<String, Object> entry : context.entrySet()) {
            binding.setVariable(entry.getKey(), entry.getValue());
        }

        Script script = (Script) scriptClass.getDeclaredConstructor().newInstance();
        script.setBinding(binding);
        Object result = script.run();
        return Boolean.TRUE.equals(result);
    }
}
```

---

### 13.3 Groovy 编译器示意

```java
class GroovyCompiler {

    GroovyScriptExecutor compile(String scriptText) {
        GroovyClassLoader classLoader = buildSandboxClassLoader();
        Class<?> scriptClass = classLoader.parseClass(scriptText);
        return new DefaultGroovyScriptExecutor(scriptClass);
    }
}
```

---

### 13.4 Groovy 使用注意点

Groovy 一定要注意：

1. **不能每条事件编译**
2. **必须沙箱化**
3. **不能允许文件 IO / 网络调用 / System.exit**
4. **需要考虑 classloader 回收**
5. **编译失败不能影响旧版本运行**

---

## 十四、RuntimeCompiler 伪代码

这个类负责把快照编译成本地运行时对象。

```java
class RuntimeCompiler {

    private final ExpressionCompiler expressionCompiler = new ExpressionCompiler();
    private final GroovyCompiler groovyCompiler = new GroovyCompiler();
    private final FeatureExecutorFactory featureExecutorFactory = new FeatureExecutorFactory();
    private final PolicyExecutorFactory policyExecutorFactory = new PolicyExecutorFactory();

    CompiledSceneRuntime compile(SceneSnapshot snapshot) {
        CompiledSceneRuntime runtime = new CompiledSceneRuntime();
        runtime.snapshot = snapshot;

        // 1. 编译 stream feature 执行器
        runtime.streamFeatureExecutors = new HashMap<>();
        for (StreamFeatureSpec spec : snapshot.getStreamFeatures()) {
            runtime.streamFeatureExecutors.put(spec.getCode(), featureExecutorFactory.buildStreamExecutor(spec));
        }

        // 2. 编译 lookup feature 执行器
        runtime.lookupFeatureExecutors = new HashMap<>();
        for (LookupFeatureSpec spec : snapshot.getLookupFeatures()) {
            runtime.lookupFeatureExecutors.put(spec.getCode(), featureExecutorFactory.buildLookupExecutor(spec));
        }

        // 3. 编译 derived feature
        runtime.derivedFeatureInvokers = new HashMap<>();
        runtime.orderedDerivedFeatures = topoSortDerivedFeatures(snapshot.getDerivedFeatures());
        for (DerivedFeatureSpec spec : snapshot.getDerivedFeatures()) {
            runtime.derivedFeatureInvokers.put(spec.getCode(), buildDerivedInvoker(spec));
        }

        // 4. 编译 rules
        runtime.ruleInvokers = new HashMap<>();
        runtime.orderedRules = snapshot.getPolicy().getRuleOrder();
        for (RuleSpec spec : snapshot.getRules()) {
            runtime.ruleInvokers.put(spec.getRuleCode(), buildRuleInvoker(spec));
        }

        // 5. 编译 policy
        runtime.policyExecutor = policyExecutorFactory.build(snapshot.getPolicy());

        return runtime;
    }

    private DerivedFeatureInvoker buildDerivedInvoker(DerivedFeatureSpec spec) {
        if ("AVIATOR".equals(spec.getEngineType())) {
            CompiledExpression expr = expressionCompiler.compile(spec.getExpr());
            return ctx -> expr.execute(ctx.values);
        }
        if ("GROOVY".equals(spec.getEngineType())) {
            GroovyScriptExecutor script = groovyCompiler.compile(spec.getExpr());
            return ctx -> script.executeBoolean(ctx.values);
        }
        throw new IllegalArgumentException("unsupported derived feature engine: " + spec.getEngineType());
    }

    private RuleInvoker buildRuleInvoker(RuleSpec spec) {
        if ("AVIATOR".equals(spec.getEngineType())) {
            CompiledExpression expr = expressionCompiler.compile(spec.getWhenExpr());
            return new ExpressionRuleInvoker(spec, expr);
        }
        if ("GROOVY".equals(spec.getEngineType())) {
            GroovyScriptExecutor script = groovyCompiler.compile(spec.getWhenExpr());
            return new GroovyRuleInvoker(spec, script);
        }
        throw new IllegalArgumentException("unsupported rule engine: " + spec.getEngineType());
    }
}
```

---

## 十五、Side Output 设计伪代码

实时决策系统里，强烈建议把：

- 主结果
- 决策日志
- 错误日志

分开输出。

```java
class OutputTags {
    static final OutputTag<DecisionLog> DECISION_LOG_TAG = new OutputTag<>("decision-log", TypeInformation.of(DecisionLog.class));
    static final OutputTag<EngineErrorLog> ERROR_TAG = new OutputTag<>("engine-error-log", TypeInformation.of(EngineErrorLog.class));
}
```

这样做的好处是：

- 主链路更清晰
- 日志落库可以异步处理
- 错误日志不会污染主结果流

---

## 十六、错误处理伪代码

---

### 16.1 无快照错误

```java
private void emitNoSnapshotError(DecisionEvent event, Collector<DecisionResult> out, long start) {
    DecisionResult result = new DecisionResult();
    result.setEventId(event.getEventId());
    result.setTraceId(event.getTraceId());
    result.setSceneCode(event.getSceneCode());
    result.setFinalAction("ERROR");
    result.setErrorMsg("no active snapshot found");
    result.setLatencyMs(System.currentTimeMillis() - start);
    out.collect(result);
}
```

---

### 16.2 执行异常错误

```java
private void emitExecutionError(
        DecisionEvent event,
        SceneSnapshot snapshot,
        Exception e,
        Collector<DecisionResult> out,
        long start) {

    DecisionResult result = new DecisionResult();
    result.setEventId(event.getEventId());
    result.setTraceId(event.getTraceId());
    result.setSceneCode(event.getSceneCode());
    result.setVersion(snapshot.getVersion());
    result.setFinalAction("ERROR");
    result.setErrorMsg(e.getMessage());
    result.setLatencyMs(System.currentTimeMillis() - start);
    out.collect(result);
}
```

注意：

- 错误最好尽量局部吞掉并输出错误日志
- 不要因为单条事件失败把整个 task 打挂

---

## 十七、Checkpoint 恢复后的关键处理

在 Flink 里，一个很容易被忽视的问题是：

- Broadcast State 会恢复
- 但本地 transient cache 不会恢复

所以 `processElement` 中必须有类似这样的逻辑：

```java
if (runtime == null || runtime.snapshot.getVersion() != snapshot.getVersion()) {
    runtime = runtimeCompiler.compile(snapshot);
    localRuntimeCache.put(sceneCode, runtime);
}
```

这一步非常重要。

否则会出现：

- checkpoint 恢复成功
- 但本地表达式 / Groovy 执行器为空
- 后续事件直接报错

---

## 十八、第一版工程实现建议

如果你现在准备按这个附录落地，我建议你第一版引擎按下面顺序实现：

### 第一步：先只支持

- 1 个 scene
- 1 个 snapshot
- 表达式规则
- FIRST\_HIT
- 2\~3 个 stream feature
- Redis 黑名单查询

### 第二步：再加

- 多 scene
- Broadcast State 动态切换
- Derived feature
- ScoreCard

### 第三步：最后再加

- Groovy
- 更复杂 feature
- 回放测试
- 更复杂日志与指标

这样推进最稳。

---

## 十九、本附录小结

这份附录最关键的是给你一条非常清晰的 Flink 引擎骨架：

1. **事件流 + 配置广播流** 是整个引擎的两条输入主线
2. `processBroadcastElement` 负责**接收快照、编译运行时对象、切换版本**
3. `processElement` 负责**构建上下文、计算特征、执行规则与策略**
4. 流式特征、lookup 特征、派生特征、规则、策略都应该做成**可替换的执行器抽象**
5. 表达式与 Groovy 一定要在**快照切换时预编译**，不能每条事件动态编译
6. Broadcast State 存**可恢复快照**，本地 transient cache 存**编译后的运行时对象**
7. 主结果、决策日志、错误日志最好分流输出

如果你后面要真正开始写 `engine` 模块，这一份内容已经足够作为第一版设计蓝图。
