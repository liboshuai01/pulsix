这章开始，我们正式从“系统理论设计”进入“控制平台后端如何落代码”。

## 20.1 这一章解决什么问题

前面第 1～19 章，我们已经把这套实时风控平台的理论骨架基本讲清楚了：

- 它是什么系统
- 核心领域对象是什么
- 架构如何分层
- 发布机制为什么要有快照
- Flink 如何接收快照、构建上下文、执行特征、规则、策略
- 表达式与 Groovy 的边界是什么
- 一条事件在引擎里如何完整流转

但是到这里，如果你真的准备开始编码，马上就会碰到一个更现实的问题：

> 控制平台这个 Spring Boot 后端，到底应该怎么拆模块、怎么分层、哪些逻辑放哪儿、发布怎么写、仿真怎么做、日志和审计怎么接？

这一章就是为了解决这个问题。

它不是讲“单个接口怎么写”，而是先把整个控制平台后端的工程形态设计出来，让你后续写代码时不会一开始就乱。

---

## 20.2 先定一个总原则：控制平台不是普通 CRUD 后台

你这个项目的控制平台，虽然表面上看有很多后台管理页面，比如：

- 场景管理
- 特征管理
- 规则管理
- 策略管理
- 发布中心
- 仿真测试
- 日志查询

但它本质上并不是一个“只做 CRUD 的后台系统”。

它真正承担的是 4 类能力：

1. **设计态管理**：管理 scene / feature / rule / policy / list 等设计态对象
2. **发布编译**：把设计态多表编译成运行时快照
3. **离线验证**：仿真执行、依赖检查、表达式校验、Groovy 校验
4. **运行态辅助查询**：发布记录、决策日志、审计日志、版本查询

所以你后端模块设计时，不能只按“表 -> service -> controller”去想，必须围绕这 4 类能力来组织。

---

## 20.3 控制平台后端的总体设计目标

对于你这个个人开源项目，我建议控制平台后端追求下面几个目标：

### 20.3.1 单体部署，逻辑分层

不要一开始拆很多微服务。

建议让 `pulsix-server` 作为 Spring Boot 启动容器承载控制平台进程，核心风控业务则集中在 `pulsix-module-risk` 中按模块和分层组织。

原因很简单：

- 一个人开发效率最高
- 联调成本低
- 部署和排错简单
- 依然可以把架构思想讲清楚

也就是说：

> **逻辑上分层，物理上单体**

这是当前阶段最合理的方案。

---

### 20.3.2 强调“设计态”和“运行态”分离

控制平台管理的是设计态对象，但又要产出运行态快照。

所以系统内部必须明确区分两类模型：

- 设计态模型：SceneDef、FeatureDef、RuleDef、PolicyDef 等
- 运行态模型：SceneSnapshot、FeatureSpec、RuleSpec、PolicySpec 等

这两套模型不要混着用。

---

### 20.3.3 发布逻辑必须作为一等能力

在这个项目里，发布不是附加页面，而是控制平台的核心能力之一。

所以在后端设计里，必须有清晰的：

- 发布应用服务
- 快照编译器
- 依赖分析器
- 表达式/Groovy 校验器
- 发布记录持久化
- 配置推送组件

---

### 20.3.4 仿真必须复用规则执行核心

仿真页不能自己单独写一套“看起来差不多”的判断逻辑。

正确做法是：

- 控制平台仿真服务依赖统一的 `pulsix-kernel`
- 和 Flink 引擎共用表达式执行器 / Groovy 执行器 / Policy 执行逻辑

这样才能保证：

- 仿真结果可信
- 线上线下一致
- 维护成本低

---

## 20.4 推荐的 Spring Boot 控制平台工程结构

### 20.4.1 如果你做成单仓库，建议结构如下

```latex
pulsix/
├── pulsix-framework/
│   ├── pulsix-common/
│   ├── pulsix-kernel/
│   └── pulsix-spring-boot-starter-*/
├── pulsix-server/                # Spring Boot 启动器
├── pulsix-module-system/         # 用户、权限、租户、菜单、审计
├── pulsix-module-infra/          # 配置、文件、任务、监控、基础日志
├── pulsix-module-risk/           # 风控控制面核心业务
├── pulsix-engine/                # Flink 实时风控引擎
├── pulsix-ui/                    # Vue3 前端
└── docs/
```

