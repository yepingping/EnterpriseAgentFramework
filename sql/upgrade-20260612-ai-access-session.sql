-- ReachAI AI quick access session progress
-- Existing dev/test databases can execute this script once; statements are idempotent.

CREATE TABLE IF NOT EXISTS `eaf_ai_access_session` (
    `id`              BIGINT       NOT NULL AUTO_INCREMENT,
    `session_id`      VARCHAR(96)  NOT NULL,
    `project_id`      BIGINT       NOT NULL,
    `project_code`    VARCHAR(128) DEFAULT NULL,
    `tool_name`       VARCHAR(64)  DEFAULT NULL,
    `scenario`        VARCHAR(32)  NOT NULL DEFAULT 'SDK_ACCESS',
    `target_page_key` VARCHAR(160) DEFAULT NULL,
    `target_route`    VARCHAR(512) DEFAULT NULL,
    `metadata_json`   TEXT         DEFAULT NULL,
    `status`          VARCHAR(24)  NOT NULL DEFAULT 'OPEN',
    `total_steps`     INT          NOT NULL DEFAULT 0,
    `completed_steps` INT          NOT NULL DEFAULT 0,
    `failed_steps`    INT          NOT NULL DEFAULT 0,
    `last_message`    VARCHAR(512) DEFAULT NULL,
    `created_at`      DATETIME     DEFAULT CURRENT_TIMESTAMP,
    `updated_at`      DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_ai_access_session` (`session_id`),
    KEY `idx_ai_access_session_project` (`project_id`, `updated_at`),
    KEY `idx_ai_access_session_target` (`project_id`, `scenario`, `target_page_key`, `updated_at`),
    KEY `idx_ai_access_session_status` (`project_code`, `status`, `updated_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI快速接入会话';

CREATE TABLE IF NOT EXISTS `eaf_ai_access_step` (
    `id`            BIGINT       NOT NULL AUTO_INCREMENT,
    `session_id`    VARCHAR(96)  NOT NULL,
    `project_id`    BIGINT       NOT NULL,
    `step_key`      VARCHAR(96)  NOT NULL,
    `title`         VARCHAR(128) NOT NULL,
    `status`        VARCHAR(24)  NOT NULL DEFAULT 'TODO',
    `message`       TEXT         DEFAULT NULL,
    `files_json`    TEXT         DEFAULT NULL,
    `evidence_json` TEXT         DEFAULT NULL,
    `reported_by`   VARCHAR(96)  DEFAULT NULL,
    `started_at`    DATETIME     DEFAULT NULL,
    `completed_at`  DATETIME     DEFAULT NULL,
    `updated_at`    DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_ai_access_step` (`session_id`, `step_key`),
    KEY `idx_ai_access_step_project` (`project_id`, `status`, `updated_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI快速接入步骤进度';

DROP PROCEDURE IF EXISTS add_ai_access_col_if_absent;
DELIMITER $$
CREATE PROCEDURE add_ai_access_col_if_absent(
    IN p_table VARCHAR(128),
    IN p_column VARCHAR(128),
    IN p_definition TEXT
)
BEGIN
    IF NOT EXISTS (
        SELECT 1
          FROM information_schema.columns
         WHERE table_schema = DATABASE()
           AND table_name = p_table
           AND column_name = p_column
    ) THEN
        SET @ddl = CONCAT('ALTER TABLE `', p_table, '` ADD COLUMN `', p_column, '` ', p_definition);
        PREPARE stmt FROM @ddl;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;
END$$
DELIMITER ;

CALL add_ai_access_col_if_absent('eaf_ai_access_session', 'scenario',
    'VARCHAR(32) NOT NULL DEFAULT ''SDK_ACCESS'' COMMENT ''SDK_ACCESS or PAGE_ASSISTANT'' AFTER `tool_name`');
CALL add_ai_access_col_if_absent('eaf_ai_access_session', 'target_page_key',
    'VARCHAR(160) DEFAULT NULL COMMENT ''Target business page key for page assistant onboarding'' AFTER `scenario`');
CALL add_ai_access_col_if_absent('eaf_ai_access_session', 'target_route',
    'VARCHAR(512) DEFAULT NULL COMMENT ''Target business frontend route for page assistant onboarding'' AFTER `target_page_key`');
CALL add_ai_access_col_if_absent('eaf_ai_access_session', 'metadata_json',
    'TEXT DEFAULT NULL COMMENT ''Scenario-specific onboarding metadata'' AFTER `target_route`');

UPDATE `eaf_ai_access_session`
   SET `scenario` = 'SDK_ACCESS'
 WHERE `scenario` IS NULL OR `scenario` = '';

DROP PROCEDURE IF EXISTS add_ai_access_col_if_absent;

DROP PROCEDURE IF EXISTS add_ai_access_index_if_absent;
DELIMITER $$
CREATE PROCEDURE add_ai_access_index_if_absent(
    IN p_table VARCHAR(128),
    IN p_index VARCHAR(128),
    IN p_definition TEXT
)
BEGIN
    IF NOT EXISTS (
        SELECT 1
          FROM information_schema.statistics
         WHERE table_schema = DATABASE()
           AND table_name = p_table
           AND index_name = p_index
    ) THEN
        SET @ddl = CONCAT('ALTER TABLE `', p_table, '` ADD INDEX `', p_index, '` ', p_definition);
        PREPARE stmt FROM @ddl;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;
END$$
DELIMITER ;

CALL add_ai_access_index_if_absent('eaf_ai_access_session', 'idx_ai_access_session_target',
    '(`project_id`, `scenario`, `target_page_key`, `updated_at`)');

DROP PROCEDURE IF EXISTS add_ai_access_index_if_absent;
