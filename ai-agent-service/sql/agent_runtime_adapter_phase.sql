-- ============================================================================
-- Agent Runtime Adapter incremental upgrade
--
-- Apply when upgrading an existing database to the Runtime Adapter phase.
--
-- What this script changes:
--   1. Adds agent_definition.runtime_type if it does not exist.
--   1a. Adds agent_definition.runtime_placement if it does not exist.
--   2. Adds agent_definition.runtime_config_json if it does not exist.
--   2a. Adds agent_definition.graph_spec_json if it does not exist.
--   3. Backfills existing agents to AGENTSCOPE, preserving the old AgentScope
--      behavior as the default runtime.
--   4. Adds idx_agent_runtime(runtime_type, enabled) for runtime management
--      and filtering.
--
-- Safe to rerun: yes.
-- Prerequisite: agent_definition table already exists.
--
-- Notes:
--   This script intentionally does not use stored procedures or DELIMITER,
--   so it works better in GUI clients such as DBeaver/DataGrip/Navicat.
-- ============================================================================

USE `ai_text_service`;

SET @has_agent_definition = (
    SELECT COUNT(*)
    FROM information_schema.tables
    WHERE table_schema = DATABASE()
      AND table_name = 'agent_definition'
);

SET @has_runtime_type = (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'agent_definition'
      AND column_name = 'runtime_type'
);

SET @has_model_instance_id = (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'agent_definition'
      AND column_name = 'model_instance_id'
);

SET @has_runtime_config = (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'agent_definition'
      AND column_name = 'runtime_config_json'
);

SET @has_runtime_placement = (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'agent_definition'
      AND column_name = 'runtime_placement'
);

SET @has_graph_spec = (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'agent_definition'
      AND column_name = 'graph_spec_json'
);

SET @sql = CASE
    WHEN @has_agent_definition = 0 THEN
        'SELECT ''Required table agent_definition does not exist. Run agent_studio_phase3_0.sql first.'' AS upgrade_error'
    WHEN @has_runtime_type > 0 THEN
        'SELECT ''agent_definition.runtime_type already exists, skip add column.'' AS upgrade_info'
    WHEN @has_model_instance_id > 0 THEN
        'ALTER TABLE `agent_definition` ADD COLUMN `runtime_type` VARCHAR(32) NOT NULL DEFAULT ''AGENTSCOPE'' COMMENT ''Agent Runtime type: AGENTSCOPE / LANGGRAPH4J / OPENAI_AGENTS / CURSOR_CODE_AGENT'' AFTER `model_instance_id`'
    ELSE
        'ALTER TABLE `agent_definition` ADD COLUMN `runtime_type` VARCHAR(32) NOT NULL DEFAULT ''AGENTSCOPE'' COMMENT ''Agent Runtime type: AGENTSCOPE / LANGGRAPH4J / OPENAI_AGENTS / CURSOR_CODE_AGENT'''
END;

PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @has_runtime_type = (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'agent_definition'
      AND column_name = 'runtime_type'
);

SET @sql = CASE
    WHEN @has_agent_definition = 0 THEN
        'SELECT ''Required table agent_definition does not exist. Run agent_studio_phase3_0.sql first.'' AS upgrade_error'
    WHEN @has_runtime_placement > 0 THEN
        'SELECT ''agent_definition.runtime_placement already exists, skip add column.'' AS upgrade_info'
    WHEN @has_runtime_type > 0 THEN
        'ALTER TABLE `agent_definition` ADD COLUMN `runtime_placement` VARCHAR(24) NOT NULL DEFAULT ''CENTRAL'' COMMENT ''Runtime placement: CENTRAL / EMBEDDED / HYBRID'' AFTER `runtime_type`'
    ELSE
        'ALTER TABLE `agent_definition` ADD COLUMN `runtime_placement` VARCHAR(24) NOT NULL DEFAULT ''CENTRAL'' COMMENT ''Runtime placement: CENTRAL / EMBEDDED / HYBRID'''
END;

PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @has_runtime_placement = (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'agent_definition'
      AND column_name = 'runtime_placement'
);

SET @sql = CASE
    WHEN @has_agent_definition = 0 THEN
        'SELECT ''Required table agent_definition does not exist. Run agent_studio_phase3_0.sql first.'' AS upgrade_error'
    WHEN @has_runtime_config > 0 THEN
        'SELECT ''agent_definition.runtime_config_json already exists, skip add column.'' AS upgrade_info'
    WHEN @has_runtime_placement > 0 THEN
        'ALTER TABLE `agent_definition` ADD COLUMN `runtime_config_json` MEDIUMTEXT DEFAULT NULL COMMENT ''Runtime specific config JSON'' AFTER `runtime_placement`'
    WHEN @has_runtime_type > 0 THEN
        'ALTER TABLE `agent_definition` ADD COLUMN `runtime_config_json` MEDIUMTEXT DEFAULT NULL COMMENT ''Runtime specific config JSON'' AFTER `runtime_type`'
    ELSE
        'ALTER TABLE `agent_definition` ADD COLUMN `runtime_config_json` MEDIUMTEXT DEFAULT NULL COMMENT ''Runtime specific config JSON'''
