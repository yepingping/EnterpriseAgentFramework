# Context Governance Kernel

## Phase-1 完成状态

**第一阶段已结束。**

完成的是：**ReachAI Context Governance Kernel v1：企业 Agent 上下文治理底座。**

它已经具备：

- 存储模型（6 张核心上下文表 + `context_runtime_user_mapping` 身份映射表 + init/upgrade SQL）
- 访问边界（tenant / project / lane / visibility / RUNTIME_USER PRIVATE）
- 运行时只读注入（嵌入式 CENTRAL）
- 候选记忆确认链路（Candidate → approve → `context_item`）
- 项目上下文管理页（`PROJECT_DEV`）
- 生命周期与运维摘要（lifecycle + ops summary）
- 审计与证据（audit + evidence）
- 检索排序解释（`rankScore` / `hitReason` / `scoreBreakdown`）

Phase-2.1/2.2/2.3/2.4/2.5/2.6/2.7/2.8/2.9/2.10/2.11/2.12/2.13/2.14/2.15/2.16/2.17/2.18/2.19/2.20/2.21/2.22/2.23 已在此基础上继续补齐智能记忆抽取、候选质量门、query-aware HYBRID 检索、注入解释 metadata、候选审核提示、AI Coding Context Candidate 入口、audit projectId 索引、治理页深链 scope、audit 日期范围、定时 lifecycle job、ops summary SQL 聚合、HYBRID central fallback runtime 注入、RUNTIME_USER 代审身份映射、映射内候选代审 UI、候选到审计的证据链跳转、manifest 审计深链模板、候选状态回查模板、Page Assistant header-auth no-echo、SDK onboarding header-auth no-echo、Workflow AI Coding prompt header-first、SDK quick-access prompt header-first 与外部 AI Coding header-only enforcement。当前仍不包含：

- 向量 / Milvus / FULLTEXT
- RUNTIME_USER 已采纳私有记忆条目管理 UI（已开放的是映射管理和映射内 candidate 代审）
- 与 `ConversationMemoryService` 的迁移/替代

本仓库 **Phase-1 Closure（2026-06-23）** 仅文档收口，无功能/SQL/前端代码变更。

---

## Phase-2.1 / 2.2 / 2.3 / 2.4 / 2.5 / 2.6 / 2.7 / 2.8 / 2.9 / 2.10 / 2.11 / 2.12 / 2.13 / 2.14 / 2.15 / 2.16 / 2.17 智能记忆抽取、候选质量、检索入口、解释性 metadata、运维聚合与身份映射

Phase-2.1 的主线是：**让 Runtime 对话具备“智能提出记忆候选”的能力，但仍受 Candidate 治理约束。**

Phase-2.2 在此基础上补了第一层质量门：**减少重复 candidate，避免用户反复确认已经待审或已经确认过的记忆。**

Phase-2.3 补的是检索“智能感”的最小入口：**保持默认精确 keyword 行为不变，显式 `HYBRID` 时允许确定性的 token fallback。**

Phase-2.4 让入口进入真实体验链路：**Runtime CENTRAL 组包用当前用户消息作为 query，并默认走 `HYBRID`；管理端组包预览可切换 KEYWORD / HYBRID。**

Phase-2.5 补的是“它想起了什么”的可解释性：**embed chat response metadata 会返回 Runtime Context 命中摘要，便于前端、日志和调试面板展示/排查。**

Phase-2.6 补的是“审核前它能提醒什么”：**Runtime 生成记忆候选时会用 HYBRID 检索查看同 scope 已确认记忆；精确重复仍跳过，非精确但相关的 ACTIVE item 会作为 compact quality signal 写入 candidate metadata，辅助用户审核。**

Phase-2.7 补的是外部 AI Coding 工具的第一条 Context Scan 落点与平台审核入口：**`/api/ai-coding/projects/{projectId}/manifest` 作为项目级 AI Coding Gateway discovery；`/api/ai-coding/projects/{projectId}/context-candidates` 与 `/batch` 使用项目级 `aiCodingKey` 接收 `PROJECT_DEV` 候选；`ContextGovernance.vue` 的“候选审核”tab 通过平台登录列出、采纳或忽略候选；候选仍为 PENDING，approve 后才写入 `context_item` + binding + evidence。**

Phase-2.10 补的是审计追踪的时间窗口：**`GET /api/context/audit` 支持 `dateFrom/dateTo` ISO datetime 过滤，`ContextGovernance.vue` 审计 tab 可用日期范围定位某次外部 AI Coding / code-scan 提交前后的候选创建、编辑、采纳或忽略事件。**

Phase-2.11 补的是生命周期治理的后台执行入口：**`ContextLifecycleScheduler` 复用 `ContextLifecycleService.run`，默认关闭；配置 `eaf.context.lifecycle.enabled=true` 后按固定延迟运行，可限定 tenant/project、dryRun、candidate expire limit 与 item stale limit。管理端 lifecycle 仍只暴露 dryRun，不开放 RUNTIME_USER 已采纳私有记忆条目管理或真实 stale 操作。**

Phase-2.12 补的是运维摘要的聚合方式：**`ContextOpsSummaryService` 对 item / candidate 统计改用 Mapper `selectCount` 条件聚合，不再加载 `context_item` / `context_memory_candidate` 明细到内存后遍历；RUNTIME_USER 仍只返回 aggregate-only 计数与 warning。**

Phase-2.13 补的是 `runtimePlacement=HYBRID` 的注入时机：**Embed 入口仍先返回 `hybrid-placement-deferred`，避免嵌入式运行成功时产生未使用的上下文注入；`AgentRouter` 仅在 embedded runtime 失败并回落 central runtime 前调用 `RuntimeContextPackageService.injectForCentralFallback` 组包，并把结果交给 central adapter 的 `effectiveUserMessage()`。**

Phase-2.14 补的是 RUNTIME_USER 候选审核的身份桥：**`context_runtime_user_mapping` 建立 `PlatformPrincipal.userId` 到 runtime `userId/globalUserId/externalUserId` 归一值的 ACTIVE 映射；`ContextRuntimeUserMappingController` 提供管理员维护映射的最小 API；`ContextRuntimeUserAccessService` 要求平台用户同时具备 `context:runtime-user:review`（或 `platform:admin` / `*`）和映射，才允许通过 `ContextMemoryCandidateController` 列表、编辑、采纳、忽略或删除指定 runtime 用户候选。映射维护本身要求 `context:runtime-user:mapping:manage`（或 `platform:admin` / `*`）。自审仍按当前 `PlatformPrincipal.userId` 走原路径。**

Phase-2.15 补的是 RUNTIME_USER 候选代审的管理端闭环：**`ContextGovernance.vue` 的 Runtime 映射 tab 在选中 ACTIVE 映射后，用 `memoryLane=RUNTIME_USER` 和映射的 `runtimeUserId` 调用 `/api/context/memory/candidates` 列出候选缓冲区，并支持单条采纳/忽略；审核动作仍由后端校验 `context:runtime-user:review` + ACTIVE 映射。该 UI 不列出已采纳的 RUNTIME_USER PRIVATE `context_item`，也不改变 PROJECT_DEV 候选审核、组包预览和 lifecycle 的 lane。**

