USE `ai_text_service`;

CREATE TABLE IF NOT EXISTS `agent_release_event` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `agent_id` VARCHAR(32) NOT NULL,
    `version_id` BIGINT DEFAULT NULL,
    `version` VARCHAR(32) DEFAULT NULL,
    `action` VARCHAR(24) NOT NULL,
    `decision` VARCHAR(24) NOT NULL,
    `rollout_percent` INT DEFAULT NULL,
    `operator` VARCHAR(64) DEFAULT NULL,
    `summary` VARCHAR(512) DEFAULT NULL,
    `validation_json` MEDIUMTEXT DEFAULT NULL,
    `metadata_json` TEXT DEFAULT NULL,
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_agent_release_event_agent` (`agent_id`, `id`),
    KEY `idx_agent_release_event_version` (`version_id`),
    KEY `idx_agent_release_event_action` (`agent_id`, `action`, `decision`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
