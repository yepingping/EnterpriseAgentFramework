/** 注册中心 / Runtime 实例状态（value 仍为后端枚举） */
const INSTANCE_STATUS_LABELS: Record<string, string> = {
  ONLINE: '在线',
  OFFLINE: '离线',
  DISABLED: '已禁用',
  STALE: '心跳超时',
}

/** 运行位置 */
const RUNTIME_PLACEMENT_LABELS: Record<string, string> = {
  CENTRAL: '中台集中',
  EMBEDDED: '业务嵌入',
  HYBRID: '混合运行',
  CAPABILITY_HOST: '能力接入侧',
}

/** Runtime Registry 角色 */
const RUNTIME_ROLE_LABELS: Record<string, string> = {
  AGENT_RUNTIME: 'Agent Runtime',
  CAPABILITY_HOST: 'Capability Host',
}

/** Runtime 类型 / Adapter 标识 */
const RUNTIME_TYPE_LABELS: Record<string, string> = {
  SPRING_BOOT_EMBEDDED: 'Spring Boot 嵌入',
  SPRING_BOOT2_CAPABILITY_HOST: 'Spring Boot 2 接入侧',
  EMBEDDED_RUNTIME: '嵌入运行时',
  LANGGRAPH4J: 'LangGraph4j 工作流',
  AGENTSCOPE: 'AgentScope',
  WORKFLOW: '工作流',
}

export const INSTANCE_STATUS_SELECT_OPTIONS = [
  { value: 'ONLINE', label: '在线' },
  { value: 'OFFLINE', label: '离线' },
  { value: 'DISABLED', label: '已禁用' },
  { value: 'STALE', label: '心跳超时' },
] as const

export const RUNTIME_PLACEMENT_SELECT_OPTIONS = [
  { value: 'CENTRAL', label: '中台集中' },
  { value: 'EMBEDDED', label: '业务嵌入' },
  { value: 'HYBRID', label: '混合运行' },
  { value: 'CAPABILITY_HOST', label: '能力接入侧' },
] as const

export const RUNTIME_ROLE_SELECT_OPTIONS = [
  { value: 'AGENT_RUNTIME', label: 'Agent Runtime' },
  { value: 'CAPABILITY_HOST', label: 'Capability Host' },
] as const

export function formatInstanceStatusLabel(status?: string | null): string {
  if (status == null || status === '') return '-'
  const key = status.toUpperCase()
  return INSTANCE_STATUS_LABELS[key] ?? status
}

export function formatRuntimePlacementLabel(placement?: string | null): string {
  if (placement == null || placement === '') return '-'
  const key = placement.toUpperCase()
  return RUNTIME_PLACEMENT_LABELS[key] ?? placement
}

export function formatRuntimeRoleLabel(role?: string | null): string {
  if (role == null || role === '') return '-'
  const key = role.toUpperCase()
  return RUNTIME_ROLE_LABELS[key] ?? role
}

export function formatRuntimeTypeLabel(runtimeType?: string | null): string {
  if (runtimeType == null || runtimeType === '') return '-'
  const key = runtimeType.toUpperCase()
  return RUNTIME_TYPE_LABELS[key] ?? runtimeType
}

/** 实例元数据能力标签 */
export function formatRuntimeFeatureLabels(meta: {
  supportsTools?: boolean
  supportsGraph?: boolean
  supportsAutonomous?: boolean
  supportsWorkflow?: boolean
  supportsEmbeddedExecution?: boolean
  supportsHybridExecution?: boolean
}): string[] {
  const labels: string[] = []
  if (meta.supportsTools) labels.push('Capability 调用')
  if (meta.supportsGraph) labels.push('图编排')
  if (meta.supportsWorkflow) labels.push('工作流')
  if (meta.supportsAutonomous) labels.push('自主推理')
  if (meta.supportsEmbeddedExecution) labels.push('嵌入执行')
  if (meta.supportsHybridExecution) labels.push('混合执行')
  return labels
}
