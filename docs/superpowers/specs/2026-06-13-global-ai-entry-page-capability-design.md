# 项目全局 AI 入口与页面能力闭环改造方案

## 背景

当前 ReachAI 已经有两条接入链路：

- 项目级 `AI 快速接入`：通过 `SdkAccessWizard.vue` 引导业务系统完成 SDK、网关路由、embed token broker 和前端 Chat Embed 接入。
- 页面级 `创建页面助手`：通过 `PageAssistantWizard.vue` 和 `pageAssistantOnboardingPrompt.ts` 引导业务前端为某个具体页面注册 Page Bridge、页面动作和自检结果。

这两条链路目前边界清楚，但闭环不完整：项目级接入完成后，业务系统有了可嵌入对话能力；页面助手接入完成后，平台有了页面动作目录；两者之间还缺少一个稳定的产品与运行时关系，让用户理解“一个全局 AI 按钮如何在不同页面自动获得不同能力”。

## 目标

本次改造目标是形成一个统一闭环：

1. 业务系统只需要接入一个全局 AI 按钮或 Chat Widget 入口。
2. 该入口默认绑定一个项目级入口 Agent。
3. 每个业务页面通过页面助手接入注册当前页面动作。
4. 平台把页面动作挂载到项目级入口 Agent 上。
5. 用户在任意已接入页面打开同一个 AI 按钮时，Agent 根据当前 `pageKey / route / pageInstanceId / origin` 自动获得当前页面可用动作。
6. 页面动作仍然只作用于当前 `pageInstanceId`，不能广播到其他页面或其他浏览器标签页。

一句话：按钮是全局的，能力是随页面切换的，会话和动作执行仍然是当前页面实例级别的。

## 非目标

- 不把每个页面都做成独立入口按钮。
- 不要求每个页面都创建独立 Agent。
- 不让 `embedToken` 跨页面、跨用户、跨 Agent 复用。
- 不允许前端保存 `appSecret` 或在浏览器侧做项目级签名。
- 不把页面动作变成后端 Tool；页面动作仍然是当前业务前端 Page Bridge 执行的 UI 行为。
- 不在本阶段引入复杂多 Agent 调度。后续可以把项目入口 Agent 演进为分发 Agent，但首期只做单入口加页面能力路由。

## 推荐方案

采用“项目全局入口 Agent + 页面能力路由”。

项目级 `AI 快速接入` 负责创建或选择一个项目入口 Agent，并指导业务前端在应用主布局挂载全局 AI 按钮。业务前端打开对话时必须传入当前页面上下文：

- `projectCode`
- `agentId`，即项目入口 Agent 的 `id` 或 `keySlug`
- `pageKey`
- `route`
- `origin`
- `pageInstanceId`

页面级 `创建页面助手` 负责把当前页面的动作注册到 `eaf_page_registry` 和 `eaf_page_action_registry`，并默认把这些动作授权给项目入口 Agent。运行时根据当前会话的 `projectCode + pageKey + pageInstanceId + agentId` 解析可用页面动作。

## 核心概念

### 项目入口 Agent

项目入口 Agent 是业务系统全局 AI 按钮默认使用的 Agent。

建议复用 `agent_definition`，不新增 Agent 主表。通过现有字段表达归属和用途：

- `project_id` / `project_code`：归属项目。
- `key_slug`：建议默认生成 `projectCode + "-entry-assistant"`，允许用户修改。
- `visibility`：项目内可见。
- `runtime_placement`：保持现有嵌入运行语义需要的值，优先沿用当前 Agent 创建逻辑。
- `runtime_config_json` 或 `default_resource_config_json`：记录 `embedEntry=true`、`entryType="PROJECT_GLOBAL"`、默认页面能力策略等扩展信息。

是否新增 `agent_definition.entry_type` 字段可以在实现前再定。首选不新增字段，用 JSON 扩展和查询约定降低 schema 改动；如果查询和 UI 过滤变得不清楚，再新增 `entry_type VARCHAR(32)`。

### 页面能力包

页面能力包不是新表，而是当前已有页面目录与动作目录的组合：

- `eaf_page_registry`：页面身份、路由、origin、当前页面实例和元数据。
- `eaf_page_action_registry`：动作标识、标题、描述、参数 schema、确认策略、动作授权白名单。

页面助手接入成功后，一个页面能力包至少包含：

- 一个 `ACTIVE` 页面记录。
- 一个或多个 `ACTIVE` 页面动作。
- 每个高风险动作的 `confirm_required=true`。
- `allowed_agent_ids_json` 默认包含项目入口 Agent 的 `id` 或 `keySlug`。

