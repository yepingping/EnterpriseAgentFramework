# ReachAI History Index For AI Agents

本文件按主题索引历史工作记忆。它不是完整变更日志，而是告诉后续 AI Agent：遇到相似任务时应优先想到哪些上下文、文件和风险。

## Docs And Positioning

相关主题：

- `docs/` 已从历史阶段文档收敛为当前知识库。
- 根 `README.md` 已按 ReachAI 对外首页重写。
- 项目定位应保持为“面向 Java 企业系统的 AI 能力中台”。
- 当前代码支持的故事包括注册中心、能力资产、Agent Studio、Runtime Adapter、RunOps/Trace、ACL、MCP/A2A、知识/模型资产和统一 SQL 基线。

优先看：

- `README.md`
- `docs/README.md`
- `docs/01-平台定位与架构总览.md`
- `docs/03-Agent-Studio与Runtime.md`

## SQL Baseline Consolidation

相关主题：

- 历史 service SQL 已合并到根 `sql/init.sql`。
- service-level SQL 目录不再是活跃迁移入口。
- 清理 SQL 前要比对 source SQL、根基线、Entity 字段和索引。
- 未来 SQL 变化必须同时维护 `init.sql` 和 `upgrade-*.sql`。

优先看：

- `sql/init.sql`
- `sql/README.md`
- `AGENTS.md`
- `docs/ai-memory/WORKING-RULES.md`

## Agent Studio Semantic Edit

相关主题：

- 用户希望 AI 能围绕当前选中节点、边或多选内容做真实编辑，而不是只给建议。
- AI 生成和局部编辑都应预览后应用。
- `validationErrors` 应能阻止无效草稿进入画布。

优先看：

- `ai-agent-service/src/main/java/com/enterprise/ai/agent/controller/AgentStudioDraftController.java`
- `ai-agent-service/src/main/java/com/enterprise/ai/agent/studio/LlmWorkflowDraftGenerator.java`
- `ai-agent-service/src/main/java/com/enterprise/ai/agent/studio/WorkflowDraftEditService.java`
- `ai-admin-front/src/views/agent/AgentStudio.vue`
- `ai-admin-front/src/types/agent.ts`

## GraphSpec And Runtime

相关主题：

- `GraphSpec` 是执行语义，`canvas_json` 是布局。
- SDK graph sync 进入 Agent Studio 后应形成 `GraphSpec`、canvas 和 `extra.sdkGraph` 元数据。
- `AgentReleaseValidationService` 把 `GraphSpec` 当作发布契约。
- LangGraph4j Runtime 已消费条件边、变量映射、输出别名和交互式节点。

优先看：

- `ai-agent-service/src/main/java/com/enterprise/ai/agent/graph/GraphSpec.java`
- `ai-agent-service/src/main/java/com/enterprise/ai/agent/runtime/LangGraph4jRuntimeAdapter.java`
- `ai-agent-service/src/main/java/com/enterprise/ai/agent/agent/AgentReleaseValidationService.java`
- `ai-agent-service/src/main/java/com/enterprise/ai/agent/registry/AiRegistryService.java`
- `ai-admin-front/src/utils/studio.ts`

## Skill To Capability Naming

相关主题：

- 当前 `Skill` 概念更接近内部能力包或历史命名，不等同于外部标准 Skill 包。
- 产品文案默认使用 `Capability / 能力`。
- SQL 表名、字段、API 路径和 SDK 契约中的 `skill` 不要盲目全局替换。

优先看：

- `ai-skill-sdk/src/main/java/com/enterprise/ai/skill/AiCapability.java`
- `ai-admin-front/src/views/skill/`
- `ai-admin-front/src/api/skill.ts`
- `ai-agent-service/src/main/java/com/enterprise/ai/agent/skill/`
- `sql/init.sql`

## API Graph And Scan Page

相关主题：

- API 图谱问题经常跨前端画布、后端边生成、SQL schema 和布局语义。
- `api_graph_edge.status` 缺列会导致点击或状态查询失败。
- G6 v5 类型和服务端渲染环境要小心。
- saved layout 应优先于 synthetic positions。

优先看：

- `ai-agent-service/src/main/java/com/enterprise/ai/agent/api/ApiGraphService.java`
- `ai-admin-front/src/views/scan/ScanProjectDetail.vue`
- `ai-admin-front/src/components/api-graph/ApiGraphCanvas.vue`
- `sql/init.sql`

## Frontend Theme And Navigation

相关主题：

- 管理端已做暗色/亮色主题，后续改动应复用变量。
- Element Plus 内部样式经常需要集中 override。
- 项目范围选择器和项目管理页行为由 `MainLayout.vue`、`ProjectSelector.vue`、router 和 store 共同决定。
- Agent Studio 进入时侧边栏折叠应由主布局层控制。

优先看：

- `ai-admin-front/src/styles/`
- `ai-admin-front/src/layouts/MainLayout.vue`
- `ai-admin-front/src/components/ProjectSelector.vue`
- `ai-admin-front/src/stores/`
- `ai-admin-front/src/router/`

## Embed Identity And Chat

相关主题：

- 企业身份、业务用户目录、应用凭证、短期 embed token、Page Action、Renderer 注册和审计是同一条链路。
- 前端 SDK 不保存 `appSecret`。
- Token、Origin、Agent 授权、用户状态、TTL、`jti` 撤销和 `kid` 都是安全边界。

优先看：

- `docs/07-平台身份与授权模型.md`
- `docs/08-平台对话框对外嵌入支持.md`
- `ai-agent-service/src/main/java/com/enterprise/ai/agent/identity/`
- `ai-agent-service/src/main/java/com/enterprise/ai/agent/controller/EmbedChatController.java`
- `sql/init.sql`

## Startup And Verification Pitfalls

相关主题：

- 用户重复同一 stack trace 时，必须重新跑或对照同一错误签名，不能只解释理论修复。
- 曾出现过 `EmbedTokenService` 构造器相关启动失败，处理时要以当前源码和当前启动日志为准。
- Windows PowerShell 中不要用 bash heredoc 写法。
- 中文 Markdown / Vue 文本要按 UTF-8 读取。

优先看：

- `docs/ai-memory/KNOWN-PITFALLS.md`
- `docs/ai-memory/VERIFICATION.md`
