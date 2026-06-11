-- Add project-level AI Coding access key for ReachAI onboarding manifest access.
-- Existing projects start disabled until a user saves a key in the console.

CALL add_col_if_absent('scan_project', 'ai_coding_access_key',
  'VARCHAR(160) DEFAULT NULL COMMENT ''AI Coding 接入秘钥'' AFTER `last_scanned_at`');

CALL add_col_if_absent('scan_project', 'ai_coding_access_enabled',
  'TINYINT NOT NULL DEFAULT 0 COMMENT ''1=允许 AI Coding 工具使用秘钥读取接入 manifest'' AFTER `ai_coding_access_key`');
