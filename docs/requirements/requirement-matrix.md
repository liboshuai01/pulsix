# Requirement Matrix（V1）

## 1. 目的与范围

- 本文件用于提供后续 AI 编码任务、代码评审、测试验收、版本对比的统一需求编号来源。
- 本版仅纳入**一期 P0 核心需求**，不把二期 / 三期增强项混入主表。
- 当前 `docs/contracts/*` 与 `docs/adr/0002` ~ `0005` 仍为占位，本版主要依据 `docs/方案/*` 长文档与 `docs/adr/0001-version-matrix.md` 提炼。

## 2. 一期明确不纳入主表的后置能力

- Groovy 全面开放
- CEP / 序列规则
- 灰度发布
- 多租户
- 拖拽式策略编排
- 同步决策 API
- 复杂评分卡增强版
- 分析型日志链路全量升级

## 3. 核心 Requirement Matrix

### 3.1 架构类需求

| ID | Requirement | Note |
| --- | --- | --- |
| REQ-ARCH-001 | 平台主链路必须围绕 `事件 -> 特征 -> 规则 -> 策略 -> 决策 -> 日志 -> 发布 / 回滚` 组织，不能退化为通用 CRUD 后台。 | P0 主链路 |
| REQ-ARCH-002 | 系统必须显式分离控制面、计算面、分析面；任一平面不得承担另外两者的核心职责。 | 三平面边界 |
| REQ-ARCH-003 | 控制面维护设计态对象，计算面只执行发布后的运行态快照；两类对象不得混用。 | 设计态 / 运行态分离 |
| REQ-ARCH-004 | Flink 引擎只能消费运行态快照，不得在运行时直接读取设计态多表拼装特征、规则或策略。 | 禁止运行时拼表 |
| REQ-ARCH-005 | 共享执行内核必须保持纯输入 / 纯输出、可重复执行、无外部 IO、无框架副作用，并同时服务仿真与线上执行。 | `pulsix-kernel` |
| REQ-ARCH-006 | 工程模块必须保持 `pulsix-module-risk` 控制面、`pulsix-engine` 计算面、`pulsix-framework/pulsix-kernel` 共享内核边界；`pulsix-engine` 不得依赖控制面业务实现。 | 模块边界 |

### 3.2 事件模型类需求

| ID | Requirement | Note |
| --- | --- | --- |
| REQ-EVENT-001 | 一期首批场景必须至少覆盖 `LOGIN_RISK`、`REGISTER_ANTI_FRAUD`、`TRADE_RISK`。 | 首批 3 场景 |
| REQ-EVENT-002 | 一期事件类型必须至少覆盖 `login`、`register`、`trade`、`withdraw`。 | 首批 4 事件 |
| REQ-EVENT-003 | 每个场景必须维护独立的 `Event Schema`、字段定义、样例报文和基础校验规则。 | 场景级事件模型 |
| REQ-EVENT-004 | 统一事件模型至少包含 `eventId`、`traceId`、`sceneCode`、`eventType`、`eventTime`、实体字段和 `ext`。 | 最小事件契约 |
| REQ-EVENT-005 | 一期实体类型必须至少覆盖 `user`、`device`、`ip`、`account`。 | 首批 4 类实体 |
| REQ-EVENT-006 | 事件接入层只允许做轻量标准化与合法性校验，禁止在接入层执行复杂业务决策或策略判断。 | 接入层不做决策 |

### 3.3 特征系统类需求

| ID | Requirement | Note |
| --- | --- | --- |
| REQ-FEATURE-001 | 特征体系必须显式区分 `stream feature`、`lookup feature`、`derived feature` 三类，不得混成单一执行模型。 | 三类特征 |
| REQ-FEATURE-002 | 一期流式特征必须至少支持 `COUNT`、`SUM`、`MAX`、`LATEST`；`DISTINCT_COUNT` 仅作为受控的小规模精确能力。 | P0 聚合能力 |
| REQ-FEATURE-003 | `stream feature` 必须由 Flink State 计算和维护，不得退化为规则表达式、任意脚本或后台临时 SQL。 | 状态特征边界 |
| REQ-FEATURE-004 | `lookup feature` 必须通过 Redis Set / Hash 等在线存储获取，并支持本地缓存与失败兜底。 | 在线查询特征 |
| REQ-FEATURE-005 | `derived feature` 必须基于事件字段、stream feature 和 lookup feature 计算，并在发布时记录依赖关系。 | 派生特征 |
| REQ-FEATURE-006 | 发布编译时必须对 `derived feature` 做依赖分析与循环依赖检测；存在循环依赖时禁止发布。 | 必须拦截环依赖 |
| REQ-FEATURE-007 | 流式状态必须按 `feature + entity key` 组织，并采用 bucket 化窗口求值与 `timer + TTL` 清理；TTL 只能做兜底回收，不能替代窗口语义。 | 状态模型 |

