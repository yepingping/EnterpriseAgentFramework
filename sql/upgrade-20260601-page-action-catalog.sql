-- ReachAI page action catalog for business frontend SDK registration.
-- Execute after the existing SQL baseline when upgrading an existing dev/test database.

CREATE TABLE IF NOT EXISTS `eaf_page_registry` (
    `id`                       BIGINT       NOT NULL AUTO_INCREMENT,
    `project_code`             VARCHAR(96)  NOT NULL,
    `app_id`                   VARCHAR(96)  NOT NULL,
    `page_key`                 VARCHAR(160) NOT NULL,
    `name`                     VARCHAR(160) NOT NULL,
    `route_pattern`            VARCHAR(512) DEFAULT NULL,
    `origin`                   VARCHAR(512) NOT NULL DEFAULT '',
    `current_page_instance_id` VARCHAR(128) DEFAULT NULL,
    `status`                   VARCHAR(24)  NOT NULL DEFAULT 'ACTIVE',
    `last_seen_at`             DATETIME     DEFAULT NULL,
    `metadata_json`            TEXT         DEFAULT NULL,
    `created_at`               DATETIME     DEFAULT CURRENT_TIMESTAMP,
    `updated_at`               DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_page_registry` (`project_code`, `page_key`, `origin`),
    KEY `idx_page_registry_project` (`project_code`, `status`, `last_seen_at`),
    KEY `idx_page_registry_instance` (`current_page_instance_id`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='业务前端页面注册目录';

CREATE TABLE IF NOT EXISTS `eaf_page_action_registry` (
    `id`                     BIGINT       NOT NULL AUTO_INCREMENT,
    `project_code`           VARCHAR(96)  NOT NULL,
    `app_id`                 VARCHAR(96)  NOT NULL,
    `page_key`               VARCHAR(160) NOT NULL,
    `action_key`             VARCHAR(160) NOT NULL,
    `title`                  VARCHAR(160) NOT NULL,
    `description`            VARCHAR(512) DEFAULT NULL,
    `confirm_required`       TINYINT(1)   DEFAULT 0,
    `input_schema_json`      TEXT         DEFAULT NULL,
    `output_schema_json`     TEXT         DEFAULT NULL,
    `sample_args_json`       TEXT         DEFAULT NULL,
    `allowed_agent_ids_json` TEXT         DEFAULT NULL,
    `metadata_json`          TEXT         DEFAULT NULL,
    `status`                 VARCHAR(24)  NOT NULL DEFAULT 'ACTIVE',
    `last_seen_at`           DATETIME     DEFAULT NULL,
    `created_at`             DATETIME     DEFAULT CURRENT_TIMESTAMP,
    `updated_at`             DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_page_action_registry` (`project_code`, `page_key`, `action_key`),
    KEY `idx_page_action_registry_project` (`project_code`, `status`, `last_seen_at`),
    KEY `idx_page_action_registry_page` (`project_code`, `page_key`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='业务前端页面动作注册目录';
