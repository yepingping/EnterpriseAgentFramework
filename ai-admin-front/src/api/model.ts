import { modelRequest } from './request'
import type {
  ProviderInfo,
  ModelChatRequest,
  ModelChatResponse,
  ModelInstance,
  ModelInstanceRequest,
  ModelInstanceTestResult,
} from '@/types/model'
import type { ApiResult } from '@/types/import'

export function getProviders() {
  return modelRequest.get<ApiResult<ProviderInfo[]>>('/providers')
}

/** 使用 query 参数，避免部分环境下 PathVariable 无 -parameters 导致绑定失败 */
export function testProvider(name: string) {
  return modelRequest.post<ApiResult<Record<string, unknown>>>('/providers/test', null, {
    params: { name },
  })
}

export function modelChat(data: ModelChatRequest) {
  return modelRequest.post<ApiResult<ModelChatResponse>>('/chat', data)
}

/**
 * SSE 流式模型对话 — 返回原始 Response
 */
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
