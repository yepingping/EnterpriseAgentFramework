-- v13: bind knowledge base answer generation to an explicit LLM model instance.

DELIMITER $$

CREATE PROCEDURE add_col_if_absent(IN tbl VARCHAR(64), IN col VARCHAR(64), IN ddl TEXT)
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = tbl
          AND COLUMN_NAME = col
    ) THEN
        SET @s = CONCAT('ALTER TABLE `', tbl, '` ADD COLUMN `', col, '` ', ddl);
        PREPARE stmt FROM @s;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;
END$$

CREATE PROCEDURE add_idx_if_absent(IN tbl VARCHAR(64), IN idx VARCHAR(64), IN cols TEXT)
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.STATISTICS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = tbl
          AND INDEX_NAME = idx
    ) THEN
        SET @s = CONCAT('ALTER TABLE `', tbl, '` ADD INDEX `', idx, '` (', cols, ')');
        PREPARE stmt FROM @s;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;
END$$

DELIMITER ;

CALL add_col_if_absent('knowledge_base', 'llm_model_instance_id',
    'VARCHAR(64) DEFAULT NULL COMMENT ''LLM model instance id for answer generation'' AFTER `rerank_model_instance_id`');
CALL add_idx_if_absent('knowledge_base', 'idx_kb_llm_instance', '`llm_model_instance_id`');

DROP PROCEDURE IF EXISTS add_col_if_absent;
DROP PROCEDURE IF EXISTS add_idx_if_absent;
