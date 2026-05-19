import { agentRequest } from './request'
import type { WorkflowCredential, WorkflowCredentialPayload } from '@/types/workflowCredential'

export function listWorkflowCredentials(params?: { projectId?: number | null; projectCode?: string | null }) {
  return agentRequest.get<WorkflowCredential[]>('/api/agent/workflow-credentials', { params })
}

export function createWorkflowCredential(data: WorkflowCredentialPayload) {
  return agentRequest.post<WorkflowCredential>('/api/agent/workflow-credentials', data)
}

export function updateWorkflowCredential(id: number, data: WorkflowCredentialPayload) {
  return agentRequest.put<WorkflowCredential>(`/api/agent/workflow-credentials/${id}`, data)
}

export function deleteWorkflowCredential(id: number) {
  return agentRequest.delete(`/api/agent/workflow-credentials/${id}`)
}
