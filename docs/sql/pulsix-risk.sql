/*
 Navicat Premium Dump SQL

 Source Server         : logical-design
 Source Server Type    : MySQL
 Source Server Version : 80045 (8.0.45)
 Source Schema         : pulsix

 Target Server Type    : MySQL
 Target Server Version : 80045 (8.0.45)
 File Encoding         : 65001

 说明：
 1. 本文件基于 docs/wiki/风控功能清单.md、docs/sql/pulsix-system-infra.sql、docs/参考资料 下相关章节整理。
 2. 设计目标覆盖【必须要做的功能】+【推荐要做的功能】所涉及的 MySQL 核心表。
 3. 表结构尽量对齐 pulsix-system-infra.sql 的风格：bigint 主键、utf8mb4_unicode_ci、BaseDO 公共字段、deleted 逻辑删除。
 4. 设计态对象优先结构化；发布态通过 scene_release.snapshot_json 固化为运行时快照。
 5. 推荐能力中的审计日志可同时复用 system_operate_log；这里额外给出 risk_audit_log，用于记录风险对象的 before/after 细粒度差异。
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ============================================================
-- P0 必须：场景、事件、接入、特征、规则、策略、发布、仿真、日志
-- P1 推荐：名单、审计、回放、监控、SCORE_CARD 分段
-- ============================================================

-- ----------------------------
-- Table structure for risk_metric_snapshot
-- ----------------------------
DROP TABLE IF EXISTS `risk_metric_snapshot`;
CREATE TABLE `risk_metric_snapshot`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '指标快照主键',
  `stat_time` datetime NOT NULL COMMENT '统计时间点，通常取分钟或 5 分钟整点',
  `stat_granularity` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '1m' COMMENT '统计粒度，例如 1m、5m、1h',
  `scene_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '场景编码；全局指标可为空',
  `metric_domain` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'DECISION' COMMENT '指标域，例如 INGEST、DECISION、ENGINE、KAFKA',
  `metric_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '' COMMENT '指标编码，例如 EVENT_IN_TOTAL、DLQ_TOTAL、P95_LATENCY_MS',
  `metric_name` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '' COMMENT '指标中文名称',
  `metric_value` decimal(20, 4) NOT NULL DEFAULT 0.0000 COMMENT '指标数值',
  `metric_unit` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '指标单位，例如 count、ratio、ms、messages',
  `metric_tags_json` json NULL COMMENT '维度标签 JSON，例如 sourceCode、topic、action 等扩展维度',
  `creator` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_stat_time_domain`(`stat_time` ASC, `metric_domain` ASC) USING BTREE,
  INDEX `idx_scene_metric`(`scene_code` ASC, `metric_code` ASC, `stat_time` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '风控监控指标快照表（P1-推荐），支撑 Dashboard/基础监控页面的时序指标展示' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for replay_job
-- ----------------------------
DROP TABLE IF EXISTS `replay_job`;
CREATE TABLE `replay_job`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '回放任务主键',
  `job_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '' COMMENT '回放任务编码，便于页面和脚本追踪',
  `scene_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '' COMMENT '场景编码',
  `baseline_version_no` int NOT NULL COMMENT '基线版本号，一般是线上当前版本',
  `target_version_no` int NOT NULL COMMENT '目标版本号，一般是候选待发布版本',
  `input_source_type` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'FILE' COMMENT '回放输入来源类型，例如 FILE、KAFKA_EXPORT、DECISION_LOG_EXPORT',
  `input_ref` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '' COMMENT '输入来源引用，例如文件路径、对象存储 URL、导出批次号',
  `job_status` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'INIT' COMMENT '任务状态，例如 INIT、RUNNING、SUCCESS、FAILED',
  `event_total_count` int NOT NULL DEFAULT 0 COMMENT '参与回放的事件总数',
  `diff_event_count` int NOT NULL DEFAULT 0 COMMENT '产生决策差异的事件数',
  `summary_json` json NULL COMMENT '回放汇总结果 JSON，例如 PASS/REVIEW/REJECT 占比变化、Top 差异规则',
  `sample_diff_json` json NULL COMMENT '样例差异 JSON，建议只存少量样本，便于页面快速理解差异原因',
  `started_at` datetime NULL DEFAULT NULL COMMENT '回放开始时间',
  `finished_at` datetime NULL DEFAULT NULL COMMENT '回放结束时间',
  `remark` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '任务备注，例如回放目的或变更背景',
  `creator` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_job_code`(`job_code` ASC) USING BTREE,
  INDEX `idx_scene_version`(`scene_code` ASC, `baseline_version_no` ASC, `target_version_no` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '回放对比任务表（P1-推荐），用于文件级或导出级历史事件回放比对' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for risk_audit_log
-- ----------------------------
DROP TABLE IF EXISTS `risk_audit_log`;
CREATE TABLE `risk_audit_log`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '审计日志主键',
  `trace_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '' COMMENT '操作链路追踪号，方便串联接口日志、发布日志和审计日志',
  `scene_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '关联场景编码；若是全局对象可为空',
  `operator_id` bigint NOT NULL DEFAULT 0 COMMENT '操作人用户编号',
  `operator_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '' COMMENT '操作人名称，便于直接展示',
  `biz_type` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '' COMMENT '业务对象类型，例如 SCENE、FEATURE、RULE、POLICY、RELEASE、LIST',
  `biz_code` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '' COMMENT '业务对象编码，例如 LOGIN_R002、TRADE_RISK_SCORECARD',
  `action_type` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '' COMMENT '动作类型，例如 CREATE、UPDATE、DELETE、PUBLISH、ROLLBACK、IMPORT',
  `before_json` json NULL COMMENT '变更前快照 JSON',
  `after_json` json NULL COMMENT '变更后快照 JSON',
  `remark` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '变更说明，尽量写成可读中文',
  `operate_time` datetime NOT NULL COMMENT '实际操作时间',
  `creator` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_biz`(`biz_type` ASC, `biz_code` ASC) USING BTREE,
  INDEX `idx_operator_time`(`operator_id` ASC, `operate_time` ASC) USING BTREE,
  INDEX `idx_scene_time`(`scene_code` ASC, `operate_time` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '风险对象审计日志表（P1-推荐），用于记录规则、特征、发布、名单等对象的 before/after 差异' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for ingest_error_log
-- ----------------------------
DROP TABLE IF EXISTS `ingest_error_log`;
CREATE TABLE `ingest_error_log`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '接入异常主键',
  `trace_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '' COMMENT '链路追踪号，可与 decision_log.trace_id 对齐',
  `source_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '' COMMENT '接入源编码',
  `scene_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '识别出的场景编码；若还未识别成功可为空',
  `event_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '识别出的事件编码；若还未识别成功可为空',
  `raw_event_id` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '原始事件编号，便于按上游事件回查',
  `ingest_stage` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '' COMMENT '出错阶段，例如 AUTH、PARSE、NORMALIZE、VALIDATE、PRODUCE',
  `error_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '' COMMENT '错误编码，例如 REQUIRED_FIELD_MISSING、AUTH_SIGN_INVALID',
  `error_message` varchar(1024) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '' COMMENT '错误说明，尽量写成可读中文',
  `raw_payload_json` json NULL COMMENT '接入层收到的原始报文 JSON',
  `standard_payload_json` json NULL COMMENT '已完成部分标准化后的报文 JSON；完全失败时可为空',
  `error_topic_name` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '异常写入的 Kafka Topic 名称，例如 pulsix.event.dlq',
  `reprocess_status` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'PENDING' COMMENT '重处理状态，例如 PENDING、IGNORED、RETRY_SUCCESS、RETRY_FAILED',
  `occur_time` datetime NOT NULL COMMENT '异常发生时间',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '记录状态：1-有效，0-归档/忽略',
  `creator` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_source_time`(`source_code` ASC, `occur_time` ASC) USING BTREE,
  INDEX `idx_trace`(`trace_id` ASC) USING BTREE,
  INDEX `idx_reprocess`(`reprocess_status` ASC, `occur_time` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '接入异常日志表（P1-推荐），支撑 DLQ 查询、错误事件追查、重放与治理' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for rule_hit_log
-- ----------------------------
DROP TABLE IF EXISTS `rule_hit_log`;
CREATE TABLE `rule_hit_log`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '规则命中日志主键',
  `decision_id` bigint NOT NULL COMMENT '所属决策日志主键，对应 decision_log.id',
  `rule_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '' COMMENT '规则编码',
  `rule_name` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '' COMMENT '规则名称，冗余存储便于直接查询展示',
  `rule_order_no` int NOT NULL DEFAULT 0 COMMENT '规则执行顺序，便于还原 FIRST_HIT 或 SCORE_CARD 的执行链路',
  `hit_flag` tinyint NOT NULL DEFAULT 0 COMMENT '是否命中：1-命中，0-未命中',
  `hit_reason` varchar(1024) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '渲染后的命中原因文本',
  `score` int NULL DEFAULT NULL COMMENT '本条规则贡献的分值；FIRST_HIT 场景可为空或直接写 risk_score',
  `hit_value_json` json NULL COMMENT '命中时参与判断的关键字段/特征值快照 JSON',
  `creator` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_decision_rule`(`decision_id` ASC, `rule_code` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '规则命中明细表（P0-必须），用于查看一条决策里哪些规则命中、按什么值命中' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for decision_log
-- ----------------------------
DROP TABLE IF EXISTS `decision_log`;
CREATE TABLE `decision_log`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '决策日志主键',
  `trace_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '' COMMENT '链路追踪号，用于串起接入、引擎、查询三段链路',
  `event_id` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '' COMMENT '标准事件编号',
  `scene_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '' COMMENT '场景编码',
  `source_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '接入源编码，便于定位来源系统',
  `event_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '' COMMENT '事件编码，例如 login、trade',
  `entity_id` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '主实体编号，通常是 userId 或 deviceId，用于日志快速筛选',
  `policy_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '' COMMENT '执行时使用的策略编码',
  `final_action` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '' COMMENT '最终动作，例如 PASS、REVIEW、REJECT',
  `final_score` int NULL DEFAULT NULL COMMENT '最终得分；FIRST_HIT 场景可为空或写入命中规则分值',
  `version_no` int NOT NULL COMMENT '运行时快照版本号，用于追溯当时到底跑的是哪一版配置',
  `latency_ms` bigint NULL DEFAULT NULL COMMENT '本次决策耗时，单位毫秒',
  `event_time` datetime NOT NULL COMMENT '事件时间，即业务事件真实发生时间',
  `input_json` json NULL COMMENT '标准事件输入 JSON',
  `feature_snapshot_json` json NULL COMMENT '参与决策时的特征快照 JSON',
  `hit_rules_json` json NULL COMMENT '命中规则摘要 JSON，方便快速渲染详情页',
  `decision_detail_json` json NULL COMMENT '额外决策明细 JSON，例如策略模式、分段命中、回退动作等',
  `creator` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_trace_id`(`trace_id` ASC) USING BTREE,
  INDEX `idx_event_id`(`event_id` ASC) USING BTREE,
  INDEX `idx_scene_time`(`scene_code` ASC, `event_time` ASC) USING BTREE,
  INDEX `idx_action_time`(`scene_code` ASC, `final_action` ASC, `event_time` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '决策日志表（P0-必须），支撑 traceId/eventId 追查、命中链路还原、版本追溯' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for simulation_report
-- ----------------------------
DROP TABLE IF EXISTS `simulation_report`;
CREATE TABLE `simulation_report`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '仿真报告主键',
  `case_id` bigint NOT NULL COMMENT '仿真用例主键，对应 simulation_case.id',
  `scene_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '' COMMENT '场景编码',
  `version_no` int NOT NULL COMMENT '执行仿真时指定的快照版本号',
  `trace_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '' COMMENT '仿真执行链路号，便于和执行日志串联',
  `result_json` json NOT NULL COMMENT '仿真结果 JSON，建议包含 finalAction、hitRules、featureSnapshot、score 等关键内容',
  `pass_flag` tinyint NOT NULL DEFAULT 0 COMMENT '是否符合预期：1-符合，0-不符合',
  `duration_ms` bigint NULL DEFAULT NULL COMMENT '仿真执行耗时，单位毫秒',
  `creator` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_case_version`(`case_id` ASC, `version_no` ASC) USING BTREE,
  INDEX `idx_scene_time`(`scene_code` ASC, `create_time` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '仿真报告表（P0-必须），记录某次仿真执行结果，支撑回归和平台演示' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for simulation_case
-- ----------------------------
DROP TABLE IF EXISTS `simulation_case`;
CREATE TABLE `simulation_case`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '仿真用例主键',
  `scene_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '' COMMENT '场景编码',
  `case_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '' COMMENT '用例编码，便于脚本或接口直接指定',
  `case_name` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '' COMMENT '用例名称',
  `version_select_mode` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'LATEST' COMMENT '版本选择模式：LATEST-最新发布版，FIXED-固定版本号',
  `version_no` int NULL DEFAULT NULL COMMENT '固定版本号；当 version_select_mode=FIXED 时使用',
  `input_event_json` json NOT NULL COMMENT '标准事件输入 JSON',
  `mock_feature_json` json NULL COMMENT '仿真时强制注入的特征值 JSON，用于在不依赖 Flink State 时快速验证规则链路',
  `mock_lookup_json` json NULL COMMENT '仿真时模拟的 lookup 值 JSON，例如 user_risk_level、device_in_blacklist',
  `expected_action` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '' COMMENT '期望动作，例如 PASS、REVIEW、REJECT',
  `expected_hit_rules` json NULL COMMENT '期望命中的规则编码数组 JSON',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '状态：1-启用，0-停用',
  `description` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '用例说明，建议写清楚想验证的风控要点',
  `creator` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_scene_case`(`scene_code` ASC, `case_code` ASC) USING BTREE,
  INDEX `idx_scene_status`(`scene_code` ASC, `status` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '仿真用例表（P0-必须），用于固定输入事件、期望动作和命中规则，保证演示与回归一致性' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for scene_release
-- ----------------------------
DROP TABLE IF EXISTS `scene_release`;
CREATE TABLE `scene_release`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '发布记录主键',
  `scene_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '' COMMENT '场景编码',
  `version_no` int NOT NULL COMMENT '场景版本号，按场景维度单独递增',
  `snapshot_json` json NOT NULL COMMENT '运行时快照 JSON，是 Flink / 仿真执行器真正消费的去关系化结果',
  `checksum` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '' COMMENT '快照摘要，例如 SHA-256，用于校验版本内容是否一致',
  `publish_status` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'DRAFT' COMMENT '发布状态，例如 DRAFT、PUBLISHED、ACTIVE、ROLLED_BACK、FAILED',
  `validation_status` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'PENDING' COMMENT '发布前校验状态，例如 PENDING、PASSED、FAILED',
  `validation_report_json` json NULL COMMENT '发布前校验报告 JSON，例如缺失依赖、表达式校验结果、场景完整性检查结果',
  `dependency_digest_json` json NULL COMMENT '依赖摘要 JSON，例如当前版本依赖的字段、特征、规则、名单集合',
  `compile_duration_ms` bigint NULL DEFAULT NULL COMMENT '快照编译耗时，单位毫秒',
  `compiled_feature_count` int NOT NULL DEFAULT 0 COMMENT '本次快照编译出的特征数量',
  `compiled_rule_count` int NOT NULL DEFAULT 0 COMMENT '本次快照编译出的规则数量',
  `compiled_policy_count` int NOT NULL DEFAULT 0 COMMENT '本次快照编译出的策略数量',
  `published_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '发布人标识，通常写用户编号或用户名',
  `published_at` datetime NULL DEFAULT NULL COMMENT '发布时间',
  `effective_from` datetime NULL DEFAULT NULL COMMENT '生效时间，可用于立即生效或定时生效',
  `rollback_from_version` int NULL DEFAULT NULL COMMENT '若是回滚产生的版本，则记录来源版本号；正常发布为空',
  `remark` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '发布说明，例如“降低高风险交易阈值，准备灰度验证”',
  `creator` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_scene_version`(`scene_code` ASC, `version_no` ASC) USING BTREE,
  INDEX `idx_scene_publish_status`(`scene_code` ASC, `publish_status` ASC, `effective_from` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '场景发布表（P0-必须），保存运行时快照、校验结果、发布状态和回滚信息' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for policy_score_band
-- ----------------------------
DROP TABLE IF EXISTS `policy_score_band`;
CREATE TABLE `policy_score_band`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '策略分段主键',
  `policy_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '' COMMENT '策略编码，对应 policy_def.policy_code',
  `band_no` int NOT NULL DEFAULT 0 COMMENT '分段顺序，越小越优先匹配',
  `min_score` int NOT NULL DEFAULT 0 COMMENT '最小分值（含）',
  `max_score` int NOT NULL DEFAULT 0 COMMENT '最大分值（含）',
  `hit_action` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '' COMMENT '落入该分段后的最终动作，例如 PASS、REVIEW、REJECT',
  `hit_reason_template` varchar(1024) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '命中该分段时的原因模板，可引用 totalScore 等占位符',
  `enabled_flag` tinyint NOT NULL DEFAULT 1 COMMENT '是否启用：1-启用，0-停用',
  `creator` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_policy_band`(`policy_code` ASC, `band_no` ASC) USING BTREE,
  INDEX `idx_policy_score`(`policy_code` ASC, `min_score` ASC, `max_score` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '策略分值分段表（P1-推荐），用于 SCORE_CARD 策略把累计分值映射为最终动作' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for policy_rule_ref
-- ----------------------------
DROP TABLE IF EXISTS `policy_rule_ref`;
CREATE TABLE `policy_rule_ref`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '策略规则关联主键',
  `policy_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '' COMMENT '策略编码',
  `rule_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '' COMMENT '规则编码',
  `order_no` int NOT NULL DEFAULT 0 COMMENT '规则顺序；FIRST_HIT 场景严格按该顺序执行',
  `enabled_flag` tinyint NOT NULL DEFAULT 1 COMMENT '是否启用：1-启用，0-停用',
  `branch_expr` varchar(1024) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '可选分支表达式；满足时才执行当前规则',
  `score_weight` int NULL DEFAULT NULL COMMENT '分值权重；SCORE_CARD 场景可用于对 rule_def.risk_score 再做加权',
  `stop_on_hit` tinyint NOT NULL DEFAULT 1 COMMENT '命中后是否建议停止继续执行：1-停止，0-继续；SCORE_CARD 通常为 0',
  `creator` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_policy_rule`(`policy_code` ASC, `rule_code` ASC) USING BTREE,
  INDEX `idx_policy_order`(`policy_code` ASC, `order_no` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '策略规则关联表（P0-必须），定义规则在策略中的顺序、分值权重和分支条件' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for policy_def
-- ----------------------------
DROP TABLE IF EXISTS `policy_def`;
CREATE TABLE `policy_def`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '策略主键',
  `scene_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '' COMMENT '所属场景编码',
  `policy_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '' COMMENT '策略编码',
  `policy_name` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '' COMMENT '策略名称',
  `decision_mode` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'FIRST_HIT' COMMENT '决策模式：FIRST_HIT 或 SCORE_CARD',
  `default_action` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'PASS' COMMENT '无规则命中或无分段命中时的默认动作',
  `score_calc_mode` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'NONE' COMMENT '分值计算模式：NONE 或 SUM_HIT_SCORE',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '状态：1-启用，0-停用',
  `version` int NOT NULL DEFAULT 1 COMMENT '设计态版本号，用于识别策略是否被修改',
  `description` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '策略描述，建议写清楚适用场景和收敛逻辑',
  `creator` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_scene_policy`(`scene_code` ASC, `policy_code` ASC) USING BTREE,
  INDEX `idx_scene_status`(`scene_code` ASC, `status` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '策略定义表（P0-必须），定义 FIRST_HIT 或 SCORE_CARD 的收敛方式与默认动作' ROW_FORMAT = Dynamic;
-- ----------------------------
-- Table structure for rule_def
-- ----------------------------
DROP TABLE IF EXISTS `rule_def`;
CREATE TABLE `rule_def`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '规则主键',
  `scene_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '' COMMENT '所属场景编码',
  `rule_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '' COMMENT '规则编码',
  `rule_name` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '' COMMENT '规则名称',
  `rule_type` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'NORMAL' COMMENT '规则类型，例如 NORMAL、TAG_ONLY、MANUAL_REVIEW_HINT',
  `engine_type` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'AVIATOR' COMMENT '执行引擎类型，例如 DSL、AVIATOR、GROOVY',
  `expr_content` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '规则表达式内容，直接面向执行器',
  `priority` int NOT NULL DEFAULT 0 COMMENT '规则优先级，数值越大越优先；页面排序和发布校验都会用到',
  `hit_action` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'TAG_ONLY' COMMENT '规则命中后的动作；FIRST_HIT 场景常用 REVIEW/REJECT，SCORE_CARD 可用 TAG_ONLY',
  `risk_score` int NOT NULL DEFAULT 0 COMMENT '规则分值；SCORE_CARD 场景会参与累计',
  `hit_reason_template` varchar(1024) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '命中原因模板，可引用字段和特征占位符',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '状态：1-启用，0-停用',
  `version` int NOT NULL DEFAULT 1 COMMENT '设计态版本号',
  `description` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '规则说明，建议写清楚风控意图和误杀边界',
  `creator` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_scene_rule`(`scene_code` ASC, `rule_code` ASC) USING BTREE,
  INDEX `idx_scene_priority`(`scene_code` ASC, `status` ASC, `priority` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '规则定义表（P0-必须），定义单条判断逻辑、优先级、动作和命中原因模板' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for list_item
-- ----------------------------
DROP TABLE IF EXISTS `list_item`;
CREATE TABLE `list_item`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '名单条目主键',
  `list_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '' COMMENT '名单集合编码',
  `match_key` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '可选匹配键名，例如 deviceId、userId、ip；纯一维名单时可为空',
  `match_value` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '' COMMENT '匹配值，例如设备号、用户号、IP、手机号',
  `expire_at` datetime NULL DEFAULT NULL COMMENT '过期时间；为空表示长期有效',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '状态：1-有效，0-失效',
  `source_type` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'MANUAL' COMMENT '来源类型，例如 MANUAL、IMPORT_FILE、API_SYNC',
  `batch_no` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '导入批次号；批量导入时便于回滚或导出',
  `remark` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '条目备注，例如拉黑原因、工单号、渠道说明',
  `ext_json` json NULL COMMENT '扩展信息 JSON，例如风控原因、标签、来源系统、人工备注',
  `creator` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_list_value`(`list_code` ASC, `match_value` ASC) USING BTREE,
  INDEX `idx_list_code`(`list_code` ASC) USING BTREE,
  INDEX `idx_match_value`(`match_value` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '名单条目表（P1-推荐），用于存放黑白名单的具体匹配值和过期时间' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for list_set
-- ----------------------------
DROP TABLE IF EXISTS `list_set`;
CREATE TABLE `list_set`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '名单集合主键',
  `scene_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '' COMMENT '所属场景编码',
  `list_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '' COMMENT '名单编码',
  `list_name` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '' COMMENT '名单名称',
  `match_type` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'DEVICE' COMMENT '匹配维度，例如 USER、DEVICE、IP、MOBILE、CARD',
  `list_type` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'BLACK' COMMENT '名单类型，例如 BLACK、WHITE、WATCH',
  `storage_type` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'REDIS_SET' COMMENT '运行时存储形式，例如 REDIS_SET、REDIS_HASH',
  `sync_mode` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'INCREMENTAL' COMMENT '同步模式，例如 FULL、INCREMENTAL',
  `sync_status` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'PENDING' COMMENT '同步状态，例如 PENDING、SUCCESS、FAILED',
  `last_sync_time` datetime NULL DEFAULT NULL COMMENT '最近一次同步到 Redis 的时间',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '状态：1-启用，0-停用',
  `description` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '名单说明，建议写清楚用途和维护规则',
  `creator` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_scene_list`(`scene_code` ASC, `list_code` ASC) USING BTREE,
  INDEX `idx_scene_type`(`scene_code` ASC, `list_type` ASC, `status` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '名单集合表（P1-推荐），用于定义黑名单、白名单、观察名单以及其同步状态' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for feature_derived_conf
-- ----------------------------
DROP TABLE IF EXISTS `feature_derived_conf`;
CREATE TABLE `feature_derived_conf`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '派生特征配置主键',
  `feature_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '' COMMENT '特征编码，对应 feature_def.feature_code',
  `engine_type` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'AVIATOR' COMMENT '表达式引擎类型，例如 AVIATOR、GROOVY',
  `expr_content` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '派生表达式内容，基于字段/特征二次推导',
  `depends_on_json` json NULL COMMENT '依赖项数组 JSON，建议显式记录依赖的字段和特征编码',
  `value_type` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'BOOLEAN' COMMENT '派生特征值类型，例如 BOOLEAN、LONG、DECIMAL、STRING',
  `sandbox_flag` tinyint NOT NULL DEFAULT 1 COMMENT '是否启用脚本沙箱：1-启用，0-关闭；Groovy 场景建议始终启用',
  `timeout_ms` int NULL DEFAULT NULL COMMENT '执行超时时间，单位毫秒；用于保护 Groovy/复杂表达式',
  `extra_json` json NULL COMMENT '扩展配置 JSON，例如预编译开关、缓存键、返回值转换规则',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '状态：1-启用，0-停用',
  `creator` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_feature_derived`(`feature_code` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '派生特征配置表（P0-必须），用于表达式/Groovy 驱动的二次推导特征' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for feature_lookup_conf
-- ----------------------------
DROP TABLE IF EXISTS `feature_lookup_conf`;
CREATE TABLE `feature_lookup_conf`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '查询特征配置主键',
  `feature_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '' COMMENT '特征编码，对应 feature_def.feature_code',
  `lookup_type` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'REDIS_STRING' COMMENT '查询类型，例如 REDIS_SET、REDIS_HASH、REDIS_STRING、DICT',
  `key_expr` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '' COMMENT '构造 lookup key 的表达式，例如 userId、deviceId、ip',
  `source_ref` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '' COMMENT '查询来源引用，例如 Redis key 前缀、字典编码、名单编码',
  `default_value` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '查不到时的默认值',
  `cache_ttl_seconds` bigint NULL DEFAULT NULL COMMENT '本地缓存 TTL，单位秒；避免高频重复 lookup',
  `timeout_ms` int NULL DEFAULT NULL COMMENT '单次 lookup 超时时间，单位毫秒',
  `extra_json` json NULL COMMENT '扩展配置 JSON，例如 Redis key pattern、hash field、返回值转换规则',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '状态：1-启用，0-停用',
  `creator` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_feature_lookup`(`feature_code` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '查询特征配置表（P0-必须），用于 Redis/字典/名单类 lookup 特征定义' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for feature_stream_conf
-- ----------------------------
DROP TABLE IF EXISTS `feature_stream_conf`;
CREATE TABLE `feature_stream_conf`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '流式特征配置主键',
  `feature_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '' COMMENT '特征编码，对应 feature_def.feature_code',
  `source_event_codes` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '' COMMENT '来源事件编码，多个事件用英文逗号分隔，例如 login,trade',
  `entity_key_expr` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '' COMMENT '实体键表达式，例如 userId、deviceId、ip',
  `agg_type` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'COUNT' COMMENT '聚合类型，例如 COUNT、SUM、MAX、LATEST、DISTINCT_COUNT',
  `value_expr` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '取值表达式；SUM/MAX/DISTINCT_COUNT 等场景需要',
  `filter_expr` varchar(1024) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '过滤表达式；仅满足条件的事件才进入统计',
  `window_type` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'SLIDING' COMMENT '窗口类型，例如 TUMBLING、SLIDING、LATEST',
  `window_size` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '5m' COMMENT '窗口大小，例如 5m、10m、1h',
  `window_slide` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '窗口滑动步长，例如 1m、5m；LATEST 场景可为空',
  `include_current_event` tinyint NOT NULL DEFAULT 1 COMMENT '是否把当前事件计入本次统计：1-计入，0-不计入',
  `ttl_seconds` bigint NULL DEFAULT NULL COMMENT '状态 TTL，单位秒；用于控制 Flink State 生命周期',
  `state_hint_json` json NULL COMMENT '状态提示 JSON，例如 bucketHint、stateBackend、预估基数',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '状态：1-启用，0-停用',
  `creator` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_feature_stream`(`feature_code` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '流式特征配置表（P0-必须），定义窗口统计、实体聚合和状态提示信息' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for feature_def
-- ----------------------------
DROP TABLE IF EXISTS `feature_def`;
CREATE TABLE `feature_def`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '特征主键',
  `scene_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '' COMMENT '所属场景编码',
  `feature_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '' COMMENT '特征编码',
  `feature_name` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '' COMMENT '特征名称',
  `feature_type` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'STREAM' COMMENT '特征类型：STREAM、LOOKUP、DERIVED',
  `entity_type` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '实体类型，例如 USER、DEVICE、IP、MERCHANT',
  `event_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '主要来源事件编码',
  `value_type` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'LONG' COMMENT '特征值类型，例如 LONG、DECIMAL、BOOLEAN、STRING',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '状态：1-启用，0-停用',
  `version` int NOT NULL DEFAULT 1 COMMENT '设计态版本号，变更配置时递增',
  `description` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '特征说明，建议写清楚口径和边界',
  `creator` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_scene_feature`(`scene_code` ASC, `feature_code` ASC) USING BTREE,
  INDEX `idx_scene_type`(`scene_code` ASC, `feature_type` ASC, `status` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '特征定义主表（P0-必须），统一管理 Stream / Lookup / Derived 三类特征的公共属性' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for ingest_mapping_def
-- ----------------------------
DROP TABLE IF EXISTS `ingest_mapping_def`;
CREATE TABLE `ingest_mapping_def`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '接入映射主键',
  `source_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '' COMMENT '接入源编码，对应 ingest_source.source_code',
  `scene_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '' COMMENT '目标场景编码',
  `event_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '' COMMENT '目标事件编码',
  `source_field_path` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '' COMMENT '原始字段路径，例如 $.uid、$.req.traceId、$.amountFen',
  `target_field_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '' COMMENT '目标标准字段编码，例如 userId、eventTime、amount',
  `target_field_name` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '' COMMENT '目标标准字段名称',
  `transform_type` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'DIRECT' COMMENT '转换类型，例如 DIRECT、CONST、TIME_MILLIS_TO_DATETIME、DIVIDE_100、ENUM_MAP',
  `transform_expr` varchar(1024) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '转换表达式或常量值；复杂场景可写成受控表达式',
  `default_value` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '默认值；源字段缺失时使用',
  `required_flag` tinyint NOT NULL DEFAULT 0 COMMENT '是否必填：1-必填，0-非必填',
  `clean_rule_json` json NULL COMMENT '清洗规则 JSON，例如 trim、大小写标准化、空字符串转 null',
  `sort_no` int NOT NULL DEFAULT 0 COMMENT '规则顺序，按顺序执行映射和标准化',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '状态：1-启用，0-停用',
  `creator` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_source_event_target`(`source_code` ASC, `event_code` ASC, `target_field_code` ASC) USING BTREE,
  INDEX `idx_scene_event_sort`(`scene_code` ASC, `event_code` ASC, `sort_no` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '接入字段映射与标准化表（P0-必须），定义原始报文字段如何映射为标准 RiskEvent 字段' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for ingest_source
-- ----------------------------
DROP TABLE IF EXISTS `ingest_source`;
CREATE TABLE `ingest_source`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '接入源主键',
  `source_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '' COMMENT '接入源编码，例如 APP_BEACON_WEB、PAY_CORE_SDK',
  `source_name` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '' COMMENT '接入源名称',
  `source_type` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'HTTP' COMMENT '接入方式：HTTP、BEACON、SDK',
  `auth_type` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'NONE' COMMENT '鉴权方式：NONE、TOKEN、HMAC、AKSK、JWT',
  `auth_config_json` json NULL COMMENT '鉴权配置 JSON，例如 appKey、签名算法、header 名、过期时间容忍度',
  `scene_scope_json` json NULL COMMENT '允许接入的场景范围 JSON，例如 ["LOGIN_RISK","TRADE_RISK"]',
  `standard_topic_name` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'pulsix.event.standard' COMMENT '标准事件写入 Topic',
  `error_topic_name` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'pulsix.event.dlq' COMMENT '异常事件写入 Topic',
  `rate_limit_qps` int NOT NULL DEFAULT 0 COMMENT '限流阈值，0 表示不限制',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '状态：1-启用，0-停用',
  `description` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '接入源说明，建议写清楚用途和责任系统',
  `creator` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_source_code`(`source_code` ASC) USING BTREE,
  INDEX `idx_type_status`(`source_type` ASC, `status` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '接入源管理表（P0-必须 + P1-推荐），定义 HTTP / Beacon / SDK 来源、鉴权和 Topic 归属' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for event_sample
-- ----------------------------
DROP TABLE IF EXISTS `event_sample`;
CREATE TABLE `event_sample`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '样例主键',
  `scene_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '' COMMENT '所属场景编码',
  `event_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '' COMMENT '所属事件编码',
  `sample_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '' COMMENT '样例编码，便于直接引用',
  `sample_name` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '' COMMENT '样例名称',
  `sample_type` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'STANDARD' COMMENT '样例类型：RAW、STANDARD、SIMULATION',
  `source_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '来源接入源编码；STANDARD/SIMULATION 样例可为空',
  `sample_json` json NOT NULL COMMENT '样例报文 JSON',
  `description` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '样例说明，建议写清楚该样例想说明的接入/业务特征',
  `sort_no` int NOT NULL DEFAULT 0 COMMENT '排序号',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '状态：1-启用，0-停用',
  `creator` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_scene_event_sample`(`scene_code` ASC, `event_code` ASC, `sample_code` ASC) USING BTREE,
  INDEX `idx_scene_event_sort`(`scene_code` ASC, `event_code` ASC, `sort_no` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '事件样例表（P0-必须），存放原始报文、标准报文和仿真输入样例' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for event_field_def
-- ----------------------------
DROP TABLE IF EXISTS `event_field_def`;
CREATE TABLE `event_field_def`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '事件字段主键',
  `event_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '' COMMENT '所属事件编码',
  `field_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '' COMMENT '字段编码，即标准事件里的技术字段名，例如 userId、amount',
  `field_name` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '' COMMENT '字段中文名称',
  `field_type` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'STRING' COMMENT '字段类型，例如 STRING、LONG、DECIMAL、BOOLEAN、DATETIME、JSON',
  `field_path` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '标准事件中的 JSONPath，例如 $.userId、$.ext.city',
  `standard_field_flag` tinyint NOT NULL DEFAULT 0 COMMENT '是否标准公共字段：1-是，0-否',
  `required_flag` tinyint NOT NULL DEFAULT 0 COMMENT '是否必填：1-必填，0-非必填',
  `nullable_flag` tinyint NOT NULL DEFAULT 1 COMMENT '是否允许为空：1-允许，0-不允许',
  `default_value` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '默认值',
  `sample_value` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '示例值，用于页面提示和样例生成',
  `validation_rule_json` json NULL COMMENT '校验规则 JSON，例如枚举值、最小值、时间格式、长度限制等',
  `description` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '字段说明',
  `sort_no` int NOT NULL DEFAULT 0 COMMENT '排序号',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '状态：1-启用，0-停用',
  `creator` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_event_field`(`event_code` ASC, `field_code` ASC) USING BTREE,
  INDEX `idx_event_sort`(`event_code` ASC, `sort_no` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '事件字段定义表（P0-必须），定义字段类型、必填性、默认值和基础校验规则' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for event_schema
-- ----------------------------
DROP TABLE IF EXISTS `event_schema`;
CREATE TABLE `event_schema`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '事件模型主键',
  `scene_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '' COMMENT '所属场景编码',
  `event_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '' COMMENT '事件编码，例如 login、trade、register',
  `event_name` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '' COMMENT '事件名称',
  `event_type` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'BUSINESS' COMMENT '事件类别，例如 BUSINESS、CALLBACK、TEST',
  `source_type` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'MIXED' COMMENT '默认接入方式：HTTP、BEACON、SDK、MIXED',
  `raw_topic_name` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '原始事件 Topic；若直接 HTTP 入库也可为空',
  `standard_topic_name` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '标准事件 Topic',
  `version` int NOT NULL DEFAULT 1 COMMENT '事件模型版本号',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '状态：1-启用，0-停用',
  `description` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '事件模型说明',
  `creator` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_scene_event`(`scene_code` ASC, `event_code` ASC) USING BTREE,
  INDEX `idx_scene_status`(`scene_code` ASC, `status` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '事件模型表（P0-必须），定义一个场景下有哪些标准事件以及它们的接入方式' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for entity_type_def
-- ----------------------------
DROP TABLE IF EXISTS `entity_type_def`;
CREATE TABLE `entity_type_def`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '实体类型主键',
  `entity_type` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '' COMMENT '实体类型编码，例如 USER、DEVICE、IP、MERCHANT',
  `entity_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '' COMMENT '实体类型名称',
  `key_field_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '' COMMENT '在标准事件中的主键字段名，例如 userId、deviceId、ip',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '状态：1-启用，0-停用',
  `description` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '实体说明',
  `creator` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_entity_type`(`entity_type` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '实体类型表（P1-推荐），用于统一 user/device/ip/merchant 等聚合维度' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for scene_def
-- ----------------------------
DROP TABLE IF EXISTS `scene_def`;
CREATE TABLE `scene_def`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '场景主键',
  `scene_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '' COMMENT '场景编码，例如 LOGIN_RISK、TRADE_RISK',
  `scene_name` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '' COMMENT '场景名称',
  `scene_type` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'GENERAL' COMMENT '场景类型，例如 ACCOUNT_SECURITY、TRADE_SECURITY',
  `access_mode` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'MIXED' COMMENT '场景主要接入模式：HTTP、BEACON、SDK、MIXED',
  `default_event_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '默认事件编码，用于场景首页展示和快捷仿真',
  `default_policy_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '默认策略编码，一期直接挂场景主策略',
  `standard_topic_name` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '标准事件 Topic',
  `decision_topic_name` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '决策结果 Topic',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '状态：1-启用，0-停用',
  `description` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '场景说明',
  `creator` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_scene_code`(`scene_code` ASC) USING BTREE,
  INDEX `idx_scene_status`(`status` ASC, `update_time` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '场景定义表（P0-必须），是事件、特征、规则、策略、发布版本的组织根对象' ROW_FORMAT = Dynamic;

-- ============================================================
-- Seed data
-- 说明：
-- 1. 样例数据尽量围绕 LOGIN_RISK 和 TRADE_RISK 两个场景展开，兼顾 FIRST_HIT 与 SCORE_CARD。
-- 2. 样例数量保持“够理解但不过载”的原则，便于直接导入后快速浏览。
-- ============================================================
-- ----------------------------
-- Records of scene_def
-- ----------------------------
INSERT INTO `scene_def` (`id`, `scene_code`, `scene_name`, `scene_type`, `access_mode`, `default_event_code`, `default_policy_code`, `standard_topic_name`, `decision_topic_name`, `status`, `description`, `creator`, `create_time`, `updater`, `update_time`, `deleted`) VALUES
(1001, 'LOGIN_RISK', '登录风控', 'ACCOUNT_SECURITY', 'MIXED', 'login', 'LOGIN_RISK_POLICY', 'pulsix.event.standard', 'pulsix.decision.result', 1, '用于识别撞库、密码爆破、黑名单设备登录和异常 IP 登录等登录风险', 'admin', '2026-03-08 09:00:00', 'admin', '2026-03-08 09:00:00', b'0'),
(1002, 'TRADE_RISK', '交易风控', 'TRADE_SECURITY', 'SDK', 'trade', 'TRADE_RISK_SCORECARD', 'pulsix.event.standard', 'pulsix.decision.result', 1, '用于演示实时聚合、lookup、派生特征和 SCORE_CARD 策略收敛', 'admin', '2026-03-08 09:00:00', 'admin', '2026-03-08 09:00:00', b'0');

-- ----------------------------
-- Records of entity_type_def
-- ----------------------------
INSERT INTO `entity_type_def` (`id`, `entity_type`, `entity_name`, `key_field_name`, `status`, `description`, `creator`, `create_time`, `updater`, `update_time`, `deleted`) VALUES
(1101, 'USER', '用户', 'userId', 1, '最常见的风控聚合维度，用于统计用户行为、画像和名单', 'admin', '2026-03-08 09:01:00', 'admin', '2026-03-08 09:01:00', b'0'),
(1102, 'DEVICE', '设备', 'deviceId', 1, '用于识别一机多号、黑产设备等设备相关风险', 'admin', '2026-03-08 09:01:00', 'admin', '2026-03-08 09:01:00', b'0'),
(1103, 'IP', 'IP 地址', 'ip', 1, '用于聚合同 IP 失败次数、代理 IP 风险等', 'admin', '2026-03-08 09:01:00', 'admin', '2026-03-08 09:01:00', b'0'),
(1104, 'MERCHANT', '商户', 'merchantId', 1, '用于交易场景中的商户风险、渠道风险和多维联防', 'admin', '2026-03-08 09:01:00', 'admin', '2026-03-08 09:01:00', b'0');

-- ----------------------------
-- Records of event_schema
-- ----------------------------
INSERT INTO `event_schema` (`id`, `scene_code`, `event_code`, `event_name`, `event_type`, `source_type`, `raw_topic_name`, `standard_topic_name`, `version`, `status`, `description`, `creator`, `create_time`, `updater`, `update_time`, `deleted`) VALUES
(1201, 'LOGIN_RISK', 'login', '登录事件', 'BUSINESS', 'MIXED', 'pulsix.event.raw.login', 'pulsix.event.standard', 1, 1, '登录风控标准事件，支持 Web Beacon、App HTTP、SDK 三种来源标准化接入', 'admin', '2026-03-08 09:02:00', 'admin', '2026-03-08 09:02:00', b'0'),
(1202, 'TRADE_RISK', 'trade', '交易事件', 'BUSINESS', 'SDK', 'pulsix.event.raw.trade', 'pulsix.event.standard', 1, 1, '交易风控标准事件，重点演示金额、用户、设备和画像联动', 'admin', '2026-03-08 09:02:00', 'admin', '2026-03-08 09:02:00', b'0');

-- ----------------------------
-- Records of event_field_def
-- ----------------------------
INSERT INTO `event_field_def` (`id`, `event_code`, `field_code`, `field_name`, `field_type`, `field_path`, `standard_field_flag`, `required_flag`, `nullable_flag`, `default_value`, `sample_value`, `validation_rule_json`, `description`, `sort_no`, `status`, `creator`, `create_time`, `updater`, `update_time`, `deleted`) VALUES
(1301, 'login', 'eventId', '事件编号', 'STRING', '$.eventId', 1, 1, 0, NULL, 'E_LOGIN_0003', '{"maxLength":64}', '业务事件唯一编号', 1, 1, 'admin', '2026-03-08 09:03:00', 'admin', '2026-03-08 09:03:00', b'0'),
(1302, 'login', 'traceId', '链路号', 'STRING', '$.traceId', 1, 1, 0, NULL, 'T_LOGIN_0003', '{"maxLength":64}', '贯穿接入、引擎、日志查询的追踪号', 2, 1, 'admin', '2026-03-08 09:03:00', 'admin', '2026-03-08 09:03:00', b'0'),
(1303, 'login', 'sceneCode', '场景编码', 'STRING', '$.sceneCode', 1, 1, 0, 'LOGIN_RISK', 'LOGIN_RISK', '{"enum":["LOGIN_RISK"]}', '标准事件场景编码', 3, 1, 'admin', '2026-03-08 09:03:00', 'admin', '2026-03-08 09:03:00', b'0'),
(1304, 'login', 'eventType', '事件类型', 'STRING', '$.eventType', 1, 1, 0, 'login', 'login', '{"enum":["login"]}', '标准事件类型', 4, 1, 'admin', '2026-03-08 09:03:00', 'admin', '2026-03-08 09:03:00', b'0'),
(1305, 'login', 'eventTime', '事件时间', 'DATETIME', '$.eventTime', 1, 1, 0, NULL, '2026-03-07T09:20:00', '{"formats":["ISO8601","yyyy-MM-dd HH:mm:ss","epoch_millis"]}', '事件发生时间', 5, 1, 'admin', '2026-03-08 09:03:00', 'admin', '2026-03-08 09:03:00', b'0'),
(1306, 'login', 'userId', '用户编号', 'STRING', '$.userId', 1, 1, 0, NULL, 'U1001', '{"maxLength":64}', '用户主键', 6, 1, 'admin', '2026-03-08 09:03:00', 'admin', '2026-03-08 09:03:00', b'0'),
(1307, 'login', 'deviceId', '设备编号', 'STRING', '$.deviceId', 1, 1, 0, NULL, 'D9002', '{"maxLength":64}', '设备主键', 7, 1, 'admin', '2026-03-08 09:03:00', 'admin', '2026-03-08 09:03:00', b'0'),
(1308, 'login', 'ip', 'IP 地址', 'STRING', '$.ip', 1, 1, 0, NULL, '10.20.30.40', '{"format":"IPV4"}', '请求来源 IP', 8, 1, 'admin', '2026-03-08 09:03:00', 'admin', '2026-03-08 09:03:00', b'0'),
(1309, 'login', 'channel', '渠道', 'STRING', '$.channel', 0, 0, 1, 'APP', 'APP', '{"enum":["APP","WEB","H5"]}', '登录渠道', 9, 1, 'admin', '2026-03-08 09:03:00', 'admin', '2026-03-08 09:03:00', b'0'),
(1310, 'login', 'loginResult', '登录结果', 'STRING', '$.loginResult', 0, 1, 0, NULL, 'FAIL', '{"enum":["SUCCESS","FAIL"]}', '登录结果，流式失败次数特征依赖该字段', 10, 1, 'admin', '2026-03-08 09:03:00', 'admin', '2026-03-08 09:03:00', b'0'),
(1321, 'trade', 'eventId', '事件编号', 'STRING', '$.eventId', 1, 1, 0, NULL, 'E_TRADE_0001', '{"maxLength":64}', '业务事件唯一编号', 1, 1, 'admin', '2026-03-08 09:03:00', 'admin', '2026-03-08 09:03:00', b'0'),
(1322, 'trade', 'traceId', '链路号', 'STRING', '$.traceId', 1, 1, 0, NULL, 'T_TRADE_0001', '{"maxLength":64}', '链路追踪号', 2, 1, 'admin', '2026-03-08 09:03:00', 'admin', '2026-03-08 09:03:00', b'0'),
(1323, 'trade', 'sceneCode', '场景编码', 'STRING', '$.sceneCode', 1, 1, 0, 'TRADE_RISK', 'TRADE_RISK', '{"enum":["TRADE_RISK"]}', '标准事件场景编码', 3, 1, 'admin', '2026-03-08 09:03:00', 'admin', '2026-03-08 09:03:00', b'0'),
(1324, 'trade', 'eventType', '事件类型', 'STRING', '$.eventType', 1, 1, 0, 'trade', 'trade', '{"enum":["trade"]}', '标准事件类型', 4, 1, 'admin', '2026-03-08 09:03:00', 'admin', '2026-03-08 09:03:00', b'0'),
(1325, 'trade', 'eventTime', '事件时间', 'DATETIME', '$.eventTime', 1, 1, 0, NULL, '2026-03-07T11:00:00', '{"formats":["ISO8601","yyyy-MM-dd HH:mm:ss","epoch_millis"]}', '交易发生时间', 5, 1, 'admin', '2026-03-08 09:03:00', 'admin', '2026-03-08 09:03:00', b'0'),
(1326, 'trade', 'userId', '用户编号', 'STRING', '$.userId', 1, 1, 0, NULL, 'U5001', '{"maxLength":64}', '用户主键', 6, 1, 'admin', '2026-03-08 09:03:00', 'admin', '2026-03-08 09:03:00', b'0'),
(1327, 'trade', 'deviceId', '设备编号', 'STRING', '$.deviceId', 1, 1, 0, NULL, 'D5001', '{"maxLength":64}', '设备主键', 7, 1, 'admin', '2026-03-08 09:03:00', 'admin', '2026-03-08 09:03:00', b'0'),
(1328, 'trade', 'ip', 'IP 地址', 'STRING', '$.ip', 1, 1, 0, NULL, '66.77.88.99', '{"format":"IPV4"}', '交易请求 IP', 8, 1, 'admin', '2026-03-08 09:03:00', 'admin', '2026-03-08 09:03:00', b'0'),
(1329, 'trade', 'amount', '交易金额', 'DECIMAL', '$.amount', 0, 1, 0, NULL, '6800', '{"min":0}', '交易金额，单位元', 9, 1, 'admin', '2026-03-08 09:03:00', 'admin', '2026-03-08 09:03:00', b'0'),
(1330, 'trade', 'merchantId', '商户编号', 'STRING', '$.merchantId', 0, 1, 0, NULL, 'M1001', '{"maxLength":64}', '商户主键', 10, 1, 'admin', '2026-03-08 09:03:00', 'admin', '2026-03-08 09:03:00', b'0'),
(1331, 'trade', 'payMethod', '支付方式', 'STRING', '$.payMethod', 0, 0, 1, 'CARD', 'CARD', '{"enum":["CARD","BALANCE","WALLET"]}', '支付方式', 11, 1, 'admin', '2026-03-08 09:03:00', 'admin', '2026-03-08 09:03:00', b'0');

-- ----------------------------
-- Records of event_sample
-- ----------------------------
INSERT INTO `event_sample` (`id`, `scene_code`, `event_code`, `sample_code`, `sample_name`, `sample_type`, `source_code`, `sample_json`, `description`, `sort_no`, `status`, `creator`, `create_time`, `updater`, `update_time`, `deleted`) VALUES
(1401, 'LOGIN_RISK', 'login', 'LOGIN_RAW_BEACON_01', 'Web Beacon 原始登录报文', 'RAW', 'APP_BEACON_WEB', '{"event_id":"raw_login_0001","trace_id":"T_LOGIN_RAW_0001","uid":"U1001","did":"D9002","clientIp":"10.20.30.40","ts_ms":1772836800000,"result":"FAIL","channel":"WEB"}', '用于说明原始报文中的字段名与标准字段名并不一致，需要做映射和时间格式转换', 1, 1, 'admin', '2026-03-08 09:04:00', 'admin', '2026-03-08 09:04:00', b'0'),
(1402, 'LOGIN_RISK', 'login', 'LOGIN_STD_01', '标准登录事件样例', 'STANDARD', NULL, '{"eventId":"E_LOGIN_0003","traceId":"T_LOGIN_0003","sceneCode":"LOGIN_RISK","eventType":"login","eventTime":"2026-03-07T09:20:00","userId":"U1001","deviceId":"D9002","ip":"10.20.30.40","channel":"APP","loginResult":"FAIL"}', '直接用于规则预览、仿真测试和 README 演示', 2, 1, 'admin', '2026-03-08 09:04:00', 'admin', '2026-03-08 09:04:00', b'0'),
(1403, 'TRADE_RISK', 'trade', 'TRADE_RAW_SDK_01', '支付核心 SDK 原始交易报文', 'RAW', 'PAY_CORE_SDK', '{"req":{"traceId":"T_TRADE_RAW_0001","occurTime":1772842800000},"biz":{"tradeId":"E_TRADE_0001","uid":"U5001","deviceNo":"D5001","ip":"66.77.88.99","amountFen":680000,"merchantNo":"M1001","payMethod":"CARD"}}', '用于说明 SDK 侧嵌套结构、毫秒时间戳、分到元金额转换', 1, 1, 'admin', '2026-03-08 09:04:00', 'admin', '2026-03-08 09:04:00', b'0'),
(1404, 'TRADE_RISK', 'trade', 'TRADE_STD_01', '标准交易事件样例', 'STANDARD', NULL, '{"eventId":"E_TRADE_0001","traceId":"T_TRADE_0001","sceneCode":"TRADE_RISK","eventType":"trade","eventTime":"2026-03-07T11:00:00","userId":"U5001","deviceId":"D5001","ip":"66.77.88.99","amount":6800,"merchantId":"M1001","payMethod":"CARD"}', '直接用于策略仿真和决策链路演示', 2, 1, 'admin', '2026-03-08 09:04:00', 'admin', '2026-03-08 09:04:00', b'0');

-- ----------------------------
-- Records of ingest_source
-- ----------------------------
INSERT INTO `ingest_source` (`id`, `source_code`, `source_name`, `source_type`, `auth_type`, `auth_config_json`, `scene_scope_json`, `standard_topic_name`, `error_topic_name`, `rate_limit_qps`, `status`, `description`, `creator`, `create_time`, `updater`, `update_time`, `deleted`) VALUES
(1501, 'APP_BEACON_WEB', 'Web/App 统一 Beacon 接入', 'BEACON', 'TOKEN', '{"headerName":"X-Pulsix-Token","tokenPrefix":"pulsix-demo-","allowClockSkewSeconds":60}', '["LOGIN_RISK"]', 'pulsix.event.standard', 'pulsix.event.dlq', 5000, 1, '前端埋点、H5 和 Web 统一通过 Beacon 接入登录相关事件', 'admin', '2026-03-08 09:05:00', 'admin', '2026-03-08 09:05:00', b'0'),
(1502, 'PAY_CORE_SDK', '支付核心服务 SDK 接入', 'SDK', 'AKSK', '{"appKey":"trade-demo-key","signAlg":"HMAC_SHA256","signatureField":"sign"}', '["TRADE_RISK"]', 'pulsix.event.standard', 'pulsix.event.dlq', 2000, 1, '核心交易服务通过 SDK 直连接入交易事件', 'admin', '2026-03-08 09:05:00', 'admin', '2026-03-08 09:05:00', b'0');

-- ----------------------------
-- Records of ingest_mapping_def
-- ----------------------------
INSERT INTO `ingest_mapping_def` (`id`, `source_code`, `scene_code`, `event_code`, `source_field_path`, `target_field_code`, `target_field_name`, `transform_type`, `transform_expr`, `default_value`, `required_flag`, `clean_rule_json`, `sort_no`, `status`, `creator`, `create_time`, `updater`, `update_time`, `deleted`) VALUES
(1601, 'APP_BEACON_WEB', 'LOGIN_RISK', 'login', '$.event_id', 'eventId', '事件编号', 'DIRECT', NULL, NULL, 1, '{"trim":true}', 1, 1, 'admin', '2026-03-08 09:06:00', 'admin', '2026-03-08 09:06:00', b'0'),
(1602, 'APP_BEACON_WEB', 'LOGIN_RISK', 'login', '$.trace_id', 'traceId', '链路号', 'DIRECT', NULL, NULL, 1, '{"trim":true}', 2, 1, 'admin', '2026-03-08 09:06:00', 'admin', '2026-03-08 09:06:00', b'0'),
(1603, 'APP_BEACON_WEB', 'LOGIN_RISK', 'login', '$.uid', 'userId', '用户编号', 'DIRECT', NULL, NULL, 1, '{"trim":true}', 3, 1, 'admin', '2026-03-08 09:06:00', 'admin', '2026-03-08 09:06:00', b'0'),
(1604, 'APP_BEACON_WEB', 'LOGIN_RISK', 'login', '$.did', 'deviceId', '设备编号', 'DIRECT', NULL, NULL, 1, '{"trim":true}', 4, 1, 'admin', '2026-03-08 09:06:00', 'admin', '2026-03-08 09:06:00', b'0'),
(1605, 'APP_BEACON_WEB', 'LOGIN_RISK', 'login', '$.clientIp', 'ip', 'IP 地址', 'DIRECT', NULL, NULL, 1, '{"trim":true}', 5, 1, 'admin', '2026-03-08 09:06:00', 'admin', '2026-03-08 09:06:00', b'0'),
(1606, 'APP_BEACON_WEB', 'LOGIN_RISK', 'login', '$.ts_ms', 'eventTime', '事件时间', 'TIME_MILLIS_TO_DATETIME', NULL, NULL, 1, NULL, 6, 1, 'admin', '2026-03-08 09:06:00', 'admin', '2026-03-08 09:06:00', b'0'),
(1607, 'APP_BEACON_WEB', 'LOGIN_RISK', 'login', '$.result', 'loginResult', '登录结果', 'ENUM_MAP', '{"SUCCESS":"SUCCESS","FAIL":"FAIL"}', NULL, 1, '{"upperCase":true}', 7, 1, 'admin', '2026-03-08 09:06:00', 'admin', '2026-03-08 09:06:00', b'0'),
(1608, 'APP_BEACON_WEB', 'LOGIN_RISK', 'login', '', 'sceneCode', '场景编码', 'CONST', 'LOGIN_RISK', 'LOGIN_RISK', 1, NULL, 8, 1, 'admin', '2026-03-08 09:06:00', 'admin', '2026-03-08 09:06:00', b'0'),
(1609, 'APP_BEACON_WEB', 'LOGIN_RISK', 'login', '', 'eventType', '事件类型', 'CONST', 'login', 'login', 1, NULL, 9, 1, 'admin', '2026-03-08 09:06:00', 'admin', '2026-03-08 09:06:00', b'0'),
(1610, 'PAY_CORE_SDK', 'TRADE_RISK', 'trade', '$.biz.tradeId', 'eventId', '事件编号', 'DIRECT', NULL, NULL, 1, '{"trim":true}', 1, 1, 'admin', '2026-03-08 09:06:00', 'admin', '2026-03-08 09:06:00', b'0'),
(1611, 'PAY_CORE_SDK', 'TRADE_RISK', 'trade', '$.req.traceId', 'traceId', '链路号', 'DIRECT', NULL, NULL, 1, '{"trim":true}', 2, 1, 'admin', '2026-03-08 09:06:00', 'admin', '2026-03-08 09:06:00', b'0'),
(1612, 'PAY_CORE_SDK', 'TRADE_RISK', 'trade', '$.req.occurTime', 'eventTime', '事件时间', 'TIME_MILLIS_TO_DATETIME', NULL, NULL, 1, NULL, 3, 1, 'admin', '2026-03-08 09:06:00', 'admin', '2026-03-08 09:06:00', b'0'),
(1613, 'PAY_CORE_SDK', 'TRADE_RISK', 'trade', '$.biz.uid', 'userId', '用户编号', 'DIRECT', NULL, NULL, 1, '{"trim":true}', 4, 1, 'admin', '2026-03-08 09:06:00', 'admin', '2026-03-08 09:06:00', b'0'),
(1614, 'PAY_CORE_SDK', 'TRADE_RISK', 'trade', '$.biz.deviceNo', 'deviceId', '设备编号', 'DIRECT', NULL, NULL, 1, '{"trim":true}', 5, 1, 'admin', '2026-03-08 09:06:00', 'admin', '2026-03-08 09:06:00', b'0'),
(1615, 'PAY_CORE_SDK', 'TRADE_RISK', 'trade', '$.biz.ip', 'ip', 'IP 地址', 'DIRECT', NULL, NULL, 1, '{"trim":true}', 6, 1, 'admin', '2026-03-08 09:06:00', 'admin', '2026-03-08 09:06:00', b'0'),
(1616, 'PAY_CORE_SDK', 'TRADE_RISK', 'trade', '$.biz.amountFen', 'amount', '交易金额', 'DIVIDE_100', NULL, NULL, 1, NULL, 7, 1, 'admin', '2026-03-08 09:06:00', 'admin', '2026-03-08 09:06:00', b'0'),
(1617, 'PAY_CORE_SDK', 'TRADE_RISK', 'trade', '$.biz.merchantNo', 'merchantId', '商户编号', 'DIRECT', NULL, NULL, 1, '{"trim":true}', 8, 1, 'admin', '2026-03-08 09:06:00', 'admin', '2026-03-08 09:06:00', b'0'),
(1618, 'PAY_CORE_SDK', 'TRADE_RISK', 'trade', '$.biz.payMethod', 'payMethod', '支付方式', 'DIRECT', NULL, 'CARD', 0, '{"upperCase":true}', 9, 1, 'admin', '2026-03-08 09:06:00', 'admin', '2026-03-08 09:06:00', b'0'),
(1619, 'PAY_CORE_SDK', 'TRADE_RISK', 'trade', '', 'sceneCode', '场景编码', 'CONST', 'TRADE_RISK', 'TRADE_RISK', 1, NULL, 10, 1, 'admin', '2026-03-08 09:06:00', 'admin', '2026-03-08 09:06:00', b'0'),
(1620, 'PAY_CORE_SDK', 'TRADE_RISK', 'trade', '', 'eventType', '事件类型', 'CONST', 'trade', 'trade', 1, NULL, 11, 1, 'admin', '2026-03-08 09:06:00', 'admin', '2026-03-08 09:06:00', b'0');

-- ----------------------------
-- Records of feature_def
-- ----------------------------
INSERT INTO `feature_def` (`id`, `scene_code`, `feature_code`, `feature_name`, `feature_type`, `entity_type`, `event_code`, `value_type`, `status`, `version`, `description`, `creator`, `create_time`, `updater`, `update_time`, `deleted`) VALUES
(1701, 'LOGIN_RISK', 'user_login_fail_cnt_10m', '用户 10 分钟登录失败次数', 'STREAM', 'USER', 'login', 'LONG', 1, 1, '统计 userId 维度 10 分钟内登录失败次数', 'admin', '2026-03-08 09:07:00', 'admin', '2026-03-08 09:07:00', b'0'),
(1702, 'LOGIN_RISK', 'ip_login_fail_cnt_10m', 'IP 10 分钟登录失败次数', 'STREAM', 'IP', 'login', 'LONG', 1, 1, '统计 IP 维度 10 分钟内登录失败次数', 'admin', '2026-03-08 09:07:00', 'admin', '2026-03-08 09:07:00', b'0'),
(1703, 'LOGIN_RISK', 'device_login_user_cnt_1h', '设备 1 小时关联登录用户数', 'STREAM', 'DEVICE', 'login', 'LONG', 1, 1, '统计同一设备 1 小时关联的去重用户数，用于识别一机多号', 'admin', '2026-03-08 09:07:00', 'admin', '2026-03-08 09:07:00', b'0'),
(1704, 'LOGIN_RISK', 'device_in_blacklist', '设备是否命中黑名单', 'LOOKUP', 'DEVICE', 'login', 'BOOLEAN', 1, 1, '通过名单中心或 Redis Set 判断设备是否在黑名单中', 'admin', '2026-03-08 09:07:00', 'admin', '2026-03-08 09:07:00', b'0'),
(1705, 'LOGIN_RISK', 'ip_risk_level', 'IP 风险等级', 'LOOKUP', 'IP', 'login', 'STRING', 1, 1, '从画像或字典中查询 IP 风险等级，例如 LOW/MEDIUM/HIGH', 'admin', '2026-03-08 09:07:00', 'admin', '2026-03-08 09:07:00', b'0'),
(1706, 'LOGIN_RISK', 'user_in_white_list', '用户是否命中白名单', 'LOOKUP', 'USER', 'login', 'BOOLEAN', 1, 1, '用于对高价值用户做保护，避免误杀', 'admin', '2026-03-08 09:07:00', 'admin', '2026-03-08 09:07:00', b'0'),
(1707, 'LOGIN_RISK', 'high_fail_user_flag', '用户失败登录过多标记', 'DERIVED', 'USER', 'login', 'BOOLEAN', 1, 1, '根据 user_login_fail_cnt_10m 二次推导出的布尔型风险标志', 'admin', '2026-03-08 09:07:00', 'admin', '2026-03-08 09:07:00', b'0'),
(1711, 'TRADE_RISK', 'user_trade_cnt_5m', '用户 5 分钟交易次数', 'STREAM', 'USER', 'trade', 'LONG', 1, 1, '统计用户 5 分钟内交易次数', 'admin', '2026-03-08 09:07:00', 'admin', '2026-03-08 09:07:00', b'0'),
(1712, 'TRADE_RISK', 'user_trade_amt_sum_30m', '用户 30 分钟交易金额汇总', 'STREAM', 'USER', 'trade', 'DECIMAL', 1, 1, '统计用户 30 分钟内交易金额汇总', 'admin', '2026-03-08 09:07:00', 'admin', '2026-03-08 09:07:00', b'0'),
(1713, 'TRADE_RISK', 'device_bind_user_cnt_1h', '设备 1 小时关联用户数', 'STREAM', 'DEVICE', 'trade', 'LONG', 1, 1, '统计设备 1 小时内关联的去重用户数', 'admin', '2026-03-08 09:07:00', 'admin', '2026-03-08 09:07:00', b'0'),
(1714, 'TRADE_RISK', 'user_risk_level', '用户风险等级', 'LOOKUP', 'USER', 'trade', 'STRING', 1, 1, '从画像中心查询用户风险等级，例如 L/M/H', 'admin', '2026-03-08 09:07:00', 'admin', '2026-03-08 09:07:00', b'0'),
(1715, 'TRADE_RISK', 'high_risk_trade_flag', '高风险交易标记', 'DERIVED', 'USER', 'trade', 'BOOLEAN', 1, 2, '根据交易金额、交易次数和用户风险等级综合推导高风险交易标记；当前是候选 v2 设计', 'admin', '2026-03-08 09:07:00', 'admin', '2026-03-08 09:07:00', b'0');

-- ----------------------------
-- Records of feature_stream_conf
-- ----------------------------
INSERT INTO `feature_stream_conf` (`id`, `feature_code`, `source_event_codes`, `entity_key_expr`, `agg_type`, `value_expr`, `filter_expr`, `window_type`, `window_size`, `window_slide`, `include_current_event`, `ttl_seconds`, `state_hint_json`, `status`, `creator`, `create_time`, `updater`, `update_time`, `deleted`) VALUES
(1801, 'user_login_fail_cnt_10m', 'login', 'userId', 'COUNT', NULL, 'loginResult == ''FAIL''', 'SLIDING', '10m', '1m', 1, 3600, '{"bucketHint":"1m","stateBackend":"rocksdb"}', 1, 'admin', '2026-03-08 09:08:00', 'admin', '2026-03-08 09:08:00', b'0'),
(1802, 'ip_login_fail_cnt_10m', 'login', 'ip', 'COUNT', NULL, 'loginResult == ''FAIL''', 'SLIDING', '10m', '1m', 1, 3600, '{"bucketHint":"1m","stateBackend":"rocksdb"}', 1, 'admin', '2026-03-08 09:08:00', 'admin', '2026-03-08 09:08:00', b'0'),
(1803, 'device_login_user_cnt_1h', 'login', 'deviceId', 'DISTINCT_COUNT', 'userId', NULL, 'SLIDING', '1h', '5m', 1, 7200, '{"bucketHint":"5m","stateBackend":"rocksdb","expectedCardinality":"medium"}', 1, 'admin', '2026-03-08 09:08:00', 'admin', '2026-03-08 09:08:00', b'0'),
(1804, 'user_trade_cnt_5m', 'trade', 'userId', 'COUNT', NULL, NULL, 'SLIDING', '5m', '1m', 1, 3600, '{"bucketHint":"1m","stateBackend":"rocksdb"}', 1, 'admin', '2026-03-08 09:08:00', 'admin', '2026-03-08 09:08:00', b'0'),
(1805, 'user_trade_amt_sum_30m', 'trade', 'userId', 'SUM', 'amount', NULL, 'SLIDING', '30m', '5m', 1, 7200, '{"bucketHint":"5m","stateBackend":"rocksdb"}', 1, 'admin', '2026-03-08 09:08:00', 'admin', '2026-03-08 09:08:00', b'0'),
(1806, 'device_bind_user_cnt_1h', 'trade', 'deviceId', 'DISTINCT_COUNT', 'userId', NULL, 'SLIDING', '1h', '5m', 1, 7200, '{"bucketHint":"5m","stateBackend":"rocksdb","expectedCardinality":"medium"}', 1, 'admin', '2026-03-08 09:08:00', 'admin', '2026-03-08 09:08:00', b'0');

-- ----------------------------
-- Records of feature_lookup_conf
-- ----------------------------
INSERT INTO `feature_lookup_conf` (`id`, `feature_code`, `lookup_type`, `key_expr`, `source_ref`, `default_value`, `cache_ttl_seconds`, `timeout_ms`, `extra_json`, `status`, `creator`, `create_time`, `updater`, `update_time`, `deleted`) VALUES
(1811, 'device_in_blacklist', 'REDIS_SET', 'deviceId', 'LOGIN_DEVICE_BLACKLIST', 'false', 60, 20, '{"redisKeyPattern":"pulsix:list:black:device:{deviceId}","returnType":"BOOLEAN"}', 1, 'admin', '2026-03-08 09:08:30', 'admin', '2026-03-08 09:08:30', b'0'),
(1812, 'ip_risk_level', 'REDIS_STRING', 'ip', 'IP_RISK_PROFILE', 'LOW', 120, 20, '{"redisKeyPattern":"pulsix:profile:ip:risk:{ip}","returnType":"STRING"}', 1, 'admin', '2026-03-08 09:08:30', 'admin', '2026-03-08 09:08:30', b'0'),
(1813, 'user_in_white_list', 'REDIS_SET', 'userId', 'LOGIN_USER_WHITE_LIST', 'false', 60, 20, '{"redisKeyPattern":"pulsix:list:white:user:{userId}","returnType":"BOOLEAN"}', 1, 'admin', '2026-03-08 09:08:30', 'admin', '2026-03-08 09:08:30', b'0'),
(1814, 'user_risk_level', 'REDIS_STRING', 'userId', 'USER_RISK_PROFILE', 'L', 120, 20, '{"redisKeyPattern":"pulsix:profile:user:risk:{userId}","returnType":"STRING"}', 1, 'admin', '2026-03-08 09:08:30', 'admin', '2026-03-08 09:08:30', b'0');

-- ----------------------------
-- Records of feature_derived_conf
-- ----------------------------
INSERT INTO `feature_derived_conf` (`id`, `feature_code`, `engine_type`, `expr_content`, `depends_on_json`, `value_type`, `sandbox_flag`, `timeout_ms`, `extra_json`, `status`, `creator`, `create_time`, `updater`, `update_time`, `deleted`) VALUES
(1821, 'high_fail_user_flag', 'AVIATOR', 'user_login_fail_cnt_10m >= 5', '["user_login_fail_cnt_10m"]', 'BOOLEAN', 1, 50, '{"precompile":true}', 1, 'admin', '2026-03-08 09:09:00', 'admin', '2026-03-08 09:09:00', b'0'),
(1822, 'high_risk_trade_flag', 'AVIATOR', 'amount >= 3000 && user_trade_cnt_5m >= 2 && (user_risk_level == ''M'' || user_risk_level == ''H'')', '["amount","user_trade_cnt_5m","user_risk_level"]', 'BOOLEAN', 1, 50, '{"precompile":true,"note":"当前设计态是 v2 候选表达式；线上 v1 快照仍使用更高阈值"}', 1, 'admin', '2026-03-08 09:09:00', 'admin', '2026-03-08 09:09:00', b'0');

-- ----------------------------
-- Records of list_set
-- ----------------------------
INSERT INTO `list_set` (`id`, `scene_code`, `list_code`, `list_name`, `match_type`, `list_type`, `storage_type`, `sync_mode`, `sync_status`, `last_sync_time`, `status`, `description`, `creator`, `create_time`, `updater`, `update_time`, `deleted`) VALUES
(1901, 'LOGIN_RISK', 'LOGIN_DEVICE_BLACKLIST', '登录设备黑名单', 'DEVICE', 'BLACK', 'REDIS_SET', 'INCREMENTAL', 'SUCCESS', '2026-03-08 09:15:00', 1, '命中后通常直接拒绝，除非用户命中白名单', 'admin', '2026-03-08 09:10:00', 'admin', '2026-03-08 09:15:00', b'0'),
(1902, 'LOGIN_RISK', 'LOGIN_USER_WHITE_LIST', '登录用户白名单', 'USER', 'WHITE', 'REDIS_SET', 'INCREMENTAL', 'SUCCESS', '2026-03-08 09:15:00', 1, '用于保护重要用户，避免设备黑名单等规则误杀', 'admin', '2026-03-08 09:10:00', 'admin', '2026-03-08 09:15:00', b'0');

-- ----------------------------
-- Records of list_item
-- ----------------------------
INSERT INTO `list_item` (`id`, `list_code`, `match_key`, `match_value`, `expire_at`, `status`, `source_type`, `batch_no`, `remark`, `ext_json`, `creator`, `create_time`, `updater`, `update_time`, `deleted`) VALUES
(1911, 'LOGIN_DEVICE_BLACKLIST', 'deviceId', 'D9001', '2026-12-31 23:59:59', 1, 'MANUAL', 'BATCH_20260308_01', '已确认的撞库设备', '{"riskReason":"credential_stuffing","sourceSystem":"risk_ops"}', 'admin', '2026-03-08 09:11:00', 'admin', '2026-03-08 09:11:00', b'0'),
(1912, 'LOGIN_DEVICE_BLACKLIST', 'deviceId', 'D9999', NULL, 1, 'IMPORT_FILE', 'BATCH_20260308_01', '离线导入的高危设备', '{"riskReason":"bot_device","importFile":"demo-blacklist.csv"}', 'admin', '2026-03-08 09:11:00', 'admin', '2026-03-08 09:11:00', b'0'),
(1913, 'LOGIN_USER_WHITE_LIST', 'userId', 'U8888', NULL, 1, 'MANUAL', 'BATCH_20260308_02', '高价值 VIP 用户', '{"userTier":"VIP","remark":"避免误杀"}', 'admin', '2026-03-08 09:11:00', 'admin', '2026-03-08 09:11:00', b'0');
-- ----------------------------
-- Records of rule_def
-- ----------------------------
INSERT INTO `rule_def` (`id`, `scene_code`, `rule_code`, `rule_name`, `rule_type`, `engine_type`, `expr_content`, `priority`, `hit_action`, `risk_score`, `hit_reason_template`, `status`, `version`, `description`, `creator`, `create_time`, `updater`, `update_time`, `deleted`) VALUES
(2001, 'LOGIN_RISK', 'LOGIN_R001', '黑名单设备直接拒绝', 'NORMAL', 'AVIATOR', 'device_in_blacklist == true && user_in_white_list != true', 100, 'REJECT', 100, '设备命中黑名单，deviceId={deviceId}', 1, 1, '演示名单中心 + 白名单保护的组合能力', 'admin', '2026-03-08 09:12:00', 'admin', '2026-03-08 09:12:00', b'0'),
(2002, 'LOGIN_RISK', 'LOGIN_R003', '高风险 IP 爆破行为', 'NORMAL', 'AVIATOR', 'ip_login_fail_cnt_10m >= 20', 95, 'REJECT', 90, 'IP 10 分钟失败次数={ip_login_fail_cnt_10m}', 1, 1, '演示 IP 聚合特征如何直接形成阻断规则', 'admin', '2026-03-08 09:12:00', 'admin', '2026-03-08 09:12:00', b'0'),
(2003, 'LOGIN_RISK', 'LOGIN_R002', '短时密码爆破复核', 'NORMAL', 'AVIATOR', 'high_fail_user_flag == true && ip_risk_level == ''HIGH''', 90, 'REVIEW', 60, '用户 10 分钟失败次数过高且 IP 风险等级={ip_risk_level}', 1, 1, '演示派生特征 + lookup 特征组合规则', 'admin', '2026-03-08 09:12:00', 'admin', '2026-03-08 09:12:00', b'0'),
(2004, 'LOGIN_RISK', 'LOGIN_R004', '设备多账号登录异常', 'NORMAL', 'AVIATOR', 'device_login_user_cnt_1h >= 5', 80, 'REVIEW', 40, '设备 1 小时关联登录用户数={device_login_user_cnt_1h}', 1, 1, '演示 DISTINCT_COUNT 场景', 'admin', '2026-03-08 09:12:00', 'admin', '2026-03-08 09:12:00', b'0'),
(2011, 'TRADE_RISK', 'TRADE_R001', '高风险交易候选命中', 'NORMAL', 'AVIATOR', 'high_risk_trade_flag == true', 100, 'TAG_ONLY', 40, '高风险交易标记命中，金额={amount}，5 分钟交易次数={user_trade_cnt_5m}', 1, 1, 'SCORE_CARD 中的第一条打分规则，当前由高风险交易派生特征驱动', 'admin', '2026-03-08 09:12:00', 'admin', '2026-03-08 09:12:00', b'0'),
(2012, 'TRADE_RISK', 'TRADE_R002', '设备多用户交易风险', 'NORMAL', 'AVIATOR', 'device_bind_user_cnt_1h >= 4', 90, 'TAG_ONLY', 35, '设备 1 小时关联用户数={device_bind_user_cnt_1h}', 1, 1, '通过设备多账号交易识别团伙或养号行为', 'admin', '2026-03-08 09:12:00', 'admin', '2026-03-08 09:12:00', b'0'),
(2013, 'TRADE_RISK', 'TRADE_R003', '高风险用户画像命中', 'NORMAL', 'AVIATOR', 'user_risk_level == ''H''', 80, 'TAG_ONLY', 25, '用户风险等级={user_risk_level}', 1, 1, '从画像系统读取用户风险等级并参与打分', 'admin', '2026-03-08 09:12:00', 'admin', '2026-03-08 09:12:00', b'0');

-- ----------------------------
-- Records of policy_def
-- ----------------------------
INSERT INTO `policy_def` (`id`, `scene_code`, `policy_code`, `policy_name`, `decision_mode`, `default_action`, `score_calc_mode`, `status`, `version`, `description`, `creator`, `create_time`, `updater`, `update_time`, `deleted`) VALUES
(2101, 'LOGIN_RISK', 'LOGIN_RISK_POLICY', '登录风控主策略', 'FIRST_HIT', 'PASS', 'NONE', 1, 1, '按优先级执行规则，命中首条阻断或复核规则后直接返回', 'admin', '2026-03-08 09:13:00', 'admin', '2026-03-08 09:13:00', b'0'),
(2102, 'TRADE_RISK', 'TRADE_RISK_SCORECARD', '交易风控评分卡策略', 'SCORE_CARD', 'PASS', 'SUM_HIT_SCORE', 1, 1, '累计命中规则分值，再通过 policy_score_band 映射最终动作', 'admin', '2026-03-08 09:13:00', 'admin', '2026-03-08 09:13:00', b'0');

-- ----------------------------
-- Records of policy_rule_ref
-- ----------------------------
INSERT INTO `policy_rule_ref` (`id`, `policy_code`, `rule_code`, `order_no`, `enabled_flag`, `branch_expr`, `score_weight`, `stop_on_hit`, `creator`, `create_time`, `updater`, `update_time`, `deleted`) VALUES
(2111, 'LOGIN_RISK_POLICY', 'LOGIN_R001', 1, 1, NULL, NULL, 1, 'admin', '2026-03-08 09:13:30', 'admin', '2026-03-08 09:13:30', b'0'),
(2112, 'LOGIN_RISK_POLICY', 'LOGIN_R003', 2, 1, NULL, NULL, 1, 'admin', '2026-03-08 09:13:30', 'admin', '2026-03-08 09:13:30', b'0'),
(2113, 'LOGIN_RISK_POLICY', 'LOGIN_R002', 3, 1, NULL, NULL, 1, 'admin', '2026-03-08 09:13:30', 'admin', '2026-03-08 09:13:30', b'0'),
(2114, 'LOGIN_RISK_POLICY', 'LOGIN_R004', 4, 1, NULL, NULL, 1, 'admin', '2026-03-08 09:13:30', 'admin', '2026-03-08 09:13:30', b'0'),
(2115, 'TRADE_RISK_SCORECARD', 'TRADE_R001', 1, 1, NULL, 1, 0, 'admin', '2026-03-08 09:13:30', 'admin', '2026-03-08 09:13:30', b'0'),
(2116, 'TRADE_RISK_SCORECARD', 'TRADE_R002', 2, 1, NULL, 1, 0, 'admin', '2026-03-08 09:13:30', 'admin', '2026-03-08 09:13:30', b'0'),
(2117, 'TRADE_RISK_SCORECARD', 'TRADE_R003', 3, 1, NULL, 1, 0, 'admin', '2026-03-08 09:13:30', 'admin', '2026-03-08 09:13:30', b'0');

-- ----------------------------
-- Records of policy_score_band
-- ----------------------------
INSERT INTO `policy_score_band` (`id`, `policy_code`, `band_no`, `min_score`, `max_score`, `hit_action`, `hit_reason_template`, `enabled_flag`, `creator`, `create_time`, `updater`, `update_time`, `deleted`) VALUES
(2121, 'TRADE_RISK_SCORECARD', 1, 0, 49, 'PASS', '累计分值={totalScore}，低于复核阈值，直接放行', 1, 'admin', '2026-03-08 09:14:00', 'admin', '2026-03-08 09:14:00', b'0'),
(2122, 'TRADE_RISK_SCORECARD', 2, 50, 79, 'REVIEW', '累计分值={totalScore}，进入人工复核区间', 1, 'admin', '2026-03-08 09:14:00', 'admin', '2026-03-08 09:14:00', b'0'),
(2123, 'TRADE_RISK_SCORECARD', 3, 80, 999999, 'REJECT', '累计分值={totalScore}，超过拒绝阈值', 1, 'admin', '2026-03-08 09:14:00', 'admin', '2026-03-08 09:14:00', b'0');

-- ----------------------------
-- Records of scene_release
-- ----------------------------
INSERT INTO `scene_release` (`id`, `scene_code`, `version_no`, `snapshot_json`, `checksum`, `publish_status`, `validation_status`, `validation_report_json`, `dependency_digest_json`, `compile_duration_ms`, `compiled_feature_count`, `compiled_rule_count`, `compiled_policy_count`, `published_by`, `published_at`, `effective_from`, `rollback_from_version`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`) VALUES
(2201, 'LOGIN_RISK', 1, '{"sceneCode":"LOGIN_RISK","versionNo":1,"eventCode":"login","features":{"stream":["user_login_fail_cnt_10m","ip_login_fail_cnt_10m","device_login_user_cnt_1h"],"lookup":["device_in_blacklist","ip_risk_level","user_in_white_list"],"derived":["high_fail_user_flag"]},"policy":{"policyCode":"LOGIN_RISK_POLICY","decisionMode":"FIRST_HIT","defaultAction":"PASS","ruleOrder":["LOGIN_R001","LOGIN_R003","LOGIN_R002","LOGIN_R004"]}}', 'sha256-login-v1-demo', 'ACTIVE', 'PASSED', '{"checks":[{"type":"FIELD","result":"PASS","message":"事件字段定义完整"},{"type":"DEPENDENCY","result":"PASS","message":"规则依赖特征均可解析"},{"type":"EXPR","result":"PASS","message":"表达式预编译通过"}],"warnings":[]}', '{"fields":["userId","deviceId","ip","loginResult"],"features":["user_login_fail_cnt_10m","ip_login_fail_cnt_10m","device_login_user_cnt_1h","device_in_blacklist","ip_risk_level","user_in_white_list","high_fail_user_flag"],"lists":["LOGIN_DEVICE_BLACKLIST","LOGIN_USER_WHITE_LIST"],"rules":["LOGIN_R001","LOGIN_R002","LOGIN_R003","LOGIN_R004"]}', 128, 7, 4, 1, 'admin', '2026-03-08 09:20:00', '2026-03-08 09:20:00', NULL, '登录风控首版正式上线', 'admin', '2026-03-08 09:20:00', 'admin', '2026-03-08 09:20:00', b'0'),
(2202, 'TRADE_RISK', 1, '{"sceneCode":"TRADE_RISK","versionNo":1,"eventCode":"trade","features":{"stream":["user_trade_cnt_5m","user_trade_amt_sum_30m","device_bind_user_cnt_1h"],"lookup":["user_risk_level"],"derived":[{"code":"high_risk_trade_flag","expr":"amount >= 5000 && user_trade_cnt_5m >= 3 && (user_risk_level == ''M'' || user_risk_level == ''H'')"}]},"policy":{"policyCode":"TRADE_RISK_SCORECARD","decisionMode":"SCORE_CARD","defaultAction":"PASS","scoreBands":[{"min":0,"max":49,"action":"PASS"},{"min":50,"max":79,"action":"REVIEW"},{"min":80,"max":999999,"action":"REJECT"}],"ruleOrder":["TRADE_R001","TRADE_R002","TRADE_R003"]}}', 'sha256-trade-v1-demo', 'ACTIVE', 'PASSED', '{"checks":[{"type":"FIELD","result":"PASS","message":"交易事件字段完整"},{"type":"DEPENDENCY","result":"PASS","message":"策略引用规则和分段完整"}],"warnings":[]}', '{"fields":["userId","deviceId","amount","merchantId"],"features":["user_trade_cnt_5m","user_trade_amt_sum_30m","device_bind_user_cnt_1h","user_risk_level","high_risk_trade_flag"],"rules":["TRADE_R001","TRADE_R002","TRADE_R003"]}', 166, 5, 3, 1, 'admin', '2026-03-08 09:25:00', '2026-03-08 09:25:00', NULL, '交易风控评分卡 v1 上线，派生特征阈值较保守', 'admin', '2026-03-08 09:25:00', 'admin', '2026-03-08 09:25:00', b'0'),
(2203, 'TRADE_RISK', 2, '{"sceneCode":"TRADE_RISK","versionNo":2,"eventCode":"trade","features":{"stream":["user_trade_cnt_5m","user_trade_amt_sum_30m","device_bind_user_cnt_1h"],"lookup":["user_risk_level"],"derived":[{"code":"high_risk_trade_flag","expr":"amount >= 3000 && user_trade_cnt_5m >= 2 && (user_risk_level == ''M'' || user_risk_level == ''H'')"}]},"policy":{"policyCode":"TRADE_RISK_SCORECARD","decisionMode":"SCORE_CARD","defaultAction":"PASS","scoreBands":[{"min":0,"max":49,"action":"PASS"},{"min":50,"max":79,"action":"REVIEW"},{"min":80,"max":999999,"action":"REJECT"}],"ruleOrder":["TRADE_R001","TRADE_R002","TRADE_R003"]}}', 'sha256-trade-v2-demo', 'PUBLISHED', 'PASSED', '{"checks":[{"type":"FIELD","result":"PASS","message":"交易事件字段完整"},{"type":"DEPENDENCY","result":"PASS","message":"依赖关系稳定"},{"type":"DIFF","result":"PASS","message":"仅调整高风险交易阈值，不涉及结构变更"}],"warnings":["预计 REVIEW 比例会上升，请先回放验证"]}', '{"changedFeatures":["high_risk_trade_flag"],"changedRules":["TRADE_R001"],"impactFields":["amount","user_trade_cnt_5m","user_risk_level"]}', 174, 5, 3, 1, 'admin', '2026-03-08 09:40:00', NULL, NULL, '候选 v2：降低高风险交易识别阈值，待回放后再决定是否激活', 'admin', '2026-03-08 09:40:00', 'admin', '2026-03-08 09:40:00', b'0');

-- ----------------------------
-- Records of simulation_case
-- ----------------------------
INSERT INTO `simulation_case` (`id`, `scene_code`, `case_code`, `case_name`, `version_select_mode`, `version_no`, `input_event_json`, `mock_feature_json`, `mock_lookup_json`, `expected_action`, `expected_hit_rules`, `status`, `description`, `creator`, `create_time`, `updater`, `update_time`, `deleted`) VALUES
(2301, 'LOGIN_RISK', 'SIM_LOGIN_PASS', '正常登录放行', 'FIXED', 1, '{"eventId":"E_LOGIN_0002","traceId":"T_LOGIN_0002","sceneCode":"LOGIN_RISK","eventType":"login","eventTime":"2026-03-07T09:10:00","userId":"U2001","deviceId":"D2001","ip":"1.1.1.1","channel":"APP","loginResult":"SUCCESS"}', '{"user_login_fail_cnt_10m":0,"ip_login_fail_cnt_10m":0,"device_login_user_cnt_1h":1}', '{"device_in_blacklist":false,"ip_risk_level":"LOW","user_in_white_list":false}', 'PASS', '[]', 1, '用于验证正常用户在首版登录策略下不会被误杀', 'admin', '2026-03-08 09:30:00', 'admin', '2026-03-08 09:30:00', b'0'),
(2302, 'LOGIN_RISK', 'SIM_LOGIN_REVIEW', '短时密码爆破复核', 'FIXED', 1, '{"eventId":"E_LOGIN_0003","traceId":"T_LOGIN_0003","sceneCode":"LOGIN_RISK","eventType":"login","eventTime":"2026-03-07T09:20:00","userId":"U1001","deviceId":"D9002","ip":"10.20.30.40","channel":"APP","loginResult":"FAIL"}', '{"user_login_fail_cnt_10m":6,"ip_login_fail_cnt_10m":8,"device_login_user_cnt_1h":1}', '{"device_in_blacklist":false,"ip_risk_level":"HIGH","user_in_white_list":false}', 'REVIEW', '["LOGIN_R002"]', 1, '对应功能清单中的仿真测试能力：输入模拟事件并展示特征、命中链路、最终动作', 'admin', '2026-03-08 09:30:00', 'admin', '2026-03-08 09:30:00', b'0'),
(2303, 'TRADE_RISK', 'SIM_TRADE_REJECT', '高风险交易评分卡拒绝', 'FIXED', 1, '{"eventId":"E_TRADE_0001","traceId":"T_TRADE_0001","sceneCode":"TRADE_RISK","eventType":"trade","eventTime":"2026-03-07T11:00:00","userId":"U5001","deviceId":"D5001","ip":"66.77.88.99","amount":6800,"merchantId":"M1001","payMethod":"CARD"}', '{"user_trade_cnt_5m":4,"user_trade_amt_sum_30m":12800,"device_bind_user_cnt_1h":5}', '{"user_risk_level":"H"}', 'REJECT', '["TRADE_R001","TRADE_R002","TRADE_R003"]', 1, '用于演示 SCORE_CARD 通过多条打分规则累计后进入拒绝分段', 'admin', '2026-03-08 09:30:00', 'admin', '2026-03-08 09:30:00', b'0');

-- ----------------------------
-- Records of simulation_report
-- ----------------------------
INSERT INTO `simulation_report` (`id`, `case_id`, `scene_code`, `version_no`, `trace_id`, `result_json`, `pass_flag`, `duration_ms`, `creator`, `create_time`, `updater`, `update_time`, `deleted`) VALUES
(2311, 2301, 'LOGIN_RISK', 1, 'SIM_TRACE_0001', '{"finalAction":"PASS","hitRules":[],"featureSnapshot":{"user_login_fail_cnt_10m":0,"ip_login_fail_cnt_10m":0,"device_login_user_cnt_1h":1},"score":0}', 1, 11, 'admin', '2026-03-08 09:31:00', 'admin', '2026-03-08 09:31:00', b'0'),
(2312, 2302, 'LOGIN_RISK', 1, 'SIM_TRACE_0002', '{"finalAction":"REVIEW","hitRules":["LOGIN_R002"],"featureSnapshot":{"user_login_fail_cnt_10m":6,"ip_risk_level":"HIGH"},"score":60}', 1, 14, 'admin', '2026-03-08 09:31:00', 'admin', '2026-03-08 09:31:00', b'0'),
(2313, 2303, 'TRADE_RISK', 1, 'SIM_TRACE_0003', '{"finalAction":"REJECT","hitRules":["TRADE_R001","TRADE_R002","TRADE_R003"],"totalScore":100,"featureSnapshot":{"user_trade_cnt_5m":4,"device_bind_user_cnt_1h":5,"user_risk_level":"H"}}', 1, 19, 'admin', '2026-03-08 09:31:00', 'admin', '2026-03-08 09:31:00', b'0');

-- ----------------------------
-- Records of decision_log
-- ----------------------------
INSERT INTO `decision_log` (`id`, `trace_id`, `event_id`, `scene_code`, `source_code`, `event_code`, `entity_id`, `policy_code`, `final_action`, `final_score`, `version_no`, `latency_ms`, `event_time`, `input_json`, `feature_snapshot_json`, `hit_rules_json`, `decision_detail_json`, `creator`, `create_time`, `updater`, `update_time`, `deleted`) VALUES
(2401, 'T_LOGIN_9001', 'E_LOGIN_9001', 'LOGIN_RISK', 'APP_BEACON_WEB', 'login', 'U1001', 'LOGIN_RISK_POLICY', 'REVIEW', 60, 1, 18, '2026-03-07 09:20:00', '{"eventId":"E_LOGIN_9001","traceId":"T_LOGIN_9001","sceneCode":"LOGIN_RISK","eventType":"login","eventTime":"2026-03-07T09:20:00","userId":"U1001","deviceId":"D9002","ip":"10.20.30.40","channel":"APP","loginResult":"FAIL"}', '{"user_login_fail_cnt_10m":6,"ip_login_fail_cnt_10m":8,"device_login_user_cnt_1h":1,"device_in_blacklist":false,"ip_risk_level":"HIGH","user_in_white_list":false}', '["LOGIN_R002"]', '{"decisionMode":"FIRST_HIT","versionNo":1,"stopRule":"LOGIN_R002"}', 'flink-risk-engine', '2026-03-08 09:32:00', 'flink-risk-engine', '2026-03-08 09:32:00', b'0'),
(2402, 'T_TRADE_9001', 'E_TRADE_9001', 'TRADE_RISK', 'PAY_CORE_SDK', 'trade', 'U5001', 'TRADE_RISK_SCORECARD', 'REJECT', 100, 1, 26, '2026-03-07 11:00:00', '{"eventId":"E_TRADE_9001","traceId":"T_TRADE_9001","sceneCode":"TRADE_RISK","eventType":"trade","eventTime":"2026-03-07T11:00:00","userId":"U5001","deviceId":"D5001","ip":"66.77.88.99","amount":6800,"merchantId":"M1001","payMethod":"CARD"}', '{"user_trade_cnt_5m":4,"user_trade_amt_sum_30m":12800,"device_bind_user_cnt_1h":5,"user_risk_level":"H","high_risk_trade_flag":true}', '["TRADE_R001","TRADE_R002","TRADE_R003"]', '{"decisionMode":"SCORE_CARD","versionNo":1,"totalScore":100,"matchedBand":{"min":80,"max":999999,"action":"REJECT"}}', 'flink-risk-engine', '2026-03-08 09:32:00', 'flink-risk-engine', '2026-03-08 09:32:00', b'0');

-- ----------------------------
-- Records of rule_hit_log
-- ----------------------------
INSERT INTO `rule_hit_log` (`id`, `decision_id`, `rule_code`, `rule_name`, `rule_order_no`, `hit_flag`, `hit_reason`, `score`, `hit_value_json`, `creator`, `create_time`, `updater`, `update_time`, `deleted`) VALUES
(2411, 2401, 'LOGIN_R002', '短时密码爆破复核', 3, 1, '用户 10 分钟失败次数过高且 IP 风险等级=HIGH', 60, '{"user_login_fail_cnt_10m":6,"ip_risk_level":"HIGH"}', 'flink-risk-engine', '2026-03-08 09:32:30', 'flink-risk-engine', '2026-03-08 09:32:30', b'0'),
(2412, 2402, 'TRADE_R001', '高风险交易候选命中', 1, 1, '高风险交易标记命中，金额=6800，5 分钟交易次数=4', 40, '{"amount":6800,"user_trade_cnt_5m":4,"user_risk_level":"H","high_risk_trade_flag":true}', 'flink-risk-engine', '2026-03-08 09:32:30', 'flink-risk-engine', '2026-03-08 09:32:30', b'0'),
(2413, 2402, 'TRADE_R002', '设备多用户交易风险', 2, 1, '设备 1 小时关联用户数=5', 35, '{"device_bind_user_cnt_1h":5}', 'flink-risk-engine', '2026-03-08 09:32:30', 'flink-risk-engine', '2026-03-08 09:32:30', b'0'),
(2414, 2402, 'TRADE_R003', '高风险用户画像命中', 3, 1, '用户风险等级=H', 25, '{"user_risk_level":"H"}', 'flink-risk-engine', '2026-03-08 09:32:30', 'flink-risk-engine', '2026-03-08 09:32:30', b'0');

-- ----------------------------
-- Records of ingest_error_log
-- ----------------------------
INSERT INTO `ingest_error_log` (`id`, `trace_id`, `source_code`, `scene_code`, `event_code`, `raw_event_id`, `ingest_stage`, `error_code`, `error_message`, `raw_payload_json`, `standard_payload_json`, `error_topic_name`, `reprocess_status`, `occur_time`, `status`, `creator`, `create_time`, `updater`, `update_time`, `deleted`) VALUES
(2501, 'T_LOGIN_ERR_0001', 'APP_BEACON_WEB', 'LOGIN_RISK', 'login', 'raw_login_bad_0001', 'VALIDATE', 'REQUIRED_FIELD_MISSING', 'eventTime 缺失，且接入层无法从 ts_ms 中解析出合法时间', '{"event_id":"raw_login_bad_0001","trace_id":"T_LOGIN_ERR_0001","uid":"U1002","did":"D1002","clientIp":"10.10.10.10","result":"FAIL"}', '{"eventId":"raw_login_bad_0001","traceId":"T_LOGIN_ERR_0001","sceneCode":"LOGIN_RISK","eventType":"login","userId":"U1002","deviceId":"D1002","ip":"10.10.10.10","loginResult":"FAIL"}', 'pulsix.event.dlq', 'PENDING', '2026-03-08 09:35:00', 1, 'ingest-gateway', '2026-03-08 09:35:00', 'ingest-gateway', '2026-03-08 09:35:00', b'0'),
(2502, 'T_TRADE_ERR_0001', 'PAY_CORE_SDK', 'TRADE_RISK', 'trade', 'raw_trade_bad_0001', 'NORMALIZE', 'AMOUNT_NEGATIVE', 'amountFen 转换后得到负数金额，疑似上游报文异常', '{"req":{"traceId":"T_TRADE_ERR_0001","occurTime":1772842800000},"biz":{"tradeId":"raw_trade_bad_0001","uid":"U5002","deviceNo":"D5002","ip":"66.77.88.10","amountFen":-1000,"merchantNo":"M2001","payMethod":"CARD"}}', '{"eventId":"raw_trade_bad_0001","traceId":"T_TRADE_ERR_0001","sceneCode":"TRADE_RISK","eventType":"trade","eventTime":"2026-03-07T11:00:00","userId":"U5002","deviceId":"D5002","ip":"66.77.88.10","amount":-10.00,"merchantId":"M2001","payMethod":"CARD"}', 'pulsix.event.dlq', 'IGNORED', '2026-03-08 09:36:00', 1, 'ingest-gateway', '2026-03-08 09:36:00', 'ingest-gateway', '2026-03-08 09:36:00', b'0');

-- ----------------------------
-- Records of risk_audit_log
-- ----------------------------
INSERT INTO `risk_audit_log` (`id`, `trace_id`, `scene_code`, `operator_id`, `operator_name`, `biz_type`, `biz_code`, `action_type`, `before_json`, `after_json`, `remark`, `operate_time`, `creator`, `create_time`, `updater`, `update_time`, `deleted`) VALUES
(2601, 'T_AUDIT_0001', 'TRADE_RISK', 1, 'admin', 'FEATURE', 'high_risk_trade_flag', 'UPDATE', '{"expr":"amount >= 5000 && user_trade_cnt_5m >= 3 && (user_risk_level == ''M'' || user_risk_level == ''H'')","version":1}', '{"expr":"amount >= 3000 && user_trade_cnt_5m >= 2 && (user_risk_level == ''M'' || user_risk_level == ''H'')","version":2}', '为了提升召回率，准备先通过回放验证阈值下调后的影响范围', '2026-03-08 09:38:00', 'admin', '2026-03-08 09:38:00', 'admin', '2026-03-08 09:38:00', b'0'),
(2602, 'T_AUDIT_0002', 'TRADE_RISK', 1, 'admin', 'RELEASE', 'TRADE_RISK#v2', 'PUBLISH', '{"publishStatus":"ACTIVE","versionNo":1}', '{"publishStatus":"PUBLISHED","versionNo":2}', '生成候选版本 v2，先做回放对比，不直接激活', '2026-03-08 09:40:00', 'admin', '2026-03-08 09:40:00', 'admin', '2026-03-08 09:40:00', b'0');

-- ----------------------------
-- Records of replay_job
-- ----------------------------
INSERT INTO `replay_job` (`id`, `job_code`, `scene_code`, `baseline_version_no`, `target_version_no`, `input_source_type`, `input_ref`, `job_status`, `event_total_count`, `diff_event_count`, `summary_json`, `sample_diff_json`, `started_at`, `finished_at`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`) VALUES
(2701, 'REPLAY_TRADE_V1_V2_20260308', 'TRADE_RISK', 1, 2, 'FILE', '/demo/replay/trade-risk-20260307.jsonl', 'SUCCESS', 500, 37, '{"oldPassRate":0.8120,"newPassRate":0.7610,"oldReviewRate":0.1510,"newReviewRate":0.2020,"oldRejectRate":0.0370,"newRejectRate":0.0370,"topChangedRule":"TRADE_R001"}', '[{"eventId":"E_TRADE_DIFF_0001","oldAction":"PASS","newAction":"REVIEW","reason":"高风险交易阈值从 5000/3 调整为 3000/2，新增命中 TRADE_R001"}]', '2026-03-08 10:00:00', '2026-03-08 10:05:00', '用于评估交易候选 v2 的上线影响，优先观察 REVIEW 比例变化', 'admin', '2026-03-08 10:00:00', 'admin', '2026-03-08 10:05:00', b'0');

-- ----------------------------
-- Records of risk_metric_snapshot
-- ----------------------------
INSERT INTO `risk_metric_snapshot` (`id`, `stat_time`, `stat_granularity`, `scene_code`, `metric_domain`, `metric_code`, `metric_name`, `metric_value`, `metric_unit`, `metric_tags_json`, `creator`, `create_time`, `updater`, `update_time`, `deleted`) VALUES
(2801, '2026-03-08 10:00:00', '1m', 'LOGIN_RISK', 'INGEST', 'EVENT_IN_TOTAL', '接入成功事件数', 1280.0000, 'count', '{"sourceCode":"APP_BEACON_WEB"}', 'metrics-job', '2026-03-08 10:01:00', 'metrics-job', '2026-03-08 10:01:00', b'0'),
(2802, '2026-03-08 10:00:00', '1m', 'LOGIN_RISK', 'INGEST', 'DLQ_TOTAL', 'DLQ 事件数', 3.0000, 'count', '{"sourceCode":"APP_BEACON_WEB"}', 'metrics-job', '2026-03-08 10:01:00', 'metrics-job', '2026-03-08 10:01:00', b'0'),
(2803, '2026-03-08 10:00:00', '1m', 'LOGIN_RISK', 'DECISION', 'REVIEW_RATIO', '复核占比', 0.0840, 'ratio', '{"policyCode":"LOGIN_RISK_POLICY"}', 'metrics-job', '2026-03-08 10:01:00', 'metrics-job', '2026-03-08 10:01:00', b'0'),
(2804, '2026-03-08 10:00:00', '1m', 'TRADE_RISK', 'DECISION', 'REJECT_RATIO', '拒绝占比', 0.0370, 'ratio', '{"policyCode":"TRADE_RISK_SCORECARD"}', 'metrics-job', '2026-03-08 10:01:00', 'metrics-job', '2026-03-08 10:01:00', b'0'),
(2805, '2026-03-08 10:00:00', '1m', 'TRADE_RISK', 'ENGINE', 'P95_LATENCY_MS', 'P95 决策耗时', 46.0000, 'ms', '{"engine":"flink-risk-engine"}', 'metrics-job', '2026-03-08 10:01:00', 'metrics-job', '2026-03-08 10:01:00', b'0'),
(2806, '2026-03-08 10:00:00', '1m', NULL, 'KAFKA', 'STANDARD_TOPIC_LAG', '标准事件 Topic Lag', 12.0000, 'messages', '{"topic":"pulsix.event.standard"}', 'metrics-job', '2026-03-08 10:01:00', 'metrics-job', '2026-03-08 10:01:00', b'0');

SET FOREIGN_KEY_CHECKS = 1;
