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

## Context Governance Kernel

相关主题：

- Context Kernel v1 是企业 Agent 上下文治理底座，独立于 `ConversationMemoryService`。
- Project Dev Memory（`PROJECT_DEV`）与 Runtime User Memory（`RUNTIME_USER`）逻辑隔离。
- Phase-1：结构化主库、LIKE 检索、ContextPackage、审计。
- Phase-2（2026-06-22）：嵌入式 CENTRAL Runtime 只读注入 `RUNTIME_USER` ContextPackage（`context/runtime/`）；HYBRID 本批跳过组包。
- Phase-3（2026-06-22）：Memory Candidate 受控写回（`context/memory/` + `context_memory_candidate`）；规则型显式意图生成 PENDING Candidate，approve 后写 `context_item`；向量/LLM 抽取/前端审核页仍为后续阶段。
- Phase-3.1：USER namespace 硬边界、RuntimeMemoryCandidateIdentity、Platform self-service 鉴权。
- Phase-4（2026-06-23）：Embed-side runtime 用户自确认 API（`EmbedMemoryCandidateController` + `RuntimeUserIdentityResolver`）；平台后台审核页推迟。
- Phase-4.1（2026-06-23）：`EmbedSessionService.requireActiveSession` 补强 tenant/app/globalUserId/pageKey 等校验；embed chat 与 memory API 共用。
- Phase-5（2026-06-23）：`ai-admin-front` PROJECT_DEV 上下文治理 MVP（`/context/governance`）；新增 `GET /api/context/items` 管理列表接口。
- Phase-5.1（2026-06-23）：`matchesProject` projectCode/projectId 双标识硬化；管理列表与 namespace 候选支持 `projectId`。
- Phase-5.2（2026-06-23）：`validateNamespaceBoundaryForCreate` 与读路径共用 `matchesProjectIdentity`；create item 项目边界对齐。
- Phase-6（2026-06-23）：`ContextProjectIdentity`；namespaceKey `pid-{id}`；candidate approve project 校验对齐；`POST /api/context/lifecycle/run`；未做 SQL/定时任务/RBAC/向量/LLM。
- Phase-7（2026-06-23）：检索多因素排序 + scoreBreakdown；`GET /api/context/ops/summary`；audit 过滤增强；管理页运维摘要 + lifecycle dryRun；未做向量/FULLTEXT/LLM/SQL。
- Phase-7.1（2026-06-23）：ops summary RUNTIME_USER gate（默认拒绝，显式 `includeRuntimeUser=true` 仅聚合）；audit + 24h ops summary 支持 `projectId` 过滤。
- **Phase-1 Closure（2026-06-23）**：Context Governance Kernel v1 从 kernel 到 ops summary 第一阶段完成；本批仅文档收口（`Context-Governance-Kernel.md` 架构基线 + `CONTEXT-GOVERNANCE-BASELINE.md`），无功能变更。
- Phase-2.10（2026-06-24）：`GET /api/context/audit` / `ContextAuditListRequest` / `ContextGovernance.vue` 支持 `dateFrom/dateTo`，按时间窗口追踪 AI Coding / code-scan 候选提交与审核。
- Phase-2.11（2026-06-24）：新增 `ContextLifecycleScheduler`，默认关闭；开启 `eaf.context.lifecycle.enabled=true` 后复用 `ContextLifecycleService.run` 定时执行 candidate expire 与 PROJECT_DEV item stale。
- Phase-2.12（2026-06-24）：`ContextOpsSummaryService` 的 item/candidate 统计改用 Mapper `selectCount` 条件聚合，避免加载明细做内存遍历；RUNTIME_USER ops summary 仍为 aggregate-only。
- Phase-2.13（2026-06-24）：`runtimePlacement=HYBRID` 初始仍 deferred；embedded runtime 成功不组包，失败回落 central runtime 前由 `AgentRouter` 调 `RuntimeContextPackageService.injectForCentralFallback` 注入 Runtime Context。
- Phase-2.14（2026-06-24）：RUNTIME_USER candidate 代审身份桥：`context_runtime_user_mapping` + `/api/context/runtime-user-mappings` + `ContextRuntimeUserAccessService`；维护映射需 `context:runtime-user:mapping:manage` 或 admin，代审需 `context:runtime-user:review` 或 admin 且存在 ACTIVE 映射。
- Phase-2.15（2026-06-24）：RUNTIME_USER candidate 代审 UI：`ContextGovernance.vue` Runtime 映射 tab 可选中 ACTIVE 映射后按 `memoryLane=RUNTIME_USER` + `runtimeUserId` 查看 candidate 缓冲区并采纳/忽略；仍不开放已采纳 PRIVATE item 管理 UI。
- Phase-2.16（2026-06-24）：PROJECT_DEV candidate 审计追踪 UI：候选行“查审计”按 `submissionId/traceId` 切到 Audit tab；`tab=audit` 深链可直接打开审计过滤页。
- Phase-2.17（2026-06-24）：AI Coding Gateway manifest 审计深链模板：`endpoints.contextCandidateAuditUrlTemplate` 与项目详情复制面板暴露 `traceId={submissionId}` 的 Audit tab URL 模板，供外部工具提交候选后引导用户查看同批次 audit。
- Phase-2.18（2026-06-24）：AI Coding Gateway manifest 候选状态回查模板：`endpoints.contextCandidateStatusUrlTemplate` 与项目详情复制面板暴露 `traceId={submissionId}&status=PENDING` 的候选列表 URL 模板，供外部工具提交后回查同批次待审核候选。
- Phase-2.19（2026-06-24）：Page Assistant header-auth no-echo：`pageAssistantManifest` 对 `X-ReachAI-AiCoding-Key` / `AiCodingKeyContext` 不回显 raw key；helper script 支持 `-AiCodingKey` / `REACHAI_AI_CODING_KEY` 并用 header 调 manifest、register、checks。
- Phase-2.20（2026-06-24）：SDK onboarding header-auth no-echo：`onboardingManifest` 对 `X-ReachAI-AiCoding-Key` / `AiCodingKeyContext` 不回显 raw key；`agentProvisioning.provisionAgentUrl` 不追加 query key，外部工具用 header 调 provisioning。
- Phase-2.21（2026-06-24）：Workflow AI Coding prompt header-first：Page Assistant Wizard 生成的 Workflow AI Coding prompt 不把 key 拼进 Workflow/report URL；workflow-ai-coding skill 与文档示例统一用 `X-ReachAI-AiCoding-Key`。
- Phase-2.22（2026-06-24）：SDK quick-access prompt header-first：SDK 接入向导生成的 onboarding prompt 不把 key 拼进 manifest、provisioning、access session、step report 或 session check URL；reachai-onboarding skill 与文档示例统一用 `X-ReachAI-AiCoding-Key`。
- Phase-2.23（2026-06-24）：External AI Coding header-only enforcement：`PlatformAuthInterceptor`、`AiAssistController`、AI Coding Gateway manifest、Page Assistant prompt 与随包 docs 移除 `aiCodingKey` query 鉴权与 discovery。

优先看：

- `docs/Context-Governance-Kernel.md`
- `docs/ai-memory/CONTEXT-GOVERNANCE-BASELINE.md`
- `ai-agent-service/src/main/java/com/enterprise/ai/agent/context/`
- `ai-agent-service/src/main/java/com/enterprise/ai/agent/context/runtime/`
- `sql/upgrade-20260622-context-kernel.sql`

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