### 3.4 规则策略类需求

| ID | Requirement | Note |
| --- | --- | --- |
| REQ-RULE-001 | 规则中心必须支持条件表达式、优先级、动作类型、命中原因模板和启停状态。 | 规则最小模型 |
| REQ-RULE-002 | 一期规则表达式必须至少支持比较、逻辑、集合和区间运算。 | `> >= < <= == != && || ! in contains between` |
| REQ-RULE-003 | 策略层必须独立于规则层；规则只负责单条判断，策略负责多条规则的组织与收敛。 | Rule / Policy 分层 |
| REQ-RULE-004 | 一期策略模式必须至少支持 `FIRST_HIT` 与 `SCORE_CARD`。 | 首批 2 种模式 |
| REQ-RULE-005 | 发布前必须对规则和派生特征完成语法校验、变量存在性校验、类型兼容校验和安全边界校验。 | 编译期校验 |
| REQ-RULE-006 | 发布前必须生成规则与派生特征的直接依赖、传递依赖和拓扑顺序，用于快照编译与运行时准备。 | 依赖图 |
| REQ-RULE-007 | 规则层、策略层和共享执行内核在求值过程中不得发起外部 IO；Groovy 全面开放不纳入一期默认路径。 | P0 以表达式为主 |

### 3.5 发布与快照类需求

| ID | Requirement | Note |
| --- | --- | --- |
| REQ-SNAPSHOT-001 | 发布必须将设计态多表配置编译为运行时快照，不得将“发布”等同于保存配置或改状态位。 | 发布是编译 |
| REQ-SNAPSHOT-002 | 运行时快照必须满足自包含、去关系化、可恢复、面向执行、边界清晰和稳定可追溯。 | 快照原则 |
| REQ-SNAPSHOT-003 | 快照元信息至少包含 `snapshotId`、`sceneCode`、`version`、`checksum`、`publishedAt`、`effectiveFrom`。 | 最小元信息 |
| REQ-SNAPSHOT-004 | 快照结构必须显式区分 `scene`、`streamFeatures`、`lookupFeatures`、`derivedFeatures`、`rules`、`policy` 和 `runtimeHints`。 | 显式分层 |
| REQ-SNAPSHOT-005 | 发布编译时必须展开设计态关系为运行态执行顺序，例如将策略规则关联展开为明确的 `ruleOrder`。 | 去关系化展开 |
| REQ-SNAPSHOT-006 | 快照必须保留 `dependsOn` 或等价的依赖图信息，以支撑派生特征顺序、规则依赖和运行时上下文准备。 | 依赖可执行化 |
| REQ-SNAPSHOT-007 | 发布中心必须管理草稿、已发布、当前生效、历史版本和回滚来源关系；一期默认“发布即生效”，灰度与延迟生效为后置能力。 | 版本化发布 |

### 3.6 Flink 运行时类需求

| ID | Requirement | Note |
| --- | --- | --- |
| REQ-ENGINE-001 | Flink 必须定位为状态型实时决策执行引擎，负责事件消费、状态计算、lookup、规则 / 策略执行和结果输出。 | 计算面定位 |
| REQ-ENGINE-002 | Flink 不得承担设计态多表拼装、配置后台管理、拖拽编排或分析报表职责。 | 不做控制面 / 分析面 |
| REQ-ENGINE-003 | 运行态配置进入 Flink 的主路径必须为发布记录 / 配置流进入 Broadcast Stream，再写入 Broadcast State。 | 配置广播路径 |
| REQ-ENGINE-004 | Broadcast State 只存可恢复的快照包络对象；表达式 / Groovy 编译结果必须存放在本地 transient cache。 | 配置与缓存分层 |
| REQ-ENGINE-005 | 配置广播包至少包含 `sceneCode`、`versionNo`、`opType`、`checksum`、`publishedAt`、`effectiveFrom` 和 `snapshot`。 | 配置包络字段 |
| REQ-ENGINE-006 | `processBroadcastElement` 必须执行版本比较、checksum 比较、幂等处理、乱序判断和“先编译成功再切换”；编译失败时必须保留旧版本。 | 切换安全性 |
| REQ-ENGINE-007 | Broadcast State 默认只保留每个 `scene` 当前稳定版本；回滚通过重新下发历史快照实现，不在广播状态中长期堆积历史版本。 | 当前稳定版本 |
| REQ-ENGINE-008 | 单条事件在进入处理链路时必须绑定一个明确快照版本，并在整条处理过程中保持一致；版本切换按处理时间生效，只影响后续事件。 | 单事件单版本 |
| REQ-ENGINE-009 | 规则执行前必须先完成基础上下文、流式特征、lookup 特征和派生特征准备；规则引擎只消费 `EvalContext`，不得自行回查外部系统。 | `EvalContext` |
| REQ-ENGINE-010 | `lookup` 失败必须有兜底策略并记录错误日志；决策主链路与日志 / 分析输出必须解耦，日志失败不得阻塞主决策。 | 热路径解耦 |
| REQ-ENGINE-011 | Checkpoint 恢复后必须恢复 keyed state、broadcast state 和 timer，并惰性重建本地运行时缓存。 | 恢复一致性 |