Phase-2.16 补的是外部 AI Coding / code-scan 候选的证据链入口：**`ContextGovernance.vue` 的候选审核表在每条候选上增加“查审计”，复用 `metadataJson.aiCodingSubmission.submissionId` 或 `traceId` 填充 audit tab 过滤条件并切换到 `tab=audit`；`initialTabFromQuery()` 支持 `/context/governance?...&tab=audit` 深链，便于从一次代码扫描批次追踪候选创建、编辑、采纳或忽略事件。**

Phase-2.17 补的是 AI Coding Gateway manifest 的证据链 discovery：**`AiCodingGatewayController` 的 `endpoints` 增加 `contextCandidateAuditUrlTemplate=/context/governance?tab=audit&projectId={projectId}&traceId={submissionId}`，项目详情的“AI Coding 接入信息”同步展示该模板；外部工具提交候选后可把实际 `submissionId` 带给用户打开同批次 audit 过滤页。该字段只是治理 UI 深链，不新增候选审核 API，也不回显原始 `aiCodingKey`。**

Phase-2.18 补的是 AI Coding Gateway manifest 的提交结果回查 discovery：**`AiCodingGatewayController` 的 `endpoints` 增加 `contextCandidateStatusUrlTemplate=/api/ai-coding/projects/{projectId}/context-candidates?traceId={submissionId}&status=PENDING`，项目详情的“AI Coding 接入信息”同步展示该模板；外部工具提交候选后可用同一个 ReachAI 生成的 `ai-coding-submission-*` `submissionId` 回查仍待审核的 PROJECT_DEV candidates。外部 AI Coding 的 `GET /context-candidates` 只允许按服务端生成的 `ai-coding-submission-*` `traceId/submissionId` 做 PENDING 状态回查，不开放项目候选宽列表；候选详情、采纳、忽略、删除仍归平台登录后的 `/api/context/memory/candidates/**` 管理端审核链路。**

Phase-2.19 补的是 Page Assistant 外部工具 header-auth no-echo：**`AiAssistController.pageAssistantManifest` 在请求通过 `X-ReachAI-AiCoding-Key` / `AiCodingKeyContext` 校验时，不再把原始 key 回显到 Page Assistant endpoint URL、`aiCodingAccess.accessKey` 或 helper 命令；`reachai-page-assistant.ps1` 新增 `-AiCodingKey`（默认 `$env:REACHAI_AI_CODING_KEY`），读取 manifest、注册页面和运行 checks 时统一发送 `X-ReachAI-AiCoding-Key`。**

Phase-2.20 补的是 SDK 快速接入外部工具 header-auth no-echo：**`AiAssistController.onboardingManifest` 在请求通过 `X-ReachAI-AiCoding-Key` / `AiCodingKeyContext` 校验时，不再把原始 key 回显到 `agentProvisioning.provisionAgentUrl` 或 `aiCodingAccess.accessKey`；外部工具继续用同一个 header 调用无 query key 的 provisioning URL。**

Phase-2.21 补的是 Workflow AI Coding prompt header-first：**`PageAssistantWorkflowAiCodingPrompt` 与 Page Assistant Wizard 回传 URL 不再把 `aiCodingKey` 拼入 Workflow AI Coding / report URL；复制给 Cursor/Codex/Claude Code 的执行步骤统一要求 `X-ReachAI-AiCoding-Key` header。workflow-ai-coding skill 与 `docs/Workflow-AI-Coding.md` 同步改为 header-first。**

Phase-2.22 补的是 SDK quick-access prompt header-first：**`SdkAccessWizard` 复制给 Cursor/Codex/Claude Code 的 onboarding prompt 不再把 `aiCodingKey` 拼入 manifest、agent provisioning、access session latest、step report 或 session check URL；认证统一要求 `X-ReachAI-AiCoding-Key` header。reachai-onboarding skill 文档示例同样 header-first。**

Phase-2.23 补的是外部 AI Coding header-only enforcement：**`PlatformAuthInterceptor` 只从 `X-ReachAI-AiCoding-Key` 提取外部工具 key，不再接受 `aiCodingKey` query；`AiAssistController` 不再把 query `aiCodingKey` 视为外部工具鉴权；`AiCodingGatewayController` manifest 认证契约不再暴露 queryParam；Page Assistant Wizard、prompt fixture 与随包 skills/docs 均停止生成或宣传 query-key URL。**

当前已接入：

- `LlmRuntimeMemoryExtractor`：通过 `LlmService` / `ai-model-service` 从用户消息和助手回复中抽取结构化候选。
- `RuntimeMemoryExtractionRequest` / `RuntimeMemoryExtraction` / `RuntimeMemoryExtractor`：抽取接口与结果模型，不绑定 AgentScope / LangGraph4j。
- `RuntimeMemoryCandidateService`：有 `modelInstanceId` 时优先走 LLM 抽取；LLM 失败且用户有显式记忆意图时回退到规则型候选。
- `EmbedChatController`：从 agent/workflow profile 传入 `modelInstanceId`，并把 assistant reply、candidate 数量、candidate IDs、抽取模式写入响应 metadata。
- Candidate 质量门：
  - `ContextMemoryCandidateService` 在写入前抑制同 tenant / user / project / type / content 的有效 PENDING 重复候选。
  - `RuntimeMemoryCandidateService` 在生成 candidate 前通过 `ContextRetrievalService` 查询同一 RUNTIME_USER 访问边界内的 ACTIVE item；类型与内容精确匹配时跳过，返回 `already-remembered`。
  - 对非精确重复但 HYBRID 检索相关的 ACTIVE item，`RuntimeMemoryCandidateService` 会在 `metadataJson.qualitySignals.relatedActiveItems[]` 写入 `itemId` / `itemType` / `title` / `rankScore` / `hitReason` / `scoreBreakdown`，并设置 `reviewHint=related-active-memory`。
- 检索入口：
  - `ContextQueryRequest.retrievalMode` 支持 `HYBRID`；空值仍按原有 KEYWORD 精确短语匹配。
  - `ContextRetrievalService` 在完整 query 不命中时，对 `HYBRID` 请求拆 token 后做确定性 fallback；中文连续文本会补 CJK bigram fallback，并把 `token keyword match` / `tokens=` 写入 `hitReason` 与 `scoreBreakdown`。
  - `RuntimeContextPackageService` 构造 RUNTIME_USER query 时默认 `retrievalMode=HYBRID`，并使用 `runtimeContextQuery`（当前用户消息）。
  - `EmbedChatController` 在 embed chat metadata 中写入 `runtimeContextQuery=request.message()`，使下游组包按当前表达召回相关记忆。
  - `ContextGovernance.vue` 的 PROJECT_DEV 组包预览提供 KEYWORD / HYBRID 模式切换，便于查看命中解释差异。
