-- v12: remove legacy embedding model string columns after modelInstanceId rollout.
-- Best effort: when ai_model_instance exists in the same schema, bind old model names
-- to matching EMBEDDING model instances before dropping the old columns.

DELIMITER $$

DROP PROCEDURE IF EXISTS drop_col_if_exists $$
CREATE PROCEDURE drop_col_if_exists(IN p_table VARCHAR(64), IN p_col VARCHAR(64))
BEGIN
    IF EXISTS (
        SELECT 1
        FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = p_table
          AND COLUMN_NAME = p_col
    ) THEN
        SET @ddl = CONCAT('ALTER TABLE `', p_table, '` DROP COLUMN `', p_col, '`');
        PREPARE stmt FROM @ddl;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;
END $$

DROP PROCEDURE IF EXISTS bind_embedding_instance_from_legacy $$
CREATE PROCEDURE bind_embedding_instance_from_legacy(IN p_table VARCHAR(64))
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.TABLES
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'ai_model_instance'
    ) AND EXISTS (
        SELECT 1 FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = p_table
          AND COLUMN_NAME = 'embedding_model'
    ) AND EXISTS (
        SELECT 1 FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = p_table
          AND COLUMN_NAME = 'embedding_model_instance_id'
    ) THEN
        SET @ddl = CONCAT(
            'UPDATE `', p_table, '` t ',
            'JOIN `ai_model_instance` mi ',
            '  ON mi.model_type = ''EMBEDDING'' AND mi.model_name = t.embedding_model ',
            'SET t.embedding_model_instance_id = mi.id ',
            'WHERE (t.embedding_model_instance_id IS NULL OR t.embedding_model_instance_id = '''')'
        );
        PREPARE stmt FROM @ddl;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;
END $$

DELIMITER ;

CALL bind_embedding_instance_from_legacy('knowledge_base');
CALL bind_embedding_instance_from_legacy('business_index');
CALL drop_col_if_exists('knowledge_base', 'embedding_model');
CALL drop_col_if_exists('business_index', 'embedding_model');

DROP PROCEDURE IF EXISTS drop_col_if_exists;
DROP PROCEDURE IF EXISTS bind_embedding_instance_from_legacy;
