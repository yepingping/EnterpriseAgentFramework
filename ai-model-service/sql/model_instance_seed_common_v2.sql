-- ============================================================================
-- ai-model-service seed: common model providers and display model instances
-- Version: v2
-- Scope:
--   - Curated seed data for the model center UI.
--   - Rows are DISABLED by default because credentials are placeholders.
--   - Edit credential_json/default_options_json and set status=ACTIVE before runtime use.
--   - credential_json may be raw JSON; ai-model-service encrypts it on later edits.
--   - Model names are a best-effort snapshot as of 2026-05-13.
-- ============================================================================

CREATE TABLE IF NOT EXISTS `ai_model_instance` (
    `id`                   VARCHAR(64)   NOT NULL PRIMARY KEY,
    `name`                 VARCHAR(128)  NOT NULL COMMENT 'Human-readable model instance name',
    `provider`             VARCHAR(64)   NOT NULL COMMENT 'Provider key, e.g. tongyi/openai/mimo/ollama',
    `model_type`           VARCHAR(32)   NOT NULL COMMENT 'LLM/EMBEDDING/RERANKER/STT/TTS/IMAGE/etc',
    `model_name`           VARCHAR(128)  NOT NULL COMMENT 'Upstream model name',
    `endpoint_type`        VARCHAR(32)   NOT NULL DEFAULT 'BUILT_IN' COMMENT 'BUILT_IN or OPENAI_COMPATIBLE',
    `workspace_id`         VARCHAR(64)   NOT NULL DEFAULT 'default' COMMENT 'Workspace isolation key',
    `credential_json`      MEDIUMTEXT    DEFAULT NULL COMMENT 'Encrypted credential JSON',
    `default_options_json` MEDIUMTEXT    DEFAULT NULL COMMENT 'Default runtime options JSON',
    `params_schema_json`   MEDIUMTEXT    DEFAULT NULL COMMENT 'UI parameter schema JSON',
    `status`               VARCHAR(32)   NOT NULL DEFAULT 'ACTIVE' COMMENT 'ACTIVE/DISABLED/ERROR',
    `remark`               VARCHAR(512)  DEFAULT NULL,
    `created_at`           DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`           DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY `uk_ai_model_instance_name_ws` (`name`, `workspace_id`),
    KEY `idx_ai_model_instance_provider` (`provider`),
    KEY `idx_ai_model_instance_type` (`model_type`),
    KEY `idx_ai_model_instance_workspace` (`workspace_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Database-backed model instances';

INSERT INTO `ai_model_instance`
(`id`, `name`, `provider`, `model_type`, `model_name`, `endpoint_type`, `workspace_id`,
 `credential_json`, `default_options_json`, `params_schema_json`, `status`, `remark`)
VALUES
-- OpenAI
('seed-openai-gpt-5-2', 'OpenAI GPT-5.2', 'openai', 'LLM', 'gpt-5.2', 'OPENAI_COMPATIBLE', 'default',
 '{"baseUrl":"https://api.openai.com/v1","apiKeyEnv":"OPENAI_API_KEY"}',
 '{"temperature":0.7,"max_tokens":4096}', '[]', 'DISABLED', 'OpenAI frontier LLM seed. Fill OPENAI_API_KEY and enable before use.'),
('seed-openai-gpt-4-1-mini', 'OpenAI GPT-4.1 Mini', 'openai', 'LLM', 'gpt-4.1-mini', 'OPENAI_COMPATIBLE', 'default',
 '{"baseUrl":"https://api.openai.com/v1","apiKeyEnv":"OPENAI_API_KEY"}',
 '{"temperature":0.7,"max_tokens":4096}', '[]', 'DISABLED', 'OpenAI small general LLM seed.'),
('seed-openai-embedding-3-large', 'OpenAI Embedding 3 Large', 'openai', 'EMBEDDING', 'text-embedding-3-large', 'OPENAI_COMPATIBLE', 'default',
 '{"baseUrl":"https://api.openai.com/v1","apiKeyEnv":"OPENAI_API_KEY"}',
 '{"dimensions":3072}', '[]', 'DISABLED', 'OpenAI high-quality text embedding seed.'),
('seed-openai-embedding-3-small', 'OpenAI Embedding 3 Small', 'openai', 'EMBEDDING', 'text-embedding-3-small', 'OPENAI_COMPATIBLE', 'default',
 '{"baseUrl":"https://api.openai.com/v1","apiKeyEnv":"OPENAI_API_KEY"}',
 '{"dimensions":1536}', '[]', 'DISABLED', 'OpenAI cost-effective text embedding seed.'),
('seed-openai-gpt-image-1', 'OpenAI Image Generation', 'openai', 'IMAGE_GENERATION', 'gpt-image-1', 'OPENAI_COMPATIBLE', 'default',
 '{"baseUrl":"https://api.openai.com/v1","apiKeyEnv":"OPENAI_API_KEY"}',
 '{"size":"1024x1024"}', '[]', 'DISABLED', 'OpenAI image generation seed.'),

-- Alibaba Cloud Model Studio / DashScope
('seed-tongyi-qwen3-max', '通义千问 Qwen3-Max', 'tongyi', 'LLM', 'qwen3-max', 'OPENAI_COMPATIBLE', 'default',
 '{"baseUrl":"https://dashscope.aliyuncs.com/compatible-mode/v1","apiKeyEnv":"DASHSCOPE_API_KEY"}',
 '{"temperature":0.7,"max_tokens":4096}', '[]', 'DISABLED', 'Alibaba Cloud flagship Qwen LLM seed.'),
('seed-tongyi-qwen-plus', '通义千问 Qwen Plus', 'tongyi', 'LLM', 'qwen-plus', 'OPENAI_COMPATIBLE', 'default',
 '{"baseUrl":"https://dashscope.aliyuncs.com/compatible-mode/v1","apiKeyEnv":"DASHSCOPE_API_KEY"}',
 '{"temperature":0.7,"max_tokens":4096}', '[]', 'DISABLED', 'Alibaba Cloud balanced Qwen LLM seed.'),
('seed-tongyi-qwen-vl-plus', '通义千问 Qwen3-VL Plus', 'tongyi', 'IMAGE', 'qwen3-vl-plus', 'OPENAI_COMPATIBLE', 'default',
 '{"baseUrl":"https://dashscope.aliyuncs.com/compatible-mode/v1","apiKeyEnv":"DASHSCOPE_API_KEY"}',
 '{"max_tokens":2048}', '[]', 'DISABLED', 'Alibaba Cloud visual understanding seed.'),
('seed-tongyi-embedding-v4', '通义文本向量 V4', 'tongyi', 'EMBEDDING', 'text-embedding-v4', 'OPENAI_COMPATIBLE', 'default',
 '{"baseUrl":"https://dashscope.aliyuncs.com/compatible-mode/v1","apiKeyEnv":"DASHSCOPE_API_KEY"}',
 '{"dimensions":1024}', '[]', 'DISABLED', 'Alibaba Cloud text embedding seed.'),
('seed-tongyi-qwen-image', '通义图像生成', 'tongyi', 'IMAGE_GENERATION', 'qwen-image', 'OPENAI_COMPATIBLE', 'default',
 '{"baseUrl":"https://dashscope.aliyuncs.com/compatible-mode/v1","apiKeyEnv":"DASHSCOPE_API_KEY"}',
 '{"size":"1024x1024"}', '[]', 'DISABLED', 'Alibaba Cloud Qwen image generation seed.'),

-- Anthropic
('seed-anthropic-opus-4-7', 'Claude Opus 4.7', 'anthropic', 'LLM', 'claude-opus-4-7', 'OPENAI_COMPATIBLE', 'default',
 '{"baseUrl":"https://api.anthropic.com/v1","apiKeyEnv":"ANTHROPIC_API_KEY"}',
 '{"temperature":0.7,"max_tokens":4096}', '[]', 'DISABLED', 'Anthropic most capable Claude seed.'),
('seed-anthropic-sonnet-4-6', 'Claude Sonnet 4.6', 'anthropic', 'LLM', 'claude-sonnet-4-6', 'OPENAI_COMPATIBLE', 'default',
 '{"baseUrl":"https://api.anthropic.com/v1","apiKeyEnv":"ANTHROPIC_API_KEY"}',
 '{"temperature":0.7,"max_tokens":4096}', '[]', 'DISABLED', 'Anthropic balanced Claude seed.'),
('seed-anthropic-haiku-4-5', 'Claude Haiku 4.5', 'anthropic', 'LLM', 'claude-haiku-4-5', 'OPENAI_COMPATIBLE', 'default',
 '{"baseUrl":"https://api.anthropic.com/v1","apiKeyEnv":"ANTHROPIC_API_KEY"}',
 '{"temperature":0.7,"max_tokens":4096}', '[]', 'DISABLED', 'Anthropic fast Claude seed.'),

-- Google Gemini
('seed-gemini-2-5-pro', 'Gemini 2.5 Pro', 'gemini', 'LLM', 'gemini-2.5-pro', 'OPENAI_COMPATIBLE', 'default',
 '{"baseUrl":"https://generativelanguage.googleapis.com/v1beta/openai","apiKeyEnv":"GEMINI_API_KEY"}',
 '{"temperature":0.7,"max_tokens":4096}', '[]', 'DISABLED', 'Google Gemini advanced reasoning seed.'),
('seed-gemini-2-5-flash', 'Gemini 2.5 Flash', 'gemini', 'LLM', 'gemini-2.5-flash', 'OPENAI_COMPATIBLE', 'default',
 '{"baseUrl":"https://generativelanguage.googleapis.com/v1beta/openai","apiKeyEnv":"GEMINI_API_KEY"}',
 '{"temperature":0.7,"max_tokens":4096}', '[]', 'DISABLED', 'Google Gemini price-performance seed.'),
('seed-gemini-embedding', 'Gemini Embedding', 'gemini', 'EMBEDDING', 'gemini-embedding-001', 'OPENAI_COMPATIBLE', 'default',
 '{"baseUrl":"https://generativelanguage.googleapis.com/v1beta/openai","apiKeyEnv":"GEMINI_API_KEY"}',
 '{"dimensions":3072}', '[]', 'DISABLED', 'Google Gemini embedding seed.'),

-- DeepSeek
('seed-deepseek-v4-pro', 'DeepSeek V4 Pro', 'deepseek', 'LLM', 'deepseek-v4-pro', 'OPENAI_COMPATIBLE', 'default',
 '{"baseUrl":"https://api.deepseek.com","apiKeyEnv":"DEEPSEEK_API_KEY"}',
 '{"temperature":0.7,"max_tokens":4096,"reasoning_effort":"high"}', '[]', 'DISABLED', 'DeepSeek reasoning-capable LLM seed.'),
('seed-deepseek-v4-flash', 'DeepSeek V4 Flash', 'deepseek', 'LLM', 'deepseek-v4-flash', 'OPENAI_COMPATIBLE', 'default',
 '{"baseUrl":"https://api.deepseek.com","apiKeyEnv":"DEEPSEEK_API_KEY"}',
 '{"temperature":0.7,"max_tokens":4096}', '[]', 'DISABLED', 'DeepSeek fast LLM seed.'),

-- Moonshot / Kimi
('seed-kimi-k2-6', 'Kimi K2.6', 'kimi', 'LLM', 'kimi-k2.6', 'OPENAI_COMPATIBLE', 'default',
 '{"baseUrl":"https://api.moonshot.ai/v1","apiKeyEnv":"MOONSHOT_API_KEY"}',
 '{"temperature":0.7,"max_tokens":4096}', '[]', 'DISABLED', 'Moonshot current Kimi family seed; verify account model access.'),
('seed-kimi-moonshot-128k', 'Moonshot v1 128K', 'kimi', 'LLM', 'moonshot-v1-128k', 'OPENAI_COMPATIBLE', 'default',
 '{"baseUrl":"https://api.moonshot.ai/v1","apiKeyEnv":"MOONSHOT_API_KEY"}',
 '{"temperature":0.7,"max_tokens":4096}', '[]', 'DISABLED', 'Moonshot long-context text generation seed.'),
('seed-kimi-vision-32k', 'Moonshot Vision 32K', 'kimi', 'IMAGE', 'moonshot-v1-32k-vision-preview', 'OPENAI_COMPATIBLE', 'default',
 '{"baseUrl":"https://api.moonshot.ai/v1","apiKeyEnv":"MOONSHOT_API_KEY"}',
 '{"max_tokens":2048}', '[]', 'DISABLED', 'Moonshot vision preview seed.'),

-- SiliconFlow
('seed-siliconflow-qwen3-32b', '硅基流动 Qwen3 32B', 'siliconflow', 'LLM', 'Qwen/Qwen3-32B', 'OPENAI_COMPATIBLE', 'default',
 '{"baseUrl":"https://api.siliconflow.cn/v1","apiKeyEnv":"SILICONFLOW_API_KEY"}',
 '{"temperature":0.7,"max_tokens":4096}', '[]', 'DISABLED', 'SiliconFlow open model LLM seed.'),
('seed-siliconflow-deepseek-v3-1', '硅基流动 DeepSeek V3.1', 'siliconflow', 'LLM', 'deepseek-ai/DeepSeek-V3.1', 'OPENAI_COMPATIBLE', 'default',
 '{"baseUrl":"https://api.siliconflow.cn/v1","apiKeyEnv":"SILICONFLOW_API_KEY"}',
 '{"temperature":0.7,"max_tokens":4096}', '[]', 'DISABLED', 'SiliconFlow DeepSeek hosted LLM seed.'),
('seed-siliconflow-bge-m3', '硅基流动 BGE-M3 向量', 'siliconflow', 'EMBEDDING', 'BAAI/bge-m3', 'OPENAI_COMPATIBLE', 'default',
 '{"baseUrl":"https://api.siliconflow.cn/v1","apiKeyEnv":"SILICONFLOW_API_KEY"}',
 '{"dimensions":1024}', '[]', 'DISABLED', 'SiliconFlow embedding seed.'),
('seed-siliconflow-bge-reranker', '硅基流动 BGE 重排', 'siliconflow', 'RERANKER', 'BAAI/bge-reranker-v2-m3', 'OPENAI_COMPATIBLE', 'default',
 '{"baseUrl":"https://api.siliconflow.cn/v1","apiKeyEnv":"SILICONFLOW_API_KEY"}',
 '{"top_n":5}', '[]', 'DISABLED', 'SiliconFlow reranker seed.'),
('seed-siliconflow-sd35-large', '硅基流动 SD3.5 Large', 'siliconflow', 'IMAGE_GENERATION', 'stabilityai/stable-diffusion-3-5-large', 'OPENAI_COMPATIBLE', 'default',
 '{"baseUrl":"https://api.siliconflow.cn/v1","apiKeyEnv":"SILICONFLOW_API_KEY"}',
 '{"image_size":"1024x1024"}', '[]', 'DISABLED', 'SiliconFlow image generation seed.'),

-- Amazon Bedrock
('seed-bedrock-nova-pro', 'Amazon Bedrock Nova Pro', 'amazon-bedrock', 'LLM', 'amazon.nova-pro-v1:0', 'BUILT_IN', 'default',
 '{"region":"us-east-1","accessKeyEnv":"AWS_ACCESS_KEY_ID","secretKeyEnv":"AWS_SECRET_ACCESS_KEY"}',
 '{"temperature":0.7,"max_tokens":4096}', '[]', 'DISABLED', 'Amazon Bedrock Nova Pro seed. Requires Bedrock adapter before use.'),
('seed-bedrock-nova-lite', 'Amazon Bedrock Nova Lite', 'amazon-bedrock', 'LLM', 'amazon.nova-lite-v1:0', 'BUILT_IN', 'default',
 '{"region":"us-east-1","accessKeyEnv":"AWS_ACCESS_KEY_ID","secretKeyEnv":"AWS_SECRET_ACCESS_KEY"}',
 '{"temperature":0.7,"max_tokens":4096}', '[]', 'DISABLED', 'Amazon Bedrock Nova Lite seed. Requires Bedrock adapter before use.'),
('seed-bedrock-titan-embed-v2', 'Amazon Titan Embeddings V2', 'amazon-bedrock', 'EMBEDDING', 'amazon.titan-embed-text-v2:0', 'BUILT_IN', 'default',
 '{"region":"us-east-1","accessKeyEnv":"AWS_ACCESS_KEY_ID","secretKeyEnv":"AWS_SECRET_ACCESS_KEY"}',
 '{"dimensions":1024}', '[]', 'DISABLED', 'Amazon Titan text embedding seed. Requires Bedrock adapter before use.'),
('seed-bedrock-titan-image-v2', 'Amazon Titan Image Generator V2', 'amazon-bedrock', 'IMAGE_GENERATION', 'amazon.titan-image-generator-v2:0', 'BUILT_IN', 'default',
 '{"region":"us-east-1","accessKeyEnv":"AWS_ACCESS_KEY_ID","secretKeyEnv":"AWS_SECRET_ACCESS_KEY"}',
 '{"quality":"standard"}', '[]', 'DISABLED', 'Amazon Titan image generation seed. Requires Bedrock adapter before use.'),

-- Azure OpenAI
('seed-azure-gpt-4-1', 'Azure OpenAI GPT-4.1', 'azure-openai', 'LLM', 'gpt-4.1', 'OPENAI_COMPATIBLE', 'default',
 '{"baseUrl":"https://{resource}.openai.azure.com/openai/v1","apiKeyEnv":"AZURE_OPENAI_API_KEY"}',
 '{"temperature":0.7,"max_tokens":4096}', '[]', 'DISABLED', 'Azure OpenAI deployment seed; replace model_name with your deployment if needed.'),
('seed-azure-embedding-3-large', 'Azure OpenAI Embedding 3 Large', 'azure-openai', 'EMBEDDING', 'text-embedding-3-large', 'OPENAI_COMPATIBLE', 'default',
 '{"baseUrl":"https://{resource}.openai.azure.com/openai/v1","apiKeyEnv":"AZURE_OPENAI_API_KEY"}',
 '{"dimensions":3072}', '[]', 'DISABLED', 'Azure OpenAI embedding seed; replace model_name with your deployment if needed.'),

-- Tencent Hunyuan
('seed-hunyuan-turbos', '腾讯混元 TurboS', 'tencent-hunyuan', 'LLM', 'hunyuan-turbos', 'OPENAI_COMPATIBLE', 'default',
 '{"baseUrl":"https://api.hunyuan.cloud.tencent.com/v1","apiKeyEnv":"TENCENT_HUNYUAN_API_KEY"}',
 '{"temperature":0.7,"max_tokens":4096}', '[]', 'DISABLED', 'Tencent Hunyuan high-speed LLM seed. Verify exact endpoint and model access.'),
('seed-hunyuan-lite', '腾讯混元 Lite', 'tencent-hunyuan', 'LLM', 'hunyuan-lite', 'OPENAI_COMPATIBLE', 'default',
 '{"baseUrl":"https://api.hunyuan.cloud.tencent.com/v1","apiKeyEnv":"TENCENT_HUNYUAN_API_KEY"}',
 '{"temperature":0.7,"max_tokens":4096}', '[]', 'DISABLED', 'Tencent Hunyuan lightweight LLM seed. Verify exact endpoint and model access.'),

-- Baidu Qianfan
('seed-qianfan-ernie-4-5-turbo', '千帆 ERNIE 4.5 Turbo', 'qianfan', 'LLM', 'ernie-4.5-turbo-128k', 'OPENAI_COMPATIBLE', 'default',
 '{"baseUrl":"https://qianfan.baidubce.com/v2","apiKeyEnv":"QIANFAN_API_KEY"}',
 '{"temperature":0.7,"max_tokens":4096}', '[]', 'DISABLED', 'Baidu Qianfan ERNIE seed. Verify exact account endpoint and model access.'),
('seed-qianfan-embedding-v1', '千帆 Embedding V1', 'qianfan', 'EMBEDDING', 'embedding-v1', 'OPENAI_COMPATIBLE', 'default',
 '{"baseUrl":"https://qianfan.baidubce.com/v2","apiKeyEnv":"QIANFAN_API_KEY"}',
 '{"dimensions":384}', '[]', 'DISABLED', 'Baidu Qianfan embedding seed. Verify exact account endpoint and model access.'),

-- Volcano Engine / Doubao
('seed-doubao-seed-1-6', '火山方舟 Doubao Seed 1.6', 'volcengine', 'LLM', 'doubao-seed-1-6', 'OPENAI_COMPATIBLE', 'default',
 '{"baseUrl":"https://ark.cn-beijing.volces.com/api/v3","apiKeyEnv":"ARK_API_KEY"}',
 '{"temperature":0.7,"max_tokens":4096}', '[]', 'DISABLED', 'Volcano Engine Ark Doubao LLM seed. Replace with your endpoint model id if needed.'),
('seed-doubao-embedding-large', '火山方舟 Doubao Embedding Large', 'volcengine', 'EMBEDDING', 'doubao-embedding-large-text-240915', 'OPENAI_COMPATIBLE', 'default',
 '{"baseUrl":"https://ark.cn-beijing.volces.com/api/v3","apiKeyEnv":"ARK_API_KEY"}',
 '{"dimensions":2048}', '[]', 'DISABLED', 'Volcano Engine Ark embedding seed. Replace with your endpoint model id if needed.'),

-- Local / self-hosted
('seed-ollama-qwen3-32b', 'Ollama Qwen3 32B', 'ollama', 'LLM', 'qwen3:32b', 'OPENAI_COMPATIBLE', 'default',
 '{"baseUrl":"http://localhost:11434/v1","apiKey":"ollama"}',
 '{"temperature":0.7,"max_tokens":4096}', '[]', 'DISABLED', 'Local Ollama seed; enable only when local model is pulled.'),
('seed-vllm-qwen3-32b', 'vLLM Qwen3 32B', 'vllm', 'LLM', 'Qwen/Qwen3-32B', 'OPENAI_COMPATIBLE', 'default',
 '{"baseUrl":"http://localhost:8000/v1","apiKey":"EMPTY"}',
 '{"temperature":0.7,"max_tokens":4096}', '[]', 'DISABLED', 'Self-hosted vLLM seed; set baseUrl/modelName to deployed model.')
ON DUPLICATE KEY UPDATE
    `name` = VALUES(`name`),
    `provider` = VALUES(`provider`),
    `model_type` = VALUES(`model_type`),
    `model_name` = VALUES(`model_name`),
    `endpoint_type` = VALUES(`endpoint_type`),
    `workspace_id` = VALUES(`workspace_id`),
    `credential_json` = VALUES(`credential_json`),
    `default_options_json` = VALUES(`default_options_json`),
    `params_schema_json` = VALUES(`params_schema_json`),
    `status` = VALUES(`status`),
    `remark` = VALUES(`remark`),
    `updated_at` = CURRENT_TIMESTAMP;
