# ADR 0005：特征边界

## Title

Feature Boundaries for Base, Stream, Lookup, and Derived Features

## Status

Accepted

## Context

- 风控平台的特征不是单一计算模型；事件原始字段、流式状态、在线事实、派生计算的生命周期和执行位置完全不同。
- 如果不把特征边界写死，系统很容易退化为“所有东西都放脚本里算”，最终丢失状态语义、发布可控性和可解释性。
- 一期 Requirement Matrix 已明确区分 `stream feature`、`lookup feature`、`derived feature`，并要求 `stream feature` 由 Flink State 维护、`derived feature` 以表达式为主。

## Decision

- `Base Feature` 指标准化事件中的基础字段与实体字段，本质上来自输入事件，不由平台二次聚合，不写状态，不走外部查询。
- `Stream Feature` 指依赖事件流和状态窗口计算出的特征，只能由 Flink 基于模板化定义动态生成。
- `Stream Feature` 可动态化的范围仅限：聚合算子、窗口、entity key、过滤条件、TTL / 清理提示等受控参数；不能开放为任意脚本、任意 SQL、任意 Groovy、任意状态机代码。
- `Lookup Feature` 指通过受控在线存储读取的事实特征，例如 Redis Set / Hash；其职责是点查外部事实，不承担窗口聚合，不拼接设计态多表。
- `Derived Feature` 指基于 `base + stream + lookup + 已完成 derived` 计算出的派生值，默认必须表达式优先，Groovy 仅作补位。
- Groovy 补位仅适用于表达式难以覆盖但仍然纯函数的派生计算；Groovy 不能访问 Flink State、Timer、Keyed Context，不能管理窗口，不能发起外部 IO，不能接管实时状态语义。
- 规则与策略只能消费 `EvalContext` 中已准备好的 `base`、`stream`、`lookup`、`derived` 值，不能在求值时临时定义新的状态特征或外部查询特征。
- 明确禁止：把流式特征写成后台 SQL 或脚本黑盒；把 lookup 当作设计态表 join；把 derived 做成另一个状态引擎；让 Groovy 成为通用执行入口并绕过表达式、快照、发布校验。

## Consequences

- 特征研发路径被明确分流：事件字段归 `base`，状态聚合归 `stream`，外部事实归 `lookup`，轻量组合归 `derived`。
- 发布编译阶段可以分别对模板参数、lookup 依赖、表达式依赖和 Groovy 安全边界做校验。
- Flink 状态语义保持在引擎内部，Groovy 只处理纯计算补位，避免脚本侵入实时状态生命周期。
- 后续若要扩展 CEP、复杂序列状态或更开放脚本能力，必须新增 ADR，而不是直接放宽当前边界。
