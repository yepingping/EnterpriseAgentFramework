# ReachAI Project Memory

## Product Positioning

睿池 ReachAI 是面向 Java 企业系统的 AI 能力中台。它不是单纯的工作流画布，也不是只扫描历史项目生成 Tool 的工具，而是把企业系统中的接口、领域方法、知识、模型、流程、权限和运行审计沉淀为 Agent 可理解、可编排、可治理、可开放的能力资产。

当前外部叙事可以稳定表述为：

- 面向 Java 企业系统的 AI 能力中台。
- Enterprise AI Capability Platform。
- AI Agent Control Plane / Enterprise Agent Runtime Platform 也可作为架构侧表达。

## Main Flow

1. 业务系统引入 `ai-spring-boot-starter`。
2. 业务方法或 Controller 使用 `@AiCapability`、`@AiParam`、`@AiOutput` 补充 AI 可理解的语义。
3. Starter 在启动时同步项目、实例、能力快照和 SDK 图。
4. 平台形成字段级 diff、评审 apply/ignore，并沉淀正式能力资产。
5. Agent Studio 基于能力资产编排 `GraphSpec`。
6. Agent 发布形成版本快照，Runtime 通过 `AgentRuntimeAdapter` 执行。
7. RunOps、Trace、ACL、Guard、Gateway、MCP、A2A 和嵌入式对话负责生产治理与开放。

## Modules

- `ai-admin-front/`: Vue 3 + Element Plus 管理端，包含项目注册、能力目录、Agent Studio、RunOps、模型、知识库、MCP/A2A、嵌入审计等页面。
- `ai-agent-service/`: Agent 定义、Studio、Runtime、发布、Trace、Tool ACL、注册中心、嵌入式身份、Gateway、MCP、A2A 等核心服务。
- `ai-skills-service/`: 知识库、文件、chunk、业务索引、语义文档等能力支撑。
- `ai-model-service/`: 模型实例中心和模型配置。
- `ai-common/`: 通用模型、响应、工具类。
- `ai-skill-sdk/`: 业务系统声明 AI 能力的 SDK 契约。
- `ai-spring-boot-starter/`: Spring Boot 接入、扫描、注册、心跳、能力同步和 SDK 图同步。
- `sql/`: 根 SQL 基线与升级脚本。
- `docs/`: 当前权威知识库。

## Current Documentation Shape

`docs/README.md` 是当前知识库入口。主文档按产品能力组织：

- `01-平台定位与架构总览.md`
- `02-项目注册与能力资产.md`
- `03-Agent-Studio与Runtime.md`
- `04-运行治理与开放协议.md`
- `05-知识模型与企业资产.md`
- `06-项目背景技术与功能说明.md`
- `07-平台身份与授权模型.md`
- `08-平台对话框对外嵌入支持.md`

不要新增阶段型验收清单作为主知识库。新文档应按产品能力组织，并指向真实代码、接口、SQL 表或前端页面。

## SQL Baseline

`sql/init.sql` 是当前唯一 SQL 基线入口。它覆盖注册中心、能力资产、Agent、GraphSpec、Trace、RunOps、Tool ACL、Guard、模型、知识库、业务索引、MCP、A2A、Gateway、市场资产和嵌入式对话等表。

历史 service-level SQL 已从活跃路径移除。未来 SQL 变化必须同时维护根基线和 `sql/upgrade-*.sql`。

## Agent Studio And Runtime

Agent Studio 的核心不是只画流程图，而是统一：

- 可视化画布。
- 交互式节点。
- 会话式调试台。
- AI 生成工作流。
- AI 局部修改工作流。
- SDK 图注册与只读展示。
- 发布校验。
- Runtime 执行。
- Trace/RunOps 复盘。

当前同时支持两类智能体形态：

- `AUTONOMOUS`: 自主智能体，主线 Runtime 是 AgentScope。
- `WORKFLOW`: 工作流智能体，主线 Runtime 是 LangGraph4j + `GraphSpec`。

`CursorCodeAgentRuntimeAdapter` 和 `OpenAIAgentsRuntimeAdapter` 当前是扩展边界，不应写成已可用生产 Runtime。

## GraphSpec

`GraphSpec` 是平台可执行语义的核心中间表示。

- 后端类型：`ai-agent-service/src/main/java/com/enterprise/ai/agent/graph/GraphSpec.java`。
- 前端类型：`ai-admin-front/src/types/agent.ts`。
- DB 字段：`agent_definition.graph_spec_json` 保存运行语义。
- DB 字段：`agent_definition.canvas_json` 保存 Studio 画布布局。
- 发布校验：`AgentReleaseValidationService`。
- Runtime 执行：`LangGraph4jRuntimeAdapter`。

新增节点、边、变量映射、条件路由或 Runtime 行为时，要优先维护 `GraphSpec` 语义，不能只扩展画布 JSON。

## AI Draft And Local Edit

AI 生成工作流：

- API: `/api/agent/studio/generate-draft`
- Controller: `AgentStudioDraftController`
- Generator: `LlmWorkflowDraftGenerator`
- 结果应标准化为 `GraphSpec` 和 `canvasSnapshot`，前端走预览/应用，不直接覆盖画布。

AI 局部编辑工作流：

- API: `/api/agent/studio/edit-draft`
- Service: `WorkflowDraftEditService`
- 输入应包含当前 canvas、用户意图和选中上下文。
- 默认围绕当前选中节点/边/多选内容做局部修改，而不是重生成整张图。

## Registration And Capability Assets

推荐新系统和核心系统使用 `ai-spring-boot-starter` 主动注册。Controller/OpenAPI 扫描保留给存量系统和低改造场景。

重要概念：

- `@AiCapability`: 声明业务方法或接口可作为 Agent 能力。
- `@AiParam`: 声明参数语义。
- `EafRegistryProperties`: 使用 `eaf.registry`、`eaf.project`、`eaf.capability` 配置。
- `EafCapabilityScanner`: 从 Spring MVC Mapping、注解和请求体结构生成能力描述。
- `tool_definition`: 统一承载 Tool 与粗粒度 Capability。

## Identity And Embed Chat

平台身份与嵌入式对话已经进入主线能力：

- 平台用户支持本地账号、Header SSO、OIDC、SAML 等入口。
- 业务用户目录以 `tenantId + appId + externalUserId` 为稳定键。
- 嵌入式对话使用短期 `embedToken`，前端 SDK 不保存 `appSecret`。
- 业务后端负责用应用凭证和当前登录用户向平台申请 token。
- Page Action 绑定 `sessionId + pageInstanceId`，只调用当前页面已注册动作。
- 自定义 renderer 只能使用注册过的 `rendererKey` 和结构化数据，不能注入任意 HTML。

相关 SQL 包括 `eaf_embed_session`、`eaf_page_action_event`、`eaf_embed_chat_event`、`eaf_embed_renderer` 和 `eaf_embed_token_revocation`。

## Frontend State

管理端已经有暗色/亮色主题基础，后续主题改动应复用 CSS 变量和现有主题文件。不要在页面里散落硬编码暗色、浅色或 Element Plus 覆盖。

项目范围选择器、注册中心、扫描项目、Agent Studio 侧边栏折叠等行为和 `MainLayout.vue`、`ProjectSelector.vue`、路由名称、项目 store 相关。改导航和布局前先查这些共享位置。
