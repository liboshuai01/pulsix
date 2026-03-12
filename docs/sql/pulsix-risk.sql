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
 6. 相比附录 D 的第一版 DDL，本文件额外补齐了若干子表的 scene_code 维度，避免跨场景复用 code 时出现配置冲突或发布歧义。
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
  `scene_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '' COMMENT '所属场景编码，用于保证场景内策略分段独立演进',
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
  UNIQUE INDEX `uk_scene_policy_band`(`scene_code` ASC, `policy_code` ASC, `band_no` ASC) USING BTREE,
  INDEX `idx_scene_policy_score`(`scene_code` ASC, `policy_code` ASC, `min_score` ASC, `max_score` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '策略分值分段表（P1-推荐），用于 SCORE_CARD 策略把累计分值映射为最终动作' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for policy_rule_ref
-- ----------------------------
DROP TABLE IF EXISTS `policy_rule_ref`;
CREATE TABLE `policy_rule_ref`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '策略规则关联主键',
  `scene_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '' COMMENT '所属场景编码，用于保证场景内策略与规则关联关系独立',
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
  UNIQUE INDEX `uk_scene_policy_rule`(`scene_code` ASC, `policy_code` ASC, `rule_code` ASC) USING BTREE,
  INDEX `idx_scene_policy_order`(`scene_code` ASC, `policy_code` ASC, `order_no` ASC) USING BTREE
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
  `scene_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '' COMMENT '所属场景编码，用于按场景隔离名单条目',
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
  UNIQUE INDEX `uk_scene_list_value`(`scene_code` ASC, `list_code` ASC, `match_value` ASC) USING BTREE,
  INDEX `idx_scene_list_code`(`scene_code` ASC, `list_code` ASC) USING BTREE,
  INDEX `idx_scene_match_value`(`scene_code` ASC, `match_value` ASC) USING BTREE
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
  `scene_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '' COMMENT '所属场景编码，用于保证派生特征配置按场景归档',
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
  UNIQUE INDEX `uk_scene_feature_derived`(`scene_code` ASC, `feature_code` ASC) USING BTREE,
  INDEX `idx_scene_derived_status`(`scene_code` ASC, `status` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '派生特征配置表（P0-必须），用于表达式/Groovy 驱动的二次推导特征' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for feature_lookup_conf
-- ----------------------------
DROP TABLE IF EXISTS `feature_lookup_conf`;
CREATE TABLE `feature_lookup_conf`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '查询特征配置主键',
  `scene_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '' COMMENT '所属场景编码，用于保证 lookup 特征配置按场景隔离',
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
  UNIQUE INDEX `uk_scene_feature_lookup`(`scene_code` ASC, `feature_code` ASC) USING BTREE,
  INDEX `idx_scene_lookup_status`(`scene_code` ASC, `status` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '查询特征配置表（P0-必须），用于 Redis/字典/名单类 lookup 特征定义' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for feature_stream_conf
-- ----------------------------
DROP TABLE IF EXISTS `feature_stream_conf`;
CREATE TABLE `feature_stream_conf`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '流式特征配置主键',
  `scene_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '' COMMENT '所属场景编码，用于保证流式特征配置按场景隔离',
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
  UNIQUE INDEX `uk_scene_feature_stream`(`scene_code` ASC, `feature_code` ASC) USING BTREE,
  INDEX `idx_scene_stream_status`(`scene_code` ASC, `status` ASC) USING BTREE
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
  UNIQUE INDEX `uk_source_scene_event_target`(`source_code` ASC, `scene_code` ASC, `event_code` ASC, `target_field_code` ASC) USING BTREE,
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
  `scene_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '' COMMENT '所属场景编码，用于保证事件字段定义按场景归档',
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
  UNIQUE INDEX `uk_scene_event_field`(`scene_code` ASC, `event_code` ASC, `field_code` ASC) USING BTREE,
  INDEX `idx_scene_event_sort`(`scene_code` ASC, `event_code` ASC, `sort_no` ASC) USING BTREE
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
  `status` tinyint NOT NULL DEFAULT 0 COMMENT '状态：0-启用，1-停用',
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
-- 1. 当前阶段保留 `scene_release` 的运行时快照样例，用于 `pulsix-kernel + pulsix-engine` 的最小闭环联调。
-- 2. `S00` 额外补齐风控菜单骨架、子菜单入口与按钮权限样例，支撑管理端页面占位接入。
-- 3. 其他控制面、审计、回放、结果类表暂不提供示例数据，待 `pulsix-module-risk` 开发时再补齐。
-- ============================================================
-- ----------------------------
-- Records of scene_def
-- ----------------------------
DELETE FROM `scene_def` WHERE `scene_code` = 'TRADE_RISK';
INSERT INTO `scene_def` (`id`, `scene_code`, `scene_name`, `scene_type`, `access_mode`, `default_event_code`, `default_policy_code`, `standard_topic_name`, `decision_topic_name`, `status`, `description`, `creator`, `create_time`, `updater`, `update_time`, `deleted`) VALUES
(2101, 'TRADE_RISK', '交易风控', 'TRADE_SECURITY', 'MIXED', 'TRADE_EVENT', 'TRADE_RISK_POLICY', 'pulsix.event.standard', 'pulsix.decision.result', 0, 'S01 初始化样例场景，对齐 TRADE_RISK 主演示链路。', 'admin', '2026-03-12 00:00:00', 'admin', '2026-03-12 00:00:00', b'0');

-- ----------------------------
-- Records of event_schema
-- ----------------------------
DELETE FROM `event_schema` WHERE `scene_code` = 'TRADE_RISK' AND `event_code` = 'TRADE_EVENT';
INSERT INTO `event_schema` (`id`, `scene_code`, `event_code`, `event_name`, `event_type`, `source_type`, `raw_topic_name`, `standard_topic_name`, `version`, `status`, `description`, `creator`, `create_time`, `updater`, `update_time`, `deleted`) VALUES
(2111, 'TRADE_RISK', 'TRADE_EVENT', '交易标准事件', 'BUSINESS', 'MIXED', 'pulsix.event.raw.trade', 'pulsix.event.standard', 1, 1, 'S02 初始化样例事件 Schema，对齐 TRADE_RISK -> TRADE_EVENT 演示链路。', 'admin', '2026-03-12 00:00:00', 'admin', '2026-03-12 00:00:00', b'0');

-- ----------------------------
-- Records of event_field_def
-- ----------------------------
DELETE FROM `event_field_def` WHERE `scene_code` = 'TRADE_RISK' AND `event_code` = 'TRADE_EVENT';
INSERT INTO `event_field_def` (`id`, `scene_code`, `event_code`, `field_code`, `field_name`, `field_type`, `field_path`, `standard_field_flag`, `required_flag`, `nullable_flag`, `default_value`, `sample_value`, `validation_rule_json`, `description`, `sort_no`, `status`, `creator`, `create_time`, `updater`, `update_time`, `deleted`) VALUES
(2121, 'TRADE_RISK', 'TRADE_EVENT', 'eventId', '事件编号', 'STRING', '$.eventId', 1, 1, 0, NULL, 'E_TRADE_9001', NULL, '交易事件唯一编号', 10, 1, 'admin', '2026-03-12 00:00:00', 'admin', '2026-03-12 00:00:00', b'0'),
(2122, 'TRADE_RISK', 'TRADE_EVENT', 'sceneCode', '场景编码', 'STRING', '$.sceneCode', 1, 1, 0, 'TRADE_RISK', 'TRADE_RISK', NULL, '所属风控场景编码', 20, 1, 'admin', '2026-03-12 00:00:00', 'admin', '2026-03-12 00:00:00', b'0'),
(2123, 'TRADE_RISK', 'TRADE_EVENT', 'eventType', '事件类型', 'STRING', '$.eventType', 1, 1, 0, 'trade', 'trade', NULL, '标准事件类型标识', 30, 1, 'admin', '2026-03-12 00:00:00', 'admin', '2026-03-12 00:00:00', b'0'),
(2124, 'TRADE_RISK', 'TRADE_EVENT', 'eventTime', '事件时间', 'DATETIME', '$.eventTime', 1, 1, 0, NULL, '2026-03-07T11:00:00', NULL, '事件发生时间', 40, 1, 'admin', '2026-03-12 00:00:00', 'admin', '2026-03-12 00:00:00', b'0'),
(2125, 'TRADE_RISK', 'TRADE_EVENT', 'traceId', '链路编号', 'STRING', '$.traceId', 1, 0, 1, NULL, 'T_TRADE_9001', NULL, '用于串联接入到决策的全链路编号', 50, 1, 'admin', '2026-03-12 00:00:00', 'admin', '2026-03-12 00:00:00', b'0'),
(2126, 'TRADE_RISK', 'TRADE_EVENT', 'userId', '用户编号', 'STRING', '$.userId', 1, 1, 0, NULL, 'U5001', NULL, '交易归属的用户编号', 60, 1, 'admin', '2026-03-12 00:00:00', 'admin', '2026-03-12 00:00:00', b'0'),
(2127, 'TRADE_RISK', 'TRADE_EVENT', 'deviceId', '设备编号', 'STRING', '$.deviceId', 1, 1, 0, NULL, 'D5001', NULL, '发起交易的设备编号', 70, 1, 'admin', '2026-03-12 00:00:00', 'admin', '2026-03-12 00:00:00', b'0'),
(2128, 'TRADE_RISK', 'TRADE_EVENT', 'ip', 'IP 地址', 'STRING', '$.ip', 1, 1, 0, NULL, '66.77.88.99', NULL, '发起交易时的网络 IP', 80, 1, 'admin', '2026-03-12 00:00:00', 'admin', '2026-03-12 00:00:00', b'0'),
(2129, 'TRADE_RISK', 'TRADE_EVENT', 'amount', '交易金额', 'DECIMAL', '$.amount', 0, 1, 0, NULL, '6800', NULL, '本次交易金额', 90, 1, 'admin', '2026-03-12 00:00:00', 'admin', '2026-03-12 00:00:00', b'0'),
(2130, 'TRADE_RISK', 'TRADE_EVENT', 'result', '交易结果', 'STRING', '$.result', 0, 1, 0, NULL, 'SUCCESS', NULL, '交易处理结果，例如 SUCCESS / FAIL', 100, 1, 'admin', '2026-03-12 00:00:00', 'admin', '2026-03-12 00:00:00', b'0');

-- ----------------------------
-- Records of ingest_source
-- ----------------------------
DELETE FROM `ingest_source` WHERE `source_code` IN ('trade_http_demo', 'trade_sdk_demo');
INSERT INTO `ingest_source` (`id`, `source_code`, `source_name`, `source_type`, `auth_type`, `auth_config_json`, `scene_scope_json`, `standard_topic_name`, `error_topic_name`, `rate_limit_qps`, `status`, `description`, `creator`, `create_time`, `updater`, `update_time`, `deleted`) VALUES
(2141, 'trade_http_demo', '交易 HTTP Demo', 'HTTP', 'HMAC', '{"headerName":"X-Pulsix-Signature","timestampHeader":"X-Pulsix-Timestamp","algorithm":"HmacSHA256","appKey":"trade-http-demo"}', '["TRADE_RISK"]', 'pulsix.event.standard', 'pulsix.event.dlq', 300, 0, 'S05 HTTP 接入源样例，演示 Header + HMAC 鉴权配置。', 'admin', '2026-03-12 00:00:00', 'admin', '2026-03-12 00:00:00', b'0'),
(2142, 'trade_sdk_demo', '交易 SDK Demo', 'SDK', 'TOKEN', '{"tokenHeader":"Authorization","tokenPrefix":"Bearer ","expireToleranceSeconds":60}', '["TRADE_RISK"]', 'pulsix.event.standard', 'pulsix.event.dlq', 1000, 0, 'S05 SDK 接入源样例，演示 Token 鉴权与更高 QPS 限额。', 'admin', '2026-03-12 00:00:00', 'admin', '2026-03-12 00:00:00', b'0');

-- ----------------------------
-- Records of ingest_mapping_def
-- ----------------------------
DELETE FROM `ingest_mapping_def` WHERE `source_code` = 'trade_http_demo' AND `scene_code` = 'TRADE_RISK' AND `event_code` = 'TRADE_EVENT';
INSERT INTO `ingest_mapping_def` (`id`, `source_code`, `scene_code`, `event_code`, `source_field_path`, `target_field_code`, `target_field_name`, `transform_type`, `transform_expr`, `default_value`, `required_flag`, `clean_rule_json`, `sort_no`, `status`, `creator`, `create_time`, `updater`, `update_time`, `deleted`) VALUES
(2151, 'trade_http_demo', 'TRADE_RISK', 'TRADE_EVENT', '$.event_id', 'eventId', '事件编号', 'DIRECT', NULL, NULL, 1, '{"trim":true,"blankToNull":true}', 10, 0, 'admin', '2026-03-12 00:00:00', 'admin', '2026-03-12 00:00:00', b'0'),
(2152, 'trade_http_demo', 'TRADE_RISK', 'TRADE_EVENT', '$.occur_time_ms', 'eventTime', '事件时间', 'TIME_MILLIS_TO_DATETIME', NULL, NULL, 1, NULL, 20, 0, 'admin', '2026-03-12 00:00:00', 'admin', '2026-03-12 00:00:00', b'0'),
(2153, 'trade_http_demo', 'TRADE_RISK', 'TRADE_EVENT', '$.req.traceId', 'traceId', '链路编号', 'DIRECT', NULL, NULL, 0, '{"trim":true,"blankToNull":true}', 30, 0, 'admin', '2026-03-12 00:00:00', 'admin', '2026-03-12 00:00:00', b'0'),
(2154, 'trade_http_demo', 'TRADE_RISK', 'TRADE_EVENT', '$.uid', 'userId', '用户编号', 'DIRECT', NULL, NULL, 1, '{"trim":true,"blankToNull":true}', 40, 0, 'admin', '2026-03-12 00:00:00', 'admin', '2026-03-12 00:00:00', b'0'),
(2155, 'trade_http_demo', 'TRADE_RISK', 'TRADE_EVENT', '$.dev_id', 'deviceId', '设备编号', 'DIRECT', NULL, NULL, 1, '{"trim":true,"blankToNull":true}', 50, 0, 'admin', '2026-03-12 00:00:00', 'admin', '2026-03-12 00:00:00', b'0'),
(2156, 'trade_http_demo', 'TRADE_RISK', 'TRADE_EVENT', '$.client_ip', 'ip', 'IP 地址', 'DIRECT', NULL, NULL, 1, '{"trim":true,"blankToNull":true}', 60, 0, 'admin', '2026-03-12 00:00:00', 'admin', '2026-03-12 00:00:00', b'0'),
(2157, 'trade_http_demo', 'TRADE_RISK', 'TRADE_EVENT', '$.pay_amt', 'amount', '交易金额', 'DIVIDE_100', NULL, NULL, 1, NULL, 70, 0, 'admin', '2026-03-12 00:00:00', 'admin', '2026-03-12 00:00:00', b'0'),
(2158, 'trade_http_demo', 'TRADE_RISK', 'TRADE_EVENT', '$.trade_result', 'result', '交易结果', 'ENUM_MAP', '{"ok":"SUCCESS","fail":"FAIL"}', NULL, 1, '{"trim":true,"lowerCase":true}', 80, 0, 'admin', '2026-03-12 00:00:00', 'admin', '2026-03-12 00:00:00', b'0');

-- ----------------------------
-- Records of event_sample
-- ----------------------------
DELETE FROM `event_sample` WHERE `scene_code` = 'TRADE_RISK' AND `event_code` = 'TRADE_EVENT';
INSERT INTO `event_sample` (`id`, `scene_code`, `event_code`, `sample_code`, `sample_name`, `sample_type`, `source_code`, `sample_json`, `description`, `sort_no`, `status`, `creator`, `create_time`, `updater`, `update_time`, `deleted`) VALUES
(2131, 'TRADE_RISK', 'TRADE_EVENT', 'TRADE_STD_SUCCESS', '交易成功标准样例', 'STANDARD', NULL, '{"eventId":"E_TRADE_9101","sceneCode":"TRADE_RISK","eventType":"trade","eventTime":"2026-03-12T09:00:00","traceId":"T_TRADE_9101","userId":"U9001","deviceId":"D9001","ip":"66.77.88.99","amount":6800,"result":"SUCCESS"}', 'S04 成功标准样例，可直接用于标准事件预览与后续仿真输入基线。', 10, 0, 'admin', '2026-03-12 00:00:00', 'admin', '2026-03-12 00:00:00', b'0'),
(2132, 'TRADE_RISK', 'TRADE_EVENT', 'TRADE_SIM_EXCEPTION', '交易异常仿真样例', 'SIMULATION', NULL, '{"eventId":"E_TRADE_9102","eventTime":"2026-03-12T09:05:00","userId":"U9002","deviceId":"D9002","ip":"10.20.30.40","amount":12800,"result":"FAIL"}', 'S04 异常仿真样例，故意省略 sceneCode/eventType 以验证默认值补齐预览。', 20, 0, 'admin', '2026-03-12 00:00:00', 'admin', '2026-03-12 00:00:00', b'0'),
(2133, 'TRADE_RISK', 'TRADE_EVENT', 'TRADE_RAW_HTTP', '交易 HTTP 原始样例', 'RAW', 'trade_http_demo', '{"event_id":"E_RAW_9103","occur_time_ms":1773287100000,"req":{"traceId":"T_RAW_9103"},"uid":" U9003 ","dev_id":"D9003","client_ip":"88.66.55.44","pay_amt":256800,"trade_result":"ok"}', 'S06 原始报文样例，可直接验证 trade_http_demo -> TRADE_EVENT 的字段映射与标准事件预览。', 30, 0, 'admin', '2026-03-12 00:00:00', 'admin', '2026-03-12 00:00:00', b'0');

-- ----------------------------
-- Records of scene_release
-- ----------------------------
DELETE FROM `scene_release` WHERE `scene_code` = 'TRADE_RISK' AND `version_no` IN (12, 13, 14);
INSERT INTO `scene_release` (`id`, `scene_code`, `version_no`, `snapshot_json`, `checksum`, `publish_status`, `validation_status`, `validation_report_json`, `dependency_digest_json`, `compile_duration_ms`, `compiled_feature_count`, `compiled_rule_count`, `compiled_policy_count`, `published_by`, `published_at`, `effective_from`, `rollback_from_version`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`) VALUES
(2201, 'TRADE_RISK', 12, '{"snapshotId":"TRADE_RISK_v12","sceneCode":"TRADE_RISK","sceneName":"交易风控","version":12,"status":"PUBLISHED","checksum":"8d2041a7cf8f47b4b6b0f91d2ab8d901","publishedAt":"2026-03-07T20:00:00Z","effectiveFrom":"2026-03-07T20:00:10Z","runtimeMode":"ASYNC_DECISION","scene":{"defaultPolicyCode":"TRADE_RISK_POLICY","allowedEventTypes":["trade"],"decisionTimeoutMs":500,"logLevel":"FULL"},"eventSchema":{"eventCode":"TRADE_EVENT","eventType":"trade","requiredFields":["eventId","sceneCode","eventType","eventTime","userId","deviceId","ip","amount","result"],"optionalFields":["merchantId","channel","province","city","ext"]},"variables":{"baseFields":["eventId","sceneCode","eventType","eventTime","traceId","userId","deviceId","ip","amount","result","merchantId","channel","province","city"]},"streamFeatures":[{"code":"user_trade_cnt_5m","name":"用户5分钟交易次数","type":"STREAM","sourceEventTypes":["trade"],"entityType":"USER","entityKeyExpr":"userId","aggType":"COUNT","valueExpr":"1","filterExpr":"result == ''SUCCESS''","windowType":"SLIDING","windowSize":"5m","windowSlide":"1m","includeCurrentEvent":true,"ttl":"10m","valueType":"LONG"},{"code":"user_trade_amt_sum_30m","name":"用户30分钟交易金额和","type":"STREAM","sourceEventTypes":["trade"],"entityType":"USER","entityKeyExpr":"userId","aggType":"SUM","valueExpr":"amount","filterExpr":"result == ''SUCCESS''","windowType":"SLIDING","windowSize":"30m","windowSlide":"1m","includeCurrentEvent":true,"ttl":"40m","valueType":"DECIMAL"},{"code":"device_bind_user_cnt_1h","name":"设备1小时关联用户数","type":"STREAM","sourceEventTypes":["trade"],"entityType":"DEVICE","entityKeyExpr":"deviceId","aggType":"DISTINCT_COUNT","valueExpr":"userId","filterExpr":"deviceId != nil && userId != nil","windowType":"SLIDING","windowSize":"1h","windowSlide":"5m","includeCurrentEvent":true,"ttl":"2h","valueType":"LONG"}],"lookupFeatures":[{"code":"device_in_blacklist","name":"设备是否命中黑名单","type":"LOOKUP","lookupType":"REDIS_SET","keyExpr":"deviceId","sourceRef":"pulsix:list:black:device","defaultValue":false,"valueType":"BOOLEAN","timeoutMs":20,"cacheTtlSeconds":30},{"code":"user_risk_level","name":"用户风险等级","type":"LOOKUP","lookupType":"REDIS_HASH","keyExpr":"userId","sourceRef":"pulsix:profile:user:risk","defaultValue":"L","valueType":"STRING","timeoutMs":20,"cacheTtlSeconds":30}],"derivedFeatures":[{"code":"high_amt_flag","name":"高金额标记","type":"DERIVED","engineType":"AVIATOR","expr":"amount >= 5000","dependsOn":["amount"],"valueType":"BOOLEAN"},{"code":"trade_burst_flag","name":"短时高频交易标记","type":"DERIVED","engineType":"AVIATOR","expr":"user_trade_cnt_5m >= 3 && amount >= 5000","dependsOn":["user_trade_cnt_5m","amount"],"valueType":"BOOLEAN"}],"rules":[{"code":"R001","name":"黑名单设备直接拒绝","engineType":"AVIATOR","priority":100,"whenExpr":"device_in_blacklist == true","dependsOn":["device_in_blacklist"],"hitAction":"REJECT","riskScore":100,"hitReasonTemplate":"设备命中黑名单","enabled":true},{"code":"R002","name":"大额且短时高频交易","engineType":"AVIATOR","priority":90,"whenExpr":"trade_burst_flag == true","dependsOn":["trade_burst_flag"],"hitAction":"REVIEW","riskScore":60,"hitReasonTemplate":"用户5分钟交易次数={user_trade_cnt_5m}, 当前金额={amount}","enabled":true},{"code":"R003","name":"高风险用户多账号设备","engineType":"GROOVY","priority":80,"whenExpr":"return device_bind_user_cnt_1h >= 4 && [''M'',''H''].contains(user_risk_level)","dependsOn":["device_bind_user_cnt_1h","user_risk_level"],"hitAction":"REJECT","riskScore":80,"hitReasonTemplate":"设备1小时关联用户数={device_bind_user_cnt_1h}, 用户风险等级={user_risk_level}","enabled":true}],"policy":{"policyCode":"TRADE_RISK_POLICY","policyName":"交易风控主策略","decisionMode":"FIRST_HIT","defaultAction":"PASS","ruleOrder":["R001","R003","R002"]},"runtimeHints":{"requiredStreamFeatures":["user_trade_cnt_5m","user_trade_amt_sum_30m","device_bind_user_cnt_1h"],"requiredLookupFeatures":["device_in_blacklist","user_risk_level"],"requiredDerivedFeatures":["high_amt_flag","trade_burst_flag"],"maxRuleExecutionCount":100,"allowGroovy":true,"needFullDecisionLog":true}}', '8d2041a7cf8f47b4b6b0f91d2ab8d901', 'PUBLISHED', 'PASSED', '{"checks":[{"type":"SNAPSHOT","result":"PASS","message":"快照结构自包含，并与 DemoFixtures.demoSnapshotJson() 对齐"}],"warnings":[]}', '{"streamFeatures":["user_trade_cnt_5m","user_trade_amt_sum_30m","device_bind_user_cnt_1h"],"lookupFeatures":["device_in_blacklist","user_risk_level"],"derivedFeatures":["high_amt_flag","trade_burst_flag"],"rules":["R001","R002","R003"]}', NULL, 7, 3, 1, 'admin', '2026-03-07 20:00:00', '2026-03-07 20:00:10', NULL, 'S14 历史稳定版本样例：可作为回滚来源的 TRADE_RISK v12。', 'admin', '2026-03-07 20:00:00', 'admin', '2026-03-07 20:00:00', b'0'),
(2202, 'TRADE_RISK', 13, '{"snapshotId":"TRADE_RISK_v13","sceneCode":"TRADE_RISK","sceneName":"交易风控","version":13,"status":"ROLLED_BACK","checksum":"8d2041a7cf8f47b4b6b0f91d2ab8d913","publishedAt":"2026-03-12T09:00:00Z","effectiveFrom":"2026-03-12T09:05:00Z","runtimeMode":"ASYNC_DECISION","scene":{"defaultPolicyCode":"TRADE_RISK_POLICY","allowedEventTypes":["trade"],"decisionTimeoutMs":500,"logLevel":"FULL"},"eventSchema":{"eventCode":"TRADE_EVENT","eventType":"trade","requiredFields":["eventId","sceneCode","eventType","eventTime","userId","deviceId","ip","amount","result"],"optionalFields":["merchantId","channel","province","city","ext"]},"variables":{"baseFields":["eventId","sceneCode","eventType","eventTime","traceId","userId","deviceId","ip","amount","result","merchantId","channel","province","city"]},"streamFeatures":[{"code":"user_trade_cnt_5m","name":"用户5分钟交易次数","type":"STREAM","sourceEventTypes":["trade"],"entityType":"USER","entityKeyExpr":"userId","aggType":"COUNT","valueExpr":"1","filterExpr":"result == ''SUCCESS''","windowType":"SLIDING","windowSize":"5m","windowSlide":"1m","includeCurrentEvent":true,"ttl":"10m","valueType":"LONG"},{"code":"user_trade_amt_sum_30m","name":"用户30分钟交易金额和","type":"STREAM","sourceEventTypes":["trade"],"entityType":"USER","entityKeyExpr":"userId","aggType":"SUM","valueExpr":"amount","filterExpr":"result == ''SUCCESS''","windowType":"SLIDING","windowSize":"30m","windowSlide":"1m","includeCurrentEvent":true,"ttl":"40m","valueType":"DECIMAL"},{"code":"device_bind_user_cnt_1h","name":"设备1小时关联用户数","type":"STREAM","sourceEventTypes":["trade"],"entityType":"DEVICE","entityKeyExpr":"deviceId","aggType":"DISTINCT_COUNT","valueExpr":"userId","filterExpr":"deviceId != nil && userId != nil","windowType":"SLIDING","windowSize":"1h","windowSlide":"5m","includeCurrentEvent":true,"ttl":"2h","valueType":"LONG"}],"lookupFeatures":[{"code":"device_in_blacklist","name":"设备是否命中黑名单","type":"LOOKUP","lookupType":"REDIS_SET","keyExpr":"deviceId","sourceRef":"pulsix:list:black:device","defaultValue":false,"valueType":"BOOLEAN","timeoutMs":20,"cacheTtlSeconds":30},{"code":"user_risk_level","name":"用户风险等级","type":"LOOKUP","lookupType":"REDIS_HASH","keyExpr":"userId","sourceRef":"pulsix:profile:user:risk","defaultValue":"L","valueType":"STRING","timeoutMs":20,"cacheTtlSeconds":30}],"derivedFeatures":[{"code":"high_amt_flag","name":"高金额标记","type":"DERIVED","engineType":"AVIATOR","expr":"amount >= 5000","dependsOn":["amount"],"valueType":"BOOLEAN"},{"code":"trade_burst_flag","name":"短时高频交易标记","type":"DERIVED","engineType":"AVIATOR","expr":"user_trade_cnt_5m >= 3 && amount >= 5000","dependsOn":["user_trade_cnt_5m","amount"],"valueType":"BOOLEAN"}],"rules":[{"code":"R001","name":"黑名单设备直接拒绝","engineType":"AVIATOR","priority":100,"whenExpr":"device_in_blacklist == true","dependsOn":["device_in_blacklist"],"hitAction":"REJECT","riskScore":100,"hitReasonTemplate":"设备命中黑名单","enabled":true},{"code":"R002","name":"大额且短时高频交易","engineType":"AVIATOR","priority":90,"whenExpr":"trade_burst_flag == true","dependsOn":["trade_burst_flag"],"hitAction":"REVIEW","riskScore":60,"hitReasonTemplate":"用户5分钟交易次数={user_trade_cnt_5m}, 当前金额={amount}","enabled":true},{"code":"R003","name":"高风险用户多账号设备","engineType":"GROOVY","priority":80,"whenExpr":"return device_bind_user_cnt_1h >= 4 && [''M'',''H''].contains(user_risk_level)","dependsOn":["device_bind_user_cnt_1h","user_risk_level"],"hitAction":"REJECT","riskScore":80,"hitReasonTemplate":"设备1小时关联用户数={device_bind_user_cnt_1h}, 用户风险等级={user_risk_level}","enabled":true}],"policy":{"policyCode":"TRADE_RISK_POLICY","policyName":"交易风控主策略","decisionMode":"FIRST_HIT","defaultAction":"PASS","ruleOrder":["R001","R003","R002"]},"runtimeHints":{"requiredStreamFeatures":["user_trade_cnt_5m","user_trade_amt_sum_30m","device_bind_user_cnt_1h"],"requiredLookupFeatures":["device_in_blacklist","user_risk_level"],"requiredDerivedFeatures":["high_amt_flag","trade_burst_flag"],"maxRuleExecutionCount":100,"allowGroovy":true,"needFullDecisionLog":true}}', '8d2041a7cf8f47b4b6b0f91d2ab8d913', 'ROLLED_BACK', 'PASSED', '{"checks":[{"type":"SNAPSHOT","result":"PASS","message":"快照结构自包含，并与 DemoFixtures.demoSnapshotJson() 对齐"}],"warnings":[]}', '{"streamFeatures":["user_trade_cnt_5m","user_trade_amt_sum_30m","device_bind_user_cnt_1h"],"lookupFeatures":["device_in_blacklist","user_risk_level"],"derivedFeatures":["high_amt_flag","trade_burst_flag"],"rules":["R001","R002","R003"]}', NULL, 7, 3, 1, 'admin', '2026-03-12 09:00:00', '2026-03-12 09:05:00', NULL, 'S14 正式发布样例：TRADE_RISK v13 发布后因观察异常被回滚。', 'admin', '2026-03-12 09:00:00', 'admin', '2026-03-12 09:00:00', b'0'),
(2203, 'TRADE_RISK', 14, '{"snapshotId":"TRADE_RISK_v14","sceneCode":"TRADE_RISK","sceneName":"交易风控","version":14,"status":"ACTIVE","checksum":"8d2041a7cf8f47b4b6b0f91d2ab8d914","publishedAt":"2026-03-12T09:20:00Z","effectiveFrom":"2026-03-12T09:25:00Z","runtimeMode":"ASYNC_DECISION","scene":{"defaultPolicyCode":"TRADE_RISK_POLICY","allowedEventTypes":["trade"],"decisionTimeoutMs":500,"logLevel":"FULL"},"eventSchema":{"eventCode":"TRADE_EVENT","eventType":"trade","requiredFields":["eventId","sceneCode","eventType","eventTime","userId","deviceId","ip","amount","result"],"optionalFields":["merchantId","channel","province","city","ext"]},"variables":{"baseFields":["eventId","sceneCode","eventType","eventTime","traceId","userId","deviceId","ip","amount","result","merchantId","channel","province","city"]},"streamFeatures":[{"code":"user_trade_cnt_5m","name":"用户5分钟交易次数","type":"STREAM","sourceEventTypes":["trade"],"entityType":"USER","entityKeyExpr":"userId","aggType":"COUNT","valueExpr":"1","filterExpr":"result == ''SUCCESS''","windowType":"SLIDING","windowSize":"5m","windowSlide":"1m","includeCurrentEvent":true,"ttl":"10m","valueType":"LONG"},{"code":"user_trade_amt_sum_30m","name":"用户30分钟交易金额和","type":"STREAM","sourceEventTypes":["trade"],"entityType":"USER","entityKeyExpr":"userId","aggType":"SUM","valueExpr":"amount","filterExpr":"result == ''SUCCESS''","windowType":"SLIDING","windowSize":"30m","windowSlide":"1m","includeCurrentEvent":true,"ttl":"40m","valueType":"DECIMAL"},{"code":"device_bind_user_cnt_1h","name":"设备1小时关联用户数","type":"STREAM","sourceEventTypes":["trade"],"entityType":"DEVICE","entityKeyExpr":"deviceId","aggType":"DISTINCT_COUNT","valueExpr":"userId","filterExpr":"deviceId != nil && userId != nil","windowType":"SLIDING","windowSize":"1h","windowSlide":"5m","includeCurrentEvent":true,"ttl":"2h","valueType":"LONG"}],"lookupFeatures":[{"code":"device_in_blacklist","name":"设备是否命中黑名单","type":"LOOKUP","lookupType":"REDIS_SET","keyExpr":"deviceId","sourceRef":"pulsix:list:black:device","defaultValue":false,"valueType":"BOOLEAN","timeoutMs":20,"cacheTtlSeconds":30},{"code":"user_risk_level","name":"用户风险等级","type":"LOOKUP","lookupType":"REDIS_HASH","keyExpr":"userId","sourceRef":"pulsix:profile:user:risk","defaultValue":"L","valueType":"STRING","timeoutMs":20,"cacheTtlSeconds":30}],"derivedFeatures":[{"code":"high_amt_flag","name":"高金额标记","type":"DERIVED","engineType":"AVIATOR","expr":"amount >= 5000","dependsOn":["amount"],"valueType":"BOOLEAN"},{"code":"trade_burst_flag","name":"短时高频交易标记","type":"DERIVED","engineType":"AVIATOR","expr":"user_trade_cnt_5m >= 3 && amount >= 5000","dependsOn":["user_trade_cnt_5m","amount"],"valueType":"BOOLEAN"}],"rules":[{"code":"R001","name":"黑名单设备直接拒绝","engineType":"AVIATOR","priority":100,"whenExpr":"device_in_blacklist == true","dependsOn":["device_in_blacklist"],"hitAction":"REJECT","riskScore":100,"hitReasonTemplate":"设备命中黑名单","enabled":true},{"code":"R002","name":"大额且短时高频交易","engineType":"AVIATOR","priority":90,"whenExpr":"trade_burst_flag == true","dependsOn":["trade_burst_flag"],"hitAction":"REVIEW","riskScore":60,"hitReasonTemplate":"用户5分钟交易次数={user_trade_cnt_5m}, 当前金额={amount}","enabled":true},{"code":"R003","name":"高风险用户多账号设备","engineType":"GROOVY","priority":80,"whenExpr":"return device_bind_user_cnt_1h >= 4 && [''M'',''H''].contains(user_risk_level)","dependsOn":["device_bind_user_cnt_1h","user_risk_level"],"hitAction":"REJECT","riskScore":80,"hitReasonTemplate":"设备1小时关联用户数={device_bind_user_cnt_1h}, 用户风险等级={user_risk_level}","enabled":true}],"policy":{"policyCode":"TRADE_RISK_POLICY","policyName":"交易风控主策略","decisionMode":"FIRST_HIT","defaultAction":"PASS","ruleOrder":["R001","R003","R002"]},"runtimeHints":{"requiredStreamFeatures":["user_trade_cnt_5m","user_trade_amt_sum_30m","device_bind_user_cnt_1h"],"requiredLookupFeatures":["device_in_blacklist","user_risk_level"],"requiredDerivedFeatures":["high_amt_flag","trade_burst_flag"],"maxRuleExecutionCount":100,"allowGroovy":true,"needFullDecisionLog":true}}', '8d2041a7cf8f47b4b6b0f91d2ab8d914', 'ACTIVE', 'PASSED', '{"checks":[{"type":"SNAPSHOT","result":"PASS","message":"快照结构自包含，并与 DemoFixtures.demoSnapshotJson() 对齐"}],"warnings":[]}', '{"streamFeatures":["user_trade_cnt_5m","user_trade_amt_sum_30m","device_bind_user_cnt_1h"],"lookupFeatures":["device_in_blacklist","user_risk_level"],"derivedFeatures":["high_amt_flag","trade_burst_flag"],"rules":["R001","R002","R003"]}', NULL, 7, 3, 1, 'admin', '2026-03-12 09:20:00', '2026-03-12 09:25:00', 12, 'S14 回滚样例：基于 TRADE_RISK v12 生成回滚版本 v14，并立即恢复生效。', 'admin', '2026-03-12 09:20:00', 'admin', '2026-03-12 09:20:00', b'0');