其中第 20 章主要讲的是控制平台后端中的 `pulsix-module-risk`，而 `pulsix-server` 只负责启动与聚合。

---

### 20.4.2 `pulsix-module-risk` 内部建议的逻辑分层

```latex
pulsix-module-risk
├── controller      # HTTP 接口层
├── service         # 应用服务 / 领域流程
│   ├── scene
│   ├── event
│   ├── feature
│   ├── list
│   ├── rule
│   ├── policy
│   ├── release
│   ├── simulation
│   └── log
├── dal             # DO / Mapper / Repository
├── convert         # DTO / DO / VO 转换
├── api             # 对外 CommonApi 或领域接口
├── enums
├── mq
└── framework       # 本模块内部配置与支撑
```

这是更贴近当前 `pulsix` 工程风格的一种分层方式。

它不要求你强行套特别重的 DDD，但能让控制平台核心业务边界保持清晰，同时又和现有 `system / infra` 模块风格一致。

---

## 20.5 控制平台建议拆成哪些业务模块

我建议你按“平台能力”拆模块，而不是按数据库表拆模块。

### 20.5.1 系统与权限模块

负责：

- 登录
- 用户管理
- 角色管理
- 菜单权限
- 接口权限
- 审计日志

你可以用：

- Spring Security 6
- JWT
- RBAC

这是平台基础，不需要过度设计。

---

### 20.5.2 场景与事件模型模块

负责：

- 场景管理
- 事件模型管理
- 事件字段定义
- 默认值 / 必填校验规则配置
- 标准事件预览
- 接入映射配置的标准字段支撑

这是所有规则和特征的上游元数据模块。

这里建议把边界再收紧一点：

- 《事件模型》页只维护标准结构
- 《接入映射》页独立放在接入治理下，维护 `eventCode + sourceCode` 维度的原始结构、样例报文与标准化规则；脚本映射一期统一使用 Aviator，最小上下文只暴露 `rawPayload / headers / sourceCode / sceneCode / eventCode`

这里我建议你再加一个非常务实的判断：

> **业务平台上传的原始报文，不要默认它已经等于标准 `RiskEvent`。**

所以控制平台虽然不需要做成一个重型 ETL 系统，但至少要能管理“事件如何映射成标准风控事件”的这组接入规则。否则后面的 Flink 执行链路、仿真输入、日志追溯，都会默认建立在一份并不存在的理想输入之上。

---

### 20.5.3 特征中心模块

负责：

- Stream Feature 管理
- Lookup Feature 管理
- Derived Feature 管理
- 特征校验
- 特征依赖分析

这个模块非常关键，因为它直接连接后面的快照编译和 Flink 引擎执行。

---

### 20.5.4 名单中心模块

负责：

- 名单集合定义
- 名单项管理
- 批量导入导出
- 名单状态启停
- 运行态同步 Redis

建议名单模块独立，不要把名单直接混成规则的一部分。

---

### 20.5.5 规则中心模块

负责：

- 规则定义
- 表达式/Groovy 配置
- 规则校验
- 命中动作配置
- 优先级配置
- 命中原因模板配置

这一层只定义“单条判断逻辑”，不要把策略编排也塞进去。

---

### 20.5.6 策略中心模块

负责：

- 策略定义
- 规则顺序编排
- 决策模式配置
- 默认动作配置
- 评分卡参数配置（后续）

规则和策略一定要分开。

- Rule 负责单条判断
- Policy 负责组织多条 Rule

---

### 20.5.7 发布中心模块

这是控制平台的核心模块之一，负责：

- 发布前校验
- 依赖分析
- 快照编译
- 版本生成
- 发布记录存储
- 回滚
- 配置推送

如果只能强调一个模块的重要性，那就是这个模块。

---

### 20.5.8 仿真测试模块

负责：

- 输入测试事件
- 指定场景 / 指定版本执行
- 调用统一规则执行内核
- 返回特征快照、命中规则、最终结果
- 保存仿真报告