### 当前页面上下文

当前页面上下文由业务前端运行时产生，不由平台猜测：

- `pageInstanceId`：每个浏览器页面实例唯一。不同标签页必须不同。
- `pageKey`：业务页面稳定标识，来自页面助手接入或前端配置。
- `route`：当前路由，可用于兜底匹配和审计。
- `origin`：当前业务前端 origin，用于 embed 策略和页面目录隔离。

`pageKey` 应成为页面能力路由的一等参数。当前已有表有 `page_key`，但 `eaf_embed_session` 是否持久化 `page_key` 需要实现时检查。如果没有，应新增 `page_key` 或在 `metadata_json` 中明确落库。更推荐新增列，因为审计、查询和调试都会用到。

## 产品流程

### 1. 项目详情页

项目详情页应把三个动作串成一个闭环，而不是两个孤立入口：

- `AI 快速接入`：接入项目全局 AI 入口。
- `创建页面助手`：接入某个业务页面的页面能力。
- `页面能力挂载状态`：展示哪些页面已挂载到项目入口 Agent，哪些页面动作还没有授权或缺少前端验证。

推荐在项目详情页展示一个“AI 入口闭环”状态区：

- 全局入口 Agent：已创建 / 未创建。
- 网关与 token broker：已完成 / 待确认。
- 前端全局按钮：已接入 / 待验证。
- 已接入页面数。
- 已启用页面动作数。
- 最近一次页面动作自检结果。

### 2. AI 快速接入向导

`SdkAccessWizard.vue` 的“使用 AI 快速接入”逻辑必须同步改造。

现有提示词主要告诉 Cursor / Codex / Claude Code 接入 SDK、网关、token broker 和某个 `defaultAgentId`。改造后，它应该明确交付物是“项目全局 AI 入口”，而不是“随便找一个可嵌入 Agent”。

向导需要增加或调整：

- manifest 中提供 `embed.entryAgentId` / `embed.entryAgentKeySlug` / `embed.entryAgentName`。
- 如果项目还没有入口 Agent，平台应提供创建入口 Agent 的操作，或在向导中明确要求先创建。
- 前端示例从页面局部挂载改为应用主布局或全局 Shell 挂载。
- `tokenProvider` 示例必须传 `pageKey / route / origin / pageInstanceId`。
- AI 提示词必须要求业务前端建立页面上下文解析函数，例如 `resolveReachAiPageContext()`。
- AI 提示词必须说明：全局按钮所有页面可见，但只有已完成页面助手接入的页面才会显示可操作页面能力。
- 自检增加“全局按钮挂载”和“当前页面上下文传递”两项。

提示词中的关键语义应改成：

```text
你要完成的是 ReachAI 项目全局 AI 入口接入。请在业务前端主布局接入一个全局 Chat 按钮，默认使用 manifest.embed.entryAgentKeySlug。每次打开对话或刷新 token 时，必须从当前路由生成 pageKey、route、origin、pageInstanceId，并传给业务后端 token broker。不要为每个页面创建不同按钮；页面能力由后续“创建页面助手”接入后自动挂载到该项目入口 Agent。
```

同时保留已有安全要求：

- appSecret 只能在服务端或密钥管理器。
- token broker 用业务登录态识别当前用户。
- `/api/reachai/embed/**` 代理流量必须透传 ReachAI embed token。
- `X-ReachAI-Invocation-Token`、`X-ReachAI-Trace-Id`、`X-ReachAI-Run-Id` 必须按现有规则透传。

### 3. 创建页面助手向导

`PageAssistantWizard.vue` 的定位也要同步变清楚：

它不是创建另一个入口，而是给项目入口 Agent 增加当前页面能力。

需要调整：

- 标题或说明从“创建页面助手”逐步转向“接入页面能力”或“创建页面能力助手”。如果保留“页面助手”名称，副标题必须说明它会挂载到项目全局 AI 入口。
- 选择页面动作时展示“将授权给：项目入口 Agent”。
- 页面动作注册请求默认携带 `allowedAgentIds=[entryAgentId or entryAgentKeySlug]`。
- 接入进度卡片增加“已挂载到全局入口 Agent”状态。
- 自检增加“入口 Agent 授权”检查，确认动作白名单包含入口 Agent。
- AI 提示词继续保持页面级边界，不做 SDK/网关/token broker，但要把当前页面动作注册到入口 Agent 可用范围。