- Runtime 解释性 metadata：
  - `EmbedChatController` 将 `runtimeContextEnabled`、`runtimeContextItemCount`、`runtimeContextTruncatedCount`、`runtimeContextSkippedReason` 合并进最终 `ChatResponse.metadata`。
  - 命中时写入 `runtimeContextHits` 精简列表：`section` / `itemId` / `itemType` / `title` / `rankScore` / `hitReason` / `scoreBreakdown`。
  - `runtimeContextHits` 不包含 `content`，避免把完整 RUNTIME_USER 私有记忆塞进响应 metadata。
  - `qualitySignals.relatedActiveItems` 同样不包含完整 `content`，只作为审核提示，不参与自动 approve。
- AI Coding Context Candidate：
  - `AiCodingGatewayController` 提供 `GET /api/ai-coding/projects/{projectId}/manifest`，统一暴露 SDK 快速接入、Page Assistant、Workflow AI Coding、Context Candidate 的项目级入口；manifest 返回 header-only 认证约定、endpoint、Context Candidate 提交契约（默认 `PROJECT_DEV/default/CODE/NOTE`、必填字段、允许类型、服务端覆盖字段、trace metadata 规则）、`contextCandidateStatusUrlTemplate` 状态回查模板和 `contextCandidateAuditUrlTemplate` 审计深链模板，不回显原始 `aiCodingKey`；`serverControlledFields` 明确包含 `tenantId/memoryLane/projectId/projectCode/proposedBy/traceId/sessionId/origin/visibility/confidence/trustLevel/expiresAt/userId/globalUserId/externalUserId`。
  - `AiCodingGatewayController`、`AiCodingContextCandidateController` 和 Workflow AI Coding controller 共用 AI Coding 错误响应：缺失 key 返回 401，错误 key 返回 403，领域对象不存在返回 404，避免 discovery 与提交链路表现不一致。
  - `AiCodingExternalAccessPolicy` 集中维护外部 AI Coding 请求的 platform-login bypass allow-list；SDK 快速接入 / Page Assistant 的外部工具入口统一走 `/api/ai-coding/projects/{projectId}/...`，由下游 controller / guard 返回领域内缺 key / 错 key 错误；控制台登录态仍可调用业务向导自己的 `/api/ai-assist/projects/**` 接口。
  - `AiCodingAccessGuard` 统一校验项目级 `aiCodingKey`，同时支持请求上下文 key 与显式 key，供 Workflow AI Coding、`ai-assist` onboarding/session 回传和 Context Candidate 复用；不把原始 key 写入 `proposedBy` 或 audit actor。
  - SDK 快速接入与 Page Assistant 的 header-auth manifest 不回显原始 `aiCodingKey`：endpoint URL 不追加 query key，`aiCodingAccess.accessKey=null`，SDK provisioning 与 Page Assistant helper 都改用 `X-ReachAI-AiCoding-Key`。
  - Workflow AI Coding 的外部工具提示词、skill 和文档均为 header-only：新示例 URL 不包含 `aiCodingKey` query，后端外部工具鉴权也不再接受 query key。
  - SDK 快速接入的外部工具提示词、reachai-onboarding skill 和文档推荐 header-first：manifest、provisioning、access session、step report 与 session check 新示例 URL 不包含 `aiCodingKey` query；外部工具用 `X-ReachAI-AiCoding-Key` 调用。
  - `WorkflowAiCodingAuthService` 作为 Workflow 语义适配层复用 `AiCodingAccessGuard`，避免 Workflow AI Coding 和 Context Candidate 各自维护一套项目 key 校验逻辑。
  - `AiCodingContextCandidateController` 只保留 REST 入口；`AiCodingContextCandidateSubmissionService` 统一处理外部工具提交归一化，支持单条提交和 `/context-candidates/batch` 批量提交；batch 中任一 item 缺少 `content` 会整批拒绝并且不会创建前序候选；外部 `GET /context-candidates` 只做服务端生成的 `ai-coding-submission-*` `traceId/submissionId` + `PENDING` 状态回查，不能用项目级 `aiCodingKey` 宽列表读取项目候选；两者都强制将提交候选绑定为 `memoryLane=PROJECT_DEV`，并固定覆盖 `tenantId=default`、`projectId/projectCode`；缺省 `sourceType=CODE`；同时覆盖客户端传入的 `proposedBy` 为 `aiCodingKey:{projectId}`，避免原始 key、跨租户字段或工具自报身份写入候选 / audit。
  - 审计身份不再用 `memoryLane=PROJECT_DEV` 反推工具身份：外部工具提交候选时 audit `actorType=AI_TOOL`、`actorId=aiCodingKey:{projectId}`；平台用户编辑、采纳、忽略、删除 PROJECT_DEV 候选时 audit `actorType=USER`，`actorId` 使用当前 reviewer / updater。
  - `PROJECT_DEV` 候选不承载 runtime 用户身份：AI Coding 提交层会清空客户端传入的 `userId/globalUserId/externalUserId`，核心 Candidate Service 也会兜底清理；采纳为 `context_item` 时同样不写 `userId`。
  - `PROJECT_DEV` 候选审核链路固定沉淀为 `visibility=PROJECT`；客户端提交或平台编辑时传入的 `PRIVATE/TENANT/GLOBAL` 不会穿透到候选或最终 item。
  - AI Coding Context Candidate 会由服务端生成本次提交的正式 `submissionId`，并写入候选 `traceId/sessionId`：单条和批量提交都生成 `ai-coding-submission-*`；客户端自带的 `traceId/sessionId` 不再作为正式 audit/session 线索，而是分别保留到 `metadataJson.aiCodingSubmission.clientTraceId` 和 `clientSessionId`；客户端自带的 `confidence/trustLevel/expiresAt` 不直接控制候选治理权重和 TTL，只作为 `clientConfidence/clientTrustLevel/clientExpiresAt` 进入 metadata；`origin=AI_CODING_CONTEXT_SCAN`，并在 `metadataJson.aiCodingSubmission` 写入 `schema`、`submissionId`、`entrypoint`、`projectId/projectCode`、`memoryLane`，用于串起一次代码扫描、多条候选、后续审核和 audit。
  - `ContextMemoryCandidateController` 对 `memoryLane=PROJECT_DEV` 走项目范围管理端审核，不再套用 RUNTIME_USER 自服务 `userId` 约束。
  - `ContextGovernance.vue` 增加“候选审核”tab，支持从项目详情 `?tab=candidates` 深链进入；平台用户可查看 PENDING/APPROVED/REJECTED/EXPIRED 候选，按 `submissionId/traceId` 过滤一次代码扫描批次，先编辑 PENDING 候选的标题、内容、类型、来源和 metadata，再执行单条或批量采纳/忽略；管理端批量审核走 `/api/context/memory/candidates/batch/approve` 与 `/batch/reject`，后端会先预检整批候选状态、重复 ID 和对象目标，再由单条候选状态机完成 item/binding/evidence 写入或拒绝，避免半批次采纳/忽略。
  - 候选审核表的“查审计”动作会把该候选的 `submissionId/traceId` 带到 audit tab，审计 tab 可继续叠加日期范围、actor、eventType，用于确认一次外部代码扫描提交后的全链路事件。
  - 项目详情“AI Coding 接入信息”和 manifest `endpoints.contextCandidateStatusUrlTemplate` 都暴露 `{submissionId}` 占位符，供外部工具提交后用 ReachAI 生成的 `ai-coding-submission-*` 批次号回查同批次仍处于 `PENDING` 的候选。
  - 项目详情“AI Coding 接入信息”和 manifest `endpoints.contextCandidateAuditUrlTemplate` 都暴露 `{submissionId}` 占位符，供外部工具提交后回填实际批次并引导用户查看 audit。
  - `ContextGovernance.vue` 会消费深链里的 `projectId/projectCode` 并同步顶部项目 scope；直接打开 `/context/governance?projectId=...&projectCode=...&tab=candidates` 或刷新页面不会退回 tenant 级列表。
  - `ContextMemoryCandidateService` 支持 `PROJECT_DEV` 候选；Workflow/Page/API 候选 approve 时写入对应 binding 与 evidence。