这是平台体验和可信度的关键功能。

---

### 20.5.9 日志与分析查询模块

负责：

- 决策日志查询
- 发布记录查询
- 审计日志查询
- 命中规则查询
- 基础 Dashboard 数据汇总接口

注意：这里主要是“查询 API”，不是真正重型分析引擎。

---

### 20.5.10 接入治理模块

建议做，但保持轻量。

负责：

- 接入源管理
- 接入映射管理（含 Aviator 脚本映射预览）
- 鉴权配置管理
- 来源启停与基础限流配置
- 错误事件 / `DLQ` 查询
- 接入监控与 SDK 接入指引

这部分的目标不是做一个重型“接入平台”，而是给 `pulsix-access/pulsix-ingest` 和 `pulsix-access/pulsix-sdk` 提供基本治理能力。

---

## 20.6 推荐的代码分层方式

这一节非常关键。很多人项目后面难维护，问题就出在这里。

我建议你后端按 5 层理解：

1. Controller 层
2. Application 层
3. Domain 层
4. Infrastructure 层
5. Support/Common 层

---

### 20.6.1 Controller 层：只做接口适配

Controller 的职责应该尽量薄，负责：

- 接收请求
- 参数校验
- 调用 application service
- 返回统一响应对象

不要把下面这些写进 Controller：

- 表达式编译
- 快照组装
- 跨表事务
- 依赖分析
- Redis 同步逻辑

Controller 只负责把 HTTP 请求转换成应用命令。

例如：

```java
@RestController
@RequestMapping("/api/release")
@RequiredArgsConstructor
public class ReleaseController {

    private final ReleaseApplicationService releaseApplicationService;

    @PostMapping("/publish/{sceneCode}")
    public Result<Long> publish(@PathVariable String sceneCode,
                                @RequestBody PublishRequest request) {
        Long releaseId = releaseApplicationService.publish(sceneCode, request);
        return Result.success(releaseId);
    }
}
```

---

### 20.6.2 Application 层：组织用例流程

Application 层负责“把一次业务用例串起来”。

例如：

- 创建特征
- 发布场景
- 回滚版本
- 仿真测试
- 导入名单

它更关心“流程”，不关心底层如何持久化。

例如发布流程：

- 加载 scene
- 加载 feature / rule / policy
- 调用校验器
- 调用依赖分析器
- 调用快照编译器
- 保存 release record
- 推送配置事件
- 写审计日志

这些都很适合放在 application service。

---

### 20.6.3 Domain 层：承载核心领域规则

Domain 层不要理解得太玄。

你在这个项目里，可以把它理解成：

- 领域对象
- 领域服务
- 校验器
- 编译器接口
- 决策模式抽象

比如：

- `FeatureDefinition`
- `RuleDefinition`
- `PolicyDefinition`
- `FeatureDependencyAnalyzer`
- `RuleSyntaxValidator`
- `SceneSnapshotCompiler`

这些东西属于平台的核心业务逻辑，不应该直接和 Controller 或 DAO 混在一起。

---

### 20.6.4 Infrastructure 层：持久化与外部集成

Infrastructure 层负责：

- MyBatis-Plus Mapper / Repository
- Kafka Producer
- Redis 同步
- 外部配置推送
- 文件导入导出

它的职责是“把领域逻辑落地到外部系统”，而不是定义领域规则。

---

### 20.6.5 Support/Common 层：通用技术支撑

例如：

- 异常体系
- 统一响应对象
- 分页
- 枚举
- 时间工具
- JSON 工具
- 安全上下文
- 审计切面

这些应该统一抽到公共支撑层，不要散落到业务模块里。

---

## 20.7 一个推荐的包结构示例

下面给你一个比较务实的 `pulsix-module-risk` 包结构示例：

