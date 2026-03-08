# Deploy

这是一套面向本地开发与单机联调的最小基础设施编排，提供 `MySQL`、`Redis`、`Kafka(KRaft)` 三个项目必需依赖。

## 文件说明

- `docker-compose.yml`：单机版基础设施编排
- `.env.template`：常用配置模板，可复制为 `.env` 后按需修改
- `mysql/init/01-import-pulsix-system-infra.sh`：MySQL 首次建库时导入 `docs/sql/pulsix-system-infra.sql`
- `kafka/init/01-create-topics.sh`：Kafka 首次初始化时创建默认 Topic

## 快速启动

在 `deploy` 目录执行：

```bash
cp .env.template .env
docker compose up -d
```

如果你暂时不想生成 `.env`，也可以直接启动，`docker-compose.yml` 已内置默认值：

```bash
docker compose up -d
```

## 首次初始化行为

- `MySQL` 使用官方镜像的 `/docker-entrypoint-initdb.d` 机制
- 只有在 `mysql-data` 数据卷为空、容器首次初始化时，才会自动导入 `docs/sql/pulsix-system-infra.sql`
- `Kafka` 通过 `kafka-init` 初始化服务在首次建卷后创建默认 Topic，并写入持久化标记文件
- 后续仅执行 `docker compose restart`、`docker compose stop/start`、`docker compose up -d` 不会重复导入 MySQL 或重复创建 Kafka Topic
- 如果你已经初始化过一次，又想重新执行初始化，需要显式删除数据卷后重建：

```bash
docker compose down -v
docker compose up -d
```

## 默认 Kafka Topics

- `pulsix.event.raw`
- `pulsix.decision.result`
- `pulsix.decision.log`
- `pulsix.event.dlq`
- `pulsix.config.snapshot`

如需调整，可修改 `.env` 中的 `KAFKA_INIT_TOPICS`、`KAFKA_DEFAULT_PARTITIONS`、`KAFKA_DEFAULT_REPLICATION_FACTOR`。

## 连接信息

- MySQL
  - 地址：`127.0.0.1:3306`
  - 数据库：`pulsix`
  - 用户：`pulsix`
  - 密码：`pulsix_123`
  - Root 密码：`pulsix_root_123`
- Redis
  - 地址：`127.0.0.1:6379`
  - 密码：`pulsix_redis_123`
- Kafka
  - 宿主机访问：`127.0.0.1:29092`
  - Docker 网络内访问：`kafka:9092`
  - 模式：`KRaft`，无需额外部署 ZooKeeper

## 常用命令

```bash
docker compose ps
docker compose logs -f
docker compose logs -f kafka-init
docker compose down
docker compose down -v
```

## 说明

- 这套编排面向单机开发、联调和本地演示，不包含高可用能力。
- 数据通过 Docker Volume 持久化，执行 `docker compose down` 不会删除数据。
- 如果端口冲突，修改 `.env` 中的 `MYSQL_PORT`、`REDIS_PORT`、`KAFKA_EXTERNAL_PORT` 后重新执行 `docker compose up -d` 即可。