治理边界保持不变：

- LLM / AI Coding 工具只生成 `context_memory_candidate`，状态为 `PENDING`。
- approve 前不写 `context_item`，不进入 Runtime prompt。
- Runtime 智能抽取仍只面向 `RUNTIME_USER`；AI Coding Context Candidate 可显式提交 `PROJECT_DEV`，但不参与 Runtime 注入。
- `/api/ai-coding/projects/{projectId}/manifest` 只做项目级 discovery；`/context-candidates` 只放行 `POST` 根路径和带服务端生成 `ai-coding-submission-*` `traceId/submissionId` 的 `GET` PENDING 状态回查，`/context-candidates/batch` 只放行 `POST` 批量提交；候选详情、采纳、忽略、删除和项目候选宽列表仍归平台登录后的 `/api/context/memory/candidates/**` 管理端审核链路。
- 新增的 RBAC 身份映射仅服务 RUNTIME_USER 候选代审；管理端只开放映射内 candidate 代审，不开放已采纳 `RUNTIME_USER` PRIVATE item 管理，不改变 AI Coding `PROJECT_DEV` 候选提交链路。
- 无 `modelInstanceId` 时保持 Phase-1 规则型显式记忆意图行为。
- HYBRID 检索是确定性 token fallback，不是向量检索、相似度召回、FULLTEXT/ngram 索引或语义合并。
- 这里的 `retrievalMode=HYBRID` 与 agent runtimePlacement 的 `HYBRID` 不是同一件事；runtimePlacement=HYBRID 初始阶段仍 deferred，只有 embedded runtime 失败并回落 central runtime 时才组包注入。
- 解释性 metadata 只返回命中摘要，不返回完整 content，也不改变 Context access policy。
- 当前去重是精确内容去重；`qualitySignals` 只是审核提示。向量相似去重、自动合并策略和语义冲突检测留给后续 Retrieval/Quality 批次。

---

## 核心目标

ReachAI Context Governance Kernel 是面向企业 Agent 的**上下文治理**能力，主语是企业领域对象（租户、项目、用户、Agent、页面、Workflow、权限、审计、可信度、来源证据），不是泛化的“聊天记忆”。

- **主体能力，不绑定 Runtime 框架**：通过 `AgentRuntimeRequest.runtimeContext` / `WorkflowRuntimeRequest.runtimeContext` 注入，AgentScope、LangGraph4j 等仅为适配器。
- **与 `ConversationMemoryService` 独立**：短期会话记忆仍走现有 conversation 链路；Context Kernel 管理可治理、可审计、可检索的**结构化上下文资产**。
- **代码事实优先**：长期上下文须携带 `source_type`、evidence、`confidence`、`trust_level`；过期/未生效/非 ACTIVE 项不进 Package。

---

## 核心对象模型

```
context_namespace（隔离域）
    └── context_item（上下文资产，含 memory_lane / visibility / status）
            ├── context_binding（绑定到 USER/SESSION/AGENT/PAGE/WORKFLOW/PROJECT…）
            └── context_evidence（来源证据）

context_memory_candidate（待确认缓冲，非长期记忆）
    └── approve → context_item + binding + evidence

context_audit_event（治理审计，横切所有操作）
```

**关系要点：**

| 对象 | 职责 |
|------|------|
| `ContextNamespace` | 逻辑隔离域；`namespace_key` 唯一；可有 `owner_type/owner_id` |
| `ContextItem` | 可检索/可组包的原子资产；归属唯一 namespace |
| `ContextBinding` | 将 item 关联到运行时目标（用户、会话、Agent 等） |
| `ContextEvidence` | 证明 item 来源（确认、文档、Trace、API 等） |
| `ContextAuditEvent` | 记录 CREATE/SEARCH/INJECT/APPROVE/LIFECYCLE_RUN 等 |
| `ContextMemoryCandidate` | PENDING 缓冲；approve 前不参与 Runtime |

领域类型与 DB Entity 位于 `com.enterprise.ai.agent.context`；勿与 `GraphRuntimeContext` / `ToolExecutionContext` 混淆。

---

## SQL 表说明

基线：`sql/init.sql`（Context 段）。升级：`sql/upgrade-20260622-context-kernel.sql`、`sql/upgrade-20260622-context-memory-candidate.sql`、`sql/upgrade-20260624-context-audit-project-id-index.sql`、`sql/upgrade-20260624-context-runtime-user-mapping.sql`。

| 表 | 用途 | 关键字段 |
|----|------|----------|
| `context_namespace` | 命名空间 | `namespace_key`, `namespace_type`, `tenant_id`, `project_id`, `project_code`, `owner_type`, `owner_id`, `status` |
| `context_item` | 上下文资产 | `namespace_id`, `item_type`, `memory_lane`, `content`, `visibility`, `status`, `trust_level`, `confidence`, `expires_at`, `stale_after` |
| `context_binding` | 绑定关系 | `item_id`, `bind_type`, `bind_id`, `tenant_id`, `project_id`, `project_code` |
| `context_evidence` | 来源证据 | `item_id`, `evidence_type`, `evidence_ref`, `trace_id` |
| `context_audit_event` | 审计事件 | `event_type`, `item_id`, `tenant_id`, `project_id`, `project_code`, `actor_type`, `actor_id`, `trace_id`, `decision` |
| `context_memory_candidate` | 记忆候选 | `candidate_key`, `memory_lane`, `status`, `user_id`, `session_id`, `expires_at`, `approved_item_id` |
| `context_runtime_user_mapping` | 平台代审身份桥 | `tenant_id`, `platform_user_id`, `runtime_user_id`, `project_id`, `project_code`, `status` |

**检索**：Phase-1 使用 `title` / `summary` / `content` 的 **LIKE**；intentionally 未加 FULLTEXT（中文分词 / ngram 跨版本差异）。

**索引**：`context_audit_event` 同时保留 `(project_code, created_at)` 与 `(project_id, created_at)`，projectCode 与 projectId 查询都走明确索引；历史行是否有 `project_id` 值仍取决于写入时版本。

---

## Memory Lane 模型

| Lane | 语义 | 典型内容 | 管理入口 |
|------|------|----------|----------|
| `PROJECT_DEV` | 项目开发/治理记忆 | 项目背景、页面/API/Workflow 契约、规则、踩坑 | `ContextGovernance.vue` |
| `RUNTIME_USER` | 运行时用户记忆 | 用户偏好、确认事实、页面上下文 | embed 自确认 API |

**隔离规则（代码强制）：**

