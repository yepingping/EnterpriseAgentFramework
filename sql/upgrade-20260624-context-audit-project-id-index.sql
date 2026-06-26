-- ReachAI Context Governance audit project_id index hardening.
-- Existing dev/test databases can execute this script after the Context Kernel upgrades.
-- Adds the project_id audit index used by projectId-only governance filters.

DROP PROCEDURE IF EXISTS add_idx_if_absent;

DELIMITER $$

CREATE PROCEDURE add_idx_if_absent(
    IN p_table VARCHAR(64),
    IN p_index VARCHAR(64),
    IN p_columns VARCHAR(255)
)
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.statistics
        WHERE table_schema = DATABASE()
          AND table_name   = p_table
          AND index_name   = p_index
    ) THEN
        SET @sql = CONCAT('CREATE INDEX `', p_index, '` ON `', p_table, '` (', p_columns, ')');
        PREPARE stmt FROM @sql;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;
END$$

DELIMITER ;

CALL add_idx_if_absent('context_audit_event', 'idx_context_audit_project_id', '`project_id`, `created_at`');

DROP PROCEDURE IF EXISTS add_idx_if_absent;
