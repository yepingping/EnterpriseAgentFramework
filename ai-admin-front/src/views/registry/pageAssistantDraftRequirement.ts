export type PageAssistantFlowMode = 'LINEAR_QUERY' | 'INTENT_ROUTER'

export type PageAssistantGoal = 'query' | 'operate' | 'queryThenAction'

export interface PageActionFlowInput {
  actionKey: string
  title?: string
  confirmRequired?: boolean
}

export interface IntentClassSpec {
  id: string
  label: string
  description: string
  keywords: string[]
  actionKeys: string[]
  requiresConfirm?: boolean
}

export interface PageAssistantFlowPlan {
  flowMode: PageAssistantFlowMode
  selectedActionKeys: string[]
  recommendedFlow: string[]
  intentClasses: IntentClassSpec[]
  safetyNotes: string[]
}

type ActionCategory = 'query_chain' | 'reset' | 'page_state' | 'operational' | 'other'

function normalizeCompact(key: string) {
  return String(key || '').toLowerCase().replace(/[^a-z0-9]/g, '')
}

function categorizeAction(action: PageActionFlowInput): ActionCategory {
  const key = action.actionKey || ''
  const compact = normalizeCompact(key)
  if (compact === 'setfilters' || compact.includes('setfilter')) return 'query_chain'
  if (compact === 'search' || compact.includes('search') || compact.includes('query')) return 'query_chain'
  if (compact === 'readtable' || compact.includes('readtable')) return 'query_chain'
  if (compact === 'reset' || compact.includes('reset')) return 'reset'
  if (compact === 'getpagestate' || compact.includes('pagestate')) return 'page_state'
  if (action.confirmRequired) return 'operational'
  if (
    compact.includes('openrow')
    || compact.includes('delete')
    || compact.includes('submit')
    || compact.includes('approve')
    || compact.includes('export')
  ) {
    return 'operational'
  }
  return 'other'
}

export function decidePageAssistantFlowMode(actions: PageActionFlowInput[]): PageAssistantFlowMode {
  if (actions.length <= 1) return 'LINEAR_QUERY'

  const categories = new Set(actions.map((action) => categorizeAction(action)))
  if ([...categories].every((category) => category === 'query_chain')) {
    return 'LINEAR_QUERY'
  }

  const hasQueryChain = categories.has('query_chain')
  const hasReset = categories.has('reset')
  const hasPageState = categories.has('page_state')
  const hasOperational = categories.has('operational')
  const hasOther = categories.has('other')

  if (hasQueryChain && (hasReset || hasPageState || hasOperational || hasOther)) {
    return 'INTENT_ROUTER'
  }

  let exclusiveGroups = 0
  if (hasReset) exclusiveGroups += 1
  if (hasPageState) exclusiveGroups += 1
  if (hasOperational) exclusiveGroups += 1
  if (hasOther) exclusiveGroups += 1
  if (exclusiveGroups >= 2) return 'INTENT_ROUTER'
  if (exclusiveGroups >= 1 && hasQueryChain) return 'INTENT_ROUTER'
  if (actions.length > 1) return 'INTENT_ROUTER'

  return 'LINEAR_QUERY'
}

function linearRecommendedFlow(actions: PageActionFlowInput[]) {
  const keys = actions.map((item) => item.actionKey).filter(Boolean)
  const flow = ['USER_INPUT']
  const hasSetFilters = keys.some((key) => normalizeCompact(key).includes('setfilter'))
  if (hasSetFilters) {
    flow.push('LLM筛选提取(extracted_filters)', 'setFilters')
  }
  for (const key of ['search', 'readTable']) {
    if (keys.some((item) => normalizeCompact(item) === normalizeCompact(key))) {
      flow.push(key)
    }
  }
  for (const key of keys) {
    const compact = normalizeCompact(key)
    if (compact.includes('setfilter') || compact === 'search' || compact === 'readtable') continue
    flow.push(key)
  }
  flow.push('ANSWER')
  return flow
}

function uniqueActionKeys(actions: PageActionFlowInput[]) {
  return [...new Set(actions.map((action) => action.actionKey).filter(Boolean))]
}

