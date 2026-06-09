-- Normalize page registry origin and remove duplicate page rows created by NULL origin.
-- Existing duplicates keep the most recently reported row per project_code/page_key/origin.

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

DELETE older
FROM `eaf_page_registry` older
JOIN `eaf_page_registry` newer
  ON older.`project_code` = newer.`project_code`
 AND older.`page_key` = newer.`page_key`
 AND COALESCE(older.`origin`, '') = COALESCE(newer.`origin`, '')
 AND (
      COALESCE(older.`last_seen_at`, older.`updated_at`, older.`created_at`, '1970-01-01') <
      COALESCE(newer.`last_seen_at`, newer.`updated_at`, newer.`created_at`, '1970-01-01')
      OR (
          COALESCE(older.`last_seen_at`, older.`updated_at`, older.`created_at`, '1970-01-01') =
          COALESCE(newer.`last_seen_at`, newer.`updated_at`, newer.`created_at`, '1970-01-01')
          AND older.`id` < newer.`id`
      )
 );

UPDATE `eaf_page_registry`
SET `origin` = ''
WHERE `origin` IS NULL;

ALTER TABLE `eaf_page_registry`
    MODIFY COLUMN `origin` VARCHAR(512) NOT NULL DEFAULT '';