DELETE FROM `risk_audit_log` WHERE `scene_code` = 'TRADE_RISK' AND `biz_type` = 'RELEASE';
INSERT INTO `risk_audit_log` (`id`, `trace_id`, `scene_code`, `operator_id`, `operator_name`, `biz_type`, `biz_code`, `action_type`, `before_json`, `after_json`, `remark`, `operate_time`, `creator`, `create_time`, `updater`, `update_time`, `deleted`) VALUES
(9101, 'TRACE-S14-PUBLISH-001', 'TRADE_RISK', 1, 'admin', 'RELEASE', 'TRADE_RISK_v13', 'PUBLISH', '{"sceneCode":"TRADE_RISK","versionNo":13,"publishStatus":"DRAFT","validationStatus":"PASSED"}', '{"sceneCode":"TRADE_RISK","versionNo":13,"publishStatus":"ROLLED_BACK","validationStatus":"PASSED","effectiveFrom":"2026-03-12 09:05:00"}', 'S14 发布样例：v13 正式发布后进入观察窗口。', '2026-03-12 09:00:00', 'admin', '2026-03-12 09:00:00', 'admin', '2026-03-12 09:00:00', b'0'),
(9102, 'TRACE-S14-ROLLBACK-001', 'TRADE_RISK', 1, 'admin', 'RELEASE', 'TRADE_RISK_v14', 'ROLLBACK', '{"sceneCode":"TRADE_RISK","versionNo":12,"publishStatus":"PUBLISHED","validationStatus":"PASSED"}', '{"sceneCode":"TRADE_RISK","versionNo":14,"publishStatus":"ACTIVE","validationStatus":"PASSED","rollbackFromVersion":12}', 'S14 回滚样例：基于 v12 生成回滚版本 v14。', '2026-03-12 09:20:00', 'admin', '2026-03-12 09:20:00', 'admin', '2026-03-12 09:20:00', b'0');

