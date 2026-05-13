import { modelRequest } from './request'
import type {
  ModelChatRequest,
  ModelChatResponse,
  ModelInstance,
  ModelInstanceRequest,
  ModelInstanceTestResult,
} from '@/types/model'
import type { ApiResult } from '@/types/import'

export function modelChat(data: ModelChatRequest) {
  return modelRequest.post<ApiResult<ModelChatResponse>>('/chat', data)
}

export function modelChatStream(data: ModelChatRequest): Promise<Response> {
  return fetch('/model/chat/stream', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(data),
  })
}

export function getModelInstances(params?: { workspaceId?: string; modelType?: string; provider?: string }) {
  return modelRequest.get<ApiResult<ModelInstance[]>>('/instances', { params })
}

export function createModelInstance(data: ModelInstanceRequest) {
  return modelRequest.post<ApiResult<ModelInstance>>('/instances', data)
}

export function updateModelInstance(id: string, data: ModelInstanceRequest) {
  return modelRequest.put<ApiResult<ModelInstance>>(`/instances/${id}`, data)
}

export function deleteModelInstance(id: string) {
  return modelRequest.delete<ApiResult<boolean>>(`/instances/${id}`)
}

export function testModelInstance(id: string) {
  return modelRequest.post<ApiResult<ModelInstanceTestResult>>(`/instances/${id}/test`)
}
