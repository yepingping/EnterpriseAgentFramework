export interface PageAssistantPromptProject {
  id?: number | string | null
  projectCode?: string | null
  name?: string | null
  appKey?: string | null
}

export interface PageAssistantPromptPage {
  pageKey?: string | null
  name?: string | null
  routePattern?: string | null
}

export interface PageAssistantPromptAction {
  actionKey?: string | null
  title?: string | null
  description?: string | null
  confirmRequired?: boolean | null
}

export interface PageAssistantPromptProgress {
  aiCodingAccessKey?: string | null
  appSecretEnv?: string | null
  sessionId?: string | null
  manifestUrl?: string | null
  latestSessionUrl?: string | null
  stepReportUrl?: string | null
  targetBindUrl?: string | null
  catalogSyncUrl?: string | null
  checksRunUrl?: string | null
  registerPageUrl?: string | null
  scaffoldCommand?: string | null
  verifyCommand?: string | null
}

export interface PageAssistantOnboardingPromptContext {
  toolName: 'Cursor' | 'Codex' | 'Claude Code' | string
  platformUrl: string
  project: PageAssistantPromptProject
  page?: PageAssistantPromptPage | null
  actions?: PageAssistantPromptAction[]
  progress?: PageAssistantPromptProgress | null
}

const suggestedStepKeys = [
  'page-manifest',
  'route-detection',
  'page-structure',
  'action-design',
  'frontend-handler',
  'page-registry',
  'browser-verify',
  'handoff-summary',
]

const mvpActions = [
  ['getPageState', '读取当前筛选条件、分页状态、表格数据和已选行'],
  ['setFilters', '设置筛选表单字段，不触发额外业务逻辑'],
  ['search', '点击或调用页面现有查询流程'],
  ['reset', '点击或调用页面现有重置流程'],
  ['readTable', '读取当前表格结果、列名、可见行和分页信息'],
  ['openRowAction', '可选：打开编辑、周期管理、立法库管理等行操作入口；需要按风险确认'],
]

