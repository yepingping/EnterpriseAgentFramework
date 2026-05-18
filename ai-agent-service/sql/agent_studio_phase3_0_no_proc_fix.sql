-- ============================================================================
-- Agent Studio Phase 3.0 compatibility fix without stored procedures.
--
-- Use this when the SQL client does not execute DELIMITER / CREATE PROCEDURE
-- blocks correctly and reports:
--   PROCEDURE ai_text_service.add_col_if_absent does not exist
--
-- This script is idempotent for the Phase 3.0 objects used by Agent Studio:
--   agent_definition columns/indexes
--   agent_version
--   agent_release_event
-- ============================================================================

USE `ai_text_service`;

CREATE TABLE IF NOT EXISTS `agent_definition` (
    `id`                      VARCHAR(32)  NOT NULL,
    `key_slug`                VARCHAR(64)  NOT NULL,
    `name`                    VARCHAR(128) NOT NULL,
    `description`             VARCHAR(512) DEFAULT NULL,
    `intent_type`             VARCHAR(64)  DEFAULT NULL,
    `system_prompt`           TEXT         DEFAULT NULL,
    `tools_json`              TEXT         DEFAULT NULL,
    `model_instance_id`       VARCHAR(64)  DEFAULT NULL,
    `runtime_type`            VARCHAR(32)  NOT NULL DEFAULT 'AGENTSCOPE',
    `runtime_placement`       VARCHAR(24)  NOT NULL DEFAULT 'CENTRAL',
    `runtime_config_json`     MEDIUMTEXT   DEFAULT NULL,
    `graph_spec_json`         MEDIUMTEXT   DEFAULT NULL,
    `max_steps`               INT          NOT NULL DEFAULT 5,
    `type`                    VARCHAR(32)  NOT NULL DEFAULT 'single',
    `pipeline_agent_ids_json` TEXT         DEFAULT NULL,
    `knowledge_base_group_id` VARCHAR(64)  DEFAULT NULL,
    `prompt_template_id`      VARCHAR(64)  DEFAULT NULL,
    `output_schema_type`      VARCHAR(64)  DEFAULT NULL,
    `trigger_mode`            VARCHAR(16)  NOT NULL DEFAULT 'all',
    `use_multi_agent_model`   TINYINT(1)   NOT NULL DEFAULT 0,
    `extra_json`              TEXT         DEFAULT NULL,
    `canvas_json`             MEDIUMTEXT   DEFAULT NULL,
    `enabled`                 TINYINT(1)   NOT NULL DEFAULT 1,
    `allow_irreversible`      TINYINT(1)   NOT NULL DEFAULT 0,
    `created_at`              DATETIME     DEFAULT CURRENT_TIMESTAMP,
    `updated_at`              DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_agent_key_slug` (`key_slug`),
    KEY `idx_agent_intent` (`intent_type`),
    KEY `idx_agent_enabled` (`enabled`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Agent definition';

SET @sql := IF(
    EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'agent_definition' AND column_name = 'key_slug'),
    'SELECT 1',
    'ALTER TABLE `agent_definition` ADD COLUMN `key_slug` VARCHAR(64) NOT NULL DEFAULT '''' COMMENT ''human readable slug'' AFTER `id`'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql := IF(
    EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'agent_definition' AND column_name = 'canvas_json'),
    'SELECT 1',
    'ALTER TABLE `agent_definition` ADD COLUMN `canvas_json` MEDIUMTEXT DEFAULT NULL COMMENT ''Agent Studio canvas JSON'''
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql := IF(
    EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'agent_definition' AND column_name = 'allow_irreversible'),
    'SELECT 1',
    'ALTER TABLE `agent_definition` ADD COLUMN `allow_irreversible` TINYINT(1) NOT NULL DEFAULT 0 COMMENT ''allow irreversible tools'''
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql := IF(
    EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'agent_definition' AND column_name = 'runtime_type'),
    'SELECT 1',
    'ALTER TABLE `agent_definition` ADD COLUMN `runtime_type` VARCHAR(32) NOT NULL DEFAULT ''AGENTSCOPE'' COMMENT ''Agent runtime type'' AFTER `model_instance_id`'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql := IF(
    EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'agent_definition' AND column_name = 'runtime_placement'),
    'SELECT 1',
    'ALTER TABLE `agent_definition` ADD COLUMN `runtime_placement` VARCHAR(24) NOT NULL DEFAULT ''CENTRAL'' COMMENT ''CENTRAL / EMBEDDED / HYBRID'' AFTER `runtime_type`'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql := IF(
    EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'agent_definition' AND column_name = 'runtime_config_json'),
    'SELECT 1',
    'ALTER TABLE `agent_definition` ADD COLUMN `runtime_config_json` MEDIUMTEXT DEFAULT NULL COMMENT ''runtime specific config JSON'' AFTER `runtime_placement`'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql := IF(
    EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'agent_definition' AND column_name = 'graph_spec_json'),
    'SELECT 1',
    'ALTER TABLE `agent_definition` ADD COLUMN `graph_spec_json` MEDIUMTEXT DEFAULT NULL COMMENT ''Platform GraphSpec JSON'' AFTER `runtime_config_json`'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql := IF(
    EXISTS (SELECT 1 FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'agent_definition' AND index_name = 'uk_agent_key_slug'),
    'SELECT 1',
    'CREATE UNIQUE INDEX `uk_agent_key_slug` ON `agent_definition` (`key_slug`)'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql := IF(
    EXISTS (SELECT 1 FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'agent_definition' AND index_name = 'idx_agent_intent'),
    'SELECT 1',
    'CREATE INDEX `idx_agent_intent` ON `agent_definition` (`intent_type`)'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql := IF(
    EXISTS (SELECT 1 FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'agent_definition' AND index_name = 'idx_agent_enabled'),
    'SELECT 1',
    'CREATE INDEX `idx_agent_enabled` ON `agent_definition` (`enabled`)'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql := IF(
    EXISTS (SELECT 1 FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'agent_definition' AND index_name = 'idx_agent_runtime'),
    'SELECT 1',
    'CREATE INDEX `idx_agent_runtime` ON `agent_definition` (`runtime_type`, `enabled`)'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql := IF(
    EXISTS (SELECT 1 FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'agent_definition' AND index_name = 'idx_agent_runtime_placement'),
    'SELECT 1',
    'CREATE INDEX `idx_agent_runtime_placement` ON `agent_definition` (`runtime_placement`, `enabled`)'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

CREATE TABLE IF NOT EXISTS `agent_version` (
    `id`               BIGINT        NOT NULL AUTO_INCREMENT,
    `agent_id`         VARCHAR(32)   NOT NULL,
    `version`          VARCHAR(32)   NOT NULL,
    `snapshot_json`    MEDIUMTEXT    NOT NULL,
    `rollout_percent`  INT           NOT NULL DEFAULT 0,
    `status`           VARCHAR(16)   NOT NULL DEFAULT 'DRAFT',
    `published_by`     VARCHAR(64)   DEFAULT NULL,
    `published_at`     DATETIME      DEFAULT NULL,
    `note`             VARCHAR(512)  DEFAULT NULL,
    `create_time`      DATETIME      DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_agent_version` (`agent_id`, `version`),
    KEY `idx_agent_status` (`agent_id`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Agent release version snapshot';

CREATE TABLE IF NOT EXISTS `agent_release_event` (
    `id`               BIGINT        NOT NULL AUTO_INCREMENT,
    `agent_id`         VARCHAR(32)   NOT NULL,
    `version_id`       BIGINT        DEFAULT NULL,
    `version`          VARCHAR(32)   DEFAULT NULL,
    `action`           VARCHAR(24)   NOT NULL,
    `decision`         VARCHAR(24)   NOT NULL,
    `rollout_percent`  INT           DEFAULT NULL,
    `operator`         VARCHAR(64)   DEFAULT NULL,
    `summary`          VARCHAR(512)  DEFAULT NULL,
    `validation_json`  MEDIUMTEXT    DEFAULT NULL,
    `metadata_json`    TEXT          DEFAULT NULL,
    `created_at`       DATETIME      DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_agent_release_event_agent` (`agent_id`, `id`),
    KEY `idx_agent_release_event_version` (`version_id`),
    KEY `idx_agent_release_event_action` (`agent_id`, `action`, `decision`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Agent release audit event';
