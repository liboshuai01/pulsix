# Deploy

这是一套面向本地开发与单机联调的最小基础设施编排，默认提供 `MySQL`、`Redis`、`Kafka(KRaft)`、`Doris` 四个依赖，可直接通过一次 `docker compose up -d` 全部拉起。

## 文件说明

- `docker-compose.yml`：单机版基础设施编排
- `.env.template`：常用配置模板，可复制为 `.env` 后按需修改
- `mysql/init/01-import-pulsix-system-infra.sh`：MySQL 首次建库时导入 `docs/sql/pulsix-system-infra.sql`
- `redis/init/01-init-redis.sh`：Redis 启动后幂等装载开发联调所需的名单、画像、特征副本、缓存和字典数据
- `kafka/init/01-create-topics.sh`：Kafka 启动后幂等创建默认 Topic
- `doris/init/01-init-doris.sh`：Doris 启动后幂等执行建库建表 SQL
- `doris/sql/01-pulsix-olap.sql`：按落地清单初始化 Doris 查询表

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
docker compose up -d doris-fe doris-be doris-init
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
- `Redis` 通过 `redis-init` 初始化服务在 `docker compose up -d` 后检查并装载默认开发数据；它会写入 marker key，后续重复执行不会覆盖已经初始化过的数据
- `Kafka` 通过 `kafka-init` 初始化服务在 `docker compose up -d` 后检查并创建默认 Topic
- `Doris` 通过 `doris-init` 初始化服务在 `docker compose up -d` 后检查并创建默认库表
- 后续仅执行 `docker compose restart`、`docker compose stop/start`、`docker compose up -d` 不会重复导入 MySQL；Redis、Kafka、Doris 的初始化服务都会做幂等检查，不会覆盖已存在对象
- 由于 Doris 使用固定容器 IP，若你修改了 `PULSIX_SUBNET` 或 Doris IP 配置，需要先执行一次 `docker compose down` 以重建网络
- `Doris` 使用独立 Docker Volume 持久化 FE 元数据和 BE 存储，重复执行 `docker compose up -d` 不会丢数据
- 如需强制重装 Redis 种子数据，可在 `.env` 里临时把 `REDIS_INIT_FORCE=true`，然后执行 `docker compose up -d redis-init`；完成后建议改回 `false`
- 如果你已经初始化过一次，又想重新执行初始化，需要显式删除数据卷后重建：

```bash
docker compose down -v
docker compose up -d
```

## 默认 Kafka Topics

- `pulsix.event.standard`
- `pulsix.decision.result`
- `pulsix.decision.log`
- `pulsix.engine.error`
- `pulsix.event.dlq`
- `pulsix.ingest.error`

当前系统不创建配置类 Kafka Topic；MySQL 配置快照通过 `scene_release -> MySQL CDC -> Flink` 直接同步。

当前 `deploy` 默认按 `docs/wiki/kafka-redis-doris-落地清单.md` 中的表格创建 Topic：

- `pulsix.event.standard`：`6` 分区，`1` 副本
- `pulsix.decision.result`：`3` 分区，`1` 副本
- `pulsix.decision.log`：`3` 分区，`1` 副本
- `pulsix.engine.error`：`1` 分区，`1` 副本
- `pulsix.event.dlq`：`1` 分区，`1` 副本
- `pulsix.ingest.error`：`1` 分区，`1` 副本

如需调整，可修改 `.env` 中的 `KAFKA_INIT_TOPIC_SPECS`；格式为 `topic:partitions:replicas`，多个 Topic 以英文逗号分隔。

## 默认 Redis 种子数据

- 名单：`LOGIN_DEVICE_BLACKLIST`、`LOGIN_USER_WHITE_LIST`，以及 `pulsix:list:black:*` / `pulsix:list:white:*` 单值 key
- 画像：`USER_RISK_PROFILE`、`IP_RISK_PROFILE`、`pulsix:profile:user:risk:*`、`pulsix:profile:ip:risk:*`、`pulsix:profile:user:*`、`pulsix:profile:device:*`
- 特征副本：登录链路 `user_login_fail_cnt_10m` / `ip_login_fail_cnt_10m` / `device_login_user_cnt_1h`，交易链路 `user_trade_cnt_5m` / `user_trade_amt_sum_30m` / `device_bind_user_cnt_1h`
- 缓存：`pulsix:cache:scene:active_version:*`、`pulsix:cache:simulation:*`、`pulsix:cache:warmup:*`
- 字典：`pulsix:dict:geo:ip:*`、`pulsix:dict:merchant:risk:*`
- 数据口径主要对齐 `docs/wiki/pulsix-engine-kernel-一期开发指南.md`、`docs/wiki/kafka-redis-doris-落地清单.md`、`docs/sql/pulsix-risk.sql` 与 `pulsix-engine` 的 demo fixture / lookup 约定

## 默认 Doris 库表

- 数据库：`pulsix_olap`
- 默认初始化表：
  - `dwd_decision_result`
  - `dwd_decision_log`
  - `dwd_rule_hit_log`
  - `dwd_ingest_error_log`
  - `dwd_engine_error_log`

如需调整 Doris 初始化重试参数，可修改 `.env` 中的 `DORIS_INIT_MAX_ATTEMPTS`、`DORIS_INIT_RETRY_INTERVAL_SECONDS`；如需调整库名，可修改 `DORIS_DATABASE`。

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
docker compose logs -f redis-init
docker compose logs -f kafka-init
docker compose logs -f doris-init
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