```latex
cn.liboshuai.pulsix.module.risk
├── api/
│   ├── scene/
│   ├── feature/
│   ├── access/
│   ├── release/
│   ├── simulation/
│   └── log/
├── controller/
│   └── admin/
│       ├── scene/
│       ├── event/
│       ├── feature/
│       ├── list/
│       ├── rule/
│       ├── policy/
│       ├── access/
│       ├── release/
│       ├── simulation/
│       └── log/
├── service/
│   ├── scene/
│   ├── event/
│   ├── feature/
│   ├── access/
│   ├── list/
│   ├── rule/
│   ├── policy/
│   ├── release/
│   │   ├── validator/
│   │   ├── compiler/
│   │   └── publisher/
│   ├── simulation/
│   └── log/
├── convert/
├── dal/
│   ├── dataobject/
│   └── mysql/
├── enums/
├── mq/
│   ├── message/
│   └── producer/
└── framework/
```

这是比较适合当前项目的“按业务域收敛 + 按模块分层”的方式。

它的优点是：

- 模块边界清楚
- 风控主业务集中在 `risk` 域，不会和 `system / infra` 混淆
- 每个业务域内部仍然可以继续按 controller / service / dal / convert 细分

---

## 20.8 核心模块应该有哪些应用服务

下面我把几个关键模块的 application service 讲一下。

### 20.8.1 FeatureApplicationService

负责：

- 新建/编辑特征
- 删除特征
- 特征合法性校验
- 特征依赖预检查
- 查询特征详情

注意：

- Stream / Lookup / Derived 三类特征可以共用一个入口，但内部要按类型分发

---

### 20.8.2 RuleApplicationService

负责：

- 规则创建与修改
- 表达式/Groovy 校验
- 规则启停
- 规则预览
- 规则依赖提示

---

### 20.8.3 PolicyApplicationService

负责：

- 策略创建
- 规则顺序绑定
- 决策模式配置
- 策略合法性检查

---

### 20.8.4 ReleaseApplicationService

这是重中之重，负责：

- 校验是否允许发布
- 组装发布上下文
- 调用 snapshotCompiler
- 持久化 scene\_release
- 通过 MySQL CDC 触发配置下发链路
- 写审计日志

它通常不会自己干所有细节，而是调用一组协作者。

---

### 20.8.5 SimulationApplicationService

负责：

- 接收测试事件
- 指定 scene/version
- 构建仿真上下文
- 调用统一规则执行内核
- 返回仿真结果
- 保存仿真报告

---

## 20.9 发布中心建议拆出的几个关键协作者

如果你把所有发布逻辑都塞进 `ReleaseApplicationService.publish()` 一个方法里，后面会非常难维护。

我建议至少拆出下面这些组件：

### 20.9.1 `SceneConfigLoader`

负责按 scene 加载当前设计态全量配置。

输入： `sceneCode`
输出： `SceneDesignBundle`

---

### 20.9.2 `SceneConfigValidator`

负责发布前检查。

例如：

- 配置是否完整
- 引用是否存在
- 策略是否有主 policy
- 规则表达式是否可编译

---

### 20.9.3 `DependencyAnalyzer`

负责分析：

- 规则依赖哪些变量
- derived feature 依赖链
- 是否有循环依赖
- 当前场景真正需要哪些 stream feature / lookup feature

---

### 20.9.4 `SceneSnapshotCompiler`

负责把设计态 bundle 编译成运行态 snapshot。

这是整个控制平台后端最有“平台味”的组件之一。

---

### 20.9.5 `SceneReleaseRepository`

负责把生成的 snapshot 写入 `scene_release`。

当前系统保持单一发布链路即可：

- 控制平台落库到 `scene_release`
- Flink 通过 MySQL CDC 感知新版本

不再额外引入主动推送、Kafka config topic 或其他第二配置通路。

---

## 20.10 发布逻辑的推荐代码组织方式

下面给你一个推荐的发布伪代码：

```java
@Service
@RequiredArgsConstructor
public class ReleaseApplicationService {

    private final SceneConfigLoader sceneConfigLoader;
    private final SceneConfigValidator sceneConfigValidator;
    private final DependencyAnalyzer dependencyAnalyzer;
    private final SceneSnapshotCompiler sceneSnapshotCompiler;
    private final SceneReleaseRepository sceneReleaseRepository;
    private final AuditLogService auditLogService;

    @Transactional
    public Long publish(String sceneCode, PublishRequest request) {
        SceneDesignBundle bundle = sceneConfigLoader.load(sceneCode);

        sceneConfigValidator.validate(bundle);

        DependencyGraph dependencyGraph = dependencyAnalyzer.analyze(bundle);

        CompiledSnapshot compiledSnapshot = sceneSnapshotCompiler.compile(bundle, dependencyGraph);

        SceneRelease release = sceneReleaseRepository.save(compiledSnapshot, request);

        auditLogService.recordPublish(sceneCode, release.getVersionNo(), request.getRemark());

        return release.getId();
    }
}
```