- `ContextRetrievalService` / `ContextComposerService` 查询强制 `memory_lane` 相等。
- `PROJECT_DEV` 与 `RUNTIME_USER` **不互查、不互包**。
- Runtime 注入 compose 固定 `memoryLane=RUNTIME_USER`；`RuntimeContextPromptFormatter` **不输出** `projectMemory` section。

**为什么不能跨 lane**：开发侧草稿与用户私有记忆混用会导致错误注入、权限穿透和审计语义混乱。

---

## 项目记忆 vs 运行时用户记忆

| 维度 | PROJECT_DEV | RUNTIME_USER |
|------|-------------|--------------|
| 生命周期 | 开发者维护；lifecycle 可 stale | 用户确认后写入；lifecycle **默认不 stale** |
| visibility | PROJECT / TENANT / GLOBAL 等 | 多为 PRIVATE + 强 binding/owner |
| 进入对话 | 仅管理端预览/检索 | CENTRAL Runtime prompt（已 approve ACTIVE） |
| Candidate | AI Coding 工具可提交 PENDING → 人工 approve | 对话后 PENDING → approve |
| Ops Summary | 默认 lane | 须 `includeRuntimeUser=true`，**aggregate-only** |

---

## Namespace / Item / Binding / Evidence / Audit / Candidate

### Namespace

- `ContextNamespaceService` + `ContextKeyFactory` 生成 `namespace_key`。
- `ContextProjectIdentity.namespaceKeyToken`：有 `projectCode` 用 code；仅 `projectId` 用 `pid-{id}`；皆空为 tenant 级 key。
- 显式传入 `namespaceKey` 优先；边界由 access policy 校验。

### Item

- 状态：`ACTIVE` / `STALE` / `REVOKED` / `DELETED`（逻辑删除）。
- `ContextItemService`：CRUD、revoke、stale、verify、lifecycle 系统路径 `markStaleByLifecycle`。
- 管理列表 `GET /api/context/items` vs 检索 `POST /api/context/query`（仅 ACTIVE）语义分离。

### Binding

- `bind_type`：TENANT / PROJECT / USER / AGENT / WORKFLOW / PAGE / API / SESSION 等。
- `RUNTIME_USER + PRIVATE`：须 query 中 `userId`/`sessionId`/`agentId`/`pageInstanceId`/`workflowId` 之一匹配，且 item 有对应 binding 或 namespace owner 一致。

### Evidence

- 每项长期记忆应有可追溯 `source_type` + evidence。
- `ContextEvidenceService` 挂接在 item 下。

### Audit

- `ContextAuditService` 记录操作；`GET /api/context/audit` 支持 `tenantId`、`projectCode`、`projectId`（双条件 AND）、`eventType`、`actorType`、`actorId`、`decision`、`traceId`、`namespaceId`、`itemId`、`dateFrom`、`dateTo`；limit 上限 500。

### Candidate

- `context_memory_candidate`：**不是**长期记忆表。
- **PENDING** 不参与检索、组包、Runtime prompt。
- approve → `ContextItemService.createItem`；`RUNTIME_USER` 写 PRIVATE + USER/SESSION/AGENT/PAGE/WORKFLOW binding，`PROJECT_DEV` 写 PROJECT visibility，并按候选目标写 PAGE/API/WORKFLOW/PROJECT binding + evidence。
- 过期：lifecycle `expire` 或 TTL；状态 `EXPIRED`。

---

## Runtime 只读注入链路

```
EmbedChatController.executeMessage
  → RuntimeContextPackageService.injectForEmbedAgent / injectForEmbedWorkflow
  → ContextComposerService.compose (memoryLane=RUNTIME_USER)
  → RuntimeContextPromptFormatter.format
  → AgentRuntimeRequest.runtimeContext / WorkflowRuntimeRequest.runtimeContext
  → AgentScopeRuntimeAdapter / LangGraph4jRuntimeAdapter
```

| 规则 | 行为 |
|------|------|
| 接入范围 | 仅 `EmbedChatController` 嵌入式 agent/workflow 分叉 |
| placement | `CENTRAL` 注入；`EMBEDDED` 跳过；`HYBRID` 初始 deferred，embedded 失败回落 central 前注入 |
| 写入 | Runtime **只读**；对话成功后可生成 Candidate，不自动写 item |
| 失败 | 组包异常不中断主执行；`RuntimeContextInjectionResult.skippedReason` |
| userId | `globalUserId → externalUserId` 优先级（与 Workflow Runtime Trace 对齐） |
| audit | `INJECT` 在 `compose` 记录；facade 不重复 INJECT |

可选配置 `context.runtime.*`：`enabled`、`max-items`、`token-budget`、各 section 注入开关。

对话成功后（显式记忆意图）：

```
RuntimeMemoryCandidateService
  → 有 modelInstanceId：LlmRuntimeMemoryExtractor 结构化抽取
  → 无 modelInstanceId：规则型显式关键词 fallback
  → PENDING candidate
```

---

## Candidate Approval 链路

### 生成（Runtime）

- 触发：embed 对话成功 + 显式意图（记住/请记住/我的偏好是/我喜欢…）。
- 产出：`context_memory_candidate`，`status=PENDING`，默认 TTL 7 天。
- metadata：`memoryCandidateCreated` / `memoryCandidateId` / `memoryCandidateSkippedReason`。

### 确认（两条 API 面，不可混用）

**Embed 用户（生产主路径）**

- `EmbedMemoryCandidateController`：`/api/embed/sessions/{sessionId}/memory/candidates/*`
- 身份：`RuntimeUserIdentityResolver` + `EmbedSessionService.requireActiveSession`
- 客户端不可传 `tenantId` / `userId` / `reviewedBy`

**Platform 用户（self-service，非 embed 私有记忆审核）**

- `ContextMemoryCandidateController`：`/api/context/memory/candidates/*`
- 身份：`PlatformAuthContext` / `PlatformPrincipal.userId`
- `userId` / `reviewedBy` 必须等于当前登录用户

approve 约束：

- namespace 必须 `namespaceType=USER`，`ownerType=USER`，`ownerId=resolvedUserId`
- `ContextProjectIdentity.requireMatch` 校验 project 边界
- 审计：`CANDIDATE_CREATE` / `UPDATE` / `APPROVE` / `REJECT` / `DELETE` / `EXPIRE`；外部 AI Coding 提交是 `AI_TOOL`，平台确认/编辑动作是 `USER`

---

## Embed API 与 Platform API 身份边界

| | Embed | Platform |
|---|-------|----------|
| Session 门禁 | `requireActiveSession`（tenant/app/project/agent/user/page 一致） | `PlatformPrincipal` |
| 用户标识 | `globalUserId` / `externalUserId` | `PlatformPrincipal.userId` |
| 适用场景 | 嵌入式终端用户确认自己的记忆 | 平台登录用户 self-service candidate；具备权限与映射时可代审指定 runtime 用户 candidate |
| 禁止 | 无 session / claims 的自报用户身份 | 无 `context_runtime_user_mapping` 时用 platform userId 冒充 embed globalUserId |

