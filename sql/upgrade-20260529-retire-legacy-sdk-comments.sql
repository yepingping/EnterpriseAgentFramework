-- ============================================================================
-- ReachAI 旧 SDK / Starter 退役后的字段注释对齐
-- 背景：capability_metadata_json 仍保留字段名，但语义已从旧 @AiCapability
--      切换为新 JDK8 接入 SDK 的 @ReachCapability。
-- ============================================================================

USE `ai_text_service`;

ALTER TABLE `scan_project_tool`
    MODIFY COLUMN `capability_metadata_json` MEDIUMTEXT DEFAULT NULL COMMENT '@ReachCapability 能力声明元数据 JSON';

ALTER TABLE `tool_definition`
    MODIFY COLUMN `capability_metadata_json` MEDIUMTEXT DEFAULT NULL COMMENT '@ReachCapability 能力声明元数据 JSON';