### 3.7 仿真与日志类需求

| ID | Requirement | Note |
| --- | --- | --- |
| REQ-SIM-001 | 控制面必须提供仿真能力，输入至少包括事件、场景和版本，输出至少包括特征值、命中规则、命中原因、最终动作和耗时。 | 仿真最小能力 |
| REQ-SIM-002 | 仿真与线上必须尽量共用同一套 `pulsix-kernel`、执行器适配层、命中原因渲染和快照结构。 | 线上线下一致 |
| REQ-SIM-003 | 决策日志至少支持按 `traceId` / `eventId` 查询最终动作、命中规则、特征快照、快照版本和耗时。 | 追溯能力 |
| REQ-SIM-004 | 决策结果与命中日志必须记录 `eventId`、`traceId`、`sceneCode`、`snapshotId / version`、最终动作、命中规则和关键特征快照。 | 最小日志字段 |
| REQ-SIM-005 | 测试体系必须覆盖单元、组件、仿真、回放、联调和非功能六层；一期至少落实单元、组件、仿真、关键联调，以及版本切换和有状态逻辑验证。 | 分层测试 |

### 3.8 部署与工程化类需求

| ID | Requirement | Note |
| --- | --- | --- |
| REQ-OPS-001 | 依赖版本、运行时版本和工具链版本必须以 `docs/adr/0001-version-matrix.md` 为唯一事实源，未经 ADR 更新不得越线升级。 | 版本矩阵唯一事实源 |
| REQ-OPS-002 | 一期默认技术基线必须锁定 `JDK 17`、`Maven 3.9.x`、`Node 20`、`pnpm 9.x`、`Spring Boot 3.3.x`、`Flink 1.20.x`、`MySQL 8`、`Redis 7`、`Kafka 3`。 | 与 ADR 0001 对齐 |
| REQ-OPS-003 | Kafka 一期至少承载原始事件流、决策结果流和日志流；配置流默认优先走 MySQL CDC，Kafka 配置 Topic 为可选增强路径。 | Kafka / CDC 边界 |
| REQ-OPS-004 | 决策日志一期可先落 MySQL；Doris / ClickHouse 等分析型存储属于后置增强，不得阻塞主链路交付。 | MySQL 先行 |
| REQ-OPS-005 | 项目必须提供 Docker Compose 一键拉起的最小演示环境，并在 README 中明确启动顺序、快速启动步骤和 Demo 链路。 | 一键运行 |
| REQ-OPS-006 | 项目必须提供最小初始化内容与基础监控，至少覆盖默认场景 / 特征 / 规则 / 策略 / 名单 / 仿真样例，以及系统层、中间件层、业务层指标。 | Demo + 监控 |

## 4. 最关键的 10 条 Requirement

- `REQ-ARCH-002`：系统必须显式分离控制面、计算面、分析面。
- `REQ-ARCH-004`：Flink 只能消费运行态快照，不能运行时拼装设计态多表。
- `REQ-ARCH-005`：共享执行内核必须纯净、无 IO，并同时服务仿真与线上。
- `REQ-FEATURE-001`：特征必须分成 `stream`、`lookup`、`derived` 三类执行模型。
- `REQ-RULE-005`：规则与派生特征必须在发布前完成编译期校验。
- `REQ-SNAPSHOT-001`：发布必须生成运行时快照，发布不是简单改表生效。
- `REQ-SNAPSHOT-002`：快照必须自包含、去关系化、可恢复、可追溯。
- `REQ-ENGINE-006`：新版本必须先编译成功再切换，失败时保留旧版本。
- `REQ-ENGINE-008`：单条事件必须绑定单一快照版本执行到底。
- `REQ-SIM-002`：仿真与线上必须尽量共用同一套执行内核与快照结构。
