-- ============================================================================
-- Tool 语义检索：持久化「重建向量索引」选用的 Embedding 模型实例 ID
-- 与 sql/init.sql 七.六节一致；已建库可单独执行。
-- 执行：mysql -uroot -p ai_text_service < ai-agent-service/sql/tool_retrieval_setting.sql
-- ============================================================================

USE `ai_text_service`;

CREATE TABLE IF NOT EXISTS `tool_retrieval_setting` (
    `id`                            CHAR(1)      NOT NULL DEFAULT '1' COMMENT '固定单例行',
    `embedding_model_instance_id`   VARCHAR(64)  DEFAULT NULL COMMENT '上次重建 Tool 向量索引选用的模型实例；对话侧语义召回与用户问题向量化共用',
    `updated_at`                    DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Tool 语义检索全局运行时设置（单例）';

INSERT IGNORE INTO `tool_retrieval_setting` (`id`, `embedding_model_instance_id`) VALUES ('1', NULL);
