/*
 pulsix-risk.sql

 基于下列文档整理的一期风险模块初始化 SQL：
 - docs/实时风控系统.md
 - docs/wiki/实时风控系统第7章：控制平台的数据模型设计.md
 - docs/wiki/实时风控系统第21章：数据库、Redis、Kafka 的落地模型设计.md
 - docs/wiki/实时风控系统附录A：完整快照 JSON 示例.md
 - docs/wiki/实时风控系统附录D：第一版 DDL 与 Redis Key 设计.md
 - docs/wiki/实时风控系统附录E：三大风控场景的样例规则与样例事件.md（仅辅助通用建模，不作为一期代表场景种子数据主来源）

 设计说明：
 1. 覆盖风险控制面一期核心表，以及主文档中已经明确列出的接入治理、错误治理、告警中心、风险事件查询相关表。
 2. 所有表统一补齐 BaseDO 字段，保持和项目内通用审计字段风格一致。
 3. 接入指引页直接复用 event_schema.sample_event_json、event_field_def、access_source_def，不单独拆表。
 4. 不添加物理外键，避免影响发布快照、历史版本、异步日志与回放数据的灵活写入。
 5. 针对 feature_code 在不同场景下会重复出现的实际情况，feature_*_conf 统一增加 scene_code 作为联合唯一键。
 6. 示例数据以 docs/实时风控系统.md 中“一期代表性业务场景”为准，统一切换为 PROMOTION_RISK、WITHDRAW_RISK、ORDER_RISK 三个异步闭环场景。

 存量库升级说明：
 如果数据库里已经存在旧版 `scene_def`，且只想收敛本次场景管理冗余字段，请单独执行下面语句，不要与本文件的全量初始化一起混跑：

 ALTER TABLE `scene_def`
     DROP COLUMN `scene_type`,
     DROP COLUMN `decision_timeout_ms`,
     DROP COLUMN `log_level`;
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
DROP TABLE IF EXISTS `event_access_binding`;
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
  `runtime_mode` varchar(32) NOT NULL DEFAULT 'ASYNC_DECISION' COMMENT '运行模式',
  `default_policy_code` varchar(64) DEFAULT NULL COMMENT '默认策略编码',
  `status` tinyint NOT NULL DEFAULT 0 COMMENT '状态',
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
  `topic_name` varchar(128) NOT NULL COMMENT '标准事件 Topic',
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

CREATE TABLE `event_access_binding` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `event_code` varchar(64) NOT NULL COMMENT '事件编码',
  `source_code` varchar(64) NOT NULL COMMENT '接入源编码',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_event_source` (`event_code`, `source_code`),
  KEY `idx_source_code` (`source_code`),
  KEY `idx_event_code` (`event_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='事件接入绑定表';

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

INSERT INTO `scene_def` (`id`, `scene_code`, `scene_name`, `runtime_mode`, `default_policy_code`, `status`, `description`, `creator`, `create_time`, `updater`, `update_time`, `deleted`) VALUES
(1, 'PROMOTION_RISK', '营销反作弊', 'ASYNC_DECISION', 'PROMOTION_RISK_POLICY_FIRST_HIT', 0, '用于领券、邀新奖励、积分发放等营销动作受理后的异步风控', 'admin', '2026-03-08 09:30:00', 'admin', '2026-03-08 09:30:00', b'0'),
(2, 'WITHDRAW_RISK', '提现审核风控', 'ASYNC_DECISION', 'WITHDRAW_RISK_POLICY_FIRST_HIT', 0, '用于提现申请落单后、打款前的异步审核场景', 'admin', '2026-03-08 09:40:00', 'admin', '2026-03-08 09:40:00', b'0'),
(3, 'ORDER_RISK', '订单后置风控', 'ASYNC_DECISION', 'ORDER_RISK_POLICY_FIRST_HIT', 0, '用于支付成功后、发货前的异步订单风险拦截', 'admin', '2026-03-08 09:50:00', 'admin', '2026-03-08 09:50:00', b'0');

INSERT INTO `entity_type_def` (`id`, `entity_type`, `entity_name`, `key_field_name`, `sample_value`, `status`, `description`, `creator`, `create_time`, `updater`, `update_time`, `deleted`) VALUES
(1, 'USER', '用户', 'userId', 'U20001', 1, '用户维度，适用于营销、提现、订单三类场景', 'admin', '2026-03-08 09:32:00', 'admin', '2026-03-08 09:32:00', b'0'),
(2, 'DEVICE', '设备', 'deviceId', 'DVC_PROMO_001', 1, '设备维度，适用于设备黑名单、共用设备识别', 'admin', '2026-03-08 09:32:00', 'admin', '2026-03-08 09:32:00', b'0'),
(3, 'IP', 'IP 地址', 'ip', '203.0.113.10', 1, 'IP 维度，适用于代理 IP 与高频活动识别', 'admin', '2026-03-08 09:32:00', 'admin', '2026-03-08 09:32:00', b'0'),
(4, 'ACTIVITY', '营销活动', 'activityId', 'ACT_INVITE_202603', 1, '营销活动维度，适用于活动套利分析', 'admin', '2026-03-08 09:32:00', 'admin', '2026-03-08 09:32:00', b'0'),
(5, 'WITHDRAW_ACCOUNT', '提现账户', 'withdrawAccountId', 'WACC_88001', 1, '提现账户维度，适用于账户关联分析', 'admin', '2026-03-08 09:32:00', 'admin', '2026-03-08 09:32:00', b'0'),
(6, 'ORDER', '订单', 'orderNo', 'ORD202603080001', 1, '订单维度，适用于订单后置风控追踪', 'admin', '2026-03-08 09:32:00', 'admin', '2026-03-08 09:32:00', b'0');

INSERT INTO `event_schema` (`id`, `scene_code`, `event_code`, `event_name`, `event_type`, `sample_event_json`, `version`, `status`, `description`, `creator`, `create_time`, `updater`, `update_time`, `deleted`) VALUES
(1, 'PROMOTION_RISK', 'PROMOTION_EVENT', '营销受理事件', 'promotion_grant', '{"eventId":"E_PROMO_0001","traceId":"T_PROMO_0001","sceneCode":"PROMOTION_RISK","eventType":"promotion_grant","eventTime":"2026-03-08T10:00:00","userId":"U20001","deviceId":"DVC_PROMO_001","ip":"203.0.113.10","channel":"APP","bizNo":"PROMO_REQ_202603080001","activityId":"ACT_INVITE_202603","promotionType":"INVITE_REWARD","rewardType":"COUPON","rewardValue":30,"grantStatus":"ACCEPTED","ext":{"inviteUserId":"U18888","campaignCode":"SPRING_GROWTH"}}', 1, 1, '营销受理标准事件模型', 'admin', '2026-03-08 10:00:00', 'admin', '2026-03-08 10:00:00', b'0'),
(2, 'WITHDRAW_RISK', 'WITHDRAW_EVENT', '提现申请事件', 'withdraw_apply', '{"eventId":"E_WD_0001","traceId":"T_WD_0001","sceneCode":"WITHDRAW_RISK","eventType":"withdraw_apply","eventTime":"2026-03-08T11:00:00","userId":"U30001","deviceId":"DVC_WD_001","ip":"198.51.100.21","channel":"APP","bizNo":"WD_REQ_202603080001","withdrawNo":"WD202603080001","withdrawAmount":3500,"currency":"CNY","withdrawAccountId":"WACC_88001","accountType":"BANK_CARD","bankCode":"ICBC","withdrawStatus":"CREATED","ext":{"city":"Hangzhou","bankCardTail":"1024"}}', 1, 1, '提现申请标准事件模型', 'admin', '2026-03-08 11:00:00', 'admin', '2026-03-08 11:00:00', b'0'),
(3, 'ORDER_RISK', 'ORDER_EVENT', '支付成功事件', 'order_paid', '{"eventId":"E_ORDER_0001","traceId":"T_ORDER_0001","sceneCode":"ORDER_RISK","eventType":"order_paid","eventTime":"2026-03-08T12:00:00","userId":"U40001","deviceId":"DVC_ORDER_001","ip":"192.0.2.18","channel":"APP","bizNo":"ORD202603080001","orderNo":"ORD202603080001","orderAmount":1299,"payAmount":1299,"currency":"CNY","merchantId":"MCH_3001","fulfillmentType":"EXPRESS","orderStatus":"PAID","ext":{"skuCount":2,"receiverCity":"Shanghai"}}', 1, 1, '订单支付成功标准事件模型', 'admin', '2026-03-08 12:00:00', 'admin', '2026-03-08 12:00:00', b'0');

INSERT INTO `event_field_def` (`id`, `event_code`, `field_name`, `field_label`, `field_type`, `required_flag`, `default_value`, `sample_value`, `description`, `sort_no`, `ext_json`, `creator`, `create_time`, `updater`, `update_time`, `deleted`) VALUES
(101, 'PROMOTION_EVENT', 'eventId', '事件ID', 'STRING', 1, NULL, 'E_PROMO_0001', '事件唯一标识', 1, NULL, 'admin', '2026-03-08 10:01:00', 'admin', '2026-03-08 10:01:00', b'0'),
(102, 'PROMOTION_EVENT', 'traceId', '链路ID', 'STRING', 1, NULL, 'T_PROMO_0001', '全链路追踪号', 2, NULL, 'admin', '2026-03-08 10:01:00', 'admin', '2026-03-08 10:01:00', b'0'),
(103, 'PROMOTION_EVENT', 'sceneCode', '场景编码', 'STRING', 1, 'PROMOTION_RISK', 'PROMOTION_RISK', '场景编码', 3, NULL, 'admin', '2026-03-08 10:01:00', 'admin', '2026-03-08 10:01:00', b'0'),
(104, 'PROMOTION_EVENT', 'eventType', '事件类型', 'STRING', 1, 'promotion_grant', 'promotion_grant', '事件类型', 4, NULL, 'admin', '2026-03-08 10:01:00', 'admin', '2026-03-08 10:01:00', b'0'),
(105, 'PROMOTION_EVENT', 'eventTime', '事件时间', 'DATETIME', 1, NULL, '2026-03-08T10:00:00', '事件发生时间', 5, NULL, 'admin', '2026-03-08 10:01:00', 'admin', '2026-03-08 10:01:00', b'0'),
(106, 'PROMOTION_EVENT', 'userId', '用户ID', 'STRING', 1, NULL, 'U20001', '用户主键', 6, NULL, 'admin', '2026-03-08 10:01:00', 'admin', '2026-03-08 10:01:00', b'0'),
(107, 'PROMOTION_EVENT', 'deviceId', '设备ID', 'STRING', 1, NULL, 'DVC_PROMO_001', '设备主键', 7, NULL, 'admin', '2026-03-08 10:01:00', 'admin', '2026-03-08 10:01:00', b'0'),
(108, 'PROMOTION_EVENT', 'ip', 'IP', 'STRING', 1, NULL, '203.0.113.10', '客户端 IP', 8, NULL, 'admin', '2026-03-08 10:01:00', 'admin', '2026-03-08 10:01:00', b'0'),
(109, 'PROMOTION_EVENT', 'channel', '渠道', 'STRING', 1, 'APP', 'APP', '请求渠道', 9, NULL, 'admin', '2026-03-08 10:01:00', 'admin', '2026-03-08 10:01:00', b'0'),
(110, 'PROMOTION_EVENT', 'bizNo', '业务请求号', 'STRING', 1, NULL, 'PROMO_REQ_202603080001', '营销业务请求号', 10, NULL, 'admin', '2026-03-08 10:01:00', 'admin', '2026-03-08 10:01:00', b'0'),
(111, 'PROMOTION_EVENT', 'activityId', '活动ID', 'STRING', 1, NULL, 'ACT_INVITE_202603', '营销活动编号', 11, NULL, 'admin', '2026-03-08 10:01:00', 'admin', '2026-03-08 10:01:00', b'0'),
(112, 'PROMOTION_EVENT', 'promotionType', '营销类型', 'STRING', 1, NULL, 'INVITE_REWARD', '营销受理类型', 12, NULL, 'admin', '2026-03-08 10:01:00', 'admin', '2026-03-08 10:01:00', b'0'),
(113, 'PROMOTION_EVENT', 'rewardType', '奖励类型', 'STRING', 1, NULL, 'COUPON', '奖励介质类型', 13, NULL, 'admin', '2026-03-08 10:01:00', 'admin', '2026-03-08 10:01:00', b'0'),
(114, 'PROMOTION_EVENT', 'rewardValue', '奖励面额', 'DECIMAL', 1, NULL, '30', '本次发放奖励面额', 14, NULL, 'admin', '2026-03-08 10:01:00', 'admin', '2026-03-08 10:01:00', b'0'),
(115, 'PROMOTION_EVENT', 'grantStatus', '受理状态', 'STRING', 1, NULL, 'ACCEPTED', '营销动作受理状态', 15, NULL, 'admin', '2026-03-08 10:01:00', 'admin', '2026-03-08 10:01:00', b'0'),
(116, 'PROMOTION_EVENT', 'ext', '扩展字段', 'JSON', 0, NULL, '{"inviteUserId":"U18888","campaignCode":"SPRING_GROWTH"}', '扩展信息', 16, NULL, 'admin', '2026-03-08 10:01:00', 'admin', '2026-03-08 10:01:00', b'0'),
(201, 'WITHDRAW_EVENT', 'eventId', '事件ID', 'STRING', 1, NULL, 'E_WD_0001', '事件唯一标识', 1, NULL, 'admin', '2026-03-08 11:01:00', 'admin', '2026-03-08 11:01:00', b'0'),
(202, 'WITHDRAW_EVENT', 'traceId', '链路ID', 'STRING', 1, NULL, 'T_WD_0001', '全链路追踪号', 2, NULL, 'admin', '2026-03-08 11:01:00', 'admin', '2026-03-08 11:01:00', b'0'),
(203, 'WITHDRAW_EVENT', 'sceneCode', '场景编码', 'STRING', 1, 'WITHDRAW_RISK', 'WITHDRAW_RISK', '场景编码', 3, NULL, 'admin', '2026-03-08 11:01:00', 'admin', '2026-03-08 11:01:00', b'0'),
(204, 'WITHDRAW_EVENT', 'eventType', '事件类型', 'STRING', 1, 'withdraw_apply', 'withdraw_apply', '事件类型', 4, NULL, 'admin', '2026-03-08 11:01:00', 'admin', '2026-03-08 11:01:00', b'0'),
(205, 'WITHDRAW_EVENT', 'eventTime', '事件时间', 'DATETIME', 1, NULL, '2026-03-08T11:00:00', '事件发生时间', 5, NULL, 'admin', '2026-03-08 11:01:00', 'admin', '2026-03-08 11:01:00', b'0'),
(206, 'WITHDRAW_EVENT', 'userId', '用户ID', 'STRING', 1, NULL, 'U30001', '用户主键', 6, NULL, 'admin', '2026-03-08 11:01:00', 'admin', '2026-03-08 11:01:00', b'0'),
(207, 'WITHDRAW_EVENT', 'deviceId', '设备ID', 'STRING', 1, NULL, 'DVC_WD_001', '设备主键', 7, NULL, 'admin', '2026-03-08 11:01:00', 'admin', '2026-03-08 11:01:00', b'0'),
(208, 'WITHDRAW_EVENT', 'ip', 'IP', 'STRING', 1, NULL, '198.51.100.21', '客户端 IP', 8, NULL, 'admin', '2026-03-08 11:01:00', 'admin', '2026-03-08 11:01:00', b'0'),
(209, 'WITHDRAW_EVENT', 'channel', '渠道', 'STRING', 1, 'APP', 'APP', '请求渠道', 9, NULL, 'admin', '2026-03-08 11:01:00', 'admin', '2026-03-08 11:01:00', b'0'),
(210, 'WITHDRAW_EVENT', 'bizNo', '业务请求号', 'STRING', 1, NULL, 'WD_REQ_202603080001', '提现业务请求号', 10, NULL, 'admin', '2026-03-08 11:01:00', 'admin', '2026-03-08 11:01:00', b'0'),
(211, 'WITHDRAW_EVENT', 'withdrawNo', '提现单号', 'STRING', 1, NULL, 'WD202603080001', '提现申请单号', 11, NULL, 'admin', '2026-03-08 11:01:00', 'admin', '2026-03-08 11:01:00', b'0'),
(212, 'WITHDRAW_EVENT', 'withdrawAmount', '提现金额', 'DECIMAL', 1, NULL, '3500', '本次提现金额', 12, NULL, 'admin', '2026-03-08 11:01:00', 'admin', '2026-03-08 11:01:00', b'0'),
(213, 'WITHDRAW_EVENT', 'currency', '币种', 'STRING', 1, 'CNY', 'CNY', '币种', 13, NULL, 'admin', '2026-03-08 11:01:00', 'admin', '2026-03-08 11:01:00', b'0'),
(214, 'WITHDRAW_EVENT', 'withdrawAccountId', '提现账户ID', 'STRING', 1, NULL, 'WACC_88001', '提现账户主键', 14, NULL, 'admin', '2026-03-08 11:01:00', 'admin', '2026-03-08 11:01:00', b'0'),
(215, 'WITHDRAW_EVENT', 'accountType', '账户类型', 'STRING', 1, NULL, 'BANK_CARD', '提现账户类型', 15, NULL, 'admin', '2026-03-08 11:01:00', 'admin', '2026-03-08 11:01:00', b'0'),
(216, 'WITHDRAW_EVENT', 'bankCode', '银行编码', 'STRING', 1, NULL, 'ICBC', '收款银行编码', 16, NULL, 'admin', '2026-03-08 11:01:00', 'admin', '2026-03-08 11:01:00', b'0'),
(217, 'WITHDRAW_EVENT', 'withdrawStatus', '提现状态', 'STRING', 1, NULL, 'CREATED', '提现申请当前状态', 17, NULL, 'admin', '2026-03-08 11:01:00', 'admin', '2026-03-08 11:01:00', b'0'),
(218, 'WITHDRAW_EVENT', 'ext', '扩展字段', 'JSON', 0, NULL, '{"city":"Hangzhou","bankCardTail":"1024"}', '扩展信息', 18, NULL, 'admin', '2026-03-08 11:01:00', 'admin', '2026-03-08 11:01:00', b'0'),
(301, 'ORDER_EVENT', 'eventId', '事件ID', 'STRING', 1, NULL, 'E_ORDER_0001', '事件唯一标识', 1, NULL, 'admin', '2026-03-08 12:01:00', 'admin', '2026-03-08 12:01:00', b'0'),
(302, 'ORDER_EVENT', 'traceId', '链路ID', 'STRING', 1, NULL, 'T_ORDER_0001', '全链路追踪号', 2, NULL, 'admin', '2026-03-08 12:01:00', 'admin', '2026-03-08 12:01:00', b'0'),
(303, 'ORDER_EVENT', 'sceneCode', '场景编码', 'STRING', 1, 'ORDER_RISK', 'ORDER_RISK', '场景编码', 3, NULL, 'admin', '2026-03-08 12:01:00', 'admin', '2026-03-08 12:01:00', b'0'),
(304, 'ORDER_EVENT', 'eventType', '事件类型', 'STRING', 1, 'order_paid', 'order_paid', '事件类型', 4, NULL, 'admin', '2026-03-08 12:01:00', 'admin', '2026-03-08 12:01:00', b'0'),
(305, 'ORDER_EVENT', 'eventTime', '事件时间', 'DATETIME', 1, NULL, '2026-03-08T12:00:00', '事件发生时间', 5, NULL, 'admin', '2026-03-08 12:01:00', 'admin', '2026-03-08 12:01:00', b'0'),
(306, 'ORDER_EVENT', 'userId', '用户ID', 'STRING', 1, NULL, 'U40001', '用户主键', 6, NULL, 'admin', '2026-03-08 12:01:00', 'admin', '2026-03-08 12:01:00', b'0'),
(307, 'ORDER_EVENT', 'deviceId', '设备ID', 'STRING', 1, NULL, 'DVC_ORDER_001', '设备主键', 7, NULL, 'admin', '2026-03-08 12:01:00', 'admin', '2026-03-08 12:01:00', b'0'),
(308, 'ORDER_EVENT', 'ip', 'IP', 'STRING', 1, NULL, '192.0.2.18', '客户端 IP', 8, NULL, 'admin', '2026-03-08 12:01:00', 'admin', '2026-03-08 12:01:00', b'0'),
(309, 'ORDER_EVENT', 'channel', '渠道', 'STRING', 1, 'APP', 'APP', '请求渠道', 9, NULL, 'admin', '2026-03-08 12:01:00', 'admin', '2026-03-08 12:01:00', b'0'),
(310, 'ORDER_EVENT', 'bizNo', '业务请求号', 'STRING', 1, NULL, 'ORD202603080001', '订单业务号', 10, NULL, 'admin', '2026-03-08 12:01:00', 'admin', '2026-03-08 12:01:00', b'0'),
(311, 'ORDER_EVENT', 'orderNo', '订单号', 'STRING', 1, NULL, 'ORD202603080001', '订单主键', 11, NULL, 'admin', '2026-03-08 12:01:00', 'admin', '2026-03-08 12:01:00', b'0'),
(312, 'ORDER_EVENT', 'orderAmount', '订单金额', 'DECIMAL', 1, NULL, '1299', '订单应付金额', 12, NULL, 'admin', '2026-03-08 12:01:00', 'admin', '2026-03-08 12:01:00', b'0'),
(313, 'ORDER_EVENT', 'payAmount', '实付金额', 'DECIMAL', 1, NULL, '1299', '订单实付金额', 13, NULL, 'admin', '2026-03-08 12:01:00', 'admin', '2026-03-08 12:01:00', b'0'),
(314, 'ORDER_EVENT', 'currency', '币种', 'STRING', 1, 'CNY', 'CNY', '币种', 14, NULL, 'admin', '2026-03-08 12:01:00', 'admin', '2026-03-08 12:01:00', b'0'),
(315, 'ORDER_EVENT', 'merchantId', '商户ID', 'STRING', 1, NULL, 'MCH_3001', '商户编号', 15, NULL, 'admin', '2026-03-08 12:01:00', 'admin', '2026-03-08 12:01:00', b'0'),
(316, 'ORDER_EVENT', 'fulfillmentType', '履约方式', 'STRING', 1, NULL, 'EXPRESS', '履约方式', 16, NULL, 'admin', '2026-03-08 12:01:00', 'admin', '2026-03-08 12:01:00', b'0'),
(317, 'ORDER_EVENT', 'orderStatus', '订单状态', 'STRING', 1, NULL, 'PAID', '订单当前状态', 17, NULL, 'admin', '2026-03-08 12:01:00', 'admin', '2026-03-08 12:01:00', b'0'),
(318, 'ORDER_EVENT', 'ext', '扩展字段', 'JSON', 0, NULL, '{"skuCount":2,"receiverCity":"Shanghai"}', '扩展信息', 18, NULL, 'admin', '2026-03-08 12:01:00', 'admin', '2026-03-08 12:01:00', b'0');

INSERT INTO `list_set` (`id`, `scene_code`, `list_code`, `list_name`, `match_type`, `list_type`, `sync_mode`, `redis_key_prefix`, `last_sync_time`, `status`, `description`, `creator`, `create_time`, `updater`, `update_time`, `deleted`) VALUES
(1, 'PROMOTION_RISK', 'PROMOTION_PROXY_IP_WATCH_LIST', '营销代理IP观察名单', 'IP', 'WATCH', 'MANUAL', 'pulsix:list:watch:promotion:ip_proxy', '2026-03-08 10:06:00', 1, '用于识别营销受理链路中的代理/机房 IP', 'admin', '2026-03-08 10:02:00', 'admin', '2026-03-08 10:06:00', b'0'),
(2, 'WITHDRAW_RISK', 'WITHDRAW_ACCOUNT_OBSERVE_LIST', '提现账户观察名单', 'WITHDRAW_ACCOUNT', 'WATCH', 'MANUAL', 'pulsix:list:watch:withdraw:account', '2026-03-08 11:06:00', 1, '用于沉淀历史可疑提现账户，供运营跟踪', 'admin', '2026-03-08 11:02:00', 'admin', '2026-03-08 11:06:00', b'0'),
(3, 'ORDER_RISK', 'ORDER_DEVICE_BLACKLIST', '订单设备黑名单', 'DEVICE', 'BLACK', 'MANUAL', 'pulsix:list:black:order:device', '2026-03-08 12:06:00', 1, '用于拦截支付成功后的高风险设备订单', 'admin', '2026-03-08 12:02:00', 'admin', '2026-03-08 12:06:00', b'0'),
(4, 'ORDER_RISK', 'ORDER_USER_WHITE_LIST', '订单用户白名单', 'USER', 'WHITE', 'MANUAL', 'pulsix:list:white:order:user', '2026-03-08 12:06:00', 1, '用于保护高价值可信用户不被误拒', 'admin', '2026-03-08 12:02:00', 'admin', '2026-03-08 12:06:00', b'0');

INSERT INTO `list_item` (`id`, `list_code`, `match_key`, `match_value`, `source_type`, `expire_at`, `tag_json`, `status`, `remark`, `ext_json`, `creator`, `create_time`, `updater`, `update_time`, `deleted`) VALUES
(1, 'PROMOTION_PROXY_IP_WATCH_LIST', 'ip', '203.0.113.10', 'MANUAL_IMPORT', NULL, '["proxy","marketing"]', 1, '营销代理 IP 风险样例', '{"provider":"cloud_proxy_a"}', 'admin', '2026-03-08 10:03:00', 'admin', '2026-03-08 10:03:00', b'0'),
(2, 'PROMOTION_PROXY_IP_WATCH_LIST', 'ip', '203.0.113.11', 'MANUAL_IMPORT', NULL, '["proxy","coupon_farm"]', 1, '营销代理 IP 补充样例', '{"provider":"cloud_proxy_b"}', 'admin', '2026-03-08 10:03:00', 'admin', '2026-03-08 10:03:00', b'0'),
(3, 'WITHDRAW_ACCOUNT_OBSERVE_LIST', 'withdrawAccountId', 'WACC_99002', 'MANUAL_IMPORT', NULL, '["manual_watch","shared_account"]', 1, '人工观察中的提现账户样例', '{"source":"ops_watchlist"}', 'admin', '2026-03-08 11:03:00', 'admin', '2026-03-08 11:03:00', b'0'),
(4, 'ORDER_DEVICE_BLACKLIST', 'deviceId', 'DVC_ORDER_BLACK_01', 'MANUAL_IMPORT', NULL, '["device_black","refund_abuse"]', 1, '订单设备黑名单样例', '{"source":"manual_import"}', 'admin', '2026-03-08 12:03:00', 'admin', '2026-03-08 12:03:00', b'0'),
(5, 'ORDER_USER_WHITE_LIST', 'userId', 'U40088', 'MANUAL_IMPORT', NULL, '["vip","trusted_user"]', 1, '订单白名单样例', '{"source":"ops_whitelist"}', 'admin', '2026-03-08 12:03:00', 'admin', '2026-03-08 12:03:00', b'0');

INSERT INTO `feature_def` (`id`, `scene_code`, `feature_code`, `feature_name`, `feature_type`, `entity_type`, `event_code`, `value_type`, `status`, `version`, `description`, `creator`, `create_time`, `updater`, `update_time`, `deleted`) VALUES
(1001, 'PROMOTION_RISK', 'ip_promo_cnt_10m', 'IP10分钟营销受理次数', 'STREAM', 'IP', 'PROMOTION_EVENT', 'LONG', 1, 1, '统计代理或正常 IP 在10分钟内的营销受理次数', 'admin', '2026-03-08 10:10:00', 'admin', '2026-03-08 10:10:00', b'0'),
(1002, 'PROMOTION_RISK', 'device_promo_user_cnt_1h', '设备1小时营销用户数', 'STREAM', 'DEVICE', 'PROMOTION_EVENT', 'LONG', 1, 1, '统计设备1小时内关联的营销受理用户数', 'admin', '2026-03-08 10:10:00', 'admin', '2026-03-08 10:10:00', b'0'),
(1003, 'PROMOTION_RISK', 'ip_in_proxy_watch_list', 'IP是否命中代理观察名单', 'LOOKUP', 'IP', 'PROMOTION_EVENT', 'BOOLEAN', 1, 1, '查询营销代理 IP 观察名单', 'admin', '2026-03-08 10:10:00', 'admin', '2026-03-08 10:10:00', b'0'),
(1004, 'PROMOTION_RISK', 'promo_proxy_burst_flag', '代理IP高频营销标记', 'DERIVED', 'IP', 'PROMOTION_EVENT', 'BOOLEAN', 1, 1, '基于代理 IP 与营销次数联合派生', 'admin', '2026-03-08 10:10:00', 'admin', '2026-03-08 10:10:00', b'0'),
(1005, 'PROMOTION_RISK', 'promo_device_cluster_flag', '设备营销聚集标记', 'DERIVED', 'DEVICE', 'PROMOTION_EVENT', 'BOOLEAN', 1, 1, '基于设备1小时营销用户数派生', 'admin', '2026-03-08 10:10:00', 'admin', '2026-03-08 10:10:00', b'0'),
(1101, 'WITHDRAW_RISK', 'user_withdraw_cnt_24h', '用户24小时提现次数', 'STREAM', 'USER', 'WITHDRAW_EVENT', 'LONG', 1, 1, '统计用户24小时内提交的提现次数', 'admin', '2026-03-08 11:10:00', 'admin', '2026-03-08 11:10:00', b'0'),
(1102, 'WITHDRAW_RISK', 'device_withdraw_user_cnt_24h', '设备24小时提现用户数', 'STREAM', 'DEVICE', 'WITHDRAW_EVENT', 'LONG', 1, 1, '统计设备24小时内关联的提现用户数', 'admin', '2026-03-08 11:10:00', 'admin', '2026-03-08 11:10:00', b'0'),
(1103, 'WITHDRAW_RISK', 'withdraw_high_amount_flag', '大额提现标记', 'DERIVED', 'USER', 'WITHDRAW_EVENT', 'BOOLEAN', 1, 1, '基于提现金额派生大额标记', 'admin', '2026-03-08 11:10:00', 'admin', '2026-03-08 11:10:00', b'0'),
(1104, 'WITHDRAW_RISK', 'withdraw_multi_actor_flag', '多主体提现标记', 'DERIVED', 'DEVICE', 'WITHDRAW_EVENT', 'BOOLEAN', 1, 2, '基于用户提现频次和设备关联用户数联合派生', 'admin', '2026-03-08 11:10:00', 'admin', '2026-03-10 09:00:00', b'0'),
(1201, 'ORDER_RISK', 'device_in_blacklist', '设备是否命中黑名单', 'LOOKUP', 'DEVICE', 'ORDER_EVENT', 'BOOLEAN', 1, 1, '查询订单设备黑名单', 'admin', '2026-03-08 12:10:00', 'admin', '2026-03-08 12:10:00', b'0'),
(1202, 'ORDER_RISK', 'user_in_white_list', '用户是否命中白名单', 'LOOKUP', 'USER', 'ORDER_EVENT', 'BOOLEAN', 1, 1, '查询订单用户白名单', 'admin', '2026-03-08 12:10:00', 'admin', '2026-03-08 12:10:00', b'0'),
(1203, 'ORDER_RISK', 'order_device_block_flag', '订单设备阻断标记', 'DERIVED', 'DEVICE', 'ORDER_EVENT', 'BOOLEAN', 1, 1, '基于设备黑名单和用户白名单联合派生', 'admin', '2026-03-08 12:10:00', 'admin', '2026-03-08 12:10:00', b'0');

INSERT INTO `feature_stream_conf` (`id`, `scene_code`, `feature_code`, `source_event_types`, `entity_key_expr`, `agg_type`, `value_expr`, `filter_expr`, `window_type`, `window_size`, `window_slide`, `include_current_event`, `ttl_seconds`, `state_hint_json`, `creator`, `create_time`, `updater`, `update_time`, `deleted`) VALUES
(2001, 'PROMOTION_RISK', 'ip_promo_cnt_10m', 'promotion_grant', 'ip', 'COUNT', '1', 'grantStatus == ''ACCEPTED''', 'SLIDING', '10m', '1m', 1, 1800, '{"keyBy":"ip"}', 'admin', '2026-03-08 10:12:00', 'admin', '2026-03-08 10:12:00', b'0'),
(2002, 'PROMOTION_RISK', 'device_promo_user_cnt_1h', 'promotion_grant', 'deviceId', 'DISTINCT_COUNT', 'userId', 'grantStatus == ''ACCEPTED'' && deviceId != null && userId != null', 'SLIDING', '1h', '5m', 1, 7200, '{"keyBy":"deviceId"}', 'admin', '2026-03-08 10:12:00', 'admin', '2026-03-08 10:12:00', b'0'),
(2101, 'WITHDRAW_RISK', 'user_withdraw_cnt_24h', 'withdraw_apply', 'userId', 'COUNT', '1', '(withdrawStatus == ''CREATED'' || withdrawStatus == ''PENDING_REVIEW'') && userId != null', 'SLIDING', '24h', '30m', 1, 172800, '{"keyBy":"userId"}', 'admin', '2026-03-08 11:12:00', 'admin', '2026-03-08 11:12:00', b'0'),
(2102, 'WITHDRAW_RISK', 'device_withdraw_user_cnt_24h', 'withdraw_apply', 'deviceId', 'DISTINCT_COUNT', 'userId', '(withdrawStatus == ''CREATED'' || withdrawStatus == ''PENDING_REVIEW'') && deviceId != null && userId != null', 'SLIDING', '24h', '30m', 1, 172800, '{"keyBy":"deviceId"}', 'admin', '2026-03-08 11:12:00', 'admin', '2026-03-08 11:12:00', b'0');

INSERT INTO `feature_lookup_conf` (`id`, `scene_code`, `feature_code`, `lookup_type`, `key_expr`, `source_ref`, `default_value`, `cache_ttl_seconds`, `timeout_ms`, `extra_json`, `creator`, `create_time`, `updater`, `update_time`, `deleted`) VALUES
(3001, 'PROMOTION_RISK', 'ip_in_proxy_watch_list', 'REDIS_SET', 'ip', 'PROMOTION_PROXY_IP_WATCH_LIST', 'false', 300, 20, '{"redisKey":"pulsix:list:watch:promotion:ip_proxy"}', 'admin', '2026-03-08 10:13:00', 'admin', '2026-03-08 10:13:00', b'0'),
(3002, 'ORDER_RISK', 'device_in_blacklist', 'REDIS_SET', 'deviceId', 'ORDER_DEVICE_BLACKLIST', 'false', 300, 20, '{"redisKey":"pulsix:list:black:order:device"}', 'admin', '2026-03-08 12:13:00', 'admin', '2026-03-08 12:13:00', b'0'),
(3003, 'ORDER_RISK', 'user_in_white_list', 'REDIS_SET', 'userId', 'ORDER_USER_WHITE_LIST', 'false', 300, 20, '{"redisKey":"pulsix:list:white:order:user"}', 'admin', '2026-03-08 12:13:00', 'admin', '2026-03-08 12:13:00', b'0');

INSERT INTO `feature_derived_conf` (`id`, `scene_code`, `feature_code`, `engine_type`, `expr_content`, `depends_on_json`, `value_type`, `extra_json`, `creator`, `create_time`, `updater`, `update_time`, `deleted`) VALUES
(4001, 'PROMOTION_RISK', 'promo_proxy_burst_flag', 'DSL', 'ip_in_proxy_watch_list == true && ip_promo_cnt_10m >= 8', '["ip_in_proxy_watch_list","ip_promo_cnt_10m"]', 'BOOLEAN', '{"orderNo":10}', 'admin', '2026-03-08 10:14:00', 'admin', '2026-03-08 10:14:00', b'0'),
(4002, 'PROMOTION_RISK', 'promo_device_cluster_flag', 'DSL', 'device_promo_user_cnt_1h >= 5', '["device_promo_user_cnt_1h"]', 'BOOLEAN', '{"orderNo":20}', 'admin', '2026-03-08 10:14:00', 'admin', '2026-03-08 10:14:00', b'0'),
(4101, 'WITHDRAW_RISK', 'withdraw_high_amount_flag', 'DSL', 'withdrawAmount >= 3000', '["withdrawAmount"]', 'BOOLEAN', '{"orderNo":10}', 'admin', '2026-03-08 11:14:00', 'admin', '2026-03-08 11:14:00', b'0'),
(4102, 'WITHDRAW_RISK', 'withdraw_multi_actor_flag', 'DSL', 'user_withdraw_cnt_24h >= 3 || device_withdraw_user_cnt_24h >= 2', '["user_withdraw_cnt_24h","device_withdraw_user_cnt_24h"]', 'BOOLEAN', '{"orderNo":20,"versionHint":2}', 'admin', '2026-03-08 11:14:00', 'admin', '2026-03-10 09:00:00', b'0'),
(4201, 'ORDER_RISK', 'order_device_block_flag', 'DSL', 'device_in_blacklist == true && user_in_white_list != true', '["device_in_blacklist","user_in_white_list"]', 'BOOLEAN', '{"orderNo":10}', 'admin', '2026-03-08 12:14:00', 'admin', '2026-03-08 12:14:00', b'0');

INSERT INTO `rule_def` (`id`, `scene_code`, `rule_code`, `rule_name`, `rule_type`, `engine_type`, `expr_content`, `depends_on_json`, `priority`, `hit_action`, `risk_score`, `alert_enabled_flag`, `status`, `hit_reason_template`, `version`, `description`, `creator`, `create_time`, `updater`, `update_time`, `deleted`) VALUES
(5001, 'PROMOTION_RISK', 'PROMOTION_R001', '代理IP或设备聚集营销受理', 'NORMAL', 'DSL', '(ip_in_proxy_watch_list == true && ip_promo_cnt_10m >= 8) || device_promo_user_cnt_1h >= 5', '["ip_in_proxy_watch_list","ip_promo_cnt_10m","device_promo_user_cnt_1h"]', 100, 'TAG_ONLY', 45, b'1', 1, '营销风控命中：代理IP={ip_in_proxy_watch_list}, IP10分钟营销次数={ip_promo_cnt_10m}, 设备1小时营销用户数={device_promo_user_cnt_1h}', 1, '命中后记录风险事件并发送预警，不阻断营销受理', 'admin', '2026-03-08 10:15:00', 'admin', '2026-03-08 10:15:00', b'0'),
(5002, 'WITHDRAW_RISK', 'WITHDRAW_R001', '大额多次提现待审核', 'NORMAL', 'DSL', 'withdrawAmount >= 3000 && (user_withdraw_cnt_24h >= 3 || device_withdraw_user_cnt_24h >= 2)', '["withdrawAmount","user_withdraw_cnt_24h","device_withdraw_user_cnt_24h"]', 100, 'REVIEW', 80, b'1', 1, '提现金额={withdrawAmount}, 用户24小时提现次数={user_withdraw_cnt_24h}, 设备24小时提现用户数={device_withdraw_user_cnt_24h}', 2, '命中后暂停打款，转人工审核', 'admin', '2026-03-08 11:15:00', 'admin', '2026-03-10 09:00:00', b'0'),
(5003, 'ORDER_RISK', 'ORDER_R001', '黑名单设备订单拒绝', 'NORMAL', 'DSL', 'device_in_blacklist == true && user_in_white_list != true', '["device_in_blacklist","user_in_white_list"]', 100, 'REJECT', 100, b'1', 1, '设备命中黑名单={device_in_blacklist}, 用户白名单={user_in_white_list}', 1, '命中后取消订单、释放库存并触发退款或关闭履约单', 'admin', '2026-03-08 12:15:00', 'admin', '2026-03-08 12:15:00', b'0');

INSERT INTO `policy_def` (`id`, `scene_code`, `policy_code`, `policy_name`, `decision_mode`, `default_action`, `status`, `version`, `description`, `creator`, `create_time`, `updater`, `update_time`, `deleted`) VALUES
(6001, 'PROMOTION_RISK', 'PROMOTION_RISK_POLICY_FIRST_HIT', '营销风控 FIRST_HIT 策略', 'FIRST_HIT', 'PASS', 1, 1, '营销反作弊当前启用的 FIRST_HIT 策略', 'admin', '2026-03-08 10:16:00', 'admin', '2026-03-08 10:16:00', b'0'),
(6002, 'WITHDRAW_RISK', 'WITHDRAW_RISK_POLICY_FIRST_HIT', '提现风控 FIRST_HIT 策略', 'FIRST_HIT', 'PASS', 1, 2, '提现风控当前启用的 FIRST_HIT 策略', 'admin', '2026-03-08 11:16:00', 'admin', '2026-03-10 09:00:00', b'0'),
(6003, 'ORDER_RISK', 'ORDER_RISK_POLICY_FIRST_HIT', '订单风控 FIRST_HIT 策略', 'FIRST_HIT', 'PASS', 1, 1, '订单后置风控当前启用的 FIRST_HIT 策略', 'admin', '2026-03-08 12:16:00', 'admin', '2026-03-08 12:16:00', b'0');

INSERT INTO `policy_rule_ref` (`id`, `scene_code`, `policy_code`, `rule_code`, `order_no`, `enabled_flag`, `branch_expr`, `score_weight`, `creator`, `create_time`, `updater`, `update_time`, `deleted`) VALUES
(7001, 'PROMOTION_RISK', 'PROMOTION_RISK_POLICY_FIRST_HIT', 'PROMOTION_R001', 1, 1, NULL, NULL, 'admin', '2026-03-08 10:17:00', 'admin', '2026-03-08 10:17:00', b'0'),
(7002, 'WITHDRAW_RISK', 'WITHDRAW_RISK_POLICY_FIRST_HIT', 'WITHDRAW_R001', 1, 1, NULL, NULL, 'admin', '2026-03-08 11:17:00', 'admin', '2026-03-10 09:00:00', b'0'),
(7003, 'ORDER_RISK', 'ORDER_RISK_POLICY_FIRST_HIT', 'ORDER_R001', 1, 1, NULL, NULL, 'admin', '2026-03-08 12:17:00', 'admin', '2026-03-08 12:17:00', b'0');

INSERT INTO `scene_release` (`id`, `scene_code`, `version_no`, `checksum`, `publish_status`, `snapshot_json`, `compile_report_json`, `effective_from`, `published_by`, `published_at`, `rollback_from_version`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`) VALUES
(8001, 'PROMOTION_RISK', 1, 'sha256:promo-risk-v1', 'ACTIVE', '{"sceneCode":"PROMOTION_RISK","version":1,"status":"ACTIVE","eventCode":"PROMOTION_EVENT","policy":{"code":"PROMOTION_RISK_POLICY_FIRST_HIT","defaultAction":"PASS","rules":[{"code":"PROMOTION_R001","action":"TAG_ONLY","expr":"(ip_in_proxy_watch_list == true && ip_promo_cnt_10m >= 8) || device_promo_user_cnt_1h >= 5"}]},"featureCodes":["ip_promo_cnt_10m","device_promo_user_cnt_1h","ip_in_proxy_watch_list","promo_proxy_burst_flag","promo_device_cluster_flag"]}', '{"validation":"PASSED","featureCount":5,"ruleCount":1,"policyCode":"PROMOTION_RISK_POLICY_FIRST_HIT"}', '2026-03-08 10:30:00', 'admin', '2026-03-08 10:30:00', NULL, '营销风控初始化版本', 'admin', '2026-03-08 10:29:50', 'admin', '2026-03-08 10:29:50', b'0'),
(8002, 'WITHDRAW_RISK', 1, 'sha256:withdraw-risk-v1', 'PUBLISHED', '{"sceneCode":"WITHDRAW_RISK","version":1,"status":"PUBLISHED","eventCode":"WITHDRAW_EVENT","policy":{"code":"WITHDRAW_RISK_POLICY_FIRST_HIT","defaultAction":"PASS","rules":[{"code":"WITHDRAW_R001","action":"REVIEW","expr":"withdrawAmount >= 3000 && (user_withdraw_cnt_24h >= 3 || device_withdraw_user_cnt_24h >= 3)"}]},"featureCodes":["user_withdraw_cnt_24h","device_withdraw_user_cnt_24h","withdraw_high_amount_flag","withdraw_multi_actor_flag"]}', '{"validation":"PASSED","featureCount":4,"ruleCount":1,"policyCode":"WITHDRAW_RISK_POLICY_FIRST_HIT"}', '2026-03-08 11:20:00', 'admin', '2026-03-08 11:20:00', NULL, '提现风控第一版，设备共用阈值为3', 'admin', '2026-03-08 11:19:50', 'admin', '2026-03-08 11:19:50', b'0'),
(8003, 'WITHDRAW_RISK', 2, 'sha256:withdraw-risk-v2', 'ACTIVE', '{"sceneCode":"WITHDRAW_RISK","version":2,"status":"ACTIVE","eventCode":"WITHDRAW_EVENT","policy":{"code":"WITHDRAW_RISK_POLICY_FIRST_HIT","defaultAction":"PASS","rules":[{"code":"WITHDRAW_R001","action":"REVIEW","expr":"withdrawAmount >= 3000 && (user_withdraw_cnt_24h >= 3 || device_withdraw_user_cnt_24h >= 2)"}]},"featureCodes":["user_withdraw_cnt_24h","device_withdraw_user_cnt_24h","withdraw_high_amount_flag","withdraw_multi_actor_flag"]}', '{"validation":"PASSED","featureCount":4,"ruleCount":1,"policyCode":"WITHDRAW_RISK_POLICY_FIRST_HIT","changes":["WITHDRAW_R001 device threshold 3 -> 2","withdraw_multi_actor_flag expr updated"]}', '2026-03-10 09:30:00', 'admin', '2026-03-10 09:30:00', NULL, '根据一线审核命中复盘放宽设备共用阈值', 'admin', '2026-03-10 09:29:50', 'admin', '2026-03-10 09:29:50', b'0'),
(8004, 'ORDER_RISK', 1, 'sha256:order-risk-v1', 'ACTIVE', '{"sceneCode":"ORDER_RISK","version":1,"status":"ACTIVE","eventCode":"ORDER_EVENT","policy":{"code":"ORDER_RISK_POLICY_FIRST_HIT","defaultAction":"PASS","rules":[{"code":"ORDER_R001","action":"REJECT","expr":"device_in_blacklist == true && user_in_white_list != true"}]},"featureCodes":["device_in_blacklist","user_in_white_list","order_device_block_flag"]}', '{"validation":"PASSED","featureCount":3,"ruleCount":1,"policyCode":"ORDER_RISK_POLICY_FIRST_HIT"}', '2026-03-08 12:30:00', 'admin', '2026-03-08 12:30:00', NULL, '订单后置风控初始化版本', 'admin', '2026-03-08 12:29:50', 'admin', '2026-03-08 12:29:50', b'0');

INSERT INTO `scene_release_change` (`id`, `release_id`, `scene_code`, `change_type`, `target_type`, `target_code`, `before_json`, `after_json`, `description`, `creator`, `create_time`, `updater`, `update_time`, `deleted`) VALUES
(9001, 8003, 'WITHDRAW_RISK', 'MODIFY', 'RULE', 'WITHDRAW_R001', '{"expr":"withdrawAmount >= 3000 && (user_withdraw_cnt_24h >= 3 || device_withdraw_user_cnt_24h >= 3)","version":1}', '{"expr":"withdrawAmount >= 3000 && (user_withdraw_cnt_24h >= 3 || device_withdraw_user_cnt_24h >= 2)","version":2}', '根据命中复盘放宽设备共用阈值', 'admin', '2026-03-10 09:25:00', 'admin', '2026-03-10 09:25:00', b'0'),
(9002, 8003, 'WITHDRAW_RISK', 'MODIFY', 'FEATURE', 'withdraw_multi_actor_flag', '{"expr":"user_withdraw_cnt_24h >= 3 || device_withdraw_user_cnt_24h >= 3","orderNo":20}', '{"expr":"user_withdraw_cnt_24h >= 3 || device_withdraw_user_cnt_24h >= 2","orderNo":20}', '同步更新多主体提现派生特征', 'admin', '2026-03-10 09:25:10', 'admin', '2026-03-10 09:25:10', b'0');

INSERT INTO `simulation_case` (`id`, `scene_code`, `case_code`, `case_name`, `input_event_json`, `context_json`, `expected_action`, `expected_hit_rules`, `status`, `description`, `creator`, `create_time`, `updater`, `update_time`, `deleted`) VALUES
(10001, 'PROMOTION_RISK', 'PROMO_CASE_001', '正常营销受理', '{"eventId":"E_PROMO_1001","traceId":"T_PROMO_1001","sceneCode":"PROMOTION_RISK","eventType":"promotion_grant","eventTime":"2026-03-08T10:08:00","userId":"U20002","deviceId":"DVC_PROMO_002","ip":"203.0.113.88","channel":"APP","bizNo":"PROMO_REQ_202603080101","activityId":"ACT_INVITE_202603","promotionType":"COUPON_CLAIM","rewardType":"COUPON","rewardValue":20,"grantStatus":"ACCEPTED"}', '{"ip_promo_cnt_10m":2,"device_promo_user_cnt_1h":1,"ip_in_proxy_watch_list":false}', 'PASS', '[]', 1, '营销场景正常样例', 'admin', '2026-03-08 10:40:00', 'admin', '2026-03-08 10:40:00', b'0'),
(10002, 'PROMOTION_RISK', 'PROMO_CASE_002', '代理IP高频领券', '{"eventId":"E_PROMO_1002","traceId":"T_PROMO_1002","sceneCode":"PROMOTION_RISK","eventType":"promotion_grant","eventTime":"2026-03-08T10:12:00","userId":"U20077","deviceId":"DVC_PROMO_CLUSTER_01","ip":"203.0.113.10","channel":"APP","bizNo":"PROMO_REQ_202603080102","activityId":"ACT_INVITE_202603","promotionType":"INVITE_REWARD","rewardType":"COUPON","rewardValue":50,"grantStatus":"ACCEPTED","ext":{"inviteUserId":"U16666","campaignCode":"SPRING_GROWTH"}}', '{"ip_promo_cnt_10m":9,"device_promo_user_cnt_1h":4,"ip_in_proxy_watch_list":true}', 'TAG_ONLY', '["PROMOTION_R001"]', 1, '营销场景代理IP高频样例', 'admin', '2026-03-08 10:40:00', 'admin', '2026-03-08 10:40:00', b'0'),
(10003, 'WITHDRAW_RISK', 'WITHDRAW_CASE_001', '低频提现放行', '{"eventId":"E_WD_1001","traceId":"T_WD_1001","sceneCode":"WITHDRAW_RISK","eventType":"withdraw_apply","eventTime":"2026-03-08T11:06:00","userId":"U30002","deviceId":"DVC_WD_002","ip":"198.51.100.22","channel":"APP","bizNo":"WD_REQ_202603080101","withdrawNo":"WD202603080101","withdrawAmount":500,"currency":"CNY","withdrawAccountId":"WACC_88002","accountType":"BANK_CARD","bankCode":"CCB","withdrawStatus":"CREATED"}', '{"user_withdraw_cnt_24h":1,"device_withdraw_user_cnt_24h":1}', 'PASS', '[]', 1, '提现场景正常样例', 'admin', '2026-03-08 11:40:00', 'admin', '2026-03-08 11:40:00', b'0'),
(10004, 'WITHDRAW_RISK', 'WITHDRAW_CASE_002', '大额重复提现', '{"eventId":"E_WD_1002","traceId":"T_WD_1002","sceneCode":"WITHDRAW_RISK","eventType":"withdraw_apply","eventTime":"2026-03-08T11:18:00","userId":"U30088","deviceId":"DVC_WD_SHARED_01","ip":"198.51.100.23","channel":"APP","bizNo":"WD_REQ_202603080102","withdrawNo":"WD202603080102","withdrawAmount":3800,"currency":"CNY","withdrawAccountId":"WACC_99002","accountType":"BANK_CARD","bankCode":"CCB","withdrawStatus":"CREATED","ext":{"city":"Hangzhou","bankCardTail":"5521"}}', '{"user_withdraw_cnt_24h":3,"device_withdraw_user_cnt_24h":2}', 'REVIEW', '["WITHDRAW_R001"]', 1, '提现场景大额重复样例', 'admin', '2026-03-08 11:40:00', 'admin', '2026-03-08 11:40:00', b'0'),
(10005, 'ORDER_RISK', 'ORDER_CASE_001', '白名单用户订单放行', '{"eventId":"E_ORDER_1001","traceId":"T_ORDER_1001","sceneCode":"ORDER_RISK","eventType":"order_paid","eventTime":"2026-03-08T12:06:00","userId":"U40088","deviceId":"DVC_ORDER_BLACK_01","ip":"192.0.2.28","channel":"APP","bizNo":"ORD202603080101","orderNo":"ORD202603080101","orderAmount":699,"payAmount":699,"currency":"CNY","merchantId":"MCH_3001","fulfillmentType":"EXPRESS","orderStatus":"PAID"}', '{"device_in_blacklist":true,"user_in_white_list":true}', 'PASS', '[]', 1, '订单场景白名单保护样例', 'admin', '2026-03-08 12:40:00', 'admin', '2026-03-08 12:40:00', b'0'),
(10006, 'ORDER_RISK', 'ORDER_CASE_002', '黑名单设备订单拒绝', '{"eventId":"E_ORDER_1002","traceId":"T_ORDER_1002","sceneCode":"ORDER_RISK","eventType":"order_paid","eventTime":"2026-03-08T12:12:00","userId":"U40099","deviceId":"DVC_ORDER_BLACK_01","ip":"192.0.2.38","channel":"APP","bizNo":"ORD202603080102","orderNo":"ORD202603080102","orderAmount":899,"payAmount":899,"currency":"CNY","merchantId":"MCH_3001","fulfillmentType":"EXPRESS","orderStatus":"PAID","ext":{"skuCount":1,"receiverCity":"Nanjing"}}', '{"device_in_blacklist":true,"user_in_white_list":false}', 'REJECT', '["ORDER_R001"]', 1, '订单场景黑名单设备样例', 'admin', '2026-03-08 12:40:00', 'admin', '2026-03-08 12:40:00', b'0');

INSERT INTO `simulation_report` (`id`, `case_id`, `scene_code`, `case_code`, `release_id`, `version_no`, `actual_action`, `actual_hit_rules`, `feature_snapshot_json`, `result_json`, `pass_flag`, `duration_ms`, `run_by`, `creator`, `create_time`, `updater`, `update_time`, `deleted`) VALUES
(10101, 10001, 'PROMOTION_RISK', 'PROMO_CASE_001', 8001, 1, 'PASS', '[]', '{"ip_promo_cnt_10m":2,"device_promo_user_cnt_1h":1,"ip_in_proxy_watch_list":false}', '{"finalAction":"PASS","hitRules":[],"latencyMs":9}', 1, 9, 'admin', 'admin', '2026-03-08 10:41:00', 'admin', '2026-03-08 10:41:00', b'0'),
(10102, 10002, 'PROMOTION_RISK', 'PROMO_CASE_002', 8001, 1, 'TAG_ONLY', '["PROMOTION_R001"]', '{"ip_promo_cnt_10m":9,"device_promo_user_cnt_1h":4,"ip_in_proxy_watch_list":true,"promo_proxy_burst_flag":true}', '{"finalAction":"TAG_ONLY","hitRules":["PROMOTION_R001"],"latencyMs":13}', 1, 13, 'admin', 'admin', '2026-03-08 10:41:10', 'admin', '2026-03-08 10:41:10', b'0'),
(10103, 10003, 'WITHDRAW_RISK', 'WITHDRAW_CASE_001', 8003, 2, 'PASS', '[]', '{"user_withdraw_cnt_24h":1,"device_withdraw_user_cnt_24h":1,"withdraw_high_amount_flag":false,"withdraw_multi_actor_flag":false}', '{"finalAction":"PASS","hitRules":[],"latencyMs":11}', 1, 11, 'admin', 'admin', '2026-03-08 11:41:00', 'admin', '2026-03-08 11:41:00', b'0'),
(10104, 10004, 'WITHDRAW_RISK', 'WITHDRAW_CASE_002', 8003, 2, 'REVIEW', '["WITHDRAW_R001"]', '{"user_withdraw_cnt_24h":3,"device_withdraw_user_cnt_24h":2,"withdraw_high_amount_flag":true,"withdraw_multi_actor_flag":true}', '{"finalAction":"REVIEW","hitRules":["WITHDRAW_R001"],"latencyMs":16}', 1, 16, 'admin', 'admin', '2026-03-08 11:41:10', 'admin', '2026-03-08 11:41:10', b'0'),
(10105, 10005, 'ORDER_RISK', 'ORDER_CASE_001', 8004, 1, 'PASS', '[]', '{"device_in_blacklist":true,"user_in_white_list":true,"order_device_block_flag":false}', '{"finalAction":"PASS","hitRules":[],"latencyMs":10}', 1, 10, 'admin', 'admin', '2026-03-08 12:41:00', 'admin', '2026-03-08 12:41:00', b'0'),
(10106, 10006, 'ORDER_RISK', 'ORDER_CASE_002', 8004, 1, 'REJECT', '["ORDER_R001"]', '{"device_in_blacklist":true,"user_in_white_list":false,"order_device_block_flag":true}', '{"finalAction":"REJECT","hitRules":["ORDER_R001"],"latencyMs":12}', 1, 12, 'admin', 'admin', '2026-03-08 12:41:10', 'admin', '2026-03-08 12:41:10', b'0');

INSERT INTO `decision_log` (`id`, `trace_id`, `event_id`, `scene_code`, `source_code`, `entity_id`, `final_action`, `final_score`, `version_no`, `latency_ms`, `risk_level`, `event_time`, `input_json`, `feature_snapshot_json`, `hit_rules_json`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`) VALUES
(11001, 'T_PROMO_0001', 'E_PROMO_0001', 'PROMOTION_RISK', 'PROMOTION_CENTER_HTTP', 'U20001', 'PASS', 0, 1, 8, 'LOW', '2026-03-08 10:00:00', '{"eventId":"E_PROMO_0001","sceneCode":"PROMOTION_RISK","eventType":"promotion_grant","userId":"U20001","deviceId":"DVC_PROMO_001","ip":"203.0.113.10","bizNo":"PROMO_REQ_202603080001","activityId":"ACT_INVITE_202603","rewardValue":30,"grantStatus":"ACCEPTED"}', '{"ip_promo_cnt_10m":1,"device_promo_user_cnt_1h":1,"ip_in_proxy_watch_list":false}', '[]', '正常营销受理样例', 'system', '2026-03-08 10:00:01', 'system', '2026-03-08 10:00:01', b'0'),
(11002, 'T_PROMO_0002', 'E_PROMO_0002', 'PROMOTION_RISK', 'PROMOTION_CENTER_HTTP', 'U20077', 'TAG_ONLY', 45, 1, 13, 'MEDIUM', '2026-03-08 10:12:00', '{"eventId":"E_PROMO_0002","sceneCode":"PROMOTION_RISK","eventType":"promotion_grant","userId":"U20077","deviceId":"DVC_PROMO_CLUSTER_01","ip":"203.0.113.10","bizNo":"PROMO_REQ_202603080002","activityId":"ACT_INVITE_202603","rewardValue":50,"grantStatus":"ACCEPTED"}', '{"ip_promo_cnt_10m":9,"device_promo_user_cnt_1h":4,"ip_in_proxy_watch_list":true,"promo_proxy_burst_flag":true}', '["PROMOTION_R001"]', '代理IP高频营销受理样例', 'system', '2026-03-08 10:12:01', 'system', '2026-03-08 10:12:01', b'0'),
(11003, 'T_WD_0001', 'E_WD_0001', 'WITHDRAW_RISK', 'WITHDRAW_CENTER_HTTP', 'U30001', 'PASS', 0, 2, 10, 'LOW', '2026-03-08 11:00:00', '{"eventId":"E_WD_0001","sceneCode":"WITHDRAW_RISK","eventType":"withdraw_apply","userId":"U30001","deviceId":"DVC_WD_001","ip":"198.51.100.21","bizNo":"WD_REQ_202603080001","withdrawNo":"WD202603080001","withdrawAmount":3500,"withdrawAccountId":"WACC_88001","withdrawStatus":"CREATED"}', '{"user_withdraw_cnt_24h":1,"device_withdraw_user_cnt_24h":1,"withdraw_high_amount_flag":true,"withdraw_multi_actor_flag":false}', '[]', '大额但低频提现放行样例', 'system', '2026-03-08 11:00:01', 'system', '2026-03-08 11:00:01', b'0'),
(11004, 'T_WD_0002', 'E_WD_0002', 'WITHDRAW_RISK', 'WITHDRAW_CENTER_HTTP', 'U30088', 'REVIEW', 80, 2, 16, 'HIGH', '2026-03-08 11:18:00', '{"eventId":"E_WD_0002","sceneCode":"WITHDRAW_RISK","eventType":"withdraw_apply","userId":"U30088","deviceId":"DVC_WD_SHARED_01","ip":"198.51.100.23","bizNo":"WD_REQ_202603080002","withdrawNo":"WD202603080002","withdrawAmount":3800,"withdrawAccountId":"WACC_99002","withdrawStatus":"CREATED"}', '{"user_withdraw_cnt_24h":3,"device_withdraw_user_cnt_24h":2,"withdraw_high_amount_flag":true,"withdraw_multi_actor_flag":true}', '["WITHDRAW_R001"]', '大额重复提现转人工审核样例', 'system', '2026-03-08 11:18:01', 'system', '2026-03-08 11:18:01', b'0'),
(11005, 'T_ORDER_0001', 'E_ORDER_0001', 'ORDER_RISK', 'ORDER_CENTER_SDK', 'U40001', 'PASS', 0, 1, 10, 'LOW', '2026-03-08 12:00:00', '{"eventId":"E_ORDER_0001","sceneCode":"ORDER_RISK","eventType":"order_paid","userId":"U40001","deviceId":"DVC_ORDER_001","ip":"192.0.2.18","bizNo":"ORD202603080001","orderNo":"ORD202603080001","orderAmount":1299,"merchantId":"MCH_3001","orderStatus":"PAID"}', '{"device_in_blacklist":false,"user_in_white_list":false,"order_device_block_flag":false}', '[]', '正常订单样例', 'system', '2026-03-08 12:00:01', 'system', '2026-03-08 12:00:01', b'0'),
(11006, 'T_ORDER_0002', 'E_ORDER_0002', 'ORDER_RISK', 'ORDER_CENTER_SDK', 'U40099', 'REJECT', 100, 1, 12, 'HIGH', '2026-03-08 12:12:00', '{"eventId":"E_ORDER_0002","sceneCode":"ORDER_RISK","eventType":"order_paid","userId":"U40099","deviceId":"DVC_ORDER_BLACK_01","ip":"192.0.2.38","bizNo":"ORD202603080002","orderNo":"ORD202603080002","orderAmount":899,"merchantId":"MCH_3001","orderStatus":"PAID"}', '{"device_in_blacklist":true,"user_in_white_list":false,"order_device_block_flag":true}', '["ORDER_R001"]', '黑名单设备支付后订单拒绝样例', 'system', '2026-03-08 12:12:01', 'system', '2026-03-08 12:12:01', b'0');

INSERT INTO `rule_hit_log` (`id`, `decision_id`, `rule_code`, `hit_flag`, `hit_action`, `score`, `hit_reason`, `hit_value_json`, `creator`, `create_time`, `updater`, `update_time`, `deleted`) VALUES
(12001, 11002, 'PROMOTION_R001', 1, 'TAG_ONLY', 45, '代理IP=true, IP10分钟营销次数=9, 设备1小时营销用户数=4', '{"ip_in_proxy_watch_list":true,"ip_promo_cnt_10m":9,"device_promo_user_cnt_1h":4}', 'system', '2026-03-08 10:12:01', 'system', '2026-03-08 10:12:01', b'0'),
(12002, 11004, 'WITHDRAW_R001', 1, 'REVIEW', 80, '提现金额=3800, 用户24小时提现次数=3, 设备24小时提现用户数=2', '{"withdrawAmount":3800,"user_withdraw_cnt_24h":3,"device_withdraw_user_cnt_24h":2}', 'system', '2026-03-08 11:18:01', 'system', '2026-03-08 11:18:01', b'0'),
(12003, 11006, 'ORDER_R001', 1, 'REJECT', 100, '设备命中黑名单且用户未命中白名单', '{"device_in_blacklist":true,"user_in_white_list":false}', 'system', '2026-03-08 12:12:01', 'system', '2026-03-08 12:12:01', b'0');

INSERT INTO `risk_event` (`id`, `risk_event_no`, `scene_code`, `trace_id`, `event_id`, `decision_id`, `source_type`, `final_action`, `risk_level`, `version_no`, `hit_rules_json`, `hit_summary`, `status`, `assigned_to`, `first_seen_time`, `last_seen_time`, `ext_json`, `creator`, `create_time`, `updater`, `update_time`, `deleted`) VALUES
(13001, 'RISK_EVT_20260308_001', 'PROMOTION_RISK', 'T_PROMO_0002', 'E_PROMO_0002', 11002, 'DECISION_RESULT', 'TAG_ONLY', 'MEDIUM', 1, '["PROMOTION_R001"]', '代理IP高频营销受理，已生成观察事件并发送预警', 'OPEN', 'marketing.ops', '2026-03-08 10:12:01', '2026-03-08 10:12:01', '{"category":"PROMOTION","nextStep":"tag_user_and_observe"}', 'system', '2026-03-08 10:12:01', 'system', '2026-03-08 10:12:01', b'0'),
(13002, 'RISK_EVT_20260308_002', 'WITHDRAW_RISK', 'T_WD_0002', 'E_WD_0002', 11004, 'DECISION_RESULT', 'REVIEW', 'HIGH', 2, '["WITHDRAW_R001"]', '大额重复提现命中审核规则，已暂停打款等待运营处理', 'REVIEWING', 'finance.review', '2026-03-08 11:18:01', '2026-03-08 11:18:01', '{"category":"WITHDRAW","nextStep":"hold_payment"}', 'system', '2026-03-08 11:18:01', 'finance.review', '2026-03-08 11:20:00', b'0'),
(13003, 'RISK_EVT_20260308_003', 'ORDER_RISK', 'T_ORDER_0002', 'E_ORDER_0002', 11006, 'DECISION_RESULT', 'REJECT', 'HIGH', 1, '["ORDER_R001"]', '黑名单设备下单，订单系统需取消订单并释放库存', 'OPEN', 'order.ops', '2026-03-08 12:12:01', '2026-03-08 12:12:01', '{"category":"ORDER","nextStep":"cancel_order"}', 'system', '2026-03-08 12:12:01', 'system', '2026-03-08 12:12:01', b'0');

INSERT INTO `access_source_def` (`id`, `source_code`, `source_name`, `source_type`, `topic_name`, `rate_limit_qps`, `allowed_scene_codes_json`, `ip_whitelist_json`, `status`, `description`, `creator`, `create_time`, `updater`, `update_time`, `deleted`) VALUES
(14001, 'PROMOTION_CENTER_HTTP', '营销中心 HTTP 接入', 'HTTP', 'pulsix.event.standard', 300, '["PROMOTION_RISK"]', '["10.30.0.0/16","172.16.10.0/24"]', 1, '服务营销受理事件的 HTTP 接入源', 'admin', '2026-03-08 09:58:00', 'admin', '2026-03-08 09:58:00', b'0'),
(14002, 'WITHDRAW_CENTER_HTTP', '资金中心 HTTP 接入', 'HTTP', 'pulsix.event.standard', 200, '["WITHDRAW_RISK"]', '["10.40.0.0/16","192.168.20.0/24"]', 1, '服务提现申请事件的 HTTP 接入源', 'admin', '2026-03-08 10:58:00', 'admin', '2026-03-08 10:58:00', b'0'),
(14003, 'ORDER_CENTER_SDK', '订单中心 SDK 接入', 'SDK', 'pulsix.event.standard', 500, '["ORDER_RISK"]', '["172.20.8.0/24"]', 1, '服务订单支付事件的后端 SDK 接入源', 'admin', '2026-03-08 11:58:00', 'admin', '2026-03-08 11:58:00', b'0');

INSERT INTO `event_access_binding` (`id`, `event_code`, `source_code`, `creator`, `create_time`, `updater`, `update_time`, `deleted`) VALUES
(14101, 'PROMOTION_EVENT', 'PROMOTION_CENTER_HTTP', 'admin', '2026-03-08 10:00:00', 'admin', '2026-03-08 10:00:00', b'0'),
(14102, 'WITHDRAW_EVENT', 'WITHDRAW_CENTER_HTTP', 'admin', '2026-03-08 11:00:00', 'admin', '2026-03-08 11:00:00', b'0'),
(14103, 'ORDER_EVENT', 'ORDER_CENTER_SDK', 'admin', '2026-03-08 12:00:00', 'admin', '2026-03-08 12:00:00', b'0');

INSERT INTO `access_auth_conf` (`id`, `source_code`, `auth_type`, `app_key`, `app_secret`, `sign_algo`, `signature_header`, `nonce_ttl_seconds`, `effective_from`, `expire_at`, `status`, `ext_json`, `creator`, `create_time`, `updater`, `update_time`, `deleted`) VALUES
(15001, 'PROMOTION_CENTER_HTTP', 'API_KEY', 'promo_http_key', 'promo_http_secret_demo', 'NONE', 'X-API-Key', 0, '2026-03-08 10:00:00', NULL, 1, '{"headers":["X-API-Key"]}', 'admin', '2026-03-08 10:00:00', 'admin', '2026-03-08 10:00:00', b'0'),
(15002, 'WITHDRAW_CENTER_HTTP', 'HMAC', 'withdraw_http_key', 'withdraw_http_secret_demo', 'HMAC_SHA256', 'X-Pulsix-Signature', 300, '2026-03-08 11:00:00', NULL, 1, '{"headers":["X-Pulsix-App-Key","X-Pulsix-Timestamp","X-Pulsix-Nonce","X-Pulsix-Signature"]}', 'admin', '2026-03-08 11:00:00', 'admin', '2026-03-08 11:00:00', b'0'),
(15003, 'ORDER_CENTER_SDK', 'HMAC', 'order_sdk_key', 'order_sdk_secret_demo', 'HMAC_SHA256', 'x-signature', 300, '2026-03-08 12:00:00', NULL, 1, '{"authFrameVersion":"v1"}', 'admin', '2026-03-08 12:00:00', 'admin', '2026-03-08 12:00:00', b'0');

INSERT INTO `risk_error_log` (`id`, `trace_id`, `event_id`, `source_code`, `scene_code`, `source_topic`, `error_stage`, `error_type`, `error_code`, `error_level`, `error_message`, `raw_payload`, `standard_payload_json`, `tag_json`, `process_status`, `process_remark`, `event_time`, `resolved_by`, `resolved_time`, `creator`, `create_time`, `updater`, `update_time`, `deleted`) VALUES
(16001, 'T_ERR_PROMO_0001', NULL, 'PROMOTION_CENTER_HTTP', 'PROMOTION_RISK', 'pulsix.ingest.error', 'AUTH', 'INVALID_SIGNATURE', 'INGEST_401', 'HIGH', '营销中心签名校验失败，拒绝写入标准事件 Topic', '{"headers":{"X-API-Key":"promo_http_key","X-Pulsix-Signature":"invalid"},"body":{"eventId":"E_PROMO_BAD_0001"}}', NULL, '["INGEST","AUTH"]', 'OPEN', '待营销中心修复签名逻辑', '2026-03-08 10:05:00', NULL, NULL, 'system', '2026-03-08 10:05:01', 'system', '2026-03-08 10:05:01', b'0'),
(16002, 'T_ERR_WD_0002', 'E_WD_BAD_0001', 'WITHDRAW_CENTER_HTTP', 'WITHDRAW_RISK', 'pulsix.ingest.error', 'STANDARDIZE', 'MISSING_REQUIRED_FIELD', 'STD_422', 'MEDIUM', '标准化失败，缺少必填字段 withdrawNo', '{"eventId":"E_WD_BAD_0001","sceneCode":"WITHDRAW_RISK","eventType":"withdraw_apply","userId":"U30123","withdrawAmount":5200}', '{"eventId":"E_WD_BAD_0001","sceneCode":"WITHDRAW_RISK","eventType":"withdraw_apply","userId":"U30123","withdrawAmount":5200}', '["INGEST","STANDARDIZE","withdrawNo"]', 'OPEN', '等待资金中心补齐 withdrawNo 字段', '2026-03-08 11:08:00', NULL, NULL, 'system', '2026-03-08 11:08:01', 'system', '2026-03-08 11:08:01', b'0'),
(16003, 'T_ERR_ORDER_0003', 'E_ORDER_DLQ_0001', 'ORDER_CENTER_SDK', 'ORDER_RISK', 'pulsix.event.dlq', 'DLQ', 'INVALID_EVENT', 'DLQ_001', 'MEDIUM', '事件缺少 deviceId，已写入 DLQ 等待回放', '{"eventId":"E_ORDER_DLQ_0001","sceneCode":"ORDER_RISK","eventType":"order_paid","orderNo":"ORD20260308DLQ1"}', '{"eventId":"E_ORDER_DLQ_0001","sceneCode":"ORDER_RISK","eventType":"order_paid","orderNo":"ORD20260308DLQ1"}', '["DLQ","REPLAYABLE"]', 'REVIEWED', '已通知订单中心修复并安排回放', '2026-03-08 12:08:00', 'risk.ops', '2026-03-08 12:20:00', 'system', '2026-03-08 12:08:01', 'risk.ops', '2026-03-08 12:20:00', b'0'),
(16004, 'T_ERR_ORDER_0004', 'E_ORDER_ENGINE_0002', 'ORDER_CENTER_SDK', 'ORDER_RISK', 'pulsix.engine.error', 'ENGINE_EXECUTE', 'LOOKUP_TIMEOUT', 'ENG_504', 'HIGH', 'device_in_blacklist 查询超时，使用默认值 false 并记录异常', '{"eventId":"E_ORDER_ENGINE_0002","sceneCode":"ORDER_RISK","deviceId":"DVC_ORDER_BLACK_01"}', '{"eventId":"E_ORDER_ENGINE_0002","sceneCode":"ORDER_RISK","deviceId":"DVC_ORDER_BLACK_01","defaultDeviceInBlacklist":false}', '["ENGINE","LOOKUP_TIMEOUT"]', 'OPEN', '待排查 Redis 黑名单查询超时', '2026-03-08 12:16:00', NULL, NULL, 'system', '2026-03-08 12:16:01', 'system', '2026-03-08 12:16:01', b'0');

INSERT INTO `alert_channel_def` (`id`, `channel_code`, `channel_name`, `channel_type`, `webhook_url`, `secret`, `headers_json`, `status`, `description`, `creator`, `create_time`, `updater`, `update_time`, `deleted`) VALUES
(17001, 'FEISHU_RISK_OPS', '飞书风控运营群', 'FEISHU', 'https://open.feishu.cn/open-apis/bot/v2/hook/demo-risk-ops', 'feishu_demo_secret', '{"Content-Type":"application/json"}', 1, '营销标签、订单拒绝等风险事件统一发往风控运营群', 'admin', '2026-03-08 12:20:00', 'admin', '2026-03-08 12:20:00', b'0'),
(17002, 'WECOM_FINANCE_REVIEW', '企业微信提现审核群', 'WECOM', 'https://qyapi.weixin.qq.com/cgi-bin/webhook/send?key=demo-finance-review', 'wecom_demo_secret', '{"Content-Type":"application/json"}', 1, '提现待审核结果发往资金审核群', 'admin', '2026-03-08 12:20:00', 'admin', '2026-03-08 12:20:00', b'0'),
(17003, 'WEBHOOK_SRE', 'SRE 通用 Webhook', 'WEBHOOK', 'https://notify.example.com/pulsix/risk', NULL, '{"Content-Type":"application/json","X-Token":"demo-token"}', 1, '接入错误与引擎异常统一发往 SRE 告警网关', 'admin', '2026-03-08 12:20:00', 'admin', '2026-03-08 12:20:00', b'0');

INSERT INTO `alert_template_def` (`id`, `template_code`, `template_name`, `source_type`, `title_template`, `content_template`, `format_type`, `status`, `description`, `creator`, `create_time`, `updater`, `update_time`, `deleted`) VALUES
(18001, 'TPL_PROMOTION_TAG', '营销风控标签告警模板', 'DECISION_RESULT', '[{sceneCode}] TAG_ONLY 营销风险', '事件 {eventId} 命中规则 {hitRules}，建议营销系统补充风险标签，traceId={traceId}，riskLevel={riskLevel}', 'TEXT', 1, '用于营销 TAG_ONLY 风险事件告警', 'admin', '2026-03-08 12:21:00', 'admin', '2026-03-08 12:21:00', b'0'),
(18002, 'TPL_WITHDRAW_REVIEW', '提现审核告警模板', 'DECISION_RESULT', '[{sceneCode}] 提现待审核', '事件 {eventId} 命中规则 {hitRules}，请人工审核后再决定是否打款，traceId={traceId}，riskLevel={riskLevel}', 'TEXT', 1, '用于提现 REVIEW 风险事件告警', 'admin', '2026-03-08 12:21:00', 'admin', '2026-03-08 12:21:00', b'0'),
(18003, 'TPL_ORDER_REJECT', '订单拒绝告警模板', 'DECISION_RESULT', '[{sceneCode}] 订单拒绝', '事件 {eventId} 命中规则 {hitRules}，订单系统需取消订单、释放库存并触发退款，traceId={traceId}', 'TEXT', 1, '用于订单 REJECT 风险事件告警', 'admin', '2026-03-08 12:21:00', 'admin', '2026-03-08 12:21:00', b'0'),
(18004, 'TPL_STREAM_ERROR', '错误流告警模板', 'ERROR_STREAM', '[Pulsix] {errorStage} 异常', 'traceId={traceId}，eventId={eventId}，stage={errorStage}，message={errorMessage}', 'TEXT', 1, '用于 ingest/dlq/engine error 告警', 'admin', '2026-03-08 12:21:00', 'admin', '2026-03-08 12:21:00', b'0');

INSERT INTO `alert_rule_def` (`id`, `alert_rule_code`, `alert_rule_name`, `scene_code`, `source_type`, `action_filter_json`, `error_stage_filter_json`, `alert_level`, `dedupe_window_seconds`, `rate_limit_json`, `silence_conf_json`, `template_code`, `channel_codes_json`, `status`, `description`, `creator`, `create_time`, `updater`, `update_time`, `deleted`) VALUES
(19001, 'ALR_PROMOTION_TAG_ONLY', '营销风险标签告警', 'PROMOTION_RISK', 'DECISION_RESULT', '["TAG_ONLY"]', NULL, 'MEDIUM', 300, '{"windowSeconds":60,"limit":30}', '{"enabled":false}', 'TPL_PROMOTION_TAG', '["FEISHU_RISK_OPS"]', 1, '营销场景 TAG_ONLY 结果触发告警', 'admin', '2026-03-08 12:22:00', 'admin', '2026-03-08 12:22:00', b'0'),
(19002, 'ALR_WITHDRAW_REVIEW', '提现待审核告警', 'WITHDRAW_RISK', 'DECISION_RESULT', '["REVIEW"]', NULL, 'HIGH', 120, '{"windowSeconds":60,"limit":10}', '{"enabled":false}', 'TPL_WITHDRAW_REVIEW', '["WECOM_FINANCE_REVIEW","FEISHU_RISK_OPS"]', 1, '提现场景 REVIEW 结果触发告警', 'admin', '2026-03-08 12:22:00', 'admin', '2026-03-08 12:22:00', b'0'),
(19003, 'ALR_ORDER_REJECT', '订单拒绝告警', 'ORDER_RISK', 'DECISION_RESULT', '["REJECT"]', NULL, 'HIGH', 300, '{"windowSeconds":60,"limit":20}', '{"enabled":false}', 'TPL_ORDER_REJECT', '["FEISHU_RISK_OPS"]', 1, '订单场景 REJECT 结果触发告警', 'admin', '2026-03-08 12:22:00', 'admin', '2026-03-08 12:22:00', b'0'),
(19004, 'ALR_ERROR_STREAM', '错误流统一告警', NULL, 'ERROR_STREAM', NULL, '["AUTH","STANDARDIZE","DLQ","ENGINE_EXECUTE"]', 'MEDIUM', 600, '{"windowSeconds":60,"limit":50}', '{"enabled":true,"timeRanges":[{"start":"00:00","end":"06:00"}]}', 'TPL_STREAM_ERROR', '["WEBHOOK_SRE","FEISHU_RISK_OPS"]', 1, '接入与引擎异常统一告警', 'admin', '2026-03-08 12:22:00', 'admin', '2026-03-08 12:22:00', b'0');

INSERT INTO `alert_record` (`id`, `alert_rule_code`, `source_type`, `source_id`, `trace_id`, `event_id`, `scene_code`, `alert_level`, `channel_code`, `template_code`, `send_status`, `dedupe_key`, `dedupe_hit_flag`, `silence_hit_flag`, `request_payload`, `response_payload`, `sent_at`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`) VALUES
(20001, 'ALR_PROMOTION_TAG_ONLY', 'DECISION_RESULT', 'RISK_EVT_20260308_001', 'T_PROMO_0002', 'E_PROMO_0002', 'PROMOTION_RISK', 'MEDIUM', 'FEISHU_RISK_OPS', 'TPL_PROMOTION_TAG', 'SUCCESS', 'PROMOTION_RISK:TAG_ONLY:E_PROMO_0002', b'0', b'0', '{"traceId":"T_PROMO_0002","eventId":"E_PROMO_0002","finalAction":"TAG_ONLY","hitRules":["PROMOTION_R001"]}', '{"code":0,"msg":"ok"}', '2026-03-08 10:12:05', '营销 TAG_ONLY 告警发送成功', 'system', '2026-03-08 10:12:05', 'system', '2026-03-08 10:12:05', b'0'),
(20002, 'ALR_WITHDRAW_REVIEW', 'DECISION_RESULT', 'RISK_EVT_20260308_002', 'T_WD_0002', 'E_WD_0002', 'WITHDRAW_RISK', 'HIGH', 'WECOM_FINANCE_REVIEW', 'TPL_WITHDRAW_REVIEW', 'SUCCESS', 'WITHDRAW_RISK:REVIEW:E_WD_0002', b'0', b'0', '{"traceId":"T_WD_0002","eventId":"E_WD_0002","finalAction":"REVIEW","hitRules":["WITHDRAW_R001"]}', '{"errcode":0,"errmsg":"ok"}', '2026-03-08 11:18:05', '提现待审核告警发送成功', 'system', '2026-03-08 11:18:05', 'system', '2026-03-08 11:18:05', b'0'),
(20003, 'ALR_ORDER_REJECT', 'DECISION_RESULT', 'RISK_EVT_20260308_003', 'T_ORDER_0002', 'E_ORDER_0002', 'ORDER_RISK', 'HIGH', 'FEISHU_RISK_OPS', 'TPL_ORDER_REJECT', 'SUCCESS', 'ORDER_RISK:REJECT:E_ORDER_0002', b'0', b'0', '{"traceId":"T_ORDER_0002","eventId":"E_ORDER_0002","finalAction":"REJECT","hitRules":["ORDER_R001"]}', '{"code":0,"msg":"ok"}', '2026-03-08 12:12:05', '订单拒绝告警发送成功', 'system', '2026-03-08 12:12:05', 'system', '2026-03-08 12:12:05', b'0'),
(20004, 'ALR_ERROR_STREAM', 'ERROR_STREAM', '16004', 'T_ERR_ORDER_0004', 'E_ORDER_ENGINE_0002', 'ORDER_RISK', 'MEDIUM', 'WEBHOOK_SRE', 'TPL_STREAM_ERROR', 'SKIPPED', 'ERROR_STREAM:ENGINE_EXECUTE:ORDER_CENTER_SDK', b'1', b'0', '{"traceId":"T_ERR_ORDER_0004","eventId":"E_ORDER_ENGINE_0002","errorStage":"ENGINE_EXECUTE"}', '{"reason":"duplicated within 600s"}', '2026-03-08 12:16:05', '命中去重窗口，跳过重复告警', 'system', '2026-03-08 12:16:05', 'system', '2026-03-08 12:16:05', b'0');

COMMIT;

SET FOREIGN_KEY_CHECKS = 1;
