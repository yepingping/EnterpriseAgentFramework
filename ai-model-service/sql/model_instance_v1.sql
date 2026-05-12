-- ============================================================================
-- ai-model-service upgrade: database-backed model instance management
-- Version: v1
-- Scope:
--   - Add ai_model_instance table for configurable model provider/model assets.
--   - Credentials are stored as encrypted JSON by ai-model-service.
--   - This script is idempotent and can be executed independently.
-- ============================================================================

CREATE TABLE IF NOT EXISTS `ai_model_instance` (
    `id`                   VARCHAR(64)   NOT NULL PRIMARY KEY,
    `name`                 VARCHAR(128)  NOT NULL COMMENT 'Human-readable model instance name',
    `provider`             VARCHAR(64)   NOT NULL COMMENT 'Provider key, e.g. tongyi/openai/mimo/ollama',
    `model_type`           VARCHAR(32)   NOT NULL COMMENT 'LLM/EMBEDDING/RERANKER/STT/TTS/IMAGE/etc',
    `model_name`           VARCHAR(128)  NOT NULL COMMENT 'Upstream model name',
    `endpoint_type`        VARCHAR(32)   NOT NULL DEFAULT 'BUILT_IN' COMMENT 'BUILT_IN or OPENAI_COMPATIBLE',
    `workspace_id`         VARCHAR(64)   NOT NULL DEFAULT 'default' COMMENT 'Workspace isolation key',
    `credential_json`      MEDIUMTEXT    DEFAULT NULL COMMENT 'Encrypted credential JSON',
    `default_options_json` MEDIUMTEXT    DEFAULT NULL COMMENT 'Default runtime options JSON',
    `params_schema_json`   MEDIUMTEXT    DEFAULT NULL COMMENT 'UI parameter schema JSON',
    `status`               VARCHAR(32)   NOT NULL DEFAULT 'ACTIVE' COMMENT 'ACTIVE/DISABLED/ERROR',
    `remark`               VARCHAR(512)  DEFAULT NULL,
    `created_at`           DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`           DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY `uk_ai_model_instance_name_ws` (`name`, `workspace_id`),
    KEY `idx_ai_model_instance_provider` (`provider`),
    KEY `idx_ai_model_instance_type` (`model_type`),
    KEY `idx_ai_model_instance_workspace` (`workspace_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Database-backed model instances';

