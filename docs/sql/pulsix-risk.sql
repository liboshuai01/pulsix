/*
 pulsix-risk.sql

 基于下列文档整理的一期风险模块初始化 SQL：
 - docs/实时风控系统.md
 - docs/wiki/实时风控系统第7章：控制平台的数据模型设计.md
 - docs/wiki/实时风控系统第21章：数据库、Redis、Kafka 的落地模型设计.md
 - docs/wiki/实时风控系统附录A：完整快照 JSON 示例.md
 - docs/wiki/实时风控系统附录D：第一版 DDL 与 Redis Key 设计.md
 - docs/wiki/实时风控系统附录E：三大风控场景的样例规则与样例事件.md

 设计说明：
 1. 覆盖风险控制面一期核心表，以及主文档中已经明确列出的接入治理、错误治理、告警中心、风险事件查询相关表。
 2. 所有表统一补齐 BaseDO 字段，保持和项目内通用审计字段风格一致。
 3. 接入指引页直接复用 event_schema.sample_event_json、event_field_def、access_source_def，不单独拆表。
 4. 不添加物理外键，避免影响发布快照、历史版本、异步日志与回放数据的灵活写入。
 5. 针对 feature_code 在不同场景下会重复出现的实际情况，feature_*_conf 统一增加 scene_code 作为联合唯一键。
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- =========================
-- Drop Tables
-- =========================

DROP TABLE IF EXISTS `alert_record`;
DROP TABLE IF EXISTS `alert_rule_def`;
DROP TABLE IF EXISTS `alert_template_def`;
DROP TABLE IF EXISTS `alert_channel_def`;
DROP TABLE IF EXISTS `risk_error_log`;
DROP TABLE IF EXISTS `access_auth_conf`;
DROP TABLE IF EXISTS `access_source_def`;
DROP TABLE IF EXISTS `risk_event`;
DROP TABLE IF EXISTS `rule_hit_log`;
DROP TABLE IF EXISTS `decision_log`;
DROP TABLE IF EXISTS `simulation_report`;
DROP TABLE IF EXISTS `simulation_case`;
DROP TABLE IF EXISTS `scene_release_change`;
DROP TABLE IF EXISTS `scene_release`;
DROP TABLE IF EXISTS `policy_rule_ref`;
DROP TABLE IF EXISTS `policy_def`;
DROP TABLE IF EXISTS `rule_def`;
DROP TABLE IF EXISTS `feature_derived_conf`;
DROP TABLE IF EXISTS `feature_lookup_conf`;
DROP TABLE IF EXISTS `feature_stream_conf`;
DROP TABLE IF EXISTS `feature_def`;
DROP TABLE IF EXISTS `list_item`;
DROP TABLE IF EXISTS `list_set`;
DROP TABLE IF EXISTS `event_field_def`;
DROP TABLE IF EXISTS `event_schema`;
DROP TABLE IF EXISTS `entity_type_def`;
DROP TABLE IF EXISTS `scene_def`;

-- =========================
-- Core Metadata
-- =========================

CREATE TABLE `scene_def` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '场景主键',
  `scene_code` varchar(64) NOT NULL COMMENT '场景编码',
  `scene_name` varchar(128) NOT NULL COMMENT '场景名称',
  `scene_type` varchar(64) DEFAULT NULL COMMENT '场景类型',
  `runtime_mode` varchar(32) NOT NULL DEFAULT 'ASYNC_DECISION' COMMENT '运行模式',
  `default_policy_code` varchar(64) DEFAULT NULL COMMENT '默认策略编码',
  `decision_timeout_ms` int NOT NULL DEFAULT 500 COMMENT '决策超时时间毫秒',
  `log_level` varchar(32) NOT NULL DEFAULT 'FULL' COMMENT '日志级别',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '状态',
  `description` varchar(512) DEFAULT NULL COMMENT '描述',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_scene_code` (`scene_code`),
  KEY `idx_scene_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='风控场景定义表';

CREATE TABLE `entity_type_def` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `entity_type` varchar(32) NOT NULL COMMENT '实体类型',
  `entity_name` varchar(64) NOT NULL COMMENT '实体名称',
  `key_field_name` varchar(64) NOT NULL COMMENT '主键字段名',
  `sample_value` varchar(128) DEFAULT NULL COMMENT '样例值',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '状态',
  `description` varchar(512) DEFAULT NULL COMMENT '描述',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_entity_type` (`entity_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='实体类型定义表';

CREATE TABLE `event_schema` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `scene_code` varchar(64) NOT NULL COMMENT '场景编码',
  `event_code` varchar(64) NOT NULL COMMENT '事件编码',
  `event_name` varchar(128) NOT NULL COMMENT '事件名称',
  `event_type` varchar(64) NOT NULL COMMENT '事件类型',
  `source_type` varchar(32) DEFAULT NULL COMMENT '接入类型',
  `topic_name` varchar(128) DEFAULT NULL COMMENT '标准事件 Topic',
  `sample_event_json` json DEFAULT NULL COMMENT '样例事件',
  `version` int NOT NULL DEFAULT 1 COMMENT '版本',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '状态',
  `description` varchar(512) DEFAULT NULL COMMENT '描述',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_scene_event` (`scene_code`, `event_code`),
  KEY `idx_scene_status` (`scene_code`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='事件模型定义表';

CREATE TABLE `event_field_def` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `event_code` varchar(64) NOT NULL COMMENT '事件编码',
  `field_name` varchar(64) NOT NULL COMMENT '字段名',
  `field_label` varchar(128) DEFAULT NULL COMMENT '字段显示名',
  `field_type` varchar(32) NOT NULL COMMENT '字段类型',
  `required_flag` tinyint NOT NULL DEFAULT 0 COMMENT '是否必填',
  `default_value` varchar(256) DEFAULT NULL COMMENT '默认值',
  `sample_value` varchar(512) DEFAULT NULL COMMENT '样例值',
  `description` varchar(512) DEFAULT NULL COMMENT '描述',
  `sort_no` int NOT NULL DEFAULT 0 COMMENT '排序',
  `ext_json` json DEFAULT NULL COMMENT '扩展配置',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_event_field` (`event_code`, `field_name`),
  KEY `idx_event_sort` (`event_code`, `sort_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='事件字段定义表';

CREATE TABLE `list_set` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `scene_code` varchar(64) NOT NULL COMMENT '场景编码',
  `list_code` varchar(64) NOT NULL COMMENT '名单编码',
  `list_name` varchar(128) NOT NULL COMMENT '名单名称',
  `match_type` varchar(32) NOT NULL COMMENT '匹配类型',
  `list_type` varchar(32) NOT NULL COMMENT '名单类型',
  `sync_mode` varchar(32) NOT NULL DEFAULT 'MANUAL' COMMENT '同步模式',
  `redis_key_prefix` varchar(256) DEFAULT NULL COMMENT 'Redis Key 前缀',
  `last_sync_time` datetime DEFAULT NULL COMMENT '最近同步时间',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '状态',
  `description` varchar(512) DEFAULT NULL COMMENT '描述',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_scene_list` (`scene_code`, `list_code`),
  KEY `idx_scene_status` (`scene_code`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='名单集合表';

CREATE TABLE `list_item` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `list_code` varchar(64) NOT NULL COMMENT '名单编码',
  `match_key` varchar(128) DEFAULT NULL COMMENT '匹配字段',
  `match_value` varchar(512) NOT NULL COMMENT '匹配值',
  `source_type` varchar(32) NOT NULL DEFAULT 'MANUAL_IMPORT' COMMENT '来源类型',
  `expire_at` datetime DEFAULT NULL COMMENT '过期时间',
  `tag_json` json DEFAULT NULL COMMENT '标签信息',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '状态',
  `remark` varchar(512) DEFAULT NULL COMMENT '备注',
  `ext_json` json DEFAULT NULL COMMENT '扩展信息',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  PRIMARY KEY (`id`),
  KEY `idx_list_status` (`list_code`, `status`),
  KEY `idx_match_value` (`match_value`(191))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='名单条目表';

-- =========================
-- Feature, Rule, Policy
-- =========================

CREATE TABLE `feature_def` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `scene_code` varchar(64) NOT NULL COMMENT '场景编码',
  `feature_code` varchar(64) NOT NULL COMMENT '特征编码',
  `feature_name` varchar(128) NOT NULL COMMENT '特征名称',
  `feature_type` varchar(32) NOT NULL COMMENT '特征类型',
  `entity_type` varchar(32) DEFAULT NULL COMMENT '实体类型',
  `event_code` varchar(64) DEFAULT NULL COMMENT '所属事件编码',
  `value_type` varchar(32) NOT NULL COMMENT '值类型',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '状态',
  `version` int NOT NULL DEFAULT 1 COMMENT '版本',
  `description` varchar(512) DEFAULT NULL COMMENT '描述',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_scene_feature` (`scene_code`, `feature_code`),
  KEY `idx_scene_feature_type` (`scene_code`, `feature_type`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='特征定义主表';

CREATE TABLE `feature_stream_conf` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `scene_code` varchar(64) NOT NULL COMMENT '场景编码',
  `feature_code` varchar(64) NOT NULL COMMENT '特征编码',
  `source_event_types` varchar(256) NOT NULL COMMENT '来源事件类型',
  `entity_key_expr` varchar(256) NOT NULL COMMENT '实体主键表达式',
  `agg_type` varchar(32) NOT NULL COMMENT '聚合类型',
  `value_expr` varchar(512) DEFAULT NULL COMMENT '聚合值表达式',
  `filter_expr` varchar(1024) DEFAULT NULL COMMENT '过滤表达式',
  `window_type` varchar(32) NOT NULL COMMENT '窗口类型',
  `window_size` varchar(32) NOT NULL COMMENT '窗口大小',
  `window_slide` varchar(32) DEFAULT NULL COMMENT '窗口滑动',
  `include_current_event` tinyint NOT NULL DEFAULT 1 COMMENT '是否包含当前事件',
  `ttl_seconds` bigint DEFAULT NULL COMMENT '状态 TTL 秒数',
  `state_hint_json` json DEFAULT NULL COMMENT '状态提示',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_scene_feature_stream` (`scene_code`, `feature_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='流式特征配置表';

CREATE TABLE `feature_lookup_conf` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `scene_code` varchar(64) NOT NULL COMMENT '场景编码',
  `feature_code` varchar(64) NOT NULL COMMENT '特征编码',
  `lookup_type` varchar(32) NOT NULL COMMENT '查询类型',
  `key_expr` varchar(256) NOT NULL COMMENT '查询 key 表达式',
  `source_ref` varchar(256) NOT NULL COMMENT '数据源标识',
  `default_value` varchar(256) DEFAULT NULL COMMENT '默认值',
  `cache_ttl_seconds` bigint DEFAULT NULL COMMENT '本地缓存 TTL',
  `timeout_ms` int DEFAULT NULL COMMENT '超时时间毫秒',
  `extra_json` json DEFAULT NULL COMMENT '扩展配置',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_scene_feature_lookup` (`scene_code`, `feature_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='查询特征配置表';

CREATE TABLE `feature_derived_conf` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `scene_code` varchar(64) NOT NULL COMMENT '场景编码',
  `feature_code` varchar(64) NOT NULL COMMENT '特征编码',
  `engine_type` varchar(32) NOT NULL COMMENT '表达式引擎',
  `expr_content` text NOT NULL COMMENT '表达式内容',
  `depends_on_json` json DEFAULT NULL COMMENT '依赖特征',
  `value_type` varchar(32) NOT NULL COMMENT '值类型',
  `extra_json` json DEFAULT NULL COMMENT '扩展配置',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_scene_feature_derived` (`scene_code`, `feature_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='派生特征配置表';

CREATE TABLE `rule_def` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `scene_code` varchar(64) NOT NULL COMMENT '场景编码',
  `rule_code` varchar(64) NOT NULL COMMENT '规则编码',
  `rule_name` varchar(128) NOT NULL COMMENT '规则名称',
  `rule_type` varchar(32) NOT NULL DEFAULT 'NORMAL' COMMENT '规则类型',
  `engine_type` varchar(32) NOT NULL COMMENT '引擎类型',
  `expr_content` text NOT NULL COMMENT '规则表达式',
  `depends_on_json` json DEFAULT NULL COMMENT '依赖变量',
  `priority` int NOT NULL DEFAULT 0 COMMENT '优先级',
  `hit_action` varchar(32) NOT NULL COMMENT '命中动作',
  `risk_score` int DEFAULT 0 COMMENT '风险分',
  `alert_enabled_flag` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否开启告警',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '状态',
  `hit_reason_template` varchar(1024) DEFAULT NULL COMMENT '命中原因模板',
  `version` int NOT NULL DEFAULT 1 COMMENT '版本',
  `description` varchar(512) DEFAULT NULL COMMENT '描述',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_scene_rule` (`scene_code`, `rule_code`),
  KEY `idx_scene_priority` (`scene_code`, `priority`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='规则定义表';

CREATE TABLE `policy_def` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `scene_code` varchar(64) NOT NULL COMMENT '场景编码',
  `policy_code` varchar(64) NOT NULL COMMENT '策略编码',
  `policy_name` varchar(128) NOT NULL COMMENT '策略名称',
  `decision_mode` varchar(32) NOT NULL COMMENT '决策模式',
  `default_action` varchar(32) NOT NULL COMMENT '默认动作',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '状态',
  `version` int NOT NULL DEFAULT 1 COMMENT '版本',
  `description` varchar(512) DEFAULT NULL COMMENT '描述',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_scene_policy` (`scene_code`, `policy_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='策略定义表';

CREATE TABLE `policy_rule_ref` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `scene_code` varchar(64) NOT NULL COMMENT '场景编码',
  `policy_code` varchar(64) NOT NULL COMMENT '策略编码',
  `rule_code` varchar(64) NOT NULL COMMENT '规则编码',
  `order_no` int NOT NULL DEFAULT 0 COMMENT '顺序',
  `enabled_flag` tinyint NOT NULL DEFAULT 1 COMMENT '是否启用',
  `branch_expr` varchar(1024) DEFAULT NULL COMMENT '分支表达式',
  `score_weight` int DEFAULT NULL COMMENT '评分权重',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_scene_policy_rule` (`scene_code`, `policy_code`, `rule_code`),
  KEY `idx_scene_policy_order` (`scene_code`, `policy_code`, `order_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='策略规则关联表';

-- =========================
-- Release, Simulation, Query
-- =========================

CREATE TABLE `scene_release` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `scene_code` varchar(64) NOT NULL COMMENT '场景编码',
  `version_no` int NOT NULL COMMENT '版本号',
  `checksum` varchar(128) NOT NULL COMMENT '校验和',
  `publish_status` varchar(32) NOT NULL COMMENT '发布状态',
  `snapshot_json` json NOT NULL COMMENT '快照 JSON',
  `compile_report_json` json DEFAULT NULL COMMENT '编译报告',
  `effective_from` datetime DEFAULT NULL COMMENT '生效时间',
  `published_by` varchar(64) DEFAULT NULL COMMENT '发布人',
  `published_at` datetime DEFAULT NULL COMMENT '发布时间',
  `rollback_from_version` int DEFAULT NULL COMMENT '回滚来源版本',
  `remark` varchar(512) DEFAULT NULL COMMENT '备注',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_scene_version` (`scene_code`, `version_no`),
  KEY `idx_scene_publish_time` (`scene_code`, `published_at`),
  KEY `idx_scene_publish_status` (`scene_code`, `publish_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='场景发布表';

CREATE TABLE `scene_release_change` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `release_id` bigint NOT NULL COMMENT '发布记录主键',
  `scene_code` varchar(64) NOT NULL COMMENT '场景编码',
  `change_type` varchar(32) NOT NULL COMMENT '变更类型',
  `target_type` varchar(32) NOT NULL COMMENT '目标类型',
  `target_code` varchar(64) NOT NULL COMMENT '目标编码',
  `before_json` json DEFAULT NULL COMMENT '变更前',
  `after_json` json DEFAULT NULL COMMENT '变更后',
  `description` varchar(512) DEFAULT NULL COMMENT '描述',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  PRIMARY KEY (`id`),
  KEY `idx_release_id` (`release_id`),
  KEY `idx_scene_target` (`scene_code`, `target_type`, `target_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='发布变更明细表';

CREATE TABLE `simulation_case` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `scene_code` varchar(64) NOT NULL COMMENT '场景编码',
  `case_code` varchar(64) NOT NULL COMMENT '用例编码',
  `case_name` varchar(128) NOT NULL COMMENT '用例名称',
  `input_event_json` json NOT NULL COMMENT '输入事件',
  `context_json` json DEFAULT NULL COMMENT '上下文假设',
  `expected_action` varchar(32) DEFAULT NULL COMMENT '期望动作',
  `expected_hit_rules` json DEFAULT NULL COMMENT '期望命中规则',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '状态',
  `description` varchar(512) DEFAULT NULL COMMENT '描述',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_scene_case` (`scene_code`, `case_code`),
  KEY `idx_scene_case_status` (`scene_code`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='仿真用例表';

CREATE TABLE `simulation_report` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `case_id` bigint NOT NULL COMMENT '用例主键',
  `scene_code` varchar(64) NOT NULL COMMENT '场景编码',
  `case_code` varchar(64) NOT NULL COMMENT '用例编码',
  `release_id` bigint DEFAULT NULL COMMENT '发布版本主键',
  `version_no` int NOT NULL COMMENT '执行版本',
  `actual_action` varchar(32) DEFAULT NULL COMMENT '实际动作',
  `actual_hit_rules` json DEFAULT NULL COMMENT '实际命中规则',
  `feature_snapshot_json` json DEFAULT NULL COMMENT '特征快照',
  `result_json` json NOT NULL COMMENT '执行结果',
  `pass_flag` tinyint NOT NULL COMMENT '是否通过',
  `duration_ms` bigint DEFAULT NULL COMMENT '耗时毫秒',
  `run_by` varchar(64) DEFAULT NULL COMMENT '执行人',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  PRIMARY KEY (`id`),
  KEY `idx_case_version` (`case_id`, `version_no`),
  KEY `idx_scene_report_time` (`scene_code`, `create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='仿真报告表';

CREATE TABLE `decision_log` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `trace_id` varchar(128) NOT NULL COMMENT '链路追踪号',
  `event_id` varchar(128) NOT NULL COMMENT '事件编号',
  `scene_code` varchar(64) NOT NULL COMMENT '场景编码',
  `source_code` varchar(64) DEFAULT NULL COMMENT '接入源编码',
  `entity_id` varchar(128) DEFAULT NULL COMMENT '实体主键',
  `final_action` varchar(32) NOT NULL COMMENT '最终动作',
  `final_score` int DEFAULT NULL COMMENT '最终分值',
  `version_no` int NOT NULL COMMENT '执行版本',
  `latency_ms` bigint DEFAULT NULL COMMENT '耗时毫秒',
  `risk_level` varchar(32) DEFAULT NULL COMMENT '风险等级',
  `event_time` datetime NOT NULL COMMENT '事件时间',
  `input_json` json DEFAULT NULL COMMENT '输入报文',
  `feature_snapshot_json` json DEFAULT NULL COMMENT '特征快照',
  `hit_rules_json` json DEFAULT NULL COMMENT '命中规则',
  `remark` varchar(512) DEFAULT NULL COMMENT '备注',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_scene_event_id` (`scene_code`, `event_id`),
  KEY `idx_trace_id` (`trace_id`),
  KEY `idx_scene_event_time` (`scene_code`, `event_time`),
  KEY `idx_action_time` (`final_action`, `event_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='决策日志表';

CREATE TABLE `rule_hit_log` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `decision_id` bigint NOT NULL COMMENT '决策日志主键',
  `rule_code` varchar(64) NOT NULL COMMENT '规则编码',
  `hit_flag` tinyint NOT NULL COMMENT '是否命中',
  `hit_action` varchar(32) DEFAULT NULL COMMENT '命中动作',
  `score` int DEFAULT NULL COMMENT '风险分',
  `hit_reason` varchar(1024) DEFAULT NULL COMMENT '命中原因',
  `hit_value_json` json DEFAULT NULL COMMENT '命中值',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  PRIMARY KEY (`id`),
  KEY `idx_decision_rule` (`decision_id`, `rule_code`),
  KEY `idx_rule_time` (`rule_code`, `create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='规则命中明细表';

CREATE TABLE `risk_event` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `risk_event_no` varchar(64) NOT NULL COMMENT '风险事件编号',
  `scene_code` varchar(64) NOT NULL COMMENT '场景编码',
  `trace_id` varchar(128) DEFAULT NULL COMMENT '链路追踪号',
  `event_id` varchar(128) DEFAULT NULL COMMENT '事件编号',
  `decision_id` bigint DEFAULT NULL COMMENT '决策日志主键',
  `source_type` varchar(32) NOT NULL COMMENT '来源类型',
  `final_action` varchar(32) NOT NULL COMMENT '最终动作',
  `risk_level` varchar(32) NOT NULL COMMENT '风险等级',
  `version_no` int DEFAULT NULL COMMENT '版本号',
  `hit_rules_json` json DEFAULT NULL COMMENT '命中规则',
  `hit_summary` varchar(1024) DEFAULT NULL COMMENT '摘要',
  `status` varchar(32) NOT NULL DEFAULT 'OPEN' COMMENT '处理状态',
  `assigned_to` varchar(64) DEFAULT NULL COMMENT '指派给',
  `first_seen_time` datetime DEFAULT NULL COMMENT '首次发现时间',
  `last_seen_time` datetime DEFAULT NULL COMMENT '最近发现时间',
  `ext_json` json DEFAULT NULL COMMENT '扩展信息',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_risk_event_no` (`risk_event_no`),
  KEY `idx_scene_status` (`scene_code`, `status`),
  KEY `idx_action_time` (`final_action`, `last_seen_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='风险事件沉淀表';

-- =========================
-- Access Governance, Error Governance, Alerting
-- =========================

CREATE TABLE `access_source_def` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `source_code` varchar(64) NOT NULL COMMENT '接入源编码',
  `source_name` varchar(128) NOT NULL COMMENT '接入源名称',
  `source_type` varchar(32) NOT NULL COMMENT '接入源类型',
  `access_protocol` varchar(32) NOT NULL COMMENT '接入协议',
  `app_id` varchar(64) DEFAULT NULL COMMENT '应用标识',
  `owner_name` varchar(64) DEFAULT NULL COMMENT '负责人',
  `contact_email` varchar(128) DEFAULT NULL COMMENT '联系邮箱',
  `rate_limit_qps` int DEFAULT NULL COMMENT '限流 QPS',
  `allowed_scene_codes_json` json DEFAULT NULL COMMENT '允许场景',
  `ip_whitelist_json` json DEFAULT NULL COMMENT 'IP 白名单',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '状态',
  `description` varchar(512) DEFAULT NULL COMMENT '描述',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_source_code` (`source_code`),
  KEY `idx_source_type_status` (`source_type`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='接入源定义表';

CREATE TABLE `access_auth_conf` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `source_code` varchar(64) NOT NULL COMMENT '接入源编码',
  `auth_type` varchar(32) NOT NULL COMMENT '鉴权类型',
  `app_key` varchar(128) DEFAULT NULL COMMENT '应用 Key',
  `app_secret` varchar(255) DEFAULT NULL COMMENT '应用 Secret',
  `sign_algo` varchar(64) DEFAULT NULL COMMENT '签名算法',
  `signature_header` varchar(64) DEFAULT NULL COMMENT '签名 Header',
  `nonce_ttl_seconds` int DEFAULT NULL COMMENT '随机数 TTL 秒数',
  `effective_from` datetime DEFAULT NULL COMMENT '生效时间',
  `expire_at` datetime DEFAULT NULL COMMENT '过期时间',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '状态',
  `ext_json` json DEFAULT NULL COMMENT '扩展配置',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_auth_source` (`source_code`),
  KEY `idx_app_key` (`app_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='接入鉴权配置表';

CREATE TABLE `risk_error_log` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `trace_id` varchar(128) DEFAULT NULL COMMENT '链路追踪号',
  `event_id` varchar(128) DEFAULT NULL COMMENT '事件编号',
  `source_code` varchar(64) DEFAULT NULL COMMENT '接入源编码',
  `scene_code` varchar(64) DEFAULT NULL COMMENT '场景编码',
  `source_topic` varchar(128) DEFAULT NULL COMMENT '来源 Topic',
  `error_stage` varchar(32) NOT NULL COMMENT '错误阶段',
  `error_type` varchar(64) NOT NULL COMMENT '错误类型',
  `error_code` varchar(64) DEFAULT NULL COMMENT '错误码',
  `error_level` varchar(32) NOT NULL COMMENT '错误级别',
  `error_message` varchar(1024) NOT NULL COMMENT '错误信息',
  `raw_payload` longtext COMMENT '原始报文',
  `standard_payload_json` json DEFAULT NULL COMMENT '标准化报文',
  `tag_json` json DEFAULT NULL COMMENT '标签',
  `process_status` varchar(32) NOT NULL DEFAULT 'OPEN' COMMENT '处理状态',
  `process_remark` varchar(512) DEFAULT NULL COMMENT '处理备注',
  `event_time` datetime NOT NULL COMMENT '事件时间',
  `resolved_by` varchar(64) DEFAULT NULL COMMENT '处理人',
  `resolved_time` datetime DEFAULT NULL COMMENT '处理时间',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  PRIMARY KEY (`id`),
  KEY `idx_source_time` (`source_code`, `event_time`),
  KEY `idx_error_stage_time` (`error_stage`, `event_time`),
  KEY `idx_trace_id` (`trace_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='错误治理日志表';

CREATE TABLE `alert_channel_def` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `channel_code` varchar(64) NOT NULL COMMENT '渠道编码',
  `channel_name` varchar(128) NOT NULL COMMENT '渠道名称',
  `channel_type` varchar(32) NOT NULL COMMENT '渠道类型',
  `webhook_url` varchar(1024) DEFAULT NULL COMMENT 'Webhook 地址',
  `secret` varchar(255) DEFAULT NULL COMMENT '密钥',
  `headers_json` json DEFAULT NULL COMMENT '请求头',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '状态',
  `description` varchar(512) DEFAULT NULL COMMENT '描述',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_channel_code` (`channel_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='告警渠道定义表';

CREATE TABLE `alert_template_def` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `template_code` varchar(64) NOT NULL COMMENT '模板编码',
  `template_name` varchar(128) NOT NULL COMMENT '模板名称',
  `source_type` varchar(32) NOT NULL COMMENT '数据来源',
  `title_template` varchar(255) NOT NULL COMMENT '标题模板',
  `content_template` text NOT NULL COMMENT '内容模板',
  `format_type` varchar(32) NOT NULL DEFAULT 'TEXT' COMMENT '模板格式',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '状态',
  `description` varchar(512) DEFAULT NULL COMMENT '描述',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_template_code` (`template_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='告警模板定义表';

CREATE TABLE `alert_rule_def` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `alert_rule_code` varchar(64) NOT NULL COMMENT '告警规则编码',
  `alert_rule_name` varchar(128) NOT NULL COMMENT '告警规则名称',
  `scene_code` varchar(64) DEFAULT NULL COMMENT '场景编码',
  `source_type` varchar(32) NOT NULL COMMENT '来源类型',
  `action_filter_json` json DEFAULT NULL COMMENT '动作过滤',
  `error_stage_filter_json` json DEFAULT NULL COMMENT '错误阶段过滤',
  `alert_level` varchar(32) NOT NULL COMMENT '告警级别',
  `dedupe_window_seconds` int NOT NULL DEFAULT 0 COMMENT '去重窗口秒数',
  `rate_limit_json` json DEFAULT NULL COMMENT '限流配置',
  `silence_conf_json` json DEFAULT NULL COMMENT '静默配置',
  `template_code` varchar(64) NOT NULL COMMENT '模板编码',
  `channel_codes_json` json NOT NULL COMMENT '渠道列表',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '状态',
  `description` varchar(512) DEFAULT NULL COMMENT '描述',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_alert_rule_code` (`alert_rule_code`),
  KEY `idx_scene_source_status` (`scene_code`, `source_type`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='告警规则定义表';

CREATE TABLE `alert_record` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `alert_rule_code` varchar(64) NOT NULL COMMENT '告警规则编码',
  `source_type` varchar(32) NOT NULL COMMENT '来源类型',
  `source_id` varchar(64) DEFAULT NULL COMMENT '来源记录主键',
  `trace_id` varchar(128) DEFAULT NULL COMMENT '链路追踪号',
  `event_id` varchar(128) DEFAULT NULL COMMENT '事件编号',
  `scene_code` varchar(64) DEFAULT NULL COMMENT '场景编码',
  `alert_level` varchar(32) NOT NULL COMMENT '告警级别',
  `channel_code` varchar(64) NOT NULL COMMENT '发送渠道',
  `template_code` varchar(64) NOT NULL COMMENT '模板编码',
  `send_status` varchar(32) NOT NULL COMMENT '发送状态',
  `dedupe_key` varchar(255) DEFAULT NULL COMMENT '去重 Key',
  `dedupe_hit_flag` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否命中去重',
  `silence_hit_flag` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否命中静默',
  `request_payload` json DEFAULT NULL COMMENT '发送请求',
  `response_payload` json DEFAULT NULL COMMENT '发送响应',
  `sent_at` datetime DEFAULT NULL COMMENT '发送时间',
  `remark` varchar(512) DEFAULT NULL COMMENT '备注',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  PRIMARY KEY (`id`),
  KEY `idx_alert_rule_time` (`alert_rule_code`, `sent_at`),
  KEY `idx_trace_id` (`trace_id`),
  KEY `idx_scene_send_status` (`scene_code`, `send_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='告警发送记录表';

-- =========================
-- Seed Data
-- =========================

BEGIN;

INSERT INTO `scene_def` (`id`, `scene_code`, `scene_name`, `scene_type`, `runtime_mode`, `default_policy_code`, `decision_timeout_ms`, `log_level`, `status`, `description`, `creator`, `create_time`, `updater`, `update_time`, `deleted`) VALUES
(1, 'LOGIN_RISK', '登录风控', 'LOGIN', 'ASYNC_DECISION', 'LOGIN_RISK_POLICY', 500, 'FULL', 1, '用于撞库、爆破密码、黑产设备识别等登录风控场景', 'admin', '2026-03-07 08:50:00', 'admin', '2026-03-07 08:50:00', b'0'),
(2, 'REGISTER_ANTI_FRAUD', '注册反作弊', 'REGISTER', 'ASYNC_DECISION', 'REGISTER_ANTI_FRAUD_POLICY', 500, 'FULL', 1, '用于同设备养号、代理 IP 批量注册、风险手机号识别', 'admin', '2026-03-07 09:30:00', 'admin', '2026-03-07 09:30:00', b'0'),
(3, 'TRADE_RISK', '交易风控', 'TRADE', 'ASYNC_DECISION', 'TRADE_RISK_POLICY_FIRST_HIT', 500, 'FULL', 1, '一期默认验收场景，覆盖高频大额、黑名单设备与高风险用户设备联动', 'admin', '2026-03-07 10:20:00', 'admin', '2026-03-07 10:20:00', b'0');

INSERT INTO `entity_type_def` (`id`, `entity_type`, `entity_name`, `key_field_name`, `sample_value`, `status`, `description`, `creator`, `create_time`, `updater`, `update_time`, `deleted`) VALUES
(1, 'USER', '用户', 'userId', 'U1001', 1, '用户维度，适用于交易、登录、注册等场景', 'admin', '2026-03-07 08:55:00', 'admin', '2026-03-07 08:55:00', b'0'),
(2, 'DEVICE', '设备', 'deviceId', 'D9001', 1, '设备维度，适用于设备黑名单、多账号关联', 'admin', '2026-03-07 08:55:00', 'admin', '2026-03-07 08:55:00', b'0'),
(3, 'IP', 'IP 地址', 'ip', '10.20.30.40', 1, 'IP 维度，适用于 IP 爆破与代理识别', 'admin', '2026-03-07 08:55:00', 'admin', '2026-03-07 08:55:00', b'0'),
(4, 'MOBILE', '手机号', 'mobile', '13800001111', 1, '手机号维度，适用于风险号码识别', 'admin', '2026-03-07 08:55:00', 'admin', '2026-03-07 08:55:00', b'0'),
(5, 'MERCHANT', '商户', 'merchantId', 'M1001', 1, '商户维度，适用于交易场景商户风险等级', 'admin', '2026-03-07 08:55:00', 'admin', '2026-03-07 08:55:00', b'0'),
(6, 'MOBILE_PREFIX', '手机号段', 'mobilePrefix', '1380000', 1, '注册场景下的号段聚合口径', 'admin', '2026-03-07 08:55:00', 'admin', '2026-03-07 08:55:00', b'0');

INSERT INTO `event_schema` (`id`, `scene_code`, `event_code`, `event_name`, `event_type`, `source_type`, `topic_name`, `sample_event_json`, `version`, `status`, `description`, `creator`, `create_time`, `updater`, `update_time`, `deleted`) VALUES
(1, 'LOGIN_RISK', 'LOGIN_EVENT', '登录事件', 'login', 'HTTP', 'pulsix.event.standard', '{"eventId":"E_LOGIN_0001","traceId":"T_LOGIN_0001","sceneCode":"LOGIN_RISK","eventType":"login","eventTime":"2026-03-07T09:00:00","userId":"U1001","deviceId":"D9001","ip":"10.20.30.40","channel":"APP","loginResult":"FAIL","failReason":"PASSWORD_ERROR","city":"Shanghai","province":"Shanghai","ext":{"os":"Android","appVersion":"1.0.2"}}', 1, 1, '登录风控标准事件模型', 'admin', '2026-03-07 09:00:00', 'admin', '2026-03-07 09:00:00', b'0'),
(2, 'REGISTER_ANTI_FRAUD', 'REGISTER_EVENT', '注册事件', 'register', 'HTTP', 'pulsix.event.standard', '{"eventId":"E_REG_0001","traceId":"T_REG_0001","sceneCode":"REGISTER_ANTI_FRAUD","eventType":"register","eventTime":"2026-03-07T10:00:00","userId":"U3001","deviceId":"D_REG_001","ip":"22.33.44.55","channel":"APP","mobile":"13800001111","inviteCode":"INV1001","registerResult":"SUCCESS","ext":{"appVersion":"2.0.1","os":"iOS"}}', 1, 1, '注册反作弊标准事件模型', 'admin', '2026-03-07 10:00:00', 'admin', '2026-03-07 10:00:00', b'0'),
(3, 'TRADE_RISK', 'TRADE_EVENT', '交易事件', 'trade', 'HTTP', 'pulsix.event.standard', '{"eventId":"E_TRADE_0001","traceId":"T_TRADE_0001","sceneCode":"TRADE_RISK","eventType":"trade","eventTime":"2026-03-07T11:00:00","userId":"U5001","deviceId":"D5001","ip":"66.77.88.99","channel":"APP","amount":6800,"currency":"CNY","tradeResult":"SUCCESS","merchantId":"M1001","payMethod":"CARD","ext":{"city":"Shanghai"}}', 1, 1, '交易风控标准事件模型', 'admin', '2026-03-07 11:00:00', 'admin', '2026-03-07 11:00:00', b'0');

INSERT INTO `event_field_def` (`id`, `event_code`, `field_name`, `field_label`, `field_type`, `required_flag`, `default_value`, `sample_value`, `description`, `sort_no`, `ext_json`, `creator`, `create_time`, `updater`, `update_time`, `deleted`) VALUES
(101, 'LOGIN_EVENT', 'eventId', '事件ID', 'STRING', 1, NULL, 'E_LOGIN_0001', '事件唯一标识', 1, NULL, 'admin', '2026-03-07 09:01:00', 'admin', '2026-03-07 09:01:00', b'0'),
(102, 'LOGIN_EVENT', 'traceId', '链路ID', 'STRING', 1, NULL, 'T_LOGIN_0001', '全链路追踪号', 2, NULL, 'admin', '2026-03-07 09:01:00', 'admin', '2026-03-07 09:01:00', b'0'),
(103, 'LOGIN_EVENT', 'sceneCode', '场景编码', 'STRING', 1, 'LOGIN_RISK', 'LOGIN_RISK', '场景编码', 3, NULL, 'admin', '2026-03-07 09:01:00', 'admin', '2026-03-07 09:01:00', b'0'),
(104, 'LOGIN_EVENT', 'eventType', '事件类型', 'STRING', 1, 'login', 'login', '事件类型', 4, NULL, 'admin', '2026-03-07 09:01:00', 'admin', '2026-03-07 09:01:00', b'0'),
(105, 'LOGIN_EVENT', 'eventTime', '事件时间', 'DATETIME', 1, NULL, '2026-03-07T09:00:00', '事件发生时间', 5, NULL, 'admin', '2026-03-07 09:01:00', 'admin', '2026-03-07 09:01:00', b'0'),
(106, 'LOGIN_EVENT', 'userId', '用户ID', 'STRING', 1, NULL, 'U1001', '用户主键', 6, NULL, 'admin', '2026-03-07 09:01:00', 'admin', '2026-03-07 09:01:00', b'0'),
(107, 'LOGIN_EVENT', 'deviceId', '设备ID', 'STRING', 1, NULL, 'D9001', '设备主键', 7, NULL, 'admin', '2026-03-07 09:01:00', 'admin', '2026-03-07 09:01:00', b'0'),
(108, 'LOGIN_EVENT', 'ip', 'IP', 'STRING', 1, NULL, '10.20.30.40', '客户端 IP', 8, NULL, 'admin', '2026-03-07 09:01:00', 'admin', '2026-03-07 09:01:00', b'0'),
(109, 'LOGIN_EVENT', 'channel', '渠道', 'STRING', 1, 'APP', 'APP', '请求渠道', 9, NULL, 'admin', '2026-03-07 09:01:00', 'admin', '2026-03-07 09:01:00', b'0'),
(110, 'LOGIN_EVENT', 'loginResult', '登录结果', 'STRING', 1, NULL, 'FAIL', 'SUCCESS/FAIL', 10, NULL, 'admin', '2026-03-07 09:01:00', 'admin', '2026-03-07 09:01:00', b'0'),
(111, 'LOGIN_EVENT', 'failReason', '失败原因', 'STRING', 0, NULL, 'PASSWORD_ERROR', '失败时的原因', 11, NULL, 'admin', '2026-03-07 09:01:00', 'admin', '2026-03-07 09:01:00', b'0'),
(112, 'LOGIN_EVENT', 'city', '城市', 'STRING', 0, NULL, 'Shanghai', '定位城市', 12, NULL, 'admin', '2026-03-07 09:01:00', 'admin', '2026-03-07 09:01:00', b'0'),
(113, 'LOGIN_EVENT', 'province', '省份', 'STRING', 0, NULL, 'Shanghai', '定位省份', 13, NULL, 'admin', '2026-03-07 09:01:00', 'admin', '2026-03-07 09:01:00', b'0'),
(114, 'LOGIN_EVENT', 'ext', '扩展字段', 'JSON', 0, NULL, '{"os":"Android","appVersion":"1.0.2"}', '扩展信息', 14, NULL, 'admin', '2026-03-07 09:01:00', 'admin', '2026-03-07 09:01:00', b'0'),
(201, 'REGISTER_EVENT', 'eventId', '事件ID', 'STRING', 1, NULL, 'E_REG_0001', '事件唯一标识', 1, NULL, 'admin', '2026-03-07 10:01:00', 'admin', '2026-03-07 10:01:00', b'0'),
(202, 'REGISTER_EVENT', 'traceId', '链路ID', 'STRING', 1, NULL, 'T_REG_0001', '全链路追踪号', 2, NULL, 'admin', '2026-03-07 10:01:00', 'admin', '2026-03-07 10:01:00', b'0'),
(203, 'REGISTER_EVENT', 'sceneCode', '场景编码', 'STRING', 1, 'REGISTER_ANTI_FRAUD', 'REGISTER_ANTI_FRAUD', '场景编码', 3, NULL, 'admin', '2026-03-07 10:01:00', 'admin', '2026-03-07 10:01:00', b'0'),
(204, 'REGISTER_EVENT', 'eventType', '事件类型', 'STRING', 1, 'register', 'register', '事件类型', 4, NULL, 'admin', '2026-03-07 10:01:00', 'admin', '2026-03-07 10:01:00', b'0'),
(205, 'REGISTER_EVENT', 'eventTime', '事件时间', 'DATETIME', 1, NULL, '2026-03-07T10:00:00', '事件发生时间', 5, NULL, 'admin', '2026-03-07 10:01:00', 'admin', '2026-03-07 10:01:00', b'0'),
(206, 'REGISTER_EVENT', 'userId', '用户ID', 'STRING', 1, NULL, 'U3001', '用户主键', 6, NULL, 'admin', '2026-03-07 10:01:00', 'admin', '2026-03-07 10:01:00', b'0'),
(207, 'REGISTER_EVENT', 'deviceId', '设备ID', 'STRING', 1, NULL, 'D_REG_001', '设备主键', 7, NULL, 'admin', '2026-03-07 10:01:00', 'admin', '2026-03-07 10:01:00', b'0'),
(208, 'REGISTER_EVENT', 'ip', 'IP', 'STRING', 1, NULL, '22.33.44.55', '客户端 IP', 8, NULL, 'admin', '2026-03-07 10:01:00', 'admin', '2026-03-07 10:01:00', b'0'),
(209, 'REGISTER_EVENT', 'channel', '渠道', 'STRING', 1, 'APP', 'APP', '请求渠道', 9, NULL, 'admin', '2026-03-07 10:01:00', 'admin', '2026-03-07 10:01:00', b'0'),
(210, 'REGISTER_EVENT', 'mobile', '手机号', 'STRING', 1, NULL, '13800001111', '手机号', 10, NULL, 'admin', '2026-03-07 10:01:00', 'admin', '2026-03-07 10:01:00', b'0'),
(211, 'REGISTER_EVENT', 'inviteCode', '邀请码', 'STRING', 0, NULL, 'INV1001', '邀请码', 11, NULL, 'admin', '2026-03-07 10:01:00', 'admin', '2026-03-07 10:01:00', b'0'),
(212, 'REGISTER_EVENT', 'registerResult', '注册结果', 'STRING', 1, NULL, 'SUCCESS', 'SUCCESS/FAIL', 12, NULL, 'admin', '2026-03-07 10:01:00', 'admin', '2026-03-07 10:01:00', b'0'),
(213, 'REGISTER_EVENT', 'ext', '扩展字段', 'JSON', 0, NULL, '{"appVersion":"2.0.1","os":"iOS"}', '扩展信息', 13, NULL, 'admin', '2026-03-07 10:01:00', 'admin', '2026-03-07 10:01:00', b'0'),
(301, 'TRADE_EVENT', 'eventId', '事件ID', 'STRING', 1, NULL, 'E_TRADE_0001', '事件唯一标识', 1, NULL, 'admin', '2026-03-07 11:01:00', 'admin', '2026-03-07 11:01:00', b'0'),
(302, 'TRADE_EVENT', 'traceId', '链路ID', 'STRING', 1, NULL, 'T_TRADE_0001', '全链路追踪号', 2, NULL, 'admin', '2026-03-07 11:01:00', 'admin', '2026-03-07 11:01:00', b'0'),
(303, 'TRADE_EVENT', 'sceneCode', '场景编码', 'STRING', 1, 'TRADE_RISK', 'TRADE_RISK', '场景编码', 3, NULL, 'admin', '2026-03-07 11:01:00', 'admin', '2026-03-07 11:01:00', b'0'),
(304, 'TRADE_EVENT', 'eventType', '事件类型', 'STRING', 1, 'trade', 'trade', '事件类型', 4, NULL, 'admin', '2026-03-07 11:01:00', 'admin', '2026-03-07 11:01:00', b'0'),
(305, 'TRADE_EVENT', 'eventTime', '事件时间', 'DATETIME', 1, NULL, '2026-03-07T11:00:00', '事件发生时间', 5, NULL, 'admin', '2026-03-07 11:01:00', 'admin', '2026-03-07 11:01:00', b'0'),
(306, 'TRADE_EVENT', 'userId', '用户ID', 'STRING', 1, NULL, 'U5001', '用户主键', 6, NULL, 'admin', '2026-03-07 11:01:00', 'admin', '2026-03-07 11:01:00', b'0'),
(307, 'TRADE_EVENT', 'deviceId', '设备ID', 'STRING', 1, NULL, 'D5001', '设备主键', 7, NULL, 'admin', '2026-03-07 11:01:00', 'admin', '2026-03-07 11:01:00', b'0'),
(308, 'TRADE_EVENT', 'ip', 'IP', 'STRING', 1, NULL, '66.77.88.99', '客户端 IP', 8, NULL, 'admin', '2026-03-07 11:01:00', 'admin', '2026-03-07 11:01:00', b'0'),
(309, 'TRADE_EVENT', 'channel', '渠道', 'STRING', 1, 'APP', 'APP', '请求渠道', 9, NULL, 'admin', '2026-03-07 11:01:00', 'admin', '2026-03-07 11:01:00', b'0'),
(310, 'TRADE_EVENT', 'amount', '交易金额', 'DECIMAL', 1, NULL, '6800', '交易金额', 10, NULL, 'admin', '2026-03-07 11:01:00', 'admin', '2026-03-07 11:01:00', b'0'),
(311, 'TRADE_EVENT', 'currency', '币种', 'STRING', 1, 'CNY', 'CNY', '币种', 11, NULL, 'admin', '2026-03-07 11:01:00', 'admin', '2026-03-07 11:01:00', b'0'),
(312, 'TRADE_EVENT', 'tradeResult', '交易结果', 'STRING', 1, NULL, 'SUCCESS', 'SUCCESS/FAIL', 12, NULL, 'admin', '2026-03-07 11:01:00', 'admin', '2026-03-07 11:01:00', b'0'),
(313, 'TRADE_EVENT', 'merchantId', '商户ID', 'STRING', 1, NULL, 'M1001', '商户编号', 13, NULL, 'admin', '2026-03-07 11:01:00', 'admin', '2026-03-07 11:01:00', b'0'),
(314, 'TRADE_EVENT', 'payMethod', '支付方式', 'STRING', 1, NULL, 'CARD', '支付方式', 14, NULL, 'admin', '2026-03-07 11:01:00', 'admin', '2026-03-07 11:01:00', b'0'),
(315, 'TRADE_EVENT', 'ext', '扩展字段', 'JSON', 0, NULL, '{"city":"Shanghai"}', '扩展信息', 15, NULL, 'admin', '2026-03-07 11:01:00', 'admin', '2026-03-07 11:01:00', b'0');

INSERT INTO `list_set` (`id`, `scene_code`, `list_code`, `list_name`, `match_type`, `list_type`, `sync_mode`, `redis_key_prefix`, `last_sync_time`, `status`, `description`, `creator`, `create_time`, `updater`, `update_time`, `deleted`) VALUES
(1, 'LOGIN_RISK', 'LOGIN_DEVICE_BLACKLIST', '登录设备黑名单', 'DEVICE', 'BLACK', 'MANUAL', 'pulsix:list:black:device', '2026-03-07 09:05:00', 1, '登录风控的设备黑名单，支持 Redis 同步', 'admin', '2026-03-07 09:02:00', 'admin', '2026-03-07 09:05:00', b'0'),
(2, 'LOGIN_RISK', 'LOGIN_USER_WHITE_LIST', '登录用户白名单', 'USER', 'WHITE', 'MANUAL', 'pulsix:list:white:user', '2026-03-07 09:05:00', 1, '登录白名单命中后可直接放行', 'admin', '2026-03-07 09:02:00', 'admin', '2026-03-07 09:05:00', b'0'),
(3, 'REGISTER_ANTI_FRAUD', 'REGISTER_DEVICE_BLACKLIST', '注册设备黑名单', 'DEVICE', 'BLACK', 'MANUAL', 'pulsix:list:black:device', '2026-03-07 10:05:00', 1, '注册反作弊设备黑名单', 'admin', '2026-03-07 10:02:00', 'admin', '2026-03-07 10:05:00', b'0'),
(4, 'REGISTER_ANTI_FRAUD', 'REGISTER_IP_PROXY_LIST', '注册代理IP名单', 'IP', 'WATCH', 'MANUAL', 'pulsix:list:watch:ip_proxy', '2026-03-07 10:05:00', 1, '用于识别代理 IP 高频注册', 'admin', '2026-03-07 10:02:00', 'admin', '2026-03-07 10:05:00', b'0'),
(5, 'REGISTER_ANTI_FRAUD', 'REGISTER_MOBILE_RISK_LIST', '注册风险手机号名单', 'MOBILE', 'WATCH', 'MANUAL', 'pulsix:list:watch:mobile', '2026-03-07 10:05:00', 1, '用于识别风险手机号注册', 'admin', '2026-03-07 10:02:00', 'admin', '2026-03-07 10:05:00', b'0'),
(6, 'TRADE_RISK', 'TRADE_DEVICE_BLACKLIST', '交易设备黑名单', 'DEVICE', 'BLACK', 'MANUAL', 'pulsix:list:black:device', '2026-03-07 11:05:00', 1, '交易场景设备黑名单', 'admin', '2026-03-07 11:02:00', 'admin', '2026-03-07 11:05:00', b'0'),
(7, 'TRADE_RISK', 'TRADE_USER_WHITE_LIST', '交易用户白名单', 'USER', 'WHITE', 'MANUAL', 'pulsix:list:white:user', '2026-03-07 11:05:00', 1, '交易白名单用户默认放行', 'admin', '2026-03-07 11:02:00', 'admin', '2026-03-07 11:05:00', b'0');

INSERT INTO `list_item` (`id`, `list_code`, `match_key`, `match_value`, `source_type`, `expire_at`, `tag_json`, `status`, `remark`, `ext_json`, `creator`, `create_time`, `updater`, `update_time`, `deleted`) VALUES
(1, 'LOGIN_DEVICE_BLACKLIST', 'deviceId', 'D9001', 'MANUAL_IMPORT', NULL, '["manual","login_black"]', 1, '黑产设备样例', '{"source":"manual_import"}', 'admin', '2026-03-07 09:03:00', 'admin', '2026-03-07 09:03:00', b'0'),
(2, 'LOGIN_USER_WHITE_LIST', 'userId', 'U2001', 'MANUAL_IMPORT', NULL, '["vip","white"]', 1, '登录白名单样例', '{"source":"ops_whitelist"}', 'admin', '2026-03-07 09:03:00', 'admin', '2026-03-07 09:03:00', b'0'),
(3, 'REGISTER_DEVICE_BLACKLIST', 'deviceId', 'D_REG_BLACK_001', 'MANUAL_IMPORT', NULL, '["device_black"]', 1, '注册设备黑名单样例', '{"source":"manual_import"}', 'admin', '2026-03-07 10:03:00', 'admin', '2026-03-07 10:03:00', b'0'),
(4, 'REGISTER_IP_PROXY_LIST', 'ip', '22.33.44.55', 'MANUAL_IMPORT', NULL, '["proxy","idc"]', 1, '代理 IP 样例', '{"provider":"idc_proxy_pool"}', 'admin', '2026-03-07 10:03:00', 'admin', '2026-03-07 10:03:00', b'0'),
(5, 'REGISTER_MOBILE_RISK_LIST', 'mobile', '13800009999', 'MANUAL_IMPORT', NULL, '["risk_mobile"]', 1, '风险手机号样例', '{"source":"risk_mobile_pool"}', 'admin', '2026-03-07 10:03:00', 'admin', '2026-03-07 10:03:00', b'0'),
(6, 'TRADE_DEVICE_BLACKLIST', 'deviceId', 'D_BLACK_001', 'MANUAL_IMPORT', NULL, '["trade_black"]', 1, '交易设备黑名单样例', '{"source":"manual_import"}', 'admin', '2026-03-07 11:03:00', 'admin', '2026-03-07 11:03:00', b'0'),
(7, 'TRADE_USER_WHITE_LIST', 'userId', 'U5999', 'MANUAL_IMPORT', NULL, '["vip","trade_white"]', 1, '交易白名单样例', '{"source":"ops_whitelist"}', 'admin', '2026-03-07 11:03:00', 'admin', '2026-03-07 11:03:00', b'0');

INSERT INTO `feature_def` (`id`, `scene_code`, `feature_code`, `feature_name`, `feature_type`, `entity_type`, `event_code`, `value_type`, `status`, `version`, `description`, `creator`, `create_time`, `updater`, `update_time`, `deleted`) VALUES
(1001, 'LOGIN_RISK', 'user_login_fail_cnt_10m', '用户10分钟登录失败次数', 'STREAM', 'USER', 'LOGIN_EVENT', 'LONG', 1, 1, '统计用户10分钟失败登录次数', 'admin', '2026-03-07 09:10:00', 'admin', '2026-03-07 09:10:00', b'0'),
(1002, 'LOGIN_RISK', 'ip_login_fail_cnt_10m', 'IP10分钟登录失败次数', 'STREAM', 'IP', 'LOGIN_EVENT', 'LONG', 1, 1, '统计 IP 10分钟失败登录次数', 'admin', '2026-03-07 09:10:00', 'admin', '2026-03-07 09:10:00', b'0'),
(1003, 'LOGIN_RISK', 'device_login_user_cnt_1h', '设备1小时登录用户数', 'STREAM', 'DEVICE', 'LOGIN_EVENT', 'LONG', 1, 1, '统计设备1小时内关联登录用户数', 'admin', '2026-03-07 09:10:00', 'admin', '2026-03-07 09:10:00', b'0'),
(1004, 'LOGIN_RISK', 'device_in_blacklist', '设备是否命中黑名单', 'LOOKUP', 'DEVICE', 'LOGIN_EVENT', 'BOOLEAN', 1, 1, '查询设备黑名单', 'admin', '2026-03-07 09:10:00', 'admin', '2026-03-07 09:10:00', b'0'),
(1005, 'LOGIN_RISK', 'ip_risk_level', 'IP风险等级', 'LOOKUP', 'IP', 'LOGIN_EVENT', 'STRING', 1, 1, '查询 IP 风险等级', 'admin', '2026-03-07 09:10:00', 'admin', '2026-03-07 09:10:00', b'0'),
(1006, 'LOGIN_RISK', 'user_in_white_list', '用户是否命中白名单', 'LOOKUP', 'USER', 'LOGIN_EVENT', 'BOOLEAN', 1, 1, '查询登录白名单', 'admin', '2026-03-07 09:10:00', 'admin', '2026-03-07 09:10:00', b'0'),
(1007, 'LOGIN_RISK', 'high_fail_user_flag', '用户高失败标记', 'DERIVED', 'USER', 'LOGIN_EVENT', 'BOOLEAN', 1, 1, '基于用户失败次数派生', 'admin', '2026-03-07 09:10:00', 'admin', '2026-03-07 09:10:00', b'0'),
(1008, 'LOGIN_RISK', 'high_fail_ip_flag', 'IP高失败标记', 'DERIVED', 'IP', 'LOGIN_EVENT', 'BOOLEAN', 1, 1, '基于 IP 失败次数派生', 'admin', '2026-03-07 09:10:00', 'admin', '2026-03-07 09:10:00', b'0'),
(1101, 'REGISTER_ANTI_FRAUD', 'device_register_user_cnt_1h', '设备1小时注册用户数', 'STREAM', 'DEVICE', 'REGISTER_EVENT', 'LONG', 1, 1, '统计设备1小时注册用户数', 'admin', '2026-03-07 10:10:00', 'admin', '2026-03-07 10:10:00', b'0'),
(1102, 'REGISTER_ANTI_FRAUD', 'ip_register_cnt_10m', 'IP10分钟注册次数', 'STREAM', 'IP', 'REGISTER_EVENT', 'LONG', 1, 1, '统计 IP 10分钟注册次数', 'admin', '2026-03-07 10:10:00', 'admin', '2026-03-07 10:10:00', b'0'),
(1103, 'REGISTER_ANTI_FRAUD', 'mobile_prefix_register_cnt_1h', '号段1小时注册次数', 'STREAM', 'MOBILE_PREFIX', 'REGISTER_EVENT', 'LONG', 1, 1, '统计手机号段1小时注册次数', 'admin', '2026-03-07 10:10:00', 'admin', '2026-03-07 10:10:00', b'0'),
(1104, 'REGISTER_ANTI_FRAUD', 'device_in_blacklist', '设备是否命中黑名单', 'LOOKUP', 'DEVICE', 'REGISTER_EVENT', 'BOOLEAN', 1, 1, '查询注册设备黑名单', 'admin', '2026-03-07 10:10:00', 'admin', '2026-03-07 10:10:00', b'0'),
(1105, 'REGISTER_ANTI_FRAUD', 'ip_in_proxy_list', 'IP是否命中代理名单', 'LOOKUP', 'IP', 'REGISTER_EVENT', 'BOOLEAN', 1, 1, '查询代理 IP 名单', 'admin', '2026-03-07 10:10:00', 'admin', '2026-03-07 10:10:00', b'0'),
(1106, 'REGISTER_ANTI_FRAUD', 'mobile_in_risk_list', '手机号是否命中风险名单', 'LOOKUP', 'MOBILE', 'REGISTER_EVENT', 'BOOLEAN', 1, 1, '查询风险手机号名单', 'admin', '2026-03-07 10:10:00', 'admin', '2026-03-07 10:10:00', b'0'),
(1107, 'REGISTER_ANTI_FRAUD', 'suspicious_device_flag', '可疑设备标记', 'DERIVED', 'DEVICE', 'REGISTER_EVENT', 'BOOLEAN', 1, 1, '基于设备注册数派生', 'admin', '2026-03-07 10:10:00', 'admin', '2026-03-07 10:10:00', b'0'),
(1108, 'REGISTER_ANTI_FRAUD', 'suspicious_ip_flag', '可疑IP标记', 'DERIVED', 'IP', 'REGISTER_EVENT', 'BOOLEAN', 1, 1, '基于 IP 注册次数派生', 'admin', '2026-03-07 10:10:00', 'admin', '2026-03-07 10:10:00', b'0'),
(1201, 'TRADE_RISK', 'user_trade_cnt_5m', '用户5分钟交易次数', 'STREAM', 'USER', 'TRADE_EVENT', 'LONG', 1, 1, '统计用户5分钟成功交易次数', 'admin', '2026-03-07 11:10:00', 'admin', '2026-03-07 11:10:00', b'0'),
(1202, 'TRADE_RISK', 'user_trade_amt_sum_30m', '用户30分钟交易金额和', 'STREAM', 'USER', 'TRADE_EVENT', 'DECIMAL', 1, 1, '统计用户30分钟成功交易金额和', 'admin', '2026-03-07 11:10:00', 'admin', '2026-03-07 11:10:00', b'0'),
(1203, 'TRADE_RISK', 'device_bind_user_cnt_1h', '设备1小时关联用户数', 'STREAM', 'DEVICE', 'TRADE_EVENT', 'LONG', 1, 1, '统计设备1小时关联用户数', 'admin', '2026-03-07 11:10:00', 'admin', '2026-03-07 11:10:00', b'0'),
(1204, 'TRADE_RISK', 'ip_trade_cnt_10m', 'IP10分钟交易次数', 'STREAM', 'IP', 'TRADE_EVENT', 'LONG', 1, 1, '统计 IP 10分钟交易次数', 'admin', '2026-03-07 11:10:00', 'admin', '2026-03-07 11:10:00', b'0'),
(1205, 'TRADE_RISK', 'device_in_blacklist', '设备是否命中黑名单', 'LOOKUP', 'DEVICE', 'TRADE_EVENT', 'BOOLEAN', 1, 1, '查询交易设备黑名单', 'admin', '2026-03-07 11:10:00', 'admin', '2026-03-07 11:10:00', b'0'),
(1206, 'TRADE_RISK', 'user_risk_level', '用户风险等级', 'LOOKUP', 'USER', 'TRADE_EVENT', 'STRING', 1, 1, '查询用户风险等级画像', 'admin', '2026-03-07 11:10:00', 'admin', '2026-03-07 11:10:00', b'0'),
(1207, 'TRADE_RISK', 'merchant_risk_level', '商户风险等级', 'LOOKUP', 'MERCHANT', 'TRADE_EVENT', 'STRING', 1, 1, '查询商户风险等级画像', 'admin', '2026-03-07 11:10:00', 'admin', '2026-03-07 11:10:00', b'0'),
(1208, 'TRADE_RISK', 'user_in_white_list', '用户是否命中白名单', 'LOOKUP', 'USER', 'TRADE_EVENT', 'BOOLEAN', 1, 1, '查询交易白名单', 'admin', '2026-03-07 11:10:00', 'admin', '2026-03-07 11:10:00', b'0'),
(1209, 'TRADE_RISK', 'high_amt_flag', '高金额标记', 'DERIVED', 'USER', 'TRADE_EVENT', 'BOOLEAN', 1, 1, '基于 amount 派生', 'admin', '2026-03-07 11:10:00', 'admin', '2026-03-07 11:10:00', b'0'),
(1210, 'TRADE_RISK', 'trade_burst_flag', '短时高频交易标记', 'DERIVED', 'USER', 'TRADE_EVENT', 'BOOLEAN', 1, 2, '基于交易次数与金额联合派生', 'admin', '2026-03-07 11:10:00', 'admin', '2026-03-07 11:20:00', b'0'),
(1211, 'TRADE_RISK', 'high_device_risk_flag', '高风险设备标记', 'DERIVED', 'DEVICE', 'TRADE_EVENT', 'BOOLEAN', 1, 1, '基于设备关联用户数派生', 'admin', '2026-03-07 11:10:00', 'admin', '2026-03-07 11:10:00', b'0');

INSERT INTO `feature_stream_conf` (`id`, `scene_code`, `feature_code`, `source_event_types`, `entity_key_expr`, `agg_type`, `value_expr`, `filter_expr`, `window_type`, `window_size`, `window_slide`, `include_current_event`, `ttl_seconds`, `state_hint_json`, `creator`, `create_time`, `updater`, `update_time`, `deleted`) VALUES
(2001, 'LOGIN_RISK', 'user_login_fail_cnt_10m', 'login', 'userId', 'COUNT', '1', 'loginResult == ''FAIL''', 'SLIDING', '10m', '1m', 1, 1800, '{"keyBy":"userId"}', 'admin', '2026-03-07 09:12:00', 'admin', '2026-03-07 09:12:00', b'0'),
(2002, 'LOGIN_RISK', 'ip_login_fail_cnt_10m', 'login', 'ip', 'COUNT', '1', 'loginResult == ''FAIL''', 'SLIDING', '10m', '1m', 1, 1800, '{"keyBy":"ip"}', 'admin', '2026-03-07 09:12:00', 'admin', '2026-03-07 09:12:00', b'0'),
(2003, 'LOGIN_RISK', 'device_login_user_cnt_1h', 'login', 'deviceId', 'DISTINCT_COUNT', 'userId', 'deviceId != null && userId != null', 'SLIDING', '1h', '5m', 1, 7200, '{"keyBy":"deviceId"}', 'admin', '2026-03-07 09:12:00', 'admin', '2026-03-07 09:12:00', b'0'),
(2004, 'REGISTER_ANTI_FRAUD', 'device_register_user_cnt_1h', 'register', 'deviceId', 'DISTINCT_COUNT', 'userId', 'registerResult == ''SUCCESS''', 'SLIDING', '1h', '5m', 1, 7200, '{"keyBy":"deviceId"}', 'admin', '2026-03-07 10:12:00', 'admin', '2026-03-07 10:12:00', b'0'),
(2005, 'REGISTER_ANTI_FRAUD', 'ip_register_cnt_10m', 'register', 'ip', 'COUNT', '1', 'registerResult == ''SUCCESS''', 'SLIDING', '10m', '1m', 1, 1800, '{"keyBy":"ip"}', 'admin', '2026-03-07 10:12:00', 'admin', '2026-03-07 10:12:00', b'0'),
(2006, 'REGISTER_ANTI_FRAUD', 'mobile_prefix_register_cnt_1h', 'register', 'mobile.substring(0, 7)', 'COUNT', '1', 'mobile != null && registerResult == ''SUCCESS''', 'SLIDING', '1h', '5m', 1, 7200, '{"keyBy":"mobilePrefix"}', 'admin', '2026-03-07 10:12:00', 'admin', '2026-03-07 10:12:00', b'0'),
(2007, 'TRADE_RISK', 'user_trade_cnt_5m', 'trade', 'userId', 'COUNT', '1', 'tradeResult == ''SUCCESS''', 'SLIDING', '5m', '1m', 1, 600, '{"keyBy":"userId"}', 'admin', '2026-03-07 11:12:00', 'admin', '2026-03-07 11:12:00', b'0'),
(2008, 'TRADE_RISK', 'user_trade_amt_sum_30m', 'trade', 'userId', 'SUM', 'amount', 'tradeResult == ''SUCCESS''', 'SLIDING', '30m', '1m', 1, 2400, '{"keyBy":"userId"}', 'admin', '2026-03-07 11:12:00', 'admin', '2026-03-07 11:12:00', b'0'),
(2009, 'TRADE_RISK', 'device_bind_user_cnt_1h', 'trade', 'deviceId', 'DISTINCT_COUNT', 'userId', 'deviceId != null && userId != null', 'SLIDING', '1h', '5m', 1, 7200, '{"keyBy":"deviceId"}', 'admin', '2026-03-07 11:12:00', 'admin', '2026-03-07 11:12:00', b'0'),
(2010, 'TRADE_RISK', 'ip_trade_cnt_10m', 'trade', 'ip', 'COUNT', '1', 'tradeResult == ''SUCCESS''', 'SLIDING', '10m', '1m', 1, 1800, '{"keyBy":"ip"}', 'admin', '2026-03-07 11:12:00', 'admin', '2026-03-07 11:12:00', b'0');

INSERT INTO `feature_lookup_conf` (`id`, `scene_code`, `feature_code`, `lookup_type`, `key_expr`, `source_ref`, `default_value`, `cache_ttl_seconds`, `timeout_ms`, `extra_json`, `creator`, `create_time`, `updater`, `update_time`, `deleted`) VALUES
(3001, 'LOGIN_RISK', 'device_in_blacklist', 'REDIS_SET', 'deviceId', 'pulsix:list:black:device', 'false', 30, 20, '{"lookupMode":"exists"}', 'admin', '2026-03-07 09:13:00', 'admin', '2026-03-07 09:13:00', b'0'),
(3002, 'LOGIN_RISK', 'ip_risk_level', 'REDIS_HASH', 'ip', 'pulsix:profile:ip:risk', 'LOW', 30, 20, '{"field":"riskLevel"}', 'admin', '2026-03-07 09:13:00', 'admin', '2026-03-07 09:13:00', b'0'),
(3003, 'LOGIN_RISK', 'user_in_white_list', 'REDIS_SET', 'userId', 'pulsix:list:white:user', 'false', 30, 20, '{"lookupMode":"exists"}', 'admin', '2026-03-07 09:13:00', 'admin', '2026-03-07 09:13:00', b'0'),
(3004, 'REGISTER_ANTI_FRAUD', 'device_in_blacklist', 'REDIS_SET', 'deviceId', 'pulsix:list:black:device', 'false', 30, 20, '{"lookupMode":"exists"}', 'admin', '2026-03-07 10:13:00', 'admin', '2026-03-07 10:13:00', b'0'),
(3005, 'REGISTER_ANTI_FRAUD', 'ip_in_proxy_list', 'REDIS_SET', 'ip', 'pulsix:list:watch:ip_proxy', 'false', 30, 20, '{"lookupMode":"exists"}', 'admin', '2026-03-07 10:13:00', 'admin', '2026-03-07 10:13:00', b'0'),
(3006, 'REGISTER_ANTI_FRAUD', 'mobile_in_risk_list', 'REDIS_SET', 'mobile', 'pulsix:list:watch:mobile', 'false', 30, 20, '{"lookupMode":"exists"}', 'admin', '2026-03-07 10:13:00', 'admin', '2026-03-07 10:13:00', b'0'),
(3007, 'TRADE_RISK', 'device_in_blacklist', 'REDIS_SET', 'deviceId', 'pulsix:list:black:device', 'false', 30, 20, '{"lookupMode":"exists"}', 'admin', '2026-03-07 11:13:00', 'admin', '2026-03-07 11:13:00', b'0'),
(3008, 'TRADE_RISK', 'user_risk_level', 'REDIS_HASH', 'userId', 'pulsix:profile:user:risk', 'L', 30, 20, '{"field":"riskLevel"}', 'admin', '2026-03-07 11:13:00', 'admin', '2026-03-07 11:13:00', b'0'),
(3009, 'TRADE_RISK', 'merchant_risk_level', 'REDIS_HASH', 'merchantId', 'pulsix:profile:merchant:risk', 'LOW', 30, 20, '{"field":"riskLevel"}', 'admin', '2026-03-07 11:13:00', 'admin', '2026-03-07 11:13:00', b'0'),
(3010, 'TRADE_RISK', 'user_in_white_list', 'REDIS_SET', 'userId', 'pulsix:list:white:user', 'false', 30, 20, '{"lookupMode":"exists"}', 'admin', '2026-03-07 11:13:00', 'admin', '2026-03-07 11:13:00', b'0');

INSERT INTO `feature_derived_conf` (`id`, `scene_code`, `feature_code`, `engine_type`, `expr_content`, `depends_on_json`, `value_type`, `extra_json`, `creator`, `create_time`, `updater`, `update_time`, `deleted`) VALUES
(4001, 'LOGIN_RISK', 'high_fail_user_flag', 'AVIATOR', 'user_login_fail_cnt_10m >= 5', '["user_login_fail_cnt_10m"]', 'BOOLEAN', NULL, 'admin', '2026-03-07 09:14:00', 'admin', '2026-03-07 09:14:00', b'0'),
(4002, 'LOGIN_RISK', 'high_fail_ip_flag', 'AVIATOR', 'ip_login_fail_cnt_10m >= 20', '["ip_login_fail_cnt_10m"]', 'BOOLEAN', NULL, 'admin', '2026-03-07 09:14:00', 'admin', '2026-03-07 09:14:00', b'0'),
(4003, 'REGISTER_ANTI_FRAUD', 'suspicious_device_flag', 'AVIATOR', 'device_register_user_cnt_1h >= 3', '["device_register_user_cnt_1h"]', 'BOOLEAN', NULL, 'admin', '2026-03-07 10:14:00', 'admin', '2026-03-07 10:14:00', b'0'),
(4004, 'REGISTER_ANTI_FRAUD', 'suspicious_ip_flag', 'AVIATOR', 'ip_register_cnt_10m >= 10', '["ip_register_cnt_10m"]', 'BOOLEAN', NULL, 'admin', '2026-03-07 10:14:00', 'admin', '2026-03-07 10:14:00', b'0'),
(4005, 'TRADE_RISK', 'high_amt_flag', 'AVIATOR', 'amount >= 5000', '["amount"]', 'BOOLEAN', NULL, 'admin', '2026-03-07 11:14:00', 'admin', '2026-03-07 11:14:00', b'0'),
(4006, 'TRADE_RISK', 'trade_burst_flag', 'AVIATOR', 'user_trade_cnt_5m >= 3 && amount >= 5000', '["user_trade_cnt_5m","amount"]', 'BOOLEAN', NULL, 'admin', '2026-03-07 11:14:00', 'admin', '2026-03-07 11:20:00', b'0'),
(4007, 'TRADE_RISK', 'high_device_risk_flag', 'AVIATOR', 'device_bind_user_cnt_1h >= 4', '["device_bind_user_cnt_1h"]', 'BOOLEAN', NULL, 'admin', '2026-03-07 11:14:00', 'admin', '2026-03-07 11:14:00', b'0');

INSERT INTO `rule_def` (`id`, `scene_code`, `rule_code`, `rule_name`, `rule_type`, `engine_type`, `expr_content`, `depends_on_json`, `priority`, `hit_action`, `risk_score`, `alert_enabled_flag`, `status`, `hit_reason_template`, `version`, `description`, `creator`, `create_time`, `updater`, `update_time`, `deleted`) VALUES
(5001, 'LOGIN_RISK', 'LOGIN_R001', '黑名单设备直接拒绝', 'NORMAL', 'AVIATOR', 'device_in_blacklist == true && user_in_white_list != true', '["device_in_blacklist","user_in_white_list"]', 100, 'REJECT', 100, b'1', 1, '设备命中黑名单', 1, '黑名单设备登录直接拒绝', 'admin', '2026-03-07 09:20:00', 'admin', '2026-03-07 09:20:00', b'0'),
(5002, 'LOGIN_RISK', 'LOGIN_R002', '用户短时高频失败登录', 'NORMAL', 'AVIATOR', 'user_login_fail_cnt_10m >= 5 && ip_risk_level == ''HIGH''', '["user_login_fail_cnt_10m","ip_risk_level"]', 90, 'REVIEW', 60, b'1', 1, '用户10分钟失败次数={user_login_fail_cnt_10m}, IP风险等级={ip_risk_level}', 1, '短时爆破类行为审单', 'admin', '2026-03-07 09:20:00', 'admin', '2026-03-07 09:20:00', b'0'),
(5003, 'LOGIN_RISK', 'LOGIN_R003', '高风险IP爆破行为', 'NORMAL', 'AVIATOR', 'ip_login_fail_cnt_10m >= 20', '["ip_login_fail_cnt_10m"]', 95, 'REJECT', 90, b'1', 1, 'IP10分钟失败次数={ip_login_fail_cnt_10m}', 1, '高风险 IP 短时大量失败', 'admin', '2026-03-07 09:20:00', 'admin', '2026-03-07 09:20:00', b'0'),
(5004, 'LOGIN_RISK', 'LOGIN_R004', '设备多账号登录异常', 'NORMAL', 'AVIATOR', 'device_login_user_cnt_1h >= 5', '["device_login_user_cnt_1h"]', 80, 'REVIEW', 50, b'1', 1, '设备1小时关联登录用户数={device_login_user_cnt_1h}', 1, '设备多账号登录异常', 'admin', '2026-03-07 09:20:00', 'admin', '2026-03-07 09:20:00', b'0'),
(5005, 'REGISTER_ANTI_FRAUD', 'REG_R001', '黑名单设备注册拒绝', 'NORMAL', 'AVIATOR', 'device_in_blacklist == true', '["device_in_blacklist"]', 100, 'REJECT', 100, b'1', 1, '设备命中黑名单', 1, '注册黑名单设备直接拒绝', 'admin', '2026-03-07 10:20:00', 'admin', '2026-03-07 10:20:00', b'0'),
(5006, 'REGISTER_ANTI_FRAUD', 'REG_R002', '代理IP高频注册', 'NORMAL', 'AVIATOR', 'ip_in_proxy_list == true && ip_register_cnt_10m >= 10', '["ip_in_proxy_list","ip_register_cnt_10m"]', 95, 'REJECT', 90, b'1', 1, '代理IP且10分钟注册次数={ip_register_cnt_10m}', 1, '代理 IP 批量注册', 'admin', '2026-03-07 10:20:00', 'admin', '2026-03-07 10:20:00', b'0'),
(5007, 'REGISTER_ANTI_FRAUD', 'REG_R003', '同设备批量注册', 'NORMAL', 'AVIATOR', 'device_register_user_cnt_1h >= 3', '["device_register_user_cnt_1h"]', 90, 'REVIEW', 60, b'1', 1, '设备1小时注册用户数={device_register_user_cnt_1h}', 1, '同设备批量养号行为', 'admin', '2026-03-07 10:20:00', 'admin', '2026-03-07 10:20:00', b'0'),
(5008, 'REGISTER_ANTI_FRAUD', 'REG_R004', '风险手机号注册', 'NORMAL', 'AVIATOR', 'mobile_in_risk_list == true', '["mobile_in_risk_list"]', 80, 'REVIEW', 50, b'1', 1, '手机号命中风险名单', 1, '风险手机号注册审单', 'admin', '2026-03-07 10:20:00', 'admin', '2026-03-07 10:20:00', b'0'),
(5009, 'TRADE_RISK', 'TRADE_R001', '白名单用户直接放行', 'NORMAL', 'AVIATOR', 'user_in_white_list == true', '["user_in_white_list"]', 110, 'PASS', 0, b'0', 1, '用户命中白名单', 1, '交易白名单优先放行', 'admin', '2026-03-07 11:20:00', 'admin', '2026-03-07 11:20:00', b'0'),
(5010, 'TRADE_RISK', 'TRADE_R002', '黑名单设备直接拒绝', 'NORMAL', 'AVIATOR', 'device_in_blacklist == true', '["device_in_blacklist"]', 100, 'REJECT', 100, b'1', 1, '设备命中黑名单', 1, '交易黑名单设备直接拒绝', 'admin', '2026-03-07 11:20:00', 'admin', '2026-03-07 11:20:00', b'0'),
(5011, 'TRADE_RISK', 'TRADE_R003', '短时高频大额交易', 'NORMAL', 'AVIATOR', 'user_trade_cnt_5m >= 3 && amount >= 5000', '["user_trade_cnt_5m","amount"]', 90, 'REVIEW', 60, b'1', 1, '用户5分钟交易次数={user_trade_cnt_5m}, 当前金额={amount}', 2, '高频大额交易审单规则，v2 将阈值从 4 次下调到 3 次', 'admin', '2026-03-07 11:20:00', 'admin', '2026-03-07 11:25:00', b'0'),
(5012, 'TRADE_RISK', 'TRADE_R004', '高风险用户+高风险设备', 'NORMAL', 'AVIATOR', 'device_bind_user_cnt_1h >= 4 && (user_risk_level == ''M'' || user_risk_level == ''H'')', '["device_bind_user_cnt_1h","user_risk_level"]', 95, 'REJECT', 90, b'1', 1, '设备1小时关联用户数={device_bind_user_cnt_1h}, 用户风险等级={user_risk_level}', 1, '高风险用户与多账号设备联动', 'admin', '2026-03-07 11:20:00', 'admin', '2026-03-07 11:20:00', b'0'),
(5013, 'TRADE_RISK', 'TRADE_R005', '商户高风险且交易频繁', 'NORMAL', 'AVIATOR', 'merchant_risk_level == ''HIGH'' && ip_trade_cnt_10m >= 10', '["merchant_risk_level","ip_trade_cnt_10m"]', 85, 'REVIEW', 50, b'1', 1, '商户风险等级={merchant_risk_level}, IP10分钟交易次数={ip_trade_cnt_10m}', 1, '高风险商户且 IP 高频交易', 'admin', '2026-03-07 11:20:00', 'admin', '2026-03-07 11:20:00', b'0');

INSERT INTO `policy_def` (`id`, `scene_code`, `policy_code`, `policy_name`, `decision_mode`, `default_action`, `status`, `version`, `description`, `creator`, `create_time`, `updater`, `update_time`, `deleted`) VALUES
(6001, 'LOGIN_RISK', 'LOGIN_RISK_POLICY', '登录风控主策略', 'FIRST_HIT', 'PASS', 1, 1, '登录风控一期 FIRST_HIT 策略', 'admin', '2026-03-07 09:25:00', 'admin', '2026-03-07 09:25:00', b'0'),
(6002, 'REGISTER_ANTI_FRAUD', 'REGISTER_ANTI_FRAUD_POLICY', '注册反作弊主策略', 'FIRST_HIT', 'PASS', 1, 1, '注册反作弊一期 FIRST_HIT 策略', 'admin', '2026-03-07 10:25:00', 'admin', '2026-03-07 10:25:00', b'0'),
(6003, 'TRADE_RISK', 'TRADE_RISK_POLICY_FIRST_HIT', '交易风控 FIRST_HIT 主策略', 'FIRST_HIT', 'PASS', 1, 2, '交易风控一期默认策略', 'admin', '2026-03-07 11:25:00', 'admin', '2026-03-07 11:25:00', b'0');

INSERT INTO `policy_rule_ref` (`id`, `scene_code`, `policy_code`, `rule_code`, `order_no`, `enabled_flag`, `branch_expr`, `score_weight`, `creator`, `create_time`, `updater`, `update_time`, `deleted`) VALUES
(7001, 'LOGIN_RISK', 'LOGIN_RISK_POLICY', 'LOGIN_R001', 1, 1, NULL, NULL, 'admin', '2026-03-07 09:26:00', 'admin', '2026-03-07 09:26:00', b'0'),
(7002, 'LOGIN_RISK', 'LOGIN_RISK_POLICY', 'LOGIN_R003', 2, 1, NULL, NULL, 'admin', '2026-03-07 09:26:00', 'admin', '2026-03-07 09:26:00', b'0'),
(7003, 'LOGIN_RISK', 'LOGIN_RISK_POLICY', 'LOGIN_R002', 3, 1, NULL, NULL, 'admin', '2026-03-07 09:26:00', 'admin', '2026-03-07 09:26:00', b'0'),
(7004, 'LOGIN_RISK', 'LOGIN_RISK_POLICY', 'LOGIN_R004', 4, 1, NULL, NULL, 'admin', '2026-03-07 09:26:00', 'admin', '2026-03-07 09:26:00', b'0'),
(7005, 'REGISTER_ANTI_FRAUD', 'REGISTER_ANTI_FRAUD_POLICY', 'REG_R001', 1, 1, NULL, NULL, 'admin', '2026-03-07 10:26:00', 'admin', '2026-03-07 10:26:00', b'0'),
(7006, 'REGISTER_ANTI_FRAUD', 'REGISTER_ANTI_FRAUD_POLICY', 'REG_R002', 2, 1, NULL, NULL, 'admin', '2026-03-07 10:26:00', 'admin', '2026-03-07 10:26:00', b'0'),
(7007, 'REGISTER_ANTI_FRAUD', 'REGISTER_ANTI_FRAUD_POLICY', 'REG_R003', 3, 1, NULL, NULL, 'admin', '2026-03-07 10:26:00', 'admin', '2026-03-07 10:26:00', b'0'),
(7008, 'REGISTER_ANTI_FRAUD', 'REGISTER_ANTI_FRAUD_POLICY', 'REG_R004', 4, 1, NULL, NULL, 'admin', '2026-03-07 10:26:00', 'admin', '2026-03-07 10:26:00', b'0'),
(7009, 'TRADE_RISK', 'TRADE_RISK_POLICY_FIRST_HIT', 'TRADE_R001', 1, 1, NULL, NULL, 'admin', '2026-03-07 11:26:00', 'admin', '2026-03-07 11:26:00', b'0'),
(7010, 'TRADE_RISK', 'TRADE_RISK_POLICY_FIRST_HIT', 'TRADE_R002', 2, 1, NULL, NULL, 'admin', '2026-03-07 11:26:00', 'admin', '2026-03-07 11:26:00', b'0'),
(7011, 'TRADE_RISK', 'TRADE_RISK_POLICY_FIRST_HIT', 'TRADE_R004', 3, 1, NULL, NULL, 'admin', '2026-03-07 11:26:00', 'admin', '2026-03-07 11:26:00', b'0'),
(7012, 'TRADE_RISK', 'TRADE_RISK_POLICY_FIRST_HIT', 'TRADE_R003', 4, 1, NULL, NULL, 'admin', '2026-03-07 11:26:00', 'admin', '2026-03-07 11:26:00', b'0'),
(7013, 'TRADE_RISK', 'TRADE_RISK_POLICY_FIRST_HIT', 'TRADE_R005', 5, 1, NULL, NULL, 'admin', '2026-03-07 11:26:00', 'admin', '2026-03-07 11:26:00', b'0');

INSERT INTO `scene_release` (`id`, `scene_code`, `version_no`, `checksum`, `publish_status`, `snapshot_json`, `compile_report_json`, `effective_from`, `published_by`, `published_at`, `rollback_from_version`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`) VALUES
(8001, 'LOGIN_RISK', 1, '3fd58d2e1c8b4b2ea7f3c4311d8b0101', 'ACTIVE', '{"snapshotId":"LOGIN_RISK_v1","sceneCode":"LOGIN_RISK","version":1,"status":"ACTIVE","scene":{"defaultPolicyCode":"LOGIN_RISK_POLICY","allowedEventTypes":["login"],"decisionTimeoutMs":500,"logLevel":"FULL"},"streamFeatures":["user_login_fail_cnt_10m","ip_login_fail_cnt_10m","device_login_user_cnt_1h"],"lookupFeatures":["device_in_blacklist","ip_risk_level","user_in_white_list"],"rules":["LOGIN_R001","LOGIN_R003","LOGIN_R002","LOGIN_R004"],"policy":{"policyCode":"LOGIN_RISK_POLICY","decisionMode":"FIRST_HIT","defaultAction":"PASS"}}', '{"validation":"PASSED","featureCount":8,"ruleCount":4,"policyCode":"LOGIN_RISK_POLICY"}', '2026-03-07 09:35:00', 'admin', '2026-03-07 09:34:50', NULL, '登录风控初始发布版本', 'admin', '2026-03-07 09:34:50', 'admin', '2026-03-07 09:34:50', b'0'),
(8002, 'REGISTER_ANTI_FRAUD', 1, '6aa508a3ff6845e8b7f5ecb6ae6d0202', 'ACTIVE', '{"snapshotId":"REGISTER_ANTI_FRAUD_v1","sceneCode":"REGISTER_ANTI_FRAUD","version":1,"status":"ACTIVE","scene":{"defaultPolicyCode":"REGISTER_ANTI_FRAUD_POLICY","allowedEventTypes":["register"],"decisionTimeoutMs":500,"logLevel":"FULL"},"streamFeatures":["device_register_user_cnt_1h","ip_register_cnt_10m","mobile_prefix_register_cnt_1h"],"lookupFeatures":["device_in_blacklist","ip_in_proxy_list","mobile_in_risk_list"],"rules":["REG_R001","REG_R002","REG_R003","REG_R004"],"policy":{"policyCode":"REGISTER_ANTI_FRAUD_POLICY","decisionMode":"FIRST_HIT","defaultAction":"PASS"}}', '{"validation":"PASSED","featureCount":8,"ruleCount":4,"policyCode":"REGISTER_ANTI_FRAUD_POLICY"}', '2026-03-07 10:35:00', 'admin', '2026-03-07 10:34:50', NULL, '注册反作弊初始发布版本', 'admin', '2026-03-07 10:34:50', 'admin', '2026-03-07 10:34:50', b'0'),
(8003, 'TRADE_RISK', 1, '8d2041a7cf8f47b4b6b0f91d2ab8d9d0', 'PUBLISHED', '{"snapshotId":"TRADE_RISK_v1","sceneCode":"TRADE_RISK","version":1,"status":"PUBLISHED","scene":{"defaultPolicyCode":"TRADE_RISK_POLICY_FIRST_HIT","allowedEventTypes":["trade"],"decisionTimeoutMs":500,"logLevel":"FULL"},"features":{"stream":[{"code":"user_trade_cnt_5m","aggType":"COUNT","windowSize":"5m"},{"code":"user_trade_amt_sum_30m","aggType":"SUM","windowSize":"30m"},{"code":"device_bind_user_cnt_1h","aggType":"DISTINCT_COUNT","windowSize":"1h"},{"code":"ip_trade_cnt_10m","aggType":"COUNT","windowSize":"10m"}],"lookup":["device_in_blacklist","user_risk_level","merchant_risk_level","user_in_white_list"],"derived":["high_amt_flag","trade_burst_flag","high_device_risk_flag"]},"rules":[{"code":"TRADE_R001","expr":"user_in_white_list == true","action":"PASS"},{"code":"TRADE_R002","expr":"device_in_blacklist == true","action":"REJECT"},{"code":"TRADE_R003","expr":"user_trade_cnt_5m >= 4 && amount >= 5000","action":"REVIEW"},{"code":"TRADE_R004","expr":"device_bind_user_cnt_1h >= 4 && (user_risk_level == ''M'' || user_risk_level == ''H'')","action":"REJECT"},{"code":"TRADE_R005","expr":"merchant_risk_level == ''HIGH'' && ip_trade_cnt_10m >= 10","action":"REVIEW"}],"policy":{"policyCode":"TRADE_RISK_POLICY_FIRST_HIT","decisionMode":"FIRST_HIT","defaultAction":"PASS","ruleOrder":["TRADE_R001","TRADE_R002","TRADE_R004","TRADE_R003","TRADE_R005"]}}', '{"validation":"PASSED","featureCount":11,"ruleCount":5,"policyCode":"TRADE_RISK_POLICY_FIRST_HIT","warnings":["TRADE_R003 threshold=4 may miss early burst cases"]}', '2026-03-07 11:35:00', 'admin', '2026-03-07 11:34:50', NULL, '交易风控首个稳定版本', 'admin', '2026-03-07 11:34:50', 'admin', '2026-03-07 11:34:50', b'0'),
(8004, 'TRADE_RISK', 2, 'be53153c4dfe4f4f8ef4e2d2f96f3939', 'ACTIVE', '{"snapshotId":"TRADE_RISK_v2","sceneCode":"TRADE_RISK","version":2,"status":"ACTIVE","scene":{"defaultPolicyCode":"TRADE_RISK_POLICY_FIRST_HIT","allowedEventTypes":["trade"],"decisionTimeoutMs":500,"logLevel":"FULL"},"features":{"stream":[{"code":"user_trade_cnt_5m","aggType":"COUNT","windowSize":"5m","windowSlide":"1m","ttl":"10m"},{"code":"user_trade_amt_sum_30m","aggType":"SUM","windowSize":"30m"},{"code":"device_bind_user_cnt_1h","aggType":"DISTINCT_COUNT","windowSize":"1h"},{"code":"ip_trade_cnt_10m","aggType":"COUNT","windowSize":"10m"}],"lookup":["device_in_blacklist","user_risk_level","merchant_risk_level","user_in_white_list"],"derived":["high_amt_flag","trade_burst_flag","high_device_risk_flag"]},"rules":[{"code":"TRADE_R001","expr":"user_in_white_list == true","action":"PASS"},{"code":"TRADE_R002","expr":"device_in_blacklist == true","action":"REJECT"},{"code":"TRADE_R003","expr":"user_trade_cnt_5m >= 3 && amount >= 5000","action":"REVIEW"},{"code":"TRADE_R004","expr":"device_bind_user_cnt_1h >= 4 && (user_risk_level == ''M'' || user_risk_level == ''H'')","action":"REJECT"},{"code":"TRADE_R005","expr":"merchant_risk_level == ''HIGH'' && ip_trade_cnt_10m >= 10","action":"REVIEW"}],"policy":{"policyCode":"TRADE_RISK_POLICY_FIRST_HIT","decisionMode":"FIRST_HIT","defaultAction":"PASS","ruleOrder":["TRADE_R001","TRADE_R002","TRADE_R004","TRADE_R003","TRADE_R005"]}}', '{"validation":"PASSED","featureCount":11,"ruleCount":5,"policyCode":"TRADE_RISK_POLICY_FIRST_HIT","changes":["TRADE_R003 threshold 4 -> 3","user_trade_cnt_5m windowSlide 5m -> 1m"]}', '2026-03-07 11:45:00', 'admin', '2026-03-07 11:44:50', NULL, '交易风控第二版，放宽高频大额阈值', 'admin', '2026-03-07 11:44:50', 'admin', '2026-03-07 11:44:50', b'0');

INSERT INTO `scene_release_change` (`id`, `release_id`, `scene_code`, `change_type`, `target_type`, `target_code`, `before_json`, `after_json`, `description`, `creator`, `create_time`, `updater`, `update_time`, `deleted`) VALUES
(9001, 8004, 'TRADE_RISK', 'MODIFY', 'RULE', 'TRADE_R003', '{"expr":"user_trade_cnt_5m >= 4 && amount >= 5000","priority":90,"hitAction":"REVIEW"}', '{"expr":"user_trade_cnt_5m >= 3 && amount >= 5000","priority":90,"hitAction":"REVIEW"}', '放宽高频大额交易命中阈值', 'admin', '2026-03-07 11:44:00', 'admin', '2026-03-07 11:44:00', b'0'),
(9002, 8004, 'TRADE_RISK', 'MODIFY', 'FEATURE', 'user_trade_cnt_5m', '{"windowSlide":"5m","ttlSeconds":300}', '{"windowSlide":"1m","ttlSeconds":600}', '提升窗口滑动粒度并延长状态 TTL', 'admin', '2026-03-07 11:44:10', 'admin', '2026-03-07 11:44:10', b'0');

INSERT INTO `simulation_case` (`id`, `scene_code`, `case_code`, `case_name`, `input_event_json`, `context_json`, `expected_action`, `expected_hit_rules`, `status`, `description`, `creator`, `create_time`, `updater`, `update_time`, `deleted`) VALUES
(10001, 'LOGIN_RISK', 'E_LOGIN_0002', '正常登录', '{"eventId":"E_LOGIN_0002","traceId":"T_LOGIN_0002","sceneCode":"LOGIN_RISK","eventType":"login","eventTime":"2026-03-07T09:10:00","userId":"U2001","deviceId":"D2001","ip":"1.1.1.1","channel":"APP","loginResult":"SUCCESS","city":"Shanghai","province":"Shanghai"}', NULL, 'PASS', '[]', 1, '登录风控样例 A', 'admin', '2026-03-07 09:40:00', 'admin', '2026-03-07 09:40:00', b'0'),
(10002, 'LOGIN_RISK', 'E_LOGIN_0003', '短时密码爆破', '{"eventId":"E_LOGIN_0003","traceId":"T_LOGIN_0003","sceneCode":"LOGIN_RISK","eventType":"login","eventTime":"2026-03-07T09:20:00","userId":"U1001","deviceId":"D9002","ip":"10.20.30.40","channel":"APP","loginResult":"FAIL","failReason":"PASSWORD_ERROR"}', '{"user_login_fail_cnt_10m":6,"ip_risk_level":"HIGH"}', 'REVIEW', '["LOGIN_R002"]', 1, '登录风控样例 B', 'admin', '2026-03-07 09:40:00', 'admin', '2026-03-07 09:40:00', b'0'),
(10003, 'REGISTER_ANTI_FRAUD', 'E_REG_0002', '正常注册', '{"eventId":"E_REG_0002","traceId":"T_REG_0002","sceneCode":"REGISTER_ANTI_FRAUD","eventType":"register","eventTime":"2026-03-07T10:30:00","userId":"U3002","deviceId":"D_REG_002","ip":"2.2.2.2","channel":"APP","mobile":"13900002222","registerResult":"SUCCESS"}', NULL, 'PASS', '[]', 1, '注册反作弊样例 A', 'admin', '2026-03-07 10:40:00', 'admin', '2026-03-07 10:40:00', b'0'),
(10004, 'REGISTER_ANTI_FRAUD', 'E_REG_0003', '同设备批量注册', '{"eventId":"E_REG_0003","traceId":"T_REG_0003","sceneCode":"REGISTER_ANTI_FRAUD","eventType":"register","eventTime":"2026-03-07T10:35:00","userId":"U3999","deviceId":"D_REG_001","ip":"22.33.44.55","channel":"APP","mobile":"13800009999","registerResult":"SUCCESS"}', '{"device_register_user_cnt_1h":4}', 'REVIEW', '["REG_R003"]', 1, '注册反作弊样例 B', 'admin', '2026-03-07 10:40:00', 'admin', '2026-03-07 10:40:00', b'0'),
(10005, 'TRADE_RISK', 'E_TRADE_0002', '正常交易', '{"eventId":"E_TRADE_0002","traceId":"T_TRADE_0002","sceneCode":"TRADE_RISK","eventType":"trade","eventTime":"2026-03-07T11:10:00","userId":"U5002","deviceId":"D5002","ip":"3.3.3.3","channel":"APP","amount":88,"currency":"CNY","tradeResult":"SUCCESS","merchantId":"M1002","payMethod":"CARD"}', NULL, 'PASS', '[]', 1, '交易风控样例 A', 'admin', '2026-03-07 11:40:00', 'admin', '2026-03-07 11:40:00', b'0'),
(10006, 'TRADE_RISK', 'E_TRADE_0003', '高频大额交易', '{"eventId":"E_TRADE_0003","traceId":"T_TRADE_0003","sceneCode":"TRADE_RISK","eventType":"trade","eventTime":"2026-03-07T11:15:00","userId":"U5001","deviceId":"D5001","ip":"66.77.88.99","channel":"APP","amount":6800,"currency":"CNY","tradeResult":"SUCCESS","merchantId":"M1001","payMethod":"CARD"}', '{"user_trade_cnt_5m":4,"device_in_blacklist":false,"device_bind_user_cnt_1h":2,"user_risk_level":"L"}', 'REVIEW', '["TRADE_R003"]', 1, '交易风控样例 B', 'admin', '2026-03-07 11:40:00', 'admin', '2026-03-07 11:40:00', b'0'),
(10007, 'TRADE_RISK', 'E_TRADE_0004', '黑名单设备交易', '{"eventId":"E_TRADE_0004","traceId":"T_TRADE_0004","sceneCode":"TRADE_RISK","eventType":"trade","eventTime":"2026-03-07T11:20:00","userId":"U5003","deviceId":"D_BLACK_001","ip":"77.88.99.11","channel":"APP","amount":1200,"currency":"CNY","tradeResult":"SUCCESS","merchantId":"M1003","payMethod":"CARD"}', '{"device_in_blacklist":true}', 'REJECT', '["TRADE_R002"]', 1, '交易风控样例 C', 'admin', '2026-03-07 11:40:00', 'admin', '2026-03-07 11:40:00', b'0'),
(10008, 'TRADE_RISK', 'E_TRADE_0005', '高风险用户+多账号设备', '{"eventId":"E_TRADE_0005","traceId":"T_TRADE_0005","sceneCode":"TRADE_RISK","eventType":"trade","eventTime":"2026-03-07T11:25:00","userId":"U5004","deviceId":"D_MULTI_001","ip":"99.88.77.66","channel":"APP","amount":2600,"currency":"CNY","tradeResult":"SUCCESS","merchantId":"M1004","payMethod":"CARD"}', '{"device_bind_user_cnt_1h":5,"user_risk_level":"H"}', 'REJECT', '["TRADE_R004"]', 1, '交易风控样例 D', 'admin', '2026-03-07 11:40:00', 'admin', '2026-03-07 11:40:00', b'0');

INSERT INTO `simulation_report` (`id`, `case_id`, `scene_code`, `case_code`, `release_id`, `version_no`, `actual_action`, `actual_hit_rules`, `feature_snapshot_json`, `result_json`, `pass_flag`, `duration_ms`, `run_by`, `creator`, `create_time`, `updater`, `update_time`, `deleted`) VALUES
(10101, 10001, 'LOGIN_RISK', 'E_LOGIN_0002', 8001, 1, 'PASS', '[]', '{}', '{"finalAction":"PASS","hitRules":[],"latencyMs":9}', 1, 9, 'admin', 'admin', '2026-03-07 09:41:00', 'admin', '2026-03-07 09:41:00', b'0'),
(10102, 10002, 'LOGIN_RISK', 'E_LOGIN_0003', 8001, 1, 'REVIEW', '["LOGIN_R002"]', '{"user_login_fail_cnt_10m":6,"ip_risk_level":"HIGH"}', '{"finalAction":"REVIEW","hitRules":["LOGIN_R002"],"latencyMs":15}', 1, 15, 'admin', 'admin', '2026-03-07 09:41:10', 'admin', '2026-03-07 09:41:10', b'0'),
(10103, 10003, 'REGISTER_ANTI_FRAUD', 'E_REG_0002', 8002, 1, 'PASS', '[]', '{}', '{"finalAction":"PASS","hitRules":[],"latencyMs":11}', 1, 11, 'admin', 'admin', '2026-03-07 10:41:00', 'admin', '2026-03-07 10:41:00', b'0'),
(10104, 10004, 'REGISTER_ANTI_FRAUD', 'E_REG_0003', 8002, 1, 'REVIEW', '["REG_R003"]', '{"device_register_user_cnt_1h":4}', '{"finalAction":"REVIEW","hitRules":["REG_R003"],"latencyMs":14}', 1, 14, 'admin', 'admin', '2026-03-07 10:41:10', 'admin', '2026-03-07 10:41:10', b'0'),
(10105, 10005, 'TRADE_RISK', 'E_TRADE_0002', 8004, 2, 'PASS', '[]', '{"user_trade_cnt_5m":1,"device_in_blacklist":false}', '{"finalAction":"PASS","hitRules":[],"latencyMs":12}', 1, 12, 'admin', 'admin', '2026-03-07 11:41:00', 'admin', '2026-03-07 11:41:00', b'0'),
(10106, 10006, 'TRADE_RISK', 'E_TRADE_0003', 8004, 2, 'REVIEW', '["TRADE_R003"]', '{"user_trade_cnt_5m":4,"device_in_blacklist":false,"device_bind_user_cnt_1h":2,"user_risk_level":"L"}', '{"finalAction":"REVIEW","hitRules":["TRADE_R003"],"latencyMs":18}', 1, 18, 'admin', 'admin', '2026-03-07 11:41:10', 'admin', '2026-03-07 11:41:10', b'0'),
(10107, 10007, 'TRADE_RISK', 'E_TRADE_0004', 8004, 2, 'REJECT', '["TRADE_R002"]', '{"device_in_blacklist":true}', '{"finalAction":"REJECT","hitRules":["TRADE_R002"],"latencyMs":10}', 1, 10, 'admin', 'admin', '2026-03-07 11:41:20', 'admin', '2026-03-07 11:41:20', b'0'),
(10108, 10008, 'TRADE_RISK', 'E_TRADE_0005', 8004, 2, 'REJECT', '["TRADE_R004"]', '{"device_bind_user_cnt_1h":5,"user_risk_level":"H"}', '{"finalAction":"REJECT","hitRules":["TRADE_R004"],"latencyMs":16}', 1, 16, 'admin', 'admin', '2026-03-07 11:41:30', 'admin', '2026-03-07 11:41:30', b'0');

INSERT INTO `decision_log` (`id`, `trace_id`, `event_id`, `scene_code`, `source_code`, `entity_id`, `final_action`, `final_score`, `version_no`, `latency_ms`, `risk_level`, `event_time`, `input_json`, `feature_snapshot_json`, `hit_rules_json`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`) VALUES
(11001, 'T_LOGIN_0003', 'E_LOGIN_0003', 'LOGIN_RISK', 'MEMBER_APP_HTTP', 'U1001', 'REVIEW', 60, 1, 15, 'MEDIUM', '2026-03-07 09:20:00', '{"eventId":"E_LOGIN_0003","sceneCode":"LOGIN_RISK","eventType":"login","userId":"U1001","deviceId":"D9002","ip":"10.20.30.40","loginResult":"FAIL"}', '{"user_login_fail_cnt_10m":6,"ip_risk_level":"HIGH"}', '["LOGIN_R002"]', '登录短时爆破样例', 'system', '2026-03-07 09:20:01', 'system', '2026-03-07 09:20:01', b'0'),
(11002, 'T_REG_0003', 'E_REG_0003', 'REGISTER_ANTI_FRAUD', 'MEMBER_APP_HTTP', 'U3999', 'REVIEW', 60, 1, 14, 'MEDIUM', '2026-03-07 10:35:00', '{"eventId":"E_REG_0003","sceneCode":"REGISTER_ANTI_FRAUD","eventType":"register","userId":"U3999","deviceId":"D_REG_001","ip":"22.33.44.55","mobile":"13800009999","registerResult":"SUCCESS"}', '{"device_register_user_cnt_1h":4}', '["REG_R003"]', '同设备批量注册样例', 'system', '2026-03-07 10:35:01', 'system', '2026-03-07 10:35:01', b'0'),
(11003, 'T_TRADE_0002', 'E_TRADE_0002', 'TRADE_RISK', 'TRADE_APP_HTTP', 'U5002', 'PASS', 0, 2, 12, 'LOW', '2026-03-07 11:10:00', '{"eventId":"E_TRADE_0002","sceneCode":"TRADE_RISK","eventType":"trade","userId":"U5002","deviceId":"D5002","ip":"3.3.3.3","amount":88,"merchantId":"M1002","tradeResult":"SUCCESS"}', '{"user_trade_cnt_5m":1,"device_in_blacklist":false}', '[]', '正常交易样例', 'system', '2026-03-07 11:10:01', 'system', '2026-03-07 11:10:01', b'0'),
(11004, 'T_TRADE_0003', 'E_TRADE_0003', 'TRADE_RISK', 'TRADE_APP_HTTP', 'U5001', 'REVIEW', 60, 2, 18, 'MEDIUM', '2026-03-07 11:15:00', '{"eventId":"E_TRADE_0003","sceneCode":"TRADE_RISK","eventType":"trade","userId":"U5001","deviceId":"D5001","ip":"66.77.88.99","amount":6800,"merchantId":"M1001","tradeResult":"SUCCESS"}', '{"user_trade_cnt_5m":4,"device_in_blacklist":false,"device_bind_user_cnt_1h":2,"user_risk_level":"L"}', '["TRADE_R003"]', '高频大额交易样例', 'system', '2026-03-07 11:15:01', 'system', '2026-03-07 11:15:01', b'0'),
(11005, 'T_TRADE_0004', 'E_TRADE_0004', 'TRADE_RISK', 'TRADE_APP_HTTP', 'U5003', 'REJECT', 100, 2, 10, 'HIGH', '2026-03-07 11:20:00', '{"eventId":"E_TRADE_0004","sceneCode":"TRADE_RISK","eventType":"trade","userId":"U5003","deviceId":"D_BLACK_001","ip":"77.88.99.11","amount":1200,"merchantId":"M1003","tradeResult":"SUCCESS"}', '{"device_in_blacklist":true}', '["TRADE_R002"]', '黑名单设备交易样例', 'system', '2026-03-07 11:20:01', 'system', '2026-03-07 11:20:01', b'0'),
(11006, 'T_TRADE_0005', 'E_TRADE_0005', 'TRADE_RISK', 'TRADE_APP_HTTP', 'U5004', 'REJECT', 90, 2, 16, 'HIGH', '2026-03-07 11:25:00', '{"eventId":"E_TRADE_0005","sceneCode":"TRADE_RISK","eventType":"trade","userId":"U5004","deviceId":"D_MULTI_001","ip":"99.88.77.66","amount":2600,"merchantId":"M1004","tradeResult":"SUCCESS"}', '{"device_bind_user_cnt_1h":5,"user_risk_level":"H"}', '["TRADE_R004"]', '高风险用户与多账号设备联动样例', 'system', '2026-03-07 11:25:01', 'system', '2026-03-07 11:25:01', b'0');

INSERT INTO `rule_hit_log` (`id`, `decision_id`, `rule_code`, `hit_flag`, `hit_action`, `score`, `hit_reason`, `hit_value_json`, `creator`, `create_time`, `updater`, `update_time`, `deleted`) VALUES
(12001, 11001, 'LOGIN_R002', 1, 'REVIEW', 60, '用户10分钟失败次数=6, IP风险等级=HIGH', '{"user_login_fail_cnt_10m":6,"ip_risk_level":"HIGH"}', 'system', '2026-03-07 09:20:01', 'system', '2026-03-07 09:20:01', b'0'),
(12002, 11002, 'REG_R003', 1, 'REVIEW', 60, '设备1小时注册用户数=4', '{"device_register_user_cnt_1h":4}', 'system', '2026-03-07 10:35:01', 'system', '2026-03-07 10:35:01', b'0'),
(12003, 11004, 'TRADE_R003', 1, 'REVIEW', 60, '用户5分钟交易次数=4, 当前金额=6800', '{"user_trade_cnt_5m":4,"amount":6800}', 'system', '2026-03-07 11:15:01', 'system', '2026-03-07 11:15:01', b'0'),
(12004, 11005, 'TRADE_R002', 1, 'REJECT', 100, '设备命中黑名单', '{"device_in_blacklist":true}', 'system', '2026-03-07 11:20:01', 'system', '2026-03-07 11:20:01', b'0'),
(12005, 11006, 'TRADE_R004', 1, 'REJECT', 90, '设备1小时关联用户数=5, 用户风险等级=H', '{"device_bind_user_cnt_1h":5,"user_risk_level":"H"}', 'system', '2026-03-07 11:25:01', 'system', '2026-03-07 11:25:01', b'0');

INSERT INTO `risk_event` (`id`, `risk_event_no`, `scene_code`, `trace_id`, `event_id`, `decision_id`, `source_type`, `final_action`, `risk_level`, `version_no`, `hit_rules_json`, `hit_summary`, `status`, `assigned_to`, `first_seen_time`, `last_seen_time`, `ext_json`, `creator`, `create_time`, `updater`, `update_time`, `deleted`) VALUES
(13001, 'RISK_EVT_20260307_001', 'LOGIN_RISK', 'T_LOGIN_0003', 'E_LOGIN_0003', 11001, 'DECISION_RESULT', 'REVIEW', 'MEDIUM', 1, '["LOGIN_R002"]', '用户10分钟失败次数过高且 IP 风险等级为 HIGH', 'OPEN', 'risk.ops', '2026-03-07 09:20:01', '2026-03-07 09:20:01', '{"category":"LOGIN"}', 'system', '2026-03-07 09:20:01', 'system', '2026-03-07 09:20:01', b'0'),
(13002, 'RISK_EVT_20260307_002', 'TRADE_RISK', 'T_TRADE_0003', 'E_TRADE_0003', 11004, 'DECISION_RESULT', 'REVIEW', 'MEDIUM', 2, '["TRADE_R003"]', '用户5分钟交易4次且当前金额6800，进入人工审核', 'OPEN', 'risk.ops', '2026-03-07 11:15:01', '2026-03-07 11:15:01', '{"category":"TRADE"}', 'system', '2026-03-07 11:15:01', 'system', '2026-03-07 11:15:01', b'0'),
(13003, 'RISK_EVT_20260307_003', 'TRADE_RISK', 'T_TRADE_0004', 'E_TRADE_0004', 11005, 'DECISION_RESULT', 'REJECT', 'HIGH', 2, '["TRADE_R002"]', '黑名单设备发起交易，直接拒绝', 'REVIEWING', 'risk.ops', '2026-03-07 11:20:01', '2026-03-07 11:20:01', '{"category":"TRADE"}', 'system', '2026-03-07 11:20:01', 'risk.ops', '2026-03-07 11:25:30', b'0'),
(13004, 'RISK_EVT_20260307_004', 'TRADE_RISK', 'T_TRADE_0005', 'E_TRADE_0005', 11006, 'DECISION_RESULT', 'REJECT', 'HIGH', 2, '["TRADE_R004"]', '高风险用户与多账号设备联动命中拒绝规则', 'OPEN', 'risk.ops', '2026-03-07 11:25:01', '2026-03-07 11:25:01', '{"category":"TRADE"}', 'system', '2026-03-07 11:25:01', 'system', '2026-03-07 11:25:01', b'0');

INSERT INTO `access_source_def` (`id`, `source_code`, `source_name`, `source_type`, `access_protocol`, `app_id`, `owner_name`, `contact_email`, `rate_limit_qps`, `allowed_scene_codes_json`, `ip_whitelist_json`, `status`, `description`, `creator`, `create_time`, `updater`, `update_time`, `deleted`) VALUES
(14001, 'MEMBER_APP_HTTP', '会员域 HTTP 接入', 'HTTP', 'HTTP', 'member-app', '张三', 'member-risk@example.com', 200, '["LOGIN_RISK","REGISTER_ANTI_FRAUD"]', '["10.10.0.0/16","172.16.0.0/12"]', 1, '服务登录与注册事件的 HTTP 接入源', 'admin', '2026-03-07 08:58:00', 'admin', '2026-03-07 08:58:00', b'0'),
(14002, 'TRADE_APP_HTTP', '交易域 HTTP 接入', 'HTTP', 'HTTP', 'trade-app', '李四', 'trade-risk@example.com', 300, '["TRADE_RISK"]', '["10.20.0.0/16","192.168.10.0/24"]', 1, '服务交易事件的 HTTP 接入源', 'admin', '2026-03-07 10:58:00', 'admin', '2026-03-07 10:58:00', b'0'),
(14003, 'TRADE_NETTY_SDK', '交易域 SDK 接入', 'SDK', 'TCP', 'trade-gateway', '王五', 'trade-gateway@example.com', 500, '["TRADE_RISK"]', '["172.20.1.0/24"]', 1, '服务后端高性能 SDK 接入', 'admin', '2026-03-07 10:59:00', 'admin', '2026-03-07 10:59:00', b'0');

INSERT INTO `access_auth_conf` (`id`, `source_code`, `auth_type`, `app_key`, `app_secret`, `sign_algo`, `signature_header`, `nonce_ttl_seconds`, `effective_from`, `expire_at`, `status`, `ext_json`, `creator`, `create_time`, `updater`, `update_time`, `deleted`) VALUES
(15001, 'MEMBER_APP_HTTP', 'API_KEY', 'member_http_key', 'member_http_secret_demo', 'NONE', 'X-API-Key', 0, '2026-03-07 09:00:00', NULL, 1, '{"headers":["X-API-Key"]}', 'admin', '2026-03-07 09:00:00', 'admin', '2026-03-07 09:00:00', b'0'),
(15002, 'TRADE_APP_HTTP', 'HMAC', 'trade_app_key', 'trade_app_secret_demo', 'HMAC_SHA256', 'X-Pulsix-Signature', 300, '2026-03-07 11:00:00', NULL, 1, '{"headers":["X-Pulsix-App-Key","X-Pulsix-Timestamp","X-Pulsix-Nonce","X-Pulsix-Signature"]}', 'admin', '2026-03-07 11:00:00', 'admin', '2026-03-07 11:00:00', b'0'),
(15003, 'TRADE_NETTY_SDK', 'HMAC', 'trade_sdk_key', 'trade_sdk_secret_demo', 'HMAC_SHA256', 'x-signature', 300, '2026-03-07 11:00:00', NULL, 1, '{"authFrameVersion":"v1"}', 'admin', '2026-03-07 11:00:00', 'admin', '2026-03-07 11:00:00', b'0');

INSERT INTO `risk_error_log` (`id`, `trace_id`, `event_id`, `source_code`, `scene_code`, `source_topic`, `error_stage`, `error_type`, `error_code`, `error_level`, `error_message`, `raw_payload`, `standard_payload_json`, `tag_json`, `process_status`, `process_remark`, `event_time`, `resolved_by`, `resolved_time`, `creator`, `create_time`, `updater`, `update_time`, `deleted`) VALUES
(16001, 'T_ERR_0001', NULL, 'TRADE_APP_HTTP', 'TRADE_RISK', 'pulsix.ingest.error', 'AUTH', 'INVALID_SIGNATURE', 'INGEST_401', 'HIGH', '签名校验失败，拒绝写入标准事件 Topic', '{"headers":{"X-Pulsix-App-Key":"trade_app_key","X-Pulsix-Signature":"invalid"},"body":{"eventId":"E_TRADE_BAD_0000"}}', NULL, '["INGEST","AUTH"]', 'OPEN', '待接入方修复签名逻辑', '2026-03-07 11:05:00', NULL, NULL, 'system', '2026-03-07 11:05:01', 'system', '2026-03-07 11:05:01', b'0'),
(16002, 'T_ERR_0002', 'E_TRADE_BAD_0001', 'TRADE_APP_HTTP', 'TRADE_RISK', 'pulsix.ingest.error', 'STANDARDIZE', 'MISSING_REQUIRED_FIELD', 'STD_422', 'MEDIUM', '标准化失败，缺少必填字段 userId', '{"eventId":"E_TRADE_BAD_0001","sceneCode":"TRADE_RISK","eventType":"trade","amount":3200}', '{"eventId":"E_TRADE_BAD_0001","sceneCode":"TRADE_RISK","eventType":"trade","amount":3200}', '["INGEST","STANDARDIZE","userId"]', 'OPEN', '等待业务方补齐 userId 字段', '2026-03-07 11:12:00', NULL, NULL, 'system', '2026-03-07 11:12:01', 'system', '2026-03-07 11:12:01', b'0'),
(16003, 'T_ERR_0003', 'E_TRADE_DLQ_0001', 'TRADE_APP_HTTP', 'TRADE_RISK', 'pulsix.event.dlq', 'DLQ', 'INVALID_EVENT', 'DLQ_001', 'MEDIUM', '事件因标准化失败被打入 DLQ', '{"eventId":"E_TRADE_DLQ_0001","sceneCode":"TRADE_RISK","eventType":"trade","deviceId":"D5009"}', '{"eventId":"E_TRADE_DLQ_0001","sceneCode":"TRADE_RISK","eventType":"trade","deviceId":"D5009"}', '["DLQ","REPLAYABLE"]', 'REVIEWED', '已通知接入方修复并准备回放', '2026-03-07 11:18:00', 'risk.ops', '2026-03-07 11:40:00', 'system', '2026-03-07 11:18:01', 'risk.ops', '2026-03-07 11:40:00', b'0'),
(16004, 'T_ERR_0004', 'E_ENGINE_0001', 'TRADE_APP_HTTP', 'TRADE_RISK', 'pulsix.engine.error', 'ENGINE_EXECUTE', 'LOOKUP_TIMEOUT', 'ENG_504', 'HIGH', 'merchant_risk_level 查询超时，使用默认值并记录异常', '{"eventId":"E_ENGINE_0001","sceneCode":"TRADE_RISK","merchantId":"M1009"}', '{"eventId":"E_ENGINE_0001","sceneCode":"TRADE_RISK","merchantId":"M1009","defaultMerchantRiskLevel":"LOW"}', '["ENGINE","LOOKUP_TIMEOUT"]', 'OPEN', '待排查画像 Redis 超时', '2026-03-07 11:30:00', NULL, NULL, 'system', '2026-03-07 11:30:01', 'system', '2026-03-07 11:30:01', b'0');

INSERT INTO `alert_channel_def` (`id`, `channel_code`, `channel_name`, `channel_type`, `webhook_url`, `secret`, `headers_json`, `status`, `description`, `creator`, `create_time`, `updater`, `update_time`, `deleted`) VALUES
(17001, 'FEISHU_RISK_OPS', '飞书风控运营群', 'FEISHU', 'https://open.feishu.cn/open-apis/bot/v2/hook/demo-risk-ops', 'feishu_demo_secret', '{"Content-Type":"application/json"}', 1, '交易风险与登录审单告警发送到飞书群', 'admin', '2026-03-07 11:00:00', 'admin', '2026-03-07 11:00:00', b'0'),
(17002, 'WEBHOOK_SRE', 'SRE 通用 Webhook', 'WEBHOOK', 'https://notify.example.com/pulsix/risk', NULL, '{"Content-Type":"application/json","X-Token":"demo-token"}', 1, '接入错误与引擎异常统一发往 SRE 告警网关', 'admin', '2026-03-07 11:00:00', 'admin', '2026-03-07 11:00:00', b'0');

INSERT INTO `alert_template_def` (`id`, `template_code`, `template_name`, `source_type`, `title_template`, `content_template`, `format_type`, `status`, `description`, `creator`, `create_time`, `updater`, `update_time`, `deleted`) VALUES
(18001, 'TPL_DECISION_RISK', '风险决策告警模板', 'DECISION_RESULT', '[{sceneCode}] {finalAction} 风险事件', '事件 {eventId} 命中动作 {finalAction}，规则={hitRules}，traceId={traceId}，riskLevel={riskLevel}', 'TEXT', 1, '用于 REVIEW/REJECT 风险事件告警', 'admin', '2026-03-07 11:01:00', 'admin', '2026-03-07 11:01:00', b'0'),
(18002, 'TPL_STREAM_ERROR', '错误流告警模板', 'ERROR_STREAM', '[Pulsix] {errorStage} 异常', 'traceId={traceId}，eventId={eventId}，stage={errorStage}，message={errorMessage}', 'TEXT', 1, '用于 ingest/dlq/engine error 告警', 'admin', '2026-03-07 11:01:00', 'admin', '2026-03-07 11:01:00', b'0');

INSERT INTO `alert_rule_def` (`id`, `alert_rule_code`, `alert_rule_name`, `scene_code`, `source_type`, `action_filter_json`, `error_stage_filter_json`, `alert_level`, `dedupe_window_seconds`, `rate_limit_json`, `silence_conf_json`, `template_code`, `channel_codes_json`, `status`, `description`, `creator`, `create_time`, `updater`, `update_time`, `deleted`) VALUES
(19001, 'ALR_TRADE_DECISION', '交易风险决策告警', 'TRADE_RISK', 'DECISION_RESULT', '["REVIEW","REJECT"]', NULL, 'HIGH', 300, '{"windowSeconds":60,"limit":20}', '{"enabled":false}', 'TPL_DECISION_RISK', '["FEISHU_RISK_OPS"]', 1, '交易场景 REVIEW/REJECT 结果触发告警', 'admin', '2026-03-07 11:02:00', 'admin', '2026-03-07 11:02:00', b'0'),
(19002, 'ALR_ERROR_STREAM', '错误流统一告警', NULL, 'ERROR_STREAM', NULL, '["AUTH","STANDARDIZE","DLQ","ENGINE_EXECUTE"]', 'MEDIUM', 600, '{"windowSeconds":60,"limit":50}', '{"enabled":true,"timeRanges":[{"start":"00:00","end":"06:00"}]}', 'TPL_STREAM_ERROR', '["WEBHOOK_SRE","FEISHU_RISK_OPS"]', 1, '接入与引擎异常统一告警', 'admin', '2026-03-07 11:02:00', 'admin', '2026-03-07 11:02:00', b'0');

INSERT INTO `alert_record` (`id`, `alert_rule_code`, `source_type`, `source_id`, `trace_id`, `event_id`, `scene_code`, `alert_level`, `channel_code`, `template_code`, `send_status`, `dedupe_key`, `dedupe_hit_flag`, `silence_hit_flag`, `request_payload`, `response_payload`, `sent_at`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`) VALUES
(20001, 'ALR_TRADE_DECISION', 'DECISION_RESULT', 'RISK_EVT_20260307_003', 'T_TRADE_0004', 'E_TRADE_0004', 'TRADE_RISK', 'HIGH', 'FEISHU_RISK_OPS', 'TPL_DECISION_RISK', 'SUCCESS', 'TRADE_RISK:REJECT:E_TRADE_0004', b'0', b'0', '{"traceId":"T_TRADE_0004","eventId":"E_TRADE_0004","finalAction":"REJECT","hitRules":["TRADE_R002"]}', '{"code":0,"msg":"ok"}', '2026-03-07 11:20:05', '黑名单设备交易告警发送成功', 'system', '2026-03-07 11:20:05', 'system', '2026-03-07 11:20:05', b'0'),
(20002, 'ALR_ERROR_STREAM', 'ERROR_STREAM', '16002', 'T_ERR_0002', 'E_TRADE_BAD_0001', 'TRADE_RISK', 'MEDIUM', 'WEBHOOK_SRE', 'TPL_STREAM_ERROR', 'SUCCESS', 'ERROR_STREAM:STANDARDIZE:TRADE_APP_HTTP', b'0', b'0', '{"traceId":"T_ERR_0002","eventId":"E_TRADE_BAD_0001","errorStage":"STANDARDIZE","errorMessage":"标准化失败，缺少必填字段 userId"}', '{"status":"accepted","requestId":"req-demo-001"}', '2026-03-07 11:12:05', '标准化失败告警发送成功', 'system', '2026-03-07 11:12:05', 'system', '2026-03-07 11:12:05', b'0'),
(20003, 'ALR_ERROR_STREAM', 'ERROR_STREAM', '16004', 'T_ERR_0004', 'E_ENGINE_0001', 'TRADE_RISK', 'HIGH', 'FEISHU_RISK_OPS', 'TPL_STREAM_ERROR', 'SKIPPED', 'ERROR_STREAM:ENGINE_EXECUTE:TRADE_RISK', b'1', b'0', '{"traceId":"T_ERR_0004","eventId":"E_ENGINE_0001","errorStage":"ENGINE_EXECUTE"}', '{"reason":"duplicated within 600s"}', '2026-03-07 11:31:00', '命中去重窗口，跳过重复告警', 'system', '2026-03-07 11:31:00', 'system', '2026-03-07 11:31:00', b'0');

COMMIT;

SET FOREIGN_KEY_CHECKS = 1;