这个写法的好处是：

- 发布流程清晰
- 每个组件职责单一
- 单元测试容易写
- 后续做回滚和灰度都方便扩展

---

## 20.11 仿真模块怎么实现才合理

很多人会把仿真做成“重新写一遍判断逻辑”，这是不对的。

### 正确原则

仿真模块应该：

- 尽量复用 `pulsix-kernel`
- 复用表达式/Groovy 执行器抽象
- 复用 policy 执行逻辑
- 复用 hit reason 生成逻辑

不同点只在于：

- 线上 Flink 上下文来自实时状态 + Redis
- 仿真上下文来自模拟事件 + 可选 mock 特征 / 可选 Redis 查询

---

### 仿真建议支持两种模式

#### 模式 A：纯输入事件 + 手工特征补齐

适合开发初期。

#### 模式 B：输入事件 + 实时查询 Redis + 执行派生规则

更接近真实线上。

后面成熟一点还可以做：

- 指定版本仿真
- 指定历史版本对比仿真

---

## 20.12 日志查询模块怎么设计

控制平台的日志查询模块，不建议承担复杂分析任务，但至少要做好下面这些：

### 20.12.1 决策日志查询

支持按：

- traceId
- eventId
- sceneCode
- userId
- 时间范围

查询：

- 输入事件
- 命中规则
- 特征快照
- 最终动作
- 版本号
- latency

---

### 20.12.2 发布记录查询

支持查：

- 某场景所有版本
- 当前运行版本
- 发布说明
- 发布人
- 发布时间
- 是否回滚版本

---

### 20.12.3 审计日志查询

支持查：

- 谁改了 rule/feature/policy
- 改前改后
- 谁发布了版本

---

## 20.13 审计日志应该如何接入

我建议你不要在每个 service 方法里手写审计日志。

比较合理的做法有两种：

### 方案 A：应用服务显式记录

优点：简单直接，可控。

### 方案 B：注解 + AOP 自动记录

优点：统一，但前期可能略复杂。

对你来说，一期建议：

- 关键动作显式记录
- 比如：创建规则、编辑规则、发布、回滚、导入名单

这样更稳，也更容易调试。

---

## 20.14 DTO、DO、VO、Snapshot 对象不要混用

这是一个非常常见但很容易被忽视的点。

我建议你明确分开下面几类对象：

### 20.14.1 Request/Command

前端传入对象。

例如：

- `CreateRuleRequest`
- `PublishRequest`

---

### 20.14.2 DO / Entity

数据库持久化对象。

例如：

- `RuleDefDO`
- `FeatureDefDO`
- `SceneReleaseDO`

---

### 20.14.3 Domain Model

业务处理中使用的领域对象。

例如：

- `RuleDefinition`
- `FeatureDefinition`
- `PolicyDefinition`

---

### 20.14.4 Snapshot Model

运行态快照对象。

例如：

- `SceneSnapshot`
- `RuleSpec`
- `FeatureSpec`

---

### 20.14.5 VO / Response

接口返回给前端的对象。

例如：

- `RuleDetailVO`
- `ReleaseHistoryVO`

这样做的好处是：

- 边界清晰
- 不容易互相污染
- 发布逻辑和页面逻辑不会混在一起

---

## 20.15 数据库事务边界应该怎么把握

控制平台里，不是所有操作都要一个超大事务。

### 建议这样处理：

#### 场景 / 规则 / 特征编辑

走常规本地事务即可。

#### 发布

建议：

- 设计态加载和编译在事务外或读事务里
- `scene_release` 落库和审计日志可在一个事务里
- Kafka 推送可用事务后回调 / 最终一致方式