END;

PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @has_runtime_config = (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'agent_definition'
      AND column_name = 'runtime_config_json'
);

SET @sql = CASE
    WHEN @has_agent_definition = 0 THEN
        'SELECT ''Required table agent_definition does not exist. Run agent_studio_phase3_0.sql first.'' AS upgrade_error'
    WHEN @has_graph_spec > 0 THEN
        'SELECT ''agent_definition.graph_spec_json already exists, skip add column.'' AS upgrade_info'
    WHEN @has_runtime_config > 0 THEN
        'ALTER TABLE `agent_definition` ADD COLUMN `graph_spec_json` MEDIUMTEXT DEFAULT NULL COMMENT ''Platform GraphSpec JSON'' AFTER `runtime_config_json`'
    ELSE
        'ALTER TABLE `agent_definition` ADD COLUMN `graph_spec_json` MEDIUMTEXT DEFAULT NULL COMMENT ''Platform GraphSpec JSON'''
END;

PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @has_runtime_type = (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'agent_definition'
      AND column_name = 'runtime_type'
);

SET @sql = CASE
    WHEN @has_agent_definition = 0 OR @has_runtime_type = 0 THEN
        'SELECT ''Skip runtime_type normalization because agent_definition.runtime_type is missing.'' AS upgrade_info'
    ELSE
        'ALTER TABLE `agent_definition` MODIFY COLUMN `runtime_type` VARCHAR(32) NOT NULL DEFAULT ''AGENTSCOPE'' COMMENT ''Agent Runtime type: AGENTSCOPE / LANGGRAPH4J / OPENAI_AGENTS / CURSOR_CODE_AGENT'''
END;

PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = CASE
    WHEN @has_agent_definition = 0 OR @has_runtime_type = 0 THEN
        'SELECT ''Skip runtime_type backfill because agent_definition.runtime_type is missing.'' AS upgrade_info'
    ELSE
        'UPDATE `agent_definition` SET `runtime_type` = ''AGENTSCOPE'' WHERE `runtime_type` IS NULL OR TRIM(`runtime_type`) = '''''
END;

PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @has_runtime_placement = (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'agent_definition'
      AND column_name = 'runtime_placement'
);

SET @sql = CASE
    WHEN @has_agent_definition = 0 OR @has_runtime_placement = 0 THEN
        'SELECT ''Skip runtime_placement normalization because agent_definition.runtime_placement is missing.'' AS upgrade_info'
    ELSE
        'UPDATE `agent_definition` SET `runtime_placement` = ''CENTRAL'' WHERE `runtime_placement` IS NULL OR TRIM(`runtime_placement`) = '''''
END;

PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @has_agent_runtime_index = (
    SELECT COUNT(*)
    FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'agent_definition'
      AND index_name = 'idx_agent_runtime'
);

SET @sql = CASE
    WHEN @has_agent_definition = 0 OR @has_runtime_type = 0 THEN
        'SELECT ''Skip idx_agent_runtime because agent_definition.runtime_type is missing.'' AS upgrade_info'
    WHEN @has_agent_runtime_index > 0 THEN
        'SELECT ''idx_agent_runtime already exists, skip create index.'' AS upgrade_info'
    ELSE
        'CREATE INDEX `idx_agent_runtime` ON `agent_definition` (`runtime_type`, `enabled`)'
END;

PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @has_agent_runtime_placement_index = (
    SELECT COUNT(*)
    FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'agent_definition'
      AND index_name = 'idx_agent_runtime_placement'
);

SET @sql = CASE
    WHEN @has_agent_definition = 0 OR @has_runtime_placement = 0 THEN
        'SELECT ''Skip idx_agent_runtime_placement because agent_definition.runtime_placement is missing.'' AS upgrade_info'
    WHEN @has_agent_runtime_placement_index > 0 THEN
        'SELECT ''idx_agent_runtime_placement already exists, skip create index.'' AS upgrade_info'
    ELSE
        'CREATE INDEX `idx_agent_runtime_placement` ON `agent_definition` (`runtime_placement`, `enabled`)'
END;

PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Verification queries. Keep these SELECTs: they make manual SQL-console upgrades
-- easier to inspect without changing data.
SELECT
    column_name,
    column_type,
    is_nullable,
    column_default
FROM information_schema.columns
WHERE table_schema = DATABASE()
  AND table_name = 'agent_definition'
  AND column_name IN ('runtime_type', 'runtime_placement', 'runtime_config_json', 'graph_spec_json')
ORDER BY column_name;

SELECT
    index_name,
    GROUP_CONCAT(column_name ORDER BY seq_in_index) AS columns_in_index
FROM information_schema.statistics
WHERE table_schema = DATABASE()
  AND table_name = 'agent_definition'
  AND index_name IN ('idx_agent_runtime', 'idx_agent_runtime_placement')
GROUP BY index_name;

SELECT
    runtime_type,
    runtime_placement,
    COUNT(*) AS agent_count
FROM `agent_definition`
GROUP BY runtime_type, runtime_placement
ORDER BY runtime_type, runtime_placement;