**平台代审边界**：`ContextRuntimeUserMappingController` 的 `/api/context/runtime-user-mappings` 由 `platform:admin` / `*` / `context:runtime-user:mapping:manage` 维护映射；`ContextRuntimeUserAccessService` 同时检查平台代审权限与 ACTIVE `context_runtime_user_mapping`；映射可为 tenant-wide（project 字段为空）或项目级（匹配 `projectCode` / `projectId`）。管理端只在 Runtime 映射 tab 中开放选中映射后的 candidate 代审，不开放已采纳 RUNTIME_USER PRIVATE item 列表。

`requireActiveSession` 强校验：tenantId、appId、projectCode、agentId、externalUserId、globalUserId、pageInstanceId、pageKey；status=ACTIVE、未过期。`route`/`origin` 会话期不强校验（避免 SPA 导航误伤）。

---

## 管理端 PROJECT_DEV 治理页

| 项 | 值 |
|----|-----|
| 页面 | `ai-admin-front/src/views/context/ContextGovernance.vue` |
| 路由 | `/context/governance`（`MainLayout` 菜单：治理运维 / 上下文治理） |
| API 封装 | `ai-admin-front/src/api/context.ts`、`src/types/context.ts` |
| Lane | 固定 `PROJECT_DEV` |
| 深链 scope | `?projectId=...&projectCode=...` 会同步 `ProjectStore` 并作为请求 fallback |

**页面能力：**

- Namespace 列表/创建/详情/删除
- Item 列表（含状态筛选）、创建/编辑/revoke/stale/verify/删除
- 类型化快捷创建（项目背景、页面、API、Workflow、规则）
- 详情：Evidence 增查、Binding 只读、Item 级 Audit
- 组包预览：`POST /api/context/package`（不渲染 `userMemory`）
- 运维摘要：`GET /api/context/ops/summary`（传 `projectId`）
- 管理端 Lifecycle：**仅 dryRun**（`POST /api/context/lifecycle/run`）
- Audit 列表（含 `projectId` 过滤）
- 候选到审计：从候选行按 `submissionId/traceId` 跳转到 Audit tab
- Runtime 映射：维护代审授权，并在选中 ACTIVE 映射后审核对应 RUNTIME_USER candidate 缓冲区

**边界：** 无 RUNTIME_USER 已采纳私有记忆条目管理 UI；Runtime 映射 tab 只开放代审授权和映射内 candidate 采纳/忽略；无 lifecycle 真实执行按钮；Binding 创建在 PROJECT_DEV 页只读。

---

## Lifecycle / Ops Summary / Audit

### Lifecycle

- `POST /api/context/lifecycle/run`：`ContextLifecycleService`
- Candidate expiry：`PENDING` 且 `expiresAt <= now`
- Item stale：`ACTIVE` 且 `staleAfter <= now`，**仅 PROJECT_DEV**；RUNTIME_USER 默认跳过
- `dryRun=true` 只计数；`markStaleByLifecycle` 系统路径（`actorType=SYSTEM`）
- `includeRuntimeUserItems=true` 本阶段仅 warning，不实际 stale RUNTIME_USER
- `ContextLifecycleScheduler`：默认 `eaf.context.lifecycle.enabled=false`；开启后按 `eaf.context.lifecycle.fixed-delay-ms` 调用同一 `ContextLifecycleService.run`，支持 `tenant-id`、`project-code`、`project-id`、`dry-run`、`candidate-expire-limit`、`item-stale-limit`。

### Ops Summary

- `GET /api/context/ops/summary`：`ContextOpsSummaryService`
- 默认 `memoryLane=PROJECT_DEV`；item / candidate 使用 Mapper `selectCount` 做 SQL 条件聚合，不加载明细
- `memoryLane=RUNTIME_USER` 且 `includeRuntimeUser=false` → **400**
- `includeRuntimeUser=true` → 仅聚合计数 + warning：`aggregate-only; private item details are not exposed`
- `auditEventCountRecent`：24h，按 tenant + 可选 `projectCode` / `projectId`

### Audit

- 写入：item CRUD、search、inject、lifecycle、candidate 等
- 查询：见上文；`projectCode` + `projectId` 同时存在时双条件 AND

---

## Retrieval Scoring / scoreBreakdown

`ContextRetrievalService` 多因素确定性排序（无新依赖）：

| 因素 | 权重/说明 |
|------|-----------|
| title keyword | 3.0 |
| summary keyword | 2.0 |
| content keyword | 1.0 |
| itemType 命中 | 0.5 |
| trustLevel | VERIFIED 1.0 / HIGH 0.7 / MEDIUM 0.4 / LOW 0.2 |
| confidence | × 0.8 |
| lastVerifiedAt / updatedAt | 7d/30d 阶梯加分 |
| sourceType | USER_CONFIRMED/MANUAL/DOC/CODE/API 轻微加分 |
| binding match | +0.2 |

- `rankScore` 四位小数；`hitReason` 主原因；`scoreBreakdown` 分项字符串
- 无 keyword 时按 trust/confidence/recency 排序
- lane/tenant/project/RUNTIME_USER PRIVATE 隔离不变

---

## 安全模型

| 边界 | 规则 |
|------|------|
| Tenant | 硬边界；不跨 tenant |
| Project | `ContextProjectIdentity` 统一 code/id 语义 |
| Lane | 强制相等；默认运维/管理仅 PROJECT_DEV |
| Visibility | PRIVATE/PROJECT 项目级；TENANT/GLOBAL 同 tenant 跨 project（检索后 policy 过滤） |
| RUNTIME_USER PRIVATE | binding 或 namespace owner 强匹配 |
| Scope | 读/写须 `tenantId` + `memoryLane` + 必要 identity；集中 `ContextAccessPolicyService` |
| Candidate | Platform 与 Embed API 分离；不可冒充他人 |
| Ops | RUNTIME_USER summary 默认拒绝；显式 aggregate-only |

**设计原则：** Controller 不散落权限；撤销（REVOKED）保留审计；删除（DELETED）逻辑删除。

---

## API 摘要

| 方法 | 路径 | 说明 |
|------|------|------|
| POST/GET/DELETE | `/api/context/namespaces` | Namespace CRUD |
| POST/GET/PUT/DELETE | `/api/context/items` | Item；GET 为管理列表 |
| POST/GET | `/api/context/items/{id}/bindings` | Binding |
| POST/GET | `/api/context/items/{id}/evidence` | Evidence |
| POST | `/api/context/query` | 检索 |
| POST | `/api/context/package` | 组包 |
| GET | `/api/context/audit` | 审计列表 |
| GET | `/api/context/ops/summary` | 运维摘要 |
| POST | `/api/context/lifecycle/run` | 手动 lifecycle |
| POST/GET/PUT/DELETE | `/api/context/memory/candidates` | Platform candidate；`memoryLane=PROJECT_DEV` 时用于管理端项目候选审核；PUT 只允许编辑 PENDING 候选 |
| POST | `/api/context/memory/candidates/batch/approve` | Platform candidate 批量采纳；先预检同一审核 scope 下的 PENDING 候选、重复 ID 和对象目标，再复用单条 approve 状态机 |
| POST | `/api/context/memory/candidates/batch/reject` | Platform candidate 批量忽略；先预检同一审核 scope 下的 PENDING 候选和重复 ID，再复用单条 reject 状态机 |
| GET/POST/DELETE | `/api/embed/sessions/{sessionId}/memory/candidates` | Embed candidate |
| GET | `/api/ai-coding/projects/{projectId}/manifest` | AI Coding Gateway discovery（`aiCodingKey`；不回显原始 key） |
| POST/GET | `/api/ai-coding/projects/{projectId}/context-candidates` | AI Coding Context Candidate（`aiCodingKey`；POST 提交候选；GET 只允许服务端生成的 `ai-coding-submission-*` `traceId/submissionId` + `PENDING` 状态回查） |
| POST | `/api/ai-coding/projects/{projectId}/context-candidates/batch` | AI Coding Context Candidate 批量提交；任一 item 缺少 `content` 时整批拒绝，不创建前序候选 |

