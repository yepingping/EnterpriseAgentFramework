-- ReachAI Context Runtime User Mapping.
-- Adds the platform-user to runtime-user identity bridge required for RBAC-controlled
-- RUNTIME_USER candidate review. This script does not backfill mappings.

CREATE TABLE IF NOT EXISTS `context_runtime_user_mapping` (
    `id`               BIGINT       NOT NULL AUTO_INCREMENT,
    `tenant_id`        VARCHAR(96)  NOT NULL DEFAULT 'default',
    `platform_user_id` BIGINT       NOT NULL COMMENT '平台管理端用户 ID',
    `runtime_user_id`  VARCHAR(128) NOT NULL COMMENT 'Context runtime user ownerId（global/external/user 归一值）',
    `global_user_id`   VARCHAR(128) DEFAULT NULL,
    `external_user_id` VARCHAR(128) DEFAULT NULL,
    `project_id`       BIGINT       DEFAULT NULL,
    `project_code`     VARCHAR(96)  DEFAULT NULL,
    `status`           VARCHAR(24)  NOT NULL DEFAULT 'ACTIVE',
    `created_by`       VARCHAR(128) DEFAULT NULL,
    `created_at`       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted_at`       DATETIME     DEFAULT NULL,
    PRIMARY KEY (`id`),
    KEY `idx_context_runtime_user_mapping_actor` (`tenant_id`, `platform_user_id`, `runtime_user_id`, `status`),
    KEY `idx_context_runtime_user_mapping_project` (`tenant_id`, `project_code`, `project_id`, `status`),
    KEY `idx_context_runtime_user_mapping_runtime` (`tenant_id`, `runtime_user_id`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='平台用户代审 Runtime User 记忆的身份映射';

INSERT IGNORE INTO `platform_permission` (`permission_code`, `permission_name`, `resource_type`, `action`)
VALUES
('context:runtime-user:review', 'Review runtime user context candidates', 'CONTEXT_RUNTIME_USER', 'REVIEW'),
('context:runtime-user:mapping:manage', 'Manage runtime user review mappings', 'CONTEXT_RUNTIME_USER', 'MANAGE_MAPPING');

INSERT IGNORE INTO `platform_role_permission` (`role_id`, `permission_id`)
SELECT r.id, p.id FROM `platform_role` r JOIN `platform_permission` p
WHERE r.role_code = 'PLATFORM_ADMIN'
  AND p.permission_code IN ('context:runtime-user:review', 'context:runtime-user:mapping:manage');
