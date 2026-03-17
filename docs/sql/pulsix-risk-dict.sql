/*
 pulsix-risk-dict.sql

 用途：
 - 风控相关字典初始化脚本
 - 当前仅包含接入治理相关字典：
   - risk_access_source_type
   - risk_access_topic_name
 - 不包含风控业务表
 - 不包含风控菜单

 建议执行顺序（全新库）：
 1. 先执行 docs/sql/pulsix-system-infra.sql
 2. 再执行 docs/sql/pulsix-risk.sql
 3. 再执行本文件
 4. 最后执行 docs/sql/pulsix-risk-menu.sql

 说明：
 - 本文件使用 NOT EXISTS 方式写入，允许重复执行
*/

START TRANSACTION;

INSERT INTO `system_dict_type`
(`name`, `type`, `status`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `deleted_time`)
SELECT '风控接入源类型', 'risk_access_source_type', 0, '风控接入源类型字典', 'admin', CURRENT_TIMESTAMP,
       'admin', CURRENT_TIMESTAMP, b'0', NULL
FROM DUAL
WHERE NOT EXISTS (
  SELECT 1 FROM `system_dict_type` WHERE `type` = 'risk_access_source_type' AND `deleted` = b'0'
);

INSERT INTO `system_dict_type`
(`name`, `type`, `status`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `deleted_time`)
SELECT '风控标准事件 Topic', 'risk_access_topic_name', 0, '风控标准事件 Topic 字典', 'admin', CURRENT_TIMESTAMP,
       'admin', CURRENT_TIMESTAMP, b'0', NULL
FROM DUAL
WHERE NOT EXISTS (
  SELECT 1 FROM `system_dict_type` WHERE `type` = 'risk_access_topic_name' AND `deleted` = b'0'
);

INSERT INTO `system_dict_data`
(`sort`, `label`, `value`, `dict_type`, `status`, `color_type`, `css_class`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT 1, 'HTTP', 'HTTP', 'risk_access_source_type', 0, 'primary', '', 'HTTP 接入源', 'admin', CURRENT_TIMESTAMP,
       'admin', CURRENT_TIMESTAMP, b'0'
FROM DUAL
WHERE NOT EXISTS (
  SELECT 1 FROM `system_dict_data`
  WHERE `dict_type` = 'risk_access_source_type' AND `value` = 'HTTP' AND `deleted` = b'0'
);

INSERT INTO `system_dict_data`
(`sort`, `label`, `value`, `dict_type`, `status`, `color_type`, `css_class`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT 2, 'SDK', 'SDK', 'risk_access_source_type', 0, 'success', '', 'SDK 接入源', 'admin', CURRENT_TIMESTAMP,
       'admin', CURRENT_TIMESTAMP, b'0'
FROM DUAL
WHERE NOT EXISTS (
  SELECT 1 FROM `system_dict_data`
  WHERE `dict_type` = 'risk_access_source_type' AND `value` = 'SDK' AND `deleted` = b'0'
);

INSERT INTO `system_dict_data`
(`sort`, `label`, `value`, `dict_type`, `status`, `color_type`, `css_class`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT 1, 'pulsix.event.standard', 'pulsix.event.standard', 'risk_access_topic_name', 0, 'warning', '',
       '统一标准事件 Topic', 'admin', CURRENT_TIMESTAMP, 'admin', CURRENT_TIMESTAMP, b'0'
FROM DUAL
WHERE NOT EXISTS (
  SELECT 1 FROM `system_dict_data`
  WHERE `dict_type` = 'risk_access_topic_name' AND `value` = 'pulsix.event.standard' AND `deleted` = b'0'
);

COMMIT;
