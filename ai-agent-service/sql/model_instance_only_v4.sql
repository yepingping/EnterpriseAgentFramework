-- Model instance only upgrade for agent definitions.
-- Best effort: when ai_model_instance exists in the same schema, bind old model_name
-- to a matching LLM model instance before removing the free-form column.

DELIMITER //

DROP PROCEDURE IF EXISTS add_agent_col_if_absent//
CREATE PROCEDURE add_agent_col_if_absent(
    IN p_table VARCHAR(64),
    IN p_column VARCHAR(64),
    IN p_definition TEXT
)
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = DATABASE()
          AND table_name = p_table
          AND column_name = p_column
    ) THEN
        SET @ddl = CONCAT('ALTER TABLE `', p_table, '` ADD COLUMN `', p_column, '` ', p_definition);
        PREPARE stmt FROM @ddl;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;
END//

DROP PROCEDURE IF EXISTS drop_agent_col_if_exists//
CREATE PROCEDURE drop_agent_col_if_exists(
    IN p_table VARCHAR(64),
    IN p_column VARCHAR(64)
)
BEGIN
    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = DATABASE()
          AND table_name = p_table
          AND column_name = p_column
    ) THEN
        SET @ddl = CONCAT('ALTER TABLE `', p_table, '` DROP COLUMN `', p_column, '`');
        PREPARE stmt FROM @ddl;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;
END//

DROP PROCEDURE IF EXISTS bind_agent_model_instance_from_legacy//
CREATE PROCEDURE bind_agent_model_instance_from_legacy()
BEGIN
    IF EXISTS (
        SELECT 1
        FROM information_schema.tables
        WHERE table_schema = DATABASE()
          AND table_name = 'ai_model_instance'
    ) AND EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = DATABASE()
          AND table_name = 'agent_definition'
          AND column_name = 'model_name'
    ) THEN
        UPDATE `agent_definition` a
        JOIN `ai_model_instance` mi
          ON mi.model_type = 'LLM'
         AND mi.model_name = a.model_name
        SET a.model_instance_id = mi.id
        WHERE (a.model_instance_id IS NULL OR a.model_instance_id = '');
    END IF;
END//

DELIMITER ;

CALL add_agent_col_if_absent('agent_definition', 'model_instance_id',
    'VARCHAR(64) DEFAULT NULL COMMENT ''Model instance id'' AFTER `tools_json`');
CALL bind_agent_model_instance_from_legacy();
CALL drop_agent_col_if_exists('agent_definition', 'model_name');

DROP PROCEDURE IF EXISTS add_agent_col_if_absent;
DROP PROCEDURE IF EXISTS drop_agent_col_if_exists;
DROP PROCEDURE IF EXISTS bind_agent_model_instance_from_legacy;
