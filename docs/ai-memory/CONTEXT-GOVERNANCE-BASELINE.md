# Context Governance Baseline (AI Memory)

给 AI 编程工具看的 **Context Governance Kernel v1 + Phase-2.1 至 Phase-2.22** 精简基线。权威长文档：`docs/Context-Governance-Kernel.md`。

## 一句话定位

**ReachAI Context Governance Kernel v1** 是企业 Agent **上下文治理底座**：结构化存储、访问边界、审计证据、运行时只读注入、候选记忆确认、PROJECT_DEV 管理页；**不是** `ConversationMemoryService` 的替代，也不绑定 AgentScope / LangGraph4j。

## 第一阶段已完成能力

| 域 | 事实 |
|----|------|
| 存储 | 6 张核心上下文表：`context_namespace` / `context_item` / `context_binding` / `context_evidence` / `context_audit_event` / `context_memory_candidate`；另有代审身份桥 `context_runtime_user_mapping` |
| 服务 | namespace / item / binding / evidence / audit / retrieval / compose / lifecycle / ops summary |
| Lane | `PROJECT_DEV` 与 `RUNTIME_USER` 逻辑隔离 |
| Runtime | `RuntimeContextPackageService` → `ContextComposerService` → `RuntimeContextPromptFormatter` → `AgentRuntimeRequest.runtimeContext` / `WorkflowRuntimeRequest.runtimeContext` |
| Candidate | 对话成功后 LLM 抽取或规则型显式意图 → PENDING candidate → approve 写 `RUNTIME_USER` + PRIVATE item；相关 ACTIVE 记忆仅作为审核 quality signal |
| Embed | `EmbedMemoryCandidateController` bearer+session 自确认 |
| 管理端 | `ContextGovernance.vue` `/context/governance`，固定 `PROJECT_DEV` |
| Ops | lifecycle dryRun/run/定时 job、ops summary（SQL count 聚合）、audit 过滤（含 `projectId`、`dateFrom/dateTo`） |
| 检索 | 多因素 `rankScore` + `hitReason` + `scoreBreakdown`；默认 KEYWORD 精确短语，显式 `HYBRID` 可 token/CJK fallback（无向量/FULLTEXT） |

代码根：`ai-agent-service/src/main/java/com/enterprise/ai/agent/context/`。

## 两条 Lane 的边界

| Lane | 用途 | 谁能管 | 能否进 Runtime prompt |
|------|------|--------|----------------------|
| `PROJECT_DEV` | 项目背景、页面/API/Workflow 契约、规则、开发治理记忆 | 管理端 `ContextGovernance.vue` | **否**（lane 隔离 + formatter 不输出 projectMemory） |
| `RUNTIME_USER` | 用户偏好/事实/页面上下文等运行时私有记忆 | embed 用户自确认 API | **是**（仅 CENTRAL、仅已 approve 的 ACTIVE item） |

**为什么不能混用**：检索/组包强制 `memory_lane` 相等；跨 lane 混查会破坏租户/项目/用户隔离，且会把开发草稿注入用户对话。`PROJECT_DEV` 候选和 item 不承载 runtime `userId/globalUserId/externalUserId`；这些字段只属于 `RUNTIME_USER`。AI Coding / Candidate 审核链路中的 `PROJECT_DEV` visibility 固定归一为 `PROJECT`。

## Runtime 注入规则

- **接入点**：`EmbedChatController.executeMessage`（agent 分叉 + workflow 分叉）；**不是** `AgentRouter.route` / 非嵌入式统一注入。
- **placement**：`CENTRAL` 直接组包；`EMBEDDED` → `embedded-placement`；`HYBRID` 初始返回 `hybrid-placement-deferred`，仅 embedded runtime 失败回落 central runtime 前调用 `injectForCentralFallback` 组包。
- **lane**：compose 固定 `memoryLane=RUNTIME_USER`。
- **query**：embed chat 当前用户消息写入 `runtimeContextQuery`；Runtime CENTRAL 组包用该消息作为 query，并默认 `retrievalMode=HYBRID`。
- **只读**：Runtime 不自动写长期记忆；失败跳过主链路（`skippedReason`）。
- **PENDING candidate 不进 prompt**；approve 后的 item 下轮可命中。
- **解释性 metadata**：最终 `ChatResponse.metadata` 会合并 `runtimeContextEnabled` / `runtimeContextItemCount` / `runtimeContextTruncatedCount` / `runtimeContextSkippedReason`；命中时有 `runtimeContextHits[]`。
- `runtimeContextHits[]` 只含 `section`、`itemId`、`itemType`、`title`、`rankScore`、`hitReason`、`scoreBreakdown`，不含完整 `content`。

