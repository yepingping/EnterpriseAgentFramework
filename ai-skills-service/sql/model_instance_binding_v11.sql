-- Model instance binding upgrade for knowledge bases and business indexes.
-- Run against ai_text_service.

USE `ai_text_service`;

DROP PROCEDURE IF EXISTS add_col_if_absent;
DROP PROCEDURE IF EXISTS add_idx_if_absent;

DELIMITER $$

CREATE PROCEDURE add_col_if_absent(
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

CREATE PROCEDURE add_idx_if_absent(
    IN p_table VARCHAR(64),
    IN p_index VARCHAR(64),
    IN p_columns VARCHAR(255)
)
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.statistics
        WHERE table_schema = DATABASE()
          AND table_name = p_table
          AND index_name = p_index
    ) THEN
        SET @sql = CONCAT('CREATE INDEX `', p_index, '` ON `', p_table, '` (', p_columns, ')');
        PREPARE stmt FROM @sql;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;
END$$

DELIMITER ;

CALL add_col_if_absent('knowledge_base', 'embedding_model_instance_id',
    'VARCHAR(64) DEFAULT NULL COMMENT ''Embedding model instance id'' AFTER `embedding_model`');
CALL add_col_if_absent('knowledge_base', 'rerank_model_instance_id',
    'VARCHAR(64) DEFAULT NULL COMMENT ''Rerank model instance id'' AFTER `embedding_model_instance_id`');
CALL add_col_if_absent('business_index', 'embedding_model_instance_id',
    'VARCHAR(64) DEFAULT NULL COMMENT ''Embedding model instance id'' AFTER `embedding_model`');

CALL add_idx_if_absent('knowledge_base', 'idx_kb_embedding_instance', '`embedding_model_instance_id`');
CALL add_idx_if_absent('knowledge_base', 'idx_kb_rerank_instance', '`rerank_model_instance_id`');
CALL add_idx_if_absent('business_index', 'idx_biz_embedding_instance', '`embedding_model_instance_id`');

DROP PROCEDURE IF EXISTS add_col_if_absent;
DROP PROCEDURE IF EXISTS add_idx_if_absent;