-- ----------------------------
-- Records of list_set
-- ----------------------------
DELETE FROM `list_item` WHERE `scene_code` = 'TRADE_RISK' AND `list_code` = 'DEVICE_BLACKLIST';
DELETE FROM `list_set` WHERE `scene_code` = 'TRADE_RISK' AND `list_code` = 'DEVICE_BLACKLIST';
INSERT INTO `list_set` (`id`, `scene_code`, `list_code`, `list_name`, `match_type`, `list_type`, `storage_type`, `sync_mode`, `sync_status`, `last_sync_time`, `status`, `description`, `creator`, `create_time`, `updater`, `update_time`, `deleted`) VALUES
(2161, 'TRADE_RISK', 'DEVICE_BLACKLIST', '设备黑名单', 'DEVICE', 'BLACK', 'REDIS_SET', 'FULL', 'PENDING', NULL, 0, 'S07 名单中心样例；手动同步后写入 pulsix:list:black:device:{deviceId}。', 'admin', '2026-03-12 00:00:00', 'admin', '2026-03-12 00:00:00', b'0');

-- ----------------------------
-- Records of list_item
-- ----------------------------
INSERT INTO `list_item` (`id`, `scene_code`, `list_code`, `match_key`, `match_value`, `expire_at`, `status`, `source_type`, `batch_no`, `remark`, `ext_json`, `creator`, `create_time`, `updater`, `update_time`, `deleted`) VALUES
(2162, 'TRADE_RISK', 'DEVICE_BLACKLIST', 'deviceId', 'D0009', NULL, 0, 'MANUAL', NULL, '命中设备黑名单，直接拒绝', '{"reason":"fraud_device","operator":"admin"}', 'admin', '2026-03-12 00:00:00', 'admin', '2026-03-12 00:00:00', b'0'),
(2163, 'TRADE_RISK', 'DEVICE_BLACKLIST', 'deviceId', 'D0099', '2026-12-31 23:59:59', 0, 'MANUAL', NULL, '高风险设备临时拉黑', '{"reason":"temp_block","ticketNo":"WK-20260312-01"}', 'admin', '2026-03-12 00:00:00', 'admin', '2026-03-12 00:00:00', b'0');

