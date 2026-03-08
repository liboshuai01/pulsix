# ADR 0001：版本矩阵基线

- 状态：Accepted
- 日期：2026-03-08

## 背景

本仓库是实时风控平台，涉及 Spring Boot 控制面、Flink 实时引擎、MySQL / Redis / Kafka 基础设施，以及 Node + pnpm 前端工具链。为避免 AI 助手和后续开发随意升级依赖，必须先锁定统一版本矩阵，再允许受控升级。

## 决策

| 范围 | 推荐版本 | 锁定规则 | 允许升级范围 | 简短理由 |
| --- | --- | --- | --- | --- |
| OS | `WSL2 Ubuntu 24.04 LTS` | 必须固定到 `24.04` 线 | 仅允许 `24.04.x` 安全更新 | 与当前开发环境一致，LTS 稳定，包生态成熟 |
| Java | `JDK 17` | 必须固定到 `17` 大版本 | 允许 `17.0.x` | Spring Boot 3 与 Flink 1.20 的共同稳定基线 |
| Maven | `3.9.x` | 必须固定到 `3.9` 线 | 允许 `3.9.x` | 对 JDK 17 和现代插件兼容稳定 |
| Node.js | `20 LTS` | 必须固定到 `20` 大版本 | 允许 `20.x` | 前端工具链成熟，兼顾稳定与可维护性 |
| pnpm | `9.x` | 必须固定到 `9` 大版本 | 允许 `9.x` | 与 Node 20 搭配稳定，锁文件行为可控 |
| Spring Boot | `3.3.x` | 必须固定到 `3.3` 线 | 允许 `3.3.x` | Boot 3 系列中足够成熟，避免追新导致生态漂移 |
| Flink | `1.20.x` | 必须固定到 `1.20` 线 | 允许 `1.20.x` | 引擎与连接器强依赖同 minor，必须严控 |
| MySQL | `8.x` | 必须固定到 `8` 大版本 | 允许 `8.x` | 生态成熟，驱动与运维经验充足 |
| Redis | `7.x` | 必须固定到 `7` 大版本 | 允许 `7.x` | 客户端支持广，适合缓存与状态协同场景 |
| Kafka | `3.x` | 必须固定到 `3` 大版本 | 允许 `3.x` | 与实时流式生态兼容成熟，避免跨代协议差异 |

## 必须固定

- `WSL2 + Ubuntu 24.04 LTS` 是唯一默认本地开发基线。
- `Java 17`、`Node 20 LTS`、`pnpm 9`、`MySQL 8`、`Redis 7`、`Kafka 3` 不得跨大版本。
- `Maven 3.9.x`、`Spring Boot 3.3.x`、`Flink 1.20.x` 不得跨 minor 版本线。
- 所有 `pom.xml`、`package.json`、`Dockerfile`、`docker-compose.yml`、脚本、文档均以本 ADR 为准；现有不一致内容不构成升级依据。

## 允许的小版本升级

- 仅允许安全补丁和兼容性补丁升级，范围限定如下：
- `Ubuntu 24.04.x`
- `JDK 17.0.x`
- `Maven 3.9.x`
- `Node 20.x`
- `pnpm 9.x`
- `Spring Boot 3.3.x`
- `Flink 1.20.x`
- `MySQL 8.x`
- `Redis 7.x`
- `Kafka 3.x`

## 明确禁止

- 禁止在未更新本 ADR 前引入或切换到：`JDK 21+`、`Maven 4`、`Node 22+`、`pnpm 10+`、`Spring Boot 3.4+`、`Flink 2.x`、`MySQL 9`、`Redis 8`、`Kafka 4`。
- 禁止 AI 助手以“样例项目默认值”“最新版本”“上游模板升级”为理由擅自改动版本矩阵。
- 如需升级版本，流程必须是：先修改 ADR，再修改代码与部署清单。

## 本地开发环境预装工具

- `git`
- `OpenJDK 17`
- `Maven 3.9.x`
- `Node.js 20 LTS`
- `pnpm 9.x`
- `Docker`
- `Docker Compose v2`
- `mysql-client`
- `redis-tools`
- `kcat`（推荐，用于 Kafka 自检；未安装时可用容器内 Kafka CLI 替代）

## 环境自检命令清单

```bash
grep -i microsoft /proc/version
lsb_release -ds
git --version
java -version
javac -version
mvn -v
node -v
pnpm -v
docker --version
docker compose version
mysql --version
redis-cli --version
kcat -V
```

## 后果

- 本 ADR 是后续 AI 编码、脚手架生成、依赖调整、镜像选择的基础约束。
- 任何偏离本矩阵的改动，都应视为架构变更，不是普通开发细节。
- 后续 ADR、README、部署脚本、CI 配置出现冲突时，以本 ADR 为准，直到新的版本 ADR 生效。