export function buildIntentClasses(actions: PageActionFlowInput[]): IntentClassSpec[] {
  const classes: IntentClassSpec[] = []
  const queryActions = actions.filter((action) => categorizeAction(action) === 'query_chain')
  if (queryActions.length) {
    classes.push({
      id: 'query_intent',
      label: '查询数据',
      description: '用户想按条件查询、搜索或读取表格结果',
      keywords: ['查询', '搜索', '筛选', '查找', '表格', '列表', '查一下'],
      actionKeys: uniqueActionKeys(queryActions),
    })
  }

  const resetActions = actions.filter((item) => categorizeAction(item) === 'reset')
  if (resetActions.length) {
    classes.push({
      id: 'reset_intent',
      label: resetActions.length === 1 ? (resetActions[0].title || '重置筛选') : '重置筛选',
      description: '用户想清空或重置页面筛选条件',
      keywords: ['重置', '清空', '恢复默认', '清除筛选'],
      actionKeys: uniqueActionKeys(resetActions),
    })
  }

  const pageStateActions = actions.filter((item) => categorizeAction(item) === 'page_state')
  if (pageStateActions.length) {
    classes.push({
      id: 'page_state_intent',
      label: pageStateActions.length === 1 ? (pageStateActions[0].title || '读取页面状态') : '读取页面状态',
      description: '用户想查看当前筛选、分页或表格状态',
      keywords: ['当前状态', '页面状态', '现在筛选', '当前条件', '分页'],
      actionKeys: uniqueActionKeys(pageStateActions),
    })
  }

  const operationalActions = actions.filter((item) => categorizeAction(item) === 'operational')
  const openRowActions = operationalActions.filter((item) => normalizeCompact(item.actionKey).includes('openrow'))
  if (openRowActions.length) {
    classes.push({
      id: 'row_action_intent',
      label: openRowActions.length === 1 ? (openRowActions[0].title || '行内操作') : '行内操作',
      description: '用户想执行表格行内操作',
      keywords: ['打开', '行操作', '周期', '详情', '操作'],
      actionKeys: uniqueActionKeys(openRowActions),
      requiresConfirm: openRowActions.some((action) => action.confirmRequired),
    })
  }
  for (const action of operationalActions.filter((item) => !normalizeCompact(item.actionKey).includes('openrow'))) {
    classes.push({
      id: `${normalizeCompact(action.actionKey) || 'operational'}_intent`.slice(0, 32),
      label: action.title || action.actionKey,
      description: `用户想执行页面操作：${action.title || action.actionKey}`,
      keywords: [action.title || action.actionKey, '操作', '执行'],
      actionKeys: [action.actionKey],
      requiresConfirm: Boolean(action.confirmRequired),
    })
  }

  const otherActions = actions.filter((item) => categorizeAction(item) === 'other')
  if (otherActions.length === 1) {
    const action = otherActions[0]
    classes.push({
      id: `${normalizeCompact(action.actionKey) || 'other'}_intent`.slice(0, 32),
      label: action.title || action.actionKey,
      description: `用户想执行页面操作：${action.title || action.actionKey}`,
      keywords: [action.title || action.actionKey, '操作', '执行'],
      actionKeys: [action.actionKey],
    })
  } else if (otherActions.length > 1) {
    classes.push({
      id: 'other_intent',
      label: '其他页面操作',
      description: '用户想执行其他已注册的页面动作',
      keywords: ['操作', '执行', '处理'],
      actionKeys: uniqueActionKeys(otherActions),
    })
  }

  return classes
}

function buildSafetyNotes(actions: PageActionFlowInput[]) {
  const notes: string[] = []
  for (const action of actions) {
    if (action.confirmRequired) {
      notes.push(`${action.actionKey} 为需确认动作：route 分支中先 INTERACTION(confirm_action)，再 PAGE_ACTION(${action.actionKey})。`)
    }
  }
  return notes
}

export function buildPageAssistantFlowPlan(actions: PageActionFlowInput[]): PageAssistantFlowPlan {
  const selectedActionKeys = actions.map((item) => item.actionKey).filter(Boolean)
  const flowMode = decidePageAssistantFlowMode(actions)
  return {
    flowMode,
    selectedActionKeys,
    recommendedFlow: linearRecommendedFlow(actions),
    intentClasses: flowMode === 'INTENT_ROUTER' ? buildIntentClasses(actions) : [],
    safetyNotes: buildSafetyNotes(actions),
  }
}

export interface PageAssistantDraftRequirementOptions {
  pageName: string
  assistantGoal: PageAssistantGoal
  actions: PageActionFlowInput[]
}

export function buildPageAssistantDraftRequirement(options: PageAssistantDraftRequirementOptions) {
  const { pageName, assistantGoal, actions } = options
  const plan = buildPageAssistantFlowPlan(actions)
  const actionNames = actions.map((item) => item.title || item.actionKey).join('、') || '已选择的页面动作'
  const goalText = {
    query: '根据用户自然语言提取查询条件，并触发页面查询/筛选动作。',
    operate: '根据用户确认后的意图触发页面动作，必要时先让用户确认。',
    queryThenAction: '先理解用户查询意图，必要时调用后端资产补充信息，再联动页面动作。',
  }[assistantGoal]

  const hasSetFilters = plan.selectedActionKeys.some((key) => normalizeCompact(key).includes('setfilter'))
  const filterInstruction = hasSetFilters
    ? `LLM 提取节点需 outputAlias=extracted_filters、outputFormat=json，并从用户问题提取 setFilters 的 inputSchema 字段。
setFilters 节点 args 必须映射为 extracted_filters.<字段名>，不可留空。`
    : '本次已选动作不包含 setFilters，不要生成 setFilters/设置筛选节点；只能使用已选页面动作完成流程。'

  const flowModeInstruction = plan.flowMode === 'LINEAR_QUERY'
    ? `flowMode=LINEAR_QUERY。按 recommendedFlow 生成线性链路，不要生成 INTENT_CLASSIFIER。
recommendedFlow: ${plan.recommendedFlow.join(' -> ')}。`
    : `flowMode=INTENT_ROUTER。必须生成 INTENT_CLASSIFIER(strategy=HYBRID, inputExpression=input, defaultRoute=else)。
每个 route:<classId> 只执行对应 intentClass 的 actionKeys 链路；互斥动作禁止线性串联。
intentClasses: ${JSON.stringify(plan.intentClasses, null, 2)}
route:else 必须连接 ANSWER，提示用户说明要查询、重置、读取状态还是执行操作。`

  const safetyBlock = plan.safetyNotes.length
    ? `safetyNotes:\n- ${plan.safetyNotes.join('\n- ')}`
    : 'safetyNotes: 无额外确认要求。'

  return `为${pageName}创建 PAGE_ASSISTANT 页面助手。${goalText}
selectedActionKeys: ${plan.selectedActionKeys.join('、') || '无'}
${flowModeInstruction}
${safetyBlock}
只能使用已选择的 actionKeys，禁止发明未选中的 pageAction actionKey。
${filterInstruction}
search/readTable/getPageState 等触发类动作 args 可为空，除非动作 schema 明确要求参数。
可用页面动作：${actionNames}。
ANSWER 节点使用固定中文状态文案，不要使用 {{ lastOutput }}。`
}