-- ----------------------------
-- Records of entity_type_def
-- ----------------------------
DELETE FROM `entity_type_def` WHERE `entity_type` IN ('USER', 'DEVICE', 'IP', 'MERCHANT');
INSERT INTO `entity_type_def` (`id`, `entity_type`, `entity_name`, `key_field_name`, `status`, `description`, `creator`, `create_time`, `updater`, `update_time`, `deleted`) VALUES
(3101, 'USER', '用户', 'userId', 0, '按用户维度聚合，用于用户短时交易次数、交易金额累计等特征。', 'admin', '2026-03-12 00:00:00', 'admin', '2026-03-12 00:00:00', b'0'),
(3102, 'DEVICE', '设备', 'deviceId', 0, '按设备维度聚合，用于设备关联账号数、设备命中率等特征。', 'admin', '2026-03-12 00:00:00', 'admin', '2026-03-12 00:00:00', b'0'),
(3103, 'IP', 'IP 地址', 'ip', 0, '按 IP 维度聚合，用于同 IP 短时请求次数等特征。', 'admin', '2026-03-12 00:00:00', 'admin', '2026-03-12 00:00:00', b'0'),
(3104, 'MERCHANT', '商户', 'merchantId', 0, '按商户维度聚合，用于商户风控指标统计。', 'admin', '2026-03-12 00:00:00', 'admin', '2026-03-12 00:00:00', b'0');

