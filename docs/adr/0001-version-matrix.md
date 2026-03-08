# ADR 0001: 版本矩阵

- Status: Accepted
- Date: 2026-03-08

## Context

- 仓库同时包含 `Spring Boot` 控制平台、`Flink` 引擎和 `Vue 3` 前端，版本漂移会直接增加联调、发布和回滚成本。
- 当前版本信息分散在 `pom.xml`、`pulsix-dependencies/pom.xml`、`pulsix-ui/package.json` 和 `README.md` 中，需要一个统一口径。
- 本 ADR 只定义当前基线版本与升级约束，不展开实现细节。

## Decision

- 以仓库中已声明的版本作为唯一基线，形成统一版本矩阵。
- 未在本 ADR 单独列出的后端依赖，统一跟随 `pulsix-dependencies/pom.xml`；前端依赖，统一跟随 `pulsix-ui/package.json`。
- 涉及矩阵项变更的 PR，必须同步更新本 ADR；跨大版本或跨关键小版本升级，先更新 ADR，再推进代码与环境切换。

## Version Matrix

| 类别 | 基线版本 | 来源 | 执行要求 |
| --- | --- | --- | --- |
| 项目发布列车 | `2026.01-SNAPSHOT` | `pom.xml`、`pulsix-dependencies/pom.xml`、`pulsix-ui/package.json` | 服务端模块与前端版本号保持同一发布列车 |
| Java | `17` | `pom.xml` | 后端与引擎统一使用 `JDK 17` |
| Maven | `3.9+` | `README.md` | 本地与 CI 使用同一主版本基线 |
| Spring Boot | `3.5.9` | `pom.xml`、`pulsix-dependencies/pom.xml` | Spring 生态依赖统一跟随 BOM |
| Flink | `1.20.3` | `pulsix-dependencies/pom.xml` | 引擎模块与 Flink 生态保持同一 minor |
| Node.js | `20 LTS`（最低 `16.0.0`） | `README.md`、`pulsix-ui/package.json` | CI 默认使用 `Node 20`，本地环境不得低于 `16` |
| pnpm | `>= 8.6.0` | `README.md`、`pulsix-ui/package.json` | 前端统一使用 `pnpm` |
| Vue | `3.5.12` | `pulsix-ui/package.json` | 前端插件与页面代码保持 `Vue 3` 兼容 |
| Vite | `5.1.4` | `pulsix-ui/package.json` | 构建链路保持 `Vite 5` 兼容 |
| TypeScript | `5.3.3` | `pulsix-ui/package.json` | 类型工具链统一 |
| Element Plus | `2.11.1` | `pulsix-ui/package.json` | 控制台组件库统一 |
| MySQL | `8.x` | `README.md` | 开发、测试、生产保持同一大版本 |
| Redis | `7.x` | `README.md` | 开发、测试、生产保持同一大版本 |

## Rules

- 新模块不得引入与矩阵冲突的运行时基线，例如新的 `JDK`、`Vue` 主版本或不同 `Flink` minor。
- 需要新增平台级版本项时，先补充本 ADR，再合入对应模块改动。
- 业务实现、部署参数和具体兼容性排障不属于本 ADR 范围。

## Consequences

- 版本基线从“分散声明”变为“单点约束”，便于评审、联调和回滚。
- 关键升级会更早暴露影响面，但需要在变更时同步维护本 ADR。
