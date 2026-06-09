import type { ToolParameter } from '@/types/tool'

export interface ApiAssetItem {
  apiId: number
  projectId: number
  projectCode?: string | null
  projectName?: string | null
  moduleId?: number | null
  moduleName?: string | null
  name: string
  description?: string | null
  parameters: ToolParameter[]
  source?: string | null
  sourceLocation?: string | null
  aiDescription?: string | null
  httpMethod?: string | null
  baseUrl?: string | null
  contextPath?: string | null
  endpointPath?: string | null
  requestBodyType?: string | null
  responseType?: string | null
  sourceType?: string | null
  parameterCount: number
  enabled: boolean
  agentVisible: boolean
  lightweightEnabled: boolean
  globalToolDefinitionId?: number | null
  globalToolName?: string | null
  globalToolQualifiedName?: string | null
  toolLinkStatus?: string | null
  semanticStatus?: string | null
  sensitiveRisk?: string | null
  removedFromSource: boolean
  lastSyncedAt?: string | null
}

export interface ApiAssetPageResponse {
  total: number
  page: number
  pageSize: number
  items: ApiAssetItem[]
}

export interface ApiAssetQuery {
  projectId?: number | null
  projectCode?: string
  moduleId?: number | null
  sourceType?: string
  keyword?: string
  toolLinkStatus?: string
  agentVisible?: boolean | null
  enabled?: boolean | null
  semanticStatus?: string
  sensitiveRisk?: string
  removedFromSource?: boolean | null
  page?: number
  pageSize?: number
}