-- ----------------------------
-- Records of feature_def
-- ----------------------------
DELETE FROM `feature_stream_conf` WHERE `scene_code` = 'TRADE_RISK' AND `feature_code` IN ('user_trade_cnt_5m', 'user_trade_amt_sum_30m', 'device_bind_user_cnt_1h');
DELETE FROM `feature_def` WHERE `scene_code` = 'TRADE_RISK' AND `feature_code` IN ('user_trade_cnt_5m', 'user_trade_amt_sum_30m', 'device_bind_user_cnt_1h');
INSERT INTO `feature_def` (`id`, `scene_code`, `feature_code`, `feature_name`, `feature_type`, `entity_type`, `event_code`, `value_type`, `status`, `version`, `description`, `creator`, `create_time`, `updater`, `update_time`, `deleted`) VALUES
(3201, 'TRADE_RISK', 'user_trade_cnt_5m', '用户 5 分钟交易次数', 'STREAM', 'USER', 'TRADE_EVENT', 'LONG', 0, 1, '统计用户在最近 5 分钟内的成功交易次数，用于短时高频交易识别。', 'admin', '2026-03-12 00:00:00', 'admin', '2026-03-12 00:00:00', b'0'),
(3202, 'TRADE_RISK', 'user_trade_amt_sum_30m', '用户 30 分钟交易金额和', 'STREAM', 'USER', 'TRADE_EVENT', 'DECIMAL', 0, 1, '统计用户在最近 30 分钟内的成功交易金额总和，用于大额集中交易识别。', 'admin', '2026-03-12 00:00:00', 'admin', '2026-03-12 00:00:00', b'0'),
(3203, 'TRADE_RISK', 'device_bind_user_cnt_1h', '设备 1 小时关联用户数', 'STREAM', 'DEVICE', 'TRADE_EVENT', 'LONG', 0, 1, '统计设备在最近 1 小时内关联过的不同用户数，用于一机多号识别。', 'admin', '2026-03-12 00:00:00', 'admin', '2026-03-12 00:00:00', b'0');

-- ----------------------------
-- Records of feature_stream_conf
-- ----------------------------
INSERT INTO `feature_stream_conf` (`id`, `scene_code`, `feature_code`, `source_event_codes`, `entity_key_expr`, `agg_type`, `value_expr`, `filter_expr`, `window_type`, `window_size`, `window_slide`, `include_current_event`, `ttl_seconds`, `state_hint_json`, `status`, `creator`, `create_time`, `updater`, `update_time`, `deleted`) VALUES
(3301, 'TRADE_RISK', 'user_trade_cnt_5m', 'TRADE_EVENT', 'userId', 'COUNT', NULL, 'result == ''SUCCESS''', 'SLIDING', '5m', '1m', 1, 600, JSON_OBJECT('bucketHint', 1024, 'replicaTtl', '10m'), 0, 'admin', '2026-03-12 00:00:00', 'admin', '2026-03-12 00:00:00', b'0'),
(3302, 'TRADE_RISK', 'user_trade_amt_sum_30m', 'TRADE_EVENT', 'userId', 'SUM', 'amount', 'result == ''SUCCESS''', 'SLIDING', '30m', '1m', 1, 2400, JSON_OBJECT('bucketHint', 2048, 'replicaTtl', '40m'), 0, 'admin', '2026-03-12 00:00:00', 'admin', '2026-03-12 00:00:00', b'0'),
(3303, 'TRADE_RISK', 'device_bind_user_cnt_1h', 'TRADE_EVENT', 'deviceId', 'DISTINCT_COUNT', 'userId', 'deviceId != nil && userId != nil', 'SLIDING', '1h', '5m', 1, 7200, JSON_OBJECT('bucketHint', 4096, 'cardinalityHint', 10000, 'replicaTtl', '2h'), 0, 'admin', '2026-03-12 00:00:00', 'admin', '2026-03-12 00:00:00', b'0');

-- ----------------------------
-- Records of feature_lookup_conf
-- ----------------------------
DELETE FROM `feature_lookup_conf` WHERE `scene_code` = 'TRADE_RISK' AND `feature_code` IN ('device_in_blacklist', 'user_risk_level');
DELETE FROM `feature_def` WHERE `scene_code` = 'TRADE_RISK' AND `feature_code` IN ('device_in_blacklist', 'user_risk_level');
INSERT INTO `feature_def` (`id`, `scene_code`, `feature_code`, `feature_name`, `feature_type`, `entity_type`, `event_code`, `value_type`, `status`, `version`, `description`, `creator`, `create_time`, `updater`, `update_time`, `deleted`) VALUES
(3401, 'TRADE_RISK', 'device_in_blacklist', '设备是否命中黑名单', 'LOOKUP', NULL, NULL, 'BOOLEAN', 0, 1, '通过 Redis Set / 前缀 Key 判断设备是否命中黑名单；查不到默认返回 false。', 'admin', '2026-03-12 00:00:00', 'admin', '2026-03-12 00:00:00', b'0'),
(3402, 'TRADE_RISK', 'user_risk_level', '用户风险等级', 'LOOKUP', NULL, NULL, 'STRING', 0, 1, '通过 Redis Hash 查询用户风险等级画像；查不到默认返回 L。', 'admin', '2026-03-12 00:00:00', 'admin', '2026-03-12 00:00:00', b'0');
INSERT INTO `feature_lookup_conf` (`id`, `scene_code`, `feature_code`, `lookup_type`, `key_expr`, `source_ref`, `default_value`, `cache_ttl_seconds`, `timeout_ms`, `extra_json`, `status`, `creator`, `create_time`, `updater`, `update_time`, `deleted`) VALUES
(3501, 'TRADE_RISK', 'device_in_blacklist', 'REDIS_SET', 'deviceId', 'pulsix:list:black:device', 'false', 30, 20, JSON_OBJECT('keyMode', 'PREFIX_KEY', 'matchTarget', 'deviceId'), 0, 'admin', '2026-03-12 00:00:00', 'admin', '2026-03-12 00:00:00', b'0'),
(3502, 'TRADE_RISK', 'user_risk_level', 'REDIS_HASH', 'userId', 'pulsix:profile:user:risk', 'L', 30, 20, JSON_OBJECT('redisOp', 'HGET', 'fieldType', 'STRING'), 0, 'admin', '2026-03-12 00:00:00', 'admin', '2026-03-12 00:00:00', b'0');

-- ----------------------------
-- Records of feature_derived_conf
-- ----------------------------
DELETE FROM `feature_derived_conf` WHERE `scene_code` = 'TRADE_RISK' AND `feature_code` IN ('high_amt_flag', 'trade_burst_flag');
DELETE FROM `feature_def` WHERE `scene_code` = 'TRADE_RISK' AND `feature_code` IN ('high_amt_flag', 'trade_burst_flag');
INSERT INTO `feature_def` (`id`, `scene_code`, `feature_code`, `feature_name`, `feature_type`, `entity_type`, `event_code`, `value_type`, `status`, `version`, `description`, `creator`, `create_time`, `updater`, `update_time`, `deleted`) VALUES
(3601, 'TRADE_RISK', 'high_amt_flag', '高金额标记', 'DERIVED', NULL, NULL, 'BOOLEAN', 0, 1, '基于事件字段 amount 判断当前交易是否达到高金额阈值。', 'admin', '2026-03-12 00:00:00', 'admin', '2026-03-12 00:00:00', b'0'),
(3602, 'TRADE_RISK', 'trade_burst_flag', '短时高频交易标记', 'DERIVED', NULL, NULL, 'BOOLEAN', 0, 1, '基于流式特征 user_trade_cnt_5m 和当前 amount 组合判断短时高频大额交易。', 'admin', '2026-03-12 00:00:00', 'admin', '2026-03-12 00:00:00', b'0');
INSERT INTO `feature_derived_conf` (`id`, `scene_code`, `feature_code`, `engine_type`, `expr_content`, `depends_on_json`, `value_type`, `sandbox_flag`, `timeout_ms`, `extra_json`, `status`, `creator`, `create_time`, `updater`, `update_time`, `deleted`) VALUES
(3701, 'TRADE_RISK', 'high_amt_flag', 'AVIATOR', 'amount >= 5000', JSON_ARRAY('amount'), 'BOOLEAN', 1, 50, JSON_OBJECT('resultAlias', 'highAmtFlag', 'compiled', true), 0, 'admin', '2026-03-12 00:00:00', 'admin', '2026-03-12 00:00:00', b'0'),
(3702, 'TRADE_RISK', 'trade_burst_flag', 'AVIATOR', 'user_trade_cnt_5m >= 3 && amount >= 5000', JSON_ARRAY('user_trade_cnt_5m', 'amount'), 'BOOLEAN', 1, 50, JSON_OBJECT('resultAlias', 'tradeBurstFlag', 'compiled', true), 0, 'admin', '2026-03-12 00:00:00', 'admin', '2026-03-12 00:00:00', b'0');

-- ----------------------------
-- Records of rule_def
-- ----------------------------
DELETE FROM `rule_def` WHERE `scene_code` = 'TRADE_RISK' AND `rule_code` IN ('R001', 'R002', 'R003');
INSERT INTO `rule_def` (`id`, `scene_code`, `rule_code`, `rule_name`, `rule_type`, `engine_type`, `expr_content`, `priority`, `hit_action`, `risk_score`, `hit_reason_template`, `status`, `version`, `description`, `creator`, `create_time`, `updater`, `update_time`, `deleted`) VALUES
(3801, 'TRADE_RISK', 'R001', '黑名单设备直接拒绝', 'NORMAL', 'AVIATOR', 'device_in_blacklist == true', 100, 'REJECT', 100, '设备命中黑名单', 0, 1, '设备命中黑名单时直接拒绝，用于快速拦截高风险设备。', 'admin', '2026-03-12 00:00:00', 'admin', '2026-03-12 00:00:00', b'0'),
(3802, 'TRADE_RISK', 'R002', '大额且短时高频交易', 'NORMAL', 'AVIATOR', 'trade_burst_flag == true', 90, 'REVIEW', 60, '用户5分钟交易次数={user_trade_cnt_5m}, 当前金额={amount}', 0, 1, '结合派生特征 trade_burst_flag，对疑似短时高频大额交易转人工复核。', 'admin', '2026-03-12 00:00:00', 'admin', '2026-03-12 00:00:00', b'0'),
(3803, 'TRADE_RISK', 'R003', '高风险用户多账号设备', 'NORMAL', 'GROOVY', 'return device_bind_user_cnt_1h >= 4 && [''M'',''H''].contains(user_risk_level)', 80, 'REJECT', 80, '设备1小时关联用户数={device_bind_user_cnt_1h}, 用户风险等级={user_risk_level}', 0, 1, '使用 Groovy 表达式识别一机多号且用户画像风险等级较高的交易。', 'admin', '2026-03-12 00:00:00', 'admin', '2026-03-12 00:00:00', b'0');

