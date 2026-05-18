-- Agent platform unified execution trace spans.
-- Safe to run repeatedly on existing environments.

CREATE TABLE IF NOT EXISTS `agent_trace_span` (
    `id`                BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `trace_id`          VARCHAR(64)  NOT NULL                COMMENT '一次 Agent 执行的 trace id',
    `span_id`           VARCHAR(64)  NOT NULL                COMMENT 'Span ID',
    `parent_span_id`    VARCHAR(64)  DEFAULT NULL            COMMENT '父 Span ID',
    `span_type`         VARCHAR(32)  NOT NULL                COMMENT 'AGENT_RUN/LLM_CALL/TOOL_CALL 等',
    `runtime_type`      VARCHAR(32)  DEFAULT NULL            COMMENT 'Runtime 类型',
    `agent_id`          VARCHAR(64)  DEFAULT NULL            COMMENT 'Agent ID',
    `agent_name`        VARCHAR(128) DEFAULT NULL            COMMENT 'Agent 名称',
    `node_id`           VARCHAR(128) DEFAULT NULL            COMMENT 'GraphSpec 节点 ID',
    `tool_name`         VARCHAR(256) DEFAULT NULL            COMMENT 'Tool/Skill 名称',
    `model_instance_id` VARCHAR(64)  DEFAULT NULL            COMMENT '模型实例 ID',
    `project_code`      VARCHAR(96)  DEFAULT NULL            COMMENT '项目编码',
    `status`            VARCHAR(16)  NOT NULL DEFAULT 'SUCCESS' COMMENT 'SUCCESS/ERROR',
    `input_summary`     MEDIUMTEXT   DEFAULT NULL            COMMENT '输入摘要',
    `output_summary`    MEDIUMTEXT   DEFAULT NULL            COMMENT '输出摘要',
    `metadata_json`     MEDIUMTEXT   DEFAULT NULL            COMMENT '结构化元数据 JSON',
    `error_code`        VARCHAR(128) DEFAULT NULL            COMMENT '错误码',
    `error_message`     TEXT         DEFAULT NULL            COMMENT '错误信息',
    `latency_ms`        INT          DEFAULT NULL            COMMENT '耗时毫秒',
    `token_cost`        INT          DEFAULT NULL            COMMENT 'Token 消耗',
    `started_at`        DATETIME     DEFAULT NULL            COMMENT '开始时间',
    `ended_at`          DATETIME     DEFAULT NULL            COMMENT '结束时间',
    `created_at`        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_trace_span` (`trace_id`, `span_id`),
    KEY `idx_trace` (`trace_id`, `created_at`),
    KEY `idx_parent` (`trace_id`, `parent_span_id`),
    KEY `idx_agent_node` (`agent_id`, `node_id`, `created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Agent 平台统一执行 Trace Span';