响应：`/api/context/**` 与 `/api/embed/**` 使用 `ApiResult<T>`；`/api/ai-coding/**` 面向外部 AI Coding 工具，沿用 Workflow AI Coding 风格返回裸 JSON。

---

## 代码与测试位置

```
ai-agent-service/src/main/java/com/enterprise/ai/agent/context/
ai-agent-service/src/main/java/com/enterprise/ai/agent/context/runtime/
ai-agent-service/src/main/java/com/enterprise/ai/agent/context/memory/
ai-agent-service/src/main/java/com/enterprise/ai/agent/aicoding/
ai-agent-service/src/main/java/com/enterprise/ai/agent/controller/ContextController.java
ai-agent-service/src/main/java/com/enterprise/ai/agent/controller/ContextMemoryCandidateController.java
ai-agent-service/src/main/java/com/enterprise/ai/agent/controller/EmbedMemoryCandidateController.java
ai-agent-service/src/main/java/com/enterprise/ai/agent/controller/AiCodingGatewayController.java
ai-agent-service/src/main/java/com/enterprise/ai/agent/controller/AiCodingContextCandidateController.java
ai-agent-service/src/main/java/com/enterprise/ai/agent/context/ContextLifecycleScheduler.java
ai-agent-service/src/main/java/com/enterprise/ai/agent/controller/EmbedChatController.java
ai-agent-service/src/main/java/com/enterprise/ai/agent/identity/EmbedSessionService.java
ai-admin-front/src/views/context/ContextGovernance.vue
ai-admin-front/src/views/registry/RegistryProjectDetail.vue
ai-admin-front/src/api/context.ts
ai-admin-front/src/types/context.ts
ai-admin-front/scripts/check-context-candidate-ui.mjs
```

测试（节选）：

- `ContextKernelServiceTest`、`ContextRetrievalServiceTest`、`ContextOpsSummaryServiceTest`、`ContextAuditServiceTest`
- `ContextLifecycleServiceTest`、`ContextLifecycleSchedulerTest`、`ContextMemoryCandidateServiceTest`、`RuntimeMemoryCandidateServiceTest`
- `LlmRuntimeMemoryExtractorTest`
- `RuntimeContextPackageServiceTest`、`RuntimeContextPromptFormatterTest`、`EmbedMemoryCandidateRuntimeFlowTest`
- `ContextMemoryCandidateControllerTest`、`ContextControllerLifecycleTest`、`EmbedSessionServiceTest`
- `AiCodingGatewayPathsTest`、`AiCodingGatewayControllerTest`
- `AiCodingContextCandidateSubmissionServiceTest`、`AiCodingContextCandidateControllerTest`
- `AiCodingExternalAccessPolicyTest`、`PlatformAuthInterceptorAuditTest`

AI 精简基线：`docs/ai-memory/CONTEXT-GOVERNANCE-BASELINE.md`。

---

## 第二阶段完成情况与后续建议（具体，非路线图）

| 优先级 | 项 | 说明 |
|--------|-----|------|
| 高 | LLM 记忆抽取 | Phase-2.1 已完成最小可用实现：结构化抽取 → PENDING candidate |
| 高 | Candidate 质量门 | Phase-2.2/2.6 已完成：有效 PENDING 精确去重 + 已确认 ACTIVE 精确去重 + 相关 ACTIVE 记忆审核提示 |
| 高 | HYBRID 检索入口 | Phase-2.3/2.4 已完成：默认 KEYWORD 不变，显式 HYBRID 启用 token/CJK fallback；Runtime CENTRAL 使用当前用户消息 query |
| 高 | Runtime 解释性 metadata | Phase-2.5 已完成：response metadata 返回命中摘要与 skipped reason，不暴露完整 content |
| 中 | HYBRID 注入 | Phase-2.13 已完成：HYBRID 初始 deferred；仅 embedded runtime 失败回落 central 前组包，避免无效 audit |
| 高 | 向量 / FULLTEXT | 后续检索质量增强；评估 Milvus 与 MySQL FULLTEXT/ngram |
| 中 | RBAC + 身份映射 | Phase-2.14/2.15 已完成后端身份桥、映射管理 tab 与映射内 candidate 代审：`context_runtime_user_mapping` + `/api/context/runtime-user-mappings` + `context:runtime-user:review` / `context:runtime-user:mapping:manage` |
| 低 | RUNTIME_USER 私有记忆内容管理 UI | 若开放已采纳 PRIVATE item 管理，须 aggregate-only 或更强 RBAC；当前仅有 candidate 代审 |
| 低 | ConversationMemory 关系 | 明确与 Context Kernel 分工或迁移策略 |
| 低 | 历史 audit 回填 | 已有 `(project_id, created_at)` 索引；历史行 `project_id` 为空时仍需另行回填策略 |
| 低 | 历史 namespaceKey 迁移 | projectId-only 旧 key 并存治理 |

---

## 验收命令

```powershell
$env:JAVA_HOME='C:\Program Files\Java\jdk-17'
$env:Path="$env:JAVA_HOME\bin;$env:Path"
mvn -pl ai-agent-service -DskipTests compile
mvn -pl ai-agent-service "-Dtest=ContextKernelServiceTest,ContextMemoryCandidateServiceTest,RuntimeMemoryCandidateServiceTest,ContextMemoryCandidateControllerTest,ContextRuntimeUserAccessServiceTest,ContextRuntimeUserMappingServiceTest,ContextRuntimeUserMappingControllerTest,AiCodingAccessGuardTest,AiAssistControllerTest,AiCodingContextCandidateControllerTest,PlatformAuthInterceptorAuditTest,RuntimeUserIdentityResolverTest,EmbedMemoryCandidateControllerTest,EmbedMemoryCandidateRuntimeFlowTest,RuntimeContextPackageServiceTest,RuntimeContextPromptFormatterTest,AgentRouterTest,EmbedSessionServiceTest,WorkflowRuntimeServiceTest,ContextLifecycleServiceTest,ContextLifecycleSchedulerTest,ContextRetrievalServiceTest,ContextOpsSummaryServiceTest,ContextAuditServiceTest,ContextControllerLifecycleTest" test
cd ai-admin-front && npm run build
cd ai-admin-front && node scripts/check-context-candidate-ui.mjs
rg -n "context_namespace|context_item|context_memory_candidate|context_runtime_user_mapping|idx_context_audit_project_id" sql/init.sql sql/upgrade-20260622-context-kernel.sql sql/upgrade-20260622-context-memory-candidate.sql sql/upgrade-20260624-context-audit-project-id-index.sql sql/upgrade-20260624-context-runtime-user-mapping.sql
```