提示词关键语义应改成：

```text
当前任务是页面能力接入。不要创建新的全局入口按钮，也不要改项目 SDK/网关/token broker。你需要在当前业务页面注册 Page Bridge 和页面动作，并把动作目录授权给 ReachAI 项目入口 Agent。完成后，同一个全局 AI 按钮在当前页面会自动拥有这些页面动作。
```

### 4. 平台管理与监控

`EmbedOpsMonitor.vue` 或项目详情页中的嵌入监控需要能按“项目入口闭环”查看：

- 入口 Agent 当前允许哪些项目凭证申请 embedToken。
- 当前项目有哪些页面能力包。
- 页面动作是否授权给入口 Agent。
- 最近会话是否携带 `pageKey`。
- 页面动作失败原因是未注册、未授权、未绑定页面实例，还是前端未回传结果。

## 后端改造

### 入口 Agent 发现与创建

在 `AiAssistController.buildEmbedManifest` 或其下沉服务中增加项目入口 Agent 解析逻辑。

建议新增服务方法：

- `findProjectEntryAgent(projectId, projectCode)`
- `ensureProjectEntryAgent(projectId, projectCode)`
- `buildProjectEntryEmbedManifest(project, credential)`

规则：

1. 优先找 `agent_definition.project_id/project_code` 匹配且标记为项目入口的启用 Agent。
2. 如果没有入口标记，则可以兼容当前逻辑，选择第一个启用的项目 Agent 作为临时入口，但 manifest 中要标记 `entryAgentConfigured=false`。
3. 如果用户选择自动创建，则生成一个默认项目入口 Agent。
4. `registry_project_credential.allowed_agent_ids_json` 如果非空，必须包含入口 Agent，否则 SDK 自检给出 FAIL 或 WARN。

### 页面能力解析接口

建议新增只读接口，供 Chat Widget、调试台、自检和管理端复用：

```http
GET /api/embed/page-capabilities?projectCode=...&agentId=...&pageKey=...&route=...&origin=...
Authorization: Bearer <embedToken>
```

或放在平台管理域：

```http
GET /api/platform/embed/page-capabilities
```

首期可以不让业务前端直接调用，而是在创建 Chat Session 时由后端解析并写入 session metadata。无论采用哪种外部形态，都需要一个内部服务：

- 输入：`projectCode`、`agentId`、`pageKey`、`route`、`origin`。
- 输出：当前页面记录、可用页面动作列表、未授权动作列表、匹配方式、诊断信息。

解析规则：

1. 优先按 `projectCode + pageKey + origin` 匹配 ACTIVE 页面。
2. 如果 `pageKey` 为空，按 `routePattern + origin` 兜底匹配。
3. 只返回 `status=ACTIVE` 的动作。
4. 如果动作 `allowed_agent_ids_json` 为空，按当前策略视为项目内可用；如果非空，必须包含当前入口 Agent 的 `id` 或 `keySlug`。
5. 高风险动作继续由 `confirm_required` 控制，不能被入口 Agent 绕过。

### Embed Token 与 Session

`POST /api/embed/token/exchange` 和 `POST /api/embed/chat/sessions` 需要明确支持页面上下文：

- token exchange 请求体包含 `pageKey`、`route`、`origin`、`pageInstanceId`。
- token claims 保留 `pageKey` 或在 session 创建时写入。
- `eaf_embed_session` 持久化 `page_key`，或至少在 `metadata_json` 中可查询。
- session 创建时解析当前页面能力，并将 `allowedPageActions` 写入 session metadata 或独立缓存。

如果要新增 `eaf_embed_session.page_key`，必须同步：

- `sql/init.sql`
- 新增 `sql/upgrade-YYYYMMDD-global-entry-page-capability.sql`
- `sql/README.md`
- `EmbedSessionEntity`
- 相关测试

### 页面动作运行时校验

`EmbedChatController.ensurePageActionAllowed` 当前已经有“当前 session 注册动作”校验方向。改造后要确保它校验的是：

- 当前 session 的 `agentId`。
- 当前 session 的 `pageKey` 或页面能力解析结果。
- 当前 `pageInstanceId`。
- 当前动作 `allowed_agent_ids_json`。
- 当前动作状态。

校验失败要能区分：

- `PAGE_CONTEXT_MISSING`
- `PAGE_NOT_REGISTERED`
- `PAGE_ACTION_NOT_FOUND`
- `PAGE_ACTION_NOT_AUTHORIZED`
- `PAGE_INSTANCE_MISMATCH`

