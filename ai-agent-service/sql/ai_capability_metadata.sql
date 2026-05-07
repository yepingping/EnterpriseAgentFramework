USE `ai_text_service`;

DROP PROCEDURE IF EXISTS add_col_if_absent_ai_capability;

DELIMITER $$

CREATE PROCEDURE add_col_if_absent_ai_capability(
    IN p_table VARCHAR(64),
    IN p_column VARCHAR(64),
    IN p_definition TEXT
)
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = DATABASE()
          AND table_name = p_table
          AND column_name = p_column
    ) THEN
        SET @sql = CONCAT('ALTER TABLE `', p_table, '` ADD COLUMN `', p_column, '` ', p_definition);
        PREPARE stmt FROM @sql;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;
END$$

DELIMITER ;

CALL add_col_if_absent_ai_capability(
    'scan_project_tool',
    'capability_metadata_json',
    'MEDIUMTEXT DEFAULT NULL COMMENT ''@AiCapability 能力声明元数据 JSON'' AFTER `ai_description`'
);

CALL add_col_if_absent_ai_capability(
    'tool_definition',
    'capability_metadata_json',
    'MEDIUMTEXT DEFAULT NULL COMMENT ''@AiCapability 能力声明元数据 JSON'' AFTER `ai_description`'
);

DROP PROCEDURE IF EXISTS add_col_if_absent_ai_capability;