## Candidate 规则

```
对话成功 → RuntimeMemoryCandidateService
  → 有 modelInstanceId：LlmRuntimeMemoryExtractor 从 user message + assistant reply 结构化抽取
  → 无 modelInstanceId：规则型显式意图（记住/请记住/我的偏好是…）
  → context_memory_candidate (PENDING, TTL 默认 7d)
  → embed 或 platform 用户 approve
  → USER namespace + context_item (RUNTIME_USER, PRIVATE) + binding + evidence
```

- approve 前：**不是**长期记忆，不参与检索/组包。
- approve 必须 USER namespace + `ownerType=USER` + `ownerId=resolvedUserId`。
- Phase-2.1 仅生成 `RUNTIME_USER` candidate；LLM 不直接写 `context_item`，不抽取 `PROJECT_DEV`。
- 写入 candidate 前会抑制同 tenant/user/project/type/content 的有效 PENDING 精确重复。
- Runtime 生成 candidate 前会通过 `ContextRetrievalService` 在同一 RUNTIME_USER scope 内查 ACTIVE item；类型与内容精确匹配时返回 `already-remembered`，不再生成 PENDING。
- Runtime 生成 candidate 时会用 `retrievalMode=HYBRID` 查看同 scope 已确认记忆；非精确但相关的 ACTIVE item 会写入 `metadataJson.qualitySignals.relatedActiveItems[]`，只含 `itemId`、`itemType`、`title`、`rankScore`、`hitReason`、`scoreBreakdown`，并带 `reviewHint=related-active-memory`。
- 这些都是精确去重或审核提示，不是向量相似去重、自动合并策略或语义冲突检测。

## Retrieval 规则

- `POST /api/context/query` 使用 `ContextQueryRequest.retrievalMode`。
- 空 `retrievalMode` 仍是原有 KEYWORD 行为：完整 query 在 title/summary/content 中做确定性包含匹配。
- `retrievalMode=HYBRID` 时，完整 query 不命中后才拆 token fallback；连续中文会补 CJK bigram fallback；命中会带 `token keyword match` 与 `tokens=` score breakdown。
- `ContextGovernance.vue` 的组包预览可切换 KEYWORD / HYBRID；Runtime CENTRAL 默认 HYBRID。
- Phase-2.3/2.4 不是 Milvus、向量相似召回、MySQL FULLTEXT/ngram，也没有新增 SQL。
- 注意：`retrievalMode=HYBRID` 与 agent runtimePlacement=`HYBRID` 不是同一概念；后者只在 embedded → central fallback 时触发 Runtime Context 注入。

## Identity 规则

| API 面 | 身份来源 | 禁止 |
|--------|----------|------|
| Platform `ContextMemoryCandidateController` | `PlatformPrincipal.userId`；代审时还需 `context_runtime_user_mapping` | 无映射时传他人 userId/reviewedBy |
| Embed `EmbedMemoryCandidateController` | `RuntimeUserIdentityResolver`（session+claims → globalUserId/externalUserId） | 客户端传 tenantId/userId/reviewedBy |
| 共用门禁 | `EmbedSessionService.requireActiveSession` | memory API 不另写 session 校验 |

**代审边界**：`/api/context/runtime-user-mappings` 维护映射，需 `context:runtime-user:mapping:manage`（或 `platform:admin` / `*`）；`ContextRuntimeUserAccessService` 要求平台账号具备 `context:runtime-user:review`（或 `platform:admin` / `*`）且存在 ACTIVE `context_runtime_user_mapping`，才可代审指定 runtime 用户 candidate。自审仍绑定 `PlatformPrincipal.userId`；管理端 Runtime 映射 tab 可选中 ACTIVE 映射审核对应 candidate 缓冲区，但不开放已采纳 RUNTIME_USER PRIVATE item 管理。

## ProjectIdentity 规则

统一入口：`ContextProjectIdentity`（`matches` / `requireMatch` / `namespaceKeyToken`）。

- 双方都有 `projectCode` / `projectId` 须相等。
- 仅 code vs 仅 id → **不匹配**。
- 单边有项目标识、另一边无 → **拒绝**。
- namespaceKey：code 优先；无 code 有 id → `pid-{projectId}`。
- 旧 projectId-only key **不自动迁移**。

用于：access policy、create 边界、candidate approve、ops/audit 过滤。

## 管理端边界