这些错误会反向支撑 SDK 向导和页面助手自检。

## 前端与 SDK 改造

### 业务前端接入示例

SDK 接入向导和 `reachai-onboarding` skill 包需要生成新的前端模式：

```ts
const pageInstanceId = sessionStorage.getItem('reachaiPageInstanceId') || crypto.randomUUID()
sessionStorage.setItem('reachaiPageInstanceId', pageInstanceId)

function resolveReachAiPageContext() {
  return {
    pageKey: window.__REACHAI_PAGE_BRIDGE__?.pageKey || route.name || location.pathname,
    route: location.pathname,
    origin: location.origin,
    pageInstanceId,
  }
}

createEafChat({
  mount: '#reachai-global-chat',
  agentId: manifest.embed.entryAgentKeySlug,
  tokenProvider: async () => {
    const page = resolveReachAiPageContext()
    const query = new URLSearchParams({
      projectCode,
      agentId: manifest.embed.entryAgentKeySlug,
      pageKey: page.pageKey,
      route: page.route,
      origin: page.origin,
      pageInstanceId: page.pageInstanceId,
    })
    const payload = await fetch('/api/reachai/embed-token?' + query).then((res) => res.json())
    return payload.data?.token || payload.token
  },
})
```

这段逻辑必须进入：

- `SdkAccessWizard.vue` 前端示例。
- `aiOnboardingPrompt`。
- `reachai-onboarding/SKILL.md`。
- `reachai-onboarding/references/java-sdk-access.md`。
- `reachai-onboarding/references/platform-apis.md`。
- `reachai-onboarding/templates` 或脚本验证逻辑。
- 对应后端 zip 内容断言测试。

### Chat Widget 行为

全局 Chat Widget 需要支持：

- 应用主布局全局挂载。
- 每次打开或发送首条消息前刷新当前页面上下文。
- 路由切换后更新 `pageKey/route`，必要时重新创建 session 或刷新 token。
- 无页面能力时仍可对话，但不暴露页面动作。
- 页面能力存在时，展示“当前页面可操作”状态或在内部作为 Agent 上下文使用。

首期可以不做复杂 UI，只要保证上下文传递、session 隔离和动作校验正确。

## SQL 改造

首选最小 schema：

1. `eaf_embed_session` 增加 `page_key VARCHAR(160) DEFAULT NULL`。
2. `agent_definition` 不新增列，入口 Agent 标记先放入 `runtime_config_json` 或 `default_resource_config_json`。
3. `eaf_page_action_registry.allowed_agent_ids_json` 继续作为动作挂载授权。
4. 如果入口 Agent 查询复杂，再评估新增：

```sql
agent_definition.entry_type VARCHAR(32) DEFAULT NULL
```

必须同步：

- `sql/init.sql`
- `sql/upgrade-YYYYMMDD-global-entry-page-capability.sql`
- `sql/README.md`

升级 SQL 使用现有 `add_col_if_absent` 和 `add_idx_if_absent` 风格。

## 自检与验证

### SDK 接入自检

`SdkAccessCheckService` 增加检查项：

- `entry-agent`：项目入口 Agent 是否存在且启用。
- `credential-agent-policy`：项目凭证是否允许入口 Agent 申请 embedToken。
- `global-chat-button`：业务前端是否声明全局入口接入证据。
- `page-context`：业务前端 token broker 请求是否包含 `pageKey/route/origin/pageInstanceId`。
- `embed-token-broker`：保留现有检查。
- `gateway-route`：保留现有检查。

### 页面助手自检

`AiAccessSessionService.runPageAssistantChecks` 增加检查项：

- `entry-agent-bound`：页面动作是否授权给入口 Agent。
- `page-context-compatible`：页面注册的 `pageKey/routePattern/origin` 是否能被全局入口上下文匹配。
- `page-action-authorized`：所选动作白名单是否包含入口 Agent。

### 运行时验证

至少覆盖：

1. 项目入口 Agent 可申请 embedToken。
2. 不同页面实例使用不同 `pageInstanceId`，动作只回到当前实例。
3. 已接入页面可以触发页面动作。
4. 未接入页面不暴露页面动作，但仍可普通对话。
5. 动作未授权给入口 Agent 时返回明确错误。
6. token 中 `pageKey/route/origin/pageInstanceId` 与 session 创建请求不一致时拒绝。

### 推荐测试命令

后端：

