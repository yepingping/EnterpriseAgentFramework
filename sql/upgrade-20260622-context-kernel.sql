-- ReachAI Context Governance Kernel v1
-- Existing dev/test databases can execute this script once; statements are idempotent.
-- Project Dev Memory and Runtime User Memory are logically isolated via memory_lane.
-- FULLTEXT index is intentionally omitted (MySQL 5.7/8 Chinese tokenization differs; phase-1 uses LIKE search).

CREATE TABLE IF NOT EXISTS `context_namespace` (
    `id`              BIGINT       NOT NULL AUTO_INCREMENT,
    `namespace_key`   VARCHAR(128) NOT NULL,
    `namespace_type`  VARCHAR(32)  NOT NULL COMMENT 'PERSONAL/PROJECT/MODULE/FEATURE/PAGE/API/WORKFLOW/AGENT/USER/TENANT/SESSION/GLOBAL',
    `tenant_id`       VARCHAR(96)  DEFAULT NULL,
    `project_id`      BIGINT       DEFAULT NULL,
    `project_code`    VARCHAR(96)  DEFAULT NULL,
    `owner_type`      VARCHAR(32)  DEFAULT NULL,
    `owner_id`        VARCHAR(128) DEFAULT NULL,
    `display_name`    VARCHAR(128) DEFAULT NULL,
    `description`     VARCHAR(512) DEFAULT NULL,
    `status`          VARCHAR(24)  NOT NULL DEFAULT 'ACTIVE',
    `created_by`      VARCHAR(128) DEFAULT NULL,
    `created_at`      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted_at`      DATETIME     DEFAULT NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_context_namespace_key` (`namespace_key`),
    KEY `idx_context_namespace_scope` (`tenant_id`, `project_code`, `namespace_type`, `status`),
    KEY `idx_context_namespace_owner` (`owner_type`, `owner_id`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='上下文隔离命名空间';

CREATE TABLE IF NOT EXISTS `context_item` (
    `id`               BIGINT        NOT NULL AUTO_INCREMENT,
    `item_key`         VARCHAR(128)  NOT NULL,
    `namespace_id`     BIGINT        NOT NULL,
    `item_type`        VARCHAR(32)   NOT NULL COMMENT 'FACT/PREFERENCE/RULE/DECISION/PITFALL/PAGE_CONTEXT/API_CONTRACT/WORKFLOW_CONTEXT/SESSION_SUMMARY/TRACE_LEARNING/NOTE',
    `memory_lane`      VARCHAR(32)   NOT NULL DEFAULT 'PROJECT_DEV' COMMENT 'PROJECT_DEV or RUNTIME_USER',
    `title`            VARCHAR(256)  DEFAULT NULL,
    `content`          MEDIUMTEXT    NOT NULL,
    `summary`          TEXT          DEFAULT NULL,
    `metadata_json`    MEDIUMTEXT    DEFAULT NULL,
    `source_type`      VARCHAR(48)   NOT NULL COMMENT 'USER_CONFIRMED/USER_MESSAGE/AGENT_OUTPUT/CODE/SQL/DOC/API/TRACE/PAGE/WORKFLOW/SYSTEM/MANUAL',
    `source_ref`       VARCHAR(512)  DEFAULT NULL,
    `confidence`       DECIMAL(5,4)  NOT NULL DEFAULT 0.7000,
    `trust_level`      VARCHAR(24)   NOT NULL DEFAULT 'MEDIUM' COMMENT 'LOW/MEDIUM/HIGH/VERIFIED',
    `visibility`       VARCHAR(32)   NOT NULL DEFAULT 'PRIVATE' COMMENT 'PRIVATE/PROJECT/TENANT/GLOBAL',
    `status`           VARCHAR(24)   NOT NULL DEFAULT 'ACTIVE' COMMENT 'ACTIVE/STALE/REVOKED/DELETED',
    `effective_from`   DATETIME      DEFAULT NULL,
    `expires_at`       DATETIME      DEFAULT NULL,
    `last_verified_at` DATETIME      DEFAULT NULL,
    `stale_after`      DATETIME      DEFAULT NULL,
    `created_by`       VARCHAR(128)  DEFAULT NULL,
    `updated_by`       VARCHAR(128)  DEFAULT NULL,
    `created_at`       DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`       DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted_at`       DATETIME      DEFAULT NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_context_item_key` (`item_key`),
    KEY `idx_context_item_namespace` (`namespace_id`, `status`, `item_type`),
    KEY `idx_context_item_lane` (`memory_lane`, `status`, `updated_at`),
    KEY `idx_context_item_expiry` (`status`, `expires_at`),
    KEY `idx_context_item_trust` (`trust_level`, `confidence`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='上下文资产主表';

CREATE TABLE IF NOT EXISTS `context_binding` (
    `id`           BIGINT       NOT NULL AUTO_INCREMENT,
    `item_id`      BIGINT       NOT NULL,
    `bind_type`    VARCHAR(32)  NOT NULL COMMENT 'TENANT/PROJECT/USER/AGENT/WORKFLOW/PAGE/API/SESSION/MODULE/FEATURE',
    `bind_id`      VARCHAR(128) NOT NULL,
    `bind_key`     VARCHAR(256) DEFAULT NULL,
    `tenant_id`    VARCHAR(96)  DEFAULT NULL,
    `project_id`   BIGINT       DEFAULT NULL,
    `project_code` VARCHAR(96)  DEFAULT NULL,
    `status`       VARCHAR(24)  NOT NULL DEFAULT 'ACTIVE',
    `created_at`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `deleted_at`   DATETIME     DEFAULT NULL,
    PRIMARY KEY (`id`),
    KEY `idx_context_binding_target` (`bind_type`, `bind_id`, `status`),
    KEY `idx_context_binding_item` (`item_id`, `status`),
    KEY `idx_context_binding_project` (`project_code`, `bind_type`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='上下文资产绑定关系';

CREATE TABLE IF NOT EXISTS `context_evidence` (
    `id`              BIGINT       NOT NULL AUTO_INCREMENT,
    `item_id`         BIGINT       NOT NULL,
    `evidence_type`   VARCHAR(48)  NOT NULL COMMENT 'USER_CONFIRMATION/SOURCE_FILE/SQL_SCHEMA/API_RESPONSE/TRACE_SPAN/TOOL_CALL/DOCUMENT/MANUAL_NOTE',
    `evidence_ref`    VARCHAR(512) DEFAULT NULL,
    `evidence_excerpt` TEXT        DEFAULT NULL,
    `trace_id`        VARCHAR(128) DEFAULT NULL,
    `confidence`      DECIMAL(5,4) DEFAULT NULL,
    `metadata_json`   MEDIUMTEXT   DEFAULT NULL,
    `created_at`      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_context_evidence_item` (`item_id`),
    KEY `idx_context_evidence_trace` (`trace_id`),
    KEY `idx_context_evidence_type` (`evidence_type`, `created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='上下文资产来源证据';

CREATE TABLE IF NOT EXISTS `context_audit_event` (
    `id`            BIGINT       NOT NULL AUTO_INCREMENT,
    `event_type`    VARCHAR(48)  NOT NULL COMMENT 'CREATE/UPDATE/READ/SEARCH/INJECT/DELETE/REVOKE/EXPIRE/VERIFY/MARK_STALE',
    `item_id`       BIGINT       DEFAULT NULL,
    `namespace_id`  BIGINT       DEFAULT NULL,
    `actor_type`    VARCHAR(32)  DEFAULT NULL,
    `actor_id`      VARCHAR(128) DEFAULT NULL,
    `tenant_id`     VARCHAR(96)  DEFAULT NULL,
    `project_id`    BIGINT       DEFAULT NULL,
    `project_code`  VARCHAR(96)  DEFAULT NULL,
    `agent_id`      VARCHAR(128) DEFAULT NULL,
    `workflow_id`   VARCHAR(128) DEFAULT NULL,
    `session_id`    VARCHAR(128) DEFAULT NULL,
    `trace_id`      VARCHAR(128) DEFAULT NULL,
    `decision`      VARCHAR(32)  DEFAULT NULL COMMENT 'ALLOW/DENY/SKIP',
    `reason`        VARCHAR(512) DEFAULT NULL,
    `metadata_json` MEDIUMTEXT   DEFAULT NULL,
    `created_at`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_context_audit_item` (`item_id`, `created_at`),
    KEY `idx_context_audit_actor` (`actor_type`, `actor_id`, `created_at`),
    KEY `idx_context_audit_trace` (`trace_id`),
    KEY `idx_context_audit_project` (`project_code`, `created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='上下文治理审计事件';