-- ----------------------------
-- Records of policy_def / policy_rule_ref
-- ----------------------------
DELETE FROM `policy_rule_ref` WHERE `scene_code` = 'TRADE_RISK' AND `policy_code` IN ('TRADE_RISK_POLICY');
DELETE FROM `policy_def` WHERE `scene_code` = 'TRADE_RISK' AND `policy_code` IN ('TRADE_RISK_POLICY');
INSERT INTO `policy_def` (`id`, `scene_code`, `policy_code`, `policy_name`, `decision_mode`, `default_action`, `score_calc_mode`, `status`, `version`, `description`, `creator`, `create_time`, `updater`, `update_time`, `deleted`) VALUES
(3901, 'TRADE_RISK', 'TRADE_RISK_POLICY', '交易风控主策略', 'FIRST_HIT', 'PASS', 'NONE', 0, 1, 'S12 策略中心初始化样例；当前阶段只做 FIRST_HIT 与规则顺序维护。', 'admin', '2026-03-12 00:00:00', 'admin', '2026-03-12 00:00:00', b'0');
INSERT INTO `policy_rule_ref` (`id`, `scene_code`, `policy_code`, `rule_code`, `order_no`, `enabled_flag`, `branch_expr`, `score_weight`, `stop_on_hit`, `creator`, `create_time`, `updater`, `update_time`, `deleted`) VALUES
(3961, 'TRADE_RISK', 'TRADE_RISK_POLICY', 'R001', 10, 1, NULL, NULL, 1, 'admin', '2026-03-12 00:00:00', 'admin', '2026-03-12 00:00:00', b'0'),
(3962, 'TRADE_RISK', 'TRADE_RISK_POLICY', 'R003', 20, 1, NULL, NULL, 1, 'admin', '2026-03-12 00:00:00', 'admin', '2026-03-12 00:00:00', b'0'),
(3963, 'TRADE_RISK', 'TRADE_RISK_POLICY', 'R002', 30, 1, NULL, NULL, 1, 'admin', '2026-03-12 00:00:00', 'admin', '2026-03-12 00:00:00', b'0');

-- ----------------------------
-- S00 风控菜单骨架（可重复执行）
-- 说明：
-- 1. 仅插入 `system_menu` 数据，不改动 `pulsix-system-infra.sql` 的表结构。
-- 2. 管理员角色具备全菜单能力，因此这里优先保证菜单与按钮权限样例完整可见。
-- ----------------------------
DELETE FROM `system_menu` WHERE `id` BETWEEN 7000 AND 7499;