- 路由：`/context/governance`（菜单：治理运维 / 上下文治理）。
- 条目、PROJECT_DEV 候选、组包、lifecycle 均固定 `memoryLane=PROJECT_DEV`；Runtime 映射 tab 维护代审授权，并可在 ACTIVE 映射内审核 RUNTIME_USER candidate 缓冲区；不展示已采纳 RUNTIME_USER PRIVATE item。
- PROJECT_DEV 候选行可用“查审计”按 `metadataJson.aiCodingSubmission.submissionId` / `traceId` 切到 Audit tab；`tab=audit` 深链可直接打开审计过滤页，用于追踪一次 AI Coding / code-scan 提交批次。
- AI Coding Gateway manifest `endpoints.contextCandidateAuditUrlTemplate` 与项目详情复制面板暴露 `traceId={submissionId}` 的 Audit tab URL 模板；这是治理 UI 深链，不是新的候选审核 API。
- AI Coding Gateway manifest `endpoints.contextCandidateStatusUrlTemplate` 与项目详情复制面板暴露 `traceId={submissionId}&status=PENDING` 的候选列表 URL 模板；这是既有 GET collection 的回查 discovery，不是新的候选详情或审核 API。
- Page Assistant header-auth manifest 不回显原始 `aiCodingKey`；helper script 用 `-AiCodingKey` / `REACHAI_AI_CODING_KEY` 发送 `X-ReachAI-AiCoding-Key`；外部工具 URL 不再携带 `aiCodingKey` query。
- SDK onboarding header-auth manifest 不回显原始 `aiCodingKey`；`agentProvisioning.provisionAgentUrl` 不追加 query key，外部工具用同一个 `X-ReachAI-AiCoding-Key` header 调 provisioning。
- Workflow AI Coding prompt / skill / docs header-only：示例 URL 不拼 `aiCodingKey`；外部工具用 `X-ReachAI-AiCoding-Key` 调 Workflow AI Coding 与页面助手 result 回传。
- SDK quick-access prompt / reachai-onboarding skill / docs header-only：示例 URL 不拼 `aiCodingKey`；外部工具用 `X-ReachAI-AiCoding-Key` 调 onboarding manifest、agent provisioning、access session、step report 与 session check。
- `/context/governance?projectId=...&projectCode=...&tab=candidates` 会同步 `ProjectStore` 并用 query 作为 scope fallback，直接打开或刷新深链不应退回 tenant 级。
- 能力：namespace/item/evidence/binding 只读、audit（含日期范围）、package 预览、lifecycle **dryRun only**、ops summary（item/candidate 使用 `selectCount` 聚合）；后台 `ContextLifecycleScheduler` 可配置开启真实定时 run。
- `GET /api/context/items` = 管理列表（含非 ACTIVE）；`POST /api/context/query` = ACTIVE 检索，语义分离。

## 不要踩的坑

1. 在 Runtime 注入里查 `PROJECT_DEV` 或把 projectMemory 打进 prompt。
2. 让 PENDING candidate 参与 compose / 检索。
3. 用 Platform API 审核 embed runtime 用户的 candidate。
4. `memoryLane=RUNTIME_USER` 调 ops summary 却未设 `includeRuntimeUser=true`（400）。
5. projectId-only 项目只传 `projectCode` 不传 `projectId`（audit/ops 会偏 tenant 级）。
6. 仅 `projectCode` vs 仅 `projectId` 当同一项目匹配。
7. 把 Context Kernel 当成 ConversationMemory 迁移完成。
8. Controller 散落权限判断——Context item 边界改 `ContextAccessPolicyService`；RUNTIME_USER candidate 代审边界改 `ContextRuntimeUserAccessService`。
9. `expiringItemCount` 实际统计 `expiresAt <= now`（已过期），命名略歧义。
10. `context_audit_event` 已有 `(project_id, created_at)` 索引；历史行 `project_id` 为空时仍不能只靠索引解决 projectId-only 查询。
11. 把 `runtimeContextHits` 当成完整记忆内容传输通道；它只是解释性摘要，完整注入仍只在 runtime prompt 内部完成。
12. 把 `qualitySignals.relatedActiveItems` 当成自动合并/冲突判断；它只是 candidate 审核提示，且不带完整 content。

## 后续阶段入口（建议）

向量/FULLTEXT、RUNTIME_USER 已采纳私有记忆条目管理 UI（若做）、`ConversationMemoryService` 关系梳理、历史 audit `project_id` 回填策略。

验证见 `docs/Context-Governance-Kernel.md` 验收命令；Context 相关测试在 `ai-agent-service/src/test/java/com/enterprise/ai/agent/context/`。
