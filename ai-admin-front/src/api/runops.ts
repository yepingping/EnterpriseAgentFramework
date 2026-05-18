import { agentRequest } from './request'
import type { ReplayRequest, ReplayResult, RunComparison, RunDetail, RunDiagnostics, RunSummary } from '@/types/runops'

export function getRunOpsDetail(traceId: string) {
  return agentRequest.get<RunDetail>(`/api/runops/traces/${traceId}`)
}

export function getRecentRunOps(params?: { userId?: string; days?: number; limit?: number }) {
  return agentRequest.get<RunSummary[]>('/api/runops/traces/recent', { params })
}

export function getRunOpsDiagnostics(params?: { userId?: string; days?: number; limit?: number }) {
  return agentRequest.get<RunDiagnostics>('/api/runops/diagnostics', { params })
}

export function replayRunOpsTrace(traceId: string, data?: ReplayRequest) {
  return agentRequest.post<ReplayResult>(`/api/runops/traces/${traceId}/replay`, data ?? {})
}

export function compareRunOpsTrace(traceId: string, candidateTraceId: string) {
  return agentRequest.get<RunComparison>(`/api/runops/traces/${traceId}/compare/${candidateTraceId}`)
}