```powershell
mvn -pl ai-agent-service "-Dtest=AiAssistControllerTest,AiAccessSessionServiceTest,EmbedChatControllerAuditTest,EmbedSessionServiceTest,PlatformEmbedOpsControllerTest,SdkAccessCheckServiceTest" test
```

前端：

```powershell
cd ai-admin-front
npm run build
node scripts/check-page-assistant-prompt.mjs
```

文档与 SQL：

```powershell
git diff --check
rg -n "page_key|entryAgent|entryAgentKeySlug|global AI|全局 AI 入口" sql ai-agent-service ai-admin-front docs
```

## 分阶段实施计划

### 阶段 1：平台入口 Agent 模型

- 增加项目入口 Agent 发现逻辑。
- 在 SDK onboarding manifest 中返回 `entryAgent` 信息。
- 如果无入口 Agent，管理端给出创建或选择入口 Agent 的动作。
- 增加相关后端单测。

### 阶段 2：SDK 接入向导与 AI 快速接入提示词

- 改造 `SdkAccessWizard.vue` 中“使用 AI 快速接入”的提示词。
- 改造前端示例，让 AI 工具在业务主布局接入全局按钮。
- 要求 `tokenProvider` 传 `pageKey/route/origin/pageInstanceId`。
- 更新 `reachai-onboarding` skill 包、references、templates 和验证脚本。
- 更新 zip 内容断言，防止提示词和下载包再次漂移。

### 阶段 3：页面助手挂载入口 Agent

- 改造 `PageAssistantWizard.vue` 和 `pageAssistantOnboardingPrompt.ts`，强调“接入页面能力并挂载到全局入口”。
- 页面动作注册默认带入口 Agent 白名单。
- 页面助手进度与自检展示入口 Agent 授权状态。
- 更新页面助手后端测试和前端 prompt assertion。

### 阶段 4：运行时页面能力路由

- token exchange/session 创建支持 `pageKey`。
- 增加内部页面能力解析服务。
- session 创建时写入当前页面可用动作。
- 页面动作执行校验接入该解析结果。
- 增加运行时审计与错误码。

### 阶段 5：管理端闭环视图

- 项目详情页展示“全局入口 + 页面能力”闭环状态。
- 嵌入监控支持按入口 Agent、页面、动作授权筛选。
- 未接入页面、未授权动作、未验证 handler 给出可操作提示。

## 交付物清单

- 后端：入口 Agent manifest、页面能力解析、session pageKey、运行时校验、自检服务。
- 前端管理端：SDK 向导、页面助手向导、项目详情闭环状态、嵌入监控增强。
- 前端接入包：全局按钮示例、页面上下文解析、tokenProvider 更新。
- AI 接入包：`reachai-onboarding` 和 `reachai-page-assistant-onboarding` 的提示词、references、脚本、测试断言。
- SQL：`init.sql`、upgrade SQL、`sql/README.md`。
- 文档：更新 `docs/08-平台对话框对外嵌入支持.md`、`docs/15-创建页面助手轻闭环.md`，必要时新增“项目全局 AI 入口与页面能力闭环”章节。

## 风险与决策点

1. 入口 Agent 是否自动创建：推荐首期提供“创建默认入口 Agent”按钮，不在加载 manifest 时静默创建。
2. `agent_definition` 是否新增 `entry_type`：推荐先用 JSON 标记；如果查询和 UI 复杂，再加列。
3. `pageKey` 是否进入 `embedToken` claims：推荐进入 claims 并落 session，便于防篡改和审计。
4. 动作白名单空数组含义：沿用当前语义前需要确认。推荐空数组表示项目内不限制，非空则必须匹配 Agent。
5. 路由切换是否复用 session：推荐 route/pageKey 变化后重新建 session，避免一个 session 混入多个页面上下文。

## 成功标准

- 新项目完成 `AI 快速接入` 后，业务系统出现一个全局 AI 按钮。
- 在未接入页面打开按钮，可以普通对话，但不会出现页面动作能力。
- 对某个页面完成 `创建页面助手` 后，不需要再改全局按钮，该页面自动拥有对应页面动作。
- 页面动作只投递给当前 `pageInstanceId`。
- SDK 接入向导的 AI 提示词明确要求全局入口、页面上下文和后续页面能力挂载。
- 页面助手提示词明确不改项目 SDK/网关/token broker，只注册当前页面能力并授权给入口 Agent。
- 平台能在项目详情或嵌入监控中解释：入口 Agent 是谁、当前页面能力有哪些、动作是否授权、最近运行是否成功。
