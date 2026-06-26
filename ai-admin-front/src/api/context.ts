import { agentRequest } from './request'
import type {
  ContextAuditEvent,
  ContextBinding,
  ContextEvidence,
  ContextEvidenceRequest,
  ContextItem,
  ContextItemCreateRequest,
  ContextItemListParams,
  ContextItemUpdateRequest,
  ContextLifecycleRunResult,
  ContextMemoryCandidate,
  ContextMemoryCandidateBatchReviewRequest,
  ContextMemoryCandidateCreateRequest,
  ContextMemoryCandidateListParams,
  ContextMemoryCandidateReviewRequest,
  ContextMemoryCandidateUpdateRequest,
  ContextNamespace,
  ContextNamespaceRequest,
  ContextOpsSummary,
  ContextPackageResponse,
  ContextRetrievalMode,
  ContextRuntimeUserMapping,
  ContextRuntimeUserMappingCreateRequest,
  ContextRuntimeUserMappingListParams,
  ContextScope,
  ContextSearchResult,
  MemoryLane,
} from '@/types/context'

function scopeParams(scope: ContextScope) {
  return {
    tenantId: scope.tenantId,
    projectCode: scope.projectCode ?? undefined,
    projectId: scope.projectId ?? undefined,
    memoryLane: scope.memoryLane,
  }
}

export function listContextNamespaces(params: {
  tenantId: string
  projectCode?: string
  projectId?: number | null
  namespaceType?: string
  status?: string
}) {
  return agentRequest.get<ContextNamespace[]>('/api/context/namespaces', { params })
}

export function createContextNamespace(data: ContextNamespaceRequest) {
  return agentRequest.post<ContextNamespace>('/api/context/namespaces', data)
}

export function getContextNamespace(id: number) {
  return agentRequest.get<ContextNamespace>(`/api/context/namespaces/${id}`)
}

export function deleteContextNamespace(id: number) {
  return agentRequest.delete<ContextNamespace>(`/api/context/namespaces/${id}`)
}

export function listContextItems(params: ContextItemListParams) {
  return agentRequest.get<ContextItem[]>('/api/context/items', { params })
}

export function getContextItem(id: number, scope: ContextScope) {
  return agentRequest.get<ContextItem>(`/api/context/items/${id}`, { params: scopeParams(scope) })
}

export function createContextItem(data: ContextItemCreateRequest) {
  return agentRequest.post<ContextItem>('/api/context/items', data)
}

export function updateContextItem(id: number, data: ContextItemUpdateRequest, scope: ContextScope) {
  return agentRequest.put<ContextItem>(`/api/context/items/${id}`, data, { params: scopeParams(scope) })
}

export function revokeContextItem(id: number, scope: ContextScope) {
  return agentRequest.post<ContextItem>(`/api/context/items/${id}/revoke`, scopeParams(scope))
}

export function markContextItemStale(id: number, scope: ContextScope) {
  return agentRequest.post<ContextItem>(`/api/context/items/${id}/stale`, scopeParams(scope))
}

export function verifyContextItem(
  id: number,
  body: { confidence?: number; trustLevel?: string } & ContextScope,
) {
  return agentRequest.post<ContextItem>(`/api/context/items/${id}/verify`, body)
}

export function deleteContextItem(id: number, scope: ContextScope) {
  return agentRequest.delete<ContextItem>(`/api/context/items/${id}`, { data: scopeParams(scope) })
}

export function listContextEvidence(itemId: number, scope: ContextScope) {
  return agentRequest.get<ContextEvidence[]>(`/api/context/items/${itemId}/evidence`, {
    params: scopeParams(scope),
  })
}

export function addContextEvidence(itemId: number, data: ContextEvidenceRequest, scope: ContextScope) {
  return agentRequest.post<ContextEvidence>(`/api/context/items/${itemId}/evidence`, data, {
    params: scopeParams(scope),
  })
}

