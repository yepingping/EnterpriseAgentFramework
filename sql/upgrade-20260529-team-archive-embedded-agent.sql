-- Seed the embedded chat Agent used by qmssmp team archive page.
-- It is intentionally model-free: deterministic extraction + PAGE_ACTION.

INSERT INTO `agent_definition`
(`id`, `key_slug`, `name`, `description`, `agent_mode`, `project_code`, `visibility`,
 `allowed_roles_json`, `intent_type`, `system_prompt`, `tools_json`, `skills_json`,
 `model_instance_id`, `runtime_type`, `runtime_placement`, `runtime_config_json`,
 `default_resource_config_json`, `graph_spec_json`, `max_steps`, `type`,
 `pipeline_agent_ids_json`, `knowledge_base_group_id`, `prompt_template_id`,
 `output_schema_type`, `trigger_mode`, `use_multi_agent_model`, `extra_json`,
 `canvas_json`, `enabled`, `allow_irreversible`, `created_at`, `updated_at`)
SELECT
    'team_archive_agent',
    'team-archive-assistant',
    '班组档案助手',
    '面向班组档案页面的嵌入式对话助手，将自然语言查询转换为页面筛选动作。',
    'WORKFLOW',
    'qmssmp-teams-construction-service',
    'PROJECT',
    '[]',
    'TEAM_ARCHIVE_PAGE_QUERY',
    '你是班组档案页面助手，只把用户查询意图转换为页面筛选条件，并触发页面查询。',
    '[]',
    '[]',
    NULL,
    'LANGGRAPH4J',
    'CENTRAL',
    NULL,
    NULL,
    JSON_OBJECT(
        'code', 'team-archive-assistant',
        'name', '班组档案助手',
        'mode', 'WORKFLOW',
        'runtimeHint', 'LANGGRAPH4J',
        'entry', 'extract_filters',
        'finish', JSON_ARRAY('apply_search'),
        'layout', JSON_OBJECT('engine', 'seed', 'direction', 'LR'),
        'nodes', JSON_ARRAY(
            JSON_OBJECT(
                'id', 'extract_filters',
                'type', 'DOCUMENT_EXTRACT',
                'name', '抽取班组筛选条件',
                'config', JSON_OBJECT(
                    'sourceExpression', 'input',
                    'outputAlias', 'filters',
                    'fields', JSON_ARRAY(
                        JSON_OBJECT('name', 'managerName', 'type', 'STRING', 'source', 'regex:(?:负责人|负责人姓名)(?:为|是|叫|=|：|:)?[ ]*([^，。,. 的]+)'),
                        JSON_OBJECT('name', 'teamName', 'type', 'STRING', 'source', 'regex:(?:班组名称|班组名|班组)(?:为|是|叫|=|：|:)[ ]*([^，。,. 的]+)'),
                        JSON_OBJECT('name', 'memberName', 'type', 'STRING', 'source', 'regex:(?:班组成员|成员)(?:包含|包括|有|为|是|叫|=|：|:)?[ ]*([^，。,. 的]+)')
                    )
                )
            ),
            JSON_OBJECT(
                'id', 'apply_search',
                'type', 'PAGE_ACTION',
                'name', '执行班组档案页面查询',
                'config', JSON_OBJECT(
                    'actionKey', 'qmssmp.teamArchive.search',
                    'title', '已按你的条件查询班组档案',
                    'confirm', false,
                    'args', JSON_OBJECT(
                        'managerName', 'filters.managerName',
                        'teamName', 'filters.teamName',
                        'memberName', 'filters.memberName'
                    )
                )
            )
        ),
        'edges', JSON_ARRAY(
            JSON_OBJECT('id', 'start_extract', 'from', 'START', 'to', 'extract_filters', 'condition', 'always'),
            JSON_OBJECT('id', 'extract_search', 'from', 'extract_filters', 'to', 'apply_search', 'condition', 'always'),
            JSON_OBJECT('id', 'search_end', 'from', 'apply_search', 'to', 'END', 'condition', 'always')
        )
    ),
    3,
    'single',
    '[]',
    NULL,
    NULL,
    NULL,
    'all',
    0,
    JSON_OBJECT('source', 'seed', 'scenario', 'qmssmp-team-archive-embedded-chat'),
    NULL,
    1,
    0,
    NOW(),
    NOW()
WHERE NOT EXISTS (
    SELECT 1 FROM `agent_definition` WHERE `key_slug` = 'team-archive-assistant'
);