原因是：

- 发布涉及外部系统推送
- 不适合把所有外部动作都锁在一个 DB 事务里

比较稳的方案是：

1. 先保存 release record
2. 提交事务
3. 再推送 Kafka
4. 若失败则记录状态并可重试

当然一期你也可以先简单一点，但思路上要清楚。

---

## 20.16 控制平台和 Flink 引擎的代码边界应该怎么划

这点非常重要。

### 控制平台负责：

- 设计态 CRUD
- 校验
- 依赖分析
- Snapshot 编译
- 发布与回滚
- 仿真
- 查询

### Flink 引擎负责：

- 消费 snapshot
- 构建运行时缓存
- 实时特征计算
- 规则/策略执行
- 决策结果输出

### 共用模块负责：

- 表达式执行器抽象
- Groovy 执行器抽象
- Policy 执行逻辑
- Hit reason 模板渲染
- 部分 Snapshot Model

所以你后面在代码上要尽量做到：

> **共享规则执行内核，但不共享控制平台特有流程。**

---

## 20.17 一个适合你当前项目的最小可落地控制平台模块清单

如果你现在就要开工，我建议 `pulsix-module-risk` 一期至少做下面这些模块：

1. auth / user / role
2. scene
3. event-schema
4. feature
5. list
6. rule
7. policy
8. release
9. simulation
10. decision-log
11. audit
12. access-ingest-governance

其中真正的主链路优先级是：

- scene
- feature
- rule
- policy
- release
- simulation

这些优先做完，平台骨架就立起来了。

---

## 20.18 控制平台后端的推荐开发顺序

### 第一阶段

先做：

- 登录与权限
- 场景管理
- 事件模型管理
- 基础菜单

### 第二阶段

做：

- 特征中心
- 规则中心
- 策略中心
- 接入治理（来源、接入映射、鉴权、DLQ 查询）

### 第三阶段

做：

- 发布中心
- 快照编译器
- 依赖分析器
- 打通 `scene_release -> MySQL CDC -> Flink` 配置链路

### 第四阶段

做：

- 仿真测试
- 决策日志查询
- 审计日志

### 第五阶段

再加：

- 回滚
- Dashboard
- 更好的错误提示
- 更完整的验证与导入导出

这是比较顺的路线。

---

## 20.19 这一章最重要的设计结论

如果把这一章浓缩成最关键的几个结论，我建议你记住下面这些：

### 结论 1

控制平台不是普通 CRUD 后台，而是“设计态管理 + 发布编译中心”。

### 结论 2

建议用单体 Spring Boot 承载控制平台，但内部必须按模块和分层组织。

### 结论 3

发布中心、快照编译器、依赖分析器、仿真模块，是控制平台最有含金量的部分。

### 结论 4

Controller 要薄，Application 负责流程，Domain 承载核心规则，Infrastructure 负责落地集成。

### 结论 5

仿真必须尽量复用统一规则执行内核，而不是另写一套判断逻辑。

### 结论 6

DTO / DO / Domain / Snapshot / VO 要分开，不要混用。

---

## 20.20 本章小结

这一章我们正式把“控制平台理论”推进到了“Spring Boot 后端工程形态”。

核心内容可以总结为：

- 控制平台要按能力模块拆，不要按表机械拆
- 后端要有清晰分层：controller / application / domain / infrastructure
- 发布中心是核心模块，要把加载、校验、依赖分析、快照编译、发布推送分开
- 仿真和线上执行核心要尽量复用
- 控制平台与 Flink 引擎要边界清晰，但可以共享规则执行基础设施

到这里，你已经可以开始着手画 `pulsix-module-risk` 的真实工程结构了。

---

## 20.21 下一章会讲什么

下一章进入： **第 21 章：数据库、Redis、Kafka 的落地模型设计**。

那一章会更进一步，把控制平台与引擎真正需要落地的：

- MySQL 表结构组织
- Redis Key 设计
- Kafka Topic 设计
- 决策日志与发布记录的存储模型

全部连接起来。

也就是说，第 20 章解决的是“后端工程怎么拆”，第 21 章解决的是“数据和消息基础设施怎么落”。