INSERT INTO `system_menu` (`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`) VALUES
(7000, '风控平台', '', 1, 300, 0, '/risk', 'ep:warning-filled', '', '', 0, b'1', b'1', b'1', 'admin', '2026-03-12 00:00:00', 'admin', '2026-03-12 00:00:00', b'0'),
(7100, '设计建模', '', 1, 10, 7000, 'model', 'ep:document', '', '', 0, b'1', b'1', b'1', 'admin', '2026-03-12 00:00:00', 'admin', '2026-03-12 00:00:00', b'0'),
(7110, '场景管理', 'risk:scene:query', 2, 10, 7100, 'scene', 'ep:management', 'risk/scene/index', 'RiskScene', 0, b'1', b'0', b'1', 'admin', '2026-03-12 00:00:00', 'admin', '2026-03-12 00:00:00', b'0'),
(7111, '场景查询', 'risk:scene:query', 3, 1, 7110, '', '', '', '', 0, b'1', b'1', b'1', 'admin', '2026-03-12 00:00:00', 'admin', '2026-03-12 00:00:00', b'0'),
(7112, '场景新增', 'risk:scene:create', 3, 2, 7110, '', '', '', '', 0, b'1', b'1', b'1', 'admin', '2026-03-12 00:00:00', 'admin', '2026-03-12 00:00:00', b'0'),
(7113, '场景修改', 'risk:scene:update', 3, 3, 7110, '', '', '', '', 0, b'1', b'1', b'1', 'admin', '2026-03-12 00:00:00', 'admin', '2026-03-12 00:00:00', b'0'),
(7114, '场景启停', 'risk:scene:update-status', 3, 4, 7110, '', '', '', '', 0, b'1', b'1', b'1', 'admin', '2026-03-12 00:00:00', 'admin', '2026-03-12 00:00:00', b'0'),
(7115, '场景详情', 'risk:scene:get', 3, 5, 7110, '', '', '', '', 0, b'1', b'1', b'1', 'admin', '2026-03-12 00:00:00', 'admin', '2026-03-12 00:00:00', b'0'),
(7120, '事件 Schema', 'risk:event-schema:query', 2, 20, 7100, 'event-schema', 'ep:files', 'risk/event-schema/index', 'RiskEventSchema', 0, b'1', b'0', b'1', 'admin', '2026-03-12 00:00:00', 'admin', '2026-03-12 00:00:00', b'0'),
(7121, '事件 Schema 查询', 'risk:event-schema:query', 3, 1, 7120, '', '', '', '', 0, b'1', b'1', b'1', 'admin', '2026-03-12 00:00:00', 'admin', '2026-03-12 00:00:00', b'0'),
(7122, '事件 Schema 新增', 'risk:event-schema:create', 3, 2, 7120, '', '', '', '', 0, b'1', b'1', b'1', 'admin', '2026-03-12 00:00:00', 'admin', '2026-03-12 00:00:00', b'0'),
(7123, '事件 Schema 修改', 'risk:event-schema:update', 3, 3, 7120, '', '', '', '', 0, b'1', b'1', b'1', 'admin', '2026-03-12 00:00:00', 'admin', '2026-03-12 00:00:00', b'0'),
(7124, '事件 Schema 删除', 'risk:event-schema:delete', 3, 4, 7120, '', '', '', '', 0, b'1', b'1', b'1', 'admin', '2026-03-12 00:00:00', 'admin', '2026-03-12 00:00:00', b'0'),
(7125, '事件 Schema 详情', 'risk:event-schema:get', 3, 5, 7120, '', '', '', '', 0, b'1', b'1', b'1', 'admin', '2026-03-12 00:00:00', 'admin', '2026-03-12 00:00:00', b'0'),
(7130, '事件字段', 'risk:event-field:query', 2, 30, 7100, 'event-field', 'ep:list', 'risk/event-field/index', 'RiskEventField', 0, b'1', b'0', b'1', 'admin', '2026-03-12 00:00:00', 'admin', '2026-03-12 00:00:00', b'0'),
(7131, '事件字段查询', 'risk:event-field:query', 3, 1, 7130, '', '', '', '', 0, b'1', b'1', b'1', 'admin', '2026-03-12 00:00:00', 'admin', '2026-03-12 00:00:00', b'0'),
(7132, '事件字段新增', 'risk:event-field:create', 3, 2, 7130, '', '', '', '', 0, b'1', b'1', b'1', 'admin', '2026-03-12 00:00:00', 'admin', '2026-03-12 00:00:00', b'0'),
(7133, '事件字段修改', 'risk:event-field:update', 3, 3, 7130, '', '', '', '', 0, b'1', b'1', b'1', 'admin', '2026-03-12 00:00:00', 'admin', '2026-03-12 00:00:00', b'0'),
(7134, '事件字段删除', 'risk:event-field:delete', 3, 4, 7130, '', '', '', '', 0, b'1', b'1', b'1', 'admin', '2026-03-12 00:00:00', 'admin', '2026-03-12 00:00:00', b'0'),
(7135, '事件字段排序', 'risk:event-field:sort', 3, 5, 7130, '', '', '', '', 0, b'1', b'1', b'1', 'admin', '2026-03-12 00:00:00', 'admin', '2026-03-12 00:00:00', b'0'),
(7140, '事件样例', 'risk:event-sample:query', 2, 40, 7100, 'event-sample', 'ep:document-copy', 'risk/event-sample/index', 'RiskEventSample', 0, b'1', b'0', b'1', 'admin', '2026-03-12 00:00:00', 'admin', '2026-03-12 00:00:00', b'0'),
(7141, '事件样例查询', 'risk:event-sample:query', 3, 1, 7140, '', '', '', '', 0, b'1', b'1', b'1', 'admin', '2026-03-12 00:00:00', 'admin', '2026-03-12 00:00:00', b'0'),
(7142, '事件样例新增', 'risk:event-sample:create', 3, 2, 7140, '', '', '', '', 0, b'1', b'1', b'1', 'admin', '2026-03-12 00:00:00', 'admin', '2026-03-12 00:00:00', b'0'),
(7143, '事件样例修改', 'risk:event-sample:update', 3, 3, 7140, '', '', '', '', 0, b'1', b'1', b'1', 'admin', '2026-03-12 00:00:00', 'admin', '2026-03-12 00:00:00', b'0'),
(7144, '事件样例删除', 'risk:event-sample:delete', 3, 4, 7140, '', '', '', '', 0, b'1', b'1', b'1', 'admin', '2026-03-12 00:00:00', 'admin', '2026-03-12 00:00:00', b'0'),
(7145, '事件样例预览', 'risk:event-sample:preview', 3, 5, 7140, '', '', '', '', 0, b'1', b'1', b'1', 'admin', '2026-03-12 00:00:00', 'admin', '2026-03-12 00:00:00', b'0'),
(7150, '接入源', 'risk:ingest-source:query', 2, 50, 7100, 'ingest-source', 'ep:link', 'risk/ingest-source/index', 'RiskIngestSource', 0, b'1', b'0', b'1', 'admin', '2026-03-12 00:00:00', 'admin', '2026-03-12 00:00:00', b'0'),
(7151, '接入源查询', 'risk:ingest-source:query', 3, 1, 7150, '', '', '', '', 0, b'1', b'1', b'1', 'admin', '2026-03-12 00:00:00', 'admin', '2026-03-12 00:00:00', b'0'),
(7152, '接入源新增', 'risk:ingest-source:create', 3, 2, 7150, '', '', '', '', 0, b'1', b'1', b'1', 'admin', '2026-03-12 00:00:00', 'admin', '2026-03-12 00:00:00', b'0'),
(7153, '接入源修改', 'risk:ingest-source:update', 3, 3, 7150, '', '', '', '', 0, b'1', b'1', b'1', 'admin', '2026-03-12 00:00:00', 'admin', '2026-03-12 00:00:00', b'0'),
(7154, '接入源启停', 'risk:ingest-source:update-status', 3, 4, 7150, '', '', '', '', 0, b'1', b'1', b'1', 'admin', '2026-03-12 00:00:00', 'admin', '2026-03-12 00:00:00', b'0'),
(7155, '接入源详情', 'risk:ingest-source:get', 3, 5, 7150, '', '', '', '', 0, b'1', b'1', b'1', 'admin', '2026-03-12 00:00:00', 'admin', '2026-03-12 00:00:00', b'0'),
(7160, '字段映射', 'risk:ingest-mapping:query', 2, 60, 7100, 'ingest-mapping', 'ep:sort', 'risk/ingest-mapping/index', 'RiskIngestMapping', 0, b'1', b'0', b'1', 'admin', '2026-03-12 00:00:00', 'admin', '2026-03-12 00:00:00', b'0'),
(7161, '字段映射查询', 'risk:ingest-mapping:query', 3, 1, 7160, '', '', '', '', 0, b'1', b'1', b'1', 'admin', '2026-03-12 00:00:00', 'admin', '2026-03-12 00:00:00', b'0'),
(7162, '字段映射新增', 'risk:ingest-mapping:create', 3, 2, 7160, '', '', '', '', 0, b'1', b'1', b'1', 'admin', '2026-03-12 00:00:00', 'admin', '2026-03-12 00:00:00', b'0'),
(7163, '字段映射修改', 'risk:ingest-mapping:update', 3, 3, 7160, '', '', '', '', 0, b'1', b'1', b'1', 'admin', '2026-03-12 00:00:00', 'admin', '2026-03-12 00:00:00', b'0'),
(7164, '字段映射删除', 'risk:ingest-mapping:delete', 3, 4, 7160, '', '', '', '', 0, b'1', b'1', b'1', 'admin', '2026-03-12 00:00:00', 'admin', '2026-03-12 00:00:00', b'0'),
(7165, '字段映射预览', 'risk:ingest-mapping:preview', 3, 5, 7160, '', '', '', '', 0, b'1', b'1', b'1', 'admin', '2026-03-12 00:00:00', 'admin', '2026-03-12 00:00:00', b'0'),
(7200, '特征规则', '', 1, 20, 7000, 'feature', 'ep:data-analysis', '', '', 0, b'1', b'1', b'1', 'admin', '2026-03-12 00:00:00', 'admin', '2026-03-12 00:00:00', b'0'),
(7210, '名单中心', 'risk:list:query', 2, 10, 7200, 'list', 'ep:collection-tag', 'risk/list/index', 'RiskList', 0, b'1', b'0', b'1', 'admin', '2026-03-12 00:00:00', 'admin', '2026-03-12 00:00:00', b'0'),
(7211, '名单查询', 'risk:list:query', 3, 1, 7210, '', '', '', '', 0, b'1', b'1', b'1', 'admin', '2026-03-12 00:00:00', 'admin', '2026-03-12 00:00:00', b'0'),
(7212, '名单新增', 'risk:list:create', 3, 2, 7210, '', '', '', '', 0, b'1', b'1', b'1', 'admin', '2026-03-12 00:00:00', 'admin', '2026-03-12 00:00:00', b'0'),
(7213, '名单修改', 'risk:list:update', 3, 3, 7210, '', '', '', '', 0, b'1', b'1', b'1', 'admin', '2026-03-12 00:00:00', 'admin', '2026-03-12 00:00:00', b'0'),
(7214, '名单删除', 'risk:list:delete', 3, 4, 7210, '', '', '', '', 0, b'1', b'1', b'1', 'admin', '2026-03-12 00:00:00', 'admin', '2026-03-12 00:00:00', b'0'),
(7215, '名单同步', 'risk:list:sync', 3, 5, 7210, '', '', '', '', 0, b'1', b'1', b'1', 'admin', '2026-03-12 00:00:00', 'admin', '2026-03-12 00:00:00', b'0'),
(7220, '流式特征', 'risk:feature-stream:query', 2, 20, 7200, 'feature-stream', 'ep:trend-charts', 'risk/feature-stream/index', 'RiskFeatureStream', 0, b'1', b'0', b'1', 'admin', '2026-03-12 00:00:00', 'admin', '2026-03-12 00:00:00', b'0'),
(7221, '流式特征查询', 'risk:feature-stream:query', 3, 1, 7220, '', '', '', '', 0, b'1', b'1', b'1', 'admin', '2026-03-12 00:00:00', 'admin', '2026-03-12 00:00:00', b'0'),
(7222, '流式特征新增', 'risk:feature-stream:create', 3, 2, 7220, '', '', '', '', 0, b'1', b'1', b'1', 'admin', '2026-03-12 00:00:00', 'admin', '2026-03-12 00:00:00', b'0'),
(7223, '流式特征修改', 'risk:feature-stream:update', 3, 3, 7220, '', '', '', '', 0, b'1', b'1', b'1', 'admin', '2026-03-12 00:00:00', 'admin', '2026-03-12 00:00:00', b'0'),
(7224, '流式特征删除', 'risk:feature-stream:delete', 3, 4, 7220, '', '', '', '', 0, b'1', b'1', b'1', 'admin', '2026-03-12 00:00:00', 'admin', '2026-03-12 00:00:00', b'0'),
(7225, '流式特征详情', 'risk:feature-stream:get', 3, 5, 7220, '', '', '', '', 0, b'1', b'1', b'1', 'admin', '2026-03-12 00:00:00', 'admin', '2026-03-12 00:00:00', b'0'),
(7230, '查询特征', 'risk:feature-lookup:query', 2, 30, 7200, 'feature-lookup', 'ep:search', 'risk/feature-lookup/index', 'RiskFeatureLookup', 0, b'1', b'0', b'1', 'admin', '2026-03-12 00:00:00', 'admin', '2026-03-12 00:00:00', b'0'),
(7231, '查询特征查询', 'risk:feature-lookup:query', 3, 1, 7230, '', '', '', '', 0, b'1', b'1', b'1', 'admin', '2026-03-12 00:00:00', 'admin', '2026-03-12 00:00:00', b'0'),
(7232, '查询特征新增', 'risk:feature-lookup:create', 3, 2, 7230, '', '', '', '', 0, b'1', b'1', b'1', 'admin', '2026-03-12 00:00:00', 'admin', '2026-03-12 00:00:00', b'0'),
(7233, '查询特征修改', 'risk:feature-lookup:update', 3, 3, 7230, '', '', '', '', 0, b'1', b'1', b'1', 'admin', '2026-03-12 00:00:00', 'admin', '2026-03-12 00:00:00', b'0'),
(7234, '查询特征删除', 'risk:feature-lookup:delete', 3, 4, 7230, '', '', '', '', 0, b'1', b'1', b'1', 'admin', '2026-03-12 00:00:00', 'admin', '2026-03-12 00:00:00', b'0'),
(7235, '查询特征详情', 'risk:feature-lookup:get', 3, 5, 7230, '', '', '', '', 0, b'1', b'1', b'1', 'admin', '2026-03-12 00:00:00', 'admin', '2026-03-12 00:00:00', b'0'),
(7240, '派生特征', 'risk:feature-derived:query', 2, 40, 7200, 'feature-derived', 'ep:cpu', 'risk/feature-derived/index', 'RiskFeatureDerived', 0, b'1', b'0', b'1', 'admin', '2026-03-12 00:00:00', 'admin', '2026-03-12 00:00:00', b'0'),
(7241, '派生特征查询', 'risk:feature-derived:query', 3, 1, 7240, '', '', '', '', 0, b'1', b'1', b'1', 'admin', '2026-03-12 00:00:00', 'admin', '2026-03-12 00:00:00', b'0'),
(7242, '派生特征新增', 'risk:feature-derived:create', 3, 2, 7240, '', '', '', '', 0, b'1', b'1', b'1', 'admin', '2026-03-12 00:00:00', 'admin', '2026-03-12 00:00:00', b'0'),
(7243, '派生特征修改', 'risk:feature-derived:update', 3, 3, 7240, '', '', '', '', 0, b'1', b'1', b'1', 'admin', '2026-03-12 00:00:00', 'admin', '2026-03-12 00:00:00', b'0'),
(7244, '派生特征删除', 'risk:feature-derived:delete', 3, 4, 7240, '', '', '', '', 0, b'1', b'1', b'1', 'admin', '2026-03-12 00:00:00', 'admin', '2026-03-12 00:00:00', b'0'),
(7245, '派生特征校验', 'risk:feature-derived:validate', 3, 5, 7240, '', '', '', '', 0, b'1', b'1', b'1', 'admin', '2026-03-12 00:00:00', 'admin', '2026-03-12 00:00:00', b'0'),
(7250, '规则中心', 'risk:rule:query', 2, 50, 7200, 'rule', 'ep:operation', 'risk/rule/index', 'RiskRule', 0, b'1', b'0', b'1', 'admin', '2026-03-12 00:00:00', 'admin', '2026-03-12 00:00:00', b'0'),
(7251, '规则查询', 'risk:rule:query', 3, 1, 7250, '', '', '', '', 0, b'1', b'1', b'1', 'admin', '2026-03-12 00:00:00', 'admin', '2026-03-12 00:00:00', b'0'),
(7252, '规则新增', 'risk:rule:create', 3, 2, 7250, '', '', '', '', 0, b'1', b'1', b'1', 'admin', '2026-03-12 00:00:00', 'admin', '2026-03-12 00:00:00', b'0'),
(7253, '规则修改', 'risk:rule:update', 3, 3, 7250, '', '', '', '', 0, b'1', b'1', b'1', 'admin', '2026-03-12 00:00:00', 'admin', '2026-03-12 00:00:00', b'0'),
(7254, '规则删除', 'risk:rule:delete', 3, 4, 7250, '', '', '', '', 0, b'1', b'1', b'1', 'admin', '2026-03-12 00:00:00', 'admin', '2026-03-12 00:00:00', b'0'),
(7255, '规则校验', 'risk:rule:validate', 3, 5, 7250, '', '', '', '', 0, b'1', b'1', b'1', 'admin', '2026-03-12 00:00:00', 'admin', '2026-03-12 00:00:00', b'0'),
(7260, '策略中心', 'risk:policy:query', 2, 60, 7200, 'policy', 'ep:set-up', 'risk/policy/index', 'RiskPolicy', 0, b'1', b'0', b'1', 'admin', '2026-03-12 00:00:00', 'admin', '2026-03-12 00:00:00', b'0'),
(7261, '策略查询', 'risk:policy:query', 3, 1, 7260, '', '', '', '', 0, b'1', b'1', b'1', 'admin', '2026-03-12 00:00:00', 'admin', '2026-03-12 00:00:00', b'0'),
(7262, '策略新增', 'risk:policy:create', 3, 2, 7260, '', '', '', '', 0, b'1', b'1', b'1', 'admin', '2026-03-12 00:00:00', 'admin', '2026-03-12 00:00:00', b'0'),
(7263, '策略修改', 'risk:policy:update', 3, 3, 7260, '', '', '', '', 0, b'1', b'1', b'1', 'admin', '2026-03-12 00:00:00', 'admin', '2026-03-12 00:00:00', b'0'),
(7264, '策略删除', 'risk:policy:delete', 3, 4, 7260, '', '', '', '', 0, b'1', b'1', b'1', 'admin', '2026-03-12 00:00:00', 'admin', '2026-03-12 00:00:00', b'0'),
(7265, '策略排序', 'risk:policy:sort', 3, 5, 7260, '', '', '', '', 0, b'1', b'1', b'1', 'admin', '2026-03-12 00:00:00', 'admin', '2026-03-12 00:00:00', b'0'),
(7300, '发布验证', '', 1, 30, 7000, 'release', 'ep:promotion', '', '', 0, b'1', b'1', b'1', 'admin', '2026-03-12 00:00:00', 'admin', '2026-03-12 00:00:00', b'0'),
(7310, '发布中心', 'risk:release:query', 2, 10, 7300, 'center', 'ep:upload-filled', 'risk/release/index', 'RiskRelease', 0, b'1', b'0', b'1', 'admin', '2026-03-12 00:00:00', 'admin', '2026-03-12 00:00:00', b'0'),
(7311, '发布查询', 'risk:release:query', 3, 1, 7310, '', '', '', '', 0, b'1', b'1', b'1', 'admin', '2026-03-12 00:00:00', 'admin', '2026-03-12 00:00:00', b'0'),
(7312, '发布预检', 'risk:release:compile', 3, 2, 7310, '', '', '', '', 0, b'1', b'1', b'1', 'admin', '2026-03-12 00:00:00', 'admin', '2026-03-12 00:00:00', b'0'),
(7313, '发布预览', 'risk:release:preview', 3, 3, 7310, '', '', '', '', 0, b'1', b'1', b'1', 'admin', '2026-03-12 00:00:00', 'admin', '2026-03-12 00:00:00', b'0'),
(7314, '正式发布', 'risk:release:publish', 3, 4, 7310, '', '', '', '', 0, b'1', b'1', b'1', 'admin', '2026-03-12 00:00:00', 'admin', '2026-03-12 00:00:00', b'0'),
(7315, '发布回滚', 'risk:release:rollback', 3, 5, 7310, '', '', '', '', 0, b'1', b'1', b'1', 'admin', '2026-03-12 00:00:00', 'admin', '2026-03-12 00:00:00', b'0'),
(7320, '仿真测试', 'risk:simulation:query', 2, 20, 7300, 'simulation', 'ep:video-play', 'risk/placeholder/index?code=simulation', 'RiskSimulation', 0, b'1', b'0', b'1', 'admin', '2026-03-12 00:00:00', 'admin', '2026-03-12 00:00:00', b'0'),
(7321, '仿真查询', 'risk:simulation:query', 3, 1, 7320, '', '', '', '', 0, b'1', b'1', b'1', 'admin', '2026-03-12 00:00:00', 'admin', '2026-03-12 00:00:00', b'0'),
(7322, '仿真新增', 'risk:simulation:create', 3, 2, 7320, '', '', '', '', 0, b'1', b'1', b'1', 'admin', '2026-03-12 00:00:00', 'admin', '2026-03-12 00:00:00', b'0'),
(7323, '仿真修改', 'risk:simulation:update', 3, 3, 7320, '', '', '', '', 0, b'1', b'1', b'1', 'admin', '2026-03-12 00:00:00', 'admin', '2026-03-12 00:00:00', b'0'),
(7324, '仿真删除', 'risk:simulation:delete', 3, 4, 7320, '', '', '', '', 0, b'1', b'1', b'1', 'admin', '2026-03-12 00:00:00', 'admin', '2026-03-12 00:00:00', b'0'),
(7325, '仿真执行', 'risk:simulation:execute', 3, 5, 7320, '', '', '', '', 0, b'1', b'1', b'1', 'admin', '2026-03-12 00:00:00', 'admin', '2026-03-12 00:00:00', b'0'),
(7400, '运行治理', '', 1, 40, 7000, 'ops', 'ep:histogram', '', '', 0, b'1', b'1', b'1', 'admin', '2026-03-12 00:00:00', 'admin', '2026-03-12 00:00:00', b'0'),
(7410, '决策日志', 'risk:decision-log:query', 2, 10, 7400, 'decision-log', 'ep:tickets', 'risk/placeholder/index?code=decision-log', 'RiskDecisionLog', 0, b'1', b'0', b'1', 'admin', '2026-03-12 00:00:00', 'admin', '2026-03-12 00:00:00', b'0'),
(7411, '决策日志查询', 'risk:decision-log:query', 3, 1, 7410, '', '', '', '', 0, b'1', b'1', b'1', 'admin', '2026-03-12 00:00:00', 'admin', '2026-03-12 00:00:00', b'0'),
(7412, '决策日志详情', 'risk:decision-log:get', 3, 2, 7410, '', '', '', '', 0, b'1', b'1', b'1', 'admin', '2026-03-12 00:00:00', 'admin', '2026-03-12 00:00:00', b'0'),
(7413, '决策日志导出', 'risk:decision-log:export', 3, 3, 7410, '', '', '', '', 0, b'1', b'1', b'1', 'admin', '2026-03-12 00:00:00', 'admin', '2026-03-12 00:00:00', b'0'),
(7414, '命中明细查看', 'risk:decision-log:detail', 3, 4, 7410, '', '', '', '', 0, b'1', b'1', b'1', 'admin', '2026-03-12 00:00:00', 'admin', '2026-03-12 00:00:00', b'0'),
(7420, '接入异常', 'risk:ingest-error:query', 2, 20, 7400, 'ingest-error', 'ep:warning', 'risk/placeholder/index?code=ingest-error', 'RiskIngestError', 0, b'1', b'0', b'1', 'admin', '2026-03-12 00:00:00', 'admin', '2026-03-12 00:00:00', b'0'),
(7421, '接入异常查询', 'risk:ingest-error:query', 3, 1, 7420, '', '', '', '', 0, b'1', b'1', b'1', 'admin', '2026-03-12 00:00:00', 'admin', '2026-03-12 00:00:00', b'0'),
(7422, '接入异常详情', 'risk:ingest-error:get', 3, 2, 7420, '', '', '', '', 0, b'1', b'1', b'1', 'admin', '2026-03-12 00:00:00', 'admin', '2026-03-12 00:00:00', b'0'),
(7423, '接入异常导出', 'risk:ingest-error:export', 3, 3, 7420, '', '', '', '', 0, b'1', b'1', b'1', 'admin', '2026-03-12 00:00:00', 'admin', '2026-03-12 00:00:00', b'0'),
(7430, '监控大盘', 'risk:dashboard:query', 2, 30, 7400, 'dashboard', 'ep:data-line', 'risk/placeholder/index?code=dashboard', 'RiskDashboard', 0, b'1', b'0', b'1', 'admin', '2026-03-12 00:00:00', 'admin', '2026-03-12 00:00:00', b'0'),
(7431, '监控大盘查询', 'risk:dashboard:query', 3, 1, 7430, '', '', '', '', 0, b'1', b'1', b'1', 'admin', '2026-03-12 00:00:00', 'admin', '2026-03-12 00:00:00', b'0'),
(7432, '监控大盘导出', 'risk:dashboard:export', 3, 2, 7430, '', '', '', '', 0, b'1', b'1', b'1', 'admin', '2026-03-12 00:00:00', 'admin', '2026-03-12 00:00:00', b'0'),
(7433, '监控大盘刷新', 'risk:dashboard:refresh', 3, 3, 7430, '', '', '', '', 0, b'1', b'1', b'1', 'admin', '2026-03-12 00:00:00', 'admin', '2026-03-12 00:00:00', b'0'),
(7440, '审计日志', 'risk:audit-log:query', 2, 40, 7400, 'audit-log', 'ep:document-checked', 'risk/placeholder/index?code=audit-log', 'RiskAuditLog', 0, b'1', b'0', b'1', 'admin', '2026-03-12 00:00:00', 'admin', '2026-03-12 00:00:00', b'0'),
(7441, '审计日志查询', 'risk:audit-log:query', 3, 1, 7440, '', '', '', '', 0, b'1', b'1', b'1', 'admin', '2026-03-12 00:00:00', 'admin', '2026-03-12 00:00:00', b'0'),
(7442, '审计日志详情', 'risk:audit-log:get', 3, 2, 7440, '', '', '', '', 0, b'1', b'1', b'1', 'admin', '2026-03-12 00:00:00', 'admin', '2026-03-12 00:00:00', b'0'),
(7443, '审计日志导出', 'risk:audit-log:export', 3, 3, 7440, '', '', '', '', 0, b'1', b'1', b'1', 'admin', '2026-03-12 00:00:00', 'admin', '2026-03-12 00:00:00', b'0'),
(7450, '回放对比', 'risk:replay:query', 2, 50, 7400, 'replay', 'ep:refresh-right', 'risk/placeholder/index?code=replay', 'RiskReplay', 0, b'1', b'0', b'1', 'admin', '2026-03-12 00:00:00', 'admin', '2026-03-12 00:00:00', b'0'),
(7451, '回放查询', 'risk:replay:query', 3, 1, 7450, '', '', '', '', 0, b'1', b'1', b'1', 'admin', '2026-03-12 00:00:00', 'admin', '2026-03-12 00:00:00', b'0'),
(7452, '回放新增', 'risk:replay:create', 3, 2, 7450, '', '', '', '', 0, b'1', b'1', b'1', 'admin', '2026-03-12 00:00:00', 'admin', '2026-03-12 00:00:00', b'0'),
(7453, '回放执行', 'risk:replay:execute', 3, 3, 7450, '', '', '', '', 0, b'1', b'1', b'1', 'admin', '2026-03-12 00:00:00', 'admin', '2026-03-12 00:00:00', b'0'),
(7454, '回放详情', 'risk:replay:get', 3, 4, 7450, '', '', '', '', 0, b'1', b'1', b'1', 'admin', '2026-03-12 00:00:00', 'admin', '2026-03-12 00:00:00', b'0'),
(7455, '回放导出', 'risk:replay:export', 3, 5, 7450, '', '', '', '', 0, b'1', b'1', b'1', 'admin', '2026-03-12 00:00:00', 'admin', '2026-03-12 00:00:00', b'0');

SET FOREIGN_KEY_CHECKS = 1;
