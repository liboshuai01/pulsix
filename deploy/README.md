# Deploy

这是一套面向本地开发与单机联调的最小基础设施编排，默认提供 `MySQL`、`Redis`、`Kafka(KRaft)`、`Doris` 四个依赖，可直接通过一次 `docker compose up -d` 全部拉起。

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

默认会使用 `pulsix` 作为 Compose 项目前缀和 Docker 网络名，因此生成的容器、数据卷、网络资源会以 `pulsix` 开头。

## Doris 单机分析层

`Doris` 属于分析层，但为了方便本地单机联调，这里直接纳入默认编排；执行 `docker compose up -d` 时会和 `MySQL`、`Redis`、`Kafka` 一起启动。由于官方镜像要求 `FE_SERVERS` 使用 IP 而不是服务名，这里为 Doris 预留了固定容器 IP。同时，Doris 的部分内部端口绑定在容器 IP 而不是 `127.0.0.1`，所以健康检查也按固定 IP 进行探测。

### 启动前准备

- 最低建议宿主机预留 `2C / 4G`，更稳定的本地联调建议预留 `4C / 8G`
- `amd64` 环境建议确认 CPU 支持 `AVX2`，避免 Doris 容器启动后异常退出
- 首次从旧版 `deploy` 切换到当前 Doris 配置时，需要重建一次 Compose 网络；普通重启后无需重复处理
- Linux 宿主机首次启动前建议执行：

```bash
sudo sysctl -w vm.max_map_count=2000000
```

### 启动 Doris

在 `deploy` 目录执行：

```bash
docker compose down
docker compose up -d
```

如果这是你第一次使用当前这版 Doris 配置，也可以直接 `docker compose up -d`；`down` 主要是为了让旧网络配置切换到固定 IP 方案。

如只想单独拉起 Doris 相关服务，也可以执行：

```bash
docker compose up -d doris-fe doris-be
```

如果你的 Docker 网段和 `172.28.0.0/24` 冲突，可修改 `.env` 中的 `PULSIX_SUBNET`、`DORIS_FE_IP`、`DORIS_BE_IP`，三者需保持同一子网。

### 检查状态

下面命令默认复用同一套 `compose` 里已经启动的 `mysql` 容器作为 MySQL 协议客户端；如果你只启动了 `doris-fe`、`doris-be`，可改为使用宿主机本地 `mysql` 客户端连接 `127.0.0.1:9030`。

```bash
docker compose ps
docker compose logs -f doris-fe
docker compose logs -f doris-be
docker compose exec mysql mysql -h doris-fe -P 9030 -uroot -e "SHOW FRONTENDS;"
docker compose exec mysql mysql -h doris-fe -P 9030 -uroot -e "SHOW BACKENDS;"
```

## 首次初始化行为

- `MySQL` 使用官方镜像的 `/docker-entrypoint-initdb.d` 机制
- 只有在 `mysql-data` 数据卷为空、容器首次初始化时，才会自动导入 `docs/sql/pulsix-system-infra.sql`
- `Kafka` 通过 `kafka-init` 初始化服务在首次建卷后创建默认 Topic，并写入持久化标记文件
- 后续仅执行 `docker compose restart`、`docker compose stop/start`、`docker compose up -d` 不会重复导入 MySQL 或重复创建 Kafka Topic
- 由于 Doris 使用固定容器 IP，若你修改了 `PULSIX_SUBNET` 或 Doris IP 配置，需要先执行一次 `docker compose down` 以重建网络
- `Doris` 使用独立 Docker Volume 持久化 FE 元数据和 BE 存储，重复执行 `docker compose up -d` 不会丢数据
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
- Doris
  - FE HTTP：`127.0.0.1:8030`
  - FE MySQL 协议端口：`127.0.0.1:9030`
  - BE HTTP：`127.0.0.1:8040`
  - 默认用户：`root`
  - 默认密码：空
  - Docker 网络内 FE：`doris-fe:9030`

## 常用命令

```bash
docker compose ps
docker compose logs -f
docker compose logs -f kafka-init
docker compose logs -f doris-fe
docker compose logs -f doris-be
docker compose down
docker compose down -v
```

## 说明

- 这套编排面向单机开发、联调和本地演示，不包含高可用能力。
- 数据通过 Docker Volume 持久化，执行 `docker compose down` 不会删除数据。
- 如果端口冲突，修改 `.env` 中的 `MYSQL_PORT`、`REDIS_PORT`、`KAFKA_EXTERNAL_PORT` 后重新执行 `docker compose up -d` 即可。
- 如需调整 Doris 对外端口，修改 `.env` 中的 `DORIS_FE_HTTP_PORT`、`DORIS_FE_QUERY_PORT`、`DORIS_BE_HTTP_PORT` 后重新执行 `docker compose up -d`。
- 如需调整 Doris 固定 IP，修改 `.env` 中的 `PULSIX_SUBNET`、`DORIS_FE_IP`、`DORIS_BE_IP`，并先执行一次 `docker compose down` 以重建网络。
- 如果你之前已经复制过旧版 `.env`，请同步将 `COMPOSE_PROJECT_NAME` 和 `PULSIX_NETWORK_NAME` 改为 `pulsix`，否则仍会继续生成 `pulsix-infra` 前缀资源。
