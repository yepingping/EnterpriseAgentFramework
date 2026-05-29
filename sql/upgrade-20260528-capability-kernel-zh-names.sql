-- 能力内核种子数据展示名称中文化（已有环境升级）
UPDATE `capability_module`
SET `name` = '系统内置能力',
    `manifest_json` = '{"code":"system","name":"系统内置能力"}'
WHERE `code` = 'system';

UPDATE `tool_asset` SET `name` = '回声工具', `description` = '用于能力内核验证的回声工具'
WHERE `qualified_name` = 'system.echo';

UPDATE `composition_definition` SET `name` = '回声组合', `description` = '用于能力内核验证的最小图组合'
WHERE `qualified_name` = 'system.echo_flow';

UPDATE `composition_definition` SET `name` = '交互式回声组合', `description` = '用于验证交互内核的示例组合'
WHERE `qualified_name` = 'system.interactive_echo';

UPDATE `interaction_definition` SET `name` = '回声输入交互', `description` = '调用回声工具前采集用户输入'
WHERE `qualified_name` = 'system.echo_input';

UPDATE `interaction_definition` SET `name` = '回声结果展示', `description` = '以详情卡片展示回声结果'
WHERE `qualified_name` = 'system.echo_result';
