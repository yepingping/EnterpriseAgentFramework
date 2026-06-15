type TagType = 'success' | 'info' | 'warning' | 'danger'

/** Workflow 类型（value 仍为后端枚举） */
const WORKFLOW_TYPE_LABELS: Record<string, string> = {
  CHAT: '对话',
  SDK_GRAPH: 'SDK 图',
  PAGE_ACTION: '页面动作',
}

/** Workflow 生命周期状态 */
const WORKFLOW_STATUS_LABELS: Record<string, string> = {
  DRAFT: '草稿',
  ACTIVE: '已发布',
  ARCHIVED: '已归档',
}

/** Workflow 管理来源 */
const WORKFLOW_MANAGED_BY_LABELS: Record<string, string> = {
  MANUAL: '手动创建',
  SDK: 'SDK 同步',
  AI_QUICK_ACCESS: 'AI 快捷接入',
}

export const WORKFLOW_TYPE_SELECT_OPTIONS = [
  { value: 'CHAT', label: '对话' },
  { value: 'SDK_GRAPH', label: 'SDK 图' },
  { value: 'PAGE_ACTION', label: '页面动作' },
] as const

export const WORKFLOW_STATUS_SELECT_OPTIONS = [
  { value: 'DRAFT', label: '草稿' },
  { value: 'ACTIVE', label: '已发布' },
  { value: 'ARCHIVED', label: '已归档' },
] as const

export function formatWorkflowTypeLabel(workflowType?: string | null): string {
  if (workflowType == null || workflowType === '') return '-'
  const key = workflowType.toUpperCase()
  return WORKFLOW_TYPE_LABELS[key] ?? workflowType
}

export function formatWorkflowStatusLabel(status?: string | null): string {
  if (status == null || status === '') return '草稿'
  const key = status.toUpperCase()
  return WORKFLOW_STATUS_LABELS[key] ?? status
}

export function workflowStatusTagType(status?: string | null): TagType {
  const key = (status || 'DRAFT').toUpperCase()
  if (key === 'ACTIVE') return 'success'
  if (key === 'ARCHIVED') return 'info'
  return 'warning'
}

export function formatWorkflowManagedByLabel(managedBy?: string | null): string {
  if (managedBy == null || managedBy === '') return '-'
  const key = managedBy.toUpperCase()
  return WORKFLOW_MANAGED_BY_LABELS[key] ?? managedBy
}