export function listContextBindings(itemId: number, scope: ContextScope) {
  return agentRequest.get<ContextBinding[]>(`/api/context/items/${itemId}/bindings`, {
    params: scopeParams(scope),
  })
}

export function listContextAudit(params: {
  tenantId?: string
  projectCode?: string
  projectId?: number | null
  itemId?: number
  namespaceId?: number
  eventType?: string
  actorType?: string
  actorId?: string
  decision?: string
  traceId?: string
  dateFrom?: string
  dateTo?: string
  limit?: number
}) {
  return agentRequest.get<ContextAuditEvent[]>('/api/context/audit', { params })
}

export function listContextMemoryCandidates(params: ContextMemoryCandidateListParams) {
  return agentRequest.get<ContextMemoryCandidate[]>('/api/context/memory/candidates', { params })
}

export function createContextMemoryCandidate(data: ContextMemoryCandidateCreateRequest) {
  return agentRequest.post<ContextMemoryCandidate>('/api/context/memory/candidates', data)
}

export function approveContextMemoryCandidate(id: number, data: ContextMemoryCandidateReviewRequest) {
  return agentRequest.post<ContextMemoryCandidate>(`/api/context/memory/candidates/${id}/approve`, data)
}

export function rejectContextMemoryCandidate(id: number, data: ContextMemoryCandidateReviewRequest) {
  return agentRequest.post<ContextMemoryCandidate>(`/api/context/memory/candidates/${id}/reject`, data)
}

export function updateContextMemoryCandidate(id: number, data: ContextMemoryCandidateUpdateRequest) {
  return agentRequest.put<ContextMemoryCandidate>(`/api/context/memory/candidates/${id}`, data)
}

export function approveContextMemoryCandidateBatch(data: ContextMemoryCandidateBatchReviewRequest) {
  return agentRequest.post<ContextMemoryCandidate[]>('/api/context/memory/candidates/batch/approve', data)
}

export function rejectContextMemoryCandidateBatch(data: ContextMemoryCandidateBatchReviewRequest) {
  return agentRequest.post<ContextMemoryCandidate[]>('/api/context/memory/candidates/batch/reject', data)
}

export function getContextOpsSummary(params: {
  tenantId: string
  projectCode?: string
  projectId?: number | null
  memoryLane?: MemoryLane
  includeRuntimeUser?: boolean
}) {
  return agentRequest.get<ContextOpsSummary>('/api/context/ops/summary', { params })
}

export function runContextLifecycleDryRun(body: {
  tenantId: string
  projectCode?: string
  projectId?: number | null
  dryRun: boolean
  includeRuntimeUserItems?: boolean
}) {
  return agentRequest.post<ContextLifecycleRunResult>('/api/context/lifecycle/run', body)
}

export function composeContextPackage(body: {
  query: {
    tenantId: string
    projectCode?: string
    projectId?: number
    memoryLane: 'PROJECT_DEV'
    retrievalMode?: ContextRetrievalMode
    query?: string
  }
  maxItems?: number
  tokenBudget?: number
}) {
  return agentRequest.post<ContextPackageResponse>('/api/context/package', body)
}

export function queryContextItems(body: {
  tenantId: string
  projectCode?: string
  projectId?: number | null
  memoryLane: 'PROJECT_DEV'
  retrievalMode?: ContextRetrievalMode
  query?: string
  itemTypes?: string[]
  topK?: number
}) {
  return agentRequest.post<ContextSearchResult[]>('/api/context/query', body)
}

export function listContextRuntimeUserMappings(params: ContextRuntimeUserMappingListParams) {
  return agentRequest.get<ContextRuntimeUserMapping[]>('/api/context/runtime-user-mappings', { params })
}

export function createContextRuntimeUserMapping(data: ContextRuntimeUserMappingCreateRequest) {
  return agentRequest.post<ContextRuntimeUserMapping>('/api/context/runtime-user-mappings', data)
}

export function deleteContextRuntimeUserMapping(id: number) {
  return agentRequest.delete<ContextRuntimeUserMapping>(`/api/context/runtime-user-mappings/${id}`)
}
