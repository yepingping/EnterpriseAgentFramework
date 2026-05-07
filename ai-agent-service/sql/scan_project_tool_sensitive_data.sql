-- 扫描接口敏感数据（LLM）结果，JSON：types / summary / scannedAt / modelName 等
ALTER TABLE `scan_project_tool`
    ADD COLUMN `sensitive_data_json` TEXT NULL COMMENT '敏感数据扫描结果 JSON' AFTER `ai_description`;
