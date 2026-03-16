/*
 pulsix-risk-menu.sql

 执行顺序：
 1. 先执行 docs/sql/pulsix-system-infra.sql
 2. 再执行 docs/sql/pulsix-risk.sql
 3. 最后执行本文件

 说明：
 - 本文件只补充 system_menu 的 risk 菜单树，不改 system_role_menu。
 - 超级管理员会通过现有权限逻辑自动看到全部菜单；普通角色后续在角色管理中按需授权。
 - 菜单 ID 独占 6000-6084 区间，便于后续继续扩展按钮权限或隐藏页。
*/

SET NAMES utf8mb4;

BEGIN;

DELETE FROM `system_menu`
WHERE `id` BETWEEN 6000 AND 6084;

INSERT INTO `system_menu` (
  `id`,
  `name`,
  `permission`,
  `type`,
  `sort`,
  `parent_id`,
  `path`,
  `icon`,
  `component`,
  `component_name`,
  `status`,
  `visible`,
  `keep_alive`,
  `always_show`,
  `creator`,
  `create_time`,
  `updater`,
  `update_time`,
  `deleted`
) VALUES
  (6000, '实时风控', '', 1, 600, 0, '/risk', 'ep:warning', NULL, NULL, 0, b'1', b'1', b'1', 'admin', CURRENT_TIMESTAMP, 'admin', CURRENT_TIMESTAMP, b'0'),
  (6001, '风控总览', '', 1, 1, 6000, 'overview', 'ep:data-analysis', NULL, NULL, 0, b'1', b'1', b'1', 'admin', CURRENT_TIMESTAMP, 'admin', CURRENT_TIMESTAMP, b'0'),
  (6002, '配置中心', '', 1, 2, 6000, 'config', 'ep:setting', NULL, NULL, 0, b'1', b'1', b'1', 'admin', CURRENT_TIMESTAMP, 'admin', CURRENT_TIMESTAMP, b'0'),
  (6003, '发布中心', '', 1, 3, 6000, 'release', 'ep:upload-filled', NULL, NULL, 0, b'1', b'1', b'1', 'admin', CURRENT_TIMESTAMP, 'admin', CURRENT_TIMESTAMP, b'0'),
  (6004, '仿真测试', '', 1, 4, 6000, 'simulation', 'ep:cpu', NULL, NULL, 0, b'1', b'1', b'1', 'admin', CURRENT_TIMESTAMP, 'admin', CURRENT_TIMESTAMP, b'0'),
  (6005, '查询分析', '', 1, 5, 6000, 'query', 'ep:histogram', NULL, NULL, 0, b'1', b'1', b'1', 'admin', CURRENT_TIMESTAMP, 'admin', CURRENT_TIMESTAMP, b'0'),
  (6006, '告警中心', '', 1, 6, 6000, 'alert', 'ep:bell', NULL, NULL, 0, b'1', b'1', b'1', 'admin', CURRENT_TIMESTAMP, 'admin', CURRENT_TIMESTAMP, b'0'),
  (6007, '接入治理', '', 1, 7, 6000, 'access', 'ep:guide', NULL, NULL, 0, b'1', b'1', b'1', 'admin', CURRENT_TIMESTAMP, 'admin', CURRENT_TIMESTAMP, b'0'),
  (6010, 'Dashboard', '', 2, 1, 6001, 'dashboard', 'ep:odometer', 'risk/overview/dashboard/index', 'RiskDashboard', 0, b'1', b'1', b'1', 'admin', CURRENT_TIMESTAMP, 'admin', CURRENT_TIMESTAMP, b'0'),
  (6020, '场景管理', '', 2, 1, 6002, 'scene', 'ep:collection', 'risk/config/scene/index', 'RiskScene', 0, b'1', b'1', b'1', 'admin', CURRENT_TIMESTAMP, 'admin', CURRENT_TIMESTAMP, b'0'),
  (6021, '事件模型', '', 2, 2, 6002, 'event-model', 'ep:document', 'risk/config/event-model/index', 'RiskEventModel', 0, b'1', b'1', b'1', 'admin', CURRENT_TIMESTAMP, 'admin', CURRENT_TIMESTAMP, b'0'),
  (6022, '实体类型', '', 2, 3, 6002, 'entity-type', 'ep:user', 'risk/config/entity-type/index', 'RiskEntityType', 0, b'1', b'1', b'1', 'admin', CURRENT_TIMESTAMP, 'admin', CURRENT_TIMESTAMP, b'0'),
  (6023, '名单中心', '', 2, 4, 6002, 'list-center', 'ep:finished', 'risk/config/list-center/index', 'RiskListCenter', 0, b'1', b'1', b'1', 'admin', CURRENT_TIMESTAMP, 'admin', CURRENT_TIMESTAMP, b'0'),
  (6024, '特征中心', '', 2, 5, 6002, 'feature-center', 'ep:trend-charts', 'risk/config/feature-center/index', 'RiskFeatureCenter', 0, b'1', b'1', b'1', 'admin', CURRENT_TIMESTAMP, 'admin', CURRENT_TIMESTAMP, b'0'),
  (6025, '规则中心', '', 2, 6, 6002, 'rule-center', 'ep:connection', 'risk/config/rule-center/index', 'RiskRuleCenter', 0, b'1', b'1', b'1', 'admin', CURRENT_TIMESTAMP, 'admin', CURRENT_TIMESTAMP, b'0'),
  (6026, '策略中心', '', 2, 7, 6002, 'policy-center', 'ep:share', 'risk/config/policy-center/index', 'RiskPolicyCenter', 0, b'1', b'1', b'1', 'admin', CURRENT_TIMESTAMP, 'admin', CURRENT_TIMESTAMP, b'0'),
  (6030, '发布管理', '', 2, 1, 6003, 'manage', 'ep:promotion', 'risk/release/manage/index', 'RiskReleaseManage', 0, b'1', b'1', b'1', 'admin', CURRENT_TIMESTAMP, 'admin', CURRENT_TIMESTAMP, b'0'),
  (6031, '发布记录', '', 2, 2, 6003, 'record', 'ep:tickets', 'risk/release/record/index', 'RiskReleaseRecord', 0, b'1', b'1', b'1', 'admin', CURRENT_TIMESTAMP, 'admin', CURRENT_TIMESTAMP, b'0'),
  (6032, '版本对比 / 回滚', '', 2, 3, 6003, 'diff-rollback', 'ep:refresh-left', 'risk/release/diff-rollback/index', 'RiskReleaseDiffRollback', 0, b'1', b'1', b'1', 'admin', CURRENT_TIMESTAMP, 'admin', CURRENT_TIMESTAMP, b'0'),
  (6040, '单条事件仿真', '', 2, 1, 6004, 'event', 'ep:edit-pen', 'risk/simulation/event/index', 'RiskSimulationEvent', 0, b'1', b'1', b'1', 'admin', CURRENT_TIMESTAMP, 'admin', CURRENT_TIMESTAMP, b'0'),
  (6041, '仿真用例', '', 2, 2, 6004, 'case', 'ep:files', 'risk/simulation/case/index', 'RiskSimulationCase', 0, b'1', b'1', b'1', 'admin', CURRENT_TIMESTAMP, 'admin', CURRENT_TIMESTAMP, b'0'),
  (6042, '仿真报告', '', 2, 3, 6004, 'report', 'ep:data-analysis', 'risk/simulation/report/index', 'RiskSimulationReport', 0, b'1', b'1', b'1', 'admin', CURRENT_TIMESTAMP, 'admin', CURRENT_TIMESTAMP, b'0'),
  (6050, '决策日志', '', 2, 1, 6005, 'decision-log', 'ep:list', 'risk/query/decision-log/index', 'RiskDecisionLog', 0, b'1', b'1', b'1', 'admin', CURRENT_TIMESTAMP, 'admin', CURRENT_TIMESTAMP, b'0'),
  (6051, '命中明细', '', 2, 2, 6005, 'hit-detail', 'ep:document-checked', 'risk/query/hit-detail/index', 'RiskHitDetail', 0, b'1', b'1', b'1', 'admin', CURRENT_TIMESTAMP, 'admin', CURRENT_TIMESTAMP, b'0'),
  (6052, '风险事件查询', '', 2, 3, 6005, 'risk-event', 'ep:warning-filled', 'risk/query/risk-event/index', 'RiskRiskEvent', 0, b'1', b'1', b'1', 'admin', CURRENT_TIMESTAMP, 'admin', CURRENT_TIMESTAMP, b'0'),
  (6060, '告警配置', '', 2, 1, 6006, 'config', 'ep:message-box', 'risk/alert/config/index', 'RiskAlertConfig', 0, b'1', b'1', b'1', 'admin', CURRENT_TIMESTAMP, 'admin', CURRENT_TIMESTAMP, b'0'),
  (6061, '告警记录', '', 2, 2, 6006, 'record', 'ep:chat-line-square', 'risk/alert/record/index', 'RiskAlertRecord', 0, b'1', b'1', b'1', 'admin', CURRENT_TIMESTAMP, 'admin', CURRENT_TIMESTAMP, b'0'),
  (6070, '接入源管理', '', 2, 1, 6007, 'source', 'ep:link', 'risk/access/source/index', 'RiskAccessSource', 0, b'1', b'1', b'1', 'admin', CURRENT_TIMESTAMP, 'admin', CURRENT_TIMESTAMP, b'0'),
  (6071, '鉴权配置', '', 2, 2, 6007, 'auth', 'ep:key', 'risk/access/auth/index', 'RiskAccessAuth', 0, b'1', b'1', b'1', 'admin', CURRENT_TIMESTAMP, 'admin', CURRENT_TIMESTAMP, b'0'),
  (6072, '错误治理', '', 2, 3, 6007, 'error', 'ep:circle-close-filled', 'risk/access/error/index', 'RiskAccessError', 0, b'1', b'1', b'1', 'admin', CURRENT_TIMESTAMP, 'admin', CURRENT_TIMESTAMP, b'0'),
  (6073, '接入指引', '', 2, 4, 6007, 'guide', 'ep:reading', 'risk/access/guide/index', 'RiskAccessGuide', 0, b'1', b'1', b'1', 'admin', CURRENT_TIMESTAMP, 'admin', CURRENT_TIMESTAMP, b'0'),
  (6074, '场景查询', 'risk:scene:query', 3, 1, 6020, '', '', '', NULL, 0, b'1', b'1', b'1', 'admin', CURRENT_TIMESTAMP, 'admin', CURRENT_TIMESTAMP, b'0'),
  (6075, '场景新增', 'risk:scene:create', 3, 2, 6020, '', '', '', NULL, 0, b'1', b'1', b'1', 'admin', CURRENT_TIMESTAMP, 'admin', CURRENT_TIMESTAMP, b'0'),
  (6076, '场景修改', 'risk:scene:update', 3, 3, 6020, '', '', '', NULL, 0, b'1', b'1', b'1', 'admin', CURRENT_TIMESTAMP, 'admin', CURRENT_TIMESTAMP, b'0'),
  (6077, '场景删除', 'risk:scene:delete', 3, 4, 6020, '', '', '', NULL, 0, b'1', b'1', b'1', 'admin', CURRENT_TIMESTAMP, 'admin', CURRENT_TIMESTAMP, b'0'),
  (6078, '事件模型查询', 'risk:event-model:query', 3, 1, 6021, '', '', '', NULL, 0, b'1', b'1', b'1', 'admin', CURRENT_TIMESTAMP, 'admin', CURRENT_TIMESTAMP, b'0'),
  (6079, '事件模型新增', 'risk:event-model:create', 3, 2, 6021, '', '', '', NULL, 0, b'1', b'1', b'1', 'admin', CURRENT_TIMESTAMP, 'admin', CURRENT_TIMESTAMP, b'0'),
  (6080, '事件模型修改', 'risk:event-model:update', 3, 3, 6021, '', '', '', NULL, 0, b'1', b'1', b'1', 'admin', CURRENT_TIMESTAMP, 'admin', CURRENT_TIMESTAMP, b'0'),
  (6081, '事件模型删除', 'risk:event-model:delete', 3, 4, 6021, '', '', '', NULL, 0, b'1', b'1', b'1', 'admin', CURRENT_TIMESTAMP, 'admin', CURRENT_TIMESTAMP, b'0');

COMMIT;
