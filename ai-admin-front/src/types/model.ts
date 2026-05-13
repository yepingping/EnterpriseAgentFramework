export interface ModelChatRequest {
  modelInstanceId?: string
  messages: ModelChatMessage[]
  options?: Record<string, unknown>
  tools?: unknown[]
  toolChoice?: string | Record<string, unknown>
}

export interface ModelChatMessage {
  role: 'system' | 'user' | 'assistant' | 'tool'
  content: string
  reasoningContent?: string
  toolCalls?: unknown[]
  toolCallId?: string
  name?: string
}

export interface ModelChatResponse {
  content: string
  model: string
  provider: string
  usage: TokenUsage
  reasoningContent?: string
  toolCalls?: unknown
  finishReason?: string
}

export interface TokenUsage {
  promptTokens: number
  completionTokens: number
  totalTokens: number
}

export type ModelType =
  | 'LLM'
  | 'EMBEDDING'
  | 'RERANKER'
  | 'STT'
  | 'TTS'
  | 'IMAGE'
  | 'IMAGE_GENERATION'
  | 'VIDEO'

export type EndpointType = 'BUILT_IN' | 'OPENAI_COMPATIBLE'
export type ModelInstanceStatus = 'ACTIVE' | 'DISABLED' | 'ERROR'

export interface ModelInstance {
  id: string
  name: string
  provider: string
  modelType: ModelType
  modelName: string
  endpointType: EndpointType
  workspaceId: string
  credential: Record<string, unknown>
  defaultOptions: Record<string, unknown>
  paramsSchema: unknown
  status: ModelInstanceStatus
  remark?: string
  createdAt?: string
  updatedAt?: string
}

export interface ModelInstanceRequest {
  name: string
  provider: string
  modelType: ModelType
  modelName: string
  endpointType: EndpointType
  workspaceId?: string
  credential?: Record<string, unknown>
  defaultOptions?: Record<string, unknown>
  paramsSchema?: unknown
  status?: ModelInstanceStatus
  remark?: string
}

export interface ModelInstanceTestResult {
  success: boolean
  latencyMs: number
  message: string
  modelInstanceId: string
  provider: string
  modelName: string
  modelType: string
  dimension?: number
}
