# ADR 0003：运行态快照契约

## Title

SceneSnapshot as the Only Runtime Contract for Published Risk Logic

## Status

Accepted

## Context

- 设计态配置分散在场景、事件模型、特征、规则、策略、发布单等多张表中，天然面向编辑和管理，不面向实时执行。
- 实时引擎要求输入稳定、结构自包含、可版本切换、可回滚、可审计；设计态多表无法直接满足这些要求。
- Requirement Matrix 已明确：发布不是保存配置，而是生成运行态快照；Flink 只能消费运行态快照。

## Decision

- `SceneSnapshot` 是场景发布后的唯一运行态契约；Flink、仿真、回放都只能消费快照，不能直接消费设计态多表。
- 每次发布必须产出新的 `SceneSnapshot` 版本；未生成快照，不得视为已发布。
- `SceneSnapshot` 至少包含以下稳定字段：
  - 元信息：`snapshotId`、`sceneCode`、`version`、`checksum`、`publishedAt`、`effectiveFrom`
  - 场景信息：`scene`（运行态所需的场景标识、事件类型绑定、默认执行参数）
  - 特征定义：`streamFeatures`、`lookupFeatures`、`derivedFeatures`
  - 规则与策略：`rules`、`policy`
  - 运行提示：`runtimeHints`
- 快照必须是面向执行的去关系化结构；运行时读取快照后，不再依赖设计态多表补全执行信息。
- `streamFeatures` 必须包含已编译好的聚合模板、窗口、实体 key、状态清理提示；不能把运行时还需要拼装的 SQL、Groovy 或后台表 join 留给 Flink。
- `derivedFeatures`、`rules`、`policy` 必须写入已通过校验的表达式 / 脚本描述和依赖顺序；不能在引擎侧二次猜测依赖关系。
- 明确禁止：Flink 直接查控制面库表拼装规则；按请求实时 join 设计态多表；用“当前数据库最新配置”替代快照版本；发布时只改状态位不生成快照。

## Consequences

- 运行时版本切换、灰度扩展、回滚、回放、审计都围绕 `SceneSnapshot` 版本展开，而不是围绕后台表当前状态展开。
- 快照编译成为发布链路中的强制步骤；表达式校验、依赖分析、冲突检查必须在发布前完成。
- 引擎可以对快照做缓存、校验和校验、原子切换，但不能修改快照语义。
- 后续若新增运行态字段，应优先扩展 `SceneSnapshot` 契约，而不是给引擎增加设计态表查询捷径。
