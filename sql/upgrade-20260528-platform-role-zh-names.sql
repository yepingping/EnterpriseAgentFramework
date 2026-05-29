-- 平台角色与默认管理员展示名中文化（已有环境升级）
UPDATE `platform_role` SET `role_name` = '平台管理员', `description` = '平台全量管理与配置' WHERE `role_code` = 'PLATFORM_ADMIN';
UPDATE `platform_role` SET `role_name` = '智能体设计者', `description` = '创建与编辑智能体及工作流' WHERE `role_code` = 'AGENT_DESIGNER';
UPDATE `platform_role` SET `role_name` = '项目负责人', `description` = '管理已分配业务项目' WHERE `role_code` = 'PROJECT_OWNER';
UPDATE `platform_role` SET `role_name` = '运维操作员', `description` = '运行、调试与回放会话' WHERE `role_code` = 'OPERATOR';
UPDATE `platform_role` SET `role_name` = '审计员', `description` = '只读审计与追踪' WHERE `role_code` = 'AUDITOR';

UPDATE `platform_user`
SET `display_name` = '平台管理员'
WHERE `username` = 'admin' AND (`display_name` IS NULL OR `display_name` = 'Platform Admin');