---

## 实施阶段索引（历史）

| 阶段 | 交付 |
|------|------|
| Phase-1 | 表结构、CRUD、LIKE 检索、ContextPackage、审计、access policy |
| Phase-2 | Runtime 只读注入（CENTRAL） |
| Phase-3 / 3.1 | Memory Candidate + Platform self-service + USER namespace 硬边界 |
| Phase-4 / 4.1 | Embed 自确认 API + session 硬化 |
| Phase-5 / 5.1 / 5.2 | 管理端 PROJECT_DEV + projectCode/projectId 硬化 |
| Phase-6 | `ContextProjectIdentity`、namespaceKey `pid-{id}`、lifecycle |
| Phase-7 / 7.1 | 检索评分、ops summary、audit 增强、RUNTIME_USER ops gate、audit projectId |
| Phase-2.1（2026-06-24） | LLM 自动记忆抽取 MVP：`LlmRuntimeMemoryExtractor` → `RuntimeMemoryCandidateService` → PENDING candidate |
| Phase-2.2（2026-06-24） | Candidate 质量门：PENDING 精确重复抑制 + ACTIVE 已确认记忆重复抑制 |
| Phase-2.3（2026-06-24） | HYBRID 检索入口：`ContextQueryRequest.retrievalMode=HYBRID` + token fallback 解释 |
| Phase-2.4（2026-06-24） | Query-aware Runtime 检索：当前用户消息 → `runtimeContextQuery` → RUNTIME_USER HYBRID 组包；管理端预览可切换 KEYWORD/HYBRID |
| Phase-2.5（2026-06-24） | Runtime Context 可解释 metadata：最终 `ChatResponse.metadata.runtimeContextHits` 返回命中摘要，不返回完整 content |
| Phase-2.6（2026-06-24） | Candidate 审核质量信号：非精确但相关的 ACTIVE 记忆写入 `metadataJson.qualitySignals.relatedActiveItems`，辅助审核且不自动合并 |
| Phase-2.7（2026-06-24） | AI Coding Gateway + Context Candidate：`/api/ai-coding/projects/{projectId}/manifest` 做项目级 discovery；`/context-candidates` 与 `/batch` 提交 `PROJECT_DEV` PENDING 候选；项目详情可深链进入 Context Governance 候选审核 tab，编辑后单条或批量采纳并写入 context item + binding + evidence |
| Phase-2.8（2026-06-24） | Context Audit projectId 索引补强：`context_audit_event` 增加 `idx_context_audit_project_id(project_id, created_at)`，让 projectId-only 治理筛选不再依赖 projectCode 索引 |
| Phase-2.9（2026-06-24） | Context Governance 深链 scope 补强：治理页消费 `projectId/projectCode` query 并同步 `ProjectStore`，直接打开候选审核深链时仍保持项目范围 |
| Phase-2.10（2026-06-24） | Context Audit 日期范围补强：`ContextAuditListRequest` / `/api/context/audit` / `ContextGovernance.vue` 支持 `dateFrom/dateTo`，便于按时间窗口追踪外部 AI Coding / code-scan 候选提交与审核链路 |
| Phase-2.11（2026-06-24） | Context Lifecycle Scheduler：新增 `ContextLifecycleScheduler`，默认关闭；开启 `eaf.context.lifecycle.enabled=true` 后复用 `ContextLifecycleService.run` 定期执行 candidate expire 与 PROJECT_DEV item stale，可配置 tenant/project、dryRun 与批量 limit |
| Phase-2.12（2026-06-24） | Context Ops Summary SQL 聚合：`ContextOpsSummaryService` 对 item/candidate 统计改用 Mapper `selectCount` 条件聚合，不再加载明细遍历；RUNTIME_USER 仍为 aggregate-only |
| Phase-2.13（2026-06-24） | HYBRID runtime 注入时机：HYBRID embed 入口先 deferred；embedded runtime 成功不组包，失败回落 central runtime 前调用 `injectForCentralFallback` 并注入 central adapter |
| Phase-2.14（2026-06-24） | Context Runtime User 代审身份桥：`context_runtime_user_mapping` + `/api/context/runtime-user-mappings` + `ContextRuntimeUserAccessService`；平台用户需有 `context:runtime-user:review` 或 admin 权限且存在 ACTIVE 映射，才可代审指定 RUNTIME_USER candidate；映射维护需 `context:runtime-user:mapping:manage` 或 admin |
| Phase-2.15（2026-06-24） | Context Runtime User 候选代审 UI：`ContextGovernance.vue` Runtime 映射 tab 支持选中 ACTIVE 映射后按 `memoryLane=RUNTIME_USER` + `runtimeUserId` 查看 candidate 缓冲区并采纳/忽略；不展示已采纳 PRIVATE item |
| Phase-2.16（2026-06-24） | Context Candidate 审计追踪 UI：候选行“查审计”按 `submissionId/traceId` 切到 Audit tab；`tab=audit` 深链可直接打开审计过滤页 |
| Phase-2.17（2026-06-24） | AI Coding Gateway 审计深链模板：manifest `endpoints.contextCandidateAuditUrlTemplate` 与项目详情复制面板暴露 `traceId={submissionId}` 的 Audit tab URL 模板 |
| Phase-2.18（2026-06-24） | AI Coding Gateway 候选状态回查模板：manifest `endpoints.contextCandidateStatusUrlTemplate` 与项目详情复制面板暴露 `traceId={submissionId}&status=PENDING` 的候选列表 URL 模板 |
| Phase-2.19（2026-06-24） | Page Assistant header-auth no-echo：`pageAssistantManifest` 对 header/context key 不回显 raw key，helper script 支持 `-AiCodingKey` / `REACHAI_AI_CODING_KEY` 并用 header 调用 manifest、register、checks |
| Phase-2.20（2026-06-24） | SDK onboarding header-auth no-echo：`onboardingManifest` 对 header/context key 不回显 raw key，`agentProvisioning.provisionAgentUrl` 不追加 query key，外部工具用 `X-ReachAI-AiCoding-Key` 调 provisioning |
| Phase-2.21（2026-06-24） | Workflow AI Coding prompt header-first：页面助手 AI Coding prompt、workflow-ai-coding skill 与文档示例不再把 key 拼进 Workflow/report URL，统一要求 `X-ReachAI-AiCoding-Key` header |
| Phase-2.22（2026-06-24） | SDK quick-access prompt header-first：SDK 接入向导复制 prompt 与 reachai-onboarding skill 文档示例不再把 key 拼进 manifest、provisioning、access session、step report 或 session check URL，统一要求 `X-ReachAI-AiCoding-Key` header |
| Phase-2.23（2026-06-24） | External AI Coding header-only enforcement：`PlatformAuthInterceptor`、`AiAssistController`、`AiCodingGatewayController`、Page Assistant prompt 与随包 skills/docs 均移除 `aiCodingKey` query 鉴权与 discovery |