export function buildPageAssistantOnboardingPrompt(context: PageAssistantOnboardingPromptContext) {
  const projectCode = clean(context.project.projectCode) || '当前 ReachAI 项目'
  const projectName = clean(context.project.name) || projectCode
  const projectId = clean(context.project.id) || '未提供'
  const appKey = clean(context.project.appKey) || '未配置'
  const pageName = clean(context.page?.name) || '待识别业务页面'
  const pageKey = clean(context.page?.pageKey) || '请根据真实页面命名'
  const routePattern = clean(context.page?.routePattern) || '请在业务前端中识别真实 route'
  const actions = (context.actions || []).filter((action) => clean(action.actionKey))
  const progress = context.progress || {}
  const aiCodingAccessKey = clean(progress.aiCodingAccessKey) || '未启用或未生成；如需自动回传进度，请先在 ReachAI 平台启用 AI Coding 接入秘钥'
  const appSecretEnv = clean(progress.appSecretEnv) || 'REACHAI_REGISTRY_APP_SECRET'
  const actionCatalog = actions.length
    ? actions.map((action) => {
      const risk = action.confirmRequired ? '高风险/需确认' : '默认低风险'
      return `- ${clean(action.actionKey)}：${clean(action.title) || clean(action.description) || '页面动作'}；${risk}`
    }).join('\n')
    : '- 当前平台尚未选中动作，请按 MVP 动作清单在业务前端实现并回填页面动作目录。'
  const progressBlock = buildProgressBlock(progress)

  return `你现在要在当前业务前端仓库中完成 ReachAI 页面助手接入，请使用 ${context.toolName || 'Cursor/Codex/Claude Code'} 执行。

任务边界：
- 当前任务是“页面助手接入”，目标是让 ReachAI Agent 通过当前业务页面的 Page Action 操作页面。
- 项目级 SDK / 网关 / embed token broker / 嵌入式对话框接入属于“项目 AI 快速接入”，不是本次任务；如果发现这些基础能力缺失，只列为前置条件或待办，不要在本次任务里扩大范围。
- 优先让 AI 操作当前页面已有 UI、组件状态、查询服务和路由，不要为了简单查询绕过页面去新增后端 API Tool。
- 不要重写页面，不要改无关业务逻辑，不要绕过页面权限。
- 不要要求用户提供真实密钥，也不要把真实密钥写入代码、日志、文档或最终总结。

ReachAI 平台上下文：
- 平台地址：${context.platformUrl || '请从当前环境确认'}
- 项目 ID：${projectId}
- 项目编码：${projectCode}
- 项目名称：${projectName}
- App Key：${appKey}
- AI Coding 接入秘钥：${aiCodingAccessKey}
- App Secret 环境变量：${appSecretEnv}（只使用变量名，不读取、不打印、不提交真实值）
- 页面名称：${pageName}
- pageKey：${pageKey}
- routePattern：${routePattern}

本地执行要求：
- ReachAI 平台地址通常是 localhost，远端 WebFetch 往往无法访问；请使用业务前端所在机器的本机 PowerShell/curl 调用 manifest、registerPage 和 step 回传接口。
- 不要因为远端 fetch 访问 localhost 失败就判断 ReachAI 服务不可用，优先用本机 shell 验证。

页面助手进度会话：
${progressBlock}

AI Coding key 可写范围：
- 可以读取页面助手接入清单、刷新最新 session、回传 step、绑定目标页、同步页面动作目录、运行页面助手自检。
- 优先使用“页面一键注册 URL”一次提交目标页、动作目录、文件证据和验证结果；只有该 URL 缺失时，才降级使用目标页绑定、目录同步和自检三个分散接口。
- 不要尝试调用需要平台登录态的后台管理接口；如果某接口返回 platform login required，请回到上面的 page-assistant 专用 URL。
- 绑定目标页时，向“目标页绑定 URL”PUT：{ "pageKey": "...", "routePattern": "...", "actionKeys": [...] }。
- 同步目录时，向“页面动作目录同步 URL”POST PageCatalogRegisterRequest，至少包含 pageKey、name、routePattern、replaceActions、actions；actions 内包含 actionKey、title、description、confirmRequired、inputSchema、outputSchema、sampleArgs、metadata。
- 一键注册时，向“页面一键注册 URL”POST：sessionId、toolName、pageKey、pageName、routePattern、framework、frameworkVersion、bridgeGlobal、replaceActions、files、actions、verification、handoffSummary。

目标页面确认规则：
- 如果平台上下文没有明确选中页面、pageKey/routePattern 为空，或业务前端仓库里出现多个可能的 route / 页面组件，必须先询问用户确认具体要改造哪一个业务页面，再修改代码。
- 询问时请列出你识别到的候选页面，包括 route、页面组件文件、菜单名称或页面标题，并说明你推荐的目标。
- 候选页面选择在当前 Cursor/Codex/Claude Code 对话里完成即可；不要把候选页面列表回传 ReachAI 平台。
- 如果平台已经明确给出 pageKey 和 routePattern，也要先校验它们是否能在业务前端仓库中找到；找不到时不要猜测落点，先向用户确认。

请先识别业务前端中的真实页面结构：
1. route：找到路由定义、菜单入口、权限守卫和真实访问路径。
2. 页面组件文件：定位主页面组件、子组件、表单组件、表格组件和状态管理文件。
3. 筛选表单字段：列出字段名、控件类型、默认值、字典/远程选项来源和与查询参数的映射。
4. 查询 / 重置按钮：找到已有 click handler、查询服务、loading 状态和错误处理。
5. 表格列：列出可见列、字段名、格式化逻辑、空值处理和行主键。
6. 分页：确认 page、pageSize、total、排序参数与现有查询函数的关系。
7. 行操作按钮：识别编辑、周期管理、立法库管理等入口，说明是否需要用户确认。

第一版 MVP 重点实现查询类页面动作：
${mvpActions.map(([key, desc]) => `- ${key}：${desc}`).join('\n')}

平台当前已选/已知页面动作：
${actionCatalog}

实现要求：
1. 优先复用业务前端现有组件、服务、状态管理、路由和权限判断。
2. 在真实页面内注册 Page Action handler，handler 只操作当前页面实例，不做全局旁路调用。
3. getPageState/readTable 必须是只读动作。
4. setFilters/search/reset 应复用现有表单和查询函数；不要复制一套查询逻辑。
5. openRowAction 仅作为可选动作；新增、编辑、删除、审批、导出等动作必须标记为高风险或需要用户确认。
6. 不要绕过页面权限、业务登录态、按钮权限、数据范围或后端鉴权。
7. 页面动作返回结构化结果，至少包含 status、message、filters、pagination、rows/rowCount、selectedRow 或 openedAction 等可验证信息。
8. 若当前页面尚未接入 ReachAI chat/page bridge，只补当前页面所需的前端 handler 和目录信息；项目级网关和 token broker 缺口放到待办。

官方 Angular scaffold：
- V1 官方模板固定使用 window.__REACHAI_PAGE_BRIDGE__。
- 如业务前端缺少 Page Action bridge，可先执行 scaffold 命令生成 Angular 模板，再把模板接入目标页面。
- scaffold 命令：${clean(progress.scaffoldCommand) || '平台未生成；可使用 .\\scripts\\reachai-page-assistant.ps1 scaffold -ManifestUrl "<页面助手接入清单 URL>" -Framework angular -OutputDir ".\\src\\app\\shared\\reachai"'}
- verify 命令：${clean(progress.verifyCommand) || '平台未生成；可使用 .\\scripts\\reachai-page-assistant.ps1 verify -ManifestUrl "<页面助手接入清单 URL>" -FrontendUrl "<业务前端地址>" -Route "<目标路由>" -PageKey "<pageKey>"'}

页面动作目录建议：
- pageKey 使用稳定业务语义，例如 teamArchive.list。
- actionKey 使用稳定动作语义，例如 getPageState、setFilters、search、reset、readTable。
- 每个动作补充 title、description、inputSchema、outputSchema、sampleArgs、confirmRequired。
- 如平台已有手工声明动作，请让前端 handler 的 actionKey 与平台目录保持一致。

进度回传要求：
- 预留 stepKey：${suggestedStepKeys.join('、')}。
- 每完成或卡住一个 stepKey，都尽量向“步骤进度回传 URL”POST 状态，包含 status、message、files、evidence、reportedBy。
- status 只能使用 RUNNING、PASS、WARN、FAIL、SKIPPED。
- 不要调用项目 AI 快速接入的 SDK access session 接口；本任务只使用页面助手 page-assistant session。
- 当你识别出真实页面后，先调用“目标页绑定 URL”写回 pageKey/routePattern/actionKeys，避免平台自检继续按未选页判定。
- 如果平台一开始没有目标页，用户在当前 AI 对话里选定页面后，再调用“目标页绑定 URL”；平台会自动在创建页面助手页展示该页面的接入卡片。
- 当你生成或更新本地页面动作目录 JSON 后，调用“页面动作目录同步 URL”把 catalog 同步为 ACTIVE，再运行自检。
- 如果“页面一键注册 URL”存在，请优先在最终验证后调用它；它会合并目标页绑定、目录同步、自检和文件证据回传。
- 如果 AI Coding 接入秘钥未启用，跳过自动回传，在最终总结里列出每个 stepKey 的完成情况。

安全边界：
- 默认先做只读 / 查询类动作。
- 新增、编辑、删除、审批、导出、批量处理、状态变更、跨页面跳转等动作必须需要用户确认或标记为高风险。
- 任何 handler 都不得绕过页面权限。
- 不要把业务登录 token 当成 ReachAI 页面动作结果或日志内容输出。
- 不要输出真实用户敏感数据；读取表格结果时只返回当前页面已展示的数据。

验证要求：
1. 运行业务前端最小构建 / 类型检查，例如 npm run build、npm run typecheck、npx vue-tsc --noEmit 或该仓库已有等价命令。
2. 如果能启动页面，用浏览器验证页面动作能填条件、点击查询、读取结果。
3. 至少验证 getPageState、setFilters、search、reset、readTable；如实现 openRowAction，验证确认逻辑。
4. 如果平台或业务系统无法启动，说明缺少的环境条件，并提供静态验证证据。
5. 如可访问“页面助手自检 URL”，在完成 page-registry/frontend-handler/browser-verify 后调用一次并记录结果。
6. 最终输出修改文件、动作清单、验证结果、未完成项。

请先输出你识别到的 route、页面组件文件、表单字段、查询/重置按钮、表格列、分页和行操作清单，再开始最小改造。`
}

function buildProgressBlock(progress: PageAssistantPromptProgress) {
  const lines = [
    ['页面助手接入清单 URL', progress.manifestUrl],
    ['最新进度会话 URL', progress.latestSessionUrl],
    ['步骤进度回传 URL', progress.stepReportUrl],
    ['目标页绑定 URL', progress.targetBindUrl],
    ['页面动作目录同步 URL', progress.catalogSyncUrl],
    ['页面助手自检 URL', progress.checksRunUrl],
    ['页面一键注册 URL', progress.registerPageUrl],
    ['AI 接入会话 ID', progress.sessionId],
    ['Angular scaffold 命令', progress.scaffoldCommand],
    ['本地 verify 命令', progress.verifyCommand],
  ]
  const available = lines.filter(([, value]) => clean(value))
  if (!available.length) {
    return '- 当前平台暂未生成页面助手专用 session；请继续按提示词完成接入，并在最终总结中按 stepKey 汇报。'
  }
  return available.map(([label, value]) => `- ${label}：${value}`).join('\n')
}

function clean(value: unknown) {
  if (value === null || value === undefined) return ''
  return String(value).trim()
}
